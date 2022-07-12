package com.example.microservice.commands

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

data class CreateBankAccountCommand(

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var aggregateId: String,
    var email: String,
    var balance: BigDecimal,
    var currency: String
)
