package com.example.microservice.dto

import com.example.microservice.domain.BankAccountAggregate
import com.example.microservice.domain.BankAccountDocument
import com.example.microservice.domain.Currency
import java.math.BigDecimal

data class BankAccountResponse(
    val id: String,
    val email: String,
    val balance: BigDecimal,
    val currency: Currency
) {
    companion object {
        fun of(bankAccountAggregate: BankAccountAggregate): BankAccountResponse {
            return BankAccountResponse(
                bankAccountAggregate.aggregateId,
                bankAccountAggregate.email ?: "",
                bankAccountAggregate.balance,
                bankAccountAggregate.currency
            )
        }

        fun of(bankAccountDocument: BankAccountDocument): BankAccountResponse {
            return BankAccountResponse(
                bankAccountDocument.aggregateId ?: "",
                bankAccountDocument.email ?: "",
                bankAccountDocument.balance ?: BigDecimal.ZERO,
                bankAccountDocument.currency ?: Currency.USD
            )
        }
    }
}
