package com.example.microservice.commands

import java.math.BigDecimal

data class DepositBalanceCommand(var aggregateId: String, var amount: BigDecimal)
