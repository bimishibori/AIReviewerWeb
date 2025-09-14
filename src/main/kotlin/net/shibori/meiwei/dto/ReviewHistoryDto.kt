package net.shibori.meiwei.dto

import net.shibori.meiwei.enums.ReviewStatus
import java.time.LocalDateTime

data class ReviewHistoryDto(
    val id: Long,
    val repositoryId: Long,
    val repositoryName: String,
    val commitHash: String?,
    val status: ReviewStatus,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val totalFiles: Int?,
    val reviewedFiles: Int?,
    val totalIssues: Int?,
    val errorMessage: String?,
    val duration: Long? = completedAt?.let {
        java.time.Duration.between(startedAt, it).toMinutes()
    }
)