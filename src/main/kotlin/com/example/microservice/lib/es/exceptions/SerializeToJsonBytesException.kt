package com.example.microservice.lib.es.exceptions

class SerializeToJsonBytesException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
}