package com.example.microservice.queries

import com.example.microservice.dto.BankAccountResponse

interface BankAccountQueryService {
    suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse
}