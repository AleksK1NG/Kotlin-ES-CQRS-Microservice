package com.example.microservice.repository

import com.example.microservice.domain.BankAccountDocument
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

interface BankAccountMongoRepository : CoroutineSortingRepository<BankAccountDocument, String> {
    suspend fun deleteByAggregateId(aggregateId: String)
    fun findByAggregateId(aggregateId: String): Flow<BankAccountDocument>
}