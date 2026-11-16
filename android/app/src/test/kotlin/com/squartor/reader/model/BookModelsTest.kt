package com.squartor.reader.model

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BookModelsTest {

    @Test
    fun bookEntry_jsonRoundTripPreservesMetadataProgressAndBookmarks() {
        val rawJson = JSONObject(
            """
            {
              "id": "book-42",
              "title": "三体",
              "author": "刘慈欣",
              "format": "epub",
              "bookDir": "/data/books/book-42",
              "sourcePath": "/data/books/book-42/source.epub",
              "importedAt": "2026-10-01T10:15:30",
              "chapters": [
                {"title": "第一章 科学边界", "href": "ch1.xhtml", "filePath": "/data/books/book-42/ch1.xhtml"}
              ],
              "coverPath": "/data/books/book-42/cover.jpg",
              "currentChapterIndex": 0,
              "currentPage": 12,
              "pageCount": 48,
              "progress": 0.42,
              "lastReadAt": "2026-11-12T20:00:00",
              "shelfName": "科幻",
              "wordCount": 210000,
              "seriesOverride": "地球往事",
              "seriesOrder": 1,
              "bookmarks": [
                {"id": "bm-1", "chapterIndex": 0, "chapterTitle": "第一章", "page": 12, "excerpt": "幽灵倒计时"}
              ]
            }
            """.trimIndent(),
        )

        val entry = BookEntry.fromJson(rawJson)
        assertEquals("book-42", entry.id)
        assertEquals("三体", entry.title)
        assertEquals("EPUB", entry.formatLabel)
        assertEquals(1, entry.chapters.size)
        assertEquals(12, entry.currentPage)
        assertEquals(48, entry.pageCount)
        assertEquals(0.42, entry.progress, 0.0001)
        assertEquals("科幻", entry.shelfName)
        assertEquals(210000, entry.wordCount)
        assertEquals("地球往事", entry.seriesOverride)
        assertEquals(1, entry.seriesOrder)
        assertEquals(1, entry.bookmarks.size)

        val roundTripped = BookEntry.fromJson(entry.toJson())
        assertEquals(entry.id, roundTripped.id)
        assertEquals(entry.title, roundTripped.title)
        assertEquals(entry.progress, roundTripped.progress, 0.0001)
        assertEquals(entry.bookmarks.size, roundTripped.bookmarks.size)
        assertNotNull(roundTripped.lastReadAtMillis)
    }

    @Test
    fun bookEntry_sanitizesInvalidDefaultsAndCoercesBounds() {
        val malformed = JSONObject().apply {
            put("id", "bad-1")
            put("title", "")
            put("author", "")
            put("pageCount", -5)
            put("progress", 2.5)
            put("chapters", JSONArray())
        }

        val entry = BookEntry.fromJson(malformed)
        assertEquals("未命名书籍", entry.title)
        assertEquals("未知作者", entry.author)
        assertEquals(1, entry.pageCount)
        assertEquals(1.0, entry.progress, 0.0001)
        assertEquals("正文", entry.safeCurrentChapter.title)
        assertNull(entry.shelfName)
    }

    @Test
    fun helpers_cleanTitleAndFormatStorageBytesAndTimestampsWork() {
        assertEquals("雪中悍刀行", cleanBookTitle("【雪中悍刀行】"))
        assertEquals("剑来", cleanBookTitle("sxsy_org 剑来"))
        assertEquals("剑来", cleanBookTitle("SXSY-ORG   【剑来】"))
        assertEquals("【甲】【乙】", cleanBookTitle("【甲】【乙】"))
        assertEquals("sxsy_org   ", cleanBookTitle("sxsy_org   "))
        assertEquals("普通标题", cleanBookTitle("  普通标题  "))

        assertEquals("512 B", StorageStats.formatBytes(512L))
        assertEquals("2.0 KB", StorageStats.formatBytes(2048L))
        assertEquals("1.5 MB", StorageStats.formatBytes(1572864L))

        val font = ImportedFont.fromJson(ImportedFont(name = "霞鹜文楷", path = "/fonts/lxgw.ttf").toJson())
        assertEquals("霞鹜文楷", font.name)
        assertEquals("/fonts/lxgw.ttf", font.path)

        val ts = parseIso8601("2026-11-12T21:30:00")
        assertNotNull(ts)
        assertEquals("2026-11-12T21:30:00", formatIso8601(ts!!))
        assertNull(parseIso8601("null"))
    }
}
