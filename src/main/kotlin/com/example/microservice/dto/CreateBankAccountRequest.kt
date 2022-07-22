package com.example.microservice.dto

import com.example.microservice.domain.Currency
import java.math.BigDecimal
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Email
import javax.validation.constraints.Size

data class CreateBankAccountRequest(
    @get:Email(message = "invalid email") @get:Size(min = 6, max = 60) var email: String,
    @get:DecimalMin(value = "0.0", message = "invalid balance amount") var balance: BigDecimal,
    var currency: Currency
)
