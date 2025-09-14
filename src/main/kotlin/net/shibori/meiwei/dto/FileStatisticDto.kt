package net.shibori.meiwei.dto

data class FileStatisticDto(
    val filePath: String,
    val issueCount: Int,
    val maxSeverity: String
)