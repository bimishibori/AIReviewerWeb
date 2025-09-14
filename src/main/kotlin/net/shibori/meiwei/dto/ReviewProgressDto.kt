package net.shibori.meiwei.dto

import net.shibori.meiwei.enums.ReviewStatus

data class ReviewProgressDto(
    val reviewHistoryId: Long,
    val status: ReviewStatus,
    val progress: Int, // 0-100のパーセンテージ
    val currentFile: String?,
    val processedFiles: Int,
    val totalFiles: Int,
    val foundIssues: Int,
    val elapsedTime: Long,
    val estimatedTimeRemaining: Long?
)