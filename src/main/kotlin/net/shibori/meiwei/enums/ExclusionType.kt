package net.shibori.meiwei.enums

enum class ExclusionType {
    FILE,         // ファイル除外
    DIRECTORY,    // ディレクトリ除外
    PATTERN,      // パターン除外（*.meta、*.asset等）
    EXTENSION     // 拡張子除外
}