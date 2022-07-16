package com.example.microservice.domain

import com.example.microservice.events.BalanceDepositedEvent
import com.example.microservice.events.BankAccountCreatedEvent
import com.example.microservice.events.EmailChangedEvent
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.Projection
import com.example.microservice.lib.es.Serializer
import com.example.microservice.lib.es.exceptions.UnknownEventTypeException
import com.example.microservice.repository.BankAccountMongoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.stereotype.Service
import reactor.util.Loggers


@Service
class BankAccountMongoProjection(
    private val mongoRepository: BankAccountMongoRepository,
    private val serializer: Serializer,
    private val tracer: Tracer
) : Projection {
    companion object {
        val log = Loggers.getLogger(BankAccountMongoProjection::class.java)
    }

    override suspend fun whenEvent(event: Event) = withContext(tracer.asContextElement()) {
        withTimeout(5000) {
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("BankAccountMongoProjection.whenEvent")
            log.info("(BankAccountMongoProjection) whenEvent event: $event")

            try {
                when (val deserializedEvent = serializer.deserialize(event)) {
                    is BankAccountCreatedEvent -> {
                        val bankAccountDocument = BankAccountDocument(event.aggregateId, deserializedEvent.email, deserializedEvent.balance, deserializedEvent.currency)
                        val savedDocument = mongoRepository.save(bankAccountDocument)
                        span.tag("savedDocument", savedDocument.toString())
                        log.info("(BankAccountMongoProjection) BankAccountCreatedEvent savedDocument: $savedDocument")
                    }

                    is BalanceDepositedEvent -> {
                        val bankAccountDocument = mongoRepository.findByAggregateId(event.aggregateId).first()
                        bankAccountDocument.balance = bankAccountDocument.balance?.add(deserializedEvent.balance)
                        val savedDocument = mongoRepository.save(bankAccountDocument)
                        span.tag("savedDocument", savedDocument.toString())
                        log.info("(BankAccountMongoProjection) BalanceDepositedEvent savedDocument: $savedDocument")
                    }

                    is EmailChangedEvent -> {
                        val bankAccountDocument = mongoRepository.findByAggregateId(event.aggregateId).first()
                        bankAccountDocument.email = deserializedEvent.email
                        val savedDocument = mongoRepository.save(bankAccountDocument)
                        span.tag("savedDocument", savedDocument.toString())
                        log.info("(BankAccountMongoProjection) EmailChangedEvent savedDocument: $savedDocument")
                    }

                    else -> throw UnknownEventTypeException("unknown event: $deserializedEvent")
                }
            } finally {
                span.end()
            }
        }
    }
}