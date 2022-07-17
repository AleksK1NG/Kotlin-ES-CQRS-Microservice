package com.example.microservice.repository

import com.example.microservice.domain.BankAccountDocument
import com.mongodb.client.result.DeleteResult


interface BankAccountCoroutineMongoRepository {
    suspend fun insert(bankAccountDocument: BankAccountDocument): BankAccountDocument

    suspend fun updateByAggregateId(bankAccountDocument: BankAccountDocument): BankAccountDocument?

    suspend fun deleteByAggregateId(aggregateId: String): DeleteResult

    suspend fun findByAggregateId(aggregateId: String): BankAccountDocument
}