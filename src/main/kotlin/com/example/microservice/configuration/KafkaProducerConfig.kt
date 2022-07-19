package com.example.microservice.configuration

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory



@Configuration
class KafkaProducerConfig(
    @Value(value = "\${spring.kafka.bootstrap-servers:localhost:9093}")
    private val bootstrapServers: String,
    private val kafkaConfigProperties: KafkaConfigProperties
) {

    private fun senderProps(): Map<String, Any> {
        val producerProps: MutableMap<String, Any> = HashMap()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
        producerProps[ProducerConfig.ACKS_CONFIG] = kafkaConfigProperties.acks
        producerProps[ProducerConfig.RETRIES_CONFIG] = kafkaConfigProperties.retries
        producerProps[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = kafkaConfigProperties.deliveryTimeoutMs
        producerProps[ProducerConfig.MAX_REQUEST_SIZE_CONFIG] = kafkaConfigProperties.maxRequestSize
        producerProps[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = kafkaConfigProperties.requestTimeoutMs
        return producerProps
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, ByteArray> {
        return DefaultKafkaProducerFactory(senderProps())
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, ByteArray>): KafkaTemplate<String, ByteArray> {
        return KafkaTemplate(producerFactory)
    }
}