package com.example.microservice.domain

import com.example.microservice.commands.ChangeEmailCommand
import com.example.microservice.commands.CreateBankAccountCommand
import com.example.microservice.commands.DepositBalanceCommand
import com.example.microservice.events.BalanceDepositedEvent
import com.example.microservice.events.BankAccountCreatedEvent
import com.example.microservice.events.EmailChangedEvent
import com.example.microservice.exceptions.InvalidAmountException
import com.example.microservice.exceptions.InvalidEmailException
import com.example.microservice.lib.es.AggregateRoot
import com.example.microservice.lib.es.exceptions.UnknownEventTypeException
import java.math.BigDecimal

class BankAccountAggregate(override val aggregateId: String) : AggregateRoot(aggregateId, type) {
    var email: String? = null
    var balance: BigDecimal = BigDecimal.ZERO
    var currency: Currency = Currency.USD

    override fun whenEvent(event: Any) {
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

            else -> throw UnknownEventTypeException("unknown event type: $event")
        }
    }


    fun createBankAccount(command: CreateBankAccountCommand) {
        if (command.balance < BigDecimal.ZERO) throw InvalidAmountException("invalid amount: ${command.balance}, aggregateId: ${command.aggregateId}")

        this.apply(
            BankAccountCreatedEvent(
                command.aggregateId,
                command.email,
                command.balance,
                command.currency,
                ByteArray(0)
            )
        )
    }

    fun depositBalance(command: DepositBalanceCommand) {
        if (command.amount < BigDecimal.ZERO) throw InvalidAmountException("invalid amount: $command.amount, aggregateId: ${command.aggregateId}")
        apply(BalanceDepositedEvent(this.aggregateId, command.amount, ByteArray(0)))
    }

    fun changeEmail(command: ChangeEmailCommand) {
        if (command.email.isEmpty()) throw InvalidEmailException("invalid email: ${command.email}, aggregateId: ${command.aggregateId}")
        apply(EmailChangedEvent(aggregateId, command.email, ByteArray(0)))
    }

    companion object {
        const val type = "BankAccount"
    }

}