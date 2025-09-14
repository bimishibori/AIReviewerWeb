package net.shibori.meiwei.repository

import net.shibori.meiwei.entity.ReviewHistory
import net.shibori.meiwei.enums.ReviewStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ReviewHistoryRepository : JpaRepository<ReviewHistory, Long> {

    // 特定リポジトリのレビュー履歴を日時降順で取得
    fun findByRepositoryIdOrderByStartedAtDesc(repositoryId: Long): List<ReviewHistory>

    // 特定リポジトリのレビュー履歴をページング
    fun findByRepositoryIdOrderByStartedAtDesc(repositoryId: Long, pageable: Pageable): Page<ReviewHistory>

    // ステータスでの検索
    fun findByStatusOrderByStartedAtDesc(status: ReviewStatus): List<ReviewHistory>

    // 実行中のレビューを取得
    fun findByStatus(status: ReviewStatus): List<ReviewHistory>

    // 特定リポジトリの最新レビューを取得
    fun findTopByRepositoryIdOrderByStartedAtDesc(repositoryId: Long): ReviewHistory?

    // 特定リポジトリの最新完了レビューを取得
    fun findTopByRepositoryIdAndStatusOrderByStartedAtDesc(
        repositoryId: Long,
        status: ReviewStatus
    ): ReviewHistory?

    // 期間指定でのレビュー履歴取得
    fun findByStartedAtBetweenOrderByStartedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<ReviewHistory>

    // カスタムクエリ：統計情報取得
    @Query("""
        SELECT rh FROM ReviewHistory rh 
        LEFT JOIN FETCH rh.reviewResults 
        WHERE rh.id = :id
    """)
    fun findByIdWithResults(@Param("id") id: Long): ReviewHistory?

    // カスタムクエリ：レビュー統計
    @Query("""
        SELECT 
            COUNT(rh) as totalReviews,
            COUNT(CASE WHEN rh.status = 'COMPLETED' THEN 1 END) as completedReviews,
            COUNT(CASE WHEN rh.status = 'FAILED' THEN 1 END) as failedReviews,
            AVG(rh.totalIssues) as avgIssues
        FROM ReviewHistory rh 
        WHERE rh.repository.id = :repositoryId
    """)
    fun getRepositoryStatistics(@Param("repositoryId") repositoryId: Long): Map<String, Any>
}