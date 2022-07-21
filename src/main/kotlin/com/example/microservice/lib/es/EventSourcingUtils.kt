package com.example.microservice.lib.es

import com.example.microservice.lib.es.exceptions.SerializationException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.cloud.sleuth.Span
import java.time.LocalDateTime
import java.util.*

object EventSourcingUtils {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    init {
        mapper.registerModule(ParameterNamesModule())
        mapper.registerModule(Jdk8Module())
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
    }


    fun writeValueAsBytes(value: Any): ByteArray {
        try {
            return mapper.writeValueAsBytes(value)
        } catch (ex: Exception) {
            throw SerializationException("value: $value", ex)
        }
    }

    fun <T> readValue(src: ByteArray, valueType: Class<T>): T {
        try {
            return mapper.readValue(src, valueType)
        } catch (ex: Exception) {
            throw SerializationException("src: ${String(src)}, valueType: $valueType", ex)
        }
    }

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
        try {
            return mapper.readValue(snapshot.data, valueType)
        } catch (ex: Exception) {
            throw SerializationException("type: $valueType, data: ${String(snapshot.data)}", ex)
        }
    }

    fun serializeToJsonBytes(data: Any): ByteArray {
        try {
            return mapper.writeValueAsBytes(data)
        } catch (ex: Exception) {
            throw SerializationException("data: $data", ex)
        }
    }

    fun <T> deserializeFromJsonBytes(data: ByteArray, valueType: Class<T>): T {
        try {
            return mapper.readValue(data, valueType)
        } catch (ex: Exception) {
            throw SerializationException("valueType: $valueType, data: ${String(data)}", ex)
        }
    }

    fun writeTraceSpanAsMetadata(span: Span): ByteArray {
        val parentId = span.context().parentId() ?: ""
        val spanId = span.context().spanId() ?: ""
        val traceId = span.context().traceId() ?: ""
        val sampled = span.context().sampled() ?: false
        val traceMeta = mutableMapOf<String, Any>(
            Pair("parentId", parentId),
            Pair("spanId", spanId),
            Pair("traceId", traceId),
            Pair("sampled", sampled),
        )
        return writeValueAsBytes(traceMeta)
    }
}