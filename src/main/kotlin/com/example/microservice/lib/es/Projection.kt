package com.example.microservice.lib.es

interface Projection {
    suspend fun whenEvent(event: Event)
}