package net.shibori.meiwei.dto

import net.shibori.meiwei.enums.ReviewStatus
import java.time.LocalDateTime

data class RepositoryListDto(
    val id: Long,
    val name: String,
    val cloneUrl: String,
    val branchName: String,
    val isActive: Boolean,
    val lastReviewStatus: ReviewStatus?,
    val lastReviewDate: LocalDateTime?,
    val totalIssues: Int?
)