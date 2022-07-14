package com.example.microservice.domain

import com.example.microservice.events.BalanceDepositedEvent
import com.example.microservice.events.BankAccountCreatedEvent
import com.example.microservice.events.EmailChangedEvent
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.Projection
import com.example.microservice.lib.es.Serializer
import com.example.microservice.lib.es.exceptions.UnknownEventTypeException
import com.example.microservice.repository.BankAccountMongoRepository
import kotlinx.coroutines.withTimeout
import org.springframework.stereotype.Service
import reactor.util.Loggers


@Service
class BankAccountMongoProjection(
    private val mongoRepository: BankAccountMongoRepository,
    private val serializer: Serializer
) : Projection {
    companion object {
        val log = Loggers.getLogger(BankAccountMongoProjection::class.java)
    }

    override suspend fun whenEvent(event: Event) = withTimeout(5000) {
        log.info("(BankAccountMongoProjection) whenEvent event: $event")

        when (val deserializedEvent = serializer.deserialize(event)) {
            is BankAccountCreatedEvent -> {
                val bankAccountDocument = BankAccountDocument(event.aggregateId, deserializedEvent.email, deserializedEvent.balance, deserializedEvent.currency)
                val savedDocument = mongoRepository.save(bankAccountDocument)
                log.info("(BankAccountMongoProjection) BankAccountCreatedEvent savedDocument: $savedDocument")
            }

            is BalanceDepositedEvent -> {
                val bankAccountDocument = mongoRepository.findByAggregateId(event.aggregateId)
                bankAccountDocument.balance = bankAccountDocument.balance?.add(deserializedEvent.balance)
                val savedDocument = mongoRepository.save(bankAccountDocument)
                log.info("(BankAccountMongoProjection) BalanceDepositedEvent savedDocument: $savedDocument")
            }

            is EmailChangedEvent -> {
                val bankAccountDocument = mongoRepository.findByAggregateId(event.aggregateId)
                bankAccountDocument.email = deserializedEvent.email
                val savedDocument = mongoRepository.save(bankAccountDocument)
                log.info("(BankAccountMongoProjection) EmailChangedEvent savedDocument: $savedDocument")
            }

            else -> throw UnknownEventTypeException("unknown event: $deserializedEvent")
        }
    }
}