package com.example.microservice.events

import com.example.microservice.lib.es.BaseEvent
import java.math.BigDecimal

data class BankAccountCreatedEvent(
    override var aggregateId: String,
    var email: String?,
    var balance: BigDecimal?,
    var currency: String?,
    var metaData: ByteArray?
) : BaseEvent(aggregateId, metaData) {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BankAccountCreatedEvent

        if (aggregateId != other.aggregateId) return false
        if (email != other.email) return false
        if (balance != other.balance) return false
        if (currency != other.currency) return false
        if (metaData != null) {
            if (other.metaData == null) return false
            if (!metaData.contentEquals(other.metaData)) return false
        } else if (other.metaData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggregateId.hashCode()
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (balance?.hashCode() ?: 0)
        result = 31 * result + (currency?.hashCode() ?: 0)
        result = 31 * result + (metaData?.contentHashCode() ?: 0)
        return result
    }
}
