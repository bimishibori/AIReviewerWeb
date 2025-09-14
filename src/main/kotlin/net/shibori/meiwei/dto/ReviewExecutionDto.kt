package net.shibori.meiwei.dto

data class ReviewExecutionDto(
    val repositoryId: Long,
    val forcePull: Boolean = false,
    val excludePatterns: List<String> = emptyList()
)