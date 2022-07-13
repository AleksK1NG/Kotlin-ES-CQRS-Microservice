package com.example.microservice.configuration

import com.example.microservice.controllers.BankAccountController
import com.example.microservice.lib.es.Event
import com.example.microservice.lib.es.EventBus
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class KafkaEventBus {

    private val log = LoggerFactory.getLogger(BankAccountController::class.java)

    @Bean
    fun initEventBus(): EventBus {
        return object :EventBus {
            override suspend fun publish(events: List<Event>) {
                log.info("(EventBus) publish events: {}", events)
            }
        }
    }
}