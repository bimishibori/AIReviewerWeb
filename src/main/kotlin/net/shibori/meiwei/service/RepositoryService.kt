package net.shibori.meiwei.service

import net.shibori.meiwei.dto.*
import net.shibori.meiwei.entity.Repository
import net.shibori.meiwei.repository.RepositoryRepository
import net.shibori.meiwei.repository.ReviewHistoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class RepositoryService(
    private val repositoryRepository: RepositoryRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository
) {

    // リポジトリ作成
    fun createRepository(formDto: RepositoryFormDto): RepositoryDto {
        // 重複チェック
        repositoryRepository.findByCloneUrlAndIsActiveTrue(formDto.cloneUrl)?.let {
            throw IllegalArgumentException("同じクローンURLのリポジトリが既に存在します")
        }

        repositoryRepository.findByNameAndIsActiveTrue(formDto.name)?.let {
            throw IllegalArgumentException("同じ名前のリポジトリが既に存在します")
        }

        val repository = Repository(
            name = formDto.name,
            cloneUrl = formDto.cloneUrl,
            branchName = formDto.branchName,
            description = formDto.description
        )

        val saved = repositoryRepository.save(repository)
        return convertToDto(saved)
    }

    // リポジトリ更新
    fun updateRepository(id: Long, formDto: RepositoryFormDto): RepositoryDto {
        val repository = repositoryRepository.findById(id).orElseThrow {
            IllegalArgumentException("リポジトリが見つかりません: $id")
        }

        // 名前の重複チェック（自分以外）
        repositoryRepository.findByNameAndIsActiveTrue(formDto.name)?.let {
            if (it.id != id) {
                throw IllegalArgumentException("同じ名前のリポジトリが既に存在します")
            }
        }

        val updated = repository.copy(
            name = formDto.name,
            cloneUrl = formDto.cloneUrl,
            branchName = formDto.branchName,
            description = formDto.description,
            updatedAt = LocalDateTime.now()
        )

        val saved = repositoryRepository.save(updated)
        return convertToDto(saved)
    }

    // リポジトリ取得（詳細）
    @Transactional(readOnly = true)
    fun getRepository(id: Long): RepositoryDto {
        val repository = repositoryRepository.findById(id).orElseThrow {
            IllegalArgumentException("リポジトリが見つかりません: $id")
        }

        // 最新のレビュー履歴を取得
        val latestReview = reviewHistoryRepository.findTopByRepositoryIdOrderByStartedAtDesc(id)

        return convertToDto(repository, latestReview?.let { convertToDto(it) })
    }

    // アクティブなリポジトリ一覧取得
    @Transactional(readOnly = true)
    fun getActiveRepositories(): List<RepositoryListDto> {
        return repositoryRepository.findByIsActiveTrue().map { repository ->
            val latestReview = reviewHistoryRepository.findTopByRepositoryIdOrderByStartedAtDesc(repository.id!!)
            convertToListDto(repository, latestReview)
        }
    }

    // リポジトリ検索
    @Transactional(readOnly = true)
    fun searchRepositories(keyword: String): List<RepositoryListDto> {
        return repositoryRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword).map { repository ->
            val latestReview = reviewHistoryRepository.findTopByRepositoryIdOrderByStartedAtDesc(repository.id!!)
            convertToListDto(repository, latestReview)
        }
    }

    // リポジトリ論理削除
    fun deactivateRepository(id: Long) {
        val repository = repositoryRepository.findById(id).orElseThrow {
            IllegalArgumentException("リポジトリが見つかりません: $id")
        }

        val deactivated = repository.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )

        repositoryRepository.save(deactivated)
    }

    // Entity -> DTO変換
    private fun convertToDto(repository: Repository, latestReview: ReviewHistoryDto? = null): RepositoryDto {
        return RepositoryDto(
            id = repository.id!!,
            name = repository.name,
            cloneUrl = repository.cloneUrl,
            branchName = repository.branchName,
            description = repository.description,
            isActive = repository.isActive,
            createdAt = repository.createdAt,
            updatedAt = repository.updatedAt,
            exclusionCount = repository.exclusions.size,
            latestReview = latestReview
        )
    }

    private fun convertToListDto(repository: Repository, latestReview: net.shibori.meiwei.entity.ReviewHistory?): RepositoryListDto {
        return RepositoryListDto(
            id = repository.id!!,
            name = repository.name,
            cloneUrl = repository.cloneUrl,
            branchName = repository.branchName,
            isActive = repository.isActive,
            lastReviewStatus = latestReview?.status,
            lastReviewDate = latestReview?.startedAt,
            totalIssues = latestReview?.totalIssues
        )
    }

    private fun convertToDto(reviewHistory: net.shibori.meiwei.entity.ReviewHistory): ReviewHistoryDto {
        return ReviewHistoryDto(
            id = reviewHistory.id!!,
            repositoryId = reviewHistory.repository.id!!,
            repositoryName = reviewHistory.repository.name,
            commitHash = reviewHistory.commitHash,
            status = reviewHistory.status,
            startedAt = reviewHistory.startedAt,
            completedAt = reviewHistory.completedAt,
            totalFiles = reviewHistory.totalFiles,
            reviewedFiles = reviewHistory.reviewedFiles,
            totalIssues = reviewHistory.totalIssues,
            errorMessage = reviewHistory.errorMessage
        )
    }
}