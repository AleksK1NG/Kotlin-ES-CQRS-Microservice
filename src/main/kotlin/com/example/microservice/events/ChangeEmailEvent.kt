package com.example.microservice.events

import com.example.microservice.lib.es.BaseEvent

data class ChangeEmailEvent(override var aggregateId: String, var email: String) : BaseEvent(aggregateId)
