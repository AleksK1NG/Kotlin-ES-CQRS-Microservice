package com.example.microservice.dto

import java.math.BigDecimal
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Email
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class CreateBankAccountRequest(
    @get:Email(message = "invalid email") @get:Size(min = 6, max = 60) var email: String,
    @get:DecimalMin(value = "0.0", message = "invalid balance amount") @NotNull var balance: BigDecimal,
    @get:Size(min = 3, max = 3) var currency: String
)
