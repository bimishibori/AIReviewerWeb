package net.shibori.meiwei.dto

import java.time.LocalDateTime

data class RepositoryDto(
    val id: Long,
    val name: String,
    val cloneUrl: String,
    val branchName: String,
    val description: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val exclusionCount: Int = 0,
    val latestReview: ReviewHistoryDto? = null
)
