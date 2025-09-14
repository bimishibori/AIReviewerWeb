package net.shibori.meiwei.dto

data class ReviewResultSummaryDto(
    val reviewHistoryId: Long,
    val totalIssues: Int,
    val severityCount: Map<String, Int>,
    val fileCount: Int,
    val topIssues: List<TopIssueDto>,
    val fileStatistics: List<FileStatisticDto>
)
