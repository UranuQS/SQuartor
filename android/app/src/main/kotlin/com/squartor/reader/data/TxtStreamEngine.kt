package com.squartor.reader.data

import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.BookFormat
import com.squartor.reader.model.ReaderChapter
import com.squartor.reader.model.cleanBookTitle
import org.jsoup.Jsoup
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import kotlin.math.min

object TxtStreamEngine {

    private const val MAX_CHAPTER_BYTES = 180_000
    private const val SAMPLE_SIZE = 64 * 1024
    private const val MAX_CACHED_FILES = 3

    private const val CN_NUM_CHARS = "零〇一二三四五六七八九十百千万两壹贰叁肆伍陆柒捌玖拾佰仟0-9０-９"

    private val decoratorPrefixPattern =
        """(?:[☆★◆◇●○◎§#*\-=—_~～]+\s*[、.．:：]?\s*)?"""

    private val volumePrefixPattern =
        """(?:(?:正文|作品相关|VIP卷|默认卷|正文卷)\s*|(?:【[^】]{1,24}】|\[[^\]]{1,24}\]|（[^）]{1,24}）)\s*|(?:第\s*[$CN_NUM_CHARS]+\s*[卷集部篇册季]\s*(?:[^\s第]{1,18}\s+)?))?"""

    private val fullLineChapterPatterns = listOf(
        Regex(
            """^\s*$decoratorPrefixPattern$volumePrefixPattern【?\s*第\s*[$CN_NUM_CHARS]+\s*[章节回卷集部篇幕话話折]\s*】?(?:$|[\s:：、．.\-—_【\[（(《「].{0,48}|[^\s，。；,.;的里中内是在有]{1,24})$"""
        ),
        Regex(
            """^\s*$decoratorPrefixPattern[$CN_NUM_CHARS]{1,6}\s*[章节回卷集部篇幕话話](?:$|[\s:：、．.\-—].{0,48})$"""
        ),
        Regex(
            """^\s*$decoratorPrefixPattern(?:chapter|chap\.?|ch\.?|part|section|sec\.?|episode|ep\.?|volume|vol\.?|book|act)\s+[0-9ivxlcdm]+(?:$|[\s:：.\-—].{0,56})$""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """^\s*$decoratorPrefixPattern(?:prologue|epilogue|interlude|preface|foreword|afterword)(?:$|[\s:：.\-—].{0,48})$""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """^\s*$decoratorPrefixPattern【?\s*(?:序章|序言|序幕|楔子|引子|前言|终章|尾声|大结局|后记|番外|番外篇|幕间|插章)\s*】?(?:$|[\s:：、．.\-—$CN_NUM_CHARS].{0,48})$"""
        ),
    )
    private val falsePositiveHeadingRegex = Regex(
        """^(?:https?://|www\.|qq|QQ群|群号|书友群|作者|PS[:：])""",
        RegexOption.IGNORE_CASE
    )
    private val dialogueStartChars = setOf('“', '"', '‘', '\'', '「', '『')
    private val proseSentenceEndRegex = Regex("""(?:[。，；]|。”|？”|！”|。」|？」|！」)$""")
    private val timeLikeRegex = Regex("""^[0-9０-９]{1,4}[:：][0-9０-９]{1,2}$""")
    private val wordCountRegex = Regex("""[\u3400-\u9FFF\uF900-\uFAFF]|[A-Za-z0-9]+""")

    private val charsetCache = LinkedHashMap<String, Charset>(16, 0.75f, true)
    private val fallbackChaptersCache = LinkedHashMap<String, List<ReaderChapter>>(MAX_CACHED_FILES, 0.75f, true)

    @Synchronized
    fun clearCache() {
        charsetCache.clear()
        fallbackChaptersCache.clear()
    }

