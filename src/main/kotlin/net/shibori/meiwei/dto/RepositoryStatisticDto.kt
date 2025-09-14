package net.shibori.meiwei.dto

import java.time.LocalDateTime

data class RepositoryStatisticDto(
    val repositoryId: Long,
    val repositoryName: String,
    val totalReviews: Int,
    val completedReviews: Int,
    val failedReviews: Int,
    val averageIssues: Double,
    val lastReviewDate: LocalDateTime?
)