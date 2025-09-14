package net.shibori.meiwei.controller

import net.shibori.meiwei.dto.ApiResponse
import net.shibori.meiwei.dto.ReviewProgressDto
import net.shibori.meiwei.service.ReviewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ApiController(
    private val reviewService: ReviewService
) {

    // レビュー進行状況取得（ポーリング用）
    @GetMapping("/reviews/{reviewHistoryId}/progress")
    fun getReviewProgress(@PathVariable reviewHistoryId: Long): ResponseEntity<ApiResponse<ReviewProgressDto>> {
        return try {
            val progress = reviewService.getReviewProgress(reviewHistoryId)
            ResponseEntity.ok(ApiResponse(
                success = true,
                data = progress,
                message = "レビュー進行状況を取得しました"
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(404).body(ApiResponse(
                success = false,
                message = e.message,
                errors = listOf("Review history not found")
            ))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(ApiResponse(
                success = false,
                message = "レビュー進行状況の取得に失敗しました",
                errors = listOf(e.message ?: "Unknown error")
            ))
        }
    }

    // レビュー完了確認（ポーリング用）
    @GetMapping("/reviews/{reviewHistoryId}/status")
    fun getReviewStatus(@PathVariable reviewHistoryId: Long): ResponseEntity<ApiResponse<String>> {
        return try {
            val progress = reviewService.getReviewProgress(reviewHistoryId)
            ResponseEntity.ok(ApiResponse(
                success = true,
                data = progress.status.name,
                message = "レビューステータスを取得しました"
            ))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(ApiResponse(
                success = false,
                message = "レビューステータスの取得に失敗しました",
                errors = listOf(e.message ?: "Unknown error")
            ))
        }
    }
}