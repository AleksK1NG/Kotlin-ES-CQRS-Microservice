package com.example.microservice.queries

data class GetBankAccountByIdQuery(val aggregateId: String, val fromStore: Boolean = false)
