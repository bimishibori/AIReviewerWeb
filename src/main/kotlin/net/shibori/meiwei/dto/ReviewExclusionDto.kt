package net.shibori.meiwei.dto

import java.time.LocalDateTime

data class ReviewExclusionDto(
    val id: Long,
    val exclusionType: String,
    val path: String,
    val pattern: String?,
    val description: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)