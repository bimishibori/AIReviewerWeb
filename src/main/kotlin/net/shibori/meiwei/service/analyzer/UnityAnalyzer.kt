package net.shibori.meiwei.service.analyzer

import net.shibori.meiwei.common.AnalysisResult
import net.shibori.meiwei.enums.ReviewSeverity
import org.springframework.stereotype.Component

@Component
class UnityAnalyzer {

    fun analyze(content: String, lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        results.addAll(detectUnityPerformanceIssues(lines))
        results.addAll(detectUnityBestPracticeIssues(lines))
        results.addAll(detectUnityLifecycleIssues(content, lines))
        results.addAll(detectUnitySecurityIssues(lines))
        results.addAll(detectUnityMemoryIssues(content, lines))

        return results
    }

    // Unityパフォーマンス問題の検出
    private fun detectUnityPerformanceIssues(lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // Update内でのGameObject.Find系
            if (isInUpdateMethod(lines, index)) {
                when {
                    line.contains("GameObject.Find") -> {
                        results.add(AnalysisResult(
                            lineNumber = lineNumber,
                            columnNumber = line.indexOf("GameObject.Find"),
                            severity = ReviewSeverity.ERROR,
                            ruleId = "UNITY_PERFORMANCE_001",
                            message = "Update()内でGameObject.Find()を使用しています。毎フレーム実行されるためパフォーマンスが悪化します。",
                            suggestion = "Start()またはAwake()でキャッシュするか、シリアライズフィールドを使用してください。",
                            codeSnippet = line.trim()
                        ))
                    }
                    line.contains("FindObjectOfType") -> {
                        results.add(AnalysisResult(
                            lineNumber = lineNumber,
                            columnNumber = line.indexOf("FindObjectOfType"),
                            severity = ReviewSeverity.ERROR,
                            ruleId = "UNITY_PERFORMANCE_002",
                            message = "Update()内でFindObjectOfType()を使用しています。非常に重い処理です。",
                            suggestion = "Start()またはAwake()でキャッシュしてください。",
                            codeSnippet = line.trim()
                        ))
                    }
                    line.contains("GetComponent") && !line.contains("//") -> {
                        results.add(AnalysisResult(
                            lineNumber = lineNumber,
                            columnNumber = line.indexOf("GetComponent"),
                            severity = ReviewSeverity.WARNING,
                            ruleId = "UNITY_PERFORMANCE_003",
                            message = "Update()内でGetComponent()を使用しています。キャッシュを検討してください。",
                            suggestion = "コンポーネントをフィールドにキャッシュしてください。",
                            codeSnippet = line.trim()
                        ))
                    }
                }
            }

            // Camera.main の多用
            if (line.contains("Camera.main") && !line.contains("//")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("Camera.main"),
                    severity = ReviewSeverity.WARNING,
                    ruleId = "UNITY_PERFORMANCE_004",
                    message = "Camera.mainは内部でFindGameObjectWithTagを実行するため重い処理です。",
                    suggestion = "カメラをキャッシュして使用してください。",
                    codeSnippet = line.trim()
                ))
            }
        }

        return results
    }

    // Unityベストプラクティス問題の検出
    private fun detectUnityBestPracticeIssues(lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // CompareTag使用推奨
            if (line.contains("gameObject.tag ==") || line.contains("transform.tag ==")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf(".tag"),
                    severity = ReviewSeverity.INFO,
                    ruleId = "UNITY_BEST_PRACTICE_001",
                    message = "tag比較には == ではなくCompareTag()の使用が推奨されます。",
                    suggestion = "gameObject.CompareTag(\"TagName\") を使用してください。",
                    codeSnippet = line.trim()
                ))
            }

            // Instantiate without parent
            if (line.contains("Instantiate(") && !line.contains("parent") && !line.contains("//")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("Instantiate"),
                    severity = ReviewSeverity.WARNING,
                    ruleId = "UNITY_BEST_PRACTICE_002",
                    message = "Instantiate時にparentを指定していません。Hierarchyが汚くなる可能性があります。",
                    suggestion = "Instantiate(prefab, parent)の形式を使用してください。",
                    codeSnippet = line.trim()
                ))
            }

            // SerializeField推奨
            if (line.trim().startsWith("public ") &&
                (line.contains("GameObject") || line.contains("Transform") || line.contains("Component")) &&
                !line.contains("(") && !line.contains("//")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("public"),
                    severity = ReviewSeverity.INFO,
                    ruleId = "UNITY_BEST_PRACTICE_003",
                    message = "UnityオブジェクトのpublicフィールドよりもSerializeFieldの使用が推奨されます。",
                    suggestion = "[SerializeField] private フィールドを使用してください。",
                    codeSnippet = line.trim()
                ))
            }
        }

        return results
    }

    // Unityライフサイクル問題の検出
    private fun detectUnityLifecycleIssues(content: String, lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // null比較のアンチパターン
            if ((line.contains("== null") || line.contains("!= null")) &&
                (line.contains("GameObject") || line.contains("Transform") || line.contains("Component"))) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("null"),
                    severity = ReviewSeverity.INFO,
                    ruleId = "UNITY_LIFECYCLE_001",
                    message = "UnityオブジェクトのNull比較は特殊です。意図的でない場合は注意が必要です。",
                    suggestion = "UnityのライフサイクルとNull比較について確認してください。",
                    codeSnippet = line.trim()
                ))
            }
        }

        return results
    }

    // Unityセキュリティ問題の検出
    private fun detectUnitySecurityIssues(lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // Debug.Log in production
            if (line.contains("Debug.Log") && !line.contains("//")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("Debug.Log"),
                    severity = ReviewSeverity.INFO,
                    ruleId = "UNITY_SECURITY_001",
                    message = "Debug.Logがプロダクションコードに含まれています。",
                    suggestion = "#if UNITY_EDITOR または条件付きコンパイルの使用を検討してください。",
                    codeSnippet = line.trim()
                ))
            }

            // PlayerPrefs without encryption
            if (line.contains("PlayerPrefs.SetString") && line.contains("password", true)) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("PlayerPrefs"),
                    severity = ReviewSeverity.ERROR,
                    ruleId = "UNITY_SECURITY_002",
                    message = "パスワードがPlayerPrefsに平文保存される可能性があります。",
                    suggestion = "機密情報は暗号化して保存してください。",
                    codeSnippet = line.trim()
                ))
            }
        }

        return results
    }

    // Unityメモリ問題の検出
    private fun detectUnityMemoryIssues(content: String, lines: List<String>): List<AnalysisResult> {
        val results = mutableListOf<AnalysisResult>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // Coroutine停止忘れ
            if (line.contains("StartCoroutine")) {
                results.add(AnalysisResult(
                    lineNumber = lineNumber,
                    columnNumber = line.indexOf("StartCoroutine"),
                    severity = ReviewSeverity.INFO,
                    ruleId = "UNITY_MEMORY_001",
                    message = "StartCoroutineを使用しています。適切に停止処理が実装されているか確認してください。",
                    suggestion = "OnDisable()やOnDestroy()でStopCoroutine()またはStopAllCoroutines()を呼び出してください。",
                    codeSnippet = line.trim()
                ))
            }
        }

        return results
    }

    private fun isInUpdateMethod(lines: List<String>, currentIndex: Int): Boolean {
        for (i in currentIndex downTo 0) {
            val line = lines[i]
            if (line.contains("void Update()") ||
                line.contains("void FixedUpdate()") ||
                line.contains("void LateUpdate()")) {
                return true
            }
            if (line.contains("void ") && line.contains("()")) {
                return false
            }
        }
        return false
    }
}