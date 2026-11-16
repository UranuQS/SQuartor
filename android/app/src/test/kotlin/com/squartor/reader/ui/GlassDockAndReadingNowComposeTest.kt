package com.squartor.reader.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.squartor.reader.AmbientSubpageBackground
import com.squartor.reader.model.AmbientGlowSource
import com.squartor.reader.model.AppThemeId
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.BookFormat
import com.squartor.reader.model.CloudSyncSettings
import com.squartor.reader.model.GlassClarityMode
import com.squartor.reader.model.ReaderChapter
import com.squartor.reader.model.ReadingStyle
import com.squartor.reader.model.ShelfBookBlock
import com.squartor.reader.model.StorageStats
import com.squartor.reader.model.buildPaletteFromSeed
import com.squartor.reader.model.darkAppPalettes
import com.squartor.reader.ui.glass.DockContextualAction
import com.squartor.reader.ui.glass.FlClashNavigationDock
import com.squartor.reader.ui.glass.GeminiCornerGlowBrushCache
import com.squartor.reader.ui.glass.Ios27GlassVariant
import com.squartor.reader.ui.glass.LocalAmbientGlowColors
import com.squartor.reader.ui.glass.LocalAmbientGlowColorsState
import com.squartor.reader.ui.glass.LocalPagerTransitioning
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.LocalVitroPopoverHost
import com.squartor.reader.ui.glass.CoverAssetCache
import com.squartor.reader.ui.glass.VitroGlassPopoverItem
import com.squartor.reader.ui.glass.VitroGlassPopoverMenu
import com.squartor.reader.ui.glass.VitroPopoverOverlayHost
import com.squartor.reader.ui.glass.ios27FilterPillTints
import com.squartor.reader.ui.glass.ios27GlassSurface
import com.squartor.reader.ui.glass.rememberIos27Glass
import com.squartor.reader.ui.glass.rememberSafeGlassEffectState
import com.squartor.reader.ui.glass.rememberVitroPopoverHostState
import com.squartor.reader.ui.glass.vitroInteractiveCard
import com.squartor.reader.ui.reader.ComposedPage
import com.squartor.reader.ui.reader.ReaderBlockItem
import com.squartor.reader.ui.reader.ReaderPreloadCache
import com.squartor.reader.ui.reader.ReaderScreen
import com.squartor.reader.ui.screens.ImportBooksScreen
import com.squartor.reader.ui.screens.ReadingNowScreen
import com.squartor.reader.ui.screens.SettingsScreen
import com.squartor.reader.ui.screens.ShelfScreen
import com.squartor.reader.ui.screens.StatsScreen
import com.squartor.reader.ui.screens.deriveHeroCtaColors
import com.squartor.reader.ui.state.ImportActivity
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassEffectState
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h900dp")
class GlassDockAndReadingNowComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleBooks(): List<BookEntry> = listOf(
        BookEntry(
            id = "b1",
            title = "魔女之旅 01",
            author = "白石定规",
            format = BookFormat.epub,
            bookDir = "/tmp/b1",
            sourcePath = "/tmp/b1.epub",
            importedAtMillis = 1_700_000_000_000L,
            chapters = listOf(ReaderChapter(title = "第一章", href = "ch1.xhtml", filePath = "ch1.xhtml")),
            progress = 0.25,
            lastReadAtMillis = 1_700_000_100_000L,
        ),
        BookEntry(
            id = "b2",
            title = "魔女之旅 02",
            author = "白石定规",
            format = BookFormat.txt,
            bookDir = "/tmp/b2",
            sourcePath = "/tmp/b2.txt",
            importedAtMillis = 1_699_999_000_000L,
            chapters = listOf(ReaderChapter(title = "序章", href = "ch0.txt", filePath = "ch0.txt")),
            progress = 0.09,
            lastReadAtMillis = 1_700_000_050_000L,
        ),
    )

    @Test
    fun navigationDock_displaysAllDestinationsAndDispatchesSelection() {
        var selected = -1
        val palette = darkAppPalettes[AppThemeId.wine]!!
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.width(400.dp)) {
                    FlClashNavigationDock(
                        selectedIndex = 0,
                        palette = palette,
                        hidden = false,
                        onSelected = { selected = it },
                    )
                }
            }
        }

        for (label in listOf("在读", "书架", "统计", "设置")) {
            composeRule.onAllNodesWithText(label).assertCountEquals(2)
            composeRule.onAllNodesWithText(label).onFirst().assertIsDisplayed()
        }

        composeRule.onAllNodesWithText("设置").onFirst().performTouchInput { click() }
        composeRule.waitForIdle()
        assertEquals(3, selected)
    }

    @Test
    fun navigationDock_unifiedAddOrbTriggersCallback() {
        var addClicks = 0
        val palette = ReadingStyle(appTheme = AppThemeId.wine).resolvePalette(isLight = true)
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.width(400.dp)) {
                    FlClashNavigationDock(
                        selectedIndex = 0,
                        palette = palette,
                        hidden = false,
                        onSelected = {},
                        contextualAction = DockContextualAction(
                            icon = Icons.Rounded.Add,
                            contentDescription = "添加书籍",
                            onClick = { addClicks++ },
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("添加书籍").assertIsDisplayed().performTouchInput { click() }
        composeRule.waitForIdle()
        assertEquals(1, addClicks)
    }

    @Test
    fun importBooksScreen_exposesMultiFileFolderAndShelfActions() {
        var filesClicks = 0
        var folderClicks = 0
        var goShelfClicks = 0
        var backClicks = 0
        val palette = ReadingStyle(appTheme = AppThemeId.wine).resolvePalette(isLight = true)

        composeRule.setContent {
            CompositionLocalProvider(LocalReadingStyle provides ReadingStyle()) {
                Box(modifier = Modifier.size(412.dp, 880.dp)) {
                    ImportBooksScreen(
                        palette = palette,
                        totalBooks = 27,
                        shelfCount = 2,
                        importActivity = ImportActivity(title = "已完成导入", detail = "新增 3 本", active = false),
                        onPickFiles = { filesClicks++ },
                        onPickFolder = { folderClicks++ },
                        onAddShelf = {},
                        onGoToShelf = { goShelfClicks++ },
                        onBack = { backClicks++ },
                    )
                }
            }
        }

        composeRule.onNodeWithText("添加书籍").assertIsDisplayed()
        composeRule.onNodeWithText("选择多本书籍").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("扫描并导入文件夹").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("前往书架浏览藏书").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.waitForIdle()

        assertEquals(1, filesClicks)
        assertEquals(1, folderClicks)
        assertEquals(1, goShelfClicks)
        assertEquals(1, backClicks)
    }

    @Test
    fun shelfScreen_resetsSearchStateWhenTabBecomesInactive() {
        var isActive by mutableStateOf(true)
        val palette = ReadingStyle(appTheme = AppThemeId.wine).resolvePalette(isLight = true)

        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                ShelfScreen(
                    books = sampleBooks(),
                    shelves = emptyList(),
                    palette = palette,
                    onOpenBook = {},
                    onImportFilesClick = {},
                    onImportFolderClick = {},
                    onDeleteBooks = {},
                    onMoveBooksToShelf = { _, _ -> },
                    onGroupSeries = { _, _ -> },
                    onRemoveFromSeries = {},
                    onUpdateBookMeta = { _, _, _ -> },
                    onAddShelf = {},
                    onDeleteShelf = {},
                    isActive = isActive,
                )
            }
        }

        composeRule.onNodeWithContentDescription("搜索书架").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("搜索书名、作者或系列…").assertIsDisplayed()
        composeRule.onAllNodesWithText("收起").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("收起搜索").assertIsDisplayed()

        isActive = false
        composeRule.waitForIdle()
        isActive = true
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("搜索书名、作者或系列…").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("搜索书架").assertIsDisplayed()
    }

    @Test
    fun readingNowScreen_emptyStateComposesCleanly() {
        var openShelfClicks = 0
        val palette = darkAppPalettes[AppThemeId.blue]!!
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                ReadingNowScreen(
                    books = emptyList(),
                    todaySeconds = 0,
                    weekSeconds = 0,
                    monthSeconds = 0,
                    weekDailySeconds = List(7) { 0 },
                    palette = palette,
                    onOpenBook = {},
                    onOpenShelf = { openShelfClicks++ },
                    onOpenStats = {},
                    onToggleBrightness = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "前往「书架」导入 TXT 或 EPUB 书籍，正在阅读的书会置顶呈现在这里。",
            substring = true,
        ).assertIsDisplayed()
        assertEquals(0, openShelfClicks)
    }

    @Test
    fun readingNowScreen_populatedCardsComposeInLightAndDarkWithoutErrors() {
        var isLightMode by mutableStateOf(true)
        val books = sampleBooks()
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            val palette = if (isLightMode) {
                ReadingStyle(appTheme = AppThemeId.wine).resolvePalette(isLight = true)
            } else {
                darkAppPalettes[AppThemeId.wine]!!
            }
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.size(412.dp, 880.dp)) {
                    ReadingNowScreen(
                        books = books,
                        todaySeconds = 4560,
                        weekSeconds = 4560,
                        monthSeconds = 4560,
                        weekDailySeconds = listOf(0, 1200, 0, 2400, 4560, 0, 0),
                        palette = palette,
                        onOpenBook = {},
                        onOpenShelf = {},
                        onOpenStats = {},
                        onToggleBrightness = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("本周累计阅读").assertIsDisplayed()
        composeRule.onNodeWithText("本月累计").assertIsDisplayed()
        composeRule.onNodeWithText("已读完").assertIsDisplayed()
        composeRule.onNodeWithText("书架藏书").assertIsDisplayed()
        composeRule.onNodeWithText("最近阅读").assertIsDisplayed()
        composeRule.onNodeWithText("继续阅读").assertIsDisplayed()
        composeRule.onAllNodesWithText("全部书架").assertCountEquals(0)

        isLightMode = false
        composeRule.waitForIdle()
        composeRule.onNodeWithText("本周累计阅读").assertIsDisplayed()
        composeRule.onNodeWithText("继续阅读").assertIsDisplayed()
        composeRule.onAllNodesWithText("全部书架").assertCountEquals(0)
    }

    @Test
    fun safeGlassEffectState_resetsInstanceWhenActiveToggles() {
        var active by mutableStateOf(true)
        var capturedState: GlassEffectState? = null
        composeRule.setContent {
            capturedState = rememberSafeGlassEffectState(active = active)
        }

        val initialState = capturedState!!
        assertFalse(initialState.isHeld)

        active = false
        composeRule.waitForIdle()
        active = true
        composeRule.waitForIdle()

        val restoredState = requireNotNull(capturedState)
        assertNotSame(initialState, restoredState)
        assertFalse(restoredState.isHeld)
        assertEquals(0f, restoredState.pressure, 0.0001f)
    }

    @Test
    fun darkModeGlass_preservesLiftedBrightnessAndSpecularHighlight() {
        val darkPalette = darkAppPalettes[AppThemeId.wine]!!
        val captured = mutableListOf<Glass>()
        composeRule.setContent {
            for (clarity in GlassClarityMode.entries) {
                for (variant in listOf(Ios27GlassVariant.Chrome, Ios27GlassVariant.AccentPill, Ios27GlassVariant.ReaderSheet)) {
                    captured += rememberIos27Glass(
                        style = ReadingStyle(glassClarityMode = clarity, glassOpacityLevel = 0.5f),
                        palette = darkPalette,
                        variant = variant,
                        appearanceOverride = GlassAppearance.Dark,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertTrue(captured.isNotEmpty())
        assertEquals(0.72f, captured.first().highlightIntensity, 0.001f)
        captured.forEach { g ->
            assertTrue("Dark glass brightness should stay lifted (>=0.36): ${g.brightness}", g.brightness >= 0.36f)
            assertTrue("Dark glass dimming should stay restrained (<=0.02): ${g.dimming}", g.dimming <= 0.021f)
            assertTrue(
                "Dark glass specular highlight should stay softened anti-wireframe ([0.55, 0.85)): ${g.highlightIntensity}",
                g.highlightIntensity >= 0.55f && g.highlightIntensity < 0.85f,
            )
        }
    }

    @Test
    fun shelfTopButtons_openDismissAndStretchWithoutSticking() {
        val palette = darkAppPalettes[AppThemeId.wine]!!
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            val popoverHost = rememberVitroPopoverHostState()
            Box(modifier = Modifier.size(412.dp, 880.dp)) {
                CompositionLocalProvider(
                    LocalReadingStyle provides ReadingStyle(),
                    LocalVitroBackdrop provides backdrop,
                    LocalVitroPopoverHost provides popoverHost,
                ) {
                    ShelfScreen(
                        books = sampleBooks(),
                        shelves = emptyList(),
                        palette = palette,
                        onOpenBook = {},
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                    )
                }
                VitroPopoverOverlayHost(hostState = popoverHost, fallbackBackdrop = backdrop)
            }
        }

        // Stretching past button edge must not falsely open popover or cancel cleanly
        composeRule.onNodeWithContentDescription("排序方式").performTouchInput {
            swipeRight(startX = centerX, endX = centerX + 120f, durationMillis = 120L)
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("按书名排序").assertCountEquals(0)

        // Tap opens sort popover
        composeRule.onNodeWithContentDescription("排序方式").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("按书名排序").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // After menu dismiss settles, both action buttons return cleanly and remain clickable
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("排序方式").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("更多操作").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("批量选择").assertDoesNotExist()
        composeRule.onNodeWithText("三列宫格").assertIsDisplayed()
        composeRule.onNodeWithText("列表视图").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_switchesAmbientGlowSource() {
        var currentStyle by mutableStateOf(ReadingStyle(ambientGlowSource = AmbientGlowSource.bookCover))
        val palette = darkAppPalettes[AppThemeId.wine]!!

        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            Box(modifier = Modifier.size(412.dp, 880.dp)) {
                CompositionLocalProvider(
                    LocalReadingStyle provides currentStyle,
                    LocalVitroBackdrop provides backdrop,
                ) {
                    SettingsScreen(
                        style = currentStyle,
                        fonts = emptyList(),
                        cloudSync = CloudSyncSettings(),
                        storageStats = StorageStats(booksBytes = 0L, cacheBytes = 0L),
                        palette = palette,
                        onUpdateStyle = { currentStyle = it },
                        onToggleBrightness = {},
                        onImportFontClick = {},
                        onDeleteFont = {},
                        onUpdateCloudSync = {},
                        onSyncUpload = {},
                        onSyncDownload = {},
                        onClearCache = {},
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("氛围光配色来源").assertCountEquals(0)
        composeRule.onNodeWithText("主题与纸张").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("氛围光配色来源").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("应用主题色").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(AmbientGlowSource.appTheme, currentStyle.ambientGlowSource)
    }

    @Test
    fun ambientExtractVibrantColorPair_preservesBlueDominantCoverOverYellowAccent() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                val color = if (x < 6 && y < 6) {
                    AndroidColor.rgb(255, 224, 32)
                } else {
                    AndroidColor.rgb(42, 132, 214)
                }
                bitmap.setPixel(x, y, color)
            }
        }
        val pair = requireNotNull(CoverAssetCache.extractVibrantColorPair(bitmap))
        val (primary, secondary) = pair
        assertTrue(
            "Expected blue-dominant primary color over small yellow accent, got $primary",
            primary.blue > primary.red && primary.blue > 0.45f,
        )
        assertTrue(
            "Expected harmonious non-yellow secondary color, got $secondary",
            secondary.blue >= secondary.red * 0.9f,
        )
    }

    @Test
    fun vitroGlassPopoverMenu_dockAddBooksListsImportAndShelfActions() {
        val palette = darkAppPalettes[AppThemeId.blue]!!
        var menuOpen by mutableStateOf(true)
        var pickedFiles = 0
        var pickedFolder = 0
        var openedNewShelf = 0

        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            val popoverHost = rememberVitroPopoverHostState()
            Box(modifier = Modifier.size(412.dp, 880.dp)) {
                CompositionLocalProvider(
                    LocalReadingStyle provides ReadingStyle(),
                    LocalVitroBackdrop provides backdrop,
                    LocalVitroPopoverHost provides popoverHost,
                ) {
                    VitroGlassPopoverMenu(
                        visible = menuOpen,
                        anchorBoundsInRoot = Rect(312f, 780f, 388f, 856f),
                        items = listOf(
                            VitroGlassPopoverItem(
                                label = "选择多本书籍",
                                icon = Icons.Rounded.UploadFile,
                                onClick = { pickedFiles++ },
                            ),
                            VitroGlassPopoverItem(
                                label = "扫描导入文件夹",
                                icon = Icons.Rounded.FolderOpen,
                                onClick = { pickedFolder++ },
                            ),
                            VitroGlassPopoverItem(
                                label = "新建书架分组",
                                icon = Icons.Rounded.LibraryAdd,
                                onClick = { openedNewShelf++ },
                            ),
                        ),
                        palette = palette,
                        onDismiss = { menuOpen = false },
                        backdrop = backdrop,
                        width = 216.dp,
                        anchorCornerDp = 31.dp,
                        anchorIcon = Icons.Rounded.Add,
                    )
                }
                VitroPopoverOverlayHost(hostState = popoverHost, fallbackBackdrop = backdrop)
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("选择多本书籍").assertIsDisplayed()
        composeRule.onNodeWithText("扫描导入文件夹").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(1, pickedFolder)
        assertEquals(0, pickedFiles)
        assertEquals(0, openedNewShelf)
        assertFalse(menuOpen)
    }

    @Test
    fun shelfScreen_seriesDrawerOpensHighDensityGridAndTogglesCompactList() {
        val palette = ReadingStyle(appTheme = AppThemeId.blue).resolvePalette(isLight = true)
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            val popoverHost = rememberVitroPopoverHostState()
            Box(modifier = Modifier.size(412.dp, 880.dp)) {
                CompositionLocalProvider(
                    LocalReadingStyle provides ReadingStyle(appTheme = AppThemeId.blue),
                    LocalVitroBackdrop provides backdrop,
                    LocalVitroPopoverHost provides popoverHost,
                ) {
                    ShelfScreen(
                        books = sampleBooks(),
                        shelves = emptyList(),
                        palette = palette,
                        onOpenBook = {},
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                    )
                }
                VitroPopoverOverlayHost(hostState = popoverHost, fallbackBackdrop = backdrop)
            }
        }

        composeRule.onNodeWithText("魔女之旅").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("白石定规 · 2 卷 · 平均 17%").assertIsDisplayed()
        composeRule.onAllNodesWithText("第01卷").assertCountEquals(0)
        composeRule.onAllNodesWithText("第02卷").assertCountEquals(0)
        composeRule.onAllNodesWithText("魔女之旅 01").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("魔女之旅 02").onFirst().assertIsDisplayed()

        composeRule.onNodeWithContentDescription("切换列表视图").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("切换网格视图").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("关闭").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("白石定规 · 2 卷 · 平均 17%").assertCountEquals(0)
    }

    @Test
    fun ios27FilterPillTints_differentiatesSelectedAccentFromUnselectedPillInLightAndDark() {
        val lightPalette = ReadingStyle(appTheme = AppThemeId.blue).resolvePalette(isLight = true)
        val darkPalette = darkAppPalettes[AppThemeId.blue]!!
        var results = emptyList<Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>>()

        composeRule.setContent {
            results = listOf(
                ios27FilterPillTints(lightPalette, ReadingStyle()),
                ios27FilterPillTints(darkPalette, ReadingStyle()),
            )
        }
        composeRule.waitForIdle()

        assertEquals(2, results.size)
        results.forEach { (unselected, selected) ->
            assertTrue(
                "Selected pill tint ($selected) should carry higher blue accent than neutral ($unselected)",
                selected.blue > unselected.blue || selected != unselected,
            )
        }
    }

    @Test
    fun powerSaveMode_dropsGlassShadowAndEdgeRefractionAndComposesReadingNowCleanly() {
        val darkPalette = darkAppPalettes[AppThemeId.wine]!!
        val captured = mutableListOf<Glass>()
        val books = sampleBooks()
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalPowerSaveMode provides true,
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                for (variant in listOf(Ios27GlassVariant.Chrome, Ios27GlassVariant.ReaderSheet)) {
                    captured += rememberIos27Glass(
                        style = ReadingStyle(),
                        palette = darkPalette,
                        variant = variant,
                        isInteractive = true,
                        appearanceOverride = GlassAppearance.Dark,
                    )
                }
                Box(modifier = Modifier.size(412.dp, 880.dp)) {
                    ReadingNowScreen(
                        books = books,
                        todaySeconds = 1200,
                        weekSeconds = 2400,
                        monthSeconds = 3600,
                        weekDailySeconds = listOf(1200, 0, 0, 0, 0, 0, 0),
                        palette = darkPalette,
                        onOpenBook = {},
                        onOpenShelf = {},
                        onOpenStats = {},
                        onToggleBrightness = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(2, captured.size)
        captured.forEach { glass ->
            assertEquals(0f, glass.shadowOpacity, 0.0001f)
            assertEquals(0.dp, glass.shadowRadius)
            assertEquals(0.dp, glass.refractionAmount)
            assertEquals(0.dp, glass.refractionHeight)
            assertEquals(0f, glass.dispersion, 0.0001f)
            assertEquals(1f, glass.magnification, 0.0001f)
            assertFalse(glass.isInteractive)
            assertTrue("Blur radius should stay non-zero downsampled", glass.blurRadius > 0.dp)
        }
        composeRule.onNodeWithText("继续阅读").assertIsDisplayed()
        composeRule.onNodeWithText("最近阅读").assertIsDisplayed()
    }

    @Test
    fun flClashNavigationDock_togglesContextualOrbAndTabSelectionAcrossResizes() {
        val palette = ReadingStyle(appTheme = AppThemeId.blue).resolvePalette(isLight = true)
        var selectedIndex by mutableStateOf(0)
        var showAction by mutableStateOf(false)
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            val action = if (showAction) {
                DockContextualAction(
                    icon = Icons.Rounded.Add,
                    contentDescription = "添加书籍",
                    onClick = {},
                )
            } else {
                null
            }
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.width(412.dp)) {
                    FlClashNavigationDock(
                        selectedIndex = selectedIndex,
                        palette = palette,
                        hidden = false,
                        onSelected = { selectedIndex = it },
                        contextualAction = action,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("在读").onFirst().assertIsDisplayed()

        // Switch from 336dp pill (no orb) to 284dp pill (with orb) while advancing selected tab
        selectedIndex = 1
        showAction = true
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("添加书籍").assertIsDisplayed()

        // Collapse orb back while changing tab again
        selectedIndex = 2
        showAction = false
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("统计").onFirst().assertIsDisplayed()
    }

    @Test
    fun flClashNavigationDock_hiddenAndRevealTransitionsSettleWithoutSpinning() {
        val palette = ReadingStyle(appTheme = AppThemeId.blue).resolvePalette(isLight = true)
        var hidden by mutableStateOf(true)
        var selectedIndex by mutableStateOf(0)
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.width(412.dp)) {
                    FlClashNavigationDock(
                        selectedIndex = selectedIndex,
                        palette = palette,
                        hidden = hidden,
                        onSelected = { selectedIndex = it },
                    )
                }
            }
        }

        // Hidden state must suspend cleanly without infinite pointer-input re-entry
        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.waitForIdle()

        // Reveal back to visible and verify idle + interaction settle smoothly
        hidden = false
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("书架").onFirst().assertIsDisplayed()

        // Hide again mid-session and settle
        hidden = true
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
    }

    @Test
    fun flClashNavigationDock_offscreenSettledUnmountsChromeAndReportsCallback() {
        val palette = ReadingStyle(appTheme = AppThemeId.blue).resolvePalette(isLight = false)
        var hidden by mutableStateOf(false)
        var selectedIndex by mutableStateOf(0)
        var settledCallback by mutableStateOf<Boolean?>(null)
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.width(412.dp)) {
                    FlClashNavigationDock(
                        selectedIndex = selectedIndex,
                        palette = palette,
                        hidden = hidden,
                        onSelected = { selectedIndex = it },
                        onOffscreenSettledChanged = { settledCallback = it },
                    )
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(false, settledCallback)
        composeRule.onAllNodesWithText("书架").onFirst().assertIsDisplayed()

        // Once hidden animation settles offscreen (>= 1.50f), dock detaches its glass chrome tree.
        hidden = true
        composeRule.mainClock.advanceTimeBy(900L)
        composeRule.waitForIdle()
        assertEquals(true, settledCallback)
        composeRule.onAllNodesWithText("书架").assertCountEquals(0)

        // Revealing brings the dock back cleanly and resets settled flag.
        hidden = false
        composeRule.mainClock.advanceTimeBy(650L)
        composeRule.waitForIdle()
        assertEquals(false, settledCallback)
        composeRule.onAllNodesWithText("书架").onFirst().assertIsDisplayed()
    }

    @Test
    fun flClashNavigationDock_unmountWhenOffscreenFalseRetainsWarmTreeAndRevealsCleanly() {
        val palette = ReadingStyle(appTheme = AppThemeId.blue).resolvePalette(isLight = false)
        var hidden by mutableStateOf(false)
        var selectedIndex by mutableStateOf(1)
        var settledCallback by mutableStateOf<Boolean?>(null)
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.width(412.dp)) {
                    FlClashNavigationDock(
                        selectedIndex = selectedIndex,
                        palette = palette,
                        hidden = hidden,
                        onSelected = { selectedIndex = it },
                        onOffscreenSettledChanged = { settledCallback = it },
                        unmountWhenOffscreen = false,
                    )
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(false, settledCallback)
        composeRule.onAllNodesWithText("书架").onFirst().assertIsDisplayed()

        // Once hidden settles offscreen with unmountWhenOffscreen = false, callback reports true
        // while the chrome tree stays mounted warm (no destroy/reattach churn).
        hidden = true
        composeRule.mainClock.advanceTimeBy(650L)
        composeRule.waitForIdle()
        assertEquals(true, settledCallback)
        assertTrue(composeRule.onAllNodesWithText("书架").fetchSemanticsNodes().isNotEmpty())

        // Revealing brings the warm dock back cleanly and rapidly resets settled flag.
        hidden = false
        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitForIdle()
        assertEquals(false, settledCallback)
        composeRule.onAllNodesWithText("书架").onFirst().assertIsDisplayed()
    }

    @Test
    fun ios27GlassSurface_powerSaveFallsBackAcrossAllVariants() {
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = style.resolvePalette(isLight = true)
        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides style,
                LocalPowerSaveMode provides true,
                LocalVitroBackdrop provides backdrop,
            ) {
                Column(modifier = Modifier.width(320.dp)) {
                    Ios27GlassVariant.entries.forEach { variant ->
                        Box(
                            modifier = Modifier
                                .size(width = 160.dp, height = 44.dp)
                                .ios27GlassSurface(
                                    shape = RoundedCornerShape(14.dp),
                                    palette = palette,
                                    style = style,
                                    variant = variant,
                                    backdrop = backdrop,
                                ),
                        ) {
                            Text(text = "ps_${variant.name}")
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        Ios27GlassVariant.entries.forEach { variant ->
            composeRule.onNodeWithText("ps_${variant.name}").assertIsDisplayed()
        }
    }

    @Test
    fun shelfScreen_seriesDrawerAndCoverCardsRenderAcrossNormalAndPowerSave() {
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = style.resolvePalette(isLight = true)
        var powerSave by mutableStateOf(false)
        var drawerOpenState by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalReadingStyle provides style,
                LocalPowerSaveMode provides powerSave,
            ) {
                ShelfScreen(
                    books = sampleBooks(),
                    shelves = emptyList(),
                    palette = palette,
                    onOpenBook = {},
                    onImportFilesClick = {},
                    onImportFolderClick = {},
                    onDeleteBooks = {},
                    onMoveBooksToShelf = { _, _ -> },
                    onGroupSeries = { _, _ -> },
                    onRemoveFromSeries = {},
                    onUpdateBookMeta = { _, _, _ -> },
                    onAddShelf = {},
                    onDeleteShelf = {},
                    onSeriesSheetOpenChanged = { drawerOpenState = it },
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("2 卷").onFirst().assertIsDisplayed()

        // Open series drawer in Normal mode -> renders SeriesVolumeGridTile without sheetBackdrop
        composeRule.onAllNodesWithText("魔女之旅").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        assertTrue(drawerOpenState)

        // Toggle drawer grid -> list mode (SeriesVolumeRow) and switch to PowerSave mode
        composeRule.onNodeWithContentDescription("切换列表视图").performClick()
        powerSave = true
        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("关闭").performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        assertFalse(drawerOpenState)
    }

    @Test
    fun rememberIos27Glass_keepsBlurTapsCapAndMergesCardAndFusibleCardMaterial() {
        val densityScale = 2.625f // 420dpi emulator/Phone density
        val captured = mutableMapOf<Triple<Boolean, GlassClarityMode, Ios27GlassVariant>, Glass>()
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = densityScale)) {
                for (powerSave in listOf(false, true)) {
                    for (clarity in GlassClarityMode.entries) {
                        val style = ReadingStyle(appTheme = AppThemeId.blue, glassClarityMode = clarity)
                        val palette = style.resolvePalette(isLight = !powerSave)
                        CompositionLocalProvider(
                            LocalPowerSaveMode provides powerSave,
                            LocalReadingStyle provides style,
                        ) {
                            for (variant in Ios27GlassVariant.entries) {
                                captured[Triple(powerSave, clarity, variant)] = rememberIos27Glass(
                                    style = style,
                                    palette = palette,
                                    variant = variant,
                                )
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        for (powerSave in listOf(false, true)) {
            for (clarity in GlassClarityMode.entries) {
                for (variant in Ios27GlassVariant.entries) {
                    val g = captured.getValue(Triple(powerSave, clarity, variant))
                    val blurPx = g.blurRadius.value * densityScale
                    assertTrue(
                        "Expected BlurTaps cap <= 17.0px for $variant/$clarity/$powerSave, was $blurPx",
                        blurPx <= 17.01f,
                    )
                    assertEquals(0f, g.dispersion, 0.0001f)
                    if (powerSave ||
                        variant == Ios27GlassVariant.Card ||
                        variant == Ios27GlassVariant.FusibleCard ||
                        variant == Ios27GlassVariant.SubtlePill ||
                        variant == Ios27GlassVariant.AccentPill
                    ) {
                        assertEquals(0f, g.shadowOpacity, 0.0001f)
                        assertEquals(0f, g.shadowRadius.value, 0.0001f)
                    }
                }
                val card = captured.getValue(Triple(powerSave, clarity, Ios27GlassVariant.Card))
                val fusible = captured.getValue(Triple(powerSave, clarity, Ios27GlassVariant.FusibleCard))
                assertEquals(card.blurRadius.value, fusible.blurRadius.value, 0.0001f)
                assertEquals(card.refractionHeight.value, fusible.refractionHeight.value, 0.0001f)
                assertEquals(card.refractionAmount.value, fusible.refractionAmount.value, 0.0001f)
                assertEquals(card.magnification, fusible.magnification, 0.0001f)
                assertEquals(card.shadowRadius.value, fusible.shadowRadius.value, 0.0001f)
                assertEquals(card.shadowOffset.value, fusible.shadowOffset.value, 0.0001f)
                assertEquals(card.shadowOpacity, fusible.shadowOpacity, 0.0001f)
            }
        }
    }

    @Test
    fun vitroInteractiveCard_supportsVerticalOnlyScrollHandoffAndClickAcrossPowerSave() {
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = style.resolvePalette(isLight = true)
        var powerSave by mutableStateOf(false)
        val clicked = mutableListOf<Int>()
        val itemsList = (1..8).toList()
        var listStateRef: androidx.compose.foundation.lazy.LazyListState? = null

        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            val rowState = rememberLazyListState()
            listStateRef = rowState
            CompositionLocalProvider(
                LocalReadingStyle provides style,
                LocalPowerSaveMode provides powerSave,
                LocalVitroBackdrop provides backdrop,
            ) {
                LazyRow(
                    state = rowState,
                    modifier = Modifier.size(width = 360.dp, height = 140.dp),
                ) {
                    items(itemsList, key = { it }) { idx ->
                        Box(
                            modifier = Modifier
                                .size(width = 180.dp, height = 120.dp)
                                .testTag("recent_card_$idx")
                                .vitroInteractiveCard(
                                    shape = RoundedCornerShape(16.dp),
                                    palette = palette,
                                    style = style,
                                    variant = Ios27GlassVariant.Card,
                                    backdrop = backdrop,
                                    verticalOnly = true,
                                    maxPullDp = 10.dp,
                                    onClick = { clicked.add(idx) },
                                ),
                        ) {
                            Text(text = "recent_card_$idx")
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("recent_card_1").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(1), clicked)

        // Horizontal swipe must be handed off smoothly to LazyRow without being swallowed
        composeRule.onNodeWithTag("recent_card_2").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        val stateAfter = listStateRef!!
        assertTrue(
            "Expected LazyRow to scroll after horizontal swipe, idx=${stateAfter.firstVisibleItemIndex} off=${stateAfter.firstVisibleItemScrollOffset}",
            stateAfter.firstVisibleItemIndex > 0 || stateAfter.firstVisibleItemScrollOffset > 0,
        )
        composeRule.onNodeWithTag("recent_card_3").assertIsDisplayed()

        // Switching to PowerSave preserves click interaction without glass overhead
        powerSave = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("recent_card_3").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(1, 3), clicked)
    }

    @Test
    fun readerPreloadCache_roundTripsBlocksAndPagesDeterministically() {
        val style = ReadingStyle(fontSize = 18f, lineHeight = 1.6f)
        val sig = ReaderPreloadCache.buildPaginationSignature(
            bookId = "book_preload_test",
            style = style,
            topMarginPx = 64,
            contentWidthPx = 960,
            contentHeightPx = 1800,
            textArgb = 0xFF1D1D1F.toInt(),
        )
        val blocks = listOf(ReaderBlockItem(kind = "paragraph", text = "Hello Reader"))
        val pages = listOf(
            ComposedPage(
                chapterIndex = 0,
                chapterTitle = "第一章",
                pageIndex = 0,
                totalPagesInChapter = 1,
                elements = emptyList(),
                plainSnippet = "Hello Reader",
            ),
        )
        ReaderPreloadCache.putBlocks("book_preload_test", 0, blocks)
        ReaderPreloadCache.putPages("book_preload_test", 0, sig, pages)

        assertEquals(blocks, ReaderPreloadCache.getBlocks("book_preload_test", 0))
        assertEquals(pages, ReaderPreloadCache.getPages("book_preload_test", 0, sig))
        assertEquals(null, ReaderPreloadCache.getPages("book_preload_test", 0, "$sig#changed"))
    }

    @Test
    fun deriveHeroCtaColors_alwaysReturnsWhiteContentWithWcagContrast() {
        val sampleInputs = listOf(
            Color(0xFF547FA4), // slate blue (similar to Fig 2 cover)
            Color(0xFFF6D347), // bright yellow
            Color(0xFFA7E3B6), // pastel mint
            Color(0xFFEAE3D2), // cream neutral
            Color(0xFF7B2145), // deep wine
            Color(0xFF8FB8E8), // pale sky blue
        )
        for (raw in sampleInputs) {
            for (isLight in listOf(true, false)) {
                val (fill, content) = deriveHeroCtaColors(raw, isLight)
                assertEquals("Expected pure white CTA content for $raw (isLight=$isLight)", Color.White, content)
                val lum1 = Color.White.luminance()
                val lum2 = fill.luminance()
                val contrast = (maxOf(lum1, lum2) + 0.05f) / (minOf(lum1, lum2) + 0.05f)
                assertTrue(
                    "Expected white CTA contrast >= 4.49 for $raw (isLight=$isLight), got $contrast",
                    contrast >= 4.49f,
                )
            }
        }
    }

    @Test
    fun coverAssetCache_extractDominantTriColor_picksTopThreeDistinctColors() {
        val bmp = Bitmap.createBitmap(40, 56, Bitmap.Config.ARGB_8888)
        for (y in 0 until 56) {
            for (x in 0 until 40) {
                val color = when {
                    y < 28 -> AndroidColor.rgb(46, 115, 184) // ~50% sky/cloak blue
                    y < 45 -> AndroidColor.rgb(44, 158, 107) // ~30% emerald green
                    y < 53 -> AndroidColor.rgb(224, 90, 122) // ~14% warm coral
                    else -> AndroidColor.rgb(252, 252, 252) // ~6% highlight white
                }
                bmp.setPixel(x, y, color)
            }
        }
        try {
            val triad = CoverAssetCache.extractDominantTriColor(bmp)
            assertTrue(triad != null)
            val (c1, c2, c3) = triad!!
            // Dominant #1 should be blue-channel dominant, #2 green-channel dominant, #3 red-channel dominant
            assertTrue("Expected #1 dominant blue, got $c1", c1.blue > c1.red && c1.blue >= c1.green)
            assertTrue("Expected #2 dominant green, got $c2", c2.green > c2.red && c2.green > c2.blue)
            assertTrue("Expected #3 dominant coral/red, got $c3", c3.red > c3.green)
        } finally {
            bmp.recycle()
        }
    }

    @Test
    fun readingNowScreen_transitionsChapterAndHeroBookSmoothly() {
        var booksState by mutableStateOf(sampleBooks())
        val palette = darkAppPalettes[AppThemeId.blue]!!
        composeRule.setContent {
            CompositionLocalProvider(LocalReadingStyle provides ReadingStyle()) {
                ReadingNowScreen(
                    books = booksState,
                    todaySeconds = 600,
                    weekSeconds = 1200,
                    monthSeconds = 3600,
                    weekDailySeconds = listOf(100, 200, 150, 0, 150, 300, 300),
                    palette = palette,
                    onOpenBook = {},
                    onOpenShelf = {},
                    onOpenStats = {},
                    onToggleBrightness = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("第一章").assertIsDisplayed()

        // Same Hero book progresses from Chapter 1 -> Chapter 2
        val updatedA = booksState[0].copy(
            currentChapterIndex = 1,
            progress = 0.55,
            chapters = listOf(
                ReaderChapter(title = "第一章", href = "ch1.xhtml", filePath = "ch1.xhtml"),
                ReaderChapter(title = "第二章", href = "ch2.xhtml", filePath = "ch2.xhtml"),
            ),
        )
        booksState = listOf(updatedA, booksState[1])
        composeRule.waitForIdle()
        composeRule.onNodeWithText("第二章").assertIsDisplayed()

        // Switching Hero book cleanly crossfades Hero from b1 -> b2
        booksState = listOf(
            booksState[1].copy(lastReadAtMillis = 1_800_000_000_000L),
            updatedA,
        )
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("魔女之旅 02").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("序章").assertIsDisplayed()
        composeRule.onNodeWithText("第二章").assertIsDisplayed()
    }

    @Test
    fun ambientSubpageBackground_rendersBottomCornerGlowCleanlyAcrossThemesSourcesAndTransitions() {
        var useLight by mutableStateOf(false)
        var glowSource by mutableStateOf(AmbientGlowSource.bookCover)
        var transitioning by mutableStateOf(false)
        val glowColors = Color(0xFF3E6BB8) to Color(0xFF3FA67E)
        composeRule.setContent {
            val style = ReadingStyle(appTheme = AppThemeId.blue, ambientGlowSource = glowSource)
            val palette = style.resolvePalette(isLight = useLight)
            CompositionLocalProvider(
                LocalReadingStyle provides style,
                LocalAmbientGlowColors provides glowColors,
                LocalPagerTransitioning provides transitioning,
            ) {
                Box(modifier = Modifier.size(390.dp, 844.dp)) {
                    AmbientSubpageBackground(
                        palette = palette,
                        topOnlyWhenPagerTransitioning = true,
                        isAnimating = true,
                    )
                    Text("Page Foreground Label")
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Page Foreground Label").assertIsDisplayed()

        // Active HorizontalPager swipe switches page to top-half-only backdrop so stationary root glow shows
        transitioning = true
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Page Foreground Label").assertIsDisplayed()

        useLight = true
        glowSource = AmbientGlowSource.appTheme
        transitioning = false
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Page Foreground Label").assertIsDisplayed()
    }

    @Test
    fun geminiCornerGlowBrushCache_constructsSingleHueLightAndDarkCornerBrushes() {
        val seedColor = Color(0xFF3E78D8)
        val darkCache = GeminiCornerGlowBrushCache(
            isLight = false,
            primaryColor = seedColor,
        )
        val lightCache = GeminiCornerGlowBrushCache(
            isLight = true,
            primaryColor = seedColor,
        )
        assertTrue(!darkCache.isLight && lightCache.isLight)
        assertTrue(darkCache.cornerLeftBrush !== darkCache.cornerRightBrush)
        assertTrue(lightCache.cornerRightBrush !== lightCache.bottomTideBrush)
    }

    @Test
    fun coverAssetCache_and_buildPaletteFromSeed_moderateSaturationForNeonCovers() {
        val neonBmp = Bitmap.createBitmap(32, 48, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(0, 235, 255))
        }
        try {
            val pair = requireNotNull(CoverAssetCache.extractVibrantColorPair(neonBmp))
            val extractedHsv = FloatArray(3)
            AndroidColor.RGBToHSV(
                (pair.first.red * 255f).toInt(),
                (pair.first.green * 255f).toInt(),
                (pair.first.blue * 255f).toInt(),
                extractedHsv,
            )
            assertTrue(
                "Expected calm cover primary saturation <= 0.54, got ${extractedHsv[1]}",
                extractedHsv[1] <= 0.54f,
            )
            for (isLight in listOf(true, false)) {
                val palette = buildPaletteFromSeed(
                    seed = Color(0xFFFF1A66),
                    isLight = isLight,
                    label = "Test",
                    id = AppThemeId.blue,
                )
                val paletteHsv = FloatArray(3)
                AndroidColor.RGBToHSV(
                    (palette.primary.red * 255f).toInt(),
                    (palette.primary.green * 255f).toInt(),
                    (palette.primary.blue * 255f).toInt(),
                    paletteHsv,
                )
                assertTrue(
                    "Expected calm palette primary saturation <= 0.48 (isLight=$isLight), got ${paletteHsv[1]}",
                    paletteHsv[1] <= 0.48f,
                )
            }
        } finally {
            neonBmp.recycle()
        }
    }

    @Test
    fun readerPreloadCache_normalizesBareSignature_andPreservesWarmCachesOnLowTrim() {
        val bookId = "preload_norm_book"
        val pages = listOf(
            ComposedPage(0, "第一章", 0, 1, emptyList(), "正文", startCharOffset = 0, endCharOffset = 2),
        )
        val blocks = listOf(ReaderBlockItem(kind = "p", text = "正文"))
        ReaderPreloadCache.putBlocks(bookId, 0, blocks)
        ReaderPreloadCache.putPages(bookId, 0, "sig_1080x2400", pages)

        assertTrue(ReaderPreloadCache.getPages(bookId, 0, "sig_1080x2400#0") === pages)
        ReaderPreloadCache.trimMemory(10) // TRIM_MEMORY_RUNNING_LOW must keep warm preload caches
        assertTrue(ReaderPreloadCache.getPages(bookId, 0, "sig_1080x2400#0") === pages)
        assertTrue(ReaderPreloadCache.getBlocks(bookId, 0) === blocks)
    }

    @Test
    fun readerScreen_placesTocOrbAndFontDropletOnLeftSideOfDock() {
        val sampleBook = sampleBooks().first().copy(
            chapters = listOf(ReaderChapter(title = "第一章 起航", href = "ch1.xhtml", filePath = "")),
        )
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = darkAppPalettes[AppThemeId.blue]!!
        composeRule.setContent {
            CompositionLocalProvider(
                LocalPowerSaveMode provides true,
                LocalReadingStyle provides style,
            ) {
                Box(modifier = Modifier.size(390.dp, 844.dp)) {
                    ReaderScreen(
                        book = sampleBook,
                        style = style,
                        palette = palette,
                        loadChapterBlocks = { _, _ -> listOf(ReaderBlockItem(kind = "p", text = "第一段测试文字")) },
                        loadImageBytes = { null },
                        onProgressChanged = { _, _, _, _ -> },
                        onUpdateStyle = {},
                        onToggleBookmark = {},
                        onRemoveBookmark = {},
                        onSaveImageToGallery = { _, _ -> },
                        onBack = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        // Center tap toggles reader overlay controls visible
        composeRule.onRoot().performTouchInput { click(center) }
        composeRule.waitForIdle()

        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val tocBounds = composeRule.onNodeWithContentDescription("目录与书签").fetchSemanticsNode().boundsInRoot
        val aaBounds = composeRule.onNodeWithText("字体").fetchSemanticsNode().boundsInRoot
        val midX = rootBounds.center.x

        assertTrue("Expected TOC orb ($tocBounds) left of Aa droplet ($aaBounds)", tocBounds.center.x < aaBounds.center.x)
        assertTrue("Expected TOC orb ($tocBounds) on left half (< $midX)", tocBounds.center.x < midX)
        assertTrue("Expected Aa droplet ($aaBounds) on left half (< $midX)", aaBounds.center.x < midX)
    }

    @Test
    fun shelfScreen_searchBarExpandAndCollapseMoveGridInLockstepWithCategoryBar() {
        val palette = ReadingStyle(appTheme = AppThemeId.wine).resolvePalette(isLight = true)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalPowerSaveMode provides true,
                LocalReadingStyle provides ReadingStyle(),
            ) {
                Box(modifier = Modifier.size(412.dp, 900.dp)) {
                    ShelfScreen(
                        books = sampleBooks(),
                        shelves = emptyList(),
                        palette = palette,
                        onOpenBook = {},
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val initialBookTop = composeRule.onAllNodesWithText("魔女之旅").onFirst().fetchSemanticsNode().boundsInRoot.top
        val initialTabTop = composeRule.onAllNodesWithText("全部", substring = true).onFirst().fetchSemanticsNode().boundsInRoot.top

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithContentDescription("搜索书架").performClick()
            composeRule.mainClock.advanceTimeBy(96L)
            val midBookTop = composeRule.onAllNodesWithText("魔女之旅").onFirst().fetchSemanticsNode().boundsInRoot.top
            val midTabTop = composeRule.onAllNodesWithText("全部", substring = true).onFirst().fetchSemanticsNode().boundsInRoot.top

            composeRule.mainClock.advanceTimeBy(450L)
            val expandedBookTop = composeRule.onAllNodesWithText("魔女之旅").onFirst().fetchSemanticsNode().boundsInRoot.top
            val expandedTabTop = composeRule.onAllNodesWithText("全部", substring = true).onFirst().fetchSemanticsNode().boundsInRoot.top

            assertTrue("Expected grid item to move down mid-expand ($initialBookTop -> $midBookTop -> $expandedBookTop)", midBookTop > initialBookTop + 5f && expandedBookTop > midBookTop + 5f)
            val midDeltaDiff = kotlin.math.abs((midBookTop - initialBookTop) - (midTabTop - initialTabTop))
            assertTrue("Expected mid-expand grid shift to match category bar shift within 2px, diff=$midDeltaDiff", midDeltaDiff <= 2f)
            val expandTabDelta = expandedTabTop - initialTabTop
            val expandBookDelta = expandedBookTop - initialBookTop
            assertTrue(
                "Expected grid expand shift ($expandBookDelta) to match category bar shift ($expandTabDelta)",
                kotlin.math.abs(expandBookDelta - expandTabDelta) <= 2f,
            )

            // Collapse using the × orb and confirm smooth return (not instant snap in 1 frame)
            composeRule.onNodeWithContentDescription("收起搜索").performClick()
            composeRule.mainClock.advanceTimeBy(96L)
            val midCollapseBookTop = composeRule.onAllNodesWithText("魔女之旅").onFirst().fetchSemanticsNode().boundsInRoot.top
            val midCollapseTabTop = composeRule.onAllNodesWithText("全部", substring = true).onFirst().fetchSemanticsNode().boundsInRoot.top
            assertTrue(
                "Expected grid item to glide smoothly up on collapse ($expandedBookTop -> $midCollapseBookTop -> $initialBookTop)",
                midCollapseBookTop < expandedBookTop - 5f && midCollapseBookTop > initialBookTop + 5f,
            )
            val midCollapseDiff = kotlin.math.abs((midCollapseBookTop - initialBookTop) - (midCollapseTabTop - initialTabTop))
            assertTrue("Expected mid-collapse grid shift to match category bar shift within 2px, diff=$midCollapseDiff", midCollapseDiff <= 2f)

            composeRule.mainClock.advanceTimeBy(450L)
            val finalBookTop = composeRule.onAllNodesWithText("魔女之旅").onFirst().fetchSemanticsNode().boundsInRoot.top
            assertTrue(
                "Expected grid item to return to initial Y ($initialBookTop), got $finalBookTop",
                kotlin.math.abs(finalBookTop - initialBookTop) <= 2f,
            )
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun shelfScreen_seriesModalCloseUpdatesOpenImmediately_andKeepsStackBoundsZeroAlphaAfterCollapse() {
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = darkAppPalettes[AppThemeId.blue]!!
        val seriesBook = sampleBooks().first { it.title.contains("魔女") }
        var sheetOpen by mutableStateOf(false)
        var sheetRendered by mutableStateOf(false)
        var boundsResolver: ((String) -> Rect?)? = null
        var alphaResolver: ((String) -> Float)? = null

        composeRule.setContent {
            CompositionLocalProvider(
                LocalPowerSaveMode provides true,
                LocalReadingStyle provides style,
            ) {
                Box(modifier = Modifier.size(412.dp, 900.dp)) {
                    ShelfScreen(
                        books = sampleBooks(),
                        shelves = emptyList(),
                        palette = palette,
                        onOpenBook = {},
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                        onSeriesSheetOpenChanged = { sheetOpen = it },
                        onSeriesSheetRenderedChanged = { sheetRendered = it },
                        onRegisterBoundsResolver = { boundsResolver = it },
                        onRegisterClosingAlphaResolver = { alphaResolver = it },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("魔女之旅").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        assertTrue(sheetOpen)
        assertTrue(sheetRendered)

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithContentDescription("关闭").performClick()
            composeRule.mainClock.advanceTimeBy(32L)
            assertFalse("Expected sheetOpen to flip false immediately on close", sheetOpen)
            assertTrue("Expected sheetRendered to remain true while collapsing", sheetRendered)

            composeRule.mainClock.advanceTimeBy(550L)
            assertFalse("Expected sheetRendered to flip false once collapsed", sheetRendered)
            val resolvedRect = boundsResolver?.invoke(seriesBook.id)
            val resolvedAlpha = alphaResolver?.invoke(seriesBook.id) ?: -1f
            assertTrue("Expected stack bounds fallback after collapse, got $resolvedRect", resolvedRect != null && resolvedRect.width >= 8f)
            assertEquals(0f, resolvedAlpha, 0.001f)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun shelfBookBlock_cachesProgressLastReadAndImportedMetadata() {
        val books = listOf(
            sampleBooks()[0].copy(progress = 0.4, lastReadAtMillis = null, importedAtMillis = 100L),
            sampleBooks()[1].copy(progress = 0.8, lastReadAtMillis = 900L, importedAtMillis = 250L),
        )
        val block = ShelfBookBlock(
            key = "series::test",
            title = "测试系列",
            author = "作者",
            books = books,
        )
        assertEquals(0.6, block.progress, 0.0001)
        assertEquals(900L, block.lastReadAtMillis)
        assertEquals(250L, block.importedAtMillis)

        val emptyBlock = ShelfBookBlock("empty", "空", "", emptyList())
        assertEquals(0.0, emptyBlock.progress, 0.0001)
        assertEquals(null, emptyBlock.lastReadAtMillis)
        assertEquals(0L, emptyBlock.importedAtMillis)
    }

    @Test
    fun shelfScreen_acceleratedSeriesClose_collapsesQuicklyViaDismissCallback() {
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = darkAppPalettes[AppThemeId.blue]!!
        var sheetOpen by mutableStateOf(false)
        var sheetRendered by mutableStateOf(false)
        var accelerateClose by mutableStateOf(false)
        var dismissSeries: (() -> Unit)? = null

        composeRule.setContent {
            CompositionLocalProvider(
                LocalPowerSaveMode provides true,
                LocalReadingStyle provides style,
            ) {
                Box(modifier = Modifier.size(412.dp, 900.dp)) {
                    ShelfScreen(
                        books = sampleBooks(),
                        shelves = emptyList(),
                        palette = palette,
                        onOpenBook = {},
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                        onSeriesSheetOpenChanged = { sheetOpen = it },
                        onSeriesSheetRenderedChanged = { sheetRendered = it },
                        accelerateSeriesClose = accelerateClose,
                        onRegisterSeriesDismiss = { dismissSeries = it },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("魔女之旅").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        assertTrue(sheetOpen)
        assertTrue(sheetRendered)
        assertTrue(dismissSeries != null)

        composeRule.mainClock.autoAdvance = false
        try {
            accelerateClose = true
            composeRule.runOnIdle { dismissSeries?.invoke() }
            composeRule.mainClock.advanceTimeBy(32L)
            assertFalse("Expected sheetOpen to flip false immediately on double-back dismiss", sheetOpen)
            assertTrue("Expected sheetRendered to remain true mid-collapse at 32ms", sheetRendered)

            composeRule.mainClock.advanceTimeBy(100L)
            assertTrue("Expected curved double-back collapse to stay smoothly animating past 130ms", sheetRendered)

            composeRule.mainClock.advanceTimeBy(320L)
            assertFalse("Expected accelerated double-back collapse to settle cleanly within ~450ms", sheetRendered)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun ambientSubpageBackground_tracksLocalAmbientGlowColorsStateSmoothly() {
        var primaryColor by mutableStateOf(Color(0xFF3E6BB8))
        val style = ReadingStyle(appTheme = AppThemeId.blue, ambientGlowSource = AmbientGlowSource.bookCover)
        val palette = style.resolvePalette(isLight = false)
        composeRule.setContent {
            val glowState = remember { derivedStateOf { primaryColor to Color(0xFF3FA67E) } }
            CompositionLocalProvider(
                LocalReadingStyle provides style,
                LocalAmbientGlowColorsState provides glowState,
            ) {
                Box(modifier = Modifier.size(390.dp, 844.dp)) {
                    AmbientSubpageBackground(palette = palette)
                    Text("AmbientStateReady")
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("AmbientStateReady").assertIsDisplayed()

        primaryColor = Color(0xFFD96A58)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("AmbientStateReady").assertIsDisplayed()
    }

    @Test
    fun shelfScreen_multiSeriesSwitchingAndRecentReordering_worksCleanlyAcrossDoubleBack() {
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = darkAppPalettes[AppThemeId.blue]!!
        fun mkBook(id: String, title: String, lastRead: Long) = BookEntry(
            id = id,
            title = title,
            author = "作者",
            format = BookFormat.epub,
            bookDir = "/tmp/$id",
            sourcePath = "/tmp/$id.epub",
            importedAtMillis = 1_700_000_000_000L,
            chapters = listOf(ReaderChapter(title = "第一章", href = "ch1.xhtml", filePath = "ch1.xhtml")),
            progress = 0.2,
            lastReadAtMillis = lastRead,
        )

        var booksState by mutableStateOf(
            listOf(
                mkBook("a1", "魔女之旅 第1卷", 1_700_000_300_000L),
                mkBook("a2", "魔女之旅 第2卷", 1_700_000_250_000L),
                mkBook("b1", "无职转生 第1卷", 1_700_000_100_000L),
                mkBook("b2", "无职转生 第2卷", 1_700_000_050_000L),
            ),
        )
        var openedBookIds = mutableListOf<String>()
        var sheetOpen by mutableStateOf(false)
        var sheetRendered by mutableStateOf(false)
        var readerActive by mutableStateOf(false)
        var accelerateClose by mutableStateOf(false)
        var dismissSeries: (() -> Unit)? = null

        composeRule.setContent {
            CompositionLocalProvider(
                LocalPowerSaveMode provides true,
                LocalReadingStyle provides style,
            ) {
                Box(modifier = Modifier.size(412.dp, 900.dp)) {
                    ShelfScreen(
                        books = booksState,
                        shelves = emptyList(),
                        palette = palette,
                        onOpenBook = { book ->
                            openedBookIds.add(book.id)
                            readerActive = true
                        },
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                        onSeriesSheetOpenChanged = { sheetOpen = it },
                        onSeriesSheetRenderedChanged = { sheetRendered = it },
                        accelerateSeriesClose = accelerateClose,
                        isReaderOverlayActive = readerActive,
                        onRegisterSeriesDismiss = { dismissSeries = it },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Cycle 1: open series B ("无职转生"), tap Vol 2, simulate double-back + Recent timestamp bump
        composeRule.onAllNodesWithText("无职转生").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        assertTrue(sheetOpen)
        // Toggle animated grid/list orb button inside SeriesModal
        composeRule.onNodeWithContentDescription("切换列表视图").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("切换网格视图").performClick()
        composeRule.waitForIdle()

        val seriesBNodes = composeRule.onAllNodes(hasText("无职转生 第2卷") and hasClickAction())
        seriesBNodes[seriesBNodes.fetchSemanticsNodes().lastIndex].performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("b2"), openedBookIds)

        accelerateClose = true
        composeRule.runOnIdle { dismissSeries?.invoke() }
        composeRule.mainClock.advanceTimeBy(280L)
        composeRule.waitForIdle()
        readerActive = false
        accelerateClose = false
        booksState = booksState.map {
            if (it.id == "b2") it.copy(lastReadAtMillis = 1_700_000_900_000L, progress = 0.5) else it
        }
        composeRule.waitForIdle()
        assertFalse(sheetOpen)
        assertFalse(sheetRendered)

        // Cycle 2: immediately open series A ("魔女之旅"), tap Vol 1 inside modal, rapid double-back settle
        composeRule.onAllNodesWithText("魔女之旅").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        assertTrue(sheetOpen)
        val seriesANodes = composeRule.onAllNodes(hasText("魔女之旅 第1卷") and hasClickAction())
        seriesANodes[seriesANodes.fetchSemanticsNodes().lastIndex].performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("b2", "a1"), openedBookIds)

        accelerateClose = true
        composeRule.runOnIdle { dismissSeries?.invoke() }
        composeRule.mainClock.advanceTimeBy(280L)
        composeRule.waitForIdle()
        assertFalse(sheetOpen)
        assertFalse(sheetRendered)
    }

    @Test
    fun shelfScreen_preservesGlassAndSettlesFastCloseAcrossRecentSortReorderAndTabSwitch() {
        val style = ReadingStyle(appTheme = AppThemeId.blue)
        val palette = style.resolvePalette(isLight = true)
        fun mkBook(id: String, title: String, lastRead: Long, shelfName: String? = null) = BookEntry(
            id = id,
            title = title,
            author = "作者",
            format = BookFormat.epub,
            bookDir = "/tmp/$id",
            sourcePath = "/tmp/$id.epub",
            importedAtMillis = 1_700_000_000_000L,
            chapters = listOf(ReaderChapter(title = "第一章", href = "ch1.xhtml", filePath = "ch1.xhtml")),
            progress = 0.2,
            lastReadAtMillis = lastRead,
            shelfName = shelfName,
        )

        var booksState by mutableStateOf(
            listOf(
                mkBook("a1", "魔女之旅 第1卷", 1_700_000_300_000L),
                mkBook("a2", "魔女之旅 第2卷", 1_700_000_250_000L),
                mkBook("b1", "无职转生 第1卷", 1_700_000_100_000L, shelfName = "轻小说"),
                mkBook("b2", "无职转生 第2卷", 1_700_000_050_000L, shelfName = "轻小说"),
            ),
        )
        var sheetOpen by mutableStateOf(false)
        var sheetRendered by mutableStateOf(false)
        var isVisible by mutableStateOf(true)
        var readerOverlayActive by mutableStateOf(false)
        var accelerateClose by mutableStateOf(false)
        var dismissSeries: (() -> Unit)? = null

        composeRule.setContent {
            val sharedBackdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalPowerSaveMode provides false,
                LocalReadingStyle provides style,
            ) {
                Box(modifier = Modifier.size(412.dp, 900.dp)) {
                    ShelfScreen(
                        books = booksState,
                        shelves = listOf("轻小说"),
                        palette = palette,
                        onOpenBook = {
                            readerOverlayActive = true
                            isVisible = false
                        },
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                        onSeriesSheetOpenChanged = { sheetOpen = it },
                        onSeriesSheetRenderedChanged = { sheetRendered = it },
                        accelerateSeriesClose = accelerateClose,
                        isReaderOverlayActive = readerOverlayActive,
                        onRegisterSeriesDismiss = { dismissSeries = it },
                        isActive = true,
                        isVisible = isVisible,
                        sharedBackdrop = sharedBackdrop,
                        requireDockBackdrop = isVisible,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Open series under Normal Glass mode and launch Vol 2 -> overlay sets isVisible = false
        composeRule.onAllNodesWithText("无职转生").onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(450L)
        composeRule.waitForIdle()
        assertTrue(sheetOpen)

        val seriesBNodes = composeRule.onAllNodes(hasText("无职转生 第2卷") and hasClickAction())
        seriesBNodes[seriesBNodes.fetchSemanticsNodes().lastIndex].performClick()
        composeRule.waitForIdle()
        assertTrue(readerOverlayActive)
        assertFalse(isVisible)

        // Rapid double-back closes Reader and SeriesModal cleanly within 220ms under Normal Glass
        accelerateClose = true
        composeRule.runOnIdle { dismissSeries?.invoke() }
        composeRule.mainClock.advanceTimeBy(220L)
        composeRule.waitForIdle()
        assertFalse(sheetOpen)
        assertFalse(sheetRendered)

        isVisible = true
        readerOverlayActive = false
        accelerateClose = false
        // Reorder via Recent timestamp bump and verify ShelfTabSwitchKey + tab switching stay accurate
        booksState = booksState.map {
            if (it.id == "b2") it.copy(lastReadAtMillis = 1_700_000_900_000L, progress = 0.65) else it
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("无职转生").onFirst().assertIsDisplayed()

        composeRule.onAllNodesWithText("在读", substring = true).onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(320L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("无职转生").onFirst().assertIsDisplayed()

        composeRule.onAllNodesWithText("未读", substring = true).onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(320L)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("全部", substring = true).onFirst().performClick()
        composeRule.mainClock.advanceTimeBy(320L)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("魔女之旅").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("无职转生").onFirst().assertIsDisplayed()
    }

    @Test
    fun readingNowScreen_offscreenFreezesAmbientGlowStateReadsAndResumesOnReveal() {
        val backingGlow = mutableStateOf(Color(0xFF3366CC) to Color(0xFF112244))
        var countingEnabled = false
        var readsWhileCounting = 0
        val trackedGlowState = object : State<Pair<Color, Color>> {
            override val value: Pair<Color, Color>
                get() {
                    if (countingEnabled) {
                        readsWhileCounting++
                    }
                    return backingGlow.value
                }
        }
        var isActive by mutableStateOf(true)
        var isVisible by mutableStateOf(true)
        val books = sampleBooks()
        val palette = darkAppPalettes[AppThemeId.blue]!!

        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
                LocalAmbientGlowColorsState provides trackedGlowState,
            ) {
                Box(modifier = Modifier.size(412.dp, 880.dp)) {
                    ReadingNowScreen(
                        books = books,
                        todaySeconds = 1200,
                        weekSeconds = 2400,
                        monthSeconds = 3600,
                        weekDailySeconds = listOf(200, 300, 100, 400, 0, 200, 0),
                        palette = palette,
                        onOpenBook = {},
                        onOpenShelf = {},
                        onOpenStats = {},
                        onToggleBrightness = {},
                        isActive = isActive,
                        isVisible = isVisible,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        isActive = false
        isVisible = false
        composeRule.waitForIdle()

        readsWhileCounting = 0
        countingEnabled = true
        backingGlow.value = Color(0xFFAA3355) to Color(0xFF441122)
        backingGlow.value = Color(0xFF228855) to Color(0xFF114422)
        composeRule.waitForIdle()
        assertEquals(
            "Offscreen ReadingNowScreen must not subscribe to live ambient glow updates",
            0,
            readsWhileCounting,
        )

        isActive = true
        isVisible = true
        composeRule.waitForIdle()
        assertTrue(
            "Visible ReadingNowScreen must resume observing live ambient glow updates",
            readsWhileCounting > 0,
        )
    }

    @Test
    fun shelfScreen_cachedSeriesCovers_openCloseAndStatsScreenBoxReplacementComposeCleanly() {
        val rawBmp = Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(64, 120, 210))
        }
        val thumb = rawBmp.asImageBitmap()
        fun mkSeriesBook(id: String, title: String, series: String, lastRead: Long) = BookEntry(
            id = id,
            title = title,
            author = "作者",
            format = BookFormat.epub,
            bookDir = "/tmp/$id",
            sourcePath = "/tmp/$id.epub",
            importedAtMillis = 1_700_000_000_000L,
            chapters = listOf(ReaderChapter(title = "第一章", href = "ch1.xhtml", filePath = "ch1.xhtml")),
            coverPath = "/tmp/$id.jpg",
            progress = 0.25,
            lastReadAtMillis = lastRead,
            seriesOverride = series,
        )
        val initialBooks = listOf(
            mkSeriesBook("cached-majo-1", "魔女之旅 01", "魔女之旅", 100L),
            mkSeriesBook("cached-majo-2", "魔女之旅 02", "魔女之旅", 90L),
            mkSeriesBook("cached-mushoku-1", "无职转生 01", "无职转生", 200L),
            mkSeriesBook("cached-mushoku-2", "无职转生 02", "无职转生", 190L),
        )
        initialBooks.forEach { CoverAssetCache.putThumbIfAbsent(it.id, it.coverPath, thumb) }

        var booksState by mutableStateOf(initialBooks)
        var sheetOpen = false
        var sheetRendered = false
        var accelerateClose by mutableStateOf(false)
        var dismissSeries: (() -> Unit)? = null
        var showStats by mutableStateOf(false)
        val palette = darkAppPalettes[AppThemeId.blue]!!

        composeRule.setContent {
            val backdrop = rememberGlassBackdrop()
            CompositionLocalProvider(
                LocalReadingStyle provides ReadingStyle(),
                LocalVitroBackdrop provides backdrop,
            ) {
                Box(modifier = Modifier.size(412.dp, 880.dp)) {
                    ShelfScreen(
                        books = booksState,
                        shelves = listOf("轻小说"),
                        palette = palette,
                        onOpenBook = {},
                        onImportFilesClick = {},
                        onImportFolderClick = {},
                        onDeleteBooks = {},
                        onMoveBooksToShelf = { _, _ -> },
                        onGroupSeries = { _, _ -> },
                        onRemoveFromSeries = {},
                        onUpdateBookMeta = { _, _, _ -> },
                        onAddShelf = {},
                        onDeleteShelf = {},
                        onSeriesSheetOpenChanged = { sheetOpen = it },
                        onSeriesSheetRenderedChanged = { sheetRendered = it },
                        accelerateSeriesClose = accelerateClose,
                        onRegisterSeriesDismiss = { dismissSeries = it },
                        isActive = !showStats,
                        isVisible = !showStats,
                    )
                    if (showStats) {
                        StatsScreen(
                            books = booksState,
                            dailyTotals = mapOf("2026-11-15" to 900),
                            bookStats = emptyMap(),
                            palette = palette,
                            onOpenBook = {},
                            onOpenDetail = {},
                            sharedBackdrop = backdrop,
                            isVisible = true,
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("魔女之旅").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue(sheetOpen)
        composeRule.onNodeWithText("魔女之旅 02").assertIsDisplayed()

        accelerateClose = true
        composeRule.runOnIdle { dismissSeries?.invoke() }
        composeRule.waitForIdle()
        assertFalse(sheetOpen)
        assertFalse(sheetRendered)

        booksState = booksState.map {
            if (it.id == "cached-majo-2") it.copy(lastReadAtMillis = 999L) else it
        }
        showStats = true
        composeRule.waitForIdle()
    }
}
