package com.example.microservice.domain

import com.example.microservice.commands.ChangeEmailCommand
import com.example.microservice.commands.CreateBankAccountCommand
import com.example.microservice.commands.DepositBalanceCommand
import com.example.microservice.events.BalanceDepositedEvent
import com.example.microservice.events.BankAccountCreatedEvent
import com.example.microservice.events.EmailChangedEvent
import com.example.microservice.lib.es.AggregateRoot
import java.math.BigDecimal

class BankAccountAggregate(override val aggregateId: String, override val aggregateType: String = "BankAccount") :
    AggregateRoot(aggregateId, aggregateType) {
    var email: String? = null
    var balance: BigDecimal = BigDecimal.ZERO
    var currency: String = "USD"

    override fun whenEvent(event: Any) {
        println(event)
        return when (event) {
            is BankAccountCreatedEvent -> {
                email = event.email
                balance = balance.add(event.balance)
                currency = event.currency
            }

            is BalanceDepositedEvent -> {
                balance = balance.add(event.balance)
            }

            is EmailChangedEvent -> {
                email = event.email
            }

            else -> throw RuntimeException("unknown event type: $event")
        }
    }


    fun createBankAccount(command: CreateBankAccountCommand) {
        if (command.balance < BigDecimal.ZERO) throw RuntimeException("invalid amount")

        this.apply(
            BankAccountCreatedEvent(
                command.aggregateId ?: "",
                command.email,
                command.balance,
                command.currency,
                ByteArray(0)
            )
        )
    }

    fun depositBalance(command: DepositBalanceCommand) {
        if (command.amount < BigDecimal.ZERO) throw RuntimeException("invalid amount")
        apply(BalanceDepositedEvent(this.aggregateId, command.amount, ByteArray(0)))
    }

    fun changeEmail(command: ChangeEmailCommand) {
        if (command.email.isEmpty()) throw RuntimeException("invalid email")
        apply(EmailChangedEvent(aggregateId, command.email, ByteArray(0)))
    }
}