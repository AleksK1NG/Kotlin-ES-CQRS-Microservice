package com.example.microservice.queries

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.lib.es.AggregateStore
import com.example.microservice.repository.BankAccountMongoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import reactor.util.Loggers


@Service
class BankAccountQueryServiceImpl(
    private val aggregateStore: AggregateStore,
    private val mongoRepository: BankAccountMongoRepository,
) : BankAccountQueryService {
    companion object {
        private val log = Loggers.getLogger(BankAccountQueryServiceImpl::class.java)
    }

    override suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse = withContext(Dispatchers.IO) {
        if (query.fromStore) {
            return@withContext BankAccountResponse.of(aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java)).also {
                log.info("(GetBankAccountByIdQuery) fromStore bankAccountResponse: $it")
            }
        }

        try {
            val bankAccountDocument = mongoRepository.findByAggregateId(query.aggregateId).first()
            log.info("(GetBankAccountByIdQuery) LOADED bankAccountDocument: $bankAccountDocument")
            return@withContext BankAccountResponse.of(bankAccountDocument).also { log.info("(GetBankAccountByIdQuery) bankAccountDocument: $it") }
        } catch (ex: Exception) {
            val bankAccountAggregate =
                aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java).also { log.info("(GetBankAccountByIdQuery) bankAccountAggregate: $it") }
            val savedBankAccountDocument =
                mongoRepository.save(BankAccountDocument.of(bankAccountAggregate)).also { log.info("(GetBankAccountByIdQuery) savedBankAccountDocument: $it") }
            BankAccountResponse.of(savedBankAccountDocument)
        }
    }
}