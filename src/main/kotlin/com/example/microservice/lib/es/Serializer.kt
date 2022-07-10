package com.example.microservice.lib.es

interface Serializer {
    fun serialize(event: Any, aggregate: AggregateRoot, metaData: Any?): Event

    fun deserialize(event: Event): Any
}