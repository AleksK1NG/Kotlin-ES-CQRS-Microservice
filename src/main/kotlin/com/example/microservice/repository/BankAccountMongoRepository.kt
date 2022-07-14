package com.example.microservice.repository

import com.example.microservice.domain.BankAccountDocument
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

interface BankAccountMongoRepository : CoroutineSortingRepository<BankAccountDocument, String> {
    suspend fun deleteByAggregateId(aggregateId: String)
    suspend fun findByAggregateId(aggregateId: String): BankAccountDocument
}