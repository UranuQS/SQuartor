package com.squartor.reader.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.squartor.reader.model.AmbientGlowSource
import com.squartor.reader.model.AppBrightnessMode
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.cleanBookTitle
import com.squartor.reader.ui.glass.BookCoverView
import com.squartor.reader.ui.glass.CoverAssetCache
import com.squartor.reader.ui.glass.GeminiCornerGlowBrushCache
import com.squartor.reader.ui.glass.Ios27GlassVariant
import com.squartor.reader.ui.glass.IsolatedAmbientGlassContainer
import com.squartor.reader.ui.glass.LiquidGlassDraggableCircleButton
import com.squartor.reader.ui.glass.LocalAmbientGlowColors
import com.squartor.reader.ui.glass.LocalAmbientGlowColorsState
import com.squartor.reader.ui.glass.LocalBookLaunchTracker
import com.squartor.reader.ui.glass.LocalHeroBookSlotAlpha
import com.squartor.reader.ui.glass.LocalPagerTransitioning
import com.squartor.reader.ui.glass.LocalPagerTransitioningState
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalRootHandlesBottomAmbient
import com.squartor.reader.ui.glass.unclippedBoundsInRoot
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.drawGeminiCornerAmbientGlow
import com.squartor.reader.ui.glass.rememberAmbientFlowSeconds
import com.squartor.reader.ui.glass.vitroInteractiveCard
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffectContainer
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingNowScreen(
    books: List<BookEntry>,
    todaySeconds: Int,
    weekSeconds: Int,
    monthSeconds: Int,
    weekDailySeconds: List<Int>,
    palette: AppPalette,
    onOpenBook: (BookEntry) -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenShelf: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenStats: () -> Unit = {},
    onToggleBrightness: (Offset) -> Unit,
    themeRevealButtonProgress: Float = 1f,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    isVisible: Boolean = true,
    sharedBackdrop: GlassBackdrop? = null,
    onScrolledOverDockChanged: (Boolean) -> Unit = {},
    onRecentRowDragLockChanged: (Boolean) -> Unit = {},
) {
    val homeDataCache = remember {
        object {
            var lastBooks: List<BookEntry>? = null
            var recentBooks: List<BookEntry> = emptyList()
            var readingBooks: List<BookEntry> = emptyList()
            var heroBook: BookEntry? = null
            var galleryBooks: List<BookEntry> = emptyList()
            var finishedCount: Int = 0
            var waterfallBookIds: List<String> = emptyList()
        }
    }
    if ((isVisible || homeDataCache.lastBooks == null) && homeDataCache.lastBooks !== books) {
        val sortedRecent = books.sortedWith { a, b ->
            val aTime = a.lastReadAtMillis ?: a.importedAtMillis
            val bTime = b.lastReadAtMillis ?: b.importedAtMillis
            val cmp = bTime.compareTo(aTime)
            if (cmp != 0) cmp else b.progress.compareTo(a.progress)
        }
        val nextReading = sortedRecent.filter { it.progress > 0.0 }
        val nextHero = sortedRecent.firstOrNull { it.lastReadAtMillis != null }
            ?: sortedRecent.firstOrNull { it.progress > 0.0 }
            ?: sortedRecent.firstOrNull()
        val nextGallery = if (nextHero == null) {
            emptyList()
        } else {
            val others = sortedRecent.filter { it.id != nextHero.id }.take(6)
            others.ifEmpty { listOf(nextHero) }
        }
        val nextFinished = books.count { it.progress >= 1.0 }
        val nextWaterfallIds = sortedRecent.filter { it.coverPath != null }.take(24).map { it.id }
        homeDataCache.lastBooks = books
        homeDataCache.recentBooks = sortedRecent
        homeDataCache.readingBooks = nextReading
        homeDataCache.heroBook = nextHero
        homeDataCache.galleryBooks = nextGallery
        homeDataCache.finishedCount = nextFinished
        homeDataCache.waterfallBookIds = nextWaterfallIds
    }
    val recentBooks = homeDataCache.recentBooks
    val readingBooks = homeDataCache.readingBooks
    val heroBook = homeDataCache.heroBook
    val galleryBooks = homeDataCache.galleryBooks
    val finishedCount = homeDataCache.finishedCount
    val todayWeekday = remember { LocalDate.now().dayOfWeek.value }
    val waterfallBookIds = homeDataCache.waterfallBookIds
    LaunchedEffect(isVisible, heroBook?.id, heroBook?.coverPath) {
        if (!isVisible) return@LaunchedEffect
        heroBook?.let { CoverAssetCache.ensureLoaded(it) }
    }
    LaunchedEffect(isVisible, recentBooks, heroBook?.id) {
        if (!isVisible) return@LaunchedEffect
        val toPreload = buildList {
            if (heroBook != null) add(heroBook)
            recentBooks.forEach { book ->
                if (book.id != heroBook?.id && size < 24) add(book)
            }
        }
        CoverAssetCache.preloadBooks(toPreload)
    }

    val powerSave = LocalPowerSaveMode.current
    val flowSecondsState = rememberAmbientFlowSeconds(isActive = isActive)

    val statusTopInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val cutoutTopInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusTopInset, cutoutTopInset)
    val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    // 令 62dp Dock 胶囊严格处于「最近阅读」卡片底边与屏幕物理底边之间的正中间
    val dockBottom = (navBarBottom.value * 0.65f + 16f).coerceIn(24f, 32f).dp
    val recentBottomToScreenBottom = dockBottom * 2 + 62.dp

    val homeBackdrop = sharedBackdrop ?: rememberGlassBackdrop()
    val scrollState = rememberScrollState()
    val hasScrolledOverDock by remember(scrollState) {
        derivedStateOf { scrollState.value > 0 }
    }
    LaunchedEffect(hasScrolledOverDock) {
        onScrolledOverDockChanged(hasScrolledOverDock)
    }
    val launchTracker = LocalBookLaunchTracker.current
    val density = LocalDensity.current

    fun launchBook(book: BookEntry, coverBounds: Rect?) {
        if (launchTracker != null) {
            launchTracker(book, coverBounds)
        } else {
            onOpenBook(book)
        }
    }

    val rootWindowHeightPx = LocalWindowInfo.current.containerSize.height.coerceAtLeast(1)
    var rootBoxHeightPx by remember { mutableIntStateOf(rootWindowHeightPx) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { sz ->
                if (sz.height > 0 && rootBoxHeightPx != sz.height) {
                    rootBoxHeightPx = sz.height
                }
            },
    ) {
        val maxHeight = with(density) { rootBoxHeightPx.toDp() }
        val galleryVerticalAllowance = 8.dp
        val columnBottomPad = (recentBottomToScreenBottom - galleryVerticalAllowance).coerceAtLeast(80.dp)
        val minRequiredContentHeight = topInset + 468.dp + columnBottomPad
        val isCompactScroll = maxHeight < minRequiredContentHeight
        var frozenCoverTick by remember { mutableIntStateOf(CoverAssetCache.versionTick) }
        val tick = if (isVisible) {
            CoverAssetCache.versionTick.also { if (frozenCoverTick != it) frozenCoverTick = it }
        } else {
            frozenCoverTick
        }
        val extractedHeroColor = remember(heroBook?.id, heroBook?.coverPath, tick) {
            CoverAssetCache.getExtractedCoverColor(heroBook)
        }
        val rawHeroAccent = remember(heroBook?.id, heroBook?.coverPath, tick, palette.primary) {
            CoverAssetCache.getAmbientColor(heroBook, palette.primary)
        }
        val hasBookSpecificAccent = extractedHeroColor != null
        val targetHeroAccent = remember(rawHeroAccent, hasBookSpecificAccent, palette) {
            if (hasBookSpecificAccent) {
                deriveHeroAccentColor(rawHeroAccent, palette.isLight)
            } else {
                if (palette.isLight) lerpColor(palette.accentText, Color.Black, 0.20f)
                else lerpColor(palette.accentText, Color.White, 0.14f)
            }
        }
        val (targetHeroCtaFill, targetHeroCtaContent) = remember(rawHeroAccent, hasBookSpecificAccent, palette) {
            if (hasBookSpecificAccent) {
                deriveHeroCtaColors(rawHeroAccent, palette.isLight)
            } else {
                var fill = lerpColor(
                    palette.primary,
                    palette.primarySoft,
                    if (palette.isLight) 0.14f else 0.24f,
                )
                val darkInk = Color(0xFF14151B)
                var step = 0
                while (contrastRatio(Color.White, fill) < 4.5f && step < 6) {
                    fill = lerpColor(fill, darkInk, 0.14f)
                    step++
                }
                fill to Color.White
            }
        }
        // 切换深浅色时按 palette.isLight 重置插值起点，保证日夜圆形揭示首帧对比度准确；换书封面时平滑过渡
        val heroAccentAnim = remember(palette.isLight) { Animatable(targetHeroAccent) }
        val heroCtaFillAnim = remember(palette.isLight) { Animatable(targetHeroCtaFill) }
        val heroCtaContentAnim = remember(palette.isLight) { Animatable(targetHeroCtaContent) }
        LaunchedEffect(targetHeroAccent, palette.isLight) {
            if (heroAccentAnim.value != targetHeroAccent) {
                heroAccentAnim.animateTo(targetHeroAccent, animationSpec = tween(durationMillis = 340))
            }
        }
        LaunchedEffect(targetHeroCtaFill, palette.isLight) {
            if (heroCtaFillAnim.value != targetHeroCtaFill) {
                heroCtaFillAnim.animateTo(targetHeroCtaFill, animationSpec = tween(durationMillis = 340))
            }
        }
        LaunchedEffect(targetHeroCtaContent, palette.isLight) {
            if (heroCtaContentAnim.value != targetHeroCtaContent) {
                heroCtaContentAnim.animateTo(targetHeroCtaContent, animationSpec = tween(durationMillis = 340))
            }
        }
        val heroAccent = heroAccentAnim.value
        val heroCtaFill = heroCtaFillAnim.value
        val heroCtaContent = heroCtaContentAnim.value

        // 计算小屏紧凑模式与固定顶部留白（scrollState 驱动的顶栏收敛隔离在 ReadingNowTopBarOverlay 内，避免滚动逐帧触发整页重组）
        val collapseThresholdPx = with(density) { 44.dp.toPx().coerceAtLeast(1f) }
        val topBarTopPad = topInset + 10.dp
        // 保持主体区顶部留白恒定（44dp 顶栏 + 16dp 呼吸间距），避免滚动时顶栏收敛引发重测量与顶栏压卡
        val contentTopClearance = topBarTopPad + 44.dp + 16.dp

        // 在自适应一屏与可滚动溢出之间无缝衔接，避免 verticalScroll 下 Modifier.weight 失效
        val availableBodyHeight = (maxHeight - contentTopClearance - columnBottomPad).coerceAtLeast(368.dp)
        val totalSpacing = 12.dp + 12.dp
        val usableCardsHeight = (availableBodyHeight - totalSpacing).coerceAtLeast(364.dp)
        val footprintHeight = if (isCompactScroll) 124.dp else (usableCardsHeight * 0.33f).coerceIn(118.dp, 158.dp)
        val heroHeight = if (isCompactScroll) 144.dp else (usableCardsHeight * 0.39f).coerceIn(136.dp, 188.dp)
        val gallerySectionHeight = if (isCompactScroll) 132.dp else (usableCardsHeight * 0.28f).coerceIn(124.dp, 162.dp)
        val recentRowTopFraction = remember(maxHeight, contentTopClearance, footprintHeight, heroHeight) {
            val topDp = contentTopClearance + footprintHeight + 12.dp + heroHeight + 12.dp + 32.dp
            (topDp / maxHeight.coerceAtLeast(1.dp)).coerceIn(0.56f, 0.72f)
        }

        val rootHandlesBottomAmbient = LocalRootHandlesBottomAmbient.current
        val homeDockBackdrop = if (powerSave || rootHandlesBottomAmbient) null else homeBackdrop
        val cardBlurBackdrop = rememberGlassBackdrop()
        val effectiveHomeBackdrop = if (powerSave) null else cardBlurBackdrop
        val ambientSource = LocalReadingStyle.current.ambientGlowSource
        val secondaryBook = if (heroBook?.coverPath != null) {
            heroBook
        } else {
            galleryBooks.firstOrNull { it.coverPath != null } ?: galleryBooks.firstOrNull()
        }
        val heroGlow = remember(heroBook?.id, heroBook?.coverPath, tick, palette, ambientSource) {
            if (ambientSource == AmbientGlowSource.appTheme) {
                palette.primary
            } else {
                heroBook?.let { CoverAssetCache.getAmbientColor(it, 0, palette) } ?: palette.primary
            }
        }
        val secondaryGlow = remember(secondaryBook?.id, secondaryBook?.coverPath, tick, palette, ambientSource) {
            if (ambientSource == AmbientGlowSource.appTheme) {
                palette.primarySoft
            } else {
                secondaryBook?.let { CoverAssetCache.getSecondaryAmbientColor(it, 1, palette) } ?: palette.primarySoft
            }
        }
        val liveAmbientColorsState = LocalAmbientGlowColorsState.current
        val staticAmbientPrimary = LocalAmbientGlowColors.current?.first
        var cachedLiveLowerPrimary by remember { mutableStateOf<Color?>(null) }
        val ambientPollActive = isActive || isVisible
        if (ambientPollActive) {
            val currentLiveLower = liveAmbientColorsState?.value?.first
            if (cachedLiveLowerPrimary != currentLiveLower) {
                cachedLiveLowerPrimary = currentLiveLower
            }
        }
        val lowerPrimary = cachedLiveLowerPrimary
            ?: staticAmbientPrimary
            ?: heroGlow
        val statusScrimColor = palette.background
        val statusScrimPeak = if (palette.isLight) 0.78f else 0.72f
        val statusScrimBrush = remember(statusScrimColor, statusScrimPeak) {
            Brush.verticalGradient(
                0.00f to statusScrimColor.copy(alpha = statusScrimPeak),
                0.55f to statusScrimColor.copy(alpha = statusScrimPeak * 0.45f),
                1.00f to Color.Transparent,
            )
        }
        val pagerTransitioningState = LocalPagerTransitioningState.current
        val staticPagerTransitioning = LocalPagerTransitioning.current
        val topHalfHeightCache = remember { floatArrayOf(-1f) }
        val topHalfFadeBrushCache = remember(palette.background) { arrayOfNulls<Brush>(1) }
        val waterfallLayer = rememberGraphicsLayer()

        CompositionLocalProvider(
            LocalVitroBackdrop provides effectiveHomeBackdrop,
            LocalGlassDarkTheme provides !palette.isLight,
            LocalOverscrollFactory provides null,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Waterfall-only Vitro backdrop (`effectiveHomeBackdrop`) for TopBar/Footprint/Hero cards,
                //    recording only the 0..56% top waterfall into `waterfallLayer` so bottom corner glow never enters Dock or Cards.
                if (effectiveHomeBackdrop != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                waterfallLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                            },
                    ) {
                        ReadingNowTopWaterfallAmbient(
                            bookIds = waterfallBookIds,
                            palette = palette,
                            versionTick = tick,
                            heroGlow = heroGlow,
                            secondaryGlow = secondaryGlow,
                            flowTimeProvider = { flowSecondsState.floatValue },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topInset + 22.dp)
                                .background(statusScrimBrush),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .glassBackdrop(effectiveHomeBackdrop)
                            .drawBehind {
                                if (rootHandlesBottomAmbient || (pagerTransitioningState?.value ?: staticPagerTransitioning)) {
                                    val fadeEndY = (size.height * 0.59f).coerceAtLeast(1f)
                                    val brush = if (topHalfFadeBrushCache[0] != null && kotlin.math.abs(fadeEndY - topHalfHeightCache[0]) <= 0.5f) {
                                        topHalfFadeBrushCache[0]!!
                                    } else {
                                        Brush.verticalGradient(
                                            0.00f to palette.background,
                                            0.86f to palette.background,
                                            1.00f to Color.Transparent,
                                            startY = 0f,
                                            endY = fadeEndY,
                                        ).also {
                                            topHalfHeightCache[0] = fadeEndY
                                            topHalfFadeBrushCache[0] = it
                                        }
                                    }
                                    drawRect(
                                        brush = brush,
                                        size = Size(size.width, fadeEndY),
                                    )
                                } else {
                                    drawRect(
                                        color = palette.background,
                                        size = size,
                                    )
                                }
                                drawLayer(waterfallLayer)
                            },
                    )
                }

                // 2. Isolate bottom ambient corner glow from `homeDockBackdrop` when standalone;
                //    when hosted inside MainActivity (`rootHandlesBottomAmbient`), root owns bottom glow + Dock backdrop.
                IsolatedAmbientGlassContainer(
                    backdrop = homeDockBackdrop,
                    paletteBackgroundColor = palette.background,
                    renderAmbientBackground = !rootHandlesBottomAmbient,
                    skipCornerGlowAndIsolation = rootHandlesBottomAmbient && effectiveHomeBackdrop != null,
                    ambientBackground = {
                        ReadingNowBottomCornerAmbient(
                            palette = palette,
                            lowerPrimary = lowerPrimary,
                            flowTimeProvider = { flowSecondsState.floatValue },
                            topLimitFraction = recentRowTopFraction.coerceAtLeast(0.58f),
                            shouldDrawProvider = { !(pagerTransitioningState?.value ?: staticPagerTransitioning) },
                        )
                    },
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (effectiveHomeBackdrop != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        drawLayer(waterfallLayer)
                                    },
                            )
                        } else {
                            ReadingNowTopWaterfallAmbient(
                                bookIds = waterfallBookIds,
                                palette = palette,
                                versionTick = tick,
                                heroGlow = heroGlow,
                                secondaryGlow = secondaryGlow,
                                flowTimeProvider = { flowSecondsState.floatValue },
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(topInset + 22.dp)
                                    .background(statusScrimBrush),
                            )
                        }

                // 2. 前景滚动内容区与顶部悬浮控制栏完全隔离：每张玻璃卡片与顶栏各自持有独立的 spacing = 0.dp 玻璃容器，
                //    保证卡片按压放大/弹性拖拽时与「• 在读」胶囊及主题球之间零融合、零变形、零鬼影。
                val canScrollDown by remember(scrollState) {
                    derivedStateOf { scrollState.canScrollForward }
                }
                val footprintFadeStartPx = with(density) { 16.dp.toPx() }
                val footprintFadeSpanPx = with(density) { 28.dp.toPx().coerceAtLeast(1f) }
                val footprintScrollAlpha by remember(scrollState, footprintFadeStartPx, footprintFadeSpanPx) {
                    derivedStateOf {
                        val over = scrollState.value - footprintFadeStartPx
                        if (over <= 0f) 1f else (1f - over / footprintFadeSpanPx).coerceIn(0f, 1f)
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(top = contentTopClearance, bottom = columnBottomPad),
                    ) {
                        // 阅读足迹周柱状卡片（独立 spacing = 0.dp 玻璃容器 + Card 变体，按压/拖拽不干扰顶栏或其他卡片）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f)
                                .then(
                                    if (effectiveHomeBackdrop != null) {
                                        Modifier.glassEffectContainer(spacing = 0.dp)
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            TopReadingFootprintCard(
                                palette = palette,
                                backdrop = effectiveHomeBackdrop,
                                accentColor = heroAccent,
                                todaySeconds = todaySeconds,
                                weekSeconds = weekSeconds,
                                monthSeconds = monthSeconds,
                                weekDailySeconds = weekDailySeconds,
                                todayWeekday = todayWeekday,
                                finishedCount = finishedCount,
                                totalBooks = books.size,
                                cardHeight = footprintHeight,
                                scale = 1f,
                                contentAlpha = footprintScrollAlpha,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 继续阅读主卡片（独立 spacing = 0.dp 玻璃容器）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(heroHeight)
                                .then(
                                    if (effectiveHomeBackdrop != null) {
                                        Modifier.glassEffectContainer(spacing = 0.dp)
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            if (heroBook == null) {
                                EmptyStageCard(
                                    palette = palette,
                                    backdrop = effectiveHomeBackdrop,
                                    title = "开启你的静谧阅读时光",
                                    subtitle = "前往「书架」导入 TXT 或 EPUB 书籍，正在阅读的书会置顶呈现在这里。",
                                    scale = 1f,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                ContinueHeroCard(
                                    book = heroBook,
                                    palette = palette,
                                    backdrop = effectiveHomeBackdrop,
                                    accentColor = heroAccent,
                                    ctaFillColor = heroCtaFill,
                                    ctaContentColor = heroCtaContent,
                                    cardHeight = heroHeight,
                                    scale = 1f,
                                    modifier = Modifier.fillMaxSize(),
                                    coverVersionTick = tick,
                                    onLaunchBook = ::launchBook,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 最近阅读画廊区块（整块区域从落指起锁定外层水平翻页并拦截残留水平增量/速度）
                        val latestOnRecentRowDragLockChanged by rememberUpdatedState(onRecentRowDragLockChanged)
                        val latestIsGalleryEmpty by rememberUpdatedState(galleryBooks.isEmpty())
                        val recentListState = rememberLazyListState()
                        val recentTouchSlop = LocalViewConfiguration.current.touchSlop
                        DisposableEffect(Unit) {
                            onDispose {
                                latestOnRecentRowDragLockChanged(false)
                            }
                        }
                        val recentRowNestedScrollConnection = remember(recentListState) {
                            object : NestedScrollConnection {
                                private fun shouldConsumeHorizontalPre(dx: Float): Boolean {
                                    if (dx == 0f) return false
                                    if (latestIsGalleryEmpty) return true
                                    return (dx > 0f && !recentListState.canScrollBackward) ||
                                        (dx < 0f && !recentListState.canScrollForward)
                                }

                                override fun onPreScroll(
                                    available: Offset,
                                    source: NestedScrollSource,
                                ): Offset {
                                    return if (shouldConsumeHorizontalPre(available.x)) {
                                        Offset(x = available.x, y = 0f)
                                    } else {
                                        Offset.Zero
                                    }
                                }

                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource,
                                ): Offset {
                                    return if (available.x != 0f) Offset(x = available.x, y = 0f) else Offset.Zero
                                }

                                override suspend fun onPreFling(available: Velocity): Velocity {
                                    return if (shouldConsumeHorizontalPre(available.x)) {
                                        Velocity(x = available.x, y = 0f)
                                    } else {
                                        Velocity.Zero
                                    }
                                }

                                override suspend fun onPostFling(
                                    consumed: Velocity,
                                    available: Velocity,
                                ): Velocity {
                                    return if (available.x != 0f) Velocity(x = available.x, y = 0f) else Velocity.Zero
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gallerySectionHeight)
                                .nestedScroll(recentRowNestedScrollConnection)
                                .pointerInput(Unit) {
                                    try {
                                        awaitPointerEventScope {
                                            var locked = false
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val hasActivePointer = event.changes.any { it.pressed }
                                                if (hasActivePointer != locked) {
                                                    locked = hasActivePointer
                                                    latestOnRecentRowDragLockChanged(hasActivePointer)
                                                }
                                            }
                                        }
                                    } finally {
                                        latestOnRecentRowDragLockChanged(false)
                                    }
                                }
                                .pointerInput(recentTouchSlop) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val pointerId = down.id
                                        val startPos = down.position
                                        var horizontalCaptured = false
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Main)
                                            val change = event.changes.firstOrNull { it.id == pointerId }
                                                ?: event.changes.firstOrNull()
                                            if (change == null || !change.pressed) {
                                                if (horizontalCaptured && change != null && !change.isConsumed) {
                                                    change.consume()
                                                }
                                                break
                                            }
                                            if (change.isConsumed) {
                                                break
                                            }
                                            val dx = change.position.x - startPos.x
                                            val dy = change.position.y - startPos.y
                                            if (!horizontalCaptured) {
                                                if (abs(dy) > recentTouchSlop && abs(dy) > abs(dx) * 1.35f) {
                                                    break
                                                }
                                                if (abs(dx) > recentTouchSlop && abs(dx) > abs(dy) * 1.05f) {
                                                    horizontalCaptured = true
                                                }
                                            }
                                            if (horizontalCaptured) {
                                                change.consume()
                                            }
                                        }
                                    }
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                Text(
                                    text = "最近阅读",
                                    color = palette.text,
                                    fontSize = 16.sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 2.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (galleryBooks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp)
                                        .then(
                                            if (effectiveHomeBackdrop != null) {
                                                Modifier.glassEffectContainer(spacing = 0.dp)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    EmptyStageCard(
                                        palette = palette,
                                        backdrop = effectiveHomeBackdrop,
                                        title = "最近阅读画廊",
                                        subtitle = "翻阅过的书籍将按时间顺序排列在横向画廊中，随时一键切回。",
                                        scale = 1f,
                                    )
                                }
                            } else {
                                val initialGalleryBoxHeightPx = remember(gallerySectionHeight, density) {
                                    with(density) { (gallerySectionHeight - 32.dp).roundToPx().coerceAtLeast(1) }
                                }
                                var galleryBoxHeightPx by remember { mutableIntStateOf(initialGalleryBoxHeightPx) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = true)
                                        .onSizeChanged { sz ->
                                            if (sz.height > 0 && galleryBoxHeightPx != sz.height) {
                                                galleryBoxHeightPx = sz.height
                                            }
                                        },
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    val galleryMaxHeight = with(density) { galleryBoxHeightPx.toDp() }
                                    val galleryCardHeight = (galleryMaxHeight - galleryVerticalAllowance * 2).coerceIn(80.dp, 124.dp)
                                    val galleryCardWidth = (galleryCardHeight.value * 1.88f).coerceIn(160f, 235f).dp
                                    LazyRow(
                                        state = recentListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clipToBounds(),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 20.dp,
                                            vertical = galleryVerticalAllowance,
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        items(
                                            count = galleryBooks.size,
                                            key = { idx -> galleryBooks[idx].id },
                                            contentType = { "recent_card" },
                                        ) { idx ->
                                            val book = galleryBooks[idx]
                                            Box(
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                                    fadeOutSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                                    placementSpec = spring(dampingRatio = 0.90f, stiffness = 380f),
                                                ),
                                            ) {
                                                RecentHorizontalCard(
                                                    book = book,
                                                    index = idx + 1,
                                                    palette = palette,
                                                    accentColor = heroAccent,
                                                    cardWidth = galleryCardWidth,
                                                    cardHeight = galleryCardHeight,
                                                    scale = 1f,
                                                    coverVersionTick = tick,
                                                    onLaunchBook = ::launchBook,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (canScrollDown) {
                        val bottomScrimColor = palette.background
                        val bottomScrimBrush = remember(bottomScrimColor) {
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.7f to bottomScrimColor.copy(alpha = 0.72f),
                                1.0f to bottomScrimColor,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(columnBottomPad)
                                .background(bottomScrimBrush),
                        )
                    }

                    // 3. 悬浮在 Backdrop 之上的 iOS 27 顶部玻璃控制栏（独立 spacing = 0.dp 玻璃容器，彻底隔离下方卡片按压/拖拽形变与鬼影）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (effectiveHomeBackdrop != null) {
                                    Modifier.glassEffectContainer(spacing = 0.dp)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(top = topBarTopPad, start = 20.dp, end = 20.dp),
                        ) {
                            ReadingNowTopBarOverlay(
                                scrollState = scrollState,
                                collapseThresholdPx = collapseThresholdPx,
                                isCompactScroll = isCompactScroll,
                                palette = palette,
                                backdrop = effectiveHomeBackdrop,
                                accentColor = heroAccent,
                                heroBook = heroBook,
                                readingCount = readingBooks.size,
                                buttonRevealProgress = themeRevealButtonProgress,
                                onToggleBrightness = onToggleBrightness,
                            )
                        }
                    }
                }
            }
        }
            }
        }
    }
}

@Composable
private fun ReadingNowTopBarOverlay(
    scrollState: androidx.compose.foundation.ScrollState,
    collapseThresholdPx: Float,
    isCompactScroll: Boolean,
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    accentColor: Color,
    heroBook: BookEntry?,
    readingCount: Int,
    buttonRevealProgress: Float,
    onToggleBrightness: (Offset) -> Unit,
) {
    val isCollapsed by remember(scrollState, collapseThresholdPx, isCompactScroll) {
        androidx.compose.runtime.derivedStateOf {
            isCompactScroll || scrollState.value > collapseThresholdPx * 0.72f
        }
    }
    ReadingNowTopBar(
        palette = palette,
        backdrop = backdrop,
        accentColor = accentColor,
        heroBook = heroBook,
        readingCount = readingCount,
        isCollapsed = isCollapsed,
        buttonRevealProgress = buttonRevealProgress,
        onToggleBrightness = onToggleBrightness,
    )
}

private class WaterfallAmbientBrushCache(
    val isLight: Boolean,
    val bg: Color,
    val heroGlow: Color,
    val secondaryGlow: Color,
) {
    private var lastSize: Size = Size.Unspecified
    var vignetteBrush: Brush? = null
        private set
    var centerGlowBrush: Brush? = null
        private set
    var centerGlowRadius: Float = 0f
        private set

    fun ensureForSize(size: Size) {
        if (size == lastSize && vignetteBrush != null) return
        lastSize = size
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)

        vignetteBrush = if (isLight) {
            Brush.verticalGradient(
                0.00f to Color.White.copy(alpha = 0.36f),
                0.20f to Color.White.copy(alpha = 0.10f),
                0.42f to Color.White.copy(alpha = 0.05f),
                0.68f to bg.copy(alpha = 0.34f),
                0.86f to bg.copy(alpha = 0.78f),
                1.00f to bg,
                startY = 0f,
                endY = h * 0.56f,
            )
        } else {
            Brush.verticalGradient(
                0.00f to bg.copy(alpha = 0.44f),
                0.20f to bg.copy(alpha = 0.18f),
                0.42f to bg.copy(alpha = 0.14f),
                0.68f to bg.copy(alpha = 0.44f),
                0.86f to bg.copy(alpha = 0.82f),
                1.00f to bg,
                startY = 0f,
                endY = h * 0.56f,
            )
        }

        centerGlowRadius = w * 0.86f
        centerGlowBrush = Brush.radialGradient(
            colors = listOf(
                heroGlow.copy(alpha = if (isLight) 0.068f else 0.056f),
                secondaryGlow.copy(alpha = if (isLight) 0.032f else 0.024f),
                Color.Transparent,
            ),
            center = Offset.Zero,
            radius = centerGlowRadius,
        )
    }
}

@Composable
private fun ReadingNowTopWaterfallAmbient(
    bookIds: List<String>,
    palette: AppPalette,
    versionTick: Int,
    heroGlow: Color,
    secondaryGlow: Color,
    flowTimeProvider: () -> Float,
) {
    val isLight = palette.isLight
    val brushCache = remember(
        isLight,
        palette.background,
        heroGlow,
        secondaryGlow,
    ) {
        WaterfallAmbientBrushCache(
            isLight = isLight,
            bg = palette.background,
            heroGlow = heroGlow,
            secondaryGlow = secondaryGlow,
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val currentFlowSeconds = flowTimeProvider()
        if (versionTick >= 0) {
            drawIntoCanvas { canvas ->
                CoverAssetCache.drawDiagonalCoverWaterfallToCanvas(
                    canvas = canvas.nativeCanvas,
                    w = size.width,
                    h = size.height,
                    bookIds = bookIds,
                    flowTimeSeconds = currentFlowSeconds,
                    isLight = isLight,
                    bgColor = palette.background,
                    skipFullClear = true,
                )
            }
        }

        brushCache.ensureForSize(size)

        // Waterfall + top scatter glow strictly confined to 0..56%, leaving lower 56..100% transparent
        // so bottom corner glow shines cleanly beneath cards without staining the Dock backdrop.
        brushCache.vignetteBrush?.let {
            drawRect(brush = it, size = Size(size.width, size.height * 0.56f))
        }

        val phase = currentFlowSeconds * 0.48f
        val driftX1 = sin(phase) * size.width * 0.08f
        val driftY1 = cos(phase * 0.72f) * size.height * 0.032f
        val pulse = 0.88f + 0.12f * ((sin(phase * 0.90f) + 1f) * 0.5f)

        brushCache.centerGlowBrush?.let { brush ->
            val r = brushCache.centerGlowRadius
            if (r > 1f) {
                val cx = size.width * 0.68f + driftX1
                val cy = size.height * 0.48f + driftY1
                clipRect(bottom = size.height * 0.56f) {
                    withTransform({ translate(left = cx, top = cy) }) {
                        drawCircle(brush = brush, radius = r, center = Offset.Zero, alpha = pulse)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingNowBottomCornerAmbient(
    palette: AppPalette,
    lowerPrimary: Color,
    flowTimeProvider: () -> Float,
    topLimitFraction: Float,
    shouldDrawProvider: () -> Boolean = { true },
) {
    val isLight = palette.isLight
    val geminiBrushCache = remember(isLight, lowerPrimary) {
        GeminiCornerGlowBrushCache(
            isLight = isLight,
            primaryColor = lowerPrimary,
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!shouldDrawProvider()) return@Canvas
        drawRect(
            color = palette.background,
            topLeft = Offset(0f, size.height * 0.55f),
        )
        drawGeminiCornerAmbientGlow(
            brushCache = geminiBrushCache,
            flowSeconds = flowTimeProvider(),
            topLimitFraction = topLimitFraction,
        )
    }
}

private enum class ThemeOrbIconMode {
    System,
    Light,
    Dark,
}

@Composable
private fun ReadingNowTopBar(
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    accentColor: Color = palette.accentText,
    heroBook: BookEntry?,
    readingCount: Int,
    isCollapsed: Boolean,
    buttonRevealProgress: Float,
    onToggleBrightness: (Offset) -> Unit,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    val badgeText = heroBook?.let { it.shelfName?.takeIf { name -> name.isNotBlank() } ?: "默认书架" }
        ?: if (readingCount > 0) "$readingCount 本在读" else null

    val topPillHeight = 44.dp
    val titleFontSp = 19.5.sp
    val pillHorizPad = 15.5.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // iOS 27 Vitro 悬浮标题胶囊：支持顺滑橡皮筋拖拽与按压微膨胀，独立于主题球与下方卡片，不可点击
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(topPillHeight)
                .vitroInteractiveCard(
                    shape = CircleShape,
                    variant = Ios27GlassVariant.Chrome,
                    palette = palette,
                    style = readingStyle,
                    backdrop = backdrop,
                    verticalOnly = false,
                    maxPullDp = 24.dp,
                    useLayoutOffset = true,
                    pressScaleTarget = 1.06f,
                    showFingerHighlight = true,
                    onClick = null,
                )
                .padding(horizontal = pillHorizPad),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
            Spacer(modifier = Modifier.width(7.5.dp))
            Text(
                text = "在读",
                color = palette.text,
                fontSize = titleFontSp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            AnimatedVisibility(
                visible = badgeText != null && !isCollapsed,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                if (badgeText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(11.dp)
                                .background(palette.text.copy(alpha = if (isLight) 0.16f else 0.22f)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = badgeText,
                            color = palette.text.copy(alpha = if (isLight) 0.66f else 0.72f),
                            fontSize = 12.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 136.dp),
                        )
                    }
                }
            }
        }

        val orbIconMode = when (readingStyle.brightnessMode) {
            AppBrightnessMode.system -> ThemeOrbIconMode.System
            AppBrightnessMode.light -> ThemeOrbIconMode.Light
            AppBrightnessMode.dark -> ThemeOrbIconMode.Dark
            AppBrightnessMode.scheduled -> if (isLight) ThemeOrbIconMode.Light else ThemeOrbIconMode.Dark
        }
        val isPowerSave = LocalPowerSaveMode.current
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (backdrop != null && !isPowerSave) {
                        Modifier.glassEffectContainer(spacing = 0.dp)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.CenterEnd,
        ) {
            LiquidGlassDraggableCircleButton(
                palette = palette,
                size = topPillHeight,
                backdropBook = heroBook,
                revealProgress = buttonRevealProgress,
                pressScaleTarget = 1.13f,
                showFingerHighlight = true,
                onClickWithCenterInRoot = onToggleBrightness,
            ) {
                AnimatedContent(
                    targetState = orbIconMode,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(durationMillis = 220)) +
                            scaleIn(initialScale = 0.72f, animationSpec = tween(durationMillis = 240)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 160)) +
                                    scaleOut(targetScale = 0.72f, animationSpec = tween(durationMillis = 180)),
                            )
                    },
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(20.dp),
                    label = "themeOrbIcon",
                ) { mode ->
                    when (mode) {
                        ThemeOrbIconMode.System -> {
                            val iconTint = palette.text
                            Canvas(
                                modifier = Modifier
                                    .size(20.dp)
                                    .semantics { contentDescription = "深浅色：跟随设备" },
                            ) {
                                val strokePx = 1.65.dp.toPx()
                                val insetPx = 1.75.dp.toPx()
                                val ringRadius = (size.minDimension * 0.5f - insetPx - strokePx * 0.5f)
                                    .coerceAtLeast(1f)
                                val arcTopLeft = Offset(
                                    x = center.x - ringRadius,
                                    y = center.y - ringRadius,
                                )
                                val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
                                drawArc(
                                    color = iconTint,
                                    startAngle = 90f,
                                    sweepAngle = 180f,
                                    useCenter = true,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                )
                                drawCircle(
                                    color = iconTint,
                                    radius = ringRadius,
                                    center = center,
                                    style = Stroke(width = strokePx),
                                )
                            }
                        }

                        ThemeOrbIconMode.Light -> {
                            Icon(
                                imageVector = Icons.Outlined.LightMode,
                                contentDescription = "深浅色：浅色",
                                tint = palette.text,
                                modifier = Modifier.size(19.5.dp),
                            )
                        }

                        ThemeOrbIconMode.Dark -> {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = "深浅色：深色",
                                tint = palette.text,
                                modifier = Modifier.size(19.5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopReadingFootprintCard(
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    accentColor: Color = if (palette.isLight) {
        lerpColor(palette.accentText, Color.Black, 0.20f)
    } else {
        lerpColor(palette.accentText, Color.White, 0.14f)
    },
    todaySeconds: Int,
    weekSeconds: Int,
    monthSeconds: Int,
    weekDailySeconds: List<Int>,
    todayWeekday: Int,
    finishedCount: Int,
    totalBooks: Int,
    cardHeight: Dp = Dp.Unspecified,
    scale: Float = 1f,
    contentAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    // 无框透亮 Bento 子块（彻底移除黑灰色突兀描边与浑浊暗底）
    val tileBg = if (isLight) Color.White.copy(alpha = 0.56f) else Color.White.copy(alpha = 0.085f)
    val accentOnGlass = accentColor
    val secondaryText = palette.text.copy(alpha = if (isLight) 0.64f else 0.70f)
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val maxDaySec = remember(weekDailySeconds) {
        (weekDailySeconds.maxOrNull() ?: 1800).coerceAtLeast(1800)
    }

    val compactScale = if (cardHeight != Dp.Unspecified && cardHeight < 132.dp) {
        minOf(scale, 0.90f)
    } else {
        scale
    }
    val vPad = (11f * compactScale).coerceIn(8f, 14f).dp
    val hPad = (15f * scale).coerceIn(12f, 16f).dp
    val barHeight = (28f * compactScale).coerceIn(22f, 32f).dp
    val barWidth = (6.5f * scale).coerceIn(5.5f, 7.5f).dp
    val cornerRadius = (22f * scale).coerceIn(18f, 22f).dp
    val todayBarBrush = remember(accentOnGlass) {
        Brush.verticalGradient(
            listOf(
                lerpColor(accentOnGlass, Color.White, 0.28f),
                accentOnGlass,
            ),
        )
    }
    val pastActiveBarBrush = remember(accentOnGlass, isLight) {
        Brush.verticalGradient(
            listOf(
                accentOnGlass.copy(alpha = if (isLight) 0.62f else 0.68f),
                accentOnGlass.copy(alpha = if (isLight) 0.28f else 0.34f),
            ),
        )
    }

    val cardShape = RoundedCornerShape(cornerRadius)
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .then(if (cardHeight != Dp.Unspecified) Modifier.heightIn(min = cardHeight) else Modifier.fillMaxHeight())
            .then(if (contentAlpha < 0.999f) Modifier.graphicsLayer { alpha = contentAlpha } else Modifier)
            .vitroInteractiveCard(
                shape = cardShape,
                variant = Ios27GlassVariant.Card,
                palette = palette,
                style = readingStyle,
                backdrop = backdrop,
                verticalOnly = false,
                maxPullDp = 20.dp,
                pressScaleTarget = 1.028f,
                showFingerHighlight = true,
                onClick = null,
            )
            .padding(horizontal = hPad, vertical = vPad),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "本周累计阅读",
                    color = secondaryText,
                    fontSize = (11.5f * scale).coerceIn(10f, 12.5f).sp,
                    lineHeight = (14f * scale).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
                Spacer(modifier = Modifier.height((2.5f * compactScale).dp))
                Text(
                    text = compactDurationLabel(weekSeconds),
                    color = palette.text,
                    fontSize = (22f * compactScale).coerceIn(18f, 24.5f).sp,
                    lineHeight = (24.5f * compactScale).sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(modifier = Modifier.height((3.5f * compactScale).dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((5f * compactScale).dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (todaySeconds > 0) {
                                accentOnGlass.copy(alpha = if (isLight) 0.085f else 0.13f)
                            } else {
                                if (isLight) Color.Black.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.09f)
                            },
                        )
                        .padding(
                            horizontal = (8f * compactScale).dp,
                            vertical = (2f * compactScale).dp,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .size((5f * scale).dp)
                            .clip(CircleShape)
                            .background(
                                if (todaySeconds > 0) accentOnGlass else secondaryText.copy(alpha = 0.55f),
                            ),
                    )
                    Text(
                        text = if (todaySeconds > 0) {
                            "今日 ${compactDurationLabel(todaySeconds)}"
                        } else {
                            "今日尚未翻阅"
                        },
                        color = if (todaySeconds > 0) accentOnGlass else secondaryText,
                        fontSize = (10.5f * scale).coerceIn(9f, 11.5f).sp,
                        lineHeight = (12.5f * scale).sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width((12f * scale).dp))

            // 极简悬浮胶囊周趋势：去灰底条，保留基线刻度与今日微光高亮
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy((6.5f * scale).coerceIn(5.5f, 8f).dp),
                ) {
                    for (i in 0 until 7) {
                        val sec = weekDailySeconds.getOrElse(i) { 0 }
                        val hasRead = sec > 0
                        val isToday = (i + 1) == todayWeekday
                        val fillRatio = (sec.toFloat() / maxDaySec).coerceIn(0f, 1f)
                        val fillHeight = when {
                            hasRead && isToday -> (barHeight * fillRatio.coerceIn(0.32f, 1f))
                            hasRead -> (barHeight * fillRatio.coerceIn(0.26f, 0.96f))
                            isToday -> (6f * scale).coerceIn(5f, 7f).dp
                            else -> (4.5f * scale).coerceIn(3.5f, 5.5f).dp
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                contentAlignment = Alignment.BottomCenter,
                                modifier = Modifier
                                    .width(barWidth)
                                    .height(barHeight),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(fillHeight)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .then(
                                            when {
                                                isToday && hasRead -> Modifier.background(todayBarBrush)
                                                isToday -> Modifier.background(
                                                    accentOnGlass.copy(alpha = if (isLight) 0.36f else 0.45f),
                                                )
                                                hasRead -> Modifier.background(pastActiveBarBrush)
                                                else -> Modifier.background(
                                                    if (isLight) Color(0xFF14151C).copy(alpha = 0.14f)
                                                    else Color.White.copy(alpha = 0.18f),
                                                )
                                            },
                                        ),
                                )
                            }
                            Spacer(modifier = Modifier.height((3.5f * scale).dp))
                            Box(
                                modifier = Modifier
                                    .size((15.5f * scale).coerceIn(13.5f, 17f).dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isToday) {
                                            accentOnGlass.copy(alpha = if (isLight) 0.14f else 0.24f)
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = weekLabels[i],
                                    color = if (isToday) accentOnGlass else secondaryText,
                                    fontSize = (9.5f * scale).coerceIn(8.5f, 10.5f).sp,
                                    lineHeight = (11f * scale).sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = (8f * compactScale).dp),
            horizontalArrangement = Arrangement.spacedBy((8f * compactScale).dp),
        ) {
            FootprintSoftTile(
                label = "本月累计",
                value = compactDurationLabel(monthSeconds),
                palette = palette,
                backgroundColor = tileBg,
                scale = compactScale,
                modifier = Modifier.weight(1f),
            )
            FootprintSoftTile(
                label = "已读完",
                value = "$finishedCount 本",
                palette = palette,
                backgroundColor = tileBg,
                scale = compactScale,
                modifier = Modifier.weight(1f),
            )
            FootprintSoftTile(
                label = "书架藏书",
                value = "$totalBooks 本",
                palette = palette,
                backgroundColor = tileBg,
                scale = compactScale,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FootprintSoftTile(
    label: String,
    value: String,
    palette: AppPalette,
    backgroundColor: Color,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val isLight = palette.isLight
    val tileShape = RoundedCornerShape((13f * scale).coerceIn(11f, 14f).dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(tileShape)
            .background(backgroundColor)
            .padding(vertical = (6f * scale).coerceIn(4.5f, 8f).dp, horizontal = 6.dp),
    ) {
        Text(
            text = value,
            color = palette.text,
            fontSize = (13.5f * scale).coerceIn(11.5f, 14.5f).sp,
            lineHeight = (15.5f * scale).sp,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height((2f * scale).dp))
        Text(
            text = label,
            color = palette.text.copy(alpha = if (isLight) 0.62f else 0.68f),
            fontSize = (10.5f * scale).coerceIn(9f, 11.5f).sp,
            lineHeight = (12.5f * scale).sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContinueHeroCard(
    book: BookEntry,
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    accentColor: Color,
    ctaFillColor: Color,
    ctaContentColor: Color,
    cardHeight: Dp = Dp.Unspecified,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
    coverVersionTick: Int = CoverAssetCache.versionTick,
    onLaunchBook: (BookEntry, Rect?) -> Unit,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    val tick = coverVersionTick
    val accentOnGlass = accentColor
    val secondaryText = palette.text.copy(alpha = if (isLight) 0.68f else 0.74f)

    val pad = (14f * scale).coerceIn(11f, 16f).dp
    val heroCorner = (22f * scale).coerceIn(18f, 22f).dp
    val coverCorner = (13f * scale).coerceIn(10f, 14f).dp
    val coverShape = RoundedCornerShape(coverCorner)

    val coverCoordsMap = remember { mutableMapOf<String, LayoutCoordinates>() }
    val latestBook by rememberUpdatedState(book)
    val heroCardShape = RoundedCornerShape(heroCorner)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (cardHeight != Dp.Unspecified) Modifier.height(cardHeight) else Modifier.fillMaxHeight())
            .vitroInteractiveCard(
                shape = heroCardShape,
                variant = Ios27GlassVariant.Card,
                palette = palette,
                style = readingStyle,
                backdrop = backdrop,
                verticalOnly = false,
                maxPullDp = 16.dp,
                pressScaleTarget = 1.028f,
                showFingerHighlight = true,
                onClick = {
                    val target = latestBook
                    val bounds = coverCoordsMap[target.id]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()
                    onLaunchBook(target, bounds)
                },
            ),
    ) {
        AnimatedContent(
            targetState = book,
            contentKey = { it.id },
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                    .togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                    )
            },
            modifier = Modifier.fillMaxSize(),
            label = "heroBookCrossfade",
        ) { displayBook ->
            val ambientColor = remember(displayBook.id, displayBook.coverPath, tick, palette) {
                CoverAssetCache.getAmbientColor(displayBook, 0, palette)
            }
            val authorClean = remember(displayBook.author) {
                displayBook.author.trim().takeIf { it.isNotEmpty() && it != "未知" && it != "未知作者" }
            }
            val eyebrowText = remember(authorClean, displayBook.lastReadAtMillis) {
                val timeText = relativeTimeAgoLabel(displayBook.lastReadAtMillis)
                if (authorClean != null) "$authorClean · $timeText" else timeText
            }
            val animatedProgress by animateFloatAsState(
                targetValue = displayBook.progress.toFloat().coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                label = "heroProgress",
            )
            val progressPercent = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)
            val coverShadowTint = if (isLight) {
                lerpColor(ambientColor, Color.Black, 0.55f).copy(alpha = 0.42f)
            } else {
                lerpColor(ambientColor, Color.Black, 0.35f).copy(alpha = 0.70f)
            }

            DisposableEffect(displayBook.id) {
                onDispose {
                    coverCoordsMap.remove(displayBook.id)
                }
            }

            val cardDensity = LocalDensity.current
            val cardFallbackHeightPx = LocalWindowInfo.current.containerSize.height.coerceAtLeast(1)
            var cardBoxHeightPx by remember(cardHeight) {
                mutableIntStateOf(
                    if (cardHeight != Dp.Unspecified) {
                        with(cardDensity) { cardHeight.roundToPx().coerceAtLeast(1) }
                    } else {
                        cardFallbackHeightPx
                    },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (cardHeight == Dp.Unspecified) {
                            Modifier.onSizeChanged { sz ->
                                if (sz.height > 0 && cardBoxHeightPx != sz.height) {
                                    cardBoxHeightPx = sz.height
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                val effectiveHeight = if (cardHeight != Dp.Unspecified) {
                    cardHeight
                } else {
                    with(cardDensity) { cardBoxHeightPx.toDp() }
                }
                val coverHeight = (effectiveHeight - pad * 2).coerceAtLeast(84.dp)
                val coverWidth = (coverHeight.value * 0.69f).dp
                val heroSlotAlpha = LocalHeroBookSlotAlpha.current
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = coverWidth, height = coverHeight)
                            .graphicsLayer { alpha = heroSlotAlpha(displayBook.id) }
                            .shadow(
                                elevation = if (isLight) 11.dp else 15.dp,
                                shape = coverShape,
                                clip = false,
                                ambientColor = coverShadowTint,
                                spotColor = coverShadowTint,
                            )
                            .onPlaced { coords ->
                                coverCoordsMap[displayBook.id] = coords
                            },
                    ) {
                        BookCoverView(
                            book = displayBook,
                            palette = palette,
                            width = coverWidth,
                            height = coverHeight,
                            cornerRadius = coverCorner,
                            coverVersionTick = tick,
                        )
                    }
                    Spacer(modifier = Modifier.width((15f * scale).dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            AnimatedContent(
                                targetState = eyebrowText,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)),
                                        )
                                },
                                label = "heroEyebrowTransition",
                            ) { labelText ->
                                Text(
                                    text = labelText,
                                    color = secondaryText,
                                    fontSize = (11.5f * scale).coerceIn(10f, 12.5f).sp,
                                    lineHeight = (14f * scale).sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(modifier = Modifier.height((4f * scale).dp))
                            Text(
                                text = cleanBookTitle(displayBook.title),
                                color = palette.text,
                                fontSize = (17.5f * scale).coerceIn(15f, 19f).sp,
                                lineHeight = (21.5f * scale).sp,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.35).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height((3.5f * scale).dp))
                            AnimatedContent(
                                targetState = displayBook.safeCurrentChapter.title,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                                        slideInVertically(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) {
                                            (it * 0.25f).roundToInt()
                                        })
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                                                slideOutVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) {
                                                    -(it * 0.25f).roundToInt()
                                                },
                                        )
                                },
                                label = "heroChapterTransition",
                            ) { chapterTitle ->
                                Text(
                                    text = chapterTitle,
                                    color = secondaryText,
                                    fontSize = (12f * scale).coerceIn(10.5f, 13f).sp,
                                    lineHeight = (15f * scale).sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    color = accentOnGlass,
                                    trackColor = palette.text.copy(alpha = if (isLight) 0.14f else 0.18f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.6.dp)
                                        .clip(RoundedCornerShape(percent = 50)),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$progressPercent%",
                                    color = palette.text.copy(alpha = if (isLight) 0.78f else 0.86f),
                                    fontSize = (11.5f * scale).coerceIn(10f, 12.5f).sp,
                                    lineHeight = (13.5f * scale).sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(modifier = Modifier.height((8f * scale).dp))
                            val ctaFill = ctaFillColor
                            val ctaContent = ctaContentColor
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .height((34f * scale).coerceIn(28f, 36f).dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(ctaFill)
                                    .padding(horizontal = (15f * scale).dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = ctaContent,
                                    modifier = Modifier.size((15.5f * scale).dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "继续阅读",
                                    color = ctaContent,
                                    fontSize = (12.5f * scale).coerceIn(11f, 13.5f).sp,
                                    lineHeight = (14.5f * scale).sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentHorizontalCard(
    book: BookEntry,
    index: Int,
    palette: AppPalette,
    accentColor: Color,
    cardWidth: Dp,
    cardHeight: Dp,
    scale: Float,
    coverVersionTick: Int = CoverAssetCache.versionTick,
    onLaunchBook: (BookEntry, Rect?) -> Unit,
) {
    val isLight = palette.isLight
    val hasCover = book.coverPath != null
    val tick = coverVersionTick
    val ambientColor = remember(book.id, book.coverPath, tick, palette) {
        CoverAssetCache.getAmbientColor(book, index, palette)
    }
    val accentOnSurface = accentColor
    val secondaryText = palette.text.copy(alpha = if (isLight) 0.66f else 0.72f)
    val progress = book.progress.coerceIn(0.0, 1.0)
    val progressPercent = (progress * 100.0).roundToInt()

    val pad = (12f * scale).coerceIn(10f, 14f).dp
    val coverHeight = (cardHeight - pad * 2).coerceAtLeast(68.dp)
    val coverWidth = (coverHeight.value * 0.68f).dp
    val recentCorner = (20f * scale).coerceIn(16f, 22f).dp
    val coverCorner = (10f * scale).coerceIn(8f, 11f).dp
    val coverShape = RoundedCornerShape(coverCorner)
    val coverShadowTint = if (isLight) {
        lerpColor(ambientColor, Color.Black, 0.55f).copy(alpha = 0.36f)
    } else {
        lerpColor(ambientColor, Color.Black, 0.35f).copy(alpha = 0.62f)
    }

    val coverCoordsHolder = remember(book.id) { arrayOfNulls<LayoutCoordinates>(1) }
    val cardCoordsHolder = remember(book.id) { arrayOfNulls<LayoutCoordinates>(1) }
    val recentCardShape = RoundedCornerShape(recentCorner)
    val recentCardInteractionSource = remember(book.id) { MutableInteractionSource() }
    val recentHaptics = LocalHapticFeedback.current
    val cardSurfaceColor = if (isLight) Color.White else Color(0xFF1C1C1E)

    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .onPlaced { coords ->
                cardCoordsHolder[0] = coords
            }
            .clip(recentCardShape)
            .background(cardSurfaceColor)
            .clickable(
                interactionSource = recentCardInteractionSource,
                indication = null,
            ) {
                recentHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val coords = coverCoordsHolder[0]?.takeIf { it.isAttached }
                    ?: cardCoordsHolder[0]?.takeIf { it.isAttached }
                onLaunchBook(book, coords?.unclippedBoundsInRoot())
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasCover) {
                val heroSlotAlpha = LocalHeroBookSlotAlpha.current
                Box(
                    modifier = Modifier
                        .size(width = coverWidth, height = coverHeight)
                        .graphicsLayer { alpha = heroSlotAlpha(book.id) }
                        .shadow(
                            elevation = if (isLight) 7.dp else 10.dp,
                            shape = coverShape,
                            clip = false,
                            ambientColor = coverShadowTint,
                            spotColor = coverShadowTint,
                        )
                        .onPlaced { coords ->
                            coverCoordsHolder[0] = coords
                        },
                ) {
                    BookCoverView(
                        book = book,
                        palette = palette,
                        width = coverWidth,
                        height = coverHeight,
                        cornerRadius = coverCorner,
                        coverVersionTick = tick,
                    )
                }
                Spacer(modifier = Modifier.width((11f * scale).dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = coverHeight * 0.8f)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(deriveHeroAccentColor(ambientColor, isLight)),
                )
                Spacer(modifier = Modifier.width((10f * scale).dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = relativeTimeAgoLabel(book.lastReadAtMillis),
                        color = secondaryText,
                        fontSize = (10.5f * scale).coerceIn(9.5f, 11.5f).sp,
                        lineHeight = (12.5f * scale).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height((2.5f * scale).dp))
                    Text(
                        text = cleanBookTitle(book.title),
                        color = palette.text,
                        fontSize = (13.5f * scale).coerceIn(12f, 14.5f).sp,
                        lineHeight = (16.5f * scale).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height((2f * scale).dp))
                    Text(
                        text = book.safeCurrentChapter.title,
                        color = secondaryText,
                        fontSize = (10.5f * scale).coerceIn(9.5f, 11.5f).sp,
                        lineHeight = (12.5f * scale).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (progressPercent >= 100) {
                    Text(
                        text = "已读完",
                        color = accentOnSurface,
                        fontSize = (10.5f * scale).coerceIn(9.5f, 11.5f).sp,
                        lineHeight = (12.5f * scale).sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progress.toFloat() },
                            color = accentOnSurface,
                            trackColor = palette.text.copy(alpha = if (isLight) 0.13f else 0.16f),
                            modifier = Modifier
                                .weight(1f)
                                .height(3.2.dp)
                                .clip(RoundedCornerShape(percent = 50)),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (progressPercent > 0) "$progressPercent%" else "未读",
                            color = palette.text.copy(alpha = if (isLight) 0.74f else 0.80f),
                            fontSize = (10.5f * scale).coerceIn(9.5f, 11.5f).sp,
                            lineHeight = (12.5f * scale).sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStageCard(
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    title: String,
    subtitle: String,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val readingStyle = LocalReadingStyle.current
    val emptyCardShape = RoundedCornerShape((20f * scale).dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .vitroInteractiveCard(
                shape = emptyCardShape,
                variant = Ios27GlassVariant.Card,
                palette = palette,
                style = readingStyle,
                backdrop = backdrop,
                verticalOnly = false,
                maxPullDp = 16.dp,
                pressScaleTarget = 1.028f,
                showFingerHighlight = true,
                onClick = null,
            )
            .padding((18f * scale).dp),
    ) {
        Text(
            text = title,
            color = palette.text,
            fontSize = (16.5f * scale).sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height((6f * scale).dp))
        Text(
            text = subtitle,
            color = palette.text.copy(alpha = if (palette.isLight) 0.66f else 0.72f),
            fontSize = (12.5f * scale).sp,
            lineHeight = (19f * scale).sp,
        )
    }
}

fun compactDurationLabel(seconds: Int): String {
    if (seconds <= 0) return "0 分钟"
    val minutes = if (seconds < 60) 1 else (seconds / 60.0).roundToInt()
    if (minutes < 60) return "$minutes 分钟"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "${hours}小时" else "${hours}小时${rest}分"
}

fun relativeTimeAgoLabel(timeMillis: Long?): String {
    if (timeMillis == null || timeMillis <= 0L) return "尚未翻阅"
    val diffSec = ((System.currentTimeMillis() - timeMillis) / 1000L).coerceAtLeast(0L)
    val diffMin = diffSec / 60L
    if (diffMin < 1L) return "刚刚读过"
    if (diffMin < 60L) return "$diffMin 分钟前读过"
    val diffHours = diffMin / 60L
    if (diffHours < 24L) return "$diffHours 小时前读过"
    val diffDays = diffHours / 24L
    if (diffDays < 7L) return "$diffDays 天前读过"
    val dt = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return "${dt.monthValue}月${dt.dayOfMonth}日读过"
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = maxOf(foreground.luminance(), background.luminance())
    val l2 = minOf(foreground.luminance(), background.luminance())
    return (l1 + 0.05f) / (l2 + 0.05f)
}

/**
 * 从书籍封面主色推导适合直接贴于日间亮面/夜间暗面玻璃上的可读强调色：
 * 保留真实色相与色彩性格，同时保证文字/指示点/进度条具备稳定对比度。
 */
private fun deriveHeroAccentColor(rawColor: Color, isLight: Boolean): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(rawColor.toArgb(), hsv)
    val isNeutral = hsv[1] < 0.08f
    if (!isNeutral) {
        hsv[1] = if (isLight) {
            (hsv[1] * 0.76f).coerceIn(0.22f, 0.48f)
        } else {
            (hsv[1] * 0.76f).coerceIn(0.20f, 0.44f)
        }
    }
    hsv[2] = if (isLight) {
        val maxVal = if (!isNeutral && hsv[0] in 42f..85f) 0.48f else 0.58f
        hsv[2].coerceIn(0.24f, maxVal)
    } else {
        hsv[2].coerceIn(0.76f, 0.96f)
    }
    var candidate = Color(AndroidColor.HSVToColor(hsv))
    val referenceSurface = if (isLight) Color(0xFFEAECEF) else Color(0xFF1D1F26)
    val minContrast = if (isLight) 4.3f else 4.6f
    val targetAnchor = if (isLight) Color(0xFF121318) else Color(0xFFF6F7FB)
    var step = 0
    while (contrastRatio(candidate, referenceSurface) < minContrast && step < 8) {
        candidate = lerpColor(candidate, targetAnchor, 0.16f)
        step++
    }
    return candidate
}

/**
 * 从书籍封面主色推导「继续阅读」实底胶囊按钮颜色与前景色：
 * 统一使用高对比度纯白文字，若封面色明度偏高则柔和压深至满足 WCAG AA 对比度并保留鲜明色相。
 */
internal fun deriveHeroCtaColors(rawColor: Color, isLight: Boolean): Pair<Color, Color> {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(rawColor.toArgb(), hsv)
    val isNeutral = hsv[1] < 0.08f
    val isWarmYellowHue = !isNeutral && hsv[0] in 38f..78f
    if (!isNeutral) {
        hsv[1] = (hsv[1] * 0.78f).coerceIn(
            if (isWarmYellowHue) 0.30f else 0.24f,
            0.52f,
        )
    }
    hsv[2] = when {
        isWarmYellowHue -> hsv[2].coerceIn(0.36f, 0.54f)
        isLight -> hsv[2].coerceIn(0.34f, 0.62f)
        else -> hsv[2].coerceIn(0.40f, 0.66f)
    }
    var fill = Color(AndroidColor.HSVToColor(hsv))
    val darkInk = Color(0xFF14151B)
    var step = 0
    while (contrastRatio(Color.White, fill) < 4.5f && step < 7) {
        fill = lerpColor(fill, darkInk, 0.14f)
        step++
    }
    return fill to Color.White
}

