package com.example.microservice.queries

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.lib.es.AggregateStore
import org.springframework.stereotype.Service


@Service
class BankAccountQueryServiceImpl(private val aggregateStore: AggregateStore) : BankAccountQueryService {

    override suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse {
        return BankAccountResponse.of(aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java))
    }

}