package com.example.microservice.events

import com.example.microservice.lib.es.BaseEvent
import java.math.BigDecimal

data class CreateBankAccountEvent(
    override var aggregateId: String,
    var email: String?,
    var balance: BigDecimal?,
    var currency: String?
) : BaseEvent(aggregateId)
