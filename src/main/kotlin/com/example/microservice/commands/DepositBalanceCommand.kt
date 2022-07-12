package com.example.microservice.commands

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

data class DepositBalanceCommand(@JsonInclude(JsonInclude.Include.ALWAYS) var aggregateId: String, var amount: BigDecimal)
