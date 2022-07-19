package com.example.microservice.events

import com.example.microservice.domain.Currency
import com.example.microservice.lib.es.BaseEvent
import java.math.BigDecimal

data class BankAccountCreatedEvent(
    override var aggregateId: String,
    var email: String,
    var balance: BigDecimal,
    var currency: Currency,
    var metaData: ByteArray
) : BaseEvent(aggregateId, metaData)
