package com.example.microservice.commands

data class ChangeEmailCommand(var aggregateId: String, var email: String)