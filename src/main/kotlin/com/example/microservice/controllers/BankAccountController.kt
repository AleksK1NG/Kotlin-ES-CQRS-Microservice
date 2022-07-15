package com.example.microservice.controllers

import com.example.microservice.commands.BankAccountCommandService
import com.example.microservice.commands.ChangeEmailCommand
import com.example.microservice.commands.CreateBankAccountCommand
import com.example.microservice.commands.DepositBalanceCommand
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.dto.ChangeEmailRequest
import com.example.microservice.dto.CreateBankAccountRequest
import com.example.microservice.dto.DepositBalanceRequest
import com.example.microservice.queries.BankAccountQueryService
import com.example.microservice.queries.GetBankAccountByIdQuery
import kotlinx.coroutines.coroutineScope
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.util.Loggers
import java.util.*


@RestController
@RequestMapping(path = ["/api/v1/bank"])
class BankAccountController(
    private val bankAccountCommandService: BankAccountCommandService,
    private val bankAccountQueryService: BankAccountQueryService,
) {
    companion object {
        private val log = Loggers.getLogger(BankAccountController::class.java)
    }

    @PostMapping(path = ["/account"])
    suspend fun createBankAccount(@RequestBody request: CreateBankAccountRequest) = coroutineScope {
        val command = CreateBankAccountCommand(UUID.randomUUID().toString(), request.email, request.balance, request.currency)
        bankAccountCommandService.handle(command)
        ResponseEntity.status(HttpStatus.CREATED).body(command.aggregateId).also { log.info("(createBankAccount) created id: {}", command.aggregateId) }
    }

    @PostMapping(path = ["/deposit/{id}"])
    suspend fun depositBalance(
        @PathVariable(value = "id", required = true, name = "id") id: String,
        @RequestBody request: DepositBalanceRequest
    ) = coroutineScope {
        val command = DepositBalanceCommand(id, request.amount)
        bankAccountCommandService.handle(command)
        ResponseEntity.ok().also { log.info("(depositBalance) deposited id: $command.aggregateId, amount: $command.amount") }
    }

    @PostMapping(path = ["/email/{id}"])
    suspend fun changeEmail(
        @PathVariable(value = "id", required = true, name = "id") id: String,
        @RequestBody request: ChangeEmailRequest
    ) = coroutineScope {
        val command = ChangeEmailCommand(id, request.email)
        bankAccountCommandService.handle(command)
        ResponseEntity.ok(id).also { log.info("(changeEmail) email changed id: $command.aggregateId, email: $command.email") }
    }


    @GetMapping(path = ["/account/{id}"])
    suspend fun getBankAccountById(@PathVariable(value = "id", required = true, name = "id") id: String): ResponseEntity<BankAccountResponse> {
        val response = bankAccountQueryService.handle(GetBankAccountByIdQuery(id, false))
        return ResponseEntity.status(HttpStatus.OK).body(response).also { log.info("(getBankAccountById) account loaded id: $id") }
    }
}