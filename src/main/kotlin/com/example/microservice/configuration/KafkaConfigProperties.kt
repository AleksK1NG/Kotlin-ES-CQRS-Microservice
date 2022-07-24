package com.example.microservice.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "kafka")
class KafkaConfigProperties {
    val acks = "0"
    val retries = 3
    val deliveryTimeoutMs = 120000
    val maxRequestSize = 1068576
    val requestTimeoutMs = 30000
    val orderMongoProjectionGroupId = "projection-group"
    val enableAutoCommit = "false"
}