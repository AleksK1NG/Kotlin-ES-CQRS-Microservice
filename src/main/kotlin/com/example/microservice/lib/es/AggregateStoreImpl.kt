package com.example.microservice.lib.es

import com.example.microservice.events.es.exceptions.AggregateNotFountException
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
    }


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
                .bind("aggregate_id", aggregate.aggregateId)
                .bind("aggregate_type", aggregate.aggregateType)
                .bind("data", snapshot.data)
                .bind("metadata", snapshot.metaData)
                .bind("version", aggregate.version)
                .await()

            log.info("(save) saveSnapshot snapshot: $snapshot, version: ${aggregate.version}")
            span.tag("saved snapshot", snapshot.toString())
        } finally {
            span.end()
        }
    }

    private suspend fun handleConcurrency(aggregateId: String) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.handleConcurrency")

        try {
            dbClient.sql(HANDLE_CONCURRENCY_QUERY).bind("aggregate_id", aggregateId).await()
            log.info("(save) handleConcurrency aggregateId: $aggregateId")
        } finally {
            span.end()
        }
    }


    private fun <T : AggregateRoot> getAggregate(aggregateId: String, aggregateType: Class<T>): T {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.getAggregate")

        try {
            val newInstance = aggregateType.getConstructor(String::class.java).newInstance(aggregateId)
            log.info("(getAggregate) newInstance: $newInstance")
            return newInstance.also { span.tag("newInstance", it.toString()) }
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
                return EventSourcingUtils.getAggregateFromSnapshot(defaultSnapshot, aggregateType).also {
                    span.tag("defaultSnapshot", it.toString())
                }
            }

            return EventSourcingUtils.getAggregateFromSnapshot(snapshot, aggregateType).also { span.tag("AggregateRoot", it.toString()) }
        } finally {
            span.end()
        }
    }

    private suspend fun loadSnapshot(aggregateId: String): Snapshot? {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.loadSnapshot")

        try {
            val snapshot = dbClient.sql(LOAD_SNAPSHOT_QUERY)
                .bind("aggregate_id", aggregateId)
                .map { row, meta ->
                    Snapshot(
                        id = UUID.randomUUID(),
                        aggregateId = row.get("aggregate_id", String::class.java) ?: "",
                        aggregateType = row.get("aggregate_type", String::class.java) ?: "",
                        data = row.get("data", ByteArray::class.java) ?: ByteArray(0),
                        metaData = row.get("metadata", ByteArray::class.java) ?: ByteArray(0),
                        version = row.get("version", BigInteger::class.java) ?: BigInteger.ZERO,
                        timeStamp = row.get("timestamp", LocalDateTime::class.java) ?: LocalDateTime.now(),
                    )
                }.awaitOne()

            log.info("(loadSnapshot) loaded snapshot: $snapshot")
            span.tag("snapshot", snapshot.toString())
            return snapshot
        } catch (e: EmptyResultDataAccessException) {
            span.error(e)
            log.info("(loadSnapshot) loaded snapshot: NOT FOUND EmptyResultDataAccessException id: $aggregateId")
            return null
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
                .bind("aggregate_id", event.aggregateId)
                .bind("aggregate_type", event.aggregateType)
                .bind("event_type", event.type)
                .bind("version", event.version)
                .bind("data", event.data)
                .bind("metadata", event.metaData)
                .await()
        } finally {
            span.end()
        }
    }

    override suspend fun loadEvents(aggregateId: String, version: BigInteger): MutableList<Event> {
        return withContext(Dispatchers.IO) {
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("AggregateStore.loadEvents")
            span.tag("aggregateId", aggregateId).tag("version", version.toString())

            try {
                dbClient.sql(LOAD_EVENTS_QUERY)
                    .bind("aggregate_id", aggregateId)
                    .bind("version", version)
                    .map { row, _ ->
                        val event = Event(
                            type = row.get("event_type", String::class.java) ?: "",
                            aggregateType = row.get("aggregate_type", String::class.java) ?: "",
                            id = "",
                            version = row.get("version", BigInteger::class.java) ?: BigInteger.ZERO,
                            aggregateId = row.get("aggregate_id", String::class.java) ?: "",
                            data = row.get("data", ByteArray::class.java) ?: byteArrayOf(),
                            metaData = row.get("metadata", ByteArray::class.java) ?: byteArrayOf(),
                            timeStamp = row.get("timestamp", LocalDateTime::class.java) ?: LocalDateTime.now(),
                        )
                        log.info("(loadEvents) type: ${event.type}, version: ${event.version}")
                        event
                    }
                    .all()
                    .toIterable()
            } finally {
                span.end()
            }
        }.toMutableList()
    }


}