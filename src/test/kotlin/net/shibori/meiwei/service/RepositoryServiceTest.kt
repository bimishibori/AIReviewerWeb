package net.shibori.meiwei.service

import net.shibori.meiwei.dto.RepositoryFormDto
import net.shibori.meiwei.entity.Repository
import net.shibori.meiwei.entity.ReviewHistory
import net.shibori.meiwei.enums.ReviewStatus
import net.shibori.meiwei.repository.RepositoryRepository
import net.shibori.meiwei.repository.ReviewHistoryRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
class RepositoryServiceTest {

    @Mock
    private lateinit var repositoryRepository: RepositoryRepository

    @Mock
    private lateinit var reviewHistoryRepository: ReviewHistoryRepository

    private lateinit var repositoryService: RepositoryService

    private val testDateTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0)

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repositoryService = RepositoryService(repositoryRepository, reviewHistoryRepository)
    }

    @Test
    fun `createRepository - 正常ケース`() {
        // Given
        val formDto = RepositoryFormDto(
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main",
            description = "テストリポジトリ"
        )

        val savedRepository = Repository(
            id = 1L,
            name = formDto.name,
            cloneUrl = formDto.cloneUrl,
            branchName = formDto.branchName,
            description = formDto.description,
            createdAt = testDateTime,
            updatedAt = testDateTime
        )

        whenever(repositoryRepository.findByCloneUrlAndIsActiveTrue(formDto.cloneUrl)).thenReturn(null)
        whenever(repositoryRepository.findByNameAndIsActiveTrue(formDto.name)).thenReturn(null)
        whenever(repositoryRepository.save(any<Repository>())).thenReturn(savedRepository)

        // When
        val result = repositoryService.createRepository(formDto)

        // Then
        assertEquals(1L, result.id)
        assertEquals("test-repo", result.name)
        assertEquals("https://github.com/test/repo.git", result.cloneUrl)
        assertEquals("main", result.branchName)
        assertEquals("テストリポジトリ", result.description)
        assertTrue(result.isActive)
        assertEquals(0, result.exclusionCount)

        verify(repositoryRepository).findByCloneUrlAndIsActiveTrue(formDto.cloneUrl)
        verify(repositoryRepository).findByNameAndIsActiveTrue(formDto.name)
        verify(repositoryRepository).save(any<Repository>())
    }

    @Test
    fun `createRepository - クローンURL重複エラー`() {
        // Given
        val formDto = RepositoryFormDto(
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main",
            description = "テストリポジトリ"
        )

        val existingRepository = Repository(
            id = 1L,
            name = "existing-repo",
            cloneUrl = formDto.cloneUrl,
            branchName = "main"
        )

        whenever(repositoryRepository.findByCloneUrlAndIsActiveTrue(formDto.cloneUrl))
            .thenReturn(existingRepository)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            repositoryService.createRepository(formDto)
        }

        assertEquals("同じクローンURLのリポジトリが既に存在します", exception.message)
        verify(repositoryRepository).findByCloneUrlAndIsActiveTrue(formDto.cloneUrl)
        verify(repositoryRepository, never()).save(any<Repository>())
    }

    @Test
    fun `createRepository - 名前重複エラー`() {
        // Given
        val formDto = RepositoryFormDto(
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main",
            description = "テストリポジトリ"
        )

        val existingRepository = Repository(
            id = 1L,
            name = formDto.name,
            cloneUrl = "https://github.com/other/repo.git",
            branchName = "main"
        )

        whenever(repositoryRepository.findByCloneUrlAndIsActiveTrue(formDto.cloneUrl)).thenReturn(null)
        whenever(repositoryRepository.findByNameAndIsActiveTrue(formDto.name))
            .thenReturn(existingRepository)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            repositoryService.createRepository(formDto)
        }

        assertEquals("同じ名前のリポジトリが既に存在します", exception.message)
        verify(repositoryRepository).findByNameAndIsActiveTrue(formDto.name)
        verify(repositoryRepository, never()).save(any<Repository>())
    }

    @Test
    fun `updateRepository - 正常ケース`() {
        // Given
        val repositoryId = 1L
        val formDto = RepositoryFormDto(
            name = "updated-repo",
            cloneUrl = "https://github.com/updated/repo.git",
            branchName = "develop",
            description = "更新されたリポジトリ"
        )

        val existingRepository = Repository(
            id = repositoryId,
            name = "old-repo",
            cloneUrl = "https://github.com/old/repo.git",
            branchName = "main",
            description = "古いリポジトリ",
            createdAt = testDateTime,
            updatedAt = testDateTime
        )

        val updatedRepository = existingRepository.copy(
            name = formDto.name,
            cloneUrl = formDto.cloneUrl,
            branchName = formDto.branchName,
            description = formDto.description,
            updatedAt = LocalDateTime.now()
        )

        whenever(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(existingRepository))
        whenever(repositoryRepository.findByNameAndIsActiveTrue(formDto.name)).thenReturn(null)
        whenever(repositoryRepository.save(any<Repository>())).thenReturn(updatedRepository)

        // When
        val result = repositoryService.updateRepository(repositoryId, formDto)

        // Then
        assertEquals(repositoryId, result.id)
        assertEquals("updated-repo", result.name)
        assertEquals("https://github.com/updated/repo.git", result.cloneUrl)
        assertEquals("develop", result.branchName)
        assertEquals("更新されたリポジトリ", result.description)

        verify(repositoryRepository).findById(repositoryId)
        verify(repositoryRepository).findByNameAndIsActiveTrue(formDto.name)
        verify(repositoryRepository).save(any<Repository>())
    }

    @Test
    fun `updateRepository - リポジトリが見つからない`() {
        // Given
        val repositoryId = 999L
        val formDto = RepositoryFormDto(
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main"
        )

        whenever(repositoryRepository.findById(repositoryId)).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            repositoryService.updateRepository(repositoryId, formDto)
        }

        assertEquals("リポジトリが見つかりません: $repositoryId", exception.message)
        verify(repositoryRepository).findById(repositoryId)
        verify(repositoryRepository, never()).save(any<Repository>())
    }

    @Test
    fun `updateRepository - 名前重複エラー（他のリポジトリと重複）`() {
        // Given
        val repositoryId = 1L
        val formDto = RepositoryFormDto(
            name = "duplicate-name",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main"
        )

        val existingRepository = Repository(
            id = repositoryId,
            name = "old-name",
            cloneUrl = "https://github.com/old/repo.git",
            branchName = "main"
        )

        val duplicateRepository = Repository(
            id = 2L,
            name = formDto.name,
            cloneUrl = "https://github.com/other/repo.git",
            branchName = "main"
        )

        whenever(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(existingRepository))
        whenever(repositoryRepository.findByNameAndIsActiveTrue(formDto.name))
            .thenReturn(duplicateRepository)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            repositoryService.updateRepository(repositoryId, formDto)
        }

        assertEquals("同じ名前のリポジトリが既に存在します", exception.message)
        verify(repositoryRepository, never()).save(any<Repository>())
    }

    @Test
    fun `getRepository - 正常ケース（レビュー履歴あり）`() {
        // Given
        val repositoryId = 1L
        val repository = Repository(
            id = repositoryId,
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main",
            description = "テストリポジトリ",
            createdAt = testDateTime,
            updatedAt = testDateTime
        )

        val reviewHistory = ReviewHistory(
            id = 1L,
            repository = repository,
            commitHash = "abc123",
            status = ReviewStatus.COMPLETED,
            startedAt = testDateTime,
            completedAt = testDateTime.plusHours(1),
            totalFiles = 10,
            reviewedFiles = 10,
            totalIssues = 5
        )

        whenever(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repository))
        whenever(reviewHistoryRepository.findTopByRepositoryIdOrderByStartedAtDesc(repositoryId))
            .thenReturn(reviewHistory)

        // When
        val result = repositoryService.getRepository(repositoryId)

        // Then
        assertEquals(repositoryId, result.id)
        assertEquals("test-repo", result.name)
        assertNotNull(result.latestReview)
        assertEquals("COMPLETED", result.latestReview?.status)
        assertEquals(5, result.latestReview?.totalIssues)

        verify(repositoryRepository).findById(repositoryId)
        verify(reviewHistoryRepository).findTopByRepositoryIdOrderByStartedAtDesc(repositoryId)
    }

    @Test
    fun `getRepository - リポジトリが見つからない`() {
        // Given
        val repositoryId = 999L
        whenever(repositoryRepository.findById(repositoryId)).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            repositoryService.getRepository(repositoryId)
        }

        assertEquals("リポジトリが見つかりません: $repositoryId", exception.message)
        verify(repositoryRepository).findById(repositoryId)
    }

    @Test
    fun `getActiveRepositories - 正常ケース`() {
        // Given
        val repository1 = Repository(
            id = 1L,
            name = "repo1",
            cloneUrl = "https://github.com/test/repo1.git",
            branchName = "main"
        )

        val repository2 = Repository(
            id = 2L,
            name = "repo2",
            cloneUrl = "https://github.com/test/repo2.git",
            branchName = "develop"
        )

        val reviewHistory1 = ReviewHistory(
            id = 1L,
            repository = repository1,
            commitHash = "abc123",
            status = ReviewStatus.COMPLETED,
            startedAt = testDateTime,
            totalIssues = 3
        )

        whenever(repositoryRepository.findByIsActiveTrue()).thenReturn(listOf(repository1, repository2))
        whenever(reviewHistoryRepository.findTopByRepositoryIdOrderByStartedAtDesc(1L))
            .thenReturn(reviewHistory1)
        whenever(reviewHistoryRepository.findTopByRepositoryIdOrderByStartedAtDesc(2L))
            .thenReturn(null)

        // When
        val result = repositoryService.getActiveRepositories()

        // Then
        assertEquals(2, result.size)
        assertEquals("repo1", result[0].name)
        assertEquals("COMPLETED", result[0].lastReviewStatus)
        assertEquals(3, result[0].totalIssues)
        assertEquals("repo2", result[1].name)
        assertNull(result[1].lastReviewStatus)

        verify(repositoryRepository).findByIsActiveTrue()
        verify(reviewHistoryRepository).findTopByRepositoryIdOrderByStartedAtDesc(1L)
        verify(reviewHistoryRepository).findTopByRepositoryIdOrderByStartedAtDesc(2L)
    }

    @Test
    fun `searchRepositories - 正常ケース`() {
        // Given
        val keyword = "test"
        val repository = Repository(
            id = 1L,
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main"
        )

        whenever(repositoryRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword))
            .thenReturn(listOf(repository))
        whenever(reviewHistoryRepository.findTopByRepositoryIdOrderByStartedAtDesc(1L))
            .thenReturn(null)

        // When
        val result = repositoryService.searchRepositories(keyword)

        // Then
        assertEquals(1, result.size)
        assertEquals("test-repo", result[0].name)

        verify(repositoryRepository).findByNameContainingIgnoreCaseAndIsActiveTrue(keyword)
        verify(reviewHistoryRepository).findTopByRepositoryIdOrderByStartedAtDesc(1L)
    }

    @Test
    fun `deactivateRepository - 正常ケース`() {
        // Given
        val repositoryId = 1L
        val repository = Repository(
            id = repositoryId,
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            branchName = "main",
            isActive = true,
            createdAt = testDateTime,
            updatedAt = testDateTime
        )

        val deactivatedRepository = repository.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )

        whenever(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repository))
        whenever(repositoryRepository.save(any<Repository>())).thenReturn(deactivatedRepository)

        // When
        repositoryService.deactivateRepository(repositoryId)

        // Then
        verify(repositoryRepository).findById(repositoryId)
        verify(repositoryRepository).save(argThat<Repository> { !this.isActive })
    }

    @Test
    fun `deactivateRepository - リポジトリが見つからない`() {
        // Given
        val repositoryId = 999L
        whenever(repositoryRepository.findById(repositoryId)).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            repositoryService.deactivateRepository(repositoryId)
        }

        assertEquals("リポジトリが見つかりません: $repositoryId", exception.message)
        verify(repositoryRepository).findById(repositoryId)
        verify(repositoryRepository, never()).save(any<Repository>())
    }
}