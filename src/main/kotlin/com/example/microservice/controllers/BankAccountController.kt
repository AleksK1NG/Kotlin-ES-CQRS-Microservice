package com.example.microservice.controllers

import com.example.microservice.commands.BankAccountCommandService
import com.example.microservice.commands.ChangeEmailCommand
import com.example.microservice.commands.CreateBankAccountCommand
import com.example.microservice.commands.DepositBalanceCommand
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.queries.BankAccountQueryService
import com.example.microservice.queries.GetBankAccountByIdQuery
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/v1/bank"])
class BankAccountController(
    private val bankAccountCommandService: BankAccountCommandService,
    private val bankAccountQueryService: BankAccountQueryService,
) {
    private val log = LoggerFactory.getLogger(BankAccountController::class.java)


    @PostMapping(path = ["/account"])
    suspend fun createBankAccount(@RequestBody command: CreateBankAccountCommand) = coroutineScope {
        command.aggregateId = UUID.randomUUID().toString()
        bankAccountCommandService.handle(command)
        log.info("(createBankAccount) created id: {}", command.aggregateId)
        ResponseEntity.status(HttpStatus.CREATED).body(command.aggregateId)
    }

    @PostMapping(path = ["/deposit/{id}"])
    suspend fun depositBalance(
        @PathVariable(value = "id", required = true, name = "id") id: String,
        @RequestBody command: DepositBalanceCommand
    ) = coroutineScope {
        command.aggregateId = id
        bankAccountCommandService.handle(command)
        log.info("(depositBalance) deposited id: {}, amount: {}", command.aggregateId, command.amount)
        ResponseEntity.ok()
    }

    @PostMapping(path = ["/email/{id}"])
    suspend fun changeEmail(
        @PathVariable(value = "id", required = true, name = "id") id: String,
        @RequestBody command: ChangeEmailCommand
    ) = coroutineScope {
        command.aggregateId = id
        bankAccountCommandService.handle(command)
        log.info("(changeEmail) email changed id: {}, email: {}", command.aggregateId, command.email)
        ResponseEntity.ok(id)
    }


    @GetMapping(path = ["/account/{id}"])
    suspend fun getBankAccountById(@PathVariable(value = "id", required = true, name = "id") id: String): ResponseEntity<BankAccountResponse> {
        val response = bankAccountQueryService.handle(GetBankAccountByIdQuery(id, true))
        log.info("(getBankAccountById) account loaded id: {}", id)
        return ResponseEntity.status(HttpStatus.OK).body(response)
    }
}