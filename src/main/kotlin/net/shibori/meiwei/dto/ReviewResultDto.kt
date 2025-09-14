package net.shibori.meiwei.dto

import java.time.LocalDateTime

data class ReviewResultDto(
    val id: Long,
    val filePath: String,
    val lineNumber: Int?,
    val columnNumber: Int?,
    val severity: String,
    val ruleId: String?,
    val message: String,
    val suggestion: String?,
    val codeSnippet: String?,
    val createdAt: LocalDateTime
)