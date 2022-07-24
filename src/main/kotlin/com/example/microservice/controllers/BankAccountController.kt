package com.example.microservice.controllers

import com.example.microservice.commands.BankAccountCommandService
import com.example.microservice.commands.ChangeEmailCommand
import com.example.microservice.commands.CreateBankAccountCommand
import com.example.microservice.commands.DepositBalanceCommand
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.dto.*
import com.example.microservice.queries.BankAccountQueryService
import com.example.microservice.queries.GetAllQuery
import com.example.microservice.queries.GetBankAccountByIdQuery
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.util.Loggers
import java.util.*
import javax.validation.Valid


@RestController
@RequestMapping(path = ["/api/v1/bank"])
@Tag(name = "Bank Accounts", description = "Bank Accounts REST API Group")
class BankAccountController(
    private val bankAccountCommandService: BankAccountCommandService,
    private val bankAccountQueryService: BankAccountQueryService,
) {

    @PostMapping(path = ["/account"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun createBankAccount(@Valid @RequestBody request: CreateBankAccountRequest): ResponseEntity<String> {
        val command = CreateBankAccountCommand(UUID.randomUUID().toString(), request.email, request.balance, request.currency)
        bankAccountCommandService.handle(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(command.aggregateId).also { log.info("(createBankAccount) created id: $command.aggregateId") }
    }

    @PostMapping(path = ["/deposit/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun depositBalance(
        @PathVariable(value = "id", required = true, name = "id") id: String,
        @Valid @RequestBody request: DepositBalanceRequest
    ): ResponseEntity.BodyBuilder {
        val command = DepositBalanceCommand(id, request.amount)
        bankAccountCommandService.handle(command)
        return ResponseEntity.ok().also { log.info("(depositBalance) deposited id: $command.aggregateId, amount: $command.amount") }
    }

    @PostMapping(path = ["/email/{id}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun changeEmail(
        @PathVariable(value = "id", required = true, name = "id") id: String,
        @Valid @RequestBody request: ChangeEmailRequest
    ): ResponseEntity.BodyBuilder {
        val command = ChangeEmailCommand(id, request.email)
        bankAccountCommandService.handle(command)
        return ResponseEntity.ok().also { log.info("(changeEmail) email changed id: $command.aggregateId, email: $command.email") }
    }


    @GetMapping(path = ["/account/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getBankAccountById(
        @PathVariable(value = "id", required = true, name = "id") id: String,
        @RequestParam(name = "store", required = false, defaultValue = "false") fromStore: Boolean
    ): ResponseEntity<BankAccountResponse> {
        return bankAccountQueryService.handle(GetBankAccountByIdQuery(id, fromStore))
            .let { ResponseEntity.ok(it) }.also { log.info("(getBankAccountById) account loaded id: $id") }
    }

    @GetMapping(path = ["/account"], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getAllWithPagination(
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "10") size: Int
    ): ResponseEntity<PaginationResponse<BankAccountDocument>> {
        return bankAccountQueryService.handle(GetAllQuery(PageRequest.of(page, size)))
            .let { ResponseEntity.ok(it) }.also { log.info("(getAllWithPagination) accounts: ${it.body}") }
    }

    companion object {
        private val log = Loggers.getLogger(BankAccountController::class.java)
    }
}