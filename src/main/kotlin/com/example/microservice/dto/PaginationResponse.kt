package com.example.microservice.dto

import org.springframework.data.domain.PageRequest

data class PaginationResponse<T>(
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val totalPages: Int,
    val hasMore: Boolean,
    val list: List<T>
) {

    companion object {
        fun <T> of(pageRequest: PageRequest, totalCount: Long, list: List<T>): PaginationResponse<T> {
            val totalPages = (totalCount.toInt() / pageRequest.pageSize)
            val hasMore = pageRequest.pageNumber < totalPages
            return PaginationResponse(pageRequest.pageNumber, pageRequest.pageSize, totalCount, totalPages, hasMore, list)
        }
    }

    override fun toString(): String {
        return "PaginationResponse(page=$page, size=$size, totalCount=$totalCount, totalPages=$totalPages, hasMore=$hasMore, listSize=${list.size})"
    }
}