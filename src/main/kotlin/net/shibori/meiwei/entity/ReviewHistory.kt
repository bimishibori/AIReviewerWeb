// ReviewHistory.kt
package net.shibori.meiwei.entity

import net.shibori.meiwei.enums.ReviewStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "review_histories")
data class ReviewHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    val repository: Repository,

    @Column(name = "commit_hash", length = 40)
    val commitHash: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: ReviewStatus = ReviewStatus.PENDING,

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null,

    @Column(name = "total_files")
    val totalFiles: Int? = null,

    @Column(name = "reviewed_files")
    val reviewedFiles: Int? = null,

    @Column(name = "total_issues")
    val totalIssues: Int? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    // リレーション
    @OneToMany(mappedBy = "reviewHistory", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val reviewResults: List<ReviewResult> = emptyList()
)