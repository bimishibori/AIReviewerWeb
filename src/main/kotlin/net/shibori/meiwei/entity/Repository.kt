// Repository.kt
package net.shibori.meiwei.entity

import net.shibori.meiwei.enums.ExclusionType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "repositories")
data class Repository(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "name", nullable = false, length = 255)
    val name: String,

    @Column(name = "clone_url", nullable = false, length = 500)
    val cloneUrl: String,

    @Column(name = "branch_name", nullable = false, length = 100)
    val branchName: String = "main",

    @Column(name = "description", length = 1000)
    val description: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // リレーション
    @OneToMany(mappedBy = "repository", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val exclusions: List<ReviewExclusion> = emptyList(),

    @OneToMany(mappedBy = "repository", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val reviewHistories: List<ReviewHistory> = emptyList()
)