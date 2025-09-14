// RepositoryRepository.kt
package net.shibori.meiwei.repository

import net.shibori.meiwei.entity.Repository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository as SpringRepository

@SpringRepository
interface RepositoryRepository : JpaRepository<Repository, Long> {

    // アクティブなリポジトリのみ取得
    fun findByIsActiveTrue(): List<Repository>

    // 名前での検索
    fun findByNameContainingIgnoreCaseAndIsActiveTrue(name: String): List<Repository>

    // クローンURLでの検索（重複チェック用）
    fun findByCloneUrlAndIsActiveTrue(cloneUrl: String): Repository?

    // 名前での完全一致検索
    fun findByNameAndIsActiveTrue(name: String): Repository?

    // カスタムクエリ：除外設定も含めて取得
    @Query("SELECT r FROM Repository r LEFT JOIN FETCH r.exclusions WHERE r.id = :id AND r.isActive = true")
    fun findByIdWithExclusions(@Param("id") id: Long): Repository?

    // カスタムクエリ：最新のレビュー履歴も含めて取得
    @Query("""
        SELECT DISTINCT r FROM Repository r 
        LEFT JOIN FETCH r.reviewHistories rh 
        WHERE r.id = :id AND r.isActive = true
        ORDER BY rh.startedAt DESC
    """)
    fun findByIdWithLatestHistory(@Param("id") id: Long): Repository?
}