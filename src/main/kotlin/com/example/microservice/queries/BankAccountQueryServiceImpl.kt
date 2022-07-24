package com.example.microservice.queries

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.dto.PaginationResponse
import com.example.microservice.lib.es.AggregateStore
import com.example.microservice.repository.BankAccountCoroutineMongoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.stereotype.Service
import reactor.util.Loggers


@Service
class BankAccountQueryServiceImpl(
    private val aggregateStore: AggregateStore,
    private val mongoRepository: BankAccountCoroutineMongoRepository,
    private val tracer: Tracer
) : BankAccountQueryService {

    override suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse = coroutineScope {
        withContext(Dispatchers.IO + tracer.asContextElement()) {
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("GetBankAccountByIdQuery.handle")

            try {
                if (query.fromStore) {
                    return@withContext aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java).let { BankAccountResponse.of(it) }
                        .also { log.info("(GetBankAccountByIdQuery) fromStore bankAccountResponse: $it") }
                        .also { span.tag("BankAccountAggregate", it.toString()) }
                }
                return@withContext mongoRepository.findByAggregateId(query.aggregateId)
                    .let { BankAccountResponse.of(it) }
                    .also { log.info("(GetBankAccountByIdQuery) bankAccountDocument: $it") }
            } catch (ex: Exception) {
                span.error(ex)
                aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java)
                    .let { BankAccountResponse.of(mongoRepository.insert(BankAccountDocument.of(it))) }
                    .also { log.info("(GetBankAccountByIdQuery) savedBankAccountDocument: $it") }
            } finally {
                span.end()
            }
        }
    }

    override suspend fun handle(query: GetAllQuery): PaginationResponse<BankAccountDocument> = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("GetAllQuery.handle")

        try {
            mongoRepository.findAll(query.pageRequest)
                .also { log.info("(GetAllQuery) bankAccountDocuments: $it") }
                .also { span.tag("bankAccountDocuments", it.toString()) }
        } finally {
            span.end()
        }
    }

    companion object {
        private val log = Loggers.getLogger(BankAccountQueryServiceImpl::class.java)
    }
}