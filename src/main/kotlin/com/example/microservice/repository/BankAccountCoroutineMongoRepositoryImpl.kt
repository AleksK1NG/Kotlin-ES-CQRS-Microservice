package com.example.microservice.repository

import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.dto.PaginationResponse
import com.mongodb.client.result.DeleteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import reactor.util.Loggers


@Repository
class BankAccountCoroutineMongoRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val tracer: Tracer,
    @Qualifier(value = "mongoTransactionOperator") private val transactionalOperator: TransactionalOperator,
    private val mongoOperations: ReactiveMongoOperations
) : BankAccountCoroutineMongoRepository {

    companion object {
        private val log = Loggers.getLogger(MongoRepository::class.java)
        private fun getByAggregateIdQuery(aggregateId: String?) = Query.query(Criteria.where("aggregateId").`is`(aggregateId))
    }

    override suspend fun insert(bankAccountDocument: BankAccountDocument): BankAccountDocument = withContext(tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("MongoRepository.insert")

        try {
            return@withContext mongoTemplate.insert(bankAccountDocument).awaitSingle().also {
                span.tag("savedBankAccountDocument", bankAccountDocument.toString())
                log.info("saved savedBankAccountDocument: $it")
            }
        } finally {
            span.end()
        }
    }


    override suspend fun updateByAggregateId(bankAccountDocument: BankAccountDocument): BankAccountDocument? = withContext(tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("MongoRepository.updateByAggregateId")

        try {
            mongoOperations.save(bankAccountDocument).awaitSingle().also {
                span.tag("savedBankAccountDocument", it.toString())
                log.info("savedBankAccountDocument: $it")
            }
        } finally {
            span.end()
        }
    }

    suspend fun findAndUpdateByAggregateIdWithTransaction(bankAccountDocument: BankAccountDocument): BankAccountDocument? = coroutineScope {
        withContext(tracer.asContextElement()) {
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("MongoRepository.findAndUpdateByAggregateIdWithTransaction")

            try {
                transactionalOperator.executeAndAwait {
                    val foundDocument = mongoOperations.findOne(getByAggregateIdQuery(bankAccountDocument.aggregateId), BankAccountDocument::class.java).awaitSingle()
                    bankAccountDocument.id = foundDocument.id

                    mongoOperations.save(bankAccountDocument).awaitSingle().also {
                        span.tag("savedBankAccountDocument", it.toString())
                        log.info("savedBankAccountDocument: $it")
                    }
                }
            } finally {
                span.end()
            }
        }
    }

    override suspend fun deleteByAggregateId(aggregateId: String): DeleteResult = withContext(tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("MongoRepository.deleteByAggregateId")

        try {
            val query = Query.query(Criteria.where("aggregateId").`is`(aggregateId))
            mongoOperations.remove(query).awaitSingle().also {
                span.tag("deleteResult", it.toString())
                log.info("deleteResult: $it")
            }
        } finally {
            span.end()
        }
    }

    override suspend fun findByAggregateId(aggregateId: String): BankAccountDocument = withContext(tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("MongoRepository.findByAggregateId")

        try {
            val query = Query.query(Criteria.where("aggregateId").`is`(aggregateId))
            mongoOperations.findOne(query, BankAccountDocument::class.java).awaitSingle().also {
                span.tag("bankAccountDocument", it.toString())
                log.info("bankAccountDocument: $it")
            }
        } finally {
            span.end()
        }
    }

    override suspend fun findAll(pageRequest: PageRequest): PaginationResponse<BankAccountDocument> = coroutineScope {
        withContext(Dispatchers.IO + tracer.asContextElement()) {
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("MongoRepository.findAll")

            try {
                val totalCount = mongoOperations.count(Query(), BankAccountDocument::class.java).awaitSingle()
                mongoOperations.find(Query().with(pageRequest), BankAccountDocument::class.java).collectList().awaitSingle()
                    .let {
                        val totalPages = (totalCount.toInt() / pageRequest.pageSize)
                        val hasMore = pageRequest.pageNumber < totalPages
                        PaginationResponse(pageRequest.pageNumber, pageRequest.pageSize, totalCount, totalPages, hasMore, it)
                    }
                    .also {
                        log.info("findAll paginationResponse: $it")
                        span.tag("paginationResponse", it.toString())
                    }
            } finally {
                span.end()
            }
        }
    }
}
