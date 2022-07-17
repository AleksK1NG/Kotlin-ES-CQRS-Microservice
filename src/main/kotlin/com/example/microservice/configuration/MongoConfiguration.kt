package com.example.microservice.configuration

import com.example.microservice.domain.BankAccountDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.util.Loggers
import javax.annotation.PostConstruct


@Configuration
class MongoConfiguration(private val mongoTemplate: ReactiveMongoTemplate) {

    companion object {
        private val log = Loggers.getLogger(MongoConfiguration::class.java)
    }


    @PostConstruct
    private fun mongoInit() = runBlocking {
        val bankAccountsCollection = mongoTemplate.getCollection("bankAccounts").awaitSingle()
        val bankAccountsIndex = mongoTemplate.indexOps(BankAccountDocument::class.java).ensureIndex(Index("aggregateId", Sort.DEFAULT_DIRECTION).unique()).awaitSingle()
        val bankAccountsIndexInfo = withContext(Dispatchers.IO) {
            mongoTemplate.indexOps(BankAccountDocument::class.java).indexInfo.toIterable()
        }
        log.info("MongoDB connected, bankAccounts aggregateId index created: $bankAccountsIndexInfo")
    }


    @Bean(name = ["mongoTransactionManager"])
    fun transactionManager(rdbf: ReactiveMongoDatabaseFactory): ReactiveMongoTransactionManager {
        return ReactiveMongoTransactionManager(rdbf)
    }

    @Bean(name = ["mongoTransactionOperator"])
    fun transactionOperator(rtm: ReactiveTransactionManager): TransactionalOperator {
        return TransactionalOperator.create(rtm)
    }
}