package com.example.microservice.dto

import javax.validation.constraints.Email
import javax.validation.constraints.Size

data class ChangeEmailRequest(@get:Email(message = "invalid email") @get:Size(min = 6, max = 60) var email: String)
