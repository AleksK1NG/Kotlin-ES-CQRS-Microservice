package com.example.microservice.queries

import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.dto.BankAccountResponse
import com.example.microservice.dto.PaginationResponse

interface BankAccountQueryService {
    suspend fun handle(query: GetBankAccountByIdQuery): BankAccountResponse
    suspend fun handle(query: GetAllQuery): PaginationResponse<BankAccountDocument>
}