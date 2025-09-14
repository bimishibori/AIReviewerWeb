package net.shibori.meiwei.service

import net.shibori.meiwei.entity.ReviewHistory
import net.shibori.meiwei.entity.ReviewResult
import net.shibori.meiwei.repository.ReviewHistoryRepository
import net.shibori.meiwei.repository.ReviewResultRepository
import net.shibori.meiwei.service.analyzer.CSharpAnalyzer
import net.shibori.meiwei.service.analyzer.UnityAnalyzer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime

@Service
class CodeAnalysisService(
    private val reviewResultRepository: ReviewResultRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val csharpAnalyzer: CSharpAnalyzer,
    private val unityAnalyzer: UnityAnalyzer
) {

    private val logger = LoggerFactory.getLogger(CodeAnalysisService::class.java)

    fun analyzeFiles(files: List<File>, reviewHistoryId: Long): List<ReviewResult> {
        logger.info("コード解析開始: ${files.size}ファイル")

        val reviewHistory = reviewHistoryRepository.findById(reviewHistoryId)
            .orElseThrow { IllegalArgumentException("レビュー履歴が見つかりません: $reviewHistoryId") }

        val allResults = mutableListOf<ReviewResult>()
        var processedCount = 0

        files.forEach { file ->
            try {
                val results = analyzeFile(file, reviewHistory)
                allResults.addAll(results)

                // 進行状況更新
                processedCount++
                updateProgress(reviewHistoryId, processedCount, files.size, allResults.size)

            } catch (e: Exception) {
                logger.error("ファイル解析エラー: ${file.absolutePath}", e)
            }
        }

        logger.info("コード解析完了: ${allResults.size}件の問題を検出")
        return allResults
    }

    private fun analyzeFile(file: File, reviewHistory: ReviewHistory): List<ReviewResult> {
        val relativePath = getRelativePath(file)
        val content = file.readText(Charsets.UTF_8)
        val lines = content.lines()

        val results = mutableListOf<ReviewResult>()

        // C#解析
        val csharpResults = csharpAnalyzer.analyze(content, lines)
        results.addAll(csharpResults.map { result ->
            createReviewResult(result, relativePath, reviewHistory)
        })

        // Unity解析
        val unityResults = unityAnalyzer.analyze(content, lines)
        results.addAll(unityResults.map { result ->
            createReviewResult(result, relativePath, reviewHistory)
        })

        // 結果を保存
        results.forEach { result ->
            reviewResultRepository.save(result)
        }

        return results
    }

    private fun createReviewResult(
        analysisResult: net.shibori.meiwei.common.AnalysisResult,
        filePath: String,
        reviewHistory: ReviewHistory
    ): ReviewResult {
        return ReviewResult(
            reviewHistory = reviewHistory,
            filePath = filePath,
            lineNumber = analysisResult.lineNumber,
            columnNumber = analysisResult.columnNumber,
            severity = analysisResult.severity,
            ruleId = analysisResult.ruleId,
            message = analysisResult.message,
            suggestion = analysisResult.suggestion,
            codeSnippet = analysisResult.codeSnippet,
            createdAt = LocalDateTime.now()
        )
    }

    private fun getRelativePath(file: File): String {
        val workspacePath = File(System.getProperty("user.dir")).resolve("workspace")
        return try {
            workspacePath.toPath().relativize(file.toPath()).toString()
        } catch (e: Exception) {
            file.name
        }
    }

    private fun updateProgress(reviewHistoryId: Long, processedFiles: Int, totalFiles: Int, totalIssues: Int) {
        val reviewHistory = reviewHistoryRepository.findById(reviewHistoryId).get()
        val updated = reviewHistory.copy(
            reviewedFiles = processedFiles,
            totalIssues = totalIssues
        )
        reviewHistoryRepository.save(updated)
    }
}