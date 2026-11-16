package com.squartor.reader.ui.screens

import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squartor.reader.AmbientSubpageBackground
import com.squartor.reader.model.AmbientGlowSource
import com.squartor.reader.model.AppBrightnessMode
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.AppThemeId
import com.squartor.reader.model.CloudSyncSettings
import com.squartor.reader.model.GlassClarityMode
import com.squartor.reader.model.ImportedFont
import com.squartor.reader.model.ReaderBackgroundId
import com.squartor.reader.model.ReaderPalette
import com.squartor.reader.model.ReadingFlowMode
import com.squartor.reader.model.ReadingStyle
import com.squartor.reader.model.StorageStats
import com.squartor.reader.model.readerPalettes
import com.squartor.reader.model.resolveThemeReaderPalette
import com.squartor.reader.ui.glass.Ios27GlassToggle
import com.squartor.reader.ui.glass.Ios27PrecisionSlider
import com.squartor.reader.ui.glass.IsolatedAmbientGlassContainer
import com.squartor.reader.ui.glass.LocalAmbientGlowColors
import com.squartor.reader.ui.glass.LocalAmbientGlowColorsState
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.ios27ElevatedDockSurface
import com.squartor.reader.ui.glass.ios27LiftedInnerPillSurface
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BUILTIN_VF_FONT_NAME = "SQuartorReaderFont"
private const val BUILTIN_VF_FONT_PATH = "asset://fonts/NotoSansSC-VF.ttf"

private enum class SettingsSubPage(val title: String) {
    ThemeAndPaper("主题与纸张"),
    FontAndWeight("字体与字重"),
    Typography("排版与翻页"),
    CloudSync("云同步与备份"),
    StorageAndCache("存储与缓存"),
    About("关于 SQuartor"),
}

private val SYNC_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatSyncTimestamp(millis: Long?): String {
    if (millis == null || millis <= 0L) return "暂无"
    return runCatching {
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(SYNC_TIME_FORMATTER)
    }.getOrDefault("暂无")
}

private fun fontWeightLabel(weight: Int): String = when {
    weight <= 250 -> "极细"
    weight <= 350 -> "细体"
    weight <= 450 -> "常规"
    weight <= 550 -> "中等"
    weight <= 650 -> "半粗"
    weight <= 750 -> "粗体"
    else -> "极粗"
}

private fun resolvePreviewPaperPalette(
    style: ReadingStyle,
    palette: AppPalette,
    isLight: Boolean = palette.isLight,
): ReaderPalette {
    if (style.readerBackground == ReaderBackgroundId.theme) {
        return resolveThemeReaderPalette(palette)
    }
    val base = readerPalettes[style.readerBackground] ?: resolveThemeReaderPalette(palette)
    if (style.autoNightPaper && !isLight &&
        (base.id == ReaderBackgroundId.paper || base.id == ReaderBackgroundId.green)
    ) {
        return readerPalettes[ReaderBackgroundId.warm] ?: base
    }
    return base
}

