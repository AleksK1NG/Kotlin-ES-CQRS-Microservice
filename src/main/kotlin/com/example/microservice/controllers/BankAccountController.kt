package com.example.microservice.controllers

import com.example.microservice.commands.CreateBankAccountCommand
import com.example.microservice.commands.DepositBalanceCommand
import com.example.microservice.domain.AccountAggregate
import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.dto.CreateBankAccountRequest
import com.example.microservice.lib.es.AggregateStore
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitOne
import org.springframework.web.bind.annotation.*
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@RestController
@RequestMapping(path = ["/api/v1/bank"])
class BankAccountController(
    private val dbClient: DatabaseClient,
    val template: R2dbcEntityTemplate,
    private val aggregateStore: AggregateStore
) {
    private val log = LoggerFactory.getLogger(BankAccountController::class.java)
    private val objectMapper = ObjectMapper()
    private val repo: ConcurrentHashMap<String, Any> = ConcurrentHashMap(100)


    @PostMapping
    suspend fun createBankAccount(req: ServerHttpRequest, @RequestBody body: CreateBankAccountRequest) =
        coroutineScope {
            log.info("request: {}", req.path.value().uppercase())
            log.info("request body: {}", body)

//        delay(1000)
            val result = async(Dispatchers.Default) {
                body.apply { this.id = UUID.randomUUID().toString() }
                body
            }

            val valueAsBytes = objectMapper.writeValueAsBytes(body)
            println("string from bytes: -> ${String(valueAsBytes)}")


            val response = result.await()
            repo[body.id ?: ""] = body
            log.info("repo: {}", repo)

            return@coroutineScope ResponseEntity.ok(
                mapOf(
                    "totalCount" to repo.size,
                    "list:" to repo.values
                )
            )
        }

    @GetMapping
    suspend fun getEvents(): AccountAggregate = coroutineScope {
        val query =
            dbClient.sql { "SELECT email, version::integer, aggregate_id, data FROM microservices.accounts WHERE aggregate_id = :aggregate_id" }
                .bind("aggregate_id", "aef5faa3-82e5-401f-812a-eba6cd26de83")
                .map { row, rowMeatadata ->
                    AccountAggregate(
                        row.get("email", String::class.java),
                        row.get("version", BigInteger::class.java),
                        row.get("aggregate_id", String::class.java),
                        row.get("data", ByteArray::class.java),
                    )
                }
                .awaitOne()

        log.info("SELECTED: {}", query)

        val deserializedAccountAggregate = objectMapper.readValue(query.data, AccountAggregate::class.java)
        log.info("DESERIALIZED: {}", deserializedAccountAggregate)

        return@coroutineScope query
    }

    @PostMapping(path = ["/create"])
    suspend fun createEvent() = coroutineScope {
        val accountAggregate =
            AccountAggregate("alexander.bryksin@yandex.ru", BigInteger.ONE, UUID.randomUUID().toString())
        val dataBytes = objectMapper.writeValueAsBytes(accountAggregate)

        dbClient.sql { "INSERT INTO microservices.accounts (email, aggregate_id, data) VALUES (:email, :aggregate_id, :data)" }
            .bind("email", accountAggregate.email ?: "")
            .bind("aggregate_id", accountAggregate.id ?: "")
            .bind("data", dataBytes)
            .await()

        log.info("INSERTED: {}", accountAggregate)
        return@coroutineScope accountAggregate
    }


    @PostMapping(path = ["/account"])
    suspend fun create(@RequestBody command: CreateBankAccountCommand) = coroutineScope {
        val bankAccountAggregate = BankAccountAggregate(UUID.randomUUID().toString())
        command.aggregateId = bankAccountAggregate.aggregateId
        bankAccountAggregate.createBankAccount(command)
        aggregateStore.save(bankAccountAggregate)
        log.info("saved : {}", bankAccountAggregate)
        ResponseEntity.status(201).body(bankAccountAggregate)
    }

    @PostMapping(path = ["/deposit/{id}"])
    suspend fun depositBalance(@PathVariable(value = "id", required = true, name = "id") id: String, @RequestBody command: DepositBalanceCommand) = coroutineScope {
        val bankAccountAggregate = aggregateStore.load(id, BankAccountAggregate::class.java)
        command.aggregateId = id
        bankAccountAggregate.depositBalance(command)
        aggregateStore.save(bankAccountAggregate)
        log.info("saved : {}", bankAccountAggregate)
        ResponseEntity.status(200).body(bankAccountAggregate)
    }


    @GetMapping(path = ["/account/{id}"])
    suspend fun loadBankAccount(@PathVariable(value = "id", required = true, name = "id") id: String): BankAccountAggregate {
        val bankAccountAggregate = aggregateStore.load(id, BankAccountAggregate::class.java)
        log.info("(loadBankAccount) bankAccountAggregate: {}", bankAccountAggregate)
        return bankAccountAggregate
    }
}