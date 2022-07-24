package com.example.microservice.lib.es

abstract class BaseEvent(open val aggregateId: String, var metadata: ByteArray = byteArrayOf())