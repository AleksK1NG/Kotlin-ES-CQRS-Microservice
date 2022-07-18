package com.example.microservice.queries

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.lib.es.AggregateStore
import com.example.microservice.repository.BankAccountCoroutineMongoRepository
import kotlinx.coroutines.Dispatchers
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
    companion object {
        private val log = Loggers.getLogger(BankAccountQueryServiceImpl::class.java)
    }

    override suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("GetBankAccountByIdQuery.handle")
        span.tag("query span started", "cool =D")

        try {
            if (query.fromStore) {
                return@withContext BankAccountResponse.of(aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java)).also {
                    log.info("(GetBankAccountByIdQuery) fromStore bankAccountResponse: $it")
                    span.tag("BankAccountAggregate", it.toString())
                }
            }

            val bankAccountDocument = mongoRepository.findByAggregateId(query.aggregateId).also {
                span.tag("bankAccountDocument", it.toString())
                log.info("(GetBankAccountByIdQuery) LOADED bankAccountDocument: $it")
            }

            return@withContext BankAccountResponse.of(bankAccountDocument).also { log.info("(GetBankAccountByIdQuery) bankAccountDocument: $it") }
        } catch (ex: Exception) {
            span.error(ex)

            val bankAccountAggregate = aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java).also {
                log.info("(GetBankAccountByIdQuery) bankAccountAggregate: $it")
                span.tag("bankAccountAggregate", it.toString())
            }

            val savedBankAccountDocument = mongoRepository.insert(BankAccountDocument.of(bankAccountAggregate)).also {
                log.info("(GetBankAccountByIdQuery) savedBankAccountDocument: $it")
                span.tag("savedBankAccountDocument", it.toString())
            }

            BankAccountResponse.of(savedBankAccountDocument)
        } finally {
            span.end()
        }
    }
}