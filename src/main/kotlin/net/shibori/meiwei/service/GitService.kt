package net.shibori.meiwei.service

import net.shibori.meiwei.common.CommandResult
import net.shibori.meiwei.enums.ExclusionType
import net.shibori.meiwei.repository.ReviewExclusionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Service
class GitService(
    private val reviewExclusionRepository: ReviewExclusionRepository,
    @Value("\${app.git.workspace-dir:./workspace}")
    private val workspaceDir: String,
    @Value("\${app.git.timeout-minutes:10}")
    private val timeoutMinutes: Long
) {

    private val logger = LoggerFactory.getLogger(GitService::class.java)

    init {
        // ワークスペースディレクトリを作成
        Files.createDirectories(Paths.get(workspaceDir))
    }

    fun cloneOrUpdateRepository(cloneUrl: String, branchName: String, forcePull: Boolean): File {
        val repositoryName = extractRepositoryName(cloneUrl)
        val repoDir = File(workspaceDir, repositoryName)

        return if (repoDir.exists() && File(repoDir, ".git").exists()) {
            logger.info("既存リポジトリを更新: ${repoDir.absolutePath}")
            updateRepository(repoDir, branchName, forcePull)
        } else {
            logger.info("リポジトリをクローン: $cloneUrl -> ${repoDir.absolutePath}")
            cloneRepository(cloneUrl, repoDir, branchName)
        }
    }

    fun getCurrentCommitHash(workDir: File): String {
        val result = executeGitCommand(workDir, listOf("rev-parse", "HEAD"))
        if (result.exitCode != 0) {
            throw RuntimeException("コミットハッシュの取得に失敗: ${result.errorOutput}")
        }
        return result.output.trim()
    }

    fun getSourceFiles(workDir: File, repositoryId: Long): List<File> {
        // 除外設定を取得
        val exclusions = reviewExclusionRepository.findByRepositoryIdAndIsActiveTrueOrderByExclusionTypeAscPathAsc(repositoryId)

        // ソースファイル拡張子
        val sourceExtensions = setOf("cs", "js", "ts", "shader", "cginc", "hlsl")

        val allFiles = mutableListOf<File>()

        workDir.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val extension = file.extension.lowercase()
                sourceExtensions.contains(extension)
            }
            .forEach { file ->
                val relativePath = workDir.toPath().relativize(file.toPath()).toString()

                // 除外設定をチェック
                if (!isPathExcluded(relativePath, exclusions)) {
                    allFiles.add(file)
                }
            }

        logger.info("取得したソースファイル数: ${allFiles.size}")
        return allFiles
    }

    private fun cloneRepository(cloneUrl: String, targetDir: File, branchName: String): File {
        // 既存ディレクトリが存在する場合は削除
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }

        // Git LFS対応のクローン
        val cloneCommand = mutableListOf("git", "clone", "--branch", branchName, cloneUrl, targetDir.absolutePath)

        val result = executeCommand(cloneCommand, File(workspaceDir))
        if (result.exitCode != 0) {
            throw RuntimeException("リポジトリのクローンに失敗: ${result.errorOutput}")
        }

        // Git LFS ファイルをプル
        pullLfsFiles(targetDir)

        return targetDir
    }

    private fun updateRepository(repoDir: File, branchName: String, forcePull: Boolean): File {
        try {
            // フェッチ
            var result = executeGitCommand(repoDir, listOf("fetch", "origin"))
            if (result.exitCode != 0) {
                logger.warn("git fetch failed: ${result.errorOutput}")
            }

            // ブランチ切り替え
            result = executeGitCommand(repoDir, listOf("checkout", branchName))
            if (result.exitCode != 0) {
                // リモートブランチから作成を試行
                result = executeGitCommand(repoDir, listOf("checkout", "-b", branchName, "origin/$branchName"))
                if (result.exitCode != 0) {
                    throw RuntimeException("ブランチの切り替えに失敗: ${result.errorOutput}")
                }
            }

            // プル実行
            val pullCommand = if (forcePull) {
                listOf("pull", "origin", branchName, "--force")
            } else {
                listOf("pull", "origin", branchName)
            }

            result = executeGitCommand(repoDir, pullCommand)
            if (result.exitCode != 0) {
                throw RuntimeException("git pull に失敗: ${result.errorOutput}")
            }

            // Git LFS ファイルをプル
            pullLfsFiles(repoDir)

            return repoDir

        } catch (e: Exception) {
            logger.error("リポジトリの更新に失敗: ${e.message}")
            throw e
        }
    }

    private fun pullLfsFiles(repoDir: File) {
        // Git LFS がセットアップされているかチェック
        val lfsCheckResult = executeGitCommand(repoDir, listOf("lfs", "env"))
        if (lfsCheckResult.exitCode != 0) {
            logger.warn("Git LFS が利用できません: ${lfsCheckResult.errorOutput}")
            return
        }

        // LFS ファイルをプル
        val lfsResult = executeGitCommand(repoDir, listOf("lfs", "pull"))
        if (lfsResult.exitCode != 0) {
            logger.warn("Git LFS pull に失敗: ${lfsResult.errorOutput}")
        } else {
            logger.info("Git LFS ファイルを取得しました")
        }
    }

    private fun executeGitCommand(workDir: File, command: List<String>): CommandResult {
        val fullCommand = listOf("git") + command
        return executeCommand(fullCommand, workDir)
    }

    private fun executeCommand(command: List<String>, workDir: File): CommandResult {
        logger.debug("Executing command: ${command.joinToString(" ")} in ${workDir.absolutePath}")

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(workDir)
        processBuilder.redirectErrorStream(false)

        // 環境変数設定（Git LFS用）
        val env = processBuilder.environment()
        env.putIfAbsent("GIT_LFS_SKIP_SMUDGE", "0")

        try {
            val process = processBuilder.start()

            // 標準出力とエラー出力を読み取り
            val outputFuture = process.inputStream.bufferedReader().use { it.readText() }
            val errorFuture = process.errorStream.bufferedReader().use { it.readText() }

            val finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES)

            if (!finished) {
                process.destroyForcibly()
                throw RuntimeException("コマンドがタイムアウトしました: ${command.joinToString(" ")}")
            }

            val result = CommandResult(
                exitCode = process.exitValue(),
                output = outputFuture,
                errorOutput = errorFuture
            )

            logger.debug("Command result: exitCode=${result.exitCode}, output=${result.output.take(200)}")

            return result

        } catch (e: IOException) {
            throw RuntimeException("コマンドの実行に失敗: ${e.message}", e)
        }
    }

    private fun extractRepositoryName(cloneUrl: String): String {
        // URLからリポジトリ名を抽出
        val lastSlash = cloneUrl.lastIndexOf('/')
        val name = if (lastSlash >= 0) {
            cloneUrl.substring(lastSlash + 1)
        } else {
            cloneUrl
        }

        // .git拡張子を削除
        return if (name.endsWith(".git")) {
            name.substring(0, name.length - 4)
        } else {
            name
        }
    }

    private fun isPathExcluded(filePath: String, exclusions: List<net.shibori.meiwei.entity.ReviewExclusion>): Boolean {
        for (exclusion in exclusions) {
            when (exclusion.exclusionType) {
                ExclusionType.FILE -> {
                    if (filePath == exclusion.path) return true
                }
                ExclusionType.DIRECTORY -> {
                    if (filePath.startsWith(exclusion.path + "/") || filePath.startsWith(exclusion.path + "\\")) return true
                }
                ExclusionType.PATTERN -> {
                    if (exclusion.pattern != null && matchesPattern(filePath, exclusion.pattern!!)) return true
                }
                ExclusionType.EXTENSION -> {
                    if (filePath.endsWith(".${exclusion.path}")) return true
                }
            }
        }
        return false
    }

    private fun matchesPattern(filePath: String, pattern: String): Boolean {
        // シンプルなワイルドカードパターンマッチング
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
            .toRegex()

        return regex.matches(filePath)
    }
}