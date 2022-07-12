package com.example.microservice.lib.es

import com.example.microservice.controllers.BankAccountController
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.math.BigInteger


class AggregateStoreImpl(
    private val dbClient: DatabaseClient,
    private val operator: TransactionalOperator,
    private val eventBus: EventBus,
    private val serializer: Serializer
) :
    AggregateStore {

    private val log = LoggerFactory.getLogger(BankAccountController::class.java)

    private val SNAPSHOT_FREQUENCY: BigInteger = BigInteger.valueOf(3)
    private val HANDLE_CONCURRENCY_QUERY =
        "SELECT aggregate_id FROM microservices.events WHERE aggregate_id = :aggregate_id ORDER BY version LIMIT 1 FOR UPDATE"
    private val SAVE_EVENTS_QUERY =
        "INSERT INTO events (aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp) values (:aggregate_id, :aggregate_type, :event_type, :data, :metadata, :version, now())"
    private val LOAD_EVENTS_QUERY =
        "SELECT event_id ,aggregate_id, aggregate_type, event_type, data, metadata, version, timestamp FROM events e WHERE e.aggregate_id = :aggregate_id AND e.version > :version ORDER BY e.version ASC"
    private val SAVE_SNAPSHOT_QUERY =
        "INSERT INTO snapshots (aggregate_id, aggregate_type, data, metadata, version, timestamp) VALUES (:aggregate_id, :aggregate_type, :data, :metadata, :version, now()) ON CONFLICT (aggregate_id) DO UPDATE SET data = :data, version = :version, timestamp = now()"
    private val LOAD_SNAPSHOT_QUERY =
        "SELECT aggregate_id, aggregate_type, data, metadata, version, timestamp FROM snapshots s WHERE s.aggregate_id = :aggregate_id"
    private val EXISTS_QUERY = "SELECT aggregate_id FROM events WHERE e e.aggregate_id = :aggregate_id"


    override suspend fun <T : AggregateRoot> save(aggregate: T): Unit = coroutineScope {
        val events = aggregate.changes.map { serializer.serialize(it, aggregate) }

        log.info("(save) serialized events: {}", events)

        operator.executeAndAwait {

            if (aggregate.version > BigInteger.ONE) {
                handleConcurrency(aggregate.aggregateId)
            }

            saveEvents(events)

            if (aggregate.version.divide(SNAPSHOT_FREQUENCY) == BigInteger.ZERO) {
                saveSnapshot(aggregate)
            }

//            withContext(Dispatchers.Default) { eventBus.publish(events) }

            log.info("(save) saved aggregate: {}", aggregate)
        }
    }

    private suspend fun <T : AggregateRoot> saveSnapshot(aggregate: T) {
        aggregate.clearChanges()
        val snapshot = EventSourcingUtils.snapshotFromAggregate(aggregate)

        dbClient.sql(SAVE_SNAPSHOT_QUERY)
            .bind("aggregate_id", aggregate.aggregateId)
            .bind("aggregate_type", aggregate.aggregateType)
            .bind("data", snapshot.data)
            .bind("metadata", snapshot.metaData ?: ByteArray(0))
            .bind("version", aggregate.version)
            .await()

        log.info("(save) saveSnapshot snapshot: {}", snapshot)

    }

    private suspend fun handleConcurrency(aggregateId: String) {
        dbClient.sql(HANDLE_CONCURRENCY_QUERY).bind("aggregate_id", aggregateId).await()
        log.info("(save) handleConcurrency aggregateId: {}", aggregateId)
    }

    override suspend fun <T : AggregateRoot> load(aggregateId: String, aggregateType: Class<T>) {
        TODO("Not yet implemented")
    }

    override suspend fun saveEvents(events: List<Event>) {
        events.forEach { saveEvent(it) }
    }

    private suspend fun saveEvent(event: Event) {
        return dbClient.sql(SAVE_EVENTS_QUERY)
            .bind("aggregate_id", event.aggregateId ?: "")
            .bind("aggregate_id", event.aggregateType ?: "")
            .bind("event_type", event.type)
            .bind("version", event.version)
            .bind("data", event.data)
            .bind("metadata", event.metaData ?: ByteArray(0))
            .await()
    }

    override suspend fun loadEvents(aggregateId: String, version: BigInteger): MutableList<Event> {
        TODO("Not yet implemented")
    }


}