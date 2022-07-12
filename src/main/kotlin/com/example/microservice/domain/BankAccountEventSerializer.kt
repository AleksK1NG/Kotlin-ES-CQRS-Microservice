package com.example.microservice.domain

import com.example.microservice.events.BalanceDepositedEvent
import com.example.microservice.events.BankAccountCreatedEvent
import com.example.microservice.events.BankAccountEvents
import com.example.microservice.events.EmailChangedEvent
import com.example.microservice.lib.es.AggregateRoot
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.EventSourcingUtils
import com.example.microservice.lib.es.Serializer
import org.springframework.stereotype.Component


@Component
class BankAccountEventSerializer : Serializer {
    override fun serialize(event: Any, aggregate: AggregateRoot): Event {
        val data = EventSourcingUtils.writeValueAsBytes(event)

        return when (event) {
            is BankAccountCreatedEvent -> Event(
                aggregate,
                BankAccountEvents.BANK_ACCOUNT_CREATED_V1.name,
                data,
                event.metaData
            )

            is BalanceDepositedEvent -> Event(
                aggregate,
                BankAccountEvents.BALANCE_DEPOSITED_V1.name,
                data,
                event.metaData
            )

            is EmailChangedEvent -> Event(
                aggregate,
                BankAccountEvents.EMAIL_CHANGED_V1.name,
                data,
                event.metaData
            )

            else -> throw RuntimeException("unknown event $event")
        }
    }

    override fun deserialize(event: Event): Any {
        return when (event.type) {
            BankAccountEvents.BANK_ACCOUNT_CREATED_V1.name -> EventSourcingUtils.readValue(
                event.data, BankAccountCreatedEvent::class.java
            )

            BankAccountEvents.BALANCE_DEPOSITED_V1.name -> EventSourcingUtils.readValue(
                event.data, BalanceDepositedEvent::class.java
            )

            BankAccountEvents.EMAIL_CHANGED_V1.name -> EventSourcingUtils.readValue(
                event.data, EmailChangedEvent::class.java
            )

            else -> throw RuntimeException("unknown event $event")
        }
    }

    private fun getMetadata(metaData: Any?): ByteArray? {
        if (metaData == null) return null
        return EventSourcingUtils.writeValueAsBytes(metaData)
    }
}