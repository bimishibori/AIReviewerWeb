package net.shibori.meiwei.common

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String
) {
    val isSuccess: Boolean
        get() = exitCode == 0
}