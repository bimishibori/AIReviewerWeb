package net.shibori.meiwei.service

import net.shibori.meiwei.dto.ReviewExecutionDto
import net.shibori.meiwei.dto.ReviewProgressDto
import net.shibori.meiwei.entity.Repository
import net.shibori.meiwei.entity.ReviewHistory
import net.shibori.meiwei.enums.ReviewStatus
import net.shibori.meiwei.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import java.time.LocalDateTime
import java.util.*

class ReviewServiceTest {

 private lateinit var repositoryRepository: RepositoryRepository
 private lateinit var reviewHistoryRepository: ReviewHistoryRepository
 private lateinit var reviewResultRepository: ReviewResultRepository
 private lateinit var reviewExclusionRepository: ReviewExclusionRepository
 private lateinit var gitService: GitService
 private lateinit var codeAnalysisService: CodeAnalysisService

 private lateinit var reviewService: ReviewService

 @BeforeEach
 fun setup() {
  repositoryRepository = mock(RepositoryRepository::class.java)
  reviewHistoryRepository = mock(ReviewHistoryRepository::class.java)
  reviewResultRepository = mock(ReviewResultRepository::class.java)
  reviewExclusionRepository = mock(ReviewExclusionRepository::class.java)
  gitService = mock(GitService::class.java)
  codeAnalysisService = mock(CodeAnalysisService::class.java)

  reviewService = ReviewService(
   repositoryRepository,
   reviewHistoryRepository,
   reviewResultRepository,
   reviewExclusionRepository,
   gitService,
   codeAnalysisService
  )
 }

 @Test
 fun `startReview should create review history and return id`() {
  val repo = Repository(id = 1L, name = "test-repo", cloneUrl = "url", branchName = "main")
  val executionDto = ReviewExecutionDto(repositoryId = 1L, forcePull = false)

  `when`(repositoryRepository.findById(1L)).thenReturn(Optional.of(repo))
  `when`(reviewHistoryRepository.findByStatus(ReviewStatus.RUNNING)).thenReturn(emptyList())
  `when`(reviewHistoryRepository.save(any(ReviewHistory::class.java)))
   .thenAnswer { invocation ->
    val history = invocation.getArgument<ReviewHistory>(0)
    history.copy(id = 100L)
   }

  val result = reviewService.startReview(executionDto)

  assertEquals(100L, result)
  verify(reviewHistoryRepository, times(1)).save(any(ReviewHistory::class.java))
 }

 @Test
 fun `getReviewProgress should return calculated progress`() {
  val history = ReviewHistory(
   id = 200L,
   repository = Repository(id = 1L, name = "test-repo", cloneUrl = "url", branchName = "main"),
   status = ReviewStatus.RUNNING,
   startedAt = LocalDateTime.now().minusSeconds(30),
   reviewedFiles = 5,
   totalFiles = 10,
   totalIssues = 2
  )

  `when`(reviewHistoryRepository.findById(200L)).thenReturn(Optional.of(history))

  val progress: ReviewProgressDto = reviewService.getReviewProgress(200L)

  assertEquals(50, progress.progress)
  assertEquals(5, progress.processedFiles)
  assertEquals(10, progress.totalFiles)
 }
}
