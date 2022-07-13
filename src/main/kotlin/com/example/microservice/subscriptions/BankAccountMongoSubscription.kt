package com.example.microservice.subscriptions

import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.EventSourcingUtils
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service


@Service
class BankAccountMongoSubscription {

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountMongoSubscription::class.java)
        private val SERVICE_NAME = "microservice"
    }


    @KafkaListener(
        topics = ["\${microservice.kafka.topics.bank-account-event-store}"],
        groupId = "\${microservice.kafka.groupId}",
        concurrency = "\${microservice.kafka.default-concurrency}"
    )
    fun bankAccountMongoSubscription(@Payload data: ByteArray, ack: Acknowledgment) {
        log.info("Subscription data: {}", String(data))
        val deserializedEvents = EventSourcingUtils.deserializeFromJsonBytes(data, Array<Event>::class.java)
        log.info("Subscription deserializedEvents: {}", deserializedEvents)
        ack.acknowledge()
    }
}