package com.example.microservice.queries

import org.springframework.data.domain.PageRequest

data class GetAllQuery(val pageRequest: PageRequest)
