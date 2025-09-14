package net.shibori.meiwei.dto

import net.shibori.meiwei.enums.ReviewStatus
import java.time.LocalDateTime

data class ReviewHistoryListDto(
    val id: Long,
    val repositoryName: String,
    val status: ReviewStatus,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val totalIssues: Int?,
    val duration: String?
)