package com.squartor.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubStreamEngineTest {

    @Test
    fun normalizeAndResolveEpubHref_handlesRelativeSegmentsAndExcessiveDotDot() {
        assertEquals(
            "OEBPS/Images/cover.jpg",
            EpubStreamEngine.resolveEpubHref("OEBPS/Text", "../Images/cover.jpg"),
        )
        assertEquals(
            "Images/cover.jpg",
            EpubStreamEngine.resolveEpubHref("OEBPS/Text", "../../../Images/cover.jpg"),
        )
        assertEquals(
            "OEBPS/Text/ch2.xhtml#sec-2",
            EpubStreamEngine.resolveEpubHref("OEBPS/Text", "./ch2.xhtml#sec-2"),
        )
    }

    @Test
    fun findZipEntry_matchesSegmentBoundariesWithoutSubstringFalsePositives() {
        val entries = listOf(
            ZipEntry("OEBPS/Images/my_cover.jpg"),
            ZipEntry("OEBPS/Images/11.jpg"),
            ZipEntry("OEBPS/Images/ch1/fig1.png"),
        )

        // Must NOT false-match my_cover.jpg when looking for cover.jpg
        assertNull(EpubStreamEngine.findZipEntry(entries, "cover.jpg"))

        // Must NOT false-match 11.jpg when looking for 1.jpg
        assertNull(EpubStreamEngine.findZipEntry(entries, "1.jpg"))

        // Excess ../ stripped to Images/ch1/fig1.png MUST match OEBPS/Images/ch1/fig1.png on '/' boundary
        val matched = EpubStreamEngine.findZipEntry(entries, "Images/ch1/fig1.png")
        assertNotNull(matched)
        assertEquals("OEBPS/Images/ch1/fig1.png", matched?.name)
    }

    @Test
    fun parseHtmlToBlocks_preservesInlineStylesAlignmentAndExactOffsets() {
        val html = """
            <html><body>
              <h2 style="text-align: center">第一章 序幕</h2>
              <p>这是   <b>加粗<i>斜体</i></b> 与 <span style="text-decoration: underline;">下划线</span> 以及 <code>inline()</code> 测试。</p>
              <blockquote><p>引用段落内容</p></blockquote>
            </body></html>
        """.trimIndent()

        val blocks = EpubStreamEngine.parseHtmlToBlocks(
            html = html,
            epubPath = "/tmp/demo.epub",
            entryHref = "OEBPS/Text/ch1.xhtml",
            startAnchor = null,
            endAnchor = null,
        )

        assertEquals(3, blocks.size)
        assertEquals("heading", blocks[0].kind)
        assertEquals("center", blocks[0].align)
        assertEquals("第一章 序幕", blocks[0].text)

        val p = blocks[1]
        assertEquals("paragraph", p.kind)
        assertEquals("这是 加粗斜体 与 下划线 以及 inline() 测试。", p.text)

        val boldSpan = p.inlineSpans.firstOrNull { it.kind == InlineStyleKind.BOLD }
        assertNotNull(boldSpan)
        assertEquals("加粗斜体", p.text.substring(boldSpan!!.start, boldSpan.end))

        val italicSpan = p.inlineSpans.firstOrNull { it.kind == InlineStyleKind.ITALIC }
        assertNotNull(italicSpan)
        assertEquals("斜体", p.text.substring(italicSpan!!.start, italicSpan.end))

        val underlineSpan = p.inlineSpans.firstOrNull { it.kind == InlineStyleKind.UNDERLINE }
        assertNotNull(underlineSpan)
        assertEquals("下划线", p.text.substring(underlineSpan!!.start, underlineSpan.end))

        val codeSpan = p.inlineSpans.firstOrNull { it.kind == InlineStyleKind.CODE }
        assertNotNull(codeSpan)
        assertEquals("inline()", p.text.substring(codeSpan!!.start, codeSpan.end))

        assertEquals("quote", blocks[2].kind)
        assertEquals("引用段落内容", blocks[2].text)
    }

    @Test
    fun parseHtmlToBlocks_respectsIdAndSyntheticTitleBoundariesWithoutOvershoot() {
        val html = """
            <html><body>
              <h1 id="c1">第一章 起始</h1>
              <p>第一章正文甲。</p>
              <h1>第二章 转折</h1>
              <p>第二章正文乙。</p>
            </body></html>
        """.trimIndent()

        val ch1 = EpubStreamEngine.parseHtmlToBlocks(
            html = html,
            epubPath = "",
            entryHref = "text/part1.xhtml",
            startAnchor = "c1",
            endAnchor = "@title:第二章 转折",
        )
        assertEquals(listOf("第一章 起始", "第一章正文甲。"), ch1.map { it.text })

        val ch2 = EpubStreamEngine.parseHtmlToBlocks(
            html = html,
            epubPath = "",
            entryHref = "text/part1.xhtml",
            startAnchor = "@title:第二章 转折",
            endAnchor = null,
        )
        assertEquals(listOf("第二章 转折", "第二章正文乙。"), ch2.map { it.text })
    }

    @Test
    fun importEpubAndReadImageBytes_roundTripHandlesEncodedPathsAndMultiChapterSpine() {
        val tempRoot = Files.createTempDirectory("squartor_epub_test").toFile()
        try {
            val epubFile = File(tempRoot, "sample.epub")
            val fakePngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

            ZipOutputStream(epubFile.outputStream()).use { zos ->
                fun putText(name: String, content: String) {
                    zos.putNextEntry(ZipEntry(name))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
                putText(
                    "META-INF/container.xml",
                    """
                    <?xml version="1.0"?>
                    <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                      <rootfiles>
                        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                      </rootfiles>
                    </container>
                    """.trimIndent()
                )
                putText(
                    "OEBPS/content.opf",
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
                      <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:title>测试书目</dc:title>
                        <dc:creator>测试作者</dc:creator>
                      </metadata>
                      <manifest>
                        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                        <item id="s1" href="Text/combined.xhtml" media-type="application/xhtml+xml"/>
                        <item id="img1" href="Images/illu 01.png" media-type="image/png"/>
                      </manifest>
                      <spine toc="ncx">
                        <itemref idref="s1"/>
                      </spine>
                    </package>
                    """.trimIndent()
                )
                putText(
                    "OEBPS/toc.ncx",
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                      <navMap>
                        <navPoint id="n1" playOrder="1">
                          <navLabel><text>第一章 晨光</text></navLabel>
                          <content src="Text/combined.xhtml"/>
                        </navPoint>
                        <navPoint id="n2" playOrder="2">
                          <navLabel><text>第二章 暮色</text></navLabel>
                          <content src="Text/combined.xhtml"/>
                        </navPoint>
                      </navMap>
                    </ncx>
                    """.trimIndent()
                )
                putText(
                    "OEBPS/Text/combined.xhtml",
                    """
                    <html xmlns="http://www.w3.org/1999/xhtml"><body>
                      <h1>第一章 晨光</h1>
                      <p>第一段文字。</p>
                      <p><img src="../Images/illu%2001.png#view"/></p>
                      <h1>第二章 暮色</h1>
                      <p>第二段文字。</p>
                    </body></html>
                    """.trimIndent()
                )
                zos.putNextEntry(ZipEntry("OEBPS/Images/illu 01.png"))
                zos.write(fakePngBytes)
                zos.closeEntry()
            }

            val bookDir = File(tempRoot, "book_out")
            val book = EpubStreamEngine.importEpub(epubFile, bookDir, "test-id")
            assertEquals(2, book.chapters.size)

            val ch1Blocks = EpubStreamEngine.readChapterBlocks(book.chapters[0], book.sourcePath)
            assertTrue(ch1Blocks.none { it.text.contains("第二段文字") })
            val imgBlock = ch1Blocks.firstOrNull { it.kind == "image" }
            assertNotNull(imgBlock)

            val loadedBytes = EpubStreamEngine.readEpubImageBytes(imgBlock!!.imageSrc!!)
            assertNotNull(loadedBytes)
            assertEquals(fakePngBytes.toList(), loadedBytes!!.toList())

            val ch2Blocks = EpubStreamEngine.readChapterBlocks(book.chapters[1], book.sourcePath)
            assertTrue(ch2Blocks.any { it.text.contains("第二段文字") })
            assertTrue(ch2Blocks.none { it.text.contains("第一段文字") })
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun zipIndexCache_preservesBoundaryMatchingAndRefreshesWhenEpubChanges() {
        val tempRoot = Files.createTempDirectory("squartor_epub_cache_test").toFile()
        try {
            val epubFile = File(tempRoot, "cached.epub")
            val imgA = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3, 4)
            val imgB = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 9, 8, 7, 6, 5, 4)

            fun writeSampleEpub(bodyText: String, imageBytes: ByteArray) {
                ZipOutputStream(epubFile.outputStream()).use { zos ->
                    fun putText(name: String, content: String) {
                        zos.putNextEntry(ZipEntry(name))
                        zos.write(content.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                    putText("OEBPS/Text/ch1.xhtml", "<html><body><p>$bodyText</p></body></html>")
                    zos.putNextEntry(ZipEntry("OEBPS/Images/my_cover.png"))
                    zos.write(imageBytes)
                    zos.closeEntry()
                }
            }

            writeSampleEpub("初始章节文本", imgA)

            // Repeated hits should return identical blocks & image bytes, while preserving segment boundary checks
            repeat(3) {
                val blocks = EpubStreamEngine.readChapterBlocks(epubFile.absolutePath, "OEBPS/Text/ch1.xhtml")
                assertEquals(listOf("初始章节文本"), blocks.map { it.text })
                assertEquals(
                    imgA.toList(),
                    EpubStreamEngine.readEpubImageBytes("sq-epub://${epubFile.absolutePath}?entry=Images/my_cover.png")?.toList(),
                )
                assertNull(
                    EpubStreamEngine.readEpubImageBytes("sq-epub://${epubFile.absolutePath}?entry=cover.png"),
                )
            }

            // Overwrite same path with distinct size & modified time so cached ZipIndex refreshes cleanly
            writeSampleEpub("更新后的扩展章节文本内容", imgB)
            epubFile.setLastModified(System.currentTimeMillis() + 5000L)

            val updatedBlocks = EpubStreamEngine.readChapterBlocks(epubFile.absolutePath, "OEBPS/Text/ch1.xhtml")
            assertEquals(listOf("更新后的扩展章节文本内容"), updatedBlocks.map { it.text })
            assertEquals(
                imgB.toList(),
                EpubStreamEngine.readEpubImageBytes("sq-epub://${epubFile.absolutePath}?entry=OEBPS/Images/my_cover.png")?.toList(),
            )
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun clearCache_evictsZipIndexEvenWhenFileMetadataUnchanged() {
        val tempRoot = Files.createTempDirectory("epub-clear-cache-test").toFile()
        try {
            val epubFile = File(tempRoot, "same-meta.epub")
            fun writeArchive(text: String) {
                ZipOutputStream(epubFile.outputStream()).use { zos ->
                    zos.putNextEntry(ZipEntry("OEBPS/ch1.xhtml"))
                    zos.write("<html><body><p>$text</p></body></html>".toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }

            // Use equal-length ASCII/UTF-8 strings and pin lastModified so only zipIndexCache eviction can expose the rewrite
            writeArchive("AlphaChapterText")
            val fixedMtime = 1_700_000_000_000L
            epubFile.setLastModified(fixedMtime)

            val first = EpubStreamEngine.readChapterBlocks(epubFile.absolutePath, "OEBPS/ch1.xhtml")
            assertEquals(listOf("AlphaChapterText"), first.map { it.text })

            EpubStreamEngine.clearCache()

            writeArchive("BravoChapterText")
            epubFile.setLastModified(fixedMtime)

            val second = EpubStreamEngine.readChapterBlocks(epubFile.absolutePath, "OEBPS/ch1.xhtml")
            assertEquals(listOf("BravoChapterText"), second.map { it.text })
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
