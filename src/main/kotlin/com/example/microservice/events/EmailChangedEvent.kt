package com.example.microservice.events

import com.example.microservice.lib.es.BaseEvent

data class EmailChangedEvent(override var aggregateId: String, var email: String, var metaData: ByteArray?) :
    BaseEvent(aggregateId, metaData) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmailChangedEvent

        if (aggregateId != other.aggregateId) return false
        if (email != other.email) return false
        if (metaData != null) {
            if (other.metaData == null) return false
            if (!metaData.contentEquals(other.metaData)) return false
        } else if (other.metaData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggregateId.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + (metaData?.contentHashCode() ?: 0)
        return result
    }
}
