package com.example.microservice.controllers

import com.example.microservice.events.es.exceptions.AggregateNotFountException
import com.example.microservice.exceptions.ErrorHttpResponse
import com.example.microservice.exceptions.InvalidAmountException
import com.example.microservice.exceptions.InvalidEmailException
import com.example.microservice.lib.es.exceptions.SerializationException
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import reactor.util.Loggers
import java.time.LocalDateTime


@Order(2)
@ControllerAdvice
class GlobalControllerAdvice {

    @ExceptionHandler(value = [RuntimeException::class])
    fun handleRuntimeException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(errorHttpResponse).also {
            log.error("(GlobalControllerAdvice) RuntimeException", ex)
        }
    }

    @ExceptionHandler(value = [AggregateNotFountException::class])
    fun handleAggregateNotFoundException(ex: AggregateNotFountException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.NOT_FOUND.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) AggregateNotFountException NOT_FOUND", ex) }
    }

    @ExceptionHandler(value = [IllegalArgumentException::class])
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.BAD_REQUEST.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) AggregateNotFountException NOT_FOUND", ex) }
    }

    @ExceptionHandler(value = [SerializationException::class, InvalidEmailException::class, InvalidAmountException::class, ServerWebInputException::class])
    fun handleBadRequestException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.BAD_REQUEST.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) BAD_REQUEST", ex) }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = [MethodArgumentNotValidException::class])
    fun handleInvalidArgument(ex: MethodArgumentNotValidException): ResponseEntity<MutableMap<String, String>> {
        val errorMap: MutableMap<String, String> = HashMap()
        ex.bindingResult.fieldErrors.forEach { error -> error.defaultMessage?.let { errorMap[error.field] = it } }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorMap)
            .also { log.error("(GlobalControllerAdvice) WebExchangeBindException BAD_REQUEST", ex) }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = [WebExchangeBindException::class])
    fun handleWebExchangeInvalidArgument(ex: WebExchangeBindException): ResponseEntity<MutableMap<String, Any>> {
        val errorMap = mutableMapOf<String, Any>()
        ex.bindingResult.fieldErrors.forEach { error ->
            error.defaultMessage?.let {
                errorMap[error.field] = mapOf(
                    "reason" to it,
                    "rejectedValue" to error.rejectedValue,
                )
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorMap)
            .also { log.error("(GlobalControllerAdvice) WebExchangeBindException BAD_REQUEST", ex) }
    }

    companion object {
        private val log = Loggers.getLogger(GlobalControllerAdvice::class.java)
    }
}