package com.example.microservice.lib.es

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

class Event(val type: String, var data: ByteArray, var aggregateId: String, var aggregateType: String) {
    var id: String? = null
    var version: BigInteger = BigInteger.ZERO
    var metaData: ByteArray = byteArrayOf()
    var timeStamp: LocalDateTime = LocalDateTime.now()

    constructor(aggregate: AggregateRoot, eventType: String, data: ByteArray, metaData: ByteArray) : this(
        eventType,
        data,
        aggregate.aggregateId,
        aggregate.aggregateType
    ) {
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
        metaData: ByteArray,
        timeStamp: LocalDateTime
    ) : this(type, data, aggregateId, aggregateType) {
        this.id = id
        this.version = version
        this.aggregateId = aggregateId
        this.metaData = metaData
        this.timeStamp = timeStamp
        this.aggregateType = aggregateType
    }

    override fun toString(): String {
        return "Event(type='$type', data=${String(data)}, aggregateId='$aggregateId', aggregateType='$aggregateType', id=$id, version=$version, metaData=${String(metaData)}, timeStamp=$timeStamp)"
    }
}
