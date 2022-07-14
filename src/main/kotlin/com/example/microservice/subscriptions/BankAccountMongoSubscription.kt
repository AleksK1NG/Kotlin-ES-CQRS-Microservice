package com.example.microservice.subscriptions

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.domain.BankAccountMongoProjection
import com.example.microservice.lib.es.AggregateStore
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.EventSourcingUtils
import com.example.microservice.lib.es.exceptions.SerializationException
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
class BankAccountMongoSubscription(
    private val mongoRepository: BankAccountMongoRepository,
    private val mongoProjection: BankAccountMongoProjection,
    private val aggregateStore: AggregateStore
) {


    companion object {
        private val log = LoggerFactory.getLogger(BankAccountMongoSubscription::class.java)
        private const val handleTimeoutMillis = 5000L
        private val errorhandler = CoroutineExceptionHandler { _, throwable ->
            run { log.error("(BankAccountMongoSubscription) CoroutineExceptionHandler", throwable) }
        }
    }

    @KafkaListener(
        topics = ["\${microservice.kafka.topics.bank-account-event-store}"],
        groupId = "\${microservice.kafka.groupId}",
        concurrency = "\${microservice.kafka.default-concurrency}"
    )
    fun bankAccountMongoSubscription(@Payload data: ByteArray, ack: Acknowledgment) = runBlocking(errorhandler) {
        try {
            val deserializedEvents = EventSourcingUtils.deserializeFromJsonBytes(data, Array<Event>::class.java)
            handleMessage(data, ack, deserializedEvents)
        } catch (ex: SerializationException) {
            log.error("Subscription SerializationException <<<commit>>>", ex)
            ack.acknowledge()
        } catch (ex: Exception) {
            log.error("Subscription SerializationException", ex)
        }
    }

    private suspend fun handleMessage(data: ByteArray, ack: Acknowledgment, deserializedEvents: Array<Event>) = withTimeout(handleTimeoutMillis) {
        log.info("Subscription data: {}", String(data))
        try {
//            flowOf(*deserializedEvents).flowOn(Dispatchers.IO).collectIndexed { index, value -> mongoProjection.whenEvent(value) }
            deserializedEvents.forEachIndexed { _, event -> mongoProjection.whenEvent(event) }
            ack.acknowledge()
            log.info("Subscription <<<commit>>> events: ${deserializedEvents.map { it.aggregateId }}")
        } catch (ex: Exception) {
            log.error("Subscription handleMessage error, starting recreate projection for id: ${deserializedEvents[0].aggregateId}", ex)
            mongoRepository.deleteByAggregateId(deserializedEvents[0].aggregateId)
            val bankAccountAggregate = aggregateStore.load(deserializedEvents[0].aggregateId, BankAccountAggregate::class.java)
            val bankAccountDocument = BankAccountDocument.fromBankAccountAggregate(bankAccountAggregate)
            val savedBankAccountDocument = mongoRepository.save(bankAccountDocument)
            ack.acknowledge()
            log.info("Subscription recreated savedBankAccountDocument: $savedBankAccountDocument")
        }
    }
}