    fun importTxt(
        sourceFile: File,
        bookDir: File,
        id: String,
    ): BookEntry {
        bookDir.mkdirs()
        val persistentFile = File(bookDir, "book.txt")
        val targetTxt = try {
            if (sourceFile.canonicalPath != persistentFile.canonicalPath) {
                sourceFile.copyTo(persistentFile, overwrite = true)
            }
            if (persistentFile.exists() && persistentFile.length() > 0L) persistentFile else sourceFile
        } catch (_: Throwable) {
            sourceFile
        }

        val bookTitle = cleanBookTitle(sourceFile.nameWithoutExtension)
        val (chapters, totalWords) = parseChaptersWithWordCount(targetTxt, bookTitle)

        return BookEntry(
            id = id,
            title = bookTitle,
            author = "本地 TXT",
            format = BookFormat.txt,
            bookDir = bookDir.absolutePath,
            sourcePath = targetTxt.absolutePath,
            importedAtMillis = System.currentTimeMillis(),
            chapters = chapters,
            wordCount = totalWords.takeIf { it > 0 },
        )
    }

    fun importTxtDirect(
        sourceFile: File,
        bookDir: File,
        id: String,
    ): BookEntry = importTxt(sourceFile, bookDir, id)

    fun parseChapters(
        sourceFile: File,
        fallbackTitle: String = cleanBookTitle(sourceFile.nameWithoutExtension),
    ): List<ReaderChapter> = parseChaptersWithWordCount(sourceFile, fallbackTitle).first

