package com.example.microservice.configuration

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration


@OpenAPIDefinition(
    info = Info(
        title = "Kotlin Spring CQRS and Event Sourcing Microservice",
        description = "Kotlin Spring Postgresql MongoDB Kafka CQRS and Event Sourcing Microservice",
        contact = Contact(name = "Alexander Bryksin", email = "alexander.bryksin@yandex.ru", url = "https://github.com/AleksK1NG")
    )
)
@Configuration
open class SwaggerOpenAPIConfigurationSwaggerOpenAPIConfiguration