package com.squartor.reader.ui

import com.squartor.reader.data.InlineStyleKind
import com.squartor.reader.data.InlineStyleSpan
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.BookFormat
import com.squartor.reader.ui.glass.SimpleSpring
import com.squartor.reader.ui.reader.ComposedPage
import com.squartor.reader.ui.reader.NativeStaticLayoutPager
import com.squartor.reader.ui.reader.computeOverallReaderProgress
import com.squartor.reader.ui.screens.buildShelfBookBlocks
import com.squartor.reader.ui.screens.parseChineseInt
import com.squartor.reader.ui.screens.refreshShelfBlocksIfStructureUnchanged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTypographyAndShelfTest {

    @Test
    fun applyCjkKinsokuWithSpans_insertsWordJoinersAndPreservesExactSpanSubstrings() {
        val raw = "　　“你好，世界！”——《春日》"
        // Span covering "世界"
        val worldStart = raw.indexOf("世界")
        val spans = listOf(
            InlineStyleSpan(start = worldStart, end = worldStart + 2, kind = InlineStyleKind.BOLD),
        )

        val (kinsoku, remapped) = NativeStaticLayoutPager.applyCjkKinsokuWithSpans(raw, spans)
        val wj = NativeStaticLayoutPager.WORD_JOINER

        // Opening quote “ binds to following char 你
        assertTrue(kinsoku.contains("“${wj}你"))
        // Comma ， binds to preceding char 好
        assertTrue(kinsoku.contains("好${wj}，"))
        // Double em-dash —— binds together
        assertTrue(kinsoku.contains("—${wj}—"))
        // Remapped span still points strictly to "世界"
        assertEquals(1, remapped.size)
        val cleanSpanText = kinsoku.substring(remapped[0].start, remapped[0].end).replace(wj.toString(), "")
        assertEquals("世界", cleanSpanText)
    }

    @Test
    fun computeCjkKernAdjustments_compressesConsecutiveFullwidthPunctuationAcrossWordJoiners() {
        val raw = "他说：“《海边的卡夫卡》很好看”。"
        val (kinsoku, _) = NativeStaticLayoutPager.applyCjkKinsokuWithSpans(raw)
        val adjustments = NativeStaticLayoutPager.computeCjkKernAdjustments(kinsoku)

        // Expect adjustments for ：(before “), 《(after “), and ”(before 。)
        val adjustedChars = adjustments.map { kinsoku[it.index] }.toSet()
        assertTrue(adjustedChars.contains('：'))
        assertTrue(adjustedChars.contains('《'))
        assertTrue(adjustedChars.contains('”'))
        assertTrue(adjustments.all { it.advanceEm in 0.5f..0.7f })
    }

    @Test
    fun simpleSpring_analyticalSolverConvergesStablyEvenUnderLargeFrameStalls() {
        val spring = SimpleSpring(0f)
        spring.springTo(newTarget = 100f, durationMs = 300, bounce = 0.22)
        assertTrue(spring.isActive)

        var sawOvershoot = false
        for (frame in 0 until 120) {
            // Alternate between 120Hz (8.3ms) and heavy 85ms frame stalls
            val dt = if (frame % 7 == 0) 0.085f else 0.0083f
            spring.step(dt)
            assertFalse("Spring value must never be NaN/Infinite", spring.value.isNaN() || spring.value.isInfinite())
            if (spring.value > 100.2f) {
                sawOvershoot = true
            }
            if (!spring.isActive) break
        }

        assertTrue("Underdamped spring with bounce > 0 should exhibit smooth overshoot", sawOvershoot)
        assertFalse("Spring should settle to inactive", spring.isActive)
        assertEquals(100f, spring.value, 0.01f)
    }

    @Test
    fun buildShelfBookBlocks_sortsSeriesVolumesConsistentlyWithoutComparatorAsymmetry() {
        fun book(id: String, title: String) = BookEntry(
            id = id,
            title = title,
            author = "作者甲",
            format = BookFormat.epub,
            bookDir = "/tmp/$id",
            sourcePath = "/tmp/$id/book.epub",
            importedAtMillis = 1000L,
            chapters = emptyList(),
            seriesOverride = "剑来",
        )

        val books = listOf(
            book("b3", "剑来 第三卷"),
            book("b0", "剑来 番外特典"),
            book("b10", "剑来 第10卷"),
            book("b1", "剑来 第一卷"),
            book("b2", "剑来 第2卷"),
        )

        val blocks = buildShelfBookBlocks(books)
        assertEquals(1, blocks.size)
        val orderedIds = blocks.first().books.map { it.id }
        assertEquals(listOf("b1", "b2", "b3", "b10", "b0"), orderedIds)
    }

    @Test
    fun buildShelfBookBlocks_groupsNormalizedSeriesAndPreservesUngroupedEntries() {
        fun book(
            id: String,
            title: String,
            override: String? = null,
            lastRead: Long? = null,
            progress: Double = 0.0,
        ) = BookEntry(
            id = id,
            title = title,
            author = "作者乙",
            format = BookFormat.epub,
            bookDir = "/tmp/$id",
            sourcePath = "/tmp/$id/book.epub",
            importedAtMillis = 1000L,
            lastReadAtMillis = lastRead,
            progress = progress,
            chapters = emptyList(),
            seriesOverride = override,
        )

        val blocks = buildShelfBookBlocks(
            listOf(
                book("m2", "sxsy_org 魔女之旅 第2卷", lastRead = 2000L, progress = 0.2),
                book("m1", "魔女之旅   第1卷", lastRead = 3000L, progress = 0.8),
                book("u1", "魔女之旅 第3卷", override = "__ungrouped__", lastRead = 1500L),
                book("w1", "无职转生 · 01", override = "无职转生", lastRead = 4000L),
                book("w2", "无职转生 · 02", override = "无职转生", lastRead = 1000L),
            ),
        )

        assertEquals(3, blocks.size)
        val majoBlock = blocks.first { it.title == "魔女之旅" && it.books.size == 2 }
        assertEquals(listOf("m1", "m2"), majoBlock.books.map { it.id })
        assertEquals(3000L, majoBlock.lastReadAtMillis)
        assertEquals(0.5, majoBlock.progress, 0.0001)

        val mushokuBlock = blocks.first { it.title == "无职转生" }
        assertEquals(listOf("w1", "w2"), mushokuBlock.books.map { it.id })
        assertEquals(4000L, mushokuBlock.lastReadAtMillis)

        val ungroupedBlock = blocks.first { it.key == "single::u1" }
        assertEquals(1, ungroupedBlock.books.size)
    }

    @Test
    fun findPageIndexForCharOffset_resolvesExactAndBoundaryOffsetsSafely() {
        assertEquals(0, NativeStaticLayoutPager.findPageIndexForCharOffset(emptyList(), 42))

        val pages = listOf(
            ComposedPage(0, "第一章", 0, 3, emptyList(), "p0", startCharOffset = 0, endCharOffset = 120),
            ComposedPage(0, "第一章", 1, 3, emptyList(), "p1", startCharOffset = 120, endCharOffset = 260),
            ComposedPage(0, "第一章", 2, 3, emptyList(), "p2", startCharOffset = 260, endCharOffset = 400),
        )

        assertEquals(0, NativeStaticLayoutPager.findPageIndexForCharOffset(pages, -10))
        assertEquals(0, NativeStaticLayoutPager.findPageIndexForCharOffset(pages, 0))
        assertEquals(0, NativeStaticLayoutPager.findPageIndexForCharOffset(pages, 119))
        assertEquals(1, NativeStaticLayoutPager.findPageIndexForCharOffset(pages, 120))
        assertEquals(1, NativeStaticLayoutPager.findPageIndexForCharOffset(pages, 200))
        assertEquals(2, NativeStaticLayoutPager.findPageIndexForCharOffset(pages, 260))
        assertEquals(2, NativeStaticLayoutPager.findPageIndexForCharOffset(pages, 999))
    }

    @Test
    fun parseChineseInt_handlesArabicChineseUnitsAndEmptyInputsWithoutAllocating() {
        assertEquals(null, parseChineseInt(""))
        assertEquals(null, parseChineseInt("零"))
        assertEquals(42, parseChineseInt("42"))
        assertEquals(1, parseChineseInt("一"))
        assertEquals(10, parseChineseInt("十"))
        assertEquals(14, parseChineseInt("十四"))
        assertEquals(20, parseChineseInt("二十"))
        assertEquals(25, parseChineseInt("廿五"))
        assertEquals(105, parseChineseInt("一百零五"))
        assertEquals(1234, parseChineseInt("壹仟贰佰叁拾肆"))
        assertEquals(null, parseChineseInt("外传"))
    }

    @Test
    fun refreshShelfBlocksIfStructureUnchanged_updatesMetadataInPlaceAndInvalidatesOnStructuralChange() {
        fun mkBook(
            id: String,
            title: String,
            lastRead: Long? = null,
            progress: Double = 0.0,
            override: String? = null,
        ) = BookEntry(
            id = id,
            title = title,
            author = "作者",
            format = BookFormat.epub,
            bookDir = "/tmp/$id",
            sourcePath = "/tmp/$id.epub",
            importedAtMillis = 1000L,
            lastReadAtMillis = lastRead,
            progress = progress,
            chapters = emptyList(),
            seriesOverride = override,
        )

        val initialBooks = listOf(
            mkBook("a1", "魔女之旅 第1卷", lastRead = 1000L, progress = 0.2),
            mkBook("a2", "魔女之旅 第2卷", lastRead = 1000L, progress = 0.0),
            mkBook("s1", "单本", lastRead = 500L, progress = 0.1),
        )
        val initialBlocks = buildShelfBookBlocks(initialBooks)

        // Same reference returns prevBlocks directly
        assertEquals(initialBlocks, refreshShelfBlocksIfStructureUnchanged(initialBooks, initialBooks, initialBlocks))

        // Metadata update (progress + lastReadAtMillis) refreshes block metrics without structural rebuild
        val progressUpdatedBooks = initialBooks.map {
            if (it.id == "a2") it.copy(lastReadAtMillis = 9000L, progress = 0.8) else it
        }
        val refreshed = refreshShelfBlocksIfStructureUnchanged(initialBooks, progressUpdatedBooks, initialBlocks)
        check(refreshed != null)
        assertEquals(9000L, refreshed.first { it.books.size == 2 }.lastReadAtMillis)
        assertEquals(0.5, refreshed.first { it.books.size == 2 }.progress, 0.0001)
        // Unchanged block instance is preserved directly
        assertEquals(initialBlocks.first { it.key == "single::s1" }, refreshed.first { it.key == "single::s1" })

        // Structural change (title/override/book count) returns null so caller performs full rebuild
        val overrideChanged = initialBooks.map {
            if (it.id == "a2") it.copy(seriesOverride = "__ungrouped__") else it
        }
        assertEquals(null, refreshShelfBlocksIfStructureUnchanged(initialBooks, overrideChanged, initialBlocks))
        assertEquals(null, refreshShelfBlocksIfStructureUnchanged(initialBooks, initialBooks.drop(1), initialBlocks))
    }

    @Test
    fun readerProgress_reportsExactOneHundredPercentOnLastPageAndScrollEnd() {
        // Single-chapter 1-page book -> 100%
        assertEquals(
            1.0,
            computeOverallReaderProgress(
                displayChapterIndex = 0,
                currentPageIndex = 0,
                pageCount = 1,
                chapterCount = 1,
            ),
            0.0001,
        )
        // Multi-chapter book ending with a 1-page epilogue/colophon -> 100% (was (N-1)/N before fix)
        assertEquals(
            1.0,
            computeOverallReaderProgress(
                displayChapterIndex = 13,
                currentPageIndex = 0,
                pageCount = 1,
                chapterCount = 14,
            ),
            0.0001,
        )
        // Multi-page final chapter on its final page -> 100%
        assertEquals(
            1.0,
            computeOverallReaderProgress(
                displayChapterIndex = 9,
                currentPageIndex = 7,
                pageCount = 8,
                chapterCount = 10,
            ),
            0.0001,
        )
        // Penultimate page of multi-page final chapter stays < 100%
        assertEquals(
            (9.0 + 6.0 / 7.0) / 10.0,
            computeOverallReaderProgress(
                displayChapterIndex = 9,
                currentPageIndex = 6,
                pageCount = 8,
                chapterCount = 10,
            ),
            0.0001,
        )
        // Scroll mode scrolled all the way to the bottom even when top visible item is earlier -> 100%
        assertEquals(
            1.0,
            computeOverallReaderProgress(
                displayChapterIndex = 12,
                currentPageIndex = 3,
                pageCount = 5,
                chapterCount = 14,
                isScrollAtBookEnd = true,
            ),
            0.0001,
        )
        // AppViewModel helper stays consistent with last-page 1-page chapter
        assertEquals(
            1.0,
            com.squartor.reader.ui.state.AppViewModel.computeReadingProgress(
                chapterCount = 14,
                chapterIndex = 13,
                page = 0,
                pageCount = 1,
            ),
            0.0001,
        )
    }
}
