package com.example.microservice.commands

interface BankAccountCommandService {
    suspend fun handle(command: CreateBankAccountCommand)
    suspend fun handle(command: DepositBalanceCommand)
    suspend fun handle(command: ChangeEmailCommand)
}