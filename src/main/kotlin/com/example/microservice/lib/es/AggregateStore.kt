package com.example.microservice.lib.es

import java.math.BigInteger

interface AggregateStore {
    suspend fun saveEvents(events: List<Event>)

    suspend fun loadEvents(aggregateId: String, version: BigInteger): MutableList<Event>

    suspend fun <T : AggregateRoot> save(aggregate: T)

    suspend fun <T : AggregateRoot> load(aggregateId: String, aggregateType: Class<T>): T
}