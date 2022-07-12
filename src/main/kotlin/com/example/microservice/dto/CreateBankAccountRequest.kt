package com.example.microservice.dto

import java.math.BigDecimal

data class CreateBankAccountRequest(var email: String, var balance: BigDecimal, var currency: String)
