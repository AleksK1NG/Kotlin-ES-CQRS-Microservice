package com.example.microservice.events

import com.example.microservice.lib.es.BaseEvent
import java.math.BigDecimal

data class BalanceDepositedEvent(override var aggregateId: String, var balance: BigDecimal, var metaData: ByteArray?) :
    BaseEvent(aggregateId, metaData) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BalanceDepositedEvent

        if (aggregateId != other.aggregateId) return false
        if (balance != other.balance) return false
        if (metaData != null) {
            if (other.metaData == null) return false
            if (!metaData.contentEquals(other.metaData)) return false
        } else if (other.metaData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggregateId.hashCode()
        result = 31 * result + balance.hashCode()
        result = 31 * result + (metaData?.contentHashCode() ?: 0)
        return result
    }
}