@Composable
private fun rememberPreviewFontFamily(
    fontName: String?,
    fontPath: String?,
    weight: Int,
): FontFamily {
    val context = LocalContext.current
    val isSystem = fontName == null && fontPath == null
    val isBuiltInVf = fontName == BUILTIN_VF_FONT_NAME || fontPath == BUILTIN_VF_FONT_PATH
    return remember(context, fontPath, fontName, weight) {
        val clamped = weight.coerceIn(100, 900)
        if (isSystem) {
            FontFamily.Default
        } else {
            runCatching {
                val tf: Typeface? = if (isBuiltInVf) {
                    Typeface.Builder(context.assets, "fonts/NotoSansSC-VF.ttf")
                        .setFontVariationSettings("'wght' $clamped")
                        .build()
                        ?: Typeface.createFromAsset(context.assets, "fonts/NotoSansSC-VF.ttf")
                } else {
                    val cleanPath = fontPath?.removePrefix("file://")?.trim().orEmpty()
                    val fontFile = File(cleanPath)
                    if (fontFile.exists() && fontFile.isFile && fontFile.canRead() && fontFile.length() > 0L) {
                        Typeface.Builder(fontFile)
                            .setFontVariationSettings("'wght' $clamped")
                            .build()
                            ?: Typeface.createFromFile(fontFile)
                    } else {
                        null
                    }
                }
                if (tf != null) FontFamily(tf) else FontFamily.Default
            }.getOrElse { FontFamily.Default }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    style: ReadingStyle,
    fonts: List<ImportedFont>,
    cloudSync: CloudSyncSettings,
    storageStats: StorageStats,
    palette: AppPalette,
    onUpdateStyle: (ReadingStyle) -> Unit,
    onToggleBrightness: (Offset) -> Unit,
    themeRevealButtonProgress: Float = 1f,
    onImportFontClick: () -> Unit,
    onDeleteFont: (ImportedFont) -> Unit,
    onUpdateCloudSync: (CloudSyncSettings) -> Unit,
    onSyncUpload: () -> Unit,
    onSyncDownload: () -> Unit,
    onClearCache: () -> Unit,
    onSubPageActiveChanged: (Boolean) -> Unit = {},
    isSyncing: Boolean = false,
    syncFeedbackMessage: String? = null,
    onDismissSyncFeedback: () -> Unit = {},
    sharedBackdrop: GlassBackdrop? = null,
    requireDockBackdrop: Boolean = false,
    isVisible: Boolean = true,
) {
    var activeSubPage by rememberSaveable { mutableStateOf<SettingsSubPage?>(null) }
    val catalogListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var edgeOffsetPx by remember { mutableFloatStateOf(0f) }
    var isEdgeDragging by remember { mutableStateOf(false) }
    var skipNextPopTransition by remember { mutableStateOf(false) }
    var edgeSettleJob by remember { mutableStateOf<Job?>(null) }

    fun popSubPageStandard() {
        edgeSettleJob?.cancel()
        edgeOffsetPx = 0f
        isEdgeDragging = false
        skipNextPopTransition = false
        activeSubPage = null
    }

    LaunchedEffect(activeSubPage != null) {
        onSubPageActiveChanged(activeSubPage != null)
    }

    LaunchedEffect(activeSubPage) {
        if (activeSubPage != null) {
            edgeSettleJob?.cancel()
            edgeOffsetPx = 0f
            isEdgeDragging = false
            skipNextPopTransition = false
        } else if (skipNextPopTransition) {
            skipNextPopTransition = false
        }
    }

    LaunchedEffect(syncFeedbackMessage) {
        if (!syncFeedbackMessage.isNullOrBlank()) {
            delay(4000L)
            onDismissSyncFeedback()
        }
    }

    BackHandler(enabled = activeSubPage != null) {
        popSubPageStandard()
    }

    val settingsWindowWidthPx = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1)
    var settingsBoxWidthPx by remember { mutableIntStateOf(settingsWindowWidthPx) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sz ->
                if (sz.width > 0 && settingsBoxWidthPx != sz.width) {
                    settingsBoxWidthPx = sz.width
                }
            },
    ) {
        val fullWidthPx = settingsBoxWidthPx.toFloat().coerceAtLeast(1f)
        val edgeZonePx = with(density) { 32.dp.toPx() }
        val minFlingDragPx = with(density) { 10.dp.toPx() }
        val shadowWidthDp = 16.dp
        val shadowWidthPx = with(density) { shadowWidthDp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeSubPage != null, fullWidthPx) {
                    if (activeSubPage == null) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        if (down.position.x > edgeZonePx) return@awaitEachGesture

                        val startPos = down.position
                        var pointerId = down.id
                        var lockedHorizontal = false
                        val touchSlop = viewConfiguration.touchSlop
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) break
                            pointerId = change.id

                            val dx = change.position.x - startPos.x
                            val dy = change.position.y - startPos.y

                            if (!lockedHorizontal) {
                                if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                                    break
                                }
                                if (dx > touchSlop && dx > abs(dy) * 1.1f) {
                                    lockedHorizontal = true
                                    edgeSettleJob?.cancel()
                                    isEdgeDragging = true
                                    change.consume()
                                }
                            }

                            if (lockedHorizontal) {
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                edgeOffsetPx = (dx - touchSlop).coerceIn(0f, fullWidthPx)
                                change.consume()
                            }
                        }

                        if (lockedHorizontal) {
                            val vx = velocityTracker.calculateVelocity().x
                            val shouldPop = edgeOffsetPx > fullWidthPx * 0.33f ||
                                (vx > 700f && edgeOffsetPx > minFlingDragPx)
                            isEdgeDragging = false
                            val startVal = edgeOffsetPx
                            if (shouldPop) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                edgeSettleJob = scope.launch {
                                    animate(
                                        initialValue = startVal,
                                        targetValue = fullWidthPx,
                                        initialVelocity = vx.coerceAtLeast(0f),
                                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                                    ) { value, _ -> edgeOffsetPx = value }
                                    skipNextPopTransition = true
                                    activeSubPage = null
                                    edgeOffsetPx = 0f
                                }
                            } else {
                                edgeSettleJob = scope.launch {
                                    animate(
                                        initialValue = startVal,
                                        targetValue = 0f,
                                        initialVelocity = vx,
                                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                                    ) { value, _ -> edgeOffsetPx = value }
                                    edgeOffsetPx = 0f
                                }
                            }
                        }
                    }
                },
        ) {
            val swipeActive by remember {
                derivedStateOf { activeSubPage != null && (isEdgeDragging || edgeOffsetPx > 0.5f) }
            }
            val shadowVisible by remember {
                derivedStateOf { swipeActive && edgeOffsetPx > 1f }
            }
            val edgeShadowBrush = remember(palette.isLight) {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = if (palette.isLight) 0.10f else 0.34f),
                    ),
                )
            }

            if (swipeActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = (edgeOffsetPx / fullWidthPx).coerceIn(0f, 1f)
                            translationX = -fullWidthPx * 0.28f * (1f - p)
                        },
                ) {
                    MainSettingsCatalog(
                        style = style,
                        cloudSync = cloudSync,
                        storageStats = storageStats,
                        palette = palette,
                        isSyncing = isSyncing,
                        listState = catalogListState,
                        showFloatingChrome = false,
                        sharedBackdrop = null,
                        requireDockBackdrop = false,
                        isVisible = false,
                        onOpenSubPage = {},
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val p = (edgeOffsetPx / fullWidthPx).coerceIn(0f, 1f)
                            drawRect(
                                Color.Black.copy(
                                    alpha = (if (palette.isLight) 0.09f else 0.28f) * (1f - p),
                                ),
                            )
                        },
                )
            }

            if (shadowVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(shadowWidthDp)
                        .graphicsLayer {
                            translationX = edgeOffsetPx - shadowWidthPx
                            alpha = (1f - (edgeOffsetPx / fullWidthPx).coerceIn(0f, 1f))
                        }
                        .background(edgeShadowBrush),
                )
            }

            AnimatedContent(
                targetState = activeSubPage,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = if (activeSubPage != null) edgeOffsetPx else 0f
                    },
                transitionSpec = {
                    if (targetState == null && skipNextPopTransition) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else if (targetState != null) {
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.86f,
                                stiffness = 380f,
                                visibilityThreshold = IntOffset(1, 1),
                            ),
                            initialOffsetX = { fullWidth -> fullWidth },
                        ) togetherWith slideOutHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.86f,
                                stiffness = 380f,
                                visibilityThreshold = IntOffset(1, 1),
                            ),
                            targetOffsetX = { fullWidth -> -(fullWidth * 0.28f).roundToInt() },
                        )
                    } else {
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.86f,
                                stiffness = 380f,
                                visibilityThreshold = IntOffset(1, 1),
                            ),
                            initialOffsetX = { fullWidth -> -(fullWidth * 0.28f).roundToInt() },
                        ) togetherWith slideOutHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.86f,
                                stiffness = 380f,
                                visibilityThreshold = IntOffset(1, 1),
                            ),
                            targetOffsetX = { fullWidth -> fullWidth },
                        )
                    }
                },
                label = "settingsSubPageSlide",
            ) { subPage ->
                when (subPage) {
                    null -> {
                        MainSettingsCatalog(
                            style = style,
                            cloudSync = cloudSync,
                            storageStats = storageStats,
                            palette = palette,
                            isSyncing = isSyncing,
                            listState = catalogListState,
                            showFloatingChrome = activeSubPage == null,
                            sharedBackdrop = sharedBackdrop,
                            requireDockBackdrop = requireDockBackdrop,
                            isVisible = isVisible,
                            onOpenSubPage = { next ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeSubPage = next
                            },
                        )
                    }
                    SettingsSubPage.ThemeAndPaper -> {
                        ThemeAndPaperSubPage(
                            style = style,
                            palette = palette,
                            onUpdateStyle = onUpdateStyle,
                            onBack = ::popSubPageStandard,
                        )
                    }
                    SettingsSubPage.FontAndWeight -> {
                        FontAndWeightSubPage(
                            style = style,
                            fonts = fonts,
                            palette = palette,
                            onUpdateStyle = onUpdateStyle,
                            onImportFontClick = onImportFontClick,
                            onDeleteFont = onDeleteFont,
                            onBack = ::popSubPageStandard,
                        )
                    }
                    SettingsSubPage.Typography -> {
                        TypographySubPage(
                            style = style,
                            palette = palette,
                            onUpdateStyle = onUpdateStyle,
                            onBack = ::popSubPageStandard,
                        )
                    }
                    SettingsSubPage.CloudSync -> {
                        CloudSyncSubPage(
                            cloudSync = cloudSync,
                            palette = palette,
                            isSyncing = isSyncing,
                            syncFeedbackMessage = syncFeedbackMessage,
                            onDismissSyncFeedback = onDismissSyncFeedback,
                            onUpdateCloudSync = onUpdateCloudSync,
                            onSyncUpload = onSyncUpload,
                            onSyncDownload = onSyncDownload,
                            onBack = ::popSubPageStandard,
                        )
                    }
                    SettingsSubPage.StorageAndCache -> {
                        StorageAndCacheSubPage(
                            storageStats = storageStats,
                            palette = palette,
                            syncFeedbackMessage = syncFeedbackMessage,
                            onDismissSyncFeedback = onDismissSyncFeedback,
                            onClearCache = onClearCache,
                            onBack = ::popSubPageStandard,
                        )
                    }
                    SettingsSubPage.About -> {
                        AboutSubPage(
                            palette = palette,
                            onBack = ::popSubPageStandard,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainSettingsCatalog(
    style: ReadingStyle,
    cloudSync: CloudSyncSettings,
    storageStats: StorageStats,
    palette: AppPalette,
    isSyncing: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    showFloatingChrome: Boolean = true,
    sharedBackdrop: GlassBackdrop? = null,
    requireDockBackdrop: Boolean = false,
    isVisible: Boolean = true,
    onOpenSubPage: (SettingsSubPage) -> Unit,
) {
    val activeReaderFontLabel = when {
        style.fontName == BUILTIN_VF_FONT_NAME || style.fontPath == BUILTIN_VF_FONT_PATH -> "SQuartor VF"
        !style.fontName.isNullOrBlank() -> style.fontName
        else -> "系统默认"
    }
    val brightnessSummary = when (style.brightnessMode) {
        AppBrightnessMode.system -> "跟随系统"
        AppBrightnessMode.scheduled -> "定时"
        AppBrightnessMode.light -> "浅色"
        AppBrightnessMode.dark -> "深色"
    }
    val cloudSummary = when {
        isSyncing -> "同步中"
        cloudSync.configured -> "已配置"
        else -> "未配置"
    }

    val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val vitroBackdrop = sharedBackdrop ?: rememberGlassBackdrop()
    val powerSave = LocalPowerSaveMode.current
    val headerScrollThresholdPx = with(density) { 42.dp.toPx().roundToInt() }
    val scrimRampPx = with(density) { 28.dp.toPx() }.coerceAtLeast(1f)
    val isScrolled by remember {
        derivedStateOf {
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            when {
                firstItem == null -> false
                firstItem.index > 0 -> true
                else -> listState.firstVisibleItemScrollOffset >=
                    (firstItem.size * 0.85f).roundToInt().coerceAtLeast(headerScrollThresholdPx)
            }
        }
    }
    val topScrimAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / scrimRampPx).coerceIn(0f, 1f)
        }
    }
    val largeTitleAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0f else 1f,
        animationSpec = tween(150),
        label = "rootLargeTitleAlpha",
    )
    val effectiveBackdrop = if (powerSave) null else vitroBackdrop

    Box(modifier = Modifier.fillMaxSize()) {
        // Wrap background + LazyColumn in IsolatedAmbientGlassContainer so Dock refracts scrolled rows
        // cleanly without the bottom corner ambient glow staining inside the Dock capsule.
        IsolatedAmbientGlassContainer(
            backdrop = if (isVisible || sharedBackdrop != null) effectiveBackdrop else null,
            paletteBackgroundColor = palette.background,
            ambientBackground = {
                AmbientSubpageBackground(
                    palette = palette,
                    topOnlyWhenPagerTransitioning = true,
                    isAnimating = isVisible,
                )
            },
        ) {
            CompositionLocalProvider(LocalVitroBackdrop provides null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = topInset + 18.dp,
                        end = 20.dp,
                        bottom = bottomInset + 116.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item {
                        Text(
                            text = "设置",
                            color = palette.text,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier
                                .padding(start = 4.dp, top = 4.dp)
                                .graphicsLayer { alpha = largeTitleAlpha },
                        )
                    }

                    item {
                        SettingsGroupCard(
                            title = "外观与阅读",
                            palette = palette,
                        ) {
                            SettingsEntryRow(
                                icon = Icons.Rounded.FormatPaint,
                                title = "主题与纸张",
                                value = "$brightnessSummary · ${(style.glassOpacityLevel * 100).roundToInt()}%",
                                palette = palette,
                                onClick = { onOpenSubPage(SettingsSubPage.ThemeAndPaper) },
                            )
                            Ios27GroupedDivider(palette, startIndent = 52.dp)
                            SettingsEntryRow(
                                icon = Icons.Rounded.FontDownload,
                                title = "字体与字重",
                                value = "$activeReaderFontLabel · W${style.fontWeightValue}",
                                palette = palette,
                                onClick = { onOpenSubPage(SettingsSubPage.FontAndWeight) },
                            )
                            Ios27GroupedDivider(palette, startIndent = 52.dp)
                            SettingsEntryRow(
                                icon = Icons.Rounded.FormatSize,
                                title = "排版与翻页",
                                value = "${style.fontSize.roundToInt()}pt · ${style.readingFlow.label}",
                                palette = palette,
                                onClick = { onOpenSubPage(SettingsSubPage.Typography) },
                            )
                        }
                    }

                    item {
                        SettingsGroupCard(
                            title = "数据与存储",
                            palette = palette,
                        ) {
                            SettingsEntryRow(
                                icon = Icons.Rounded.CloudSync,
                                title = "云同步与备份",
                                value = cloudSummary,
                                palette = palette,
                                onClick = { onOpenSubPage(SettingsSubPage.CloudSync) },
                            )
                            Ios27GroupedDivider(palette, startIndent = 52.dp)
                            SettingsEntryRow(
                                icon = Icons.Rounded.Storage,
                                title = "存储与缓存",
                                value = StorageStats.formatBytes(storageStats.totalBytes),
                                palette = palette,
                                onClick = { onOpenSubPage(SettingsSubPage.StorageAndCache) },
                            )
                        }
                    }

                    item {
                        SettingsGroupCard(
                            title = "关于",
                            palette = palette,
                        ) {
                            SettingsEntryRow(
                                icon = Icons.Rounded.Info,
                                title = "关于 SQuartor",
                                value = "v2.0.0",
                                palette = palette,
                                onClick = { onOpenSubPage(SettingsSubPage.About) },
                            )
                        }
                    }
                }
            }
        }

        SettingsTopScrollFadeScrim(
            topInset = topInset,
            alpha = topScrimAlpha,
            palette = palette,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Scrolled floating 3D Dock title pill sits as a sibling above the backdrop and scrim layers.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, top = topInset + 10.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = showFloatingChrome && isScrolled,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140)),
            ) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .ios27ElevatedDockSurface(
                            shape = CircleShape,
                            palette = palette,
                            style = style,
                            backdrop = if (isScrolled) effectiveBackdrop else null,
                            shadowScale = 0.88f,
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "设置",
                        color = palette.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeAndPaperSubPage(
    style: ReadingStyle,
    palette: AppPalette,
    onUpdateStyle: (ReadingStyle) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val currentStyle by rememberUpdatedState(style)
    var isScrubbingOpacity by remember { mutableStateOf(false) }
    var localOpacity by remember { mutableFloatStateOf(style.glassOpacityLevel) }
    LaunchedEffect(style.glassOpacityLevel) {
        if (!isScrubbingOpacity) {
            localOpacity = style.glassOpacityLevel
        }
    }

    val liveStyle = remember(style, localOpacity) {
        style.copy(glassOpacityLevel = localOpacity)
    }

    val previewIsLight = when (style.brightnessMode) {
        AppBrightnessMode.light -> true
        AppBrightnessMode.dark -> false
        AppBrightnessMode.scheduled -> style.isScheduledLight()
        AppBrightnessMode.system -> palette.isLight
    }
    val previewPalette = remember(style.appTheme, style.customThemeColorValue, previewIsLight, palette.primary) {
        style.resolvePalette(
            isLight = previewIsLight,
            dynamicSeedColor = if (style.appTheme == AppThemeId.wallpaper) palette.primary else null,
        )
    }
    val previewPaper = remember(style.readerBackground, style.autoNightPaper, previewIsLight, previewPalette) {
        resolvePreviewPaperPalette(
            style = style,
            palette = previewPalette,
            isLight = previewIsLight,
        )
    }
    val previewFont = rememberPreviewFontFamily(
        fontName = style.fontName,
        fontPath = style.fontPath,
        weight = style.fontWeightValue,
    )

    val brightnessPillOptions = remember {
        listOf(
            AppBrightnessMode.system to "跟随系统",
            AppBrightnessMode.light to "浅色",
            AppBrightnessMode.dark to "深色",
        )
    }
    val clarityOptions = remember {
        listOf(
            GlassClarityMode.clear to "通透",
            GlassClarityMode.regular to "标准",
            GlassClarityMode.tinted to "高对比",
        )
    }

    SubPageScaffold(
        title = "主题与纸张",
        palette = palette,
        onBack = onBack,
    ) {
        CompositionLocalProvider(LocalReadingStyle provides liveStyle) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // 1. Live Visual Preview Stage (iOS Preview-Over-Slider pattern)
                ThemeAndGlassLivePreviewCard(
                    style = liveStyle,
                    palette = previewPalette,
                    paperPalette = previewPaper,
                    fontFamily = previewFont,
                )

                // 2. Liquid Glass Controls directly underneath the preview
                FrostedSectionCard(palette = palette, style = liveStyle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "液态玻璃",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${(localOpacity * 100).roundToInt()}%",
                    color = palette.accentText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TabularNumStyle,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsDockSegmentedPill(
                options = clarityOptions,
                selected = style.glassClarityMode,
                onSelect = { mode ->
                    if (style.glassClarityMode != mode) {
                        onUpdateStyle(currentStyle.copy(glassClarityMode = mode))
                    }
                },
                palette = palette,
                style = liveStyle,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "玻璃浓度",
                    color = palette.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(68.dp),
                )
                Ios27PrecisionSlider(
                    value = localOpacity,
                    valueRange = 0f..1f,
                    palette = palette,
                    onValueChange = { raw ->
                        isScrubbingOpacity = true
                        val clamped = raw.coerceIn(0f, 1f)
                        if ((clamped * 20).roundToInt() != (localOpacity * 20).roundToInt()) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        localOpacity = clamped
                    },
                    onValueChangeFinished = {
                        isScrubbingOpacity = false
                        val committed = localOpacity.coerceIn(0f, 1f)
                        if (committed != currentStyle.glassOpacityLevel) {
                            onUpdateStyle(currentStyle.copy(glassOpacityLevel = committed))
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
            }
        }

        // 3. Appearance Mode (3-way Dock Pill + Scheduled Switch)
        FrostedSectionCard(palette = palette) {
            Text(
                text = "外观",
                color = palette.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsDockSegmentedPill(
                options = brightnessPillOptions,
                selected = if (style.brightnessMode == AppBrightnessMode.scheduled) null else style.brightnessMode,
                onSelect = { mode ->
                    if (style.brightnessMode != mode) {
                        onUpdateStyle(style.copy(brightnessMode = mode))
                    }
                },
                palette = palette,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SettingSwitchRow(
                title = "定时切换",
                checked = style.brightnessMode == AppBrightnessMode.scheduled,
                palette = palette,
                onCheckedChange = { enabled ->
                    val next = if (enabled) AppBrightnessMode.scheduled else AppBrightnessMode.system
                    onUpdateStyle(style.copy(brightnessMode = next))
                },
            )
            AnimatedVisibility(
                visible = style.brightnessMode == AppBrightnessMode.scheduled,
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(140)) + shrinkVertically(),
            ) {
                Column {
                    Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
                    HourStepperRow(
                        label = "日间开始",
                        hour = style.dayStartHour,
                        palette = palette,
                        onHourChange = { onUpdateStyle(style.copy(dayStartHour = it)) },
                    )
                    Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
                    HourStepperRow(
                        label = "夜间开始",
                        hour = style.nightStartHour,
                        palette = palette,
                        onHourChange = { onUpdateStyle(style.copy(nightStartHour = it)) },
                    )
                }
            }
        }

        // 4. Theme Accent Color & Ambient Glow Source
        FrostedSectionCard(palette = palette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "主题色",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = style.appTheme.label,
                    color = palette.accentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppThemeId.entries.forEach { themeId ->
                    val selected = style.appTheme == themeId && style.customThemeColorValue == null
                    val swatchColor = if (themeId == AppThemeId.wallpaper && selected) {
                        palette.primary
                    } else {
                        themeId.seedColor
                    }
                    CircularColorSwatchItem(
                        label = themeId.label,
                        swatchColor = swatchColor,
                        selected = selected,
                        palette = palette,
                        circleSize = 34.dp,
                        minWidth = 36.dp,
                        onClick = {
                            onUpdateStyle(
                                style.copy(
                                    appTheme = themeId,
                                    customThemeColorValue = null,
                                ),
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            AmbientGlowSourceSelector(
                style = style,
                palette = palette,
                onUpdateStyle = onUpdateStyle,
            )
        }

        // 5. Reader Paper Background
        FrostedSectionCard(palette = palette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "阅读纸张",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = style.readerBackground.label,
                    color = palette.accentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReaderBackgroundId.entries.forEach { bgId ->
                    val bgColor = if (bgId == ReaderBackgroundId.theme) {
                        resolveThemeReaderPalette(previewPalette).background
                    } else {
                        readerPalettes[bgId]?.background ?: Color(0xFF17100D)
                    }
                    CircularColorSwatchItem(
                        label = bgId.label,
                        swatchColor = bgColor,
                        selected = style.readerBackground == bgId,
                        palette = palette,
                        circleSize = 38.dp,
                        minWidth = 44.dp,
                        onClick = {
                            val disableAutoNight = !previewIsLight &&
                                style.autoNightPaper &&
                                (bgId == ReaderBackgroundId.paper || bgId == ReaderBackgroundId.green)
                            onUpdateStyle(
                                style.copy(
                                    readerBackground = bgId,
                                    autoNightPaper = if (disableAutoNight) false else style.autoNightPaper,
                                ),
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SettingSwitchRow(
                title = "夜间自动联动",
                checked = style.autoNightPaper,
                palette = palette,
                onCheckedChange = { onUpdateStyle(style.copy(autoNightPaper = it)) },
            )
        }
    }
}

@Composable
private fun ThemeAndGlassLivePreviewCard(
    style: ReadingStyle,
    palette: AppPalette,
    paperPalette: ReaderPalette,
    fontFamily: FontFamily,
) {
    val powerSave = LocalPowerSaveMode.current
    val previewBackdrop = if (powerSave) null else rememberGlassBackdrop()
    val ambientGlowColors = LocalAmbientGlowColorsState.current?.value ?: LocalAmbientGlowColors.current
    val stageShape = RoundedCornerShape(26.dp)
    val glowPair = remember(style.ambientGlowSource, ambientGlowColors, palette) {
        if (style.ambientGlowSource == AmbientGlowSource.bookCover && ambientGlowColors != null) {
            ambientGlowColors
        } else {
            palette.primary to palette.primarySoft
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .clip(stageShape)
            .border(
                width = 0.65.dp,
                brush = ios27GroupedEdgeBrush(palette, style),
                shape = stageShape,
            ),
    ) {
        // Layer 1: Backdrop scene (ambient cover/theme glow + mini open book sheet + color ribbon behind Dock)
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (previewBackdrop != null) {
                        Modifier.glassBackdrop(previewBackdrop)
                    } else {
                        Modifier
                    },
                )
                .background(palette.background),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (-24).dp, y = (-26).dp)
                    .size(164.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowPair.first.copy(alpha = if (palette.isLight) 0.34f else 0.44f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 24.dp, y = 26.dp)
                    .size(172.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowPair.second.copy(alpha = if (palette.isLight) 0.32f else 0.38f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // Crisp miniature reader sheet
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 36.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(paperPalette.background)
                    .border(
                        width = 0.6.dp,
                        color = paperPalette.text.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "第一章 · 晴窗",
                    color = paperPalette.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "风穿过长廊与檐角，光影在纸页间缓缓流动，静候翻开下一页。",
                    color = paperPalette.text,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight(style.fontWeightValue.coerceIn(100, 900)),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Layer 2: Floating miniature 3D Dock capsule refracting Layer 1 directly underneath
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .width(232.dp)
                .height(48.dp)
                .ios27ElevatedDockSurface(
                    shape = CircleShape,
                    palette = palette,
                    style = style,
                    backdrop = previewBackdrop,
                    shadowScale = 0.92f,
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .ios27LiftedInnerPillSurface(
                        shape = CircleShape,
                        palette = palette,
                        style = style,
                        lifted = true,
                        shadowScale = 0.85f,
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoStories,
                    contentDescription = null,
                    tint = palette.accentText,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "在读",
                    color = palette.accentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "书架",
                    color = palette.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "设置",
                    color = palette.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AmbientGlowSourceSelector(
    style: ReadingStyle,
    palette: AppPalette,
    onUpdateStyle: (ReadingStyle) -> Unit,
) {
    val options = remember {
        listOf(
            AmbientGlowSource.bookCover to "在读封面取色",
            AmbientGlowSource.appTheme to "应用主题色",
        )
    }
    Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "氛围光配色来源",
            color = palette.text,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = if (style.ambientGlowSource == AmbientGlowSource.bookCover) "封面" else "主题",
            color = palette.accentText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    SettingsDockSegmentedPill(
        options = options,
        selected = style.ambientGlowSource,
        onSelect = { next ->
            if (next != style.ambientGlowSource) {
                onUpdateStyle(style.copy(ambientGlowSource = next))
            }
        },
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FontAndWeightSubPage(
    style: ReadingStyle,
    fonts: List<ImportedFont>,
    palette: AppPalette,
    onUpdateStyle: (ReadingStyle) -> Unit,
    onImportFontClick: () -> Unit,
    onDeleteFont: (ImportedFont) -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val isSystemFont = style.fontName == null && style.fontPath == null
    val isBuiltInVf = style.fontName == BUILTIN_VF_FONT_NAME || style.fontPath == BUILTIN_VF_FONT_PATH

    var isScrubbingWeight by remember { mutableStateOf(false) }
    var localWeightValue by remember { mutableFloatStateOf(style.fontWeightValue.toFloat()) }
    LaunchedEffect(style.fontWeightValue) {
        if (!isScrubbingWeight) {
            localWeightValue = style.fontWeightValue.toFloat()
        }
    }
    val displayWeight = ((localWeightValue / 25f).roundToInt() * 25).coerceIn(100, 900)
    val previewFontFamily = rememberPreviewFontFamily(
        fontName = style.fontName,
        fontPath = style.fontPath,
        weight = displayWeight,
    )

    SubPageScaffold(
        title = "字体与字重",
        palette = palette,
        onBack = onBack,
    ) {
        // Live Font & Variable Weight Preview Card
        FrostedSectionCard(palette = palette) {
            Text(
                text = "月光穿过林间的薄雾，落在铺满落叶的小径上。旅行者合上笔记本，听见远处钟塔传来回响。",
                color = palette.text,
                fontFamily = previewFontFamily,
                fontSize = style.fontSize.coerceIn(15f, 22f).sp,
                lineHeight = (style.fontSize.coerceIn(15f, 22f) * style.lineHeight).sp,
                letterSpacing = style.letterSpacing.sp,
                fontWeight = if (isBuiltInVf) FontWeight.Normal else FontWeight(displayWeight),
            )
        }

        // Variable Font Weight Slider directly under Preview
        FrostedSectionCard(palette = palette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "字重",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "W$displayWeight · ${fontWeightLabel(displayWeight)}",
                    color = palette.accentText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TabularNumStyle,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Ios27PrecisionSlider(
                value = localWeightValue,
                valueRange = 100f..900f,
                palette = palette,
                onValueChange = { raw ->
                    isScrubbingWeight = true
                    val stepped = ((raw / 25f).roundToInt() * 25).coerceIn(100, 900)
                    if (stepped != displayWeight) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    localWeightValue = stepped.toFloat()
                },
                onValueChangeFinished = {
                    isScrubbingWeight = false
                    val committed = ((localWeightValue / 25f).roundToInt() * 25).coerceIn(100, 900)
                    if (committed != style.fontWeightValue) {
                        onUpdateStyle(style.copy(fontWeightValue = committed))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Reader Font Selection
        FrostedSectionCard(palette = palette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "正文字体",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Ios27GlassCapsuleButton(
                    text = "导入字体",
                    icon = Icons.Rounded.UploadFile,
                    palette = palette,
                    prominent = true,
                    compact = true,
                    onClick = onImportFontClick,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            InsetGroupedCheckRow(
                title = "系统默认",
                selected = isSystemFont,
                palette = palette,
                onClick = { onUpdateStyle(style.copy(fontName = null, fontPath = null)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            InsetGroupedCheckRow(
                title = "SQuartorReaderFont",
                subtitle = "可变字重",
                selected = isBuiltInVf,
                palette = palette,
                onClick = {
                    onUpdateStyle(
                        style.copy(
                            fontName = BUILTIN_VF_FONT_NAME,
                            fontPath = BUILTIN_VF_FONT_PATH,
                        ),
                    )
                },
            )
            fonts.forEach { imported ->
                val selected = style.fontPath == imported.path && !isBuiltInVf
                Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
                InsetGroupedCheckRow(
                    title = imported.name,
                    selected = selected,
                    palette = palette,
                    onClick = {
                        onUpdateStyle(
                            style.copy(
                                fontName = imported.name,
                                fontPath = imported.path,
                            ),
                        )
                    },
                    onDelete = { onDeleteFont(imported) },
                )
            }
        }

        // Interface Font Selection
        FrostedSectionCard(palette = palette) {
            Text(
                text = "界面字体",
                color = palette.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            InsetGroupedCheckRow(
                title = "系统字体",
                selected = style.appFontPath == null && style.appFontName == null,
                palette = palette,
                onClick = { onUpdateStyle(style.copy(appFontName = null, appFontPath = null)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            InsetGroupedCheckRow(
                title = "SQuartorReaderFont",
                selected = style.appFontName == BUILTIN_VF_FONT_NAME || style.appFontPath == BUILTIN_VF_FONT_PATH,
                palette = palette,
                onClick = {
                    onUpdateStyle(
                        style.copy(
                            appFontName = BUILTIN_VF_FONT_NAME,
                            appFontPath = BUILTIN_VF_FONT_PATH,
                        ),
                    )
                },
            )
            fonts.forEach { imported ->
                Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
                InsetGroupedCheckRow(
                    title = imported.name,
                    selected = style.appFontPath == imported.path,
                    palette = palette,
                    onClick = {
                        onUpdateStyle(
                            style.copy(
                                appFontName = imported.name,
                                appFontPath = imported.path,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TypographySubPage(
    style: ReadingStyle,
    palette: AppPalette,
    onUpdateStyle: (ReadingStyle) -> Unit,
    onBack: () -> Unit,
) {
    val currentStyle by rememberUpdatedState(style)
    var previewFontSize by remember { mutableFloatStateOf(style.fontSize) }
    var previewWeight by remember { mutableFloatStateOf(style.fontWeightValue.toFloat()) }
    var previewLineHeight by remember { mutableFloatStateOf(style.lineHeight) }
    var previewParagraphSpacing by remember { mutableFloatStateOf(style.paragraphSpacing) }
    var previewLetterSpacing by remember { mutableFloatStateOf(style.letterSpacing) }
    var previewPageMargin by remember { mutableFloatStateOf(style.pageMargin) }

    LaunchedEffect(
        style.fontSize,
        style.fontWeightValue,
        style.lineHeight,
        style.paragraphSpacing,
        style.letterSpacing,
        style.pageMargin,
    ) {
        previewFontSize = style.fontSize
        previewWeight = style.fontWeightValue.toFloat()
        previewLineHeight = style.lineHeight
        previewParagraphSpacing = style.paragraphSpacing
        previewLetterSpacing = style.letterSpacing
        previewPageMargin = style.pageMargin
    }

    val paperPalette = remember(style.readerBackground, style.autoNightPaper, palette) {
        resolvePreviewPaperPalette(style = style, palette = palette)
    }
    val roundedWeight = ((previewWeight / 25f).roundToInt() * 25).coerceIn(100, 900)
    val previewFont = rememberPreviewFontFamily(
        fontName = style.fontName,
        fontPath = style.fontPath,
        weight = roundedWeight,
    )
    val isBuiltInVf = style.fontName == BUILTIN_VF_FONT_NAME || style.fontPath == BUILTIN_VF_FONT_PATH
    val indentPrefix = if (style.firstLineIndent) "\u3000\u3000" else ""

    SubPageScaffold(
        title = "排版与翻页",
        palette = palette,
        onBack = onBack,
    ) {
        // Live Typography Preview Card on top (3D Dock elevated showcase frame + inset reader sheet)
        TypographyLivePreviewCard(
            palette = palette,
            paperPalette = paperPalette,
            fontFamily = previewFont,
            fontSize = previewFontSize,
            weightValue = roundedWeight,
            isBuiltInVf = isBuiltInVf,
            lineHeight = previewLineHeight,
            paragraphSpacing = previewParagraphSpacing,
            letterSpacing = previewLetterSpacing,
            pageMargin = previewPageMargin,
            indentPrefix = indentPrefix,
        )

        // Sliders right below Preview
        FrostedSectionCard(palette = palette) {
            DeferredTypographySlider(
                label = "字号",
                committedValue = style.fontSize,
                valueRange = 14f..32f,
                snap = { it.roundToInt().toFloat().coerceIn(14f, 32f) },
                formatValue = { "${it.roundToInt()}" },
                palette = palette,
                onScrub = { previewFontSize = it },
                onCommit = { onUpdateStyle(currentStyle.copy(fontSize = it)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)

            DeferredTypographySlider(
                label = "字重",
                committedValue = style.fontWeightValue.toFloat(),
                valueRange = 100f..900f,
                snap = { ((it / 100f).roundToInt() * 100).coerceIn(100, 900).toFloat() },
                formatValue = { "${it.roundToInt()}" },
                palette = palette,
                onScrub = { previewWeight = it },
                onCommit = { onUpdateStyle(currentStyle.copy(fontWeightValue = it.roundToInt())) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)

            DeferredTypographySlider(
                label = "行高",
                committedValue = style.lineHeight,
                valueRange = 1.2f..2.6f,
                snap = { ((it * 20f).roundToInt() / 20f).coerceIn(1.2f, 2.6f) },
                formatValue = { String.format(Locale.US, "%.2f", it) },
                palette = palette,
                onScrub = { previewLineHeight = it },
                onCommit = { onUpdateStyle(currentStyle.copy(lineHeight = it)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)

            DeferredTypographySlider(
                label = "段距",
                committedValue = style.paragraphSpacing,
                valueRange = 0f..30f,
                snap = { it.roundToInt().toFloat().coerceIn(0f, 30f) },
                formatValue = { "${it.roundToInt()}" },
                palette = palette,
                onScrub = { previewParagraphSpacing = it },
                onCommit = { onUpdateStyle(currentStyle.copy(paragraphSpacing = it)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)

            DeferredTypographySlider(
                label = "字距",
                committedValue = style.letterSpacing,
                valueRange = 0f..2f,
                snap = { ((it * 10f).roundToInt() / 10f).coerceIn(0f, 2f) },
                formatValue = { String.format(Locale.US, "%.1f", it) },
                palette = palette,
                onScrub = { previewLetterSpacing = it },
                onCommit = { onUpdateStyle(currentStyle.copy(letterSpacing = it)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)

            DeferredTypographySlider(
                label = "左右边距",
                committedValue = style.pageMargin,
                valueRange = 0f..52f,
                snap = { it.roundToInt().toFloat().coerceIn(0f, 52f) },
                formatValue = { "${it.roundToInt()}" },
                palette = palette,
                onScrub = { previewPageMargin = it },
                onCommit = { onUpdateStyle(currentStyle.copy(pageMargin = it)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)

            DeferredTypographySlider(
                label = "上下边距",
                committedValue = style.verticalMargin,
                valueRange = 4f..96f,
                snap = { it.roundToInt().toFloat().coerceIn(4f, 96f) },
                formatValue = { "${it.roundToInt()}" },
                palette = palette,
                onCommit = { onUpdateStyle(currentStyle.copy(verticalMargin = it)) },
            )
        }

        // Reading Interaction Card
        FrostedSectionCard(palette = palette) {
            Text(
                text = "翻页方式",
                color = palette.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsDockSegmentedPill(
                options = remember { ReadingFlowMode.entries.map { it to it.label } },
                selected = style.readingFlow,
                onSelect = { mode ->
                    if (style.readingFlow != mode) {
                        onUpdateStyle(style.copy(readingFlow = mode))
                    }
                },
                palette = palette,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SettingSwitchRow(
                title = "首行缩进两字符",
                checked = style.firstLineIndent,
                palette = palette,
                onCheckedChange = { onUpdateStyle(style.copy(firstLineIndent = it)) },
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            SettingSwitchRow(
                title = "反转点击翻页",
                checked = style.reverseTapPageTurn,
                palette = palette,
                onCheckedChange = { onUpdateStyle(style.copy(reverseTapPageTurn = it)) },
            )
        }
    }
}

@Composable
private fun TypographyLivePreviewCard(
    palette: AppPalette,
    paperPalette: ReaderPalette,
    fontFamily: FontFamily,
    fontSize: Float,
    weightValue: Int,
    isBuiltInVf: Boolean,
    lineHeight: Float,
    paragraphSpacing: Float,
    letterSpacing: Float,
    pageMargin: Float,
    indentPrefix: String,
) {
    val stageShape = RoundedCornerShape(26.dp)
    val sheetShape = RoundedCornerShape(18.dp)
    val backdrop = LocalVitroBackdrop.current
    val paperIsLight = paperPalette.background.luminance() > 0.5f
    val paperBorderColor = paperPalette.text.copy(alpha = if (paperIsLight) 0.09f else 0.14f)
    val paperShadowColor = Color.Black.copy(alpha = if (palette.isLight) 0.06f else 0.24f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .ios27ElevatedDockSurface(
                shape = stageShape,
                palette = palette,
                backdrop = backdrop,
                shadowScale = 1f,
            )
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val radiusPx = 18.dp.toPx()
                    drawRoundRect(
                        color = paperShadowColor,
                        topLeft = Offset(0f, 2.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                    )
                }
                .clip(sheetShape)
                .background(paperPalette.background)
                .border(width = 0.65.dp, color = paperBorderColor, shape = sheetShape)
                .padding(
                    horizontal = pageMargin.coerceIn(10f, 34f).dp,
                    vertical = 14.dp,
                ),
        ) {
            val scaledSize = fontSize.coerceIn(13f, 23f)
            val bodyStyle = TextStyle(
                color = paperPalette.text,
                fontFamily = fontFamily,
                fontWeight = if (isBuiltInVf) FontWeight.Normal else FontWeight(weightValue),
                fontSize = scaledSize.sp,
                lineHeight = (scaledSize * lineHeight).sp,
                letterSpacing = letterSpacing.sp,
            )
            Text(
                text = "${indentPrefix}窗外的雨声逐渐微弱，灯影落在展开的书脊上。",
                style = bodyStyle,
            )
            Spacer(modifier = Modifier.height(paragraphSpacing.coerceIn(2f, 24f).dp))
            Text(
                text = "${indentPrefix}他翻过薄薄的纸页，墨迹在安静的夜色中显得格外清晰。",
                style = bodyStyle,
            )
        }
    }
}

@Composable
private fun CloudSyncSubPage(
    cloudSync: CloudSyncSettings,
    palette: AppPalette,
    isSyncing: Boolean,
    syncFeedbackMessage: String?,
    onDismissSyncFeedback: () -> Unit,
    onUpdateCloudSync: (CloudSyncSettings) -> Unit,
    onSyncUpload: () -> Unit,
    onSyncDownload: () -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var endpoint by remember(cloudSync.endpoint) { mutableStateOf(cloudSync.endpoint) }
    var username by remember(cloudSync.username) { mutableStateOf(cloudSync.username) }
    var password by remember(cloudSync.password) { mutableStateOf(cloudSync.password) }

    fun buildUpdatedSettings(): CloudSyncSettings {
        val ep = endpoint.trim()
        val un = username.trim()
        return cloudSync.copy(
            enabled = ep.isNotEmpty() && un.isNotEmpty() && password.isNotEmpty(),
            endpoint = ep,
            username = un,
            password = password,
        )
    }

    SubPageScaffold(
        title = "云同步与备份",
        palette = palette,
        onBack = onBack,
    ) {
        FrostedSectionCard(palette = palette) {
            Text(
                text = "WebDAV 配置",
                color = palette.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            InsetGlassTextFieldRow(
                label = "服务器",
                value = endpoint,
                placeholder = "https://dav.jianguoyun.com/dav/",
                onValueChange = { endpoint = it },
                enabled = !isSyncing,
                palette = palette,
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            InsetGlassTextFieldRow(
                label = "账号",
                value = username,
                placeholder = "用户名",
                onValueChange = { username = it },
                enabled = !isSyncing,
                palette = palette,
            )
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            InsetGlassTextFieldRow(
                label = "授权码",
                value = password,
                placeholder = "应用密码",
                onValueChange = { password = it },
                enabled = !isSyncing,
                visualTransformation = PasswordVisualTransformation(),
                palette = palette,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Ios27GlassCapsuleButton(
                text = "保存配置",
                icon = Icons.Rounded.Save,
                palette = palette,
                enabled = !isSyncing,
                prominent = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUpdateCloudSync(buildUpdatedSettings())
                },
            )
        }

        FrostedSectionCard(palette = palette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "备份与恢复",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                AnimatedVisibility(
                    visible = isSyncing,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(160)),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ios27SubtleChipColor(palette))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = palette.accentText,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "同步中",
                            color = palette.accentText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Ios27GlassCapsuleButton(
                    text = if (isSyncing) "同步中" else "上传本机",
                    icon = Icons.Rounded.CloudUpload,
                    palette = palette,
                    enabled = !isSyncing,
                    isLoading = isSyncing,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateCloudSync(buildUpdatedSettings())
                        onSyncUpload()
                    },
                )
                Ios27GlassCapsuleButton(
                    text = if (isSyncing) "同步中" else "下载云端",
                    icon = Icons.Rounded.CloudDownload,
                    palette = palette,
                    enabled = !isSyncing,
                    isLoading = isSyncing,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateCloudSync(buildUpdatedSettings())
                        onSyncDownload()
                    },
                )
            }

            AnimatedVisibility(
                visible = !isSyncing && !syncFeedbackMessage.isNullOrBlank(),
                enter = fadeIn(tween(200)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f,
                        visibilityThreshold = IntSize(1, 1),
                    ),
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    SyncFeedbackStatusPill(
                        message = syncFeedbackMessage.orEmpty(),
                        palette = palette,
                        onDismiss = onDismissSyncFeedback,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            Spacer(modifier = Modifier.height(10.dp))
            StorageMetricRow(
                label = "上次上传",
                value = formatSyncTimestamp(cloudSync.lastUploadAtMillis),
                palette = palette,
            )
            Spacer(modifier = Modifier.height(8.dp))
            StorageMetricRow(
                label = "上次下载",
                value = formatSyncTimestamp(cloudSync.lastDownloadAtMillis),
                palette = palette,
            )
        }
    }
}

@Composable
private fun SyncFeedbackStatusPill(
    message: String,
    palette: AppPalette,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ios27LiftedInnerPillSurface(
                shape = RoundedCornerShape(14.dp),
                palette = palette,
                lifted = true,
                shadowScale = 0.7f,
            )
            .clickable(onClick = onDismiss)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = palette.accentText,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = message,
            color = palette.text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "关闭提示",
            tint = palette.muted,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun StorageAndCacheSubPage(
    storageStats: StorageStats,
    palette: AppPalette,
    syncFeedbackMessage: String? = null,
    onDismissSyncFeedback: () -> Unit = {},
    onClearCache: () -> Unit,
    onBack: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val total = storageStats.totalBytes.coerceAtLeast(1L)
    val booksRatio = (storageStats.booksBytes.toFloat() / total.toFloat()).coerceIn(0.05f, 0.95f)

    SubPageScaffold(
        title = "存储与缓存",
        palette = palette,
        onBack = onBack,
    ) {
        FrostedSectionCard(palette = palette) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = palette.accentText,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = "空间占用",
                    color = palette.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(palette.line.copy(alpha = 0.3f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(booksRatio)
                        .fillMaxHeight()
                        .background(palette.primary),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.accentText.copy(alpha = 0.45f)),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            StorageMetricRow(
                label = "书籍与封面",
                value = StorageStats.formatBytes(storageStats.booksBytes),
                palette = palette,
            )
            Spacer(modifier = Modifier.height(10.dp))
            StorageMetricRow(
                label = "缓存数据",
                value = StorageStats.formatBytes(storageStats.cacheBytes),
                palette = palette,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            Spacer(modifier = Modifier.height(12.dp))
            StorageMetricRow(
                label = "总占用",
                value = StorageStats.formatBytes(storageStats.totalBytes),
                palette = palette,
                highlight = true,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Ios27GlassCapsuleButton(
                text = "清理缓存",
                icon = Icons.Rounded.CleaningServices,
                palette = palette,
                prominent = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClearCache()
                },
            )
            AnimatedVisibility(
                visible = !syncFeedbackMessage.isNullOrBlank(),
                enter = fadeIn(tween(200)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f,
                        visibilityThreshold = IntSize(1, 1),
                    ),
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    SyncFeedbackStatusPill(
                        message = syncFeedbackMessage.orEmpty(),
                        palette = palette,
                        onDismiss = onDismissSyncFeedback,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSubPage(
    palette: AppPalette,
    onBack: () -> Unit,
) {
    SubPageScaffold(
        title = "关于 SQuartor",
        palette = palette,
        onBack = onBack,
    ) {
        FrostedSectionCard(palette = palette) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .ios27LiftedInnerPillSurface(
                            shape = RoundedCornerShape(14.dp),
                            palette = palette,
                            lifted = true,
                            shadowScale = 0.85f,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = palette.accentText,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "SQuartor Reader",
                        color = palette.text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "v2.0.0",
                        color = palette.muted,
                        fontSize = 13.sp,
                        style = TabularNumStyle,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Ios27GroupedDivider(palette, startIndent = 0.dp, endIndent = 0.dp)
            Spacer(modifier = Modifier.height(12.dp))
            StorageMetricRow(
                label = "版本",
                value = "2.0.0-native",
                palette = palette,
            )
            Spacer(modifier = Modifier.height(10.dp))
            StorageMetricRow(
                label = "字体",
                value = "NotoSansSC-VF",
                palette = palette,
            )
            Spacer(modifier = Modifier.height(10.dp))
            StorageMetricRow(
                label = "渲染",
                value = "Jetpack Compose · Vitro",
                palette = palette,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubPageScaffold(
    title: String,
    palette: AppPalette,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val powerSave = LocalPowerSaveMode.current
    val vitroBackdrop = rememberGlassBackdrop()
    val effectiveBackdrop = if (powerSave) null else vitroBackdrop
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val backInteraction = remember { MutableInteractionSource() }
    val headerScrollThresholdPx = with(density) { 42.dp.toPx().roundToInt() }
    val scrimRampPx = with(density) { 28.dp.toPx() }.coerceAtLeast(1f)
    val isScrolled by remember {
        derivedStateOf {
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            when {
                firstItem == null -> false
                firstItem.index > 0 -> true
                else -> listState.firstVisibleItemScrollOffset >=
                    (firstItem.size * 0.85f).roundToInt().coerceAtLeast(headerScrollThresholdPx)
            }
        }
    }
    val topScrimAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / scrimRampPx).coerceIn(0f, 1f)
        }
    }
    val largeTitleAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0f else 1f,
        animationSpec = tween(150),
        label = "subPageLargeTitleAlpha",
    )

    CompositionLocalProvider(LocalVitroBackdrop provides effectiveBackdrop) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Non-scrolling background source sampled cleanly by all sibling 3D Dock cards and top chrome.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (effectiveBackdrop != null) {
                            Modifier.glassBackdrop(effectiveBackdrop)
                        } else {
                            Modifier
                        },
                    )
                    .background(palette.background),
            ) {
                AmbientSubpageBackground(palette = palette)
            }

            // Layer 2: Scrollable content cards
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = topInset + 68.dp,
                    end = 20.dp,
                    bottom = bottomInset.coerceAtLeast(24.dp) + 56.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Text(
                        text = title,
                        color = palette.text,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                            .graphicsLayer { alpha = largeTitleAlpha },
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        content()
                    }
                }
            }

            // Layer 2.5: Soft vertical top fade scrim dissolving scrolled cards beneath status bar and top chrome
            SettingsTopScrollFadeScrim(
                topInset = topInset,
                alpha = topScrimAlpha,
                palette = palette,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            // Layer 3: Floating 3D Dock Navigation Chrome
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = topInset + 10.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .ios27ElevatedDockSurface(
                            shape = CircleShape,
                            palette = palette,
                            backdrop = effectiveBackdrop,
                            shadowScale = 0.92f,
                            isInteractive = true,
                            interactionSource = backInteraction,
                        )
                        .clickable(
                            interactionSource = backInteraction,
                            indication = null,
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = palette.text,
                        modifier = Modifier.size(20.dp),
                    )
                }

                AnimatedVisibility(
                    visible = isScrolled,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(140)),
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .ios27ElevatedDockSurface(
                                shape = CircleShape,
                                palette = palette,
                                backdrop = effectiveBackdrop,
                                shadowScale = 0.85f,
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTopScrollFadeScrim(
    topInset: Dp,
    alpha: Float,
    palette: AppPalette,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.01f) return
    val density = LocalDensity.current
    val totalHeight = topInset + 72.dp
    val scrimBrush = remember(palette.background, topInset, density) {
        val totalPx = with(density) { totalHeight.toPx() }.coerceAtLeast(1f)
        val solidStop = (with(density) { (topInset + 40.dp).toPx() } / totalPx).coerceIn(0.1f, 0.9f)
        val midStop = (with(density) { (topInset + 56.dp).toPx() } / totalPx).coerceIn(solidStop + 0.01f, 0.98f)
        Brush.verticalGradient(
            0.0f to palette.background.copy(alpha = 0.98f),
            solidStop to palette.background.copy(alpha = 0.92f),
            midStop to palette.background.copy(alpha = 0.50f),
            1.0f to Color.Transparent,
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .graphicsLayer { this.alpha = alpha }
            .background(scrimBrush),
    )
}

@Composable
private fun SettingsGroupCard(
    title: String,
    palette: AppPalette,
    content: @Composable () -> Unit,
) {
    val cardShape = RoundedCornerShape(26.dp)
    val backdrop = LocalVitroBackdrop.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = palette.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .ios27ElevatedDockSurface(
                    shape = cardShape,
                    palette = palette,
                    backdrop = backdrop,
                    shadowScale = 1f,
                )
                .padding(vertical = 2.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun FrostedSectionCard(
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
    content: @Composable () -> Unit,
) {
    val cardShape = RoundedCornerShape(26.dp)
    val backdrop = LocalVitroBackdrop.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ios27ElevatedDockSurface(
                shape = cardShape,
                palette = palette,
                style = style,
                backdrop = backdrop,
                shadowScale = 1f,
            )
            .padding(18.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsEntryRow(
    icon: ImageVector,
    title: String,
    value: String,
    palette: AppPalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accentText,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = palette.text,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = palette.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            style = TabularNumStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.subtle,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun <T> SettingsDockSegmentedPill(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    palette: AppPalette,
    modifier: Modifier = Modifier,
    style: ReadingStyle = LocalReadingStyle.current,
) {
    if (options.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val isLight = palette.isLight
    val selectedIndex = if (selected == null) -1 else options.indexOfFirst { it.first == selected }
    val hasSelection = selectedIndex >= 0
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "dockSegmentThumb",
    )
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val trackBg = if (isLight) {
        Color(0xFF14161D).copy(alpha = 0.055f + opacity * 0.020f)
    } else {
        Color.White.copy(alpha = 0.08f + opacity * 0.03f)
    }
    val trackBorder = palette.text.copy(alpha = if (isLight) 0.045f else 0.07f)

    BoxWithConstraints(
        modifier = modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(trackBg, CircleShape)
            .border(0.5.dp, trackBorder, CircleShape)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / options.size

        if (hasSelection) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = size.width * animatedIndex
                    }
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .ios27LiftedInnerPillSurface(
                        shape = CircleShape,
                        palette = palette,
                        style = style,
                        lifted = true,
                        shadowScale = 0.85f,
                    ),
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (item, label) ->
                val isSelected = selected != null && item == selected
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(item)
                        }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = when {
                            isSelected -> if (isLight) Color(0xFF11131B) else Color.White
                            else -> if (isLight) Color(0xFF4A4E5A) else Color.White.copy(alpha = 0.66f)
                        },
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun InsetGroupedCheckRow(
    title: String,
    selected: Boolean,
    palette: AppPalette,
    onClick: () -> Unit,
    subtitle: String? = null,
    onDelete: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable {
                if (!selected) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = if (selected) palette.accentText else palette.text,
                fontSize = 14.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = palette.muted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "已选中",
                tint = palette.accentText,
                modifier = Modifier.size(19.dp),
            )
        }
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "删除字体",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CircularColorSwatchItem(
    label: String,
    swatchColor: Color,
    selected: Boolean,
    palette: AppPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circleSize: Dp = 40.dp,
    minWidth: Dp = 48.dp,
) {
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "circularSwatchScale",
    )
    val checkTint = if (swatchColor.luminance() > 0.62f) Color(0xFF1F2937) else Color.White
    val ringPadding = if (circleSize <= 36.dp) 2.5.dp else 3.5.dp
    val checkSize = if (circleSize <= 36.dp) 13.dp else 14.dp
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!selected) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .padding(vertical = 4.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 2.dp,
                            color = palette.primary,
                            shape = CircleShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(ringPadding)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    width = 0.6.dp,
                    color = palette.line.copy(alpha = if (palette.isLight) 0.45f else 0.55f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = label,
                    tint = checkTint,
                    modifier = Modifier.size(checkSize),
                )
            }
        }
        Text(
            text = label,
            color = if (selected) palette.accentText else palette.muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun InsetGlassTextFieldRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    palette: AppPalette,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = palette.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(64.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            visualTransformation = visualTransformation,
            textStyle = TextStyle(
                color = if (enabled) palette.text else palette.muted,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(palette.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = palette.muted.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun Ios27GlassCapsuleButton(
    text: String,
    palette: AppPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    prominent: Boolean = false,
    compact: Boolean = false,
    isLoading: Boolean = false,
) {
    val capsuleShape = CircleShape
    val style = LocalReadingStyle.current
    val tintOverride = when {
        prominent -> palette.primary.copy(alpha = if (palette.isLight) 0.16f else 0.30f)
        else -> Color.Unspecified
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = if (compact) 44.dp else 48.dp)
            .ios27LiftedInnerPillSurface(
                shape = capsuleShape,
                palette = palette,
                style = style,
                lifted = true,
                shadowScale = if (enabled) 0.85f else 0.35f,
                tintOverride = if (enabled || !prominent) tintOverride else tintOverride.copy(alpha = 0.08f),
            )
            .then(
                if (prominent && enabled) {
                    Modifier.border(
                        width = 0.65.dp,
                        color = palette.primary.copy(alpha = if (palette.isLight) 0.32f else 0.48f),
                        shape = capsuleShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(
                horizontal = if (compact) 14.dp else 16.dp,
                vertical = if (compact) 6.dp else 10.dp,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = palette.accentText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) palette.accentText else palette.muted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = when {
                !enabled -> palette.muted
                prominent -> palette.accentText
                else -> palette.text
            },
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun HourStepperRow(
    label: String,
    hour: Int,
    palette: AppPalette,
    onHourChange: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = palette.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onHourChange((hour - 1 + 24) % 24)
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "减少一小时",
                tint = palette.text,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = String.format(Locale.US, "%02d:00", hour),
            color = palette.accentText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            style = TabularNumStyle,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onHourChange((hour + 1) % 24)
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "增加一小时",
                tint = palette.text,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DeferredTypographySlider(
    label: String,
    committedValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    snap: (Float) -> Float,
    formatValue: (Float) -> String,
    palette: AppPalette,
    onScrub: ((Float) -> Unit)? = null,
    onCommit: (Float) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val currentCommitted by rememberUpdatedState(committedValue)
    var isScrubbing by remember { mutableStateOf(false) }
    var localValue by remember { mutableFloatStateOf(committedValue) }
    LaunchedEffect(committedValue) {
        if (!isScrubbing) {
            localValue = committedValue
        }
    }
    val snappedDisplay = snap(localValue)
    TypographySliderRow(
        label = label,
        valueText = formatValue(snappedDisplay),
        value = localValue,
        valueRange = valueRange,
        palette = palette,
        onValueChange = { raw ->
            isScrubbing = true
            val next = snap(raw)
            if (next != snap(localValue)) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            localValue = next
            onScrub?.invoke(next)
        },
        onValueChangeFinished = {
            isScrubbing = false
            val finalVal = snap(localValue)
            if (finalVal != currentCommitted) {
                onCommit(finalVal)
            }
        },
    )
}

@Composable
private fun TypographySliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    palette: AppPalette,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = palette.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.width(68.dp),
        )
        Ios27PrecisionSlider(
            value = value,
            valueRange = valueRange,
            palette = palette,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = valueText,
            color = palette.text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            style = TabularNumStyle,
            maxLines = 1,
            modifier = Modifier.width(46.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    palette: AppPalette,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = palette.text,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Ios27GlassToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            palette = palette,
        )
    }
}

@Composable
private fun StorageMetricRow(
    label: String,
    value: String,
    palette: AppPalette,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (highlight) palette.text else palette.muted,
            fontSize = if (highlight) 15.sp else 13.5.sp,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            text = value,
            color = if (highlight) palette.accentText else palette.text,
            fontSize = if (highlight) 16.sp else 14.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            style = TabularNumStyle,
        )
    }
}