    fun parseChaptersWithWordCount(
        sourceFile: File,
        fallbackTitle: String = cleanBookTitle(sourceFile.nameWithoutExtension),
    ): Pair<List<ReaderChapter>, Int> {
        if (!sourceFile.exists() || !sourceFile.isFile) {
            return emptyList<ReaderChapter>() to 0
        }
        val rawBytes = sourceFile.readBytes()
        if (rawBytes.isEmpty()) {
            val emptyChapter = ReaderChapter(
                title = fallbackTitle.ifBlank { "正文" },
                href = "txt-0",
                filePath = "sq-txt://${sourceFile.absolutePath}#0",
                anchor = "0:0",
                wordCount = 0,
            )
            return listOf(emptyChapter) to 0
        }

        val charset = detectCharset(rawBytes)
        cacheCharset(sourceFile.absolutePath, charset)
        val bomSkip = bomByteLength(rawBytes)

        data class RawSpan(
            val title: String,
            val startByte: Int,
            val endByte: Int,
            val wordCount: Int,
        )

        val spans = mutableListOf<RawSpan>()
        var currentTitle = "正文"
        var currentPart = 1
        var hasExplicitHeading = false
        var spanStartByte = bomSkip
        var accumulatedWords = 0

        fun commitSpan(endByte: Int) {
            val safeEnd = endByte.coerceIn(spanStartByte, rawBytes.size)
            if (safeEnd <= spanStartByte && spans.isNotEmpty()) {
                return
            }
            if (accumulatedWords == 0 && safeEnd <= spanStartByte) {
                return
            }
            val baseTitle = currentTitle.trim().ifEmpty { fallbackTitle.ifEmpty { "正文" } }
            val resolvedTitle = if (currentPart <= 1) baseTitle else "$baseTitle（$currentPart）"
            spans.add(
                RawSpan(
                    title = resolvedTitle,
                    startByte = spanStartByte,
                    endByte = safeEnd,
                    wordCount = accumulatedWords,
                )
            )
            spanStartByte = safeEnd
            accumulatedWords = 0
            currentPart += 1
        }

        val step = when (charset) {
            Charsets.UTF_16LE, Charsets.UTF_16BE -> 2
            else -> 1
        }

        var cursor = bomSkip
        while (cursor < rawBytes.size) {
            val lineStart = cursor
            var lineEnd = rawBytes.size
            var nextCursor = rawBytes.size

            if (step == 1) {
                var i = cursor
                while (i < rawBytes.size) {
                    if (rawBytes[i] == 0x0A.toByte()) {
                        lineEnd = if (i > lineStart && rawBytes[i - 1] == 0x0D.toByte()) i - 1 else i
                        nextCursor = i + 1
                        break
                    }
                    i += 1
                }
            } else if (charset == Charsets.UTF_16LE) {
                var i = cursor
                while (i + 1 < rawBytes.size) {
                    if (rawBytes[i] == 0x0A.toByte() && rawBytes[i + 1] == 0x00.toByte()) {
                        lineEnd = if (i >= lineStart + 2 &&
                            rawBytes[i - 2] == 0x0D.toByte() &&
                            rawBytes[i - 1] == 0x00.toByte()
                        ) i - 2 else i
                        nextCursor = i + 2
                        break
                    }
                    i += 2
                }
            } else {
                var i = cursor
                while (i + 1 < rawBytes.size) {
                    if (rawBytes[i] == 0x00.toByte() && rawBytes[i + 1] == 0x0A.toByte()) {
                        lineEnd = if (i >= lineStart + 2 &&
                            rawBytes[i - 2] == 0x00.toByte() &&
                            rawBytes[i - 1] == 0x0D.toByte()
                        ) i - 2 else i
                        nextCursor = i + 2
                        break
                    }
                    i += 2
                }
            }

            val lineLen = (lineEnd - lineStart).coerceAtLeast(0)
            val lineStr = if (lineLen > 0) {
                String(rawBytes, lineStart, lineLen, charset).replace("\uFEFF", "").trim()
            } else {
                ""
            }

            if (lineStr.isNotEmpty()) {
                if (isChapterHeading(lineStr)) {
                    if (accumulatedWords > 0 || (hasExplicitHeading && spans.isEmpty())) {
                        commitSpan(lineStart)
                    } else {
                        spanStartByte = lineStart
                    }
                    currentTitle = normalizeHeadingTitle(lineStr)
                    currentPart = 1
                    hasExplicitHeading = true
                    accumulatedWords += estimateWordCount(lineStr)
                } else {
                    val lineWords = estimateWordCount(lineStr)
                    if (accumulatedWords > 0 && (nextCursor - spanStartByte) > MAX_CHAPTER_BYTES) {
                        commitSpan(lineStart)
                    }
                    accumulatedWords += lineWords
                }
            }

            cursor = nextCursor
        }

        if (accumulatedWords > 0 || spans.isEmpty()) {
            val finalEnd = rawBytes.size
            val baseTitle = currentTitle.trim().ifEmpty { fallbackTitle.ifEmpty { "正文" } }
            val resolvedTitle = if (currentPart <= 1) baseTitle else "$baseTitle（$currentPart）"
            spans.add(
                RawSpan(
                    title = resolvedTitle,
                    startByte = spanStartByte,
                    endByte = finalEnd,
                    wordCount = accumulatedWords,
                )
            )
        }

        val absPath = sourceFile.absolutePath
        var totalWords = 0
        val chapters = spans.mapIndexed { index, span ->
            totalWords += span.wordCount
            ReaderChapter(
                title = span.title,
                href = "txt-$index",
                filePath = "sq-txt://$absPath#$index",
                anchor = "${span.startByte}:${span.endByte}",
                wordCount = span.wordCount.takeIf { it > 0 },
            )
        }
        cacheFallbackChapters(absPath, chapters)
        return chapters to totalWords
    }

    fun readChapterText(
        chapter: ReaderChapter,
        fallbackTxtPath: String = "",
    ): String {
        val rawFilePath = chapter.filePath.trim()
        // Support legacy generated HTML chapters on disk
        if (!rawFilePath.startsWith("sq-txt://") && rawFilePath.endsWith(".html", ignoreCase = true)) {
            val htmlFile = File(rawFilePath)
            if (htmlFile.exists() && htmlFile.isFile) {
                return try {
                    val doc = Jsoup.parse(htmlFile.readText(Charsets.UTF_8))
                    val paragraphs = doc.select("p").map { it.text().trim() }.filter { it.isNotEmpty() }
                    if (paragraphs.isNotEmpty()) {
                        paragraphs.joinToString("\n\n")
                    } else {
                        doc.body().text().trim()
                    }
                } catch (_: Throwable) {
                    ""
                }
            }
        }

        val (resolvedPath, chapterIdxFromPath) = parseTxtFilePath(chapter, fallbackTxtPath)
        return readChapterText(
            txtPath = resolvedPath,
            anchor = chapter.anchor,
            chapterTitle = chapter.title,
            fallbackChapterIndex = chapterIdxFromPath,
            fallbackTxtPath = fallbackTxtPath,
        )
    }

