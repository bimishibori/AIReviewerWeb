package net.shibori.meiwei.dto

import jakarta.validation.constraints.*

data class ReviewExclusionFormDto(
    val repositoryId: Long,

    @field:NotNull(message = "除外タイプは必須です")
    val exclusionType: String = "FILE",

    @field:NotBlank(message = "パスは必須です")
    @field:Size(max = 500, message = "パスは500文字以内で入力してください")
    val path: String = "",

    @field:Size(max = 200, message = "パターンは200文字以内で入力してください")
    val pattern: String? = null,

    @field:Size(max = 500, message = "説明は500文字以内で入力してください")
    val description: String? = null
)