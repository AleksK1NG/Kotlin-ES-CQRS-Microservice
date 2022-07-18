package com.example.microservice.lib.es

import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

class Snapshot(
    var id: UUID,
    var aggregateId: String,
    var aggregateType: String,
    var data: ByteArray,
    var metaData: ByteArray = ByteArray(0),
    var version: BigInteger = BigInteger.ZERO,
    var timeStamp: LocalDateTime
) {


    override fun toString(): String {
        return "Snapshot(id=$id, aggregateId='$aggregateId', aggregateType='$aggregateType', data=${data.contentToString()}, metaData=${metaData.contentToString()}, version=$version, timeStamp=$timeStamp)"
    }
}