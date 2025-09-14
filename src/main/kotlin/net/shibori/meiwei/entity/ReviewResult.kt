// ReviewResult.kt
package net.shibori.meiwei.entity

import net.shibori.meiwei.enums.ReviewSeverity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "review_results")
data class ReviewResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_history_id", nullable = false)
    val reviewHistory: ReviewHistory,

    @Column(name = "file_path", nullable = false, length = 500)
    val filePath: String,

    @Column(name = "line_number")
    val lineNumber: Int? = null,

    @Column(name = "column_number")
    val columnNumber: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    val severity: ReviewSeverity,

    @Column(name = "rule_id", length = 100)
    val ruleId: String? = null,

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Column(name = "suggestion", columnDefinition = "TEXT")
    val suggestion: String? = null,

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    val codeSnippet: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)