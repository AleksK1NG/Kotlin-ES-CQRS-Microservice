package com.example.microservice.lib.es

interface EventBus {
    suspend fun publish(events: List<Event>)
}