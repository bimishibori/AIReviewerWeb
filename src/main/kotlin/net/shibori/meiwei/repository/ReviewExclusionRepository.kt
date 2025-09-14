package net.shibori.meiwei.repository

import net.shibori.meiwei.entity.ReviewExclusion
import net.shibori.meiwei.enums.ExclusionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReviewExclusionRepository : JpaRepository<ReviewExclusion, Long> {

    // 特定リポジトリのアクティブな除外設定を取得
    fun findByRepositoryIdAndIsActiveTrueOrderByExclusionTypeAscPathAsc(repositoryId: Long): List<ReviewExclusion>

    // 除外タイプでの検索
    fun findByRepositoryIdAndExclusionTypeAndIsActiveTrueOrderByPathAsc(
        repositoryId: Long,
        exclusionType: ExclusionType
    ): List<ReviewExclusion>

    // パス完全一致での検索（重複チェック用）
    fun findByRepositoryIdAndPathAndIsActiveTrue(repositoryId: Long, path: String): ReviewExclusion?

    // パス部分一致での検索
    fun findByRepositoryIdAndPathContainingIgnoreCaseAndIsActiveTrue(
        repositoryId: Long,
        pathPart: String
    ): List<ReviewExclusion>

    // カスタムクエリ：特定ファイルパスが除外対象かチェック
    @Query("""
        SELECT COUNT(re) > 0 FROM ReviewExclusion re 
        WHERE re.repository.id = :repositoryId 
        AND re.isActive = true
        AND (
            (re.exclusionType = 'FILE' AND re.path = :filePath) OR
            (re.exclusionType = 'DIRECTORY' AND :filePath LIKE CONCAT(re.path, '%')) OR
            (re.exclusionType = 'PATTERN' AND :filePath LIKE re.pattern) OR
            (re.exclusionType = 'EXTENSION' AND :filePath LIKE CONCAT('%.', re.path))
        )
    """)
    fun isPathExcluded(@Param("repositoryId") repositoryId: Long, @Param("filePath") filePath: String): Boolean

    // 特定リポジトリの除外設定数を取得
    fun countByRepositoryIdAndIsActiveTrue(repositoryId: Long): Long
}