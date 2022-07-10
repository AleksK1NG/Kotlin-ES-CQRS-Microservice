package com.example.microservice.lib.es

import java.math.BigInteger

abstract class AggregateRoot(
    open val aggregateId: String,
    open val aggregateType: String
) {
    val changes: MutableList<Any> = mutableListOf()
    var version: BigInteger = BigInteger.ZERO

    protected abstract fun whenEvent(event: Any)

    public fun apply(event: Any) {
        changes.add(event)
        whenEvent(event)
        version++
    }

    public fun raiseEvent(event: Any) {
        whenEvent(event)
        version++
    }

    public fun load(events: MutableList<Any>) = events.forEach { raiseEvent(it) }

    public fun clearChanges() = changes.clear()
    override fun toString(): String {
        return "AggregateRoot(aggregateId='$aggregateId', aggregateType='$aggregateType', changes=$changes, version=$version)"
    }
}