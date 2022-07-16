package com.example.microservice.commands

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.lib.es.AggregateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.stereotype.Service
import reactor.util.Loggers


@Service
class BankAccountCommandServiceImpl(
    private val aggregateStore: AggregateStore,
    private val tracer: Tracer
) : BankAccountCommandService {

    companion object {
        private val log = Loggers.getLogger(BankAccountCommandServiceImpl::class.java)
    }

    override suspend fun handle(command: CreateBankAccountCommand) = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("BankAccountCommandServiceImpl.createBankAccount")

        try {
            val bankAccount = BankAccountAggregate(command.aggregateId ?: "")
            bankAccount.createBankAccount(command)
            aggregateStore.save(bankAccount).run {
                span.tag("bankAccount", bankAccount.toString())
                log.info("(CreateBankAccountCommand) saved bankAccount: $bankAccount")
            }
        } finally {
            span.end()
        }
    }

    override suspend fun handle(command: DepositBalanceCommand) = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("BankAccountCommandServiceImpl.depositBalance")
        span.tag("cmd", command.toString())

        try {
            val bankAccount = aggregateStore.load(command.aggregateId ?: "", BankAccountAggregate::class.java)
            bankAccount.depositBalance(command)
            aggregateStore.save(bankAccount).run {
                span.tag("bankAccount", bankAccount.toString())
                log.info("(DepositBalanceCommand) saved bankAccount: $bankAccount")
            }
        } finally {
            span.end()
        }
    }

    override suspend fun handle(command: ChangeEmailCommand) = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("BankAccountCommandServiceImpl.changeEmail")

        try {
            val bankAccount = aggregateStore.load(command.aggregateId ?: "", BankAccountAggregate::class.java)
            bankAccount.changeEmail(command)
            aggregateStore.save(bankAccount).run {
                span.tag("bankAccount", bankAccount.toString())
                log.info("(ChangeEmailCommand) saved bankAccount: $bankAccount")
            }
        } finally {
            span.end()
        }
    }
}