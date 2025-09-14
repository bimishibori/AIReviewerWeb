package net.shibori.meiwei.dto

data class ReviewResultFileDto(
    val filePath: String,
    val totalIssues: Int,
    val maxSeverity: String,
    val issues: List<ReviewResultDto>
)