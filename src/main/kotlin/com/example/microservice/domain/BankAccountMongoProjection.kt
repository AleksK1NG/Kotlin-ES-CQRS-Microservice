package com.example.microservice.domain

import com.example.microservice.events.BalanceDepositedEvent
import com.example.microservice.events.BankAccountCreatedEvent
import com.example.microservice.events.EmailChangedEvent
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.Projection
import com.example.microservice.lib.es.Serializer
import com.example.microservice.lib.es.exceptions.UnknownEventTypeException
import com.example.microservice.repository.BankAccountCoroutineMongoRepository
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.stereotype.Service
import reactor.util.Loggers


@Service
class BankAccountMongoProjection(
    private val mongoRepository: BankAccountCoroutineMongoRepository,
    private val serializer: Serializer,
    private val tracer: Tracer
) : Projection {
    companion object {
        val log = Loggers.getLogger(BankAccountMongoProjection::class.java)
        private const val handleEventTimeout = 5000L
    }

    override suspend fun whenEvent(event: Event): Unit = withContext(tracer.asContextElement()) {
        withTimeout(handleEventTimeout) {
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("BankAccountMongoProjection.whenEvent")
            span.tag("event", event.toString())
            log.info("(BankAccountMongoProjection) whenEvent event: $event")

            try {
                when (val deserializedEvent = serializer.deserialize(event)) {
                    is BankAccountCreatedEvent -> {
                        val bankAccountDocument = BankAccountDocument(event.aggregateId, deserializedEvent.email, deserializedEvent.balance, deserializedEvent.currency)
                        mongoRepository.insert(bankAccountDocument).also {
                            span.tag("savedDocument", it.toString())
                            log.info("(BankAccountMongoProjection) BankAccountCreatedEvent savedDocument: $it")
                        }
                    }

                    is BalanceDepositedEvent -> {
                        val bankAccountDocument = mongoRepository.findByAggregateId(event.aggregateId)
                        bankAccountDocument.balance = bankAccountDocument.balance?.add(deserializedEvent.balance)
                        mongoRepository.updateByAggregateId(bankAccountDocument).also {
                            span.tag("savedDocument", it.toString())
                            log.info("(BankAccountMongoProjection) BalanceDepositedEvent savedDocument: $it")
                        }
                    }

                    is EmailChangedEvent -> {
                        val bankAccountDocument = mongoRepository.findByAggregateId(event.aggregateId)
                        bankAccountDocument.email = deserializedEvent.email
                        mongoRepository.updateByAggregateId(bankAccountDocument).also {
                            span.tag("savedDocument", it.toString())
                            log.info("(BankAccountMongoProjection) EmailChangedEvent savedDocument: $it")
                        }
                    }

                    else -> throw UnknownEventTypeException("unknown event: $deserializedEvent")
                }
            } finally {
                span.end()
            }
        }
    }
}