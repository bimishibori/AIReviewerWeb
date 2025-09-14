package net.shibori.meiwei.repository

import net.shibori.meiwei.entity.ReviewResult
import net.shibori.meiwei.enums.ReviewSeverity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReviewResultRepository : JpaRepository<ReviewResult, Long> {

    // 特定レビュー履歴の結果を取得
    fun findByReviewHistoryIdOrderByFilePathAscLineNumberAsc(reviewHistoryId: Long): List<ReviewResult>

    // 特定レビュー履歴の結果をページング
    fun findByReviewHistoryIdOrderByFilePathAscLineNumberAsc(
        reviewHistoryId: Long,
        pageable: Pageable
    ): Page<ReviewResult>

    // 重要度でのフィルタリング
    fun findByReviewHistoryIdAndSeverityOrderByFilePathAscLineNumberAsc(
        reviewHistoryId: Long,
        severity: ReviewSeverity
    ): List<ReviewResult>

    // 特定ファイルの結果を取得
    fun findByReviewHistoryIdAndFilePathOrderByLineNumberAsc(
        reviewHistoryId: Long,
        filePath: String
    ): List<ReviewResult>

    // 重要度以上の結果を取得
    @Query("""
        SELECT rr FROM ReviewResult rr 
        WHERE rr.reviewHistory.id = :reviewHistoryId 
        AND rr.severity IN :severities
        ORDER BY rr.filePath ASC, rr.lineNumber ASC
    """)
    fun findBySeverityIn(
        @Param("reviewHistoryId") reviewHistoryId: Long,
        @Param("severities") severities: List<ReviewSeverity>
    ): List<ReviewResult>

    // ルールIDでの検索
    fun findByReviewHistoryIdAndRuleIdOrderByFilePathAscLineNumberAsc(
        reviewHistoryId: Long,
        ruleId: String
    ): List<ReviewResult>

    // カスタムクエリ：重要度別カウント
    @Query("""
        SELECT rr.severity, COUNT(rr) FROM ReviewResult rr 
        WHERE rr.reviewHistory.id = :reviewHistoryId 
        GROUP BY rr.severity
    """)
    fun countBySeverity(@Param("reviewHistoryId") reviewHistoryId: Long): List<Array<Any>>

    // カスタムクエリ：ファイル別問題数
    @Query("""
        SELECT rr.filePath, COUNT(rr), MAX(rr.severity) FROM ReviewResult rr 
        WHERE rr.reviewHistory.id = :reviewHistoryId 
        GROUP BY rr.filePath 
        ORDER BY COUNT(rr) DESC
    """)
    fun getFileStatistics(@Param("reviewHistoryId") reviewHistoryId: Long): List<Array<Any>>

    // カスタムクエリ：よく出る問題トップ10
    @Query("""
        SELECT rr.ruleId, rr.message, COUNT(rr) FROM ReviewResult rr 
        WHERE rr.reviewHistory.repository.id = :repositoryId 
        AND rr.ruleId IS NOT NULL
        GROUP BY rr.ruleId, rr.message 
        ORDER BY COUNT(rr) DESC
    """)
    fun getTopIssues(@Param("repositoryId") repositoryId: Long, pageable: Pageable): Page<Array<Any>>
}