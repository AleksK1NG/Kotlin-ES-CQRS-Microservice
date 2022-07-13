package com.example.microservice.queries

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.lib.es.AggregateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class BankAccountQueryServiceImpl(private val aggregateStore: AggregateStore) : BankAccountQueryService {
    companion object {
        private val log = LoggerFactory.getLogger(BankAccountQueryServiceImpl::class.java)
    }

    override suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse = withContext(Dispatchers.IO) {
        val bankAccountResponse = BankAccountResponse.of(aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java))
        log.info("(GetBankAccountByIdQuery) bankAccountResponse: {}", bankAccountResponse)
        bankAccountResponse
    }

}