package com.example.microservice.dto

import java.math.BigDecimal
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotNull

data class DepositBalanceRequest(@get:DecimalMin(value = "0.00", inclusive = false, message = "invalid amount") @NotNull val amount: BigDecimal)
