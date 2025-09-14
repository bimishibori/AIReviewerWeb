package net.shibori.meiwei.service

import net.shibori.meiwei.dto.*
import net.shibori.meiwei.entity.ReviewHistory
import net.shibori.meiwei.enums.ReviewStatus
import net.shibori.meiwei.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Service
@Transactional
class ReviewService(
    private val repositoryRepository: RepositoryRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val reviewResultRepository: ReviewResultRepository,
    private val reviewExclusionRepository: ReviewExclusionRepository,
    private val gitService: GitService,
    private val codeAnalysisService: CodeAnalysisService
) {

    // レビュー実行開始
    fun startReview(executionDto: ReviewExecutionDto): Long {
        val repository = repositoryRepository.findById(executionDto.repositoryId).orElseThrow {
            IllegalArgumentException("リポジトリが見つかりません: ${executionDto.repositoryId}")
        }

        // 実行中のレビューがないかチェック
        val runningReviews = reviewHistoryRepository.findByStatus(ReviewStatus.RUNNING)
        if (runningReviews.any { it.repository.id == executionDto.repositoryId }) {
            throw IllegalStateException("既に実行中のレビューがあります")
        }

        // レビュー履歴作成
        val reviewHistory = ReviewHistory(
            repository = repository,
            status = ReviewStatus.PENDING
        )

        val saved = reviewHistoryRepository.save(reviewHistory)

        // 非同期でレビュー実行
        CompletableFuture.runAsync {
            executeReviewAsync(saved.id!!, executionDto)
        }

        return saved.id!!
    }

    // レビュー進行状況取得
    @Transactional(readOnly = true)
    fun getReviewProgress(reviewHistoryId: Long): ReviewProgressDto {
        val reviewHistory = reviewHistoryRepository.findById(reviewHistoryId).orElseThrow {
            IllegalArgumentException("レビュー履歴が見つかりません: $reviewHistoryId")
        }

        val processedFiles = reviewHistory.reviewedFiles ?: 0
        val totalFiles = reviewHistory.totalFiles ?: 0
        val progress = if (totalFiles > 0) (processedFiles * 100 / totalFiles) else 0

        val elapsedTime = java.time.Duration.between(reviewHistory.startedAt, LocalDateTime.now()).toSeconds()
        val estimatedRemaining = if (processedFiles > 0 && totalFiles > processedFiles) {
            (elapsedTime * (totalFiles - processedFiles) / processedFiles)
        } else null

        return ReviewProgressDto(
            reviewHistoryId = reviewHistoryId,
            status = reviewHistory.status,
            progress = progress,
            currentFile = "処理中...", // 実際の実装では現在処理中のファイル名
            processedFiles = processedFiles,
            totalFiles = totalFiles,
            foundIssues = reviewHistory.totalIssues ?: 0,
            elapsedTime = elapsedTime,
            estimatedTimeRemaining = estimatedRemaining
        )
    }

    // レビュー結果取得
    @Transactional(readOnly = true)
    fun getReviewResults(reviewHistoryId: Long, pageable: Pageable): PageDto<ReviewResultDto> {
        val page = reviewResultRepository.findByReviewHistoryIdOrderByFilePathAscLineNumberAsc(reviewHistoryId, pageable)

        val content = page.content.map { result ->
            ReviewResultDto(
                id = result.id!!,
                filePath = result.filePath,
                lineNumber = result.lineNumber,
                columnNumber = result.columnNumber,
                severity = result.severity.name,
                ruleId = result.ruleId,
                message = result.message,
                suggestion = result.suggestion,
                codeSnippet = result.codeSnippet,
                createdAt = result.createdAt
            )
        }

        return PageDto(
            content = content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    // レビュー結果サマリー取得
    @Transactional(readOnly = true)
    fun getReviewSummary(reviewHistoryId: Long): ReviewResultSummaryDto {
        // 重要度別カウント
        val severityCount = reviewResultRepository.countBySeverity(reviewHistoryId)
            .associate { it[0].toString() to (it[1] as Long).toInt() }

        // ファイル別統計
        val fileStats = reviewResultRepository.getFileStatistics(reviewHistoryId)
            .map { FileStatisticDto(it[0] as String, (it[1] as Long).toInt(), it[2] as String) }

        // よく出る問題トップ10
        val topIssues = reviewResultRepository.getTopIssues(
            reviewHistoryRepository.findById(reviewHistoryId).get().repository.id!!,
            PageRequest.of(0, 10)
        ).content.map { TopIssueDto(it[0] as String, it[1] as String, (it[2] as Long).toInt()) }

        val totalIssues = severityCount.values.sum()

        return ReviewResultSummaryDto(
            reviewHistoryId = reviewHistoryId,
            totalIssues = totalIssues,
            severityCount = severityCount,
            fileCount = fileStats.size,
            topIssues = topIssues,
            fileStatistics = fileStats
        )
    }

    // レビュー履歴一覧取得
    @Transactional(readOnly = true)
    fun getReviewHistories(repositoryId: Long?, pageable: Pageable): PageDto<ReviewHistoryListDto> {
        val page = if (repositoryId != null) {
            reviewHistoryRepository.findByRepositoryIdOrderByStartedAtDesc(repositoryId, pageable)
        } else {
            reviewHistoryRepository.findAll(pageable)
        }

        val content = page.content.map { history ->
            ReviewHistoryListDto(
                id = history.id!!,
                repositoryName = history.repository.name,
                status = history.status,
                startedAt = history.startedAt,
                completedAt = history.completedAt,
                totalIssues = history.totalIssues,
                duration = history.completedAt?.let {
                    "${java.time.Duration.between(history.startedAt, it).toMinutes()}分"
                }
            )
        }

        return PageDto(
            content = content,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    // 非同期レビュー実行
    private fun executeReviewAsync(reviewHistoryId: Long, executionDto: ReviewExecutionDto) {
        try {
            // レビュー開始
            var reviewHistory = reviewHistoryRepository.findById(reviewHistoryId).get()
            reviewHistory = reviewHistory.copy(status = ReviewStatus.RUNNING)
            reviewHistoryRepository.save(reviewHistory)

            // Git操作
            val workDir = gitService.cloneOrUpdateRepository(
                reviewHistory.repository.cloneUrl,
                reviewHistory.repository.branchName,
                executionDto.forcePull
            )

            // コミットハッシュ取得
            val commitHash = gitService.getCurrentCommitHash(workDir)

            // ファイル一覧取得（除外設定適用）
            val files = gitService.getSourceFiles(workDir, reviewHistory.repository.id!!)

            // 進行状況更新
            reviewHistory = reviewHistory.copy(
                commitHash = commitHash,
                totalFiles = files.size
            )
            reviewHistoryRepository.save(reviewHistory)

            // コード解析実行
            val results = codeAnalysisService.analyzeFiles(files, reviewHistoryId)

            // レビュー完了
            reviewHistory = reviewHistory.copy(
                status = ReviewStatus.COMPLETED,
                completedAt = LocalDateTime.now(),
                reviewedFiles = files.size,
                totalIssues = results.size
            )
            reviewHistoryRepository.save(reviewHistory)

        } catch (e: Exception) {
            // エラー処理
            val reviewHistory = reviewHistoryRepository.findById(reviewHistoryId).get()
            val updated = reviewHistory.copy(
                status = ReviewStatus.FAILED,
                completedAt = LocalDateTime.now(),
                errorMessage = e.message
            )
            reviewHistoryRepository.save(updated)
        }
    }
}
