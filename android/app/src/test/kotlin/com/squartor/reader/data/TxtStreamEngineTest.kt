package com.squartor.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files

class TxtStreamEngineTest {

    @Test
    fun isChapterHeading_matchesValidChineseAndEnglishHeadings() {
        val validHeadings = listOf(
            "第一章 风起云涌",
            "  ☆、第一百零八章 破阵！",
            "第一卷 潜龙在渊 第二章 夜雨",
            "【第三章 归途】",
            "第12章 未完的旅途……",
            "Chapter 12: The Glass Tower",
            "Part II - Awakening",
            "Section 4",
            "楔子",
            "番外二 春日宴",
        )
        for (heading in validHeadings) {
            assertTrue("Expected heading match for: $heading", TxtStreamEngine.isChapterHeading(heading))
        }
    }

    @Test
    fun isChapterHeading_rejectsMidSentenceProseAndDialogueFalsePositives() {
        val proseLines = listOf(
            "他在第三章里提到了这件事。",
            "“第一章我已经看完了，”她轻声说道。",
            "「第二卷的线索就在这里。」",
            "第三章写得真好，让人回味无穷。",
            "今天是第一集开播的日子，大家都守在屏幕前。",
            "QQ群：123456789",
            "作者：某某",
            "12:30",
        )
        for (line in proseLines) {
            assertFalse("Expected non-heading for prose: $line", TxtStreamEngine.isChapterHeading(line))
        }
    }

    @Test
    fun parseChaptersAndReadText_handlesUtf8AndGb18030WithoutFalseSplits() {
        val tempDir = Files.createTempDirectory("squartor_txt_test").toFile()
        try {
            val content = """
                楔子
                夜色如水，他在第三章里提到了这件事。
                “第一章我已经看完了，”她说道。

                第一章 初遇
                少年推开木门，风雪扑面而来。

                第二章 远行
                晨光熹微，马车驶出城门。
            """.trimIndent()

            val utf8File = File(tempDir, "utf8_novel.txt")
            utf8File.writeText(content, Charsets.UTF_8)

            val (utf8Chapters, totalWords) = TxtStreamEngine.parseChaptersWithWordCount(utf8File, "测试小说")
            assertEquals(3, utf8Chapters.size)
            assertEquals("楔子", utf8Chapters[0].title)
            assertEquals("第一章 初遇", utf8Chapters[1].title)
            assertEquals("第二章 远行", utf8Chapters[2].title)
            assertTrue(totalWords > 20)

            val prologueText = TxtStreamEngine.readChapterText(utf8Chapters[0], utf8File.absolutePath)
            assertTrue(prologueText.contains("他在第三章里提到了这件事"))
            assertTrue(prologueText.contains("第一章我已经看完了"))

            val gbCharset = Charset.forName("GB18030")
            val gbFile = File(tempDir, "gb_novel.txt")
            gbFile.writeBytes(content.toByteArray(gbCharset))

            val (gbChapters, _) = TxtStreamEngine.parseChaptersWithWordCount(gbFile, "GB小说")
            assertEquals(3, gbChapters.size)
            val ch2Text = TxtStreamEngine.readChapterText(gbChapters[2], gbFile.absolutePath)
            assertTrue(ch2Text.contains("马车驶出城门"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
