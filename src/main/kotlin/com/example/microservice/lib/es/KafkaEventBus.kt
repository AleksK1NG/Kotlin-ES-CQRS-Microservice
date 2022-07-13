package com.example.microservice.lib.es

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit


@Service
class KafkaEventBus(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
    @Value(value = "\${order.kafka.topics.bank-account-event-store:bank-account-event-store}")
    private val bankAccountTopicName: String
) : EventBus {

    companion object {
        private val log = LoggerFactory.getLogger(KafkaEventBus::class.java)
        private const val sendTimeout: Long = 3000
    }


    override suspend fun publish(events: Array<Event>) {
        val eventsBytes = EventSourcingUtils.serializeToJsonBytes(events)
        val record = ProducerRecord<String, ByteArray>(bankAccountTopicName, eventsBytes)

        withContext(Dispatchers.IO) {
            try {
                kafkaTemplate.send(record).get(sendTimeout, TimeUnit.MILLISECONDS)
                log.info("publishing kafka record value >>>>> {}", String(record.value()))
            } catch (ex: Exception) {
                log.error("(KafkaEventBus) publish get timeout", ex)
                throw RuntimeException(ex)
            }
        }
    }
}