    fun readChapterText(
        txtPath: String,
        chapter: ReaderChapter,
    ): String = readChapterText(chapter = chapter, fallbackTxtPath = txtPath)

    fun readChapterText(
        txtPath: String,
        anchor: String?,
        chapterTitle: String? = null,
        fallbackChapterIndex: Int? = null,
        fallbackTxtPath: String = "",
    ): String {
        val file = resolveTxtFile(txtPath)
            ?: fallbackTxtPath.takeIf { it.isNotBlank() }?.let(::resolveTxtFile)
            ?: return ""
        val charset = getOrDetectCharset(file)

        val byteRange = parseByteRangeAnchor(anchor, file.length())
        if (byteRange != null) {
            val rawSlice = readByteSlice(file, byteRange.first, byteRange.second)
            if (rawSlice.isNotEmpty()) {
                val decoded = String(rawSlice, charset)
                return formatChapterText(decoded, chapterTitle)
            }
        }

        // Full text / parsed chapter fallback when anchor is "top" or missing
        return try {
            val chapters = getOrParseFallbackChapters(file)
            val matchedChapter = when {
                fallbackChapterIndex != null && fallbackChapterIndex in chapters.indices ->
                    chapters[fallbackChapterIndex]
                !chapterTitle.isNullOrBlank() ->
                    chapters.firstOrNull { it.title == chapterTitle.trim() }
                        ?: chapters.firstOrNull {
                            normalizeHeadingTitle(it.title) == normalizeHeadingTitle(chapterTitle)
                        }
                else -> chapters.firstOrNull()
            }
            val matchedRange = matchedChapter?.let { parseByteRangeAnchor(it.anchor, file.length()) }
            if (matchedRange != null) {
                val slice = readByteSlice(file, matchedRange.first, matchedRange.second)
                return formatChapterText(String(slice, charset), chapterTitle ?: matchedChapter.title)
            }
            val fullText = String(file.readBytes(), charset)
            formatChapterText(fullText, chapterTitle)
        } catch (_: Throwable) {
            ""
        }
    }

