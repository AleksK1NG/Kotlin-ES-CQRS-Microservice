package com.example.microservice.commands

import java.math.BigDecimal

data class CreateBankAccountCommand(
    var aggregateId: String,
    var email: String,
    var balance: BigDecimal,
    var currency: String
)
