package net.shibori.meiwei.service.analyzer

import net.shibori.meiwei.common.AnalysisResult
import net.shibori.meiwei.enums.ReviewSeverity
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class CSharpAnalyzer {

    fun analyze(content: String, lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        results.addAll(detectStringConcatenationIssues(lines))
        results.addAll(detectCodeQualityIssues(lines))
        results.addAll(detectLinqIssues(content, lines))
        results.addAll(detectExceptionHandlingIssues(lines))
        results.addAll(detectEventSubscriptionIssues(content, lines))

        return results
    }

    // 文字列連結問題の検出
    private fun detectStringConcatenationIssues(lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // String連結のアンチパターン
            if (line.contains("\"") && line.contains("+") && !line.contains("//")) {
                val stringConcatPattern = Pattern.compile("\".*\"\\s*\\+|\\+\\s*\".*\"")
                if (stringConcatPattern.matcher(line).find()) {
                    results.add(AnalysisResult(
                        lineNumber = lineNumber,
                        columnNumber = line.indexOf("+"),
                        severity = ReviewSeverity.WARNING,
                        ruleId = "CSHARP_PERFORMANCE_001",
                        message = "文字列の + 演算子による連結はGCを発生させます。",
                        suggestion = "StringBuilder, string.Format, または補間文字列($\"\")を使用してください。",
                        codeSnippet = line.trim()
                    ))
                }
            }
        }

        return results
    }

    // コード品質問題の検出
    private fun detectCodeQualityIssues(lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // public フィールドの多用
            if (line.trim().startsWith("public ") &&
                !line.contains("(") &&
                !line.contains("class") &&
                !line.contains("interface") &&
                !line.contains("enum") &&
                !line.contains("//")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("public"),
                    severity = ReviewSeverity.INFO,
                    ruleId = "CSHARP_ENCAPSULATION_001",
                    message = "publicフィールドはカプセル化を破ります。",
                    suggestion = "privateフィールドとプロパティの使用を検討してください。",
                    codeSnippet = line.trim()
                ))
            }

            // マジックナンバー
            val magicNumberPattern = Pattern.compile("\\b\\d{2,}\\b")
            val matcher = magicNumberPattern.matcher(line)
            if (matcher.find() && !line.contains("//") && !line.contains("const") && !line.contains("readonly")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = matcher.start(),
                    severity = ReviewSeverity.INFO,
                    ruleId = "CSHARP_MAINTAINABILITY_001",
                    message = "マジックナンバーが使用されています。",
                    suggestion = "定数やenumの使用を検討してください。",
                    codeSnippet = line.trim()
                ))
            }

            // 不適切な命名規則
            val variablePattern = Pattern.compile("\\b[a-z][a-zA-Z0-9]*\\s+([A-Z][a-zA-Z0-9]*)\\s*[=;]")
            val variableMatcher = variablePattern.matcher(line)
            if (variableMatcher.find() && !line.contains("//")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = variableMatcher.start(1),
                    severity = ReviewSeverity.INFO,
                    ruleId = "CSHARP_NAMING_001",
                    message = "ローカル変数名がPascalCaseになっています。",
                    suggestion = "ローカル変数はcamelCaseを使用してください。",
                    codeSnippet = line.trim()
                ))
            }
        }

        return results
    }

    // LINQ問題の検出
    private fun detectLinqIssues(content: String, lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()
        val heavyLinqMethods = listOf("Where", "Select", "First", "Any", "All", "ToList", "ToArray", "OrderBy")

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            heavyLinqMethods.forEach { method ->
                if (line.contains(".$method(") && !line.contains("//")) {
                    // パフォーマンスクリティカルな場所での使用をチェック
                    if (isInPerformanceCriticalContext(lines, index)) {
                        results.add(AnalysisResult(
                            lineNumber = lineNumber,
                            columnNumber = line.indexOf(method),
                            severity = ReviewSeverity.WARNING,
                            ruleId = "CSHARP_PERFORMANCE_002",
                            message = "パフォーマンスクリティカルな場所でLINQ($method)を使用しています。",
                            suggestion = "forループまたはforeachループの使用を検討してください。",
                            codeSnippet = line.trim()
                        ))
                    }
                }
            }
        }

        return results
    }

    // 例外処理問題の検出
    private fun detectExceptionHandlingIssues(lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // 空のcatch句
            if (line.trim().startsWith("catch")) {
                val nextLine = lines.getOrNull(index + 1)?.trim()
                val nextNextLine = lines.getOrNull(index + 2)?.trim()

                if (nextLine == "{" && nextNextLine == "}") {
                    results.add(AnalysisResult(
                        lineNumber = lineNumber,
                        columnNumber = line.indexOf("catch"),
                        severity = ReviewSeverity.WARNING,
                        ruleId = "CSHARP_EXCEPTION_001",
                        message = "空のcatch句は例外を隠蔽します。",
                        suggestion = "ログ出力または適切な例外処理を追加してください。",
                        codeSnippet = line.trim()
                    ))
                }
            }

            // Exception の直接キャッチ
            if (line.contains("catch (Exception") && !line.contains("//")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("Exception"),
                    severity = ReviewSeverity.INFO,
                    ruleId = "CSHARP_EXCEPTION_002",
                    message = "Exceptionを直接キャッチしています。",
                    suggestion = "より具体的な例外型をキャッチすることを検討してください。",
                    codeSnippet = line.trim()
                ))
            }
        }

        return results
    }

    // イベント購読問題の検出
    private fun detectEventSubscriptionIssues(content: String, lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // イベント購読の解除忘れ
            if (line.contains("+=") && (line.contains("Event") || line.contains("Action") || line.contains("Func"))) {
                val hasUnsubscribe = content.contains("-=")
                if (!hasUnsubscribe) {
                    results.add(AnalysisResult(
                        lineNumber = lineNumber,
                        columnNumber = line.indexOf("+="),
                        severity = ReviewSeverity.WARNING,
                        ruleId = "CSHARP_MEMORY_001",
                        message = "イベント購読の解除が見当たりません。メモリリークの原因となる可能性があります。",
                        suggestion = "適切なタイミングで -= による購読解除を追加してください。",
                        codeSnippet = line.trim()
                    ))
                }
            }
        }

        return results
    }

    private fun isInPerformanceCriticalContext(lines: List<String>, currentIndex: Int): Boolean {
        // パフォーマンスクリティカルなコンテキストかどうかを判定
        for (i in (currentIndex - 10).coerceAtLeast(0)..currentIndex) {
            val line = lines[i]
            if (line.contains("Update()") ||
                line.contains("FixedUpdate()") ||
                line.contains("LateUpdate()") ||
                line.contains("for (") ||
                line.contains("foreach (")) {
                return true
            }
        }
        return false
    }
}