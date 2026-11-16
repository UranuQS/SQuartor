package com.squartor.reader.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class BookFormat {
    epub, txt;

    companion object {
        fun fromName(name: String?): BookFormat =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: txt
    }
}

@Immutable
data class ReaderChapter(
    val title: String,
    val href: String,
    val filePath: String,
    val anchor: String? = null,
    val endAnchor: String? = null,
    val tocDepth: Int = 0,
    val cachedPageCount: Int? = null,
    val wordCount: Int? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("href", href)
        put("filePath", filePath)
        put("anchor", anchor ?: JSONObject.NULL)
        put("endAnchor", endAnchor ?: JSONObject.NULL)
        put("tocDepth", tocDepth)
        put("cachedPageCount", cachedPageCount ?: JSONObject.NULL)
        put("wordCount", wordCount ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(obj: JSONObject): ReaderChapter = ReaderChapter(
            title = obj.optString("title", "章节").ifEmpty { "章节" },
            href = obj.optString("href", ""),
            filePath = obj.optString("filePath", ""),
            anchor = obj.optString("anchor").takeIf { it.isNotEmpty() && !obj.isNull("anchor") },
            endAnchor = obj.optString("endAnchor").takeIf { it.isNotEmpty() && !obj.isNull("endAnchor") },
            tocDepth = obj.optInt("tocDepth", 0),
            cachedPageCount = if (obj.isNull("cachedPageCount") || !obj.has("cachedPageCount")) null else obj.optInt("cachedPageCount"),
            wordCount = if (obj.isNull("wordCount") || !obj.has("wordCount")) null else obj.optInt("wordCount"),
        )
    }
}

@Immutable
data class BookBookmark(
    val id: String,
    val chapterIndex: Int,
    val page: Int,
    val pageCount: Int,
    val progress: Double,
    val createdAtMillis: Long,
    val snippet: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("chapterIndex", chapterIndex)
        put("page", page)
        put("pageCount", pageCount)
        put("progress", progress)
        put("createdAt", formatIso8601(createdAtMillis))
        put("snippet", snippet)
    }

    companion object {
        fun fromJson(obj: JSONObject): BookBookmark = BookBookmark(
            id = obj.optString("id", ""),
            chapterIndex = obj.optInt("chapterIndex", 0),
            page = obj.optInt("page", 0),
            pageCount = obj.optInt("pageCount", 1).coerceAtLeast(1),
            progress = obj.optDouble("progress", 0.0).let { if (it.isNaN()) 0.0 else it.coerceIn(0.0, 1.0) },
            createdAtMillis = parseIso8601(obj.optString("createdAt")) ?: System.currentTimeMillis(),
            snippet = obj.optString("snippet", ""),
        )
    }
}

