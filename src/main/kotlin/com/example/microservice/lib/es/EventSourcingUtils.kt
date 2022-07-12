package com.example.microservice.lib.es

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.*

object EventSourcingUtils {
    private val mapper = jacksonObjectMapper()

    fun writeValueAsBytes(value: Any): ByteArray = mapper.writeValueAsBytes(value)

    fun <T> readValue(src: ByteArray, valueType: Class<T>): T = mapper.readValue(src, valueType)

    fun getAggregateTypeTopic(aggregateType: String): String = "{$aggregateType}_events"

    fun <T : AggregateRoot> snapshotFromAggregate(aggregate: T): Snapshot {
        val dataBytes = mapper.writeValueAsBytes(aggregate)
        return Snapshot(
            id = UUID.randomUUID(),
            aggregateId = aggregate.aggregateId,
            aggregateType = aggregate.aggregateType,
            version = aggregate.version,
            data = dataBytes,
            timeStamp = LocalDateTime.now()
        )
    }

    fun <T : AggregateRoot> getAggregateFromSnapshot(snapshot: Snapshot, valueType: Class<T>): T {
        return mapper.readValue(snapshot.data, valueType)
    }
}