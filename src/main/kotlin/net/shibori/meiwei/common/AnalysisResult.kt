package net.shibori.meiwei.common

import net.shibori.meiwei.enums.ReviewSeverity

data class AnalysisResult(
    val lineNumber: Int,
    val columnNumber: Int,
    val severity: ReviewSeverity,
    val ruleId: String,
    val message: String,
    val suggestion: String,
    val codeSnippet: String
)
