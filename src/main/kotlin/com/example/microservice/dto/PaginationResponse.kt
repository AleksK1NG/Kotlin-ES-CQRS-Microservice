package com.example.microservice.dto

data class PaginationResponse<T>(
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val totalPages: Int,
    val hasMore: Boolean,
    val list: List<T>
) {

    override fun toString(): String {
        return "PaginationResponse(page=$page, size=$size, totalCount=$totalCount, totalPages=$totalPages, hasMore=$hasMore, listSize=${list.size})"
    }
}