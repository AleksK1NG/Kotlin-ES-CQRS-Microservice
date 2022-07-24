package com.example.microservice.configuration

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff


@Configuration
class KafkaConsumerConfig(
    private val kafkaConfigProperties: KafkaConfigProperties,
    @Value(value = "\${spring.kafka.bootstrap-servers:localhost:9093}")
    private val bootstrapServers: String
) {


    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, ByteArray>): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray>()
        factory.consumerFactory = consumerFactory
        factory.setConcurrency(Runtime.getRuntime().availableProcessors())
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setCommonErrorHandler(DefaultErrorHandler(FixedBackOff(fixedBackOffInterval, maxAttempts)))
        return factory
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, ByteArray> {
        return DefaultKafkaConsumerFactory(consumerProps())
    }

    private fun consumerProps(): Map<String, Any> {
        val consumerProps: MutableMap<String, Any> = HashMap()
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = kafkaConfigProperties.orderMongoProjectionGroupId
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        consumerProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = kafkaConfigProperties.enableAutoCommit
        return consumerProps
    }

    companion object {
        private const val fixedBackOffInterval = 1000L
        private const val maxAttempts = 5L
    }
}