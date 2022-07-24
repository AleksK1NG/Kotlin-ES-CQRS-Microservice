package com.example.microservice.lib.es

import com.example.microservice.events.es.exceptions.AggregateNotFountException
import com.example.microservice.lib.es.EventSourcingConstants.AGGREGATE_ID
import com.example.microservice.lib.es.EventSourcingConstants.AGGREGATE_TYPE
import com.example.microservice.lib.es.EventSourcingConstants.DATA
import com.example.microservice.lib.es.EventSourcingConstants.EVENT_TYPE
import com.example.microservice.lib.es.EventSourcingConstants.METADATA
import com.example.microservice.lib.es.EventSourcingConstants.TIMESTAMP
import com.example.microservice.lib.es.EventSourcingConstants.VERSION
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.cloud.sleuth.Tracer
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitOne
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import reactor.util.Loggers
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


@Repository
class AggregateStoreImpl(
    private val dbClient: DatabaseClient,
    private val operator: TransactionalOperator,
    private val eventBus: EventBus,
    private val serializer: Serializer,
    private val tracer: Tracer
) : AggregateStore {

    override suspend fun <T : AggregateRoot> load(aggregateId: String, aggregateType: Class<T>): T {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.load")

        try {
            val snapshot = loadSnapshot(aggregateId)

            val aggregate = getAggregateFromSnapshotClass(snapshot, aggregateId, aggregateType)

            loadEvents(aggregateId, aggregate.version)
                .map { serializer.deserialize(it) }
                .forEach { aggregate.raiseEvent(it) }

            if (aggregate.version == BigInteger.ZERO) throw AggregateNotFountException("aggregate not found id: $aggregateId, type: ${aggregateType.name}")

            return aggregate
        } finally {
            span.end()
        }
    }

    override suspend fun <T : AggregateRoot> save(aggregate: T) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.save")

        try {
            val events = aggregate.changes.map { serializer.serialize(it, aggregate) }
            log.info("(save) serialized events: $events")
            span.tag("events", events.toString())

            operator.executeAndAwait {
                if (aggregate.version > BigInteger.ONE) handleConcurrency(aggregate.aggregateId)

                saveEvents(events)

                if (aggregate.version % SNAPSHOT_FREQUENCY == BigInteger.ZERO) saveSnapshot(aggregate)

                eventBus.publish(events.toTypedArray())

                log.info("(save) saved aggregate: $aggregate")
                aggregate.clearChanges()
            }
        } finally {
            span.end()
        }
    }

    private suspend fun <T : AggregateRoot> saveSnapshot(aggregate: T) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.saveSnapshot")

        try {
            aggregate.clearChanges()
            val snapshot = EventSourcingUtils.snapshotFromAggregate(aggregate)

            dbClient.sql(SAVE_SNAPSHOT_QUERY)
                .bind(AGGREGATE_ID, aggregate.aggregateId)
                .bind(AGGREGATE_TYPE, aggregate.aggregateType)
                .bind(DATA, snapshot.data)
                .bind(METADATA, snapshot.metaData)
                .bind(VERSION, aggregate.version)
                .await()

            log.info("(save) saved snapshot: $snapshot, version: ${aggregate.version}")
            span.tag("saved snapshot", snapshot.toString())
        } finally {
            span.end()
        }
    }

    private suspend fun handleConcurrency(aggregateId: String) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.handleConcurrency")

        try {
            dbClient.sql(HANDLE_CONCURRENCY_QUERY).bind(AGGREGATE_ID, aggregateId).await()
                .also { log.info("(save) handleConcurrency aggregateId: $aggregateId") }
        } finally {
            span.end()
        }
    }


    private fun <T : AggregateRoot> getAggregate(aggregateId: String, aggregateType: Class<T>): T {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.getAggregate")

        try {
            return aggregateType.getConstructor(String::class.java).newInstance(aggregateId)
                .also {
                    span.tag("newInstance", it.toString())
                    log.info("(getAggregate) newInstance: $it")
                }
        } catch (ex: Exception) {
            span.error(ex)
            log.error("create default aggregate ex:", ex)
            throw RuntimeException("create default aggregate ex:", ex)
        } finally {
            span.end()
        }

    }

    private suspend fun <T : AggregateRoot> getAggregateFromSnapshotClass(snapshot: Snapshot?, aggregateId: String, aggregateType: Class<T>): T {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.loadSnapshot")

        try {
            if (snapshot == null) {
                val defaultSnapshot = EventSourcingUtils.snapshotFromAggregate(aggregate = getAggregate(aggregateId, aggregateType))
                return EventSourcingUtils.getAggregateFromSnapshot(defaultSnapshot, aggregateType)
                    .also { span.tag("defaultSnapshot", it.toString()) }
            }

            return EventSourcingUtils.getAggregateFromSnapshot(snapshot, aggregateType).also { span.tag("AggregateRoot", it.toString()) }
        } finally {
            span.end()
        }
    }

    private suspend fun loadSnapshot(aggregateId: String): Snapshot? {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.loadSnapshot")

        return try {
            dbClient.sql(LOAD_SNAPSHOT_QUERY)
                .bind(AGGREGATE_ID, aggregateId)
                .map { row, meta -> snapshotFromRow(row, meta) }
                .awaitOne()
                .also {
                    log.info("(loadSnapshot) loaded snapshot: $it")
                    span.tag("snapshot", it.toString())
                }
        } catch (ex: EmptyResultDataAccessException) {
            log.info("(loadSnapshot) snapshot not found EmptyResultDataAccessException, creating default for id: $aggregateId")
            null
        } finally {
            span.end()
        }
    }

    override suspend fun saveEvents(events: List<Event>) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.saveEvents")

        try {
            span.tag("events", events.toString())
            return events.forEach { saveEvent(it) }
        } finally {
            span.end()
        }
    }

    private suspend fun saveEvent(event: Event) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.saveEvent")

        try {
            log.info("saving event: $event")
            span.tag("event", event.toString())
            return dbClient.sql(SAVE_EVENTS_QUERY)
                .bind(AGGREGATE_ID, event.aggregateId)
                .bind(AGGREGATE_TYPE, event.aggregateType)
                .bind(EVENT_TYPE, event.type)
                .bind(VERSION, event.version)
                .bind(DATA, event.data)
                .bind(METADATA, event.metaData)
                .await()
        } finally {
            span.end()
        }
    }

    override suspend fun loadEvents(aggregateId: String, version: BigInteger): MutableList<Event> = withContext(Dispatchers.IO) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.loadEvents")
        span.tag("aggregateId", aggregateId).tag("version", version.toString())

        try {
            dbClient.sql(LOAD_EVENTS_QUERY)
                .bind(AGGREGATE_ID, aggregateId)
                .bind(VERSION, version)
                .map { row, meta -> eventFromRow(row, meta).also { log.info("(loadEvents) type: ${it.type}, version: ${it.version}") } }
                .all()
                .toIterable()
        } finally {
            span.end()
        }
    }.toMutableList()


    companion object {
        private val log = Loggers.getLogger(AggregateStoreImpl::class.java)

        private val SNAPSHOT_FREQUENCY: BigInteger = BigInteger.valueOf(3)

        private const val HANDLE_CONCURRENCY_QUERY =
            """SELECT aggregate_id FROM microservices.events WHERE aggregate_id = :aggregate_id ORDER BY version LIMIT 1 FOR UPDATE"""
        private const val SAVE_EVENTS_QUERY =
            """INSERT INTO microservices.events (aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp) values (:aggregate_id, :aggregate_type, :event_type, :data, :metadata, :version, now())"""
        private const val LOAD_EVENTS_QUERY =
            """SELECT event_id ,aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp FROM microservices.events e WHERE e.aggregate_id = :aggregate_id AND e.version > :version ORDER BY e.version ASC"""
        private const val SAVE_SNAPSHOT_QUERY =
            """INSERT INTO microservices.snapshots (aggregate_id, aggregate_type, data, metadata, version, timestamp) VALUES (:aggregate_id, :aggregate_type, :data, :metadata, :version, now()) ON CONFLICT (aggregate_id) DO UPDATE SET data = :data, version = :version, timestamp = now()"""
        private const val LOAD_SNAPSHOT_QUERY =
            """SELECT aggregate_id, aggregate_type, data, metadata, version, timestamp FROM microservices.snapshots s WHERE s.aggregate_id = :aggregate_id"""
        private const val EXISTS_QUERY = """SELECT aggregate_id FROM microservices.events WHERE e e.aggregate_id = :aggregate_id"""

        private fun eventFromRow(row: Row, meta: RowMetadata): Event {
            return Event(
                type = row.get(EVENT_TYPE, String::class.java) ?: "",
                aggregateType = row.get(AGGREGATE_TYPE, String::class.java) ?: "",
                id = "",
                version = row.get(VERSION, BigInteger::class.java) ?: BigInteger.ZERO,
                aggregateId = row.get(AGGREGATE_ID, String::class.java) ?: "",
                data = row.get(DATA, ByteArray::class.java) ?: byteArrayOf(),
                metaData = row.get(METADATA, ByteArray::class.java) ?: byteArrayOf(),
                timeStamp = row.get(TIMESTAMP, LocalDateTime::class.java) ?: LocalDateTime.now(),
            )
        }

        private fun snapshotFromRow(row: Row, meta: RowMetadata): Snapshot {
            return Snapshot(
                id = UUID.randomUUID(),
                aggregateId = row.get(AGGREGATE_ID, String::class.java) ?: "",
                aggregateType = row.get(AGGREGATE_TYPE, String::class.java) ?: "",
                data = row.get(DATA, ByteArray::class.java) ?: byteArrayOf(),
                metaData = row.get(METADATA, ByteArray::class.java) ?: byteArrayOf(),
                version = row.get(VERSION, BigInteger::class.java) ?: BigInteger.ZERO,
                timeStamp = row.get(TIMESTAMP, LocalDateTime::class.java) ?: LocalDateTime.now(),
            )
        }
    }

}