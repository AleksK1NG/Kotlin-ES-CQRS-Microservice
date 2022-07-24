package com.example.microservice.lib.es

import com.example.microservice.lib.es.exceptions.PublishEventException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit


@Service
class KafkaEventBus(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
    @Value(value = "\${order.kafka.topics.bank-account-event-store:bank-account-event-store}")
    private val bankAccountTopicName: String,
    private val tracer: Tracer
) : EventBus {

    override suspend fun publish(events: Array<Event>) = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("KafkaEventBus.publish")

        try {
            val eventsBytes = EventSourcingUtils.serializeToJsonBytes(events)
            val record = ProducerRecord<String, ByteArray>(bankAccountTopicName, eventsBytes)
            kafkaTemplate.send(record).get(sendTimeout, TimeUnit.MILLISECONDS)
            span.tag("record", record.toString())
            log.info("publishing kafka record value >>>>> ${String(record.value())}")
        } catch (ex: Exception) {
            span.error(ex)
            log.error("(KafkaEventBus) publish get timeout", ex)
            throw PublishEventException(ex)
        } finally {
            span.end()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaEventBus::class.java)
        private const val sendTimeout: Long = 3000
    }

}