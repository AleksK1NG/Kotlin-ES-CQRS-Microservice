package com.example.microservice.queries

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.lib.es.AggregateStore
import com.example.microservice.repository.BankAccountMongoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class BankAccountQueryServiceImpl(
    private val aggregateStore: AggregateStore,
    private val mongoRepository: BankAccountMongoRepository,
) : BankAccountQueryService {
    companion object {
        private val log = LoggerFactory.getLogger(BankAccountQueryServiceImpl::class.java)
    }

    override suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse = withContext(Dispatchers.IO) {
        if (query.fromStore) {
            val bankAccountResponse = BankAccountResponse.of(aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java)).also {
                log.info("(GetBankAccountByIdQuery) fromStore bankAccountResponse: $it")
            }
            return@withContext bankAccountResponse
        }

        try {
            val bankAccountDocument = mongoRepository.findByAggregateId(query.aggregateId)
            log.info("(GetBankAccountByIdQuery) LOADED bankAccountDocument: $bankAccountDocument")
            return@withContext BankAccountResponse.of(bankAccountDocument).also {
                log.info("(GetBankAccountByIdQuery) bankAccountDocument: $it")
            }
        } catch (ex: Exception) {
            val bankAccountAggregate = aggregateStore.load(query.aggregateId, BankAccountAggregate::class.java).also {
                log.info("(GetBankAccountByIdQuery) bankAccountAggregate: $it")
            }

            val savedBankAccountDocument = mongoRepository.save(BankAccountDocument.of(bankAccountAggregate)).also {
                log.info("(GetBankAccountByIdQuery) savedBankAccountDocument: $it")
            }
            BankAccountResponse.of(savedBankAccountDocument)
        }
    }

}