    fun readChapterParagraphs(
        chapter: ReaderChapter,
        fallbackTxtPath: String = "",
    ): List<String> {
        val text = readChapterText(chapter, fallbackTxtPath)
        if (text.isBlank()) return emptyList()
        return text
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun readChapterParagraphs(
        txtPath: String,
        chapter: ReaderChapter,
    ): List<String> = readChapterParagraphs(chapter = chapter, fallbackTxtPath = txtPath)

    fun loadChapterBlocks(
        chapter: ReaderChapter,
        fallbackTxtPath: String = "",
    ): List<EpubBlock> {
        val paragraphs = readChapterParagraphs(chapter, fallbackTxtPath)
        val blocks = mutableListOf<EpubBlock>()
        if (chapter.title.isNotBlank()) {
            blocks.add(EpubBlock(kind = "heading", text = chapter.title.trim()))
        }
        for (p in paragraphs) {
            blocks.add(EpubBlock(kind = "paragraph", text = p))
        }
        return blocks
    }

    fun detectCharset(file: File): Charset {
        if (!file.exists() || !file.isFile) return Charsets.UTF_8
        return try {
            val sampleLen = min(file.length(), SAMPLE_SIZE.toLong()).toInt()
            if (sampleLen <= 0) return Charsets.UTF_8
            val buffer = ByteArray(sampleLen)
            file.inputStream().use { input ->
                var offset = 0
                while (offset < sampleLen) {
                    val read = input.read(buffer, offset, sampleLen - offset)
                    if (read <= 0) break
                    offset += read
                }
                detectCharset(buffer, offset)
            }
        } catch (_: Throwable) {
            Charsets.UTF_8
        }
    }

    fun detectCharset(bytes: ByteArray, length: Int = bytes.size): Charset {
        val len = min(length, bytes.size)
        if (len <= 0) return Charsets.UTF_8

        // 1. Check BOM
        if (len >= 3 &&
            (bytes[0].toInt() and 0xFF) == 0xEF &&
            (bytes[1].toInt() and 0xFF) == 0xBB &&
            (bytes[2].toInt() and 0xFF) == 0xBF
        ) {
            return Charsets.UTF_8
        }
        if (len >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0xFF && b1 == 0xFE) return Charsets.UTF_16LE
            if (b0 == 0xFE && b1 == 0xFF) return Charsets.UTF_16BE
        }

        // 2. Check UTF-16 without BOM heuristic (high proportion of 0x00 in even or odd positions)
        val sampleLen = min(len, SAMPLE_SIZE)
        if (sampleLen >= 16) {
            var evenZeros = 0
            var oddZeros = 0
            val pairs = sampleLen / 2
            for (i in 0 until pairs) {
                if (bytes[i * 2] == 0.toByte()) evenZeros++
                if (bytes[i * 2 + 1] == 0.toByte()) oddZeros++
            }
            if (oddZeros > pairs / 3 && evenZeros < pairs / 20) {
                return Charsets.UTF_16LE
            }
            if (evenZeros > pairs / 3 && oddZeros < pairs / 20) {
                return Charsets.UTF_16BE
            }
        }

        // 3. Check if valid UTF-8 (allowing truncated multi-byte sequence at the very end of sample)
        if (isLikelyValidUtf8(bytes, sampleLen)) {
            return Charsets.UTF_8
        }

        // 4. Compare UTF-8 vs GB18030 / GBK replacement counts
        val utf8Text = String(bytes, 0, sampleLen, Charsets.UTF_8)
        val utf8Replacements = utf8Text.count { it == '\uFFFD' }
        if (utf8Replacements > 0) {
            val gbCharset = try {
                Charset.forName("GB18030")
            } catch (_: Throwable) {
                try {
                    Charset.forName("GBK")
                } catch (_: Throwable) {
                    Charsets.UTF_8
                }
            }
            try {
                val gbText = String(bytes, 0, sampleLen, gbCharset)
                val gbReplacements = gbText.count { it == '\uFFFD' }
                if (gbReplacements <= utf8Replacements) {
                    return gbCharset
                }
            } catch (_: Throwable) {
            }
        }

        return Charsets.UTF_8
    }

    fun decodeBytes(bytes: ByteArray, length: Int = bytes.size): String {
        val len = min(length, bytes.size)
        if (len <= 0) return ""
        val charset = detectCharset(bytes, len)
        val bomSkip = bomByteLength(bytes, len)
        return String(bytes, bomSkip, (len - bomSkip).coerceAtLeast(0), charset)
    }

    fun estimateWordCount(text: String): Int {
        if (text.isEmpty()) return 0
        return wordCountRegex.findAll(text).count()
    }

