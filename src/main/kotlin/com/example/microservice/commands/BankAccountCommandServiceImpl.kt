package com.example.microservice.commands

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.lib.es.AggregateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class BankAccountCommandServiceImpl(private val aggregateStore: AggregateStore) : BankAccountCommandService {

    companion object {
        private val log = LoggerFactory.getLogger(BankAccountCommandServiceImpl::class.java)
    }

    override suspend fun handle(command: CreateBankAccountCommand) = withContext(Dispatchers.IO) {
        val bankAccount = BankAccountAggregate(command.aggregateId ?: "")
        bankAccount.createBankAccount(command)
        aggregateStore.save(bankAccount)
        log.info("(CreateBankAccountCommand) saved bankAccount: {}", bankAccount)
    }

    override suspend fun handle(command: DepositBalanceCommand) = withContext(Dispatchers.IO) {
        val bankAccount = aggregateStore.load(command.aggregateId ?: "", BankAccountAggregate::class.java)
        bankAccount.depositBalance(command)
        aggregateStore.save(bankAccount)
        log.info("(DepositBalanceCommand) saved bankAccount: {}", bankAccount)
    }

    override suspend fun handle(command: ChangeEmailCommand) = withContext(Dispatchers.IO) {
        val bankAccount = aggregateStore.load(command.aggregateId ?: "", BankAccountAggregate::class.java)
        bankAccount.changeEmail(command)
        aggregateStore.save(bankAccount)
        log.info("(ChangeEmailCommand) saved bankAccount: {}", bankAccount)
    }
}