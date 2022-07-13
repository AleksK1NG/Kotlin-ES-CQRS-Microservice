package com.example.microservice.subscriptions

import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.EventSourcingUtils
import com.example.microservice.repository.BankAccountMongoRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.*


@Service
class BankAccountMongoSubscription(private val mongoRepository: BankAccountMongoRepository) {


    companion object {
        private val log = LoggerFactory.getLogger(BankAccountMongoSubscription::class.java)
        private const val handleTimeoutMillis = 5000L
        private val errorhandler = CoroutineExceptionHandler { _, throwable ->
            run { log.error("(CoroutineExceptionHandler) handleMessage: {}", throwable.stackTraceToString()) }
        }
    }

    @KafkaListener(
        topics = ["\${microservice.kafka.topics.bank-account-event-store}"],
        groupId = "\${microservice.kafka.groupId}",
        concurrency = "\${microservice.kafka.default-concurrency}"
    )
    fun bankAccountMongoSubscription(@Payload data: ByteArray, ack: Acknowledgment) = runBlocking(errorhandler) {
        handleMessage(data, ack)
    }

    private suspend fun handleMessage(data: ByteArray, ack: Acknowledgment) = withTimeout(handleTimeoutMillis) {
        log.info("Subscription data: {}", String(data))
        val deserializedEvents = EventSourcingUtils.deserializeFromJsonBytes(data, Array<Event>::class.java)
        log.info("Subscription deserializedEvents: {}", deserializedEvents)
        val bankAccountDocument = BankAccountDocument()
        bankAccountDocument.aggregateId = UUID.randomUUID().toString()
        bankAccountDocument.email = deserializedEvents[0].type

        val savedBankAccountDocument = mongoRepository.save(bankAccountDocument)
        log.info("Subscription savedBankAccountDocument: {}", savedBankAccountDocument)
        ack.acknowledge()
    }
}