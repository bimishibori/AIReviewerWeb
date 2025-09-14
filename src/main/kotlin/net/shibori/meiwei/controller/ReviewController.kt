package net.shibori.meiwei.controller

import net.shibori.meiwei.dto.ReviewExecutionDto
import net.shibori.meiwei.service.ReviewService
import net.shibori.meiwei.repository.RepositoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val repositoryRepository: RepositoryRepository
) {

    // レビュー実行画面表示
    @GetMapping("/execute")
    fun showExecuteForm(@RequestParam repositoryId: Long, model: Model): String {
        try {
            val repository = repositoryRepository.findById(repositoryId).orElseThrow {
                IllegalArgumentException("リポジトリが見つかりません")
            }

            model.addAttribute("repository", repository)
            return "reviews/execute"
        } catch (e: IllegalArgumentException) {
            return "redirect:/repositories?error=notfound"
        }
    }

    // レビュー実行処理
    @PostMapping("/execute")
    fun execute(
        @RequestParam repositoryId: Long,
        @RequestParam(defaultValue = "false") forcePull: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val executionDto = ReviewExecutionDto(
                repositoryId = repositoryId,
                forcePull = forcePull
            )

            val reviewHistoryId = reviewService.startReview(executionDto)
            redirectAttributes.addFlashAttribute("successMessage", "コードレビューを開始しました。")

            return "redirect:/reviews/$reviewHistoryId/progress"
        } catch (e: IllegalStateException) {
            redirectAttributes.addFlashAttribute("errorMessage", e.message)
            return "redirect:/reviews/execute?repositoryId=$repositoryId"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "レビューの開始に失敗しました: ${e.message}")
            return "redirect:/reviews/execute?repositoryId=$repositoryId"
        }
    }

    // レビュー進行状況表示
    @GetMapping("/{reviewHistoryId}/progress")
    fun showProgress(@PathVariable reviewHistoryId: Long, model: Model): String {
        try {
            val progress = reviewService.getReviewProgress(reviewHistoryId)
            model.addAttribute("progress", progress)
            return "reviews/progress"
        } catch (e: IllegalArgumentException) {
            return "redirect:/repositories?error=notfound"
        }
    }

    // レビュー結果表示
    @GetMapping("/{reviewHistoryId}/results")
    fun showResults(
        @PathVariable reviewHistoryId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "filePath") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
        model: Model
    ): String {
        try {
            val sort = Sort.by(
                if (sortDir.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC,
                sortBy
            )
            val pageable = PageRequest.of(page, size, sort)

            val results = reviewService.getReviewResults(reviewHistoryId, pageable)
            val summary = reviewService.getReviewSummary(reviewHistoryId)

            model.addAttribute("results", results)
            model.addAttribute("summary", summary)
            model.addAttribute("reviewHistoryId", reviewHistoryId)
            model.addAttribute("currentPage", page)
            model.addAttribute("sortBy", sortBy)
            model.addAttribute("sortDir", sortDir)

            return "reviews/results"
        } catch (e: Exception) {
            return "redirect:/repositories?error=review"
        }
    }

    // レビュー履歴一覧
    @GetMapping("/history")
    fun showHistory(
        @RequestParam(required = false) repositoryId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"))
        val histories = reviewService.getReviewHistories(repositoryId, pageable)

        model.addAttribute("histories", histories)
        model.addAttribute("repositoryId", repositoryId)
        model.addAttribute("currentPage", page)

        if (repositoryId != null) {
            val repository = repositoryRepository.findById(repositoryId).orElse(null)
            model.addAttribute("repository", repository)
        }

        return "reviews/history"
    }
}