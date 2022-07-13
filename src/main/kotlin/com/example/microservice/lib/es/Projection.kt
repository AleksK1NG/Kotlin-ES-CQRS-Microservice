package com.example.microservice.lib.es

interface Projection {
    fun whenEvent(event: Event)
}