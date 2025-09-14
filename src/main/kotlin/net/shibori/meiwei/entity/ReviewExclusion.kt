// ReviewExclusion.kt
package net.shibori.meiwei.entity

import net.shibori.meiwei.enums.ExclusionType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "review_exclusions")
data class ReviewExclusion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    val repository: Repository,

    @Enumerated(EnumType.STRING)
    @Column(name = "exclusion_type", nullable = false, length = 20)
    val exclusionType: ExclusionType,

    @Column(name = "path", nullable = false, length = 500)
    val path: String,

    @Column(name = "pattern", length = 200)
    val pattern: String? = null,

    @Column(name = "description", length = 500)
    val description: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)