package com.squartor.reader.data

import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.BookFormat
import com.squartor.reader.model.ReaderChapter
import com.squartor.reader.model.cleanBookTitle
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class InlineStyleKind {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    CODE,
}

data class InlineStyleSpan(
    val start: Int,
    val end: Int,
    val kind: InlineStyleKind,
)

data class EpubBlock(
    val kind: String,
    val text: String,
    val imageSrc: String? = null,
    val inlineSpans: List<InlineStyleSpan> = emptyList(),
    val align: String? = null,
)

object EpubStreamEngine {

    private data class ManifestItem(
        val id: String,
        val href: String,
        val fullHref: String,
        val mediaType: String,
        val properties: String,
    )

    private data class SpineItem(
        val idref: String,
        val linear: Boolean,
        val properties: String,
    )

    private data class TocItem(
        val title: String,
        val href: String,
        val depth: Int,
    )

    private class EpubZipIndex(
        val exactMap: Map<String, String>,
        val normalizedPairs: List<Pair<String, String>>,
        val xhtmlRawNames: List<String>,
    )

    private val zipIndexLock = Any()
    private val zipIndexCache = object : LinkedHashMap<String, EpubZipIndex>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, EpubZipIndex>?): Boolean = size > 24
    }
    private val chapterNumRegex = Regex("""(\d+)""")

    private fun getOrBuildZipIndex(epubFile: File, zip: ZipFile): EpubZipIndex {
        val key = "${epubFile.absolutePath}:${epubFile.length()}:${epubFile.lastModified()}"
        synchronized(zipIndexLock) {
            zipIndexCache[key]?.let { return it }
        }
        val exactMap = HashMap<String, String>(256)
        val normalizedPairs = ArrayList<Pair<String, String>>(256)
        val xhtmlRawNames = ArrayList<String>(64)
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            if (entry.isDirectory) continue
            val rawName = entry.name
            val entryNorm = normalizeEpubHref(rawName).lowercase()
            if (!exactMap.containsKey(entryNorm)) {
                exactMap[entryNorm] = rawName
            }
            normalizedPairs.add(entryNorm to rawName)
            if (entryNorm.endsWith(".xhtml") || entryNorm.endsWith(".html") || entryNorm.endsWith(".htm")) {
                xhtmlRawNames.add(rawName)
            }
        }
        val built = EpubZipIndex(exactMap, normalizedPairs, xhtmlRawNames)
        synchronized(zipIndexLock) {
            zipIndexCache[key] = built
        }
        return built
    }

    private fun resolveZipEntryFromIndex(
        zip: ZipFile,
        index: EpubZipIndex,
        relativeHref: String,
    ): ZipEntry? {
        if (relativeHref.isEmpty()) return null
        val cleanHref = relativeHref.substringBefore('#').substringBefore('?')
        val decodedHref = decodeLooseUriComponent(cleanHref)
        val normalized = normalizeEpubHref(decodedHref).lowercase()
        if (normalized.isEmpty()) return null

        index.exactMap[normalized]?.let { rawName ->
            zip.getEntry(rawName)?.let { return it }
        }

        val slashNormalized = "/$normalized"
        var bestRawName: String? = null
        var bestDiff = Int.MAX_VALUE
        for ((entryNorm, rawName) in index.normalizedPairs) {
            if (entryNorm.endsWith(slashNormalized)) {
                val diff = entryNorm.length - normalized.length
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestRawName = rawName
                }
            } else if (normalized.endsWith("/$entryNorm")) {
                val diff = normalized.length - entryNorm.length
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestRawName = rawName
                }
            }
        }
        if (bestRawName != null) {
            zip.getEntry(bestRawName)?.let { return it }
        }

        val base = posixBasename(normalized)
        if (base.isNotEmpty()) {
            val parentBase = posixBasename(posixDirname(normalized))
            var fallbackRawName: String? = null
            for ((entryNorm, rawName) in index.normalizedPairs) {
                if (posixBasename(entryNorm) == base) {
                    val entryParentBase = posixBasename(posixDirname(entryNorm))
                    if (parentBase.isNotEmpty() && entryParentBase == parentBase) {
                        return zip.getEntry(rawName)
                    }
                    if (fallbackRawName == null) {
                        fallbackRawName = rawName
                    }
                }
            }
            if (fallbackRawName != null) {
                return zip.getEntry(fallbackRawName)
            }
        }
        return null
    }

    private data class TocResult(
        val items: List<TocItem>,
        val docTitle: String? = null,
    )

    private data class ChapterBoundary(
        val spineIndex: Int,
        val title: String,
        val href: String,
        val anchor: String? = null,
        val depth: Int = 0,
        val order: Int = 0,
    )

    private data class HrefParts(
        val path: String,
        val fragment: String?,
    )

    private val wordCountRegex = Regex("""[\u3400-\u9FFF\uF900-\uFAFF]|[A-Za-z0-9]+""")
    private val ignoredTags = setOf("script", "style", "noscript", "template", "head")
    private val headingTags = setOf("h1", "h2", "h3", "h4", "h5", "h6")
    private val containerTags = setOf(
        "body", "main", "article", "section", "header", "footer",
        "aside", "nav", "div", "figure", "figcaption", "ul", "ol", "dl"
    )
    private val blockTags = headingTags + containerTags + setOf(
        "p", "li", "dt", "dd", "blockquote", "pre", "table", "tr", "hr", "img", "svg", "image"
    )

    fun clearCache() {
        synchronized(zipIndexLock) {
            zipIndexCache.clear()
        }
    }

    fun importEpub(
        sourceFile: File,
        bookDir: File,
        id: String,
    ): BookEntry {
        bookDir.mkdirs()
        val persistentFile = File(bookDir, "book.epub")
        val targetEpub = try {
            if (sourceFile.canonicalPath != persistentFile.canonicalPath) {
                sourceFile.copyTo(persistentFile, overwrite = true)
            }
            if (persistentFile.exists() && persistentFile.length() > 0L) persistentFile else sourceFile
        } catch (_: Throwable) {
            sourceFile
        }

        ZipFile(targetEpub).use { zip ->
            val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
            val containerEntry = findZipEntry(entries, "META-INF/container.xml")
                ?: throw IllegalArgumentException("Invalid EPUB: missing META-INF/container.xml")
            val containerXml = Jsoup.parse(readEntryString(zip, containerEntry), "", Parser.xmlParser())
            val rootFilePath = containerXml.select("rootfile")
                .mapNotNull { it.attr("full-path").takeIf(String::isNotBlank) }
                .firstOrNull()
                ?: throw IllegalArgumentException("Invalid EPUB: missing rootfile full-path")

            val opfEntry = findZipEntry(entries, rootFilePath)
                ?: throw IllegalArgumentException("Invalid EPUB: OPF file not found: $rootFilePath")
            val opfXml = Jsoup.parse(readEntryString(zip, opfEntry), "", Parser.xmlParser())
            val opfDir = posixDirname(rootFilePath)

            val rawTitle = firstTextByLocalName(opfXml, "title")?.trim()
            val rawAuthor = firstTextByLocalName(opfXml, "creator")?.trim()
            val fallbackTitle = cleanBookTitle(sourceFile.nameWithoutExtension)
            val bookTitle = cleanBookTitle(if (!rawTitle.isNullOrEmpty()) rawTitle else fallbackTitle)
            val bookAuthor = if (!rawAuthor.isNullOrEmpty()) rawAuthor else "未知作者"

            val manifest = LinkedHashMap<String, ManifestItem>()
            for (item in opfXml.getElementsByTag("item")) {
                val itemId = item.attr("id").trim()
                val href = item.attr("href").trim()
                if (itemId.isEmpty() || href.isEmpty()) continue
                manifest[itemId] = ManifestItem(
                    id = itemId,
                    href = href,
                    fullHref = normalizeEpubHref(posixJoin(opfDir, href)),
                    mediaType = item.attr("media-type").trim(),
                    properties = item.attr("properties").trim(),
                )
            }

            val spine = opfXml.getElementsByTag("itemref")
                .map { item ->
                    SpineItem(
                        idref = item.attr("idref").trim(),
                        linear = !item.attr("linear").trim().equals("no", ignoreCase = true),
                        properties = item.attr("properties").trim(),
                    )
                }
                .filter { item ->
                    val manifestItem = manifest[item.idref]
                    item.linear && manifestItem != null && isReadableDocument(manifestItem.mediaType)
                }

            val tocResult = readTocFromZip(zip, entries, manifest)

            val guideTocHref = opfXml.getElementsByTag("reference")
                .firstOrNull { it.attr("type").equals("toc", ignoreCase = true) }
                ?.attr("href")
                ?.takeIf { it.isNotBlank() }
            val normalizedGuideTocHref = guideTocHref?.let {
                normalizeEpubHref(posixJoin(opfDir, it.substringBefore('#')))
            }

            var coverPath: String? = null
            val coverEntryHref = findCoverEntryHref(opfXml, manifest)
            if (coverEntryHref != null) {
                val coverEntry = findZipEntry(entries, coverEntryHref)
                if (coverEntry != null) {
                    try {
                        val coverFile = File(bookDir, "cover.jpg")
                        zip.getInputStream(coverEntry).use { input ->
                            coverFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (coverFile.exists() && coverFile.length() > 0L) {
                            coverPath = coverFile.absolutePath
                        }
                    } catch (_: Throwable) {
                    }
                }
            }

            val readableItems = spine
                .mapNotNull { manifest[it.idref] }
                .filter { findZipEntry(entries, it.fullHref) != null }
                .ifEmpty {
                    manifest.values.filter {
                        isReadableDocument(it.mediaType) && findZipEntry(entries, it.fullHref) != null
                    }
                }

            val indexByHref = HashMap<String, Int>(readableItems.size)
            for (i in readableItems.indices) {
                indexByHref[readableItems[i].fullHref] = i
            }

            val boundaries = mutableListOf<ChapterBoundary>()
            if (tocResult.items.isNotEmpty() && readableItems.isNotEmpty()) {
                val resolvedIndices = MutableList<Int?>(tocResult.items.size) { null }
                for (i in tocResult.items.indices) {
                    val item = tocResult.items[i]
                    val parts = splitEpubHref(item.href)
                    var index = indexByHref[parts.path]
                    if (index == null) {
                        val normalizedPath = parts.path.lowercase().replace('\\', '/')
                        for ((entryKey, entryIdx) in indexByHref) {
                            val entryNorm = entryKey.lowercase().replace('\\', '/')
                            if (entryNorm.endsWith(normalizedPath) || normalizedPath.endsWith(entryNorm)) {
                                index = entryIdx
                                break
                            }
                        }
                    }
                    if (index == null) {
                        val targetBase = posixBasename(parts.path).lowercase()
                        if (targetBase.isNotEmpty()) {
                            for ((entryKey, entryIdx) in indexByHref) {
                                if (posixBasename(entryKey).lowercase() == targetBase) {
                                    index = entryIdx
                                    break
                                }
                            }
                        }
                    }
                    if (index == null) {
                        val titleLower = item.title.lowercase()
                        if (titleLower.contains("封面") || titleLower.contains("表紙") || titleLower.contains("cover")) {
                            val firstBase = posixBasename(readableItems.first().fullHref).lowercase()
                            if (firstBase.contains("cover") || firstBase.contains("title") || firstBase.startsWith("c0")) {
                                index = 0
                            }
                        }
                    }
                    resolvedIndices[i] = index
                }

                for (i in tocResult.items.indices) {
                    if (resolvedIndices[i] == null) {
                        var prevIndex: Int? = null
                        for (p in i - 1 downTo 0) {
                            if (resolvedIndices[p] != null) {
                                prevIndex = resolvedIndices[p]
                                break
                            }
                        }
                        var nextIndex: Int? = null
                        for (n in i + 1 until tocResult.items.size) {
                            if (resolvedIndices[n] != null) {
                                nextIndex = resolvedIndices[n]
                                break
                            }
                        }
                        resolvedIndices[i] = when {
                            prevIndex == null && nextIndex != null -> 0
                            prevIndex != null && nextIndex == null -> min(prevIndex + 1, readableItems.size - 1)
                            prevIndex != null && nextIndex != null -> {
                                if (nextIndex > prevIndex + 1) prevIndex + 1 else prevIndex
                            }
                            else -> min(i, readableItems.size - 1)
                        }
                    }
                }

                for (i in tocResult.items.indices) {
                    val item = tocResult.items[i]
                    val parts = splitEpubHref(item.href)
                    val index = (resolvedIndices[i] ?: 0).coerceIn(0, readableItems.size - 1)
                    boundaries.add(
                        ChapterBoundary(
                            spineIndex = index,
                            title = item.title,
                            href = item.href,
                            anchor = parts.fragment,
                            depth = item.depth,
                            order = boundaries.size,
                        )
                    )
                }

                boundaries.sortWith(compareBy<ChapterBoundary> { it.spineIndex }.thenBy { it.order })

                if (boundaries.isNotEmpty() && boundaries.first().spineIndex > 0) {
                    val firstItem = readableItems.first()
                    val firstEntry = findZipEntry(entries, firstItem.fullHref)
                    val raw = if (firstEntry != null) readEntryString(zip, firstEntry) else ""
                    val doc = Jsoup.parse(raw)
                    val heading = doc.selectFirst("h1, h2, h3, h4, h5, h6")?.text()?.trim()
                    val docTitle = doc.selectFirst("title")?.text()?.trim()
                    val chapterTitle = when {
                        !tocResult.docTitle.isNullOrEmpty() -> tocResult.docTitle
                        !heading.isNullOrEmpty() -> heading
                        !docTitle.isNullOrEmpty() -> docTitle
                        bookTitle.isNotEmpty() -> bookTitle
                        else -> "封面"
                    }
                    boundaries.add(
                        0,
                        ChapterBoundary(
                            spineIndex = 0,
                            title = chapterTitle,
                            href = firstItem.fullHref,
                            depth = 0,
                            order = -1,
                        )
                    )
                }
            }

            if (boundaries.isEmpty() && readableItems.isNotEmpty()) {
                for (i in readableItems.indices) {
                    val item = readableItems[i]
                    if (normalizedGuideTocHref != null && item.fullHref == normalizedGuideTocHref) {
                        continue
                    }
                    val entry = findZipEntry(entries, item.fullHref)
                    val raw = if (entry != null) readEntryString(zip, entry) else ""
                    val doc = Jsoup.parse(raw)
                    val heading = doc.selectFirst("h1, h2, h3, h4, h5, h6")?.text()?.trim()
                    val docTitle = doc.selectFirst("title")?.text()?.trim()
                    val chapterTitle = when {
                        !heading.isNullOrEmpty() -> heading
                        !docTitle.isNullOrEmpty() -> docTitle
                        else -> "第 ${i + 1} 节"
                    }
                    boundaries.add(
                        ChapterBoundary(
                            spineIndex = i,
                            title = chapterTitle,
                            href = item.fullHref,
                            depth = 0,
                            order = boundaries.size,
                        )
                    )
                }
            }

            val wordCountByHref = HashMap<String, Int>(readableItems.size)
            var totalEstimatedWords = 0
            for (item in readableItems) {
                val entry = findZipEntry(entries, item.fullHref)
                if (entry != null) {
                    val raw = readEntryString(zip, entry)
                    val plainText = Jsoup.parse(raw).body().text().ifEmpty { raw }
                    val count = estimateWordCount(plainText)
                    wordCountByHref[item.fullHref] = count
                    totalEstimatedWords += count
                }
            }
            if (totalEstimatedWords == 0 && targetEpub.exists()) {
                totalEstimatedWords = (targetEpub.length() * 0.38).roundToInt()
            }

            val epubAbsPath = targetEpub.absolutePath
            val dedupedBoundaries = mutableListOf<ChapterBoundary>()
            for (b in boundaries) {
                val prev = dedupedBoundaries.lastOrNull()
                if (prev != null && prev.spineIndex == b.spineIndex) {
                    val prevAnchor = prev.anchor?.trim().orEmpty()
                    val currAnchor = b.anchor?.trim().orEmpty()
                    if (currAnchor.isNotEmpty() && prevAnchor == currAnchor) {
                        continue
                    }
                    if (currAnchor.isEmpty() && prevAnchor.isEmpty()) {
                        val trimmedTitle = b.title.trim()
                        if (trimmedTitle.isEmpty() || trimmedTitle == prev.title.trim()) {
                            continue
                        }
                        dedupedBoundaries.add(b.copy(anchor = "@title:$trimmedTitle"))
                        continue
                    }
                }
                dedupedBoundaries.add(b)
            }
            val chapters = mutableListOf<ReaderChapter>()
            for (i in dedupedBoundaries.indices) {
                val boundary = dedupedBoundaries[i]
                val nextBoundary = dedupedBoundaries.getOrNull(i + 1)
                val endAnchor = if (nextBoundary?.spineIndex == boundary.spineIndex) {
                    nextBoundary.anchor ?: nextBoundary.title.trim().takeIf { it.isNotEmpty() }?.let { "@title:$it" }
                } else {
                    null
                }

                val startSpine = boundary.spineIndex
                val endSpine = when {
                    nextBoundary == null -> readableItems.size
                    nextBoundary.spineIndex == boundary.spineIndex -> boundary.spineIndex + 1
                    else -> max(boundary.spineIndex + 1, nextBoundary.spineIndex)
                }

                val safeStart = startSpine.coerceIn(0, readableItems.size)
                val safeEnd = endSpine.coerceIn(safeStart, readableItems.size)
                val chapterSpineItems = readableItems.subList(safeStart, safeEnd)
                val spineHrefs = if (chapterSpineItems.isNotEmpty()) {
                    chapterSpineItems.joinToString("|") { it.fullHref }
                } else if (readableItems.isNotEmpty()) {
                    readableItems[boundary.spineIndex.coerceIn(0, readableItems.size - 1)].fullHref
                } else {
                    boundary.href.substringBefore('#')
                }

                val chapterWords = chapterSpineItems.sumOf { wordCountByHref[it.fullHref] ?: 0 }
                chapters.add(
                    ReaderChapter(
                        title = boundary.title.ifBlank { "第 ${i + 1} 章" },
                        href = boundary.href,
                        filePath = "sq-epub://$epubAbsPath#$spineHrefs",
                        anchor = boundary.anchor,
                        endAnchor = endAnchor,
                        tocDepth = boundary.depth,
                        wordCount = chapterWords.takeIf { it > 0 },
                    )
                )
            }

            return BookEntry(
                id = id,
                title = bookTitle,
                author = bookAuthor,
                format = BookFormat.epub,
                bookDir = bookDir.absolutePath,
                sourcePath = epubAbsPath,
                importedAtMillis = System.currentTimeMillis(),
                chapters = chapters,
                coverPath = coverPath,
                wordCount = totalEstimatedWords.takeIf { it > 0 },
            )
        }
    }

    fun importEpubDirect(
        sourceFile: File,
        bookDir: File,
        id: String,
    ): BookEntry = importEpub(sourceFile, bookDir, id)

    fun extractCoverIfMissing(epubPath: String, bookDir: File): String? {
        try {
            val coverFile = File(bookDir, "cover.jpg")
            if (coverFile.exists() && coverFile.length() > 0L) {
                return coverFile.absolutePath
            }
            val resolvedFile = resolveEpubFile(epubPath, bookDir.absolutePath) ?: return null
            ZipFile(resolvedFile).use { zip ->
                val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
                val containerEntry = findZipEntry(entries, "META-INF/container.xml") ?: return null
                val containerXml = Jsoup.parse(readEntryString(zip, containerEntry), "", Parser.xmlParser())
                val opfPath = containerXml.select("rootfile")
                    .mapNotNull { it.attr("full-path").takeIf(String::isNotBlank) }
                    .firstOrNull() ?: return null
                val opfEntry = findZipEntry(entries, opfPath) ?: return null
                val opfXml = Jsoup.parse(readEntryString(zip, opfEntry), "", Parser.xmlParser())
                val opfDir = posixDirname(opfPath)
                val manifest = LinkedHashMap<String, ManifestItem>()
                for (item in opfXml.getElementsByTag("item")) {
                    val itemId = item.attr("id").trim()
                    val href = item.attr("href").trim()
                    if (itemId.isNotEmpty() && href.isNotEmpty()) {
                        manifest[itemId] = ManifestItem(
                            id = itemId,
                            href = href,
                            fullHref = normalizeEpubHref(posixJoin(opfDir, href)),
                            mediaType = item.attr("media-type").trim(),
                            properties = item.attr("properties").trim(),
                        )
                    }
                }
                val coverHref = findCoverEntryHref(opfXml, manifest) ?: return null
                val imgEntry = findZipEntry(entries, coverHref) ?: return null
                bookDir.mkdirs()
                zip.getInputStream(imgEntry).use { input ->
                    coverFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (coverFile.exists() && coverFile.length() > 0L) {
                    return coverFile.absolutePath
                }
            }
        } catch (_: Throwable) {
        }
        return null
    }

    fun readChapterBlocks(
        chapter: ReaderChapter,
        fallbackEpubPath: String = "",
    ): List<EpubBlock> {
        val rawFilePath = chapter.filePath.trim()
        // Handle legacy extracted HTML files on disk if present
        if (!rawFilePath.startsWith("sq-epub://") && rawFilePath.isNotEmpty()) {
            val directFile = File(rawFilePath)
            if (directFile.exists() && directFile.isFile && !rawFilePath.endsWith(".epub", ignoreCase = true)) {
                return try {
                    val html = directFile.readText(Charsets.UTF_8)
                    val blocks = parseHtmlToBlocks(
                        html = html,
                        epubPath = fallbackEpubPath,
                        entryHref = chapter.href.substringBefore('#'),
                        startAnchor = chapter.anchor,
                        endAnchor = chapter.endAnchor,
                    )
                    blocks.ifEmpty { listOf(EpubBlock(kind = "heading", text = chapter.title)) }
                } catch (_: Throwable) {
                    listOf(EpubBlock(kind = "heading", text = chapter.title))
                }
            }
        }

        val (parsedEpubPath, parsedHrefs) = parseChapterFilePath(chapter, fallbackEpubPath)
        val resolvedEpubPath = resolveEpubFile(parsedEpubPath)?.absolutePath
            ?: fallbackEpubPath.takeIf { it.isNotBlank() }?.let { resolveEpubFile(it)?.absolutePath }
            ?: parsedEpubPath
        val blocks = readChapterBlocks(
            epubPath = resolvedEpubPath,
            entryHref = parsedHrefs,
            startAnchor = chapter.anchor,
            endAnchor = chapter.endAnchor,
        )
        return blocks.ifEmpty {
            listOf(EpubBlock(kind = "heading", text = chapter.title))
        }
    }

    fun readChapterBlocks(
        epubPath: String,
        chapter: ReaderChapter,
    ): List<EpubBlock> = readChapterBlocks(chapter = chapter, fallbackEpubPath = epubPath)

    fun loadChapterBlocks(
        chapter: ReaderChapter,
        fallbackEpubPath: String = "",
    ): List<EpubBlock> = readChapterBlocks(chapter = chapter, fallbackEpubPath = fallbackEpubPath)

    fun loadChapterBlocks(
        book: BookEntry,
        chapterIndex: Int,
    ): List<EpubBlock> {
        if (book.chapters.isEmpty()) return emptyList()
        val chapter = book.chapters[chapterIndex.coerceIn(0, book.chapters.size - 1)]
        return readChapterBlocks(chapter = chapter, fallbackEpubPath = book.sourcePath)
    }

    fun parseChapterBlocks(
        chapter: ReaderChapter,
        fallbackEpubPath: String = "",
    ): List<EpubBlock> = readChapterBlocks(chapter = chapter, fallbackEpubPath = fallbackEpubPath)

    fun readChapterBlocks(
        epubPath: String,
        entryHref: String,
        startAnchor: String? = null,
        endAnchor: String? = null,
    ): List<EpubBlock> {
        val epubFile = resolveEpubFile(epubPath) ?: return emptyList()
        val canonicalEpubPath = epubFile.absolutePath
        val hrefList = entryHref.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        if (hrefList.isEmpty()) return emptyList()

        val result = mutableListOf<EpubBlock>()
        try {
            ZipFile(epubFile).use { zip ->
                val zipIndex = getOrBuildZipIndex(epubFile, zip)
                for ((idx, targetHref) in hrefList.withIndex()) {
                    var zipEntry = resolveZipEntryFromIndex(zip, zipIndex, targetHref)
                    if (zipEntry == null) {
                        val xhtmlNames = zipIndex.xhtmlRawNames
                        val numMatch = chapterNumRegex.find(targetHref)
                        val num = numMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                        var fallbackName: String? = null
                        if (num != null) {
                            fallbackName = xhtmlNames.getOrNull(num) ?: xhtmlNames.getOrNull(num - 1)
                        }
                        if (fallbackName == null && xhtmlNames.isNotEmpty() && hrefList.size == 1) {
                            fallbackName = xhtmlNames.first()
                        }
                        if (fallbackName != null) {
                            zipEntry = zip.getEntry(fallbackName)
                        }
                    }
                    if (zipEntry == null) continue

                    val rawHtml = readEntryString(zip, zipEntry)
                    val effectiveStartAnchor = if (idx == 0) startAnchor else null
                    val effectiveEndAnchor = if (idx == hrefList.lastIndex) endAnchor else null
                    val entryBlocks = parseHtmlToBlocks(
                        html = rawHtml,
                        epubPath = canonicalEpubPath,
                        entryHref = zipEntry.name,
                        startAnchor = effectiveStartAnchor,
                        endAnchor = effectiveEndAnchor,
                    )
                    result.addAll(entryBlocks)
                }
            }
        } catch (_: Throwable) {
        }
        return result
    }

    fun readChapterText(
        chapter: ReaderChapter,
        fallbackEpubPath: String = "",
    ): String {
        return readChapterBlocks(chapter, fallbackEpubPath)
            .mapNotNull { block ->
                when (block.kind) {
                    "image" -> null
                    else -> block.text.takeIf { it.isNotBlank() }
                }
            }
            .joinToString("\n\n")
    }

    fun readChapterText(
        epubPath: String,
        chapter: ReaderChapter,
    ): String = readChapterText(chapter = chapter, fallbackEpubPath = epubPath)

    fun readEpubImageBytes(epubPath: String, entryHref: String): ByteArray? {
        try {
            var actualEpubPath = epubPath
            var actualEntryHref = entryHref
            if (actualEpubPath.startsWith("sq-epub://")) {
                val stripped = actualEpubPath.removePrefix("sq-epub://")
                if (stripped.contains("?entry=")) {
                    actualEpubPath = stripped.substringBefore("?entry=")
                    if (actualEntryHref.isEmpty()) {
                        actualEntryHref = decodeLooseUriComponent(
                            stripped.substringAfter("?entry=").substringBefore('#')
                        )
                    }
                } else if (stripped.contains("#")) {
                    actualEpubPath = stripped.substringBefore("#")
                    if (actualEntryHref.isEmpty()) {
                        actualEntryHref = decodeLooseUriComponent(stripped.substringAfter("#"))
                    }
                } else {
                    actualEpubPath = stripped
                }
            }
            if (actualEntryHref.startsWith("sq-epub://")) {
                val stripped = actualEntryHref.removePrefix("sq-epub://")
                if (stripped.contains("?entry=")) {
                    if (actualEpubPath.isEmpty()) {
                        actualEpubPath = stripped.substringBefore("?entry=")
                    }
                    actualEntryHref = decodeLooseUriComponent(
                        stripped.substringAfter("?entry=").substringBefore('#')
                    )
                } else if (stripped.contains("#")) {
                    if (actualEpubPath.isEmpty()) {
                        actualEpubPath = stripped.substringBefore("#")
                    }
                    actualEntryHref = decodeLooseUriComponent(stripped.substringAfter("#"))
                }
            }
            actualEntryHref = actualEntryHref.substringBefore('#').trim()
            if (actualEntryHref.isEmpty()) return null
            val epubFile = resolveEpubFile(actualEpubPath) ?: return null
            ZipFile(epubFile).use { zip ->
                val zipIndex = getOrBuildZipIndex(epubFile, zip)
                val target = resolveZipEntryFromIndex(zip, zipIndex, actualEntryHref) ?: return null
                return zip.getInputStream(target).use { it.readBytes() }
            }
        } catch (_: Throwable) {
            return null
        }
    }

    fun readEpubImageBytes(imageUri: String): ByteArray? {
        return readEpubImageBytes(imageUri, "")
    }

    fun estimateWordCount(text: String): Int {
        if (text.isEmpty()) return 0
        return wordCountRegex.findAll(text).count()
    }

    private fun parseChapterFilePath(
        chapter: ReaderChapter,
        fallbackEpubPath: String,
    ): Pair<String, String> {
        val raw = chapter.filePath.trim()
        if (raw.startsWith("sq-epub://")) {
            val rest = raw.removePrefix("sq-epub://")
            return when {
                rest.contains("#") -> {
                    val pathPart = rest.substringBefore("#").ifEmpty { fallbackEpubPath }
                    val hrefPart = rest.substringAfter("#").ifEmpty { chapter.href.substringBefore('#') }
                    pathPart to hrefPart
                }
                rest.contains("?entry=") -> {
                    val pathPart = rest.substringBefore("?entry=").ifEmpty { fallbackEpubPath }
                    val hrefPart = decodeLooseUriComponent(rest.substringAfter("?entry="))
                    pathPart to hrefPart
                }
                else -> {
                    val pathPart = rest.ifEmpty { fallbackEpubPath }
                    pathPart to chapter.href.substringBefore('#')
                }
            }
        }
        return fallbackEpubPath.ifEmpty { raw } to chapter.href.substringBefore('#')
    }

    internal fun parseHtmlToBlocks(
        html: String,
        epubPath: String,
        entryHref: String,
        startAnchor: String?,
        endAnchor: String?,
    ): List<EpubBlock> {
        val doc = Jsoup.parse(html)
        val body = doc.body()
        val entryDir = posixDirname(entryHref.substringBefore('#'))

        val cleanStart = startAnchor?.trim()?.takeIf { it.isNotEmpty() && it != "top" }
        val cleanEnd = endAnchor?.trim()?.takeIf { it.isNotEmpty() }

        val startTarget = cleanStart?.takeIf { hasAnchorInTree(body, it) }
        val endTarget = cleanEnd?.takeIf { hasAnchorInTree(body, it) }

        var started = startTarget == null
        var ended = false
        val blocks = mutableListOf<EpubBlock>()

        fun buildImageUri(rawSrc: String): String? {
            val trimmed = rawSrc.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.startsWith("data:") || trimmed.startsWith("http://") ||
                trimmed.startsWith("https://") || trimmed.startsWith("sq-epub://") ||
                trimmed.startsWith("file://")
            ) {
                return trimmed
            }
            val cleanRaw = trimmed.substringBefore('#').substringBefore('?')
            if (cleanRaw.isEmpty()) return null
            val resolved = if (cleanRaw.startsWith("/")) {
                normalizeEpubHref(cleanRaw.trimStart('/'))
            } else {
                resolveEpubHref(entryDir, cleanRaw).substringBefore('#')
            }
            return if (epubPath.isNotEmpty()) {
                "sq-epub://$epubPath?entry=${encodeUriQueryComponent(resolved)}"
            } else {
                resolved
            }
        }

        fun checkNodeAnchors(node: Node): Boolean {
            if (node !is Element) return false
            if (!started) {
                if (startTarget != null && matchesElementAnchor(node, startTarget)) {
                    started = true
                }
                return false
            }
            if (endTarget != null && matchesElementAnchor(node, endTarget)) {
                ended = true
                return true
            }
            return false
        }

        class StyledLineAccumulator(
            val kind: String,
            val align: String?,
        ) {
            private val sb = StringBuilder()
            private val rawSpans = mutableListOf<InlineStyleSpan>()

            fun appendText(raw: String, activeStyles: Set<InlineStyleKind>) {
                if (raw.isEmpty()) return
                val norm = normalizeReaderPunctuation(raw.replace('\u00A0', ' '))
                val startIdx = sb.length
                for (ch in norm) {
                    if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                        if (sb.isNotEmpty() && sb[sb.length - 1] != ' ') {
                            sb.append(' ')
                        }
                    } else {
                        sb.append(ch)
                    }
                }
                val endIdx = sb.length
                if (endIdx > startIdx && activeStyles.isNotEmpty()) {
                    for (style in activeStyles) {
                        rawSpans.add(InlineStyleSpan(start = startIdx, end = endIdx, kind = style))
                    }
                }
            }

            fun flushLineToBlocks() {
                while (sb.isNotEmpty() && sb[sb.length - 1] == ' ') {
                    sb.setLength(sb.length - 1)
                }
                val text = sb.toString()
                if (text.isNotEmpty()) {
                    val len = text.length
                    val clamped = rawSpans.mapNotNull { span ->
                        val s = span.start.coerceIn(0, len)
                        val e = span.end.coerceIn(0, len)
                        if (e > s) span.copy(start = s, end = e) else null
                    }
                    blocks.add(
                        EpubBlock(
                            kind = kind,
                            text = text,
                            inlineSpans = mergeStyleSpans(clamped),
                            align = align,
                        )
                    )
                }
                sb.setLength(0)
                rawSpans.clear()
            }
        }

        fun extractInlineBuffer(
            node: Node,
            acc: StyledLineAccumulator,
            activeStyles: Set<InlineStyleKind>,
        ) {
            if (ended) return
            if (checkNodeAnchors(node)) return
            when (node) {
                is TextNode -> {
                    if (started) {
                        acc.appendText(node.wholeText, activeStyles)
                    }
                }
                is Element -> {
                    val tag = node.tagName().lowercase()
                    if (ignoredTags.contains(tag) || isFootnoteContainer(node)) return
                    if (tag == "rt" || tag == "rp") return
                    if (tag == "br") {
                        if (started) acc.flushLineToBlocks()
                        return
                    }
                    if (tag == "img" || tag == "svg" || tag == "image") {
                        if (started && !isAnnotationMarkerMedia(node)) {
                            val src = extractMediaSrc(node)
                            val resolved = src?.let(::buildImageUri)
                            if (!resolved.isNullOrEmpty()) {
                                acc.flushLineToBlocks()
                                blocks.add(
                                    EpubBlock(
                                        kind = "image",
                                        text = node.attr("alt").trim(),
                                        imageSrc = resolved,
                                        align = detectElementAlign(node) ?: acc.align,
                                    )
                                )
                            }
                        }
                        return
                    }
                    val elementStyles = inlineKindsForElement(node)
                    val mergedStyles = if (elementStyles.isEmpty()) activeStyles else activeStyles + elementStyles
                    for (child in node.childNodes()) {
                        extractInlineBuffer(child, acc, mergedStyles)
                        if (ended) return
                    }
                }
            }
        }

        fun emitFormattedElement(kind: String, element: Element) {
            val align = detectElementAlign(element)
            val rootStyles = inlineKindsForElement(element)
            val acc = StyledLineAccumulator(kind = kind, align = align)
            for (child in element.childNodes()) {
                extractInlineBuffer(child, acc, rootStyles)
                if (ended) break
            }
            acc.flushLineToBlocks()
        }

        fun walk(node: Node) {
            if (ended) return
            if (checkNodeAnchors(node)) return

            if (node is TextNode) {
                if (started) {
                    val text = cleanInlineText(node.wholeText)
                    if (text.isNotEmpty()) {
                        blocks.add(EpubBlock(kind = "paragraph", text = text))
                    }
                }
                return
            }

            if (node !is Element) return
            val tag = node.tagName().lowercase()
            if (ignoredTags.contains(tag) || isFootnoteContainer(node)) return

            if (!started && startTarget != null && !hasAnchorInTree(node, startTarget)) {
                return
            }

            when {
                headingTags.contains(tag) -> {
                    emitFormattedElement("heading", node)
                }
                tag == "blockquote" -> {
                    if (hasDirectBlockChild(node)) {
                        val before = blocks.size
                        for (child in node.childNodes()) {
                            walk(child)
                            if (ended) break
                        }
                        for (idx in before until blocks.size) {
                            if (blocks[idx].kind == "paragraph") {
                                blocks[idx] = blocks[idx].copy(kind = "quote")
                            }
                        }
                    } else {
                        emitFormattedElement("quote", node)
                    }
                }
                tag == "img" || tag == "svg" || tag == "image" -> {
                    if (started && !isAnnotationMarkerMedia(node)) {
                        val rawSrc = extractMediaSrc(node)
                        val resolved = rawSrc?.let(::buildImageUri)
                        if (!resolved.isNullOrEmpty()) {
                            blocks.add(
                                EpubBlock(
                                    kind = "image",
                                    text = node.attr("alt").trim(),
                                    imageSrc = resolved,
                                    align = detectElementAlign(node),
                                )
                            )
                        }
                    }
                }
                tag == "p" || tag == "li" || tag == "dt" || tag == "dd" -> {
                    val isQuote = node.classNames().any {
                        val lower = it.lowercase()
                        lower.contains("quote") || lower.contains("blockquote")
                    }
                    emitFormattedElement(if (isQuote) "quote" else "paragraph", node)
                }
                tag == "pre" -> {
                    if (started) {
                        val raw = normalizeReaderPunctuation(node.wholeText()).trim()
                        if (raw.isNotEmpty()) {
                            blocks.add(
                                EpubBlock(
                                    kind = "quote",
                                    text = raw,
                                    inlineSpans = listOf(
                                        InlineStyleSpan(0, raw.length, InlineStyleKind.CODE)
                                    ),
                                )
                            )
                        }
                    }
                }
                tag == "table" -> {
                    for (row in node.select("tr")) {
                        if (checkNodeAnchors(row)) return
                        if (!started) continue
                        val rowText = row.select("th, td")
                            .map { cleanInlineText(it.text()) }
                            .filter { it.isNotEmpty() }
                            .joinToString("　")
                        if (rowText.isNotEmpty()) {
                            blocks.add(EpubBlock(kind = "paragraph", text = rowText))
                        }
                    }
                }
                containerTags.contains(tag) -> {
                    if (hasDirectBlockChild(node)) {
                        for (child in node.childNodes()) {
                            walk(child)
                            if (ended) return
                        }
                    } else {
                        emitFormattedElement("paragraph", node)
                    }
                }
                else -> {
                    if (hasDirectBlockChild(node)) {
                        for (child in node.childNodes()) {
                            walk(child)
                            if (ended) return
                        }
                    } else {
                        emitFormattedElement("paragraph", node)
                    }
                }
            }
        }

        for (child in body.childNodes()) {
            walk(child)
            if (ended) break
        }

        // Fallback for CSS background-image cover pages
        if (blocks.isEmpty()) {
            val bgHref = extractCssBackgroundImage(doc, body)
            val resolvedBg = bgHref?.let(::buildImageUri)
            if (!resolvedBg.isNullOrEmpty()) {
                blocks.add(EpubBlock(kind = "image", text = "", imageSrc = resolvedBg))
            }
        }

        return blocks
    }

    private fun extractMediaSrc(element: Element): String? {
        val tag = element.tagName().lowercase()
        if (tag == "img") {
            val srcsetUrl = element.attr("srcset")
                .substringBefore(',')
                .trim()
                .substringBefore(' ')
                .trim()
            return element.attr("src")
                .ifBlank { element.attr("data-src") }
                .ifBlank { element.attr("data-original") }
                .ifBlank { srcsetUrl }
                .takeIf { it.isNotBlank() }
        }
        if (tag == "image") {
            return element.attr("href")
                .ifBlank { element.attr("xlink:href") }
                .ifBlank {
                    element.attributes().firstOrNull { it.key.endsWith(":href") }?.value ?: ""
                }
                .takeIf { it.isNotBlank() }
        }
        if (tag == "svg") {
            val imageEl = element.selectFirst("image") ?: return null
            return extractMediaSrc(imageEl)
        }
        return null
    }

    private fun extractCssBackgroundImage(doc: Document, body: Element): String? {
        val urlRegex = Regex(
            """background(?:-image)?\s*:\s*[^;}]*url\(\s*['"]?([^'")]+)['"]?\s*\)""",
            RegexOption.IGNORE_CASE
        )
        val bodyStyle = body.attr("style")
        urlRegex.find(bodyStyle)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        for (child in body.children()) {
            val style = child.attr("style")
            urlRegex.find(style)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        for (styleEl in doc.select("style")) {
            urlRegex.find(styleEl.data())?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }

    private fun hasDirectBlockChild(element: Element): Boolean {
        return element.children().any { child ->
            blockTags.contains(child.tagName().lowercase())
        }
    }

    private fun matchesElementAnchor(el: Element, anchor: String): Boolean {
        if (anchor.startsWith("@title:")) {
            val tag = el.tagName().lowercase()
            if (!headingTags.contains(tag)) return false
            val targetTitle = cleanInlineText(anchor.removePrefix("@title:"))
            return targetTitle.isNotEmpty() && cleanInlineText(el.text()) == targetTitle
        }
        val id = el.id().trim()
        val name = el.attr("name").trim()
        return id == anchor || name == anchor
    }

    private fun hasAnchorInTree(root: Element, anchor: String): Boolean {
        if (matchesElementAnchor(root, anchor)) return true
        for (el in root.allElements) {
            if (matchesElementAnchor(el, anchor)) return true
        }
        return false
    }

    private fun inlineKindsForElement(el: Element): Set<InlineStyleKind> {
        val tag = el.tagName().lowercase()
        val kinds = LinkedHashSet<InlineStyleKind>()
        when (tag) {
            "b", "strong" -> kinds.add(InlineStyleKind.BOLD)
            "i", "em", "cite", "dfn", "var" -> kinds.add(InlineStyleKind.ITALIC)
            "u", "ins" -> kinds.add(InlineStyleKind.UNDERLINE)
            "s", "strike", "del" -> kinds.add(InlineStyleKind.STRIKETHROUGH)
            "code", "kbd", "samp", "tt" -> kinds.add(InlineStyleKind.CODE)
        }
        val style = el.attr("style").lowercase()
        if (style.isNotEmpty()) {
            if (Regex("""font-weight\s*:\s*(bold|bolder|[6-9]00)""").containsMatchIn(style)) {
                kinds.add(InlineStyleKind.BOLD)
            }
            if (Regex("""font-style\s*:\s*(italic|oblique)""").containsMatchIn(style)) {
                kinds.add(InlineStyleKind.ITALIC)
            }
            if (Regex("""text-decoration[^;]*underline""").containsMatchIn(style)) {
                kinds.add(InlineStyleKind.UNDERLINE)
            }
            if (Regex("""text-decoration[^;]*line-through""").containsMatchIn(style)) {
                kinds.add(InlineStyleKind.STRIKETHROUGH)
            }
        }
        for (cls in el.classNames()) {
            when (cls.lowercase()) {
                "bold", "strong", "fw-bold", "font-bold" -> kinds.add(InlineStyleKind.BOLD)
                "italic", "em", "fst-italic" -> kinds.add(InlineStyleKind.ITALIC)
                "underline" -> kinds.add(InlineStyleKind.UNDERLINE)
                "strikethrough", "line-through" -> kinds.add(InlineStyleKind.STRIKETHROUGH)
                "code", "monospace" -> kinds.add(InlineStyleKind.CODE)
            }
        }
        return kinds
    }

    private fun detectElementAlign(element: Element): String? {
        var cur: Element? = element
        var depth = 0
        while (cur != null && depth < 3) {
            val tag = cur.tagName().lowercase()
            if (tag == "body" || tag == "html") break
            val alignAttr = cur.attr("align").trim().lowercase()
            if (alignAttr == "center" || alignAttr == "right") return alignAttr
            val style = cur.attr("style").lowercase()
            if (style.isNotEmpty()) {
                val match = Regex("""text-align\s*:\s*(center|right)""").find(style)
                if (match != null) return match.groupValues[1]
            }
            for (cls in cur.classNames()) {
                when (cls.lowercase()) {
                    "center", "text-center", "align-center", "duokan-image-single" -> return "center"
                    "right", "text-right", "align-right" -> return "right"
                }
            }
            cur = cur.parent()
            depth++
        }
        return null
    }

    internal fun mergeStyleSpans(spans: List<InlineStyleSpan>): List<InlineStyleSpan> {
        if (spans.size <= 1) return spans
        val result = mutableListOf<InlineStyleSpan>()
        for (kind in InlineStyleKind.entries) {
            val kindSpans = spans.filter { it.kind == kind && it.end > it.start }
                .sortedWith(compareBy<InlineStyleSpan> { it.start }.thenBy { it.end })
            if (kindSpans.isEmpty()) continue
            var cur = kindSpans.first()
            for (i in 1 until kindSpans.size) {
                val next = kindSpans[i]
                if (next.start <= cur.end) {
                    cur = cur.copy(end = max(cur.end, next.end))
                } else {
                    result.add(cur)
                    cur = next
                }
            }
            result.add(cur)
        }
        result.sortWith(compareBy<InlineStyleSpan> { it.start }.thenBy { it.end }.thenBy { it.kind.ordinal })
        return result
    }

    private fun encodeUriQueryComponent(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
                .replace("%2F", "/")
        } catch (_: Throwable) {
            value
        }
    }

    private fun isFootnoteContainer(element: Element): Boolean {
        val tag = element.tagName().lowercase()
        val epubType = listOf(
            element.attr("epub:type"),
            element.attr("type"),
            element.attr("role"),
        ).joinToString(" ").lowercase()
        if (epubType.contains("noteref")) return false
        val marker = listOf(
            tag,
            epubType,
            element.attr("class"),
            element.id(),
        ).joinToString(" ").lowercase()
        return (tag == "aside" && marker.contains("footnote")) ||
            marker.contains("duokan-footnote-content") ||
            marker.contains("duokan-footnote-item") ||
            marker.contains("endnote") ||
            marker.contains("sq-footnote-body")
    }

    private fun isAnnotationMarkerMedia(element: Element): Boolean {
        val mediaHref = extractMediaSrc(element) ?: ""
        val source = listOf(
            mediaHref,
            element.attr("alt"),
            element.attr("title"),
            element.attr("class"),
            element.id(),
        ).joinToString(" ").lowercase()
        val parent = element.parent()
        val parentText = if (parent != null) {
            listOf(
                parent.attr("class"),
                parent.id(),
                parent.attr("epub:type"),
                parent.attr("type"),
                parent.attr("role"),
            ).joinToString(" ").lowercase()
        } else {
            ""
        }
        val markerText = "$source $parentText"
        val looksLikeNote = Regex("""(footnote|noteref|note|annotation|marker|kobo|duokan|kindle)""").containsMatchIn(markerText) ||
            markerText.contains("注") || markerText.contains("脚") || markerText.contains("註")
        if (!looksLikeNote) return false

        val width = element.attr("width").toDoubleOrNull()
        val height = element.attr("height").toDoubleOrNull()
        if (width != null && height != null) {
            return width <= 96.0 && height <= 96.0
        }
        return Regex("""(^|/)(note|footnote|noteref|marker)\.(png|jpe?g|gif|webp|svg)$""")
            .containsMatchIn(mediaHref.lowercase())
    }

    private fun readTocFromZip(
        zip: ZipFile,
        entries: List<ZipEntry>,
        manifest: Map<String, ManifestItem>,
    ): TocResult {
        val navItem = manifest.values.firstOrNull { item ->
            item.properties.split(Regex("""\s+""")).contains("nav")
        }
        if (navItem != null) {
            val entry = findZipEntry(entries, navItem.fullHref)
            if (entry != null) {
                val document = Jsoup.parse(readEntryString(zip, entry))
                val navDocTitle = document.selectFirst("title")?.text()?.trim()
                val base = posixDirname(navItem.fullHref)
                val nav = document.selectFirst("nav[epub\\:type=toc], nav[type=toc], nav")
                val scope = nav ?: document
                val links = scope.select("a[href]")
                val items = links.map { anchor ->
                    var depth = 0
                    var parent = anchor.parent()
                    while (parent != null && parent != nav) {
                        val tag = parent.tagName().lowercase()
                        if (tag == "ol" || tag == "ul") {
                            depth += 1
                        }
                        parent = parent.parent()
                    }
                    val rawHref = anchor.attr("href").trim()
                    val title = anchor.text().trim().replace(Regex("""\s+"""), " ").ifEmpty { "章节" }
                    TocItem(
                        title = title,
                        href = resolveEpubHref(base, rawHref),
                        depth = (depth - 1).coerceIn(0, 99),
                    )
                }
                if (items.isNotEmpty()) {
                    return TocResult(items = items, docTitle = navDocTitle)
                }
            }
        }

        val ncxItem = manifest.values.firstOrNull { item ->
            item.mediaType.lowercase().contains("ncx") || item.href.lowercase().endsWith(".ncx")
        }
        if (ncxItem != null) {
            val entry = findZipEntry(entries, ncxItem.fullHref)
            if (entry != null) {
                val xml = Jsoup.parse(readEntryString(zip, entry), "", Parser.xmlParser())
                val docTitleEl = xml.getElementsByTag("docTitle").firstOrNull()
                val ncxDocTitle = docTitleEl?.let { firstTextByLocalName(it, "text")?.trim() }
                val base = posixDirname(ncxItem.fullHref)
                val items = xml.getElementsByTag("navPoint").mapNotNull { point ->
                    val labelEl = point.children().firstOrNull { it.tagName().substringAfterLast(':').equals("navLabel", ignoreCase = true) }
                    val text = (labelEl?.let { firstTextByLocalName(it, "text") }
                        ?: firstTextByLocalName(point, "text")
                        ?: "章节").trim().ifEmpty { "章节" }
                    val contentEl = point.children().firstOrNull { it.tagName().substringAfterLast(':').equals("content", ignoreCase = true) }
                        ?: point.getElementsByTag("content").firstOrNull()
                    val src = contentEl?.attr("src")?.trim() ?: ""
                    if (src.isEmpty()) return@mapNotNull null
                    var depth = 0
                    var p = point.parent()
                    while (p != null) {
                        if (p.tagName().substringAfterLast(':').equals("navPoint", ignoreCase = true)) {
                            depth += 1
                        }
                        p = p.parent()
                    }
                    TocItem(
                        title = text,
                        href = resolveEpubHref(base, src),
                        depth = depth,
                    )
                }
                if (items.isNotEmpty()) {
                    return TocResult(items = items, docTitle = ncxDocTitle)
                }
            }
        }

        return TocResult(items = emptyList())
    }

    private fun findCoverEntryHref(
        opfXml: Document,
        manifest: Map<String, ManifestItem>,
    ): String? {
        for (meta in opfXml.getElementsByTag("meta")) {
            if (meta.attr("name").equals("cover", ignoreCase = true)) {
                val content = meta.attr("content").trim()
                if (content.isNotEmpty()) {
                    manifest[content]?.let { return it.fullHref }
                    manifest.values.firstOrNull { it.href == content || it.fullHref == content }?.let {
                        return it.fullHref
                    }
                }
            }
        }
        for (item in manifest.values) {
            if (item.properties.split(Regex("""\s+""")).contains("cover-image")) {
                return item.fullHref
            }
        }
        for (item in manifest.values) {
            val lower = item.href.lowercase()
            val idLower = item.id.lowercase()
            val isImageExt = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp")
            if ((lower.contains("cover") || idLower.contains("cover")) && isImageExt) {
                return item.fullHref
            }
        }
        return null
    }

    internal fun findZipEntry(entries: List<ZipEntry>, relativeHref: String): ZipEntry? {
        if (relativeHref.isEmpty()) return null
        val cleanHref = relativeHref.substringBefore('#').substringBefore('?')
        val decodedHref = decodeLooseUriComponent(cleanHref)
        val normalized = normalizeEpubHref(decodedHref).lowercase()
        if (normalized.isEmpty()) return null
        val base = posixBasename(normalized)
        val parentBase = posixBasename(posixDirname(normalized))

        for (entry in entries) {
            val entryNorm = normalizeEpubHref(entry.name).lowercase()
            if (entryNorm == normalized) {
                return entry
            }
        }

        var bestSegmentMatch: ZipEntry? = null
        var bestSegmentDiff = Int.MAX_VALUE
        for (entry in entries) {
            val entryNorm = normalizeEpubHref(entry.name).lowercase()
            if (entryNorm.endsWith("/$normalized")) {
                val diff = entryNorm.length - normalized.length
                if (diff < bestSegmentDiff) {
                    bestSegmentDiff = diff
                    bestSegmentMatch = entry
                }
            } else if (normalized.endsWith("/$entryNorm")) {
                val diff = normalized.length - entryNorm.length
                if (diff < bestSegmentDiff) {
                    bestSegmentDiff = diff
                    bestSegmentMatch = entry
                }
            }
        }
        if (bestSegmentMatch != null) return bestSegmentMatch

        if (base.isNotEmpty()) {
            var fallbackBaseMatch: ZipEntry? = null
            for (entry in entries) {
                val entryNorm = normalizeEpubHref(entry.name).lowercase()
                if (posixBasename(entryNorm) == base) {
                    val entryParentBase = posixBasename(posixDirname(entryNorm))
                    if (parentBase.isNotEmpty() && entryParentBase == parentBase) {
                        return entry
                    }
                    if (fallbackBaseMatch == null) {
                        fallbackBaseMatch = entry
                    }
                }
            }
            if (fallbackBaseMatch != null) return fallbackBaseMatch
        }
        return null
    }

    private fun resolveEpubFile(epubPath: String, bookDirHint: String = ""): File? {
        val cleanPath = epubPath
            .removePrefix("sq-epub://")
            .substringBefore('#')
            .substringBefore("?entry=")
            .trim()
        if (cleanPath.isNotEmpty()) {
            val direct = File(cleanPath)
            if (direct.exists() && direct.isFile) return direct
            val siblingLocal = direct.parentFile?.let { File(it, "book.epub") }
            if (siblingLocal != null && siblingLocal.exists() && siblingLocal.isFile) return siblingLocal
        }
        if (bookDirHint.isNotEmpty()) {
            val inBookDir = File(bookDirHint, "book.epub")
            if (inBookDir.exists() && inBookDir.isFile) return inBookDir
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

    private fun readEntryString(zip: ZipFile, entry: ZipEntry): String {
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        return decodeTextBytes(bytes)
    }

    private fun decodeTextBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        if (bytes.size >= 3 &&
            (bytes[0].toInt() and 0xFF) == 0xEF &&
            (bytes[1].toInt() and 0xFF) == 0xBB &&
            (bytes[2].toInt() and 0xFF) == 0xBF
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0xFF && b1 == 0xFE) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            }
            if (b0 == 0xFE && b1 == 0xFF) {
                return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            }
        }
        val utf8Text = String(bytes, Charsets.UTF_8)
        val replacementCount = utf8Text.count { it == '\uFFFD' }
        if (replacementCount > 0) {
            try {
                val gbkText = String(bytes, Charset.forName("GB18030"))
                val gbkReplacements = gbkText.count { it == '\uFFFD' }
                if (gbkReplacements < replacementCount) {
                    return gbkText
                }
            } catch (_: Throwable) {
            }
        }
        return utf8Text
    }

    private fun firstTextByLocalName(root: Element, localName: String): String? {
        for (el in root.allElements) {
            if (el === root) continue
            val tagLocal = el.tagName().substringAfterLast(':')
            if (tagLocal.equals(localName, ignoreCase = true)) {
                val text = el.text().trim()
                if (text.isNotEmpty()) return text
            }
        }
        return null
    }

    private fun isReadableDocument(mediaType: String): Boolean {
        val lower = mediaType.lowercase()
        return lower.contains("xhtml") || lower == "text/html" || lower == "application/xml"
    }

    fun normalizeEpubHref(href: String): String {
        val decoded = decodeLooseUriComponent(href).replace('\\', '/')
        val parts = decoded.split('/')
        val stack = ArrayDeque<String>()
        for (part in parts) {
            if (part.isEmpty() || part == ".") continue
            if (part == "..") {
                if (stack.isNotEmpty() && stack.last() != "..") {
                    stack.removeLast()
                }
            } else {
                stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }

    fun resolveEpubHref(baseDir: String, href: String): String {
        val pathPart = href.substringBefore('#')
        val fragmentPart = href.substringAfter('#', missingDelimiterValue = "")
        val joined = if (baseDir.isEmpty() || baseDir == ".") pathPart else "$baseDir/$pathPart"
        val normalized = normalizeEpubHref(joined)
        return if (fragmentPart.isEmpty()) normalized else "$normalized#$fragmentPart"
    }

    private fun splitEpubHref(href: String): HrefParts {
        val pathPart = normalizeEpubHref(href.substringBefore('#'))
        val fragment = if (href.contains('#')) {
            decodeLooseUriComponent(href.substringAfter('#')).takeIf { it.isNotEmpty() }
        } else {
            null
        }
        return HrefParts(path = pathPart, fragment = fragment)
    }

    private fun posixDirname(path: String): String {
        val normalized = path.replace('\\', '/').trimEnd('/')
        val idx = normalized.lastIndexOf('/')
        return if (idx <= 0) "" else normalized.substring(0, idx)
    }

    private fun posixBasename(path: String): String {
        val normalized = path.replace('\\', '/').trimEnd('/')
        val idx = normalized.lastIndexOf('/')
        return if (idx < 0) normalized else normalized.substring(idx + 1)
    }

    private fun posixJoin(dir: String, rel: String): String {
        if (dir.isEmpty() || dir == ".") return rel
        if (rel.isEmpty()) return dir
        return "${dir.trimEnd('/')}/${rel.trimStart('/')}"
    }

    private fun decodeLooseUriComponent(value: String): String {
        if (!value.contains('%')) return value
        return try {
            URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
        } catch (_: Throwable) {
            val repaired = value.replace(Regex("""%(?![0-9A-Fa-f]{2})"""), "%25")
                .replace("+", "%2B")
            try {
                URLDecoder.decode(repaired, "UTF-8")
            } catch (_: Throwable) {
                value
            }
        }
    }

    private fun cleanInlineText(input: String): String {
        return normalizeReaderPunctuation(input.replace('\u00A0', ' '))
            .replace(Regex("""[ \t\r\n]+"""), " ")
            .trim()
    }

    internal fun normalizeReaderPunctuation(input: String): String {
        if (input.isEmpty()) return input
        return input
            .replace(Regex("""\uFF5C{2,}""")) { "—".repeat(it.value.length) }
            .replace('\uFF5C', '—')
            .replace(Regex("""\uFE31+""")) { "—".repeat(it.value.length) }
            .replace(Regex("""\uFE32+""")) { "–".repeat(it.value.length) }
            .replace('\uFE33', '_')
            .replace('\uFE34', '～')
            .replace('\uFE30', '‥')
            .replace('\uFE19', '…')
            .replace(Regex("""\u2502{2,}""")) { "—".repeat(it.value.length) }
            .replace(Regex("""\u4E28{2,}""")) { "—".repeat(it.value.length) }
            .replace(
                Regex("""(?<=[\u4E00-\u9FA5\u3000-\u303F\uFF00-\uFFEF])\|{2,}(?=[\u4E00-\u9FA5\u3000-\u303F\uFF00-\uFFEF])""")
            ) { "—".repeat(it.value.length) }
    }
}