    internal fun isChapterHeading(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length < 2 || trimmed.length > 64) return false
        if (trimmed.contains('\n') || trimmed.contains('\r')) return false
        if (dialogueStartChars.contains(trimmed[0])) return false
        val normalized = normalizeHeadingTitle(trimmed)
        if (falsePositiveHeadingRegex.containsMatchIn(normalized)) return false
        if (timeLikeRegex.matches(normalized)) return false
        if (proseSentenceEndRegex.containsMatchIn(normalized)) return false
        return fullLineChapterPatterns.any { it.matches(normalized) }
    }

    private fun normalizeHeadingTitle(raw: String): String {
        return raw.replace(Regex("""\s+"""), " ").trim()
    }

    private fun parseByteRangeAnchor(anchor: String?, fileLength: Long): Pair<Long, Long>? {
        if (anchor.isNullOrBlank() || !anchor.contains(':')) return null
        val parts = anchor.split(':', limit = 2)
        val start = parts[0].trim().toLongOrNull() ?: return null
        val end = parts[1].trim().toLongOrNull() ?: return null
        if (start < 0L || end <= start || start >= fileLength) return null
        val clampedEnd = min(end, fileLength)
        if (clampedEnd <= start) return null
        return start to clampedEnd
    }

    private fun readByteSlice(file: File, start: Long, end: Long): ByteArray {
        val length = (end - start).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (length <= 0) return ByteArray(0)
        return try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buffer = ByteArray(length)
                var totalRead = 0
                while (totalRead < length) {
                    val read = raf.read(buffer, totalRead, length - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }
                if (totalRead == length) buffer else buffer.copyOf(totalRead)
            }
        } catch (_: Throwable) {
            ByteArray(0)
        }
    }

    private fun formatChapterText(raw: String, chapterTitle: String?): String {
        val normalized = EpubStreamEngine.normalizeReaderPunctuation(raw)
            .replace("\uFEFF", "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val lines = normalized.split('\n')
        val cleanTitle = chapterTitle?.let(::normalizeHeadingTitle)

        val paragraphs = mutableListOf<String>()
        var skippedLeadingTitle = false
        for (rawLine in lines) {
            val trimmed = rawLine.replace('\u3000', ' ').trim()
            if (trimmed.isEmpty()) continue
            if (!skippedLeadingTitle && cleanTitle != null) {
                val normLine = normalizeHeadingTitle(trimmed)
                if (normLine == cleanTitle || cleanTitle.startsWith("$normLine（")) {
                    skippedLeadingTitle = true
                    continue
                }
                skippedLeadingTitle = true
            }
            if (trimmed.length > 2200) {
                splitUltraLongLine(trimmed, paragraphs)
            } else {
                paragraphs.add(trimmed)
            }
        }
        return if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            normalized.trim()
        }
    }

    /**
     * Splits an unwrapped ultra-long TXT line (>2200 chars) at natural sentence boundaries
     * so StaticLayout pagination stays fast and readable without altering normal lines.
     */
    private fun splitUltraLongLine(line: String, out: MutableList<String>) {
        var start = 0
        val n = line.length
        while (start < n) {
            val remaining = n - start
            if (remaining <= 2200) {
                val tail = line.substring(start).trim()
                if (tail.isNotEmpty()) out.add(tail)
                break
            }
            val minSplit = start + 800
            val maxSplit = min(start + 2000, n - 1)
            var splitAt = -1
            for (i in maxSplit downTo minSplit) {
                val ch = line[i]
                if (ch == '。' || ch == '！' || ch == '？' || ch == '；' ||
                    ch == '.' || ch == '!' || ch == '?' || ch == ';'
                ) {
                    var endIdx = i + 1
                    while (endIdx < n && (line[endIdx] == '”' || line[endIdx] == '"' ||
                            line[endIdx] == '’' || line[endIdx] == '」' || line[endIdx] == '』' ||
                            line[endIdx] == '）' || line[endIdx] == ')')
                    ) {
                        endIdx++
                    }
                    splitAt = endIdx
                    break
                }
            }
            if (splitAt <= start) {
                // Fall back to whitespace or hard split at 1800 chars
                for (i in maxSplit downTo minSplit) {
                    if (line[i].isWhitespace()) {
                        splitAt = i + 1
                        break
                    }
                }
            }
            if (splitAt <= start) {
                splitAt = min(start + 1800, n)
            }
            val chunk = line.substring(start, splitAt).trim()
            if (chunk.isNotEmpty()) out.add(chunk)
            start = splitAt
            while (start < n && line[start].isWhitespace()) start++
        }
    }

    private fun parseTxtFilePath(
        chapter: ReaderChapter,
        fallbackTxtPath: String,
    ): Pair<String, Int?> {
        val raw = chapter.filePath.trim()
        if (raw.startsWith("sq-txt://")) {
            val rest = raw.removePrefix("sq-txt://")
            val pathPart = rest.substringBefore('#').ifEmpty { fallbackTxtPath }
            val indexPart = rest.substringAfter('#', "").toIntOrNull()
                ?: chapter.href.removePrefix("txt-").substringBefore('#').toIntOrNull()
            return pathPart to indexPart
        }
        val indexFromHref = chapter.href.removePrefix("txt-").substringBefore('#').toIntOrNull()
        return fallbackTxtPath.ifEmpty { raw } to indexFromHref
    }

    private fun resolveTxtFile(txtPath: String): File? {
        val cleanPath = txtPath
            .removePrefix("sq-txt://")
            .substringBefore('#')
            .trim()
        if (cleanPath.isNotEmpty()) {
            val direct = File(cleanPath)
            if (direct.exists() && direct.isFile) return direct
            val siblingLocal = direct.parentFile?.let { File(it, "book.txt") }
            if (siblingLocal != null && siblingLocal.exists() && siblingLocal.isFile) return siblingLocal
        }
        val fileName = File(cleanPath).name
        if (fileName.isNotEmpty()) {
            val candidateDirs = listOf(
                "/storage/emulated/0/Download",
                "/sdcard/Download",
                "/storage/emulated/0/Books",
                "/storage/emulated/0/Documents",
                "/sdcard",
            )
            for (dir in candidateDirs) {
                val candidate = File(dir, fileName)
                if (candidate.exists() && candidate.isFile) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun bomByteLength(bytes: ByteArray, length: Int = bytes.size): Int {
        val len = min(length, bytes.size)
        if (len >= 3 &&
            (bytes[0].toInt() and 0xFF) == 0xEF &&
            (bytes[1].toInt() and 0xFF) == 0xBB &&
            (bytes[2].toInt() and 0xFF) == 0xBF
        ) {
            return 3
        }
        if (len >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if ((b0 == 0xFF && b1 == 0xFE) || (b0 == 0xFE && b1 == 0xFF)) {
                return 2
            }
        }
        return 0
    }

    private fun isLikelyValidUtf8(bytes: ByteArray, length: Int): Boolean {
        val safeLen = trimTrailingIncompleteUtf8(bytes, length)
        if (safeLen <= 0) return true
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes, 0, safeLen))
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun trimTrailingIncompleteUtf8(bytes: ByteArray, length: Int): Int {
        var end = min(length, bytes.size)
        if (end <= 0) return 0
        var idx = end - 1
        var continuationCount = 0
        while (idx >= 0 && continuationCount < 3) {
            val b = bytes[idx].toInt() and 0xFF
            if ((b and 0xC0) == 0x80) {
                continuationCount++
                idx--
            } else {
                val expectedTotal = when {
                    (b and 0x80) == 0x00 -> 1
                    (b and 0xE0) == 0xC0 -> 2
                    (b and 0xF0) == 0xE0 -> 3
                    (b and 0xF8) == 0xF0 -> 4
                    else -> 1
                }
                if (1 + continuationCount < expectedTotal) {
                    end = idx
                }
                break
            }
        }
        return end
    }

    @Synchronized
    private fun cacheCharset(path: String, charset: Charset) {
        charsetCache[path] = charset
    }

    @Synchronized
    private fun getOrDetectCharset(file: File): Charset {
        val cached = charsetCache[file.absolutePath]
        if (cached != null) return cached
        val detected = detectCharset(file)
        charsetCache[file.absolutePath] = detected
        return detected
    }

    @Synchronized
    private fun cacheFallbackChapters(path: String, chapters: List<ReaderChapter>) {
        if (fallbackChaptersCache.size >= MAX_CACHED_FILES && !fallbackChaptersCache.containsKey(path)) {
            val firstKey = fallbackChaptersCache.keys.firstOrNull()
            if (firstKey != null) fallbackChaptersCache.remove(firstKey)
        }
        fallbackChaptersCache[path] = chapters
    }

    private fun getOrParseFallbackChapters(file: File): List<ReaderChapter> {
        synchronized(this) {
            fallbackChaptersCache[file.absolutePath]?.let { return it }
        }
        return parseChapters(file)
    }
}
