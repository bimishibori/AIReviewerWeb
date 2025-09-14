package net.shibori.meiwei.dto

data class ReviewDashboardDto(
    val totalRepositories: Int,
    val activeRepositories: Int,
    val runningReviews: Int,
    val todayReviews: Int,
    val recentReviews: List<ReviewHistoryListDto>,
    val repositoryStatistics: List<RepositoryStatisticDto>
)