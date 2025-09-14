package net.shibori.meiwei.dto

import jakarta.validation.constraints.*

// リポジトリ作成・更新用フォームDTO
data class RepositoryFormDto(
    @field:NotBlank(message = "リポジトリ名は必須です")
    @field:Size(max = 255, message = "リポジトリ名は255文字以内で入力してください")
    val name: String = "",

    @field:NotBlank(message = "クローンURLは必須です")
    @field:Size(max = 500, message = "クローンURLは500文字以内で入力してください")
    @field:Pattern(
        regexp = "^(https?://|git@|ssh://)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+$",
        message = "正しいGitのURLを入力してください"
    )
    val cloneUrl: String = "",

    @field:NotBlank(message = "ブランチ名は必須です")
    @field:Size(max = 100, message = "ブランチ名は100文字以内で入力してください")
    val branchName: String = "main",

    @field:Size(max = 1000, message = "説明は1000文字以内で入力してください")
    val description: String? = null
)