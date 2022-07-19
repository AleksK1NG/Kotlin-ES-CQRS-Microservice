package com.example.microservice.configuration

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin
import reactor.util.Loggers


@Configuration
class KafkaTopicConfiguration(
    @Value(value = "\${spring.kafka.bootstrap-servers:localhost:9093}")
    private val bootstrapServers: String,
    @Value(value = "\${microservice.kafka.topics.bank-account-event-store:bank-account-event-store}")
    private val bankAccountTopicName: String
) {

    companion object {
        private val log = Loggers.getLogger(KafkaTopicConfiguration::class.java)
    }

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs: MutableMap<String, Any> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        return KafkaAdmin(configs)
    }

    @Bean
    fun bankAccountEventStoreTopicInitializer(kafkaAdmin: KafkaAdmin): NewTopic? {
        return try {
            val topic = NewTopic(bankAccountTopicName, 3, 1.toShort())
            kafkaAdmin.createOrModifyTopics(topic)
            log.info("(bankAccountEventStoreTopicInitializer) topic: $topic")
            topic
        } catch (e: Exception) {
            log.error("bankAccountEventStoreTopicInitializer", e)
            null
        }
    }


}