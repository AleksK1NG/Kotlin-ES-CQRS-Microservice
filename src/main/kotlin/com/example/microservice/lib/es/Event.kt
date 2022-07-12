package com.example.microservice.lib.es

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

class Event(val type: String, var data: ByteArray) {
    var id: String? = null
    var version: BigInteger = BigInteger.ZERO
    var aggregateId: String? = null
    var aggregateType: String? = null
    var metaData: ByteArray? = null
    var timeStamp: LocalDateTime? = null

    constructor(aggregate: AggregateRoot, eventType: String, data: ByteArray, metaData: ByteArray?) : this(eventType, data) {
        this.id = UUID.randomUUID().toString()
        this.aggregateId = aggregate.aggregateId
        this.aggregateType = aggregate.aggregateType
        this.version = aggregate.version
        this.metaData = metaData
        this.timeStamp = LocalDateTime.now()
    }

    constructor(
        type: String,
        aggregateType: String,
        id: String,
        version: BigInteger,
        aggregateId: String,
        data: ByteArray,
        metaData: ByteArray? = null,
        timeStamp: LocalDateTime
    ) : this(type, data) {
        this.id = id
        this.version = version
        this.aggregateId = aggregateId
//        this.data = data
        this.metaData = metaData
        this.timeStamp = timeStamp
        this.aggregateType = aggregateType
    }
}