@Immutable
data class BookEntry(
    val id: String,
    val title: String,
    val author: String,
    val format: BookFormat,
    val bookDir: String,
    val sourcePath: String,
    val importedAtMillis: Long,
    val chapters: List<ReaderChapter>,
    val coverPath: String? = null,
    val currentChapterIndex: Int = 0,
    val currentPage: Int = 0,
    val pageCount: Int = 1,
    val progress: Double = 0.0,
    val lastReadAtMillis: Long? = null,
    val shelfName: String? = null,
    val wordCount: Int? = null,
    val seriesOverride: String? = null,
    val seriesOrder: Int? = null,
    val bookmarks: List<BookBookmark> = emptyList(),
) {
    val formatLabel: String get() = if (format == BookFormat.epub) "EPUB" else "TXT"

    val safeCurrentChapter: ReaderChapter
        get() = if (chapters.isEmpty()) {
            ReaderChapter(title = "正文", href = "", filePath = "")
        } else {
            chapters[currentChapterIndex.coerceIn(0, chapters.size - 1)]
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("author", author)
        put("format", format.name)
        put("bookDir", bookDir)
        put("sourcePath", sourcePath)
        put("importedAt", formatIso8601(importedAtMillis))
        put("chapters", JSONArray().apply { chapters.forEach { put(it.toJson()) } })
        put("coverPath", coverPath ?: JSONObject.NULL)
        put("currentChapterIndex", currentChapterIndex)
        put("currentPage", currentPage)
        put("pageCount", pageCount)
        put("progress", progress)
        put("lastReadAt", lastReadAtMillis?.let { formatIso8601(it) } ?: JSONObject.NULL)
        put("shelfName", shelfName ?: JSONObject.NULL)
        put("wordCount", wordCount ?: JSONObject.NULL)
        put("seriesOverride", seriesOverride ?: JSONObject.NULL)
        put("seriesOrder", seriesOrder ?: JSONObject.NULL)
        put("bookmarks", JSONArray().apply { bookmarks.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(obj: JSONObject): BookEntry {
            val chaptersArr = obj.optJSONArray("chapters")
            val chapters = mutableListOf<ReaderChapter>()
            if (chaptersArr != null) {
                for (i in 0 until chaptersArr.length()) {
                    chaptersArr.optJSONObject(i)?.let { chapters.add(ReaderChapter.fromJson(it)) }
                }
            }
            val bookmarksArr = obj.optJSONArray("bookmarks")
            val bookmarks = mutableListOf<BookBookmark>()
            if (bookmarksArr != null) {
                for (i in 0 until bookmarksArr.length()) {
                    bookmarksArr.optJSONObject(i)?.let {
                        val bm = BookBookmark.fromJson(it)
                        if (bm.id.isNotEmpty()) bookmarks.add(bm)
                    }
                }
            }
            return BookEntry(
                id = obj.optString("id", ""),
                title = obj.optString("title", "未命名书籍").ifEmpty { "未命名书籍" },
                author = obj.optString("author", "未知作者").ifEmpty { "未知作者" },
                format = BookFormat.fromName(obj.optString("format")),
                bookDir = obj.optString("bookDir", ""),
                sourcePath = obj.optString("sourcePath", ""),
                importedAtMillis = parseIso8601(obj.optString("importedAt")) ?: System.currentTimeMillis(),
                chapters = chapters,
                coverPath = obj.optString("coverPath").takeIf { it.isNotEmpty() && !obj.isNull("coverPath") },
                currentChapterIndex = obj.optInt("currentChapterIndex", 0),
                currentPage = obj.optInt("currentPage", 0),
                pageCount = obj.optInt("pageCount", 1).coerceAtLeast(1),
                progress = obj.optDouble("progress", 0.0).let { if (it.isNaN()) 0.0 else it.coerceIn(0.0, 1.0) },
                lastReadAtMillis = if (obj.isNull("lastReadAt")) null else parseIso8601(obj.optString("lastReadAt")),
                shelfName = obj.optString("shelfName").takeIf { it.isNotEmpty() && !obj.isNull("shelfName") },
                wordCount = if (obj.isNull("wordCount") || !obj.has("wordCount")) null else obj.optInt("wordCount"),
                seriesOverride = obj.optString("seriesOverride").takeIf { it.isNotEmpty() && !obj.isNull("seriesOverride") },
                seriesOrder = if (obj.isNull("seriesOrder") || !obj.has("seriesOrder")) null else obj.optInt("seriesOrder"),
                bookmarks = bookmarks,
            )
        }
    }
}

@Immutable
data class ShelfBookBlock(
    val key: String,
    val title: String,
    val author: String,
    val books: List<BookEntry>,
) {
    val progress: Double = if (books.isEmpty()) 0.0 else books.sumOf { it.progress } / books.size
    val lastReadAtMillis: Long? = run {
        var maxVal: Long? = null
        for (book in books) {
            val t = book.lastReadAtMillis ?: continue
            if (maxVal == null || t > maxVal) maxVal = t
        }
        maxVal
    }
    val importedAtMillis: Long = run {
        var maxVal = 0L
        for (book in books) {
            if (book.importedAtMillis > maxVal) maxVal = book.importedAtMillis
        }
        maxVal
    }
}

data class ImportedFont(
    val name: String,
    val path: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("path", path)
    }

    companion object {
        fun fromJson(obj: JSONObject): ImportedFont = ImportedFont(
            name = obj.optString("name", "自定义字体").ifEmpty { "自定义字体" },
            path = obj.optString("path", ""),
        )
    }
}

data class StorageStats(
    val booksBytes: Long,
    val cacheBytes: Long,
) {
    val totalBytes: Long get() = booksBytes + cacheBytes

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024L) return "$bytes B"
            if (bytes < 1024L * 1024L) return String.format("%.1f KB", bytes / 1024.0)
            if (bytes < 1024L * 1024L * 1024L) return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

const val DEFAULT_SHELF_NAME = "__all__"
const val DEFAULT_SHELF_LABEL = "全部"

private val SXSY_PREFIX_REGEX = Regex("^sxsy[_-]org\\s*", RegexOption.IGNORE_CASE)

fun cleanBookTitle(raw: String): String {
    var s = raw.trim()
    if (s.startsWith("sxsy", ignoreCase = true)) {
        s = s.replace(SXSY_PREFIX_REGEX, "")
    }
    if (s.startsWith("【") && s.endsWith("】") && s.length > 2) {
        val inner = s.substring(1, s.length - 1)
        if (!inner.contains("【") && !inner.contains("】")) {
            s = inner.trim()
        }
    }
    return s.ifEmpty { raw }
}

fun parseIso8601(raw: String?): Long? {
    if (raw.isNullOrBlank() || raw == "null") return null
    return try {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (_: Throwable) {
        try {
            LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Throwable) {
            try {
                Instant.parse(raw).toEpochMilli()
            } catch (_: Throwable) {
                null
            }
        }
    }
}

fun formatIso8601(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
