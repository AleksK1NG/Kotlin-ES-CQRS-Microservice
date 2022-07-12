package com.example.microservice.dto

import com.example.microservice.domain.BankAccountAggregate
import java.math.BigDecimal

data class BankAccountResponse(
    val id: String,
    val email: String,
    val balance: BigDecimal,
    val currency: String
) {
    companion object {
        public fun of(bankAccountAggregate: BankAccountAggregate): BankAccountResponse {
            return BankAccountResponse(
                bankAccountAggregate.aggregateId,
                bankAccountAggregate.email ?: "",
                bankAccountAggregate.balance,
                bankAccountAggregate.currency
            )
        }
    }
}
