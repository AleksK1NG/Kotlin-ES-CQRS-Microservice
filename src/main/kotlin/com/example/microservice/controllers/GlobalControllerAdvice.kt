package com.example.microservice.controllers

import com.example.microservice.events.es.exceptions.AggregateNotFountException
import com.example.microservice.exceptions.ErrorHttpResponse
import com.example.microservice.exceptions.InvalidAmountException
import com.example.microservice.exceptions.InvalidEmailException
import com.example.microservice.lib.es.exceptions.SerializationException
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import reactor.util.Loggers
import java.time.LocalDateTime


@Order(2)
@ControllerAdvice
class GlobalControllerAdvice {

    companion object {
        private val log = Loggers.getLogger(GlobalControllerAdvice::class.java)
    }

    @ExceptionHandler(value = [RuntimeException::class])
    fun handleRuntimeException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorHttpResponse).also {
            log.error("(GlobalControllerAdvice) RuntimeException", ex)
        }
    }

    @ExceptionHandler(value = [AggregateNotFountException::class])
    fun handleAggregateNotFoundException(ex: AggregateNotFountException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.NOT_FOUND.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorHttpResponse).also { log.error("(GlobalControllerAdvice) NOT_FOUND", ex) }
    }

    @ExceptionHandler(value = [SerializationException::class, InvalidEmailException::class, InvalidAmountException::class])
    fun handleBadRequestException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.BAD_REQUEST.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorHttpResponse).also { log.error("(GlobalControllerAdvice) BAD_REQUEST", ex) }
    }
}