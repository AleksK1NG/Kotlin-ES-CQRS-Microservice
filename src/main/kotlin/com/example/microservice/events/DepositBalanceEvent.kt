package com.example.microservice.events

import com.example.microservice.lib.es.BaseEvent
import java.math.BigDecimal

class DepositBalanceEvent(override var aggregateId: String, var balance: BigDecimal): BaseEvent(aggregateId) {
}