package com.example.microservice.subscriptions

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.domain.BankAccountMongoProjection
import com.example.microservice.lib.es.AggregateStore
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.EventSourcingUtils
import com.example.microservice.lib.es.exceptions.SerializationException
import com.example.microservice.repository.BankAccountCoroutineMongoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import reactor.util.Loggers
import java.util.*


@Service
class BankAccountMongoSubscription(
    private val mongoRepository: BankAccountCoroutineMongoRepository,
    private val mongoProjection: BankAccountMongoProjection,
    private val aggregateStore: AggregateStore,
    private val tracer: Tracer
) {

    companion object {
        private val log = Loggers.getLogger(BankAccountMongoSubscription::class.java)
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
    fun subscribe(@Payload data: ByteArray, ack: Acknowledgment): Any = runBlocking(errorhandler + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("BankAccountMongoSubscription.subscribe")

        try {
            val deserializedEvents = EventSourcingUtils.deserializeFromJsonBytes(data, Array<Event>::class.java)
            handleMessage(data, ack, deserializedEvents).run { span.tag("deserializedEvents", deserializedEvents.contentDeepToString()) }
        } catch (ex: SerializationException) {
            ack.acknowledge().also {
                span.error(ex)
                log.error("Subscription SerializationException <<<commit>>>", ex)
            }
        } catch (ex: Exception) {
            span.error(ex)
            log.error("Subscription Exception", ex)
        } finally {
            span.end()
        }
    }

    private suspend fun handleMessage(data: ByteArray, ack: Acknowledgment, deserializedEvents: Array<Event>) = coroutineScope {
        withContext(tracer.asContextElement()) {
            withTimeout(handleTimeoutMillis) {
                val span = tracer.nextSpan(tracer.currentSpan()).start().name("BankAccountMongoSubscription.handleMessage")
                log.info("Subscription data: ${String(data)}")
                span.tag("Subscription data", String(data))

                try {
                    flowOf(*deserializedEvents).flowOn(Dispatchers.IO).collectIndexed { _, value -> mongoProjection.whenEvent(value) }
                    ack.acknowledge().also {
                        span.tag("deserializedEvents", deserializedEvents.map { it.aggregateId }.toString())
                        log.info("Subscription <<<commit>>> events: ${deserializedEvents.map { it.aggregateId }}")
                    }
                } catch (ex: Exception) {
                    span.error(ex)
                    log.error("Subscription handleMessage error, starting recreate projection for id: ${deserializedEvents[0].aggregateId}", ex)

                    mongoRepository.deleteByAggregateId(deserializedEvents[0].aggregateId)

                    aggregateStore.load(deserializedEvents[0].aggregateId, BankAccountAggregate::class.java)
                        .let { mongoRepository.insert(BankAccountDocument.of(it)) }.also {
                            ack.acknowledge().also {
                                span.tag("savedBankAccountDocument", it.toString())
                                log.info("Subscription recreated savedBankAccountDocument <<<commit>>>: $it")
                            }
                        }
                } finally {
                    span.end()
                }
            }
        }
    }
}