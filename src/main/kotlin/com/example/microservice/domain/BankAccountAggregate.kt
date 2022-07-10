package com.example.microservice.domain

import com.example.microservice.commands.ChangeEmailCommand
import com.example.microservice.commands.CreateBankAccountCommand
import com.example.microservice.commands.DepositBalanceCommand
import com.example.microservice.events.ChangeEmailEvent
import com.example.microservice.events.CreateBankAccountEvent
import com.example.microservice.events.DepositBalanceEvent
import com.example.microservice.lib.es.AggregateRoot
import java.math.BigDecimal

class BankAccountAggregate(override val aggregateId: String, override val aggregateType: String) :
    AggregateRoot(aggregateId, aggregateType) {
    var email: String? = null
    var balance: BigDecimal = BigDecimal.ZERO
    var currency: String = "USD"

    override fun whenEvent(event: Any) {
        when (event) {
            is CreateBankAccountEvent -> apply {
                email = event.email
                balance = balance.add(event.balance)
                currency = event.currency ?: "USD"
            }
            is DepositBalanceEvent -> apply { balance = balance.add(event.balance) }
            is ChangeEmailEvent -> apply { email = event.email }
            else -> throw RuntimeException("unknown event type: $event")
        }
    }


    fun createBankAccount(command: CreateBankAccountCommand) {
        if (command.balance < BigDecimal.ZERO) throw RuntimeException("invalid amount")

        this.apply(
            CreateBankAccountEvent(
                command.aggregateId,
                command.email,
                command.balance,
                command.currency
            )
        )
    }

    fun depositBalance(command: DepositBalanceCommand) {
        if (command.amount < BigDecimal.ZERO) throw RuntimeException("invalid amount")
        apply(DepositBalanceEvent(this.aggregateId, command.amount))
    }

    fun changeEmail(command: ChangeEmailCommand) {
        if (command.email.isEmpty()) throw RuntimeException("invalid email")
        apply(ChangeEmailEvent(aggregateId, command.email))
    }
}