package com.example.microservice.lib.es

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitOne
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*


@Service
class AggregateStoreImpl(
    private val dbClient: DatabaseClient,
    private val operator: TransactionalOperator,
    private val eventBus: EventBus,
    private val serializer: Serializer
) : AggregateStore {

    companion object {
        private val log = LoggerFactory.getLogger(AggregateStoreImpl::class.java)
    }

    private val SNAPSHOT_FREQUENCY: BigInteger = BigInteger.valueOf(3)
    private val HANDLE_CONCURRENCY_QUERY =
        "SELECT aggregate_id FROM microservices.events WHERE aggregate_id = :aggregate_id ORDER BY version LIMIT 1 FOR UPDATE"
    private val SAVE_EVENTS_QUERY =
        "INSERT INTO microservices.events (aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp) values (:aggregate_id, :aggregate_type, :event_type, :data, :metadata, :version, now())"
    private val LOAD_EVENTS_QUERY =
        "SELECT event_id ,aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp FROM microservices.events e WHERE e.aggregate_id = :aggregate_id AND e.version > :version ORDER BY e.version ASC"
    private val SAVE_SNAPSHOT_QUERY =
        "INSERT INTO microservices.snapshots (aggregate_id, aggregate_type, data, metadata, version, timestamp) VALUES (:aggregate_id, :aggregate_type, :data, :metadata, :version, now()) ON CONFLICT (aggregate_id) DO UPDATE SET data = :data, version = :version, timestamp = now()"
    private val LOAD_SNAPSHOT_QUERY =
        "SELECT aggregate_id, aggregate_type, data, metadata, version, timestamp FROM microservices.snapshots s WHERE s.aggregate_id = :aggregate_id"
    private val EXISTS_QUERY = "SELECT aggregate_id FROM microservices.events WHERE e e.aggregate_id = :aggregate_id"


    override suspend fun <T : AggregateRoot> load(aggregateId: String, aggregateType: Class<T>): T {
        val snapshot = loadSnapshot(aggregateId)

        val aggregate = getAggregateFromSnapshotClass(snapshot, aggregateId, aggregateType)

        val events = loadEvents(aggregateId, aggregate.version)

        events.map { serializer.deserialize(it) }.forEach { aggregate.raiseEvent(it) }

        if (aggregate.version == BigInteger.ZERO) throw java.lang.RuntimeException("load aggregate with zero version")

        return aggregate
    }


    override suspend fun <T : AggregateRoot> save(aggregate: T): Unit = coroutineScope {
        val events = aggregate.changes.map { serializer.serialize(it, aggregate) }
        log.info("(save) serialized events: {}", events)

        operator.executeAndAwait {
            if (aggregate.version > BigInteger.ONE) handleConcurrency(aggregate.aggregateId)

            saveEvents(events)

            val res = aggregate.version % SNAPSHOT_FREQUENCY
            log.info("(VERSION CHECK) res: {}", res)

            if (aggregate.version % SNAPSHOT_FREQUENCY == BigInteger.ZERO) saveSnapshot(aggregate)


            eventBus.publish(events)

            log.info("(save) saved aggregate: {}", aggregate)
            aggregate.clearChanges()
        }
    }

    private suspend fun <T : AggregateRoot> saveSnapshot(aggregate: T) {
        aggregate.clearChanges()
        val snapshot = EventSourcingUtils.snapshotFromAggregate(aggregate)

        dbClient.sql(SAVE_SNAPSHOT_QUERY)
            .bind("aggregate_id", aggregate.aggregateId)
            .bind("aggregate_type", aggregate.aggregateType)
            .bind("data", snapshot.data)
            .bind("metadata", snapshot.metaData)
            .bind("version", aggregate.version)
            .await()


        log.info("(save) saveSnapshot snapshot: {}, version: {}", snapshot, aggregate.version)
    }

    private suspend fun handleConcurrency(aggregateId: String) {
        dbClient.sql(HANDLE_CONCURRENCY_QUERY).bind("aggregate_id", aggregateId).await()
        log.info("(save) handleConcurrency aggregateId: {}", aggregateId)
    }


    private fun <T : AggregateRoot> getAggregate(aggregateId: String, aggregateType: Class<T>): T {
        try {
            val newInstance = aggregateType.getConstructor(String::class.java).newInstance(aggregateId)
            log.info("(getAggregate) newInstance: {}", newInstance)
            return newInstance
        } catch (ex: Exception) {
            log.error("create default aggregate ex:", ex)
            throw RuntimeException("create default aggregate ex:", ex)
        }

    }

    private suspend fun <T : AggregateRoot> getAggregateFromSnapshotClass(snapshot: Snapshot?, aggregateId: String, aggregateType: Class<T>): T {
        if (snapshot == null) {
            val defaultSnapshot = EventSourcingUtils.snapshotFromAggregate(aggregate = getAggregate(aggregateId, aggregateType))
            return EventSourcingUtils.getAggregateFromSnapshot(defaultSnapshot, aggregateType)
        }
        return EventSourcingUtils.getAggregateFromSnapshot(snapshot, aggregateType)
    }

    private suspend fun loadSnapshot(aggregateId: String): Snapshot? {
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

            log.info("(loadSnapshot) loaded snapshot: {}", snapshot)
            return snapshot
        } catch (e: EmptyResultDataAccessException) {
            log.info("(loadSnapshot) loaded snapshot: NOT FOUND: {}", aggregateId)
            return null
        }
    }

    override suspend fun saveEvents(events: List<Event>) = events.forEach { saveEvent(it) }

    private suspend fun saveEvent(event: Event) {
        log.info("saving event: {}", event)
        return dbClient.sql(SAVE_EVENTS_QUERY)
            .bind("aggregate_id", event.aggregateId)
            .bind("aggregate_type", event.aggregateType)
            .bind("event_type", event.type)
            .bind("version", event.version)
            .bind("data", event.data)
            .bind("metadata", event.metaData)
            .await()
    }

    override suspend fun loadEvents(aggregateId: String, version: BigInteger): MutableList<Event> {
        return withContext(Dispatchers.IO) {
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
                    log.info("(loadEvents) type: {}, version: {}", event.type, event.version)
                    event
                }
                .all()
                .toIterable()
        }.toMutableList()
    }


}