package com.squartor.reader.ui.reader

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.os.SystemClock
import android.text.TextPaint
import android.util.Base64
import android.util.LruCache
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.BookBookmark
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.GlassClarityMode
import com.squartor.reader.model.ReaderBackgroundId
import com.squartor.reader.model.ReaderPalette
import com.squartor.reader.model.ReadingFlowMode
import com.squartor.reader.model.ReadingStyle
import com.squartor.reader.model.readerPalettes
import com.squartor.reader.model.resolveThemeReaderPalette
import com.squartor.reader.ui.glass.Ios27GlassToggle
import com.squartor.reader.ui.glass.Ios27GlassVariant
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.ios27ContrastBorderColor
import com.squartor.reader.ui.glass.ios27GlassSurface
import com.squartor.reader.ui.glass.rememberSafeGlassEffectState
import com.squartor.reader.ui.glass.vitroFingerSpotlight
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffectContainer
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassEffectState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private data class PagerWindowSlot(
    val chapterIndex: Int,
    val pageIndexInChapter: Int,
    val pageCountInChapter: Int,
    val page: ComposedPage?,
    val isPlaceholder: Boolean = false,
)

private data class PagerWindowState(
    val centerChapterIndex: Int,
    val prevCount: Int,
    val currCount: Int,
    val nextCount: Int,
    val prevPlaceholder: Boolean,
    val currPlaceholder: Boolean,
    val nextPlaceholder: Boolean,
    val initialWindowIndex: Int,
    val slots: List<PagerWindowSlot>,
) {
    fun hasSameWindowTopology(other: PagerWindowState): Boolean {
        return centerChapterIndex == other.centerChapterIndex &&
            prevCount == other.prevCount &&
            currCount == other.currCount &&
            nextCount == other.nextCount &&
            prevPlaceholder == other.prevPlaceholder &&
            currPlaceholder == other.currPlaceholder &&
            nextPlaceholder == other.nextPlaceholder &&
            slots.size == other.slots.size
    }
}

private sealed interface ScrollStreamItem {
    val key: String
    val chapterIndex: Int
    val pageIndexInChapter: Int
    val pageCountInChapter: Int

    data class ChapterDivider(
        override val chapterIndex: Int,
        val chapterTitle: String,
        override val pageCountInChapter: Int,
    ) : ScrollStreamItem {
        override val key: String = "scroll-div-$chapterIndex"
        override val pageIndexInChapter: Int = 0
    }

    data class PageBlock(
        override val chapterIndex: Int,
        override val pageIndexInChapter: Int,
        override val pageCountInChapter: Int,
        val page: ComposedPage,
        val naturalHeightPx: Int,
    ) : ScrollStreamItem {
        override val key: String = "scroll-ch-$chapterIndex-p-$pageIndexInChapter"
    }
}

private enum class ReaderOverlayState {
    None,
    Chrome,
    Toc,
    Settings,
}

private data class ReaderImageViewerState(
    val src: String,
    val sourceBounds: Rect?,
)

private data class ReaderGlassColors(
    val dark: Boolean,
    val panelSurface: Color,
    val innerCard: Color,
    val pill: Color,
    val pillSelected: Color,
    val text: Color,
    val muted: Color,
    val subtle: Color,
    val line: Color,
    val scrim: Color,
) {
    companion object {
        fun from(appPalette: AppPalette, readerPaper: ReaderPalette): ReaderGlassColors {
            val isDark = readerPaper.background.luminance() < 0.5f
            return if (isDark) {
                ReaderGlassColors(
                    dark = true,
                    panelSurface = Color(0xFF1A1B22).copy(alpha = 0.72f),
                    innerCard = Color.White.copy(alpha = 0.075f),
                    pill = Color.White.copy(alpha = 0.11f),
                    pillSelected = Color.White.copy(alpha = 0.20f),
                    text = Color(0xFFF5F5F7),
                    muted = Color(0xFFAEAEB6),
                    subtle = Color(0xFF83838D),
                    line = Color.White.copy(alpha = 0.14f),
                    scrim = Color.Black.copy(alpha = 0.28f),
                )
            } else {
                ReaderGlassColors(
                    dark = false,
                    panelSurface = Color(0xFFF8F8FC).copy(alpha = 0.72f),
                    innerCard = Color.White.copy(alpha = 0.36f),
                    pill = Color.Black.copy(alpha = 0.055f),
                    pillSelected = Color.White.copy(alpha = 0.86f),
                    text = Color(0xFF1D1D1F),
                    muted = Color(0xFF5C5C64),
                    subtle = Color(0xFF8C8C94),
                    line = Color.Black.copy(alpha = 0.09f),
                    scrim = Color.Black.copy(alpha = 0.14f),
                )
            }
        }
    }
}

private object ReaderImageCache {
    private val bitmapCache = LruCache<String, ImageBitmap>(10)
    private val bytesCache = LruCache<String, ByteArray>(8)
    private val inFlightDecodes = ConcurrentHashMap<String, CompletableDeferred<ImageBitmap?>>()

    fun getBitmap(src: String): ImageBitmap? = bitmapCache.get(src)
    fun putBitmap(src: String, bmp: ImageBitmap) {
        bitmapCache.put(src, bmp)
    }

    fun getBytes(src: String): ByteArray? = bytesCache.get(src)
    fun putBytes(src: String, bytes: ByteArray) {
        bytesCache.put(src, bytes)
    }

    fun getOrRegisterInFlight(
        src: String,
        candidate: CompletableDeferred<ImageBitmap?>,
    ): CompletableDeferred<ImageBitmap?>? = inFlightDecodes.putIfAbsent(src, candidate)

    fun finishInFlight(
        src: String,
        candidate: CompletableDeferred<ImageBitmap?>,
        result: ImageBitmap?,
    ) {
        inFlightDecodes.remove(src, candidate)
        if (!candidate.isCompleted) {
            candidate.complete(result)
        }
    }
}

internal data class ReaderPaginationMetrics(
    val readerPaper: ReaderPalette,
    val effectiveTopPadding: Dp,
    val horizontalMarginPx: Int,
    val topMarginPx: Int,
    val bottomMarginPx: Int,
    val contentWidthPx: Int,
    val contentHeightPx: Int,
    val textArgb: Int,
    val signature: String,
)

internal fun resolveEffectiveReaderPalette(
    style: ReadingStyle,
    palette: AppPalette,
): ReaderPalette {
    return if (style.readerBackground == ReaderBackgroundId.theme) {
        resolveThemeReaderPalette(palette)
    } else {
        val base = style.readerPalette
        if (style.autoNightPaper && !palette.isLight &&
            (base.id == ReaderBackgroundId.paper || base.id == ReaderBackgroundId.green)
        ) {
            readerPalettes[ReaderBackgroundId.warm]!!
        } else {
            base
        }
    }
}

internal fun computeReaderPaginationMetrics(
    bookId: String,
    style: ReadingStyle,
    palette: AppPalette,
    screenWidthPx: Int,
    screenHeightPx: Int,
    density: Float,
    safeTopDp: Dp,
    safeBottomDp: Dp,
): ReaderPaginationMetrics {
    val readerPaper = resolveEffectiveReaderPalette(style, palette)
    val effectiveTopPadding = maxOf(style.verticalMargin.dp, safeTopDp + 54.dp)
    val horizontalMarginPx = (style.pageMargin * density).roundToInt()
    val topMarginPx = (effectiveTopPadding.value * density).roundToInt()
    val bottomMarginPx = (maxOf(style.verticalMargin.dp, maxOf(48.dp, safeBottomDp + 28.dp)).value * density).roundToInt()
    val contentWidthPx = (screenWidthPx - horizontalMarginPx * 2).coerceAtLeast(120)
    val contentHeightPx = (screenHeightPx - topMarginPx - bottomMarginPx).coerceAtLeast(180)
    val textArgb = readerPaper.text.toArgb()
    val signature = ReaderPreloadCache.buildPaginationSignature(
        bookId = bookId,
        style = style,
        topMarginPx = topMarginPx,
        contentWidthPx = contentWidthPx,
        contentHeightPx = contentHeightPx,
        textArgb = textArgb,
    )
    return ReaderPaginationMetrics(
        readerPaper = readerPaper,
        effectiveTopPadding = effectiveTopPadding,
        horizontalMarginPx = horizontalMarginPx,
        topMarginPx = topMarginPx,
        bottomMarginPx = bottomMarginPx,
        contentWidthPx = contentWidthPx,
        contentHeightPx = contentHeightPx,
        textArgb = textArgb,
        signature = signature,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
    book: BookEntry,
    style: ReadingStyle,
    palette: AppPalette,
    loadChapterBlocks: suspend (BookEntry, Int) -> List<ReaderBlockItem>,
    loadImageBytes: suspend (String) -> ByteArray?,
    onProgressChanged: (chapterIndex: Int, page: Int, pageCount: Int, progress: Double) -> Unit,
    onUpdateStyle: (ReadingStyle) -> Unit,
    onToggleBookmark: (BookBookmark) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onSaveImageToGallery: (ByteArray, String) -> Unit,
    onBack: () -> Unit,
    immersiveReady: Boolean = true,
    manageSystemBars: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    // Enter sticky immersive mode after hero settle so statusBar insets animation never stutters Book Open Hero,
    // and restore status bars as soon as exit begins rather than colliding with Shelf/Dock settle on dispose.
    val view = LocalView.current
    var didHideSystemBars by remember(view) { mutableStateOf(false) }
    LaunchedEffect(view, immersiveReady, manageSystemBars) {
        if (!manageSystemBars) return@LaunchedEffect
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (immersiveReady && !didHideSystemBars) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
            didHideSystemBars = true
        } else if (!immersiveReady && didHideSystemBars) {
            controller.show(WindowInsetsCompat.Type.statusBars())
            didHideSystemBars = false
        }
    }
    DisposableEffect(view, manageSystemBars) {
        onDispose {
            if (manageSystemBars && didHideSystemBars) {
                val window = (view.context as? Activity)?.window
                val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null
                controller?.show(WindowInsetsCompat.Type.statusBars())
                didHideSystemBars = false
            }
        }
    }

    val readerPaper = remember(style.readerBackground, style.autoNightPaper, palette) {
        resolveEffectiveReaderPalette(style, palette)
    }

    val isPaperDark = remember(readerPaper) { readerPaper.background.luminance() < 0.5f }
    val glass = remember(palette, readerPaper) {
        ReaderGlassColors.from(palette, readerPaper)
    }
    val effectiveGlassPalette = remember(palette, style, isPaperDark) {
        if (palette.isLight == !isPaperDark) palette else style.resolvePalette(isLight = !isPaperDark)
    }
    val readerBackdrop = rememberGlassBackdrop()

    val chapterCount = book.chapters.size.coerceAtLeast(1)
    var currentChapterIndex by rememberSaveable(book.id) {
        mutableIntStateOf(book.currentChapterIndex.coerceIn(0, chapterCount - 1))
    }
    var pendingTargetPage by rememberSaveable(book.id) {
        mutableIntStateOf(book.currentPage.coerceAtLeast(0))
    }
    var anchorChapterIndex by rememberSaveable(book.id) {
        mutableIntStateOf(book.currentChapterIndex.coerceIn(0, chapterCount - 1))
    }
    var anchorCharOffset by rememberSaveable(book.id) {
        mutableIntStateOf(-1)
    }
    var pendingReflowTarget by remember(book.id) { mutableStateOf<Pair<Int, Int>?>(null) }
    var previousJumpPosition by remember(book.id) { mutableStateOf<Pair<Int, Int>?>(null) }

    var externalJumpEpoch by remember(book.id) { mutableIntStateOf(1) }
    var handledJumpEpoch by remember(book.id) { mutableIntStateOf(0) }
    var handledScrollJumpEpoch by remember(book.id) { mutableIntStateOf(0) }

    fun requestExternalJump(targetChapter: Int, targetPage: Int) {
        val clamped = targetChapter.coerceIn(0, chapterCount - 1)
        currentChapterIndex = clamped
        pendingTargetPage = targetPage.coerceAtLeast(0)
        anchorChapterIndex = clamped
        anchorCharOffset = -1
        pendingReflowTarget = null
        externalJumpEpoch += 1
    }

    var overlayState by remember { mutableStateOf(ReaderOverlayState.None) }
    var tocShowsBookmarks by remember { mutableStateOf(false) }
    var bookmarkPulseSeq by remember { mutableIntStateOf(0) }
    var activeImageViewer by remember { mutableStateOf<ReaderImageViewerState?>(null) }
    var isImageViewerClosing by remember { mutableStateOf(false) }
    val readerRootCoordsHolder = remember { arrayOfNulls<LayoutCoordinates>(1) }

    fun openImageViewer(src: String, sourceBounds: Rect?) {
        if (overlayState != ReaderOverlayState.None) {
            overlayState = ReaderOverlayState.None
        }
        isImageViewerClosing = false
        activeImageViewer = ReaderImageViewerState(src = src, sourceBounds = sourceBounds)
    }

    val chapterBlocksMap = remember(book.id) {
        mutableStateMapOf<Int, List<ReaderBlockItem>>().apply {
            ReaderPreloadCache.getBlocks(book.id, currentChapterIndex)?.let { put(currentChapterIndex, it) }
        }
    }
    val chapterImageVersions = remember(book.id) { mutableStateMapOf<Int, Int>() }

    fun notifyImageDecoded(src: String) {
        chapterBlocksMap.forEach { (chIdx, blocks) ->
            val hasImg = blocks.any { b ->
                b.imageSrc == src || (b.kind.equals("image", ignoreCase = true) && b.text == src)
            }
            if (hasImg) {
                chapterImageVersions[chIdx] = (chapterImageVersions[chIdx] ?: 0) + 1
            }
        }
    }

    // Preload current chapter and adjacent chapters + prune distant chapters to bound memory
    LaunchedEffect(book.id, currentChapterIndex) {
        val distantBlockKeys = chapterBlocksMap.keys.filter { abs(it - currentChapterIndex) > 5 }
        distantBlockKeys.forEach { chapterBlocksMap.remove(it) }

        if (!chapterBlocksMap.containsKey(currentChapterIndex)) {
            val loaded = ReaderPreloadCache.getBlocks(book.id, currentChapterIndex)
                ?: withContext(Dispatchers.IO) {
                    loadChapterBlocks(book, currentChapterIndex)
                }.also {
                    ReaderPreloadCache.putBlocks(book.id, currentChapterIndex, it)
                }
            chapterBlocksMap[currentChapterIndex] = loaded
        }
        val adjIndices = listOf(currentChapterIndex + 1, currentChapterIndex - 1)
            .filter { it in 0 until chapterCount && !chapterBlocksMap.containsKey(it) }
        if (adjIndices.isNotEmpty()) {
            launch(Dispatchers.IO) {
                delay(900L)
                for (adj in adjIndices) {
                    val adjBlocks = ReaderPreloadCache.getBlocks(book.id, adj)
                        ?: loadChapterBlocks(book, adj).also {
                            ReaderPreloadCache.putBlocks(book.id, adj, it)
                        }
                    withContext(Dispatchers.Main) {
                        chapterBlocksMap[adj] = adjBlocks
                    }
                }
            }
        }
    }

    // Bottom status bar clock (HH:mm)
    var clockText by remember {
        mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    LaunchedEffect(Unit) {
        while (true) {
            clockText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            delay(15_000L)
        }
    }

    BackHandler(enabled = activeImageViewer != null || overlayState != ReaderOverlayState.None) {
        when {
            activeImageViewer != null -> isImageViewerClosing = true
            overlayState == ReaderOverlayState.Toc || overlayState == ReaderOverlayState.Settings -> {
                overlayState = ReaderOverlayState.Chrome
            }
            overlayState == ReaderOverlayState.Chrome -> {
                overlayState = ReaderOverlayState.None
            }
            else -> onBack()
        }
    }

    val currentChapterTitle = book.chapters.getOrNull(currentChapterIndex)?.title ?: book.title
    val currentBlocks = chapterBlocksMap[currentChapterIndex] ?: emptyList()

    // Pre-decode image blocks in the current chapter so aspect ratios and cover pages are ready immediately
    LaunchedEffect(currentBlocks) {
        val imageSrcs = currentBlocks.mapNotNull { b ->
            b.imageSrc?.takeIf { it.isNotBlank() } ?: if (b.kind.equals("image", ignoreCase = true)) b.text else null
        }
        if (imageSrcs.isNotEmpty()) {
            // Defer off-page pre-decoding past the Hero open and rapid Double-Back window (~700ms)
            // and cap eager chapter decodes so rapid book switching never triggers big-heap GC spikes.
            delay(700L)
            val newlyDecoded = ArrayList<String>()
            withContext(Dispatchers.IO) {
                for (src in imageSrcs.take(2)) {
                    ensureActive()
                    if (ReaderImageCache.getBitmap(src) == null) {
                        val decoded = decodeReaderImageBitmap(src, loadImageBytes)
                        if (decoded != null) newlyDecoded.add(src)
                    }
                }
            }
            newlyDecoded.forEach { src -> notifyImageDecoded(src) }
        }
    }

    val statusBarTop = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val safeTop = maxOf(statusBarTop, cutoutTop)
    val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val safeBottom = maxOf(navBarBottom, 12.dp)
    val effectiveTopPadding = maxOf(style.verticalMargin.dp, safeTop + 54.dp)
    val mainDockBottomDp = (navBarBottom.value * 0.65f + 16f).coerceIn(24f, 32f).dp
    // Align bottom edge identically with Main Dock on gesture nav while preserving clearance above 3-button bars
    val dockBottomDp = if (navBarBottom > 34.dp) navBarBottom + 10.dp else mainDockBottomDp

    val readerWindowSize = LocalWindowInfo.current.containerSize
    var readerBoxWidthPx by remember { mutableIntStateOf(readerWindowSize.width.coerceAtLeast(1)) }
    var readerBoxHeightPx by remember { mutableIntStateOf(readerWindowSize.height.coerceAtLeast(1)) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerPaper.background)
            .onSizeChanged { sz ->
                if (sz.width > 0 && readerBoxWidthPx != sz.width) {
                    readerBoxWidthPx = sz.width
                }
                if (sz.height > 0 && readerBoxHeightPx != sz.height) {
                    readerBoxHeightPx = sz.height
                }
            }
            .onGloballyPositioned { readerRootCoordsHolder[0] = it },
    ) {
        val rootDensity = LocalDensity.current
        val maxWidth = with(rootDensity) { readerBoxWidthPx.toDp() }
        val maxHeight = with(rootDensity) { readerBoxHeightPx.toDp() }
        val screenWidthPx = readerBoxWidthPx
        val screenHeightPx = readerBoxHeightPx
        val horizontalMarginPx = (style.pageMargin * density).roundToInt()
        val topMarginPx = with(LocalDensity.current) { effectiveTopPadding.toPx().roundToInt() }
        val bottomMarginPx = with(LocalDensity.current) {
            maxOf(style.verticalMargin.dp, maxOf(48.dp, safeBottom + 28.dp)).toPx().roundToInt()
        }

        val contentWidthPx = (screenWidthPx - horizontalMarginPx * 2).coerceAtLeast(120)
        val contentHeightPx = (screenHeightPx - topMarginPx - bottomMarginPx).coerceAtLeast(180)
        val textArgb = readerPaper.text.toArgb()
        val paragraphSpacingPx = (style.paragraphSpacing.coerceIn(0f, 30f) * density).roundToInt()
        val bodyPaint = remember(style, density, textArgb) {
            NativeStaticLayoutPager.createBodyPaint(context, style, density, textArgb)
        }
        val titlePaint = remember(style, density, textArgb) {
            NativeStaticLayoutPager.createTitlePaint(context, style, density, textArgb)
        }

        val paginationSignature = remember(
            book.id,
            style.fontSize,
            style.fontWeightValue,
            style.lineHeight,
            style.paragraphSpacing,
            style.letterSpacing,
            style.pageMargin,
            style.verticalMargin,
            topMarginPx,
            style.firstLineIndent,
            style.dimJapaneseText,
            style.fontPath,
            contentWidthPx,
            contentHeightPx,
            textArgb,
        ) {
            ReaderPreloadCache.buildPaginationSignature(
                bookId = book.id,
                style = style,
                topMarginPx = topMarginPx,
                contentWidthPx = contentWidthPx,
                contentHeightPx = contentHeightPx,
                textArgb = textArgb,
            )
        }

        // Persist paginated pages across style/image reflows so visible pages never flash to spinner
        val initialChapterImgVer = chapterImageVersions[currentChapterIndex] ?: 0
        val initialTargetSignature = "$paginationSignature#$initialChapterImgVer"
        val chapterPagesMap = remember(book.id) {
            mutableStateMapOf<Int, List<ComposedPage>>().apply {
                ReaderPreloadCache.getPages(book.id, currentChapterIndex, initialTargetSignature)?.let {
                    put(currentChapterIndex, it)
                }
            }
        }
        val chapterPaginatedSigMap = remember(book.id) {
            mutableStateMapOf<Int, String>().apply {
                if (chapterPagesMap.containsKey(currentChapterIndex)) {
                    put(currentChapterIndex, initialTargetSignature)
                }
            }
        }
        val currentChapterImgVer = chapterImageVersions[currentChapterIndex] ?: 0
        val currentTargetSignature = "$paginationSignature#$currentChapterImgVer"
        val currentBlocksLoaded = chapterBlocksMap.containsKey(currentChapterIndex)

        val prevChapterImgVer = chapterImageVersions[currentChapterIndex - 1] ?: 0
        val nextChapterImgVer = chapterImageVersions[currentChapterIndex + 1] ?: 0

        LaunchedEffect(
            book.id,
            currentChapterIndex,
            currentBlocksLoaded,
            currentBlocks,
            currentTargetSignature,
            prevChapterImgVer,
            nextChapterImgVer,
        ) {
            val distantPageKeys = chapterPagesMap.keys.filter { abs(it - currentChapterIndex) > 4 }
            distantPageKeys.forEach { key ->
                chapterPagesMap.remove(key)
                chapterPaginatedSigMap.remove(key)
            }

            if (currentBlocksLoaded && chapterPaginatedSigMap[currentChapterIndex] != currentTargetSignature) {
                val hadPagesBefore = chapterPagesMap.containsKey(currentChapterIndex)
                val isReflow = hadPagesBefore && anchorChapterIndex == currentChapterIndex && anchorCharOffset >= 0
                val pages = ReaderPreloadCache.getPages(book.id, currentChapterIndex, currentTargetSignature)
                    ?: withContext(Dispatchers.Default) {
                        NativeStaticLayoutPager.paginateChapter(
                            context = context,
                            chapterIndex = currentChapterIndex,
                            chapterTitle = currentChapterTitle,
                            blocks = currentBlocks,
                            style = style,
                            contentWidthPx = contentWidthPx,
                            contentHeightPx = contentHeightPx,
                            density = density,
                            textColorArgb = textArgb,
                        )
                    }.also { computed ->
                        if (currentChapterImgVer == 0) {
                            ReaderPreloadCache.putPages(book.id, currentChapterIndex, currentTargetSignature, computed)
                        }
                    }
                if (isReflow) {
                    val anchoredPage = NativeStaticLayoutPager.findPageIndexForCharOffset(pages, anchorCharOffset)
                    pendingTargetPage = anchoredPage
                    pendingReflowTarget = currentChapterIndex to anchoredPage
                }
                chapterPagesMap[currentChapterIndex] = pages
                chapterPaginatedSigMap[currentChapterIndex] = currentTargetSignature
            }

            val uncachedAdj = listOf(currentChapterIndex + 1, currentChapterIndex - 1).filter { adj ->
                adj in 0 until chapterCount &&
                    chapterPaginatedSigMap[adj] != "$paginationSignature#${chapterImageVersions[adj] ?: 0}"
            }
            if (uncachedAdj.isNotEmpty()) {
                launch(Dispatchers.Default) {
                    // 错峰让出 Hero 打开动画与当前章首帧预算，再串行预排下一章与上一章
                    delay(260L)
                    for (adj in uncachedAdj) {
                        val adjImgVer = chapterImageVersions[adj] ?: 0
                        val adjTargetSig = "$paginationSignature#$adjImgVer"
                        val existingBlocks = withContext(Dispatchers.Main) { chapterBlocksMap[adj] }
                        val adjBlocks = existingBlocks ?: withContext(Dispatchers.IO) {
                            loadChapterBlocks(book, adj)
                        }.also { loaded ->
                            withContext(Dispatchers.Main) {
                                chapterBlocksMap[adj] = loaded
                            }
                        }
                        val adjTitle = book.chapters.getOrNull(adj)?.title ?: book.title
                        val adjPages = NativeStaticLayoutPager.paginateChapter(
                            context = context,
                            chapterIndex = adj,
                            chapterTitle = adjTitle,
                            blocks = adjBlocks,
                            style = style,
                            contentWidthPx = contentWidthPx,
                            contentHeightPx = contentHeightPx,
                            density = density,
                            textColorArgb = textArgb,
                        )
                        withContext(Dispatchers.Main) {
                            chapterPagesMap[adj] = adjPages
                            chapterPaginatedSigMap[adj] = adjTargetSig
                        }
                    }
                }
            }
        }

        val composedPages = chapterPagesMap[currentChapterIndex] ?: emptyList()
        val prevChapterPages = if (currentChapterIndex > 0) chapterPagesMap[currentChapterIndex - 1] else null
        val nextChapterPages = if (currentChapterIndex < chapterCount - 1) chapterPagesMap[currentChapterIndex + 1] else null

        fun buildPagerWindow(centerChapter: Int, targetPageInCenter: Int): PagerWindowState {
            val currPages = chapterPagesMap[centerChapter]
            val prevLoaded = if (centerChapter > 0) chapterPagesMap[centerChapter - 1] else null
            val nextLoaded = if (centerChapter < chapterCount - 1) chapterPagesMap[centerChapter + 1] else null

            val slots = ArrayList<PagerWindowSlot>()
            var prevCount = 0
            var prevPlaceholder = false
            if (centerChapter > 0) {
                if (!prevLoaded.isNullOrEmpty()) {
                    prevLoaded.forEachIndexed { idx, p ->
                        slots.add(PagerWindowSlot(centerChapter - 1, idx, prevLoaded.size, p, isPlaceholder = false))
                    }
                    prevCount = prevLoaded.size
                } else {
                    // Always reserve a placeholder slot when centerChapter > 0 so backward swipe never deadlocks
                    slots.add(PagerWindowSlot(centerChapter - 1, 99999, 1, null, isPlaceholder = true))
                    prevCount = 1
                    prevPlaceholder = true
                }
            }

            var currCount = 0
            var currPlaceholder = false
            val clampedInCenter: Int
            if (!currPages.isNullOrEmpty()) {
                currPages.forEachIndexed { idx, p ->
                    slots.add(PagerWindowSlot(centerChapter, idx, currPages.size, p, isPlaceholder = false))
                }
                currCount = currPages.size
                clampedInCenter = when {
                    targetPageInCenter >= 99999 -> currPages.size - 1
                    else -> targetPageInCenter.coerceIn(0, currPages.size - 1)
                }
            } else {
                slots.add(PagerWindowSlot(centerChapter, targetPageInCenter.coerceAtLeast(0), 1, null, isPlaceholder = true))
                currCount = 1
                currPlaceholder = true
                clampedInCenter = 0
            }

            var nextCount = 0
            var nextPlaceholder = false
            if (centerChapter < chapterCount - 1) {
                if (!nextLoaded.isNullOrEmpty()) {
                    nextLoaded.forEachIndexed { idx, p ->
                        slots.add(PagerWindowSlot(centerChapter + 1, idx, nextLoaded.size, p, isPlaceholder = false))
                    }
                    nextCount = nextLoaded.size
                } else {
                    slots.add(PagerWindowSlot(centerChapter + 1, 0, 1, null, isPlaceholder = true))
                    nextCount = 1
                    nextPlaceholder = true
                }
            }

            val initialWindowIdx = (prevCount + clampedInCenter).coerceIn(0, (slots.size - 1).coerceAtLeast(0))
            return PagerWindowState(
                centerChapterIndex = centerChapter,
                prevCount = prevCount,
                currCount = currCount,
                nextCount = nextCount,
                prevPlaceholder = prevPlaceholder,
                currPlaceholder = currPlaceholder,
                nextPlaceholder = nextPlaceholder,
                initialWindowIndex = initialWindowIdx,
                slots = slots,
            )
        }

        // Decouple PagerState from layout/window rebuilds so PagerState is NEVER destroyed mid-book
        var pagerWindowState by remember(book.id) {
            mutableStateOf(buildPagerWindow(currentChapterIndex, pendingTargetPage))
        }
        val windowStateRef = remember(book.id) { mutableStateOf(pagerWindowState) }
        windowStateRef.value = pagerWindowState

        val pagerState = remember(book.id) {
            PagerState(
                currentPage = windowStateRef.value.initialWindowIndex,
                currentPageOffsetFraction = 0f,
                pageCount = { windowStateRef.value.slots.size.coerceAtLeast(1) },
            )
        }
        val scrollListState = rememberLazyListState()

        // Single unified state machine for Paged mode window synchronization (eliminates race conditions & page-0 bounce)
        LaunchedEffect(
            currentChapterIndex,
            pendingTargetPage,
            externalJumpEpoch,
            pendingReflowTarget,
            composedPages,
            prevChapterPages,
            nextChapterPages,
            pagerState.currentPage,
            pagerState.isScrollInProgress,
            style.readingFlow,
        ) {
            if (!style.readingFlow.isPaged || pagerState.isScrollInProgress) return@LaunchedEffect

            // 1. Explicit external jump (TOC, Bookmark, Seekbar, initial open, mode switch)
            if (handledJumpEpoch != externalJumpEpoch) {
                val nextWindow = buildPagerWindow(currentChapterIndex, pendingTargetPage)
                pagerWindowState = nextWindow
                windowStateRef.value = nextWindow
                if (pagerState.currentPage != nextWindow.initialWindowIndex ||
                    abs(pagerState.currentPageOffsetFraction) > 0.001f
                ) {
                    pagerState.scrollToPage(nextWindow.initialWindowIndex, 0f)
                }
                if (!nextWindow.currPlaceholder) {
                    handledJumpEpoch = externalJumpEpoch
                    pendingReflowTarget = null
                    val resolvedSlot = nextWindow.slots.getOrNull(nextWindow.initialWindowIndex)
                    if (resolvedSlot != null) {
                        pendingTargetPage = resolvedSlot.pageIndexInChapter
                    }
                }
                return@LaunchedEffect
            }

            // 2. Style/image reflow completed for current chapter: snap straight to anchored page
            val reflowTarget = pendingReflowTarget
            if (reflowTarget != null && reflowTarget.first == currentChapterIndex && composedPages.isNotEmpty()) {
                val targetPage = reflowTarget.second.coerceIn(0, composedPages.size - 1)
                val nextWindow = buildPagerWindow(currentChapterIndex, targetPage)
                pagerWindowState = nextWindow
                windowStateRef.value = nextWindow
                if (pagerState.currentPage != nextWindow.initialWindowIndex ||
                    abs(pagerState.currentPageOffsetFraction) > 0.001f
                ) {
                    pagerState.scrollToPage(nextWindow.initialWindowIndex, 0f)
                }
                val resolvedSlot = nextWindow.slots.getOrNull(nextWindow.initialWindowIndex)
                if (resolvedSlot != null && !resolvedSlot.isPlaceholder) {
                    pendingTargetPage = resolvedSlot.pageIndexInChapter
                }
                pendingReflowTarget = null
                return@LaunchedEffect
            }

            val currentSlot = pagerWindowState.slots.getOrNull(pagerState.currentPage)

            // 3. User swiped across chapter boundary and settled
            if (currentSlot != null && currentSlot.chapterIndex != pagerWindowState.centerChapterIndex) {
                val newCenter = currentSlot.chapterIndex.coerceIn(0, chapterCount - 1)
                val targetPage = currentSlot.pageIndexInChapter
                currentChapterIndex = newCenter
                pendingTargetPage = targetPage
                val nextWindow = buildPagerWindow(newCenter, targetPage)
                pagerWindowState = nextWindow
                windowStateRef.value = nextWindow
                pagerState.scrollToPage(nextWindow.initialWindowIndex, 0f)
                if (nextWindow.currPlaceholder) {
                    externalJumpEpoch += 1
                } else {
                    val resolvedSlot = nextWindow.slots.getOrNull(nextWindow.initialWindowIndex)
                    if (resolvedSlot != null) {
                        pendingTargetPage = resolvedSlot.pageIndexInChapter
                    }
                }
                return@LaunchedEffect
            }

            // 4. Same center chapter: update window if curr/prev/next finished paginating or style resized pages
            val targetInCurr = when {
                currentSlot == null || currentSlot.isPlaceholder -> pendingTargetPage
                composedPages.isNotEmpty() && currentSlot.pageCountInChapter != composedPages.size -> {
                    if (anchorChapterIndex == currentChapterIndex && anchorCharOffset >= 0) {
                        NativeStaticLayoutPager.findPageIndexForCharOffset(composedPages, anchorCharOffset)
                    } else {
                        val ratio = if (currentSlot.pageCountInChapter <= 1) {
                            0f
                        } else {
                            currentSlot.pageIndexInChapter.toFloat() / (currentSlot.pageCountInChapter - 1).toFloat()
                        }
                        (ratio * (composedPages.size - 1).coerceAtLeast(0)).roundToInt()
                    }
                }
                else -> currentSlot.pageIndexInChapter
            }

            val candidateWindow = buildPagerWindow(currentChapterIndex, targetInCurr)
            if (!candidateWindow.hasSameWindowTopology(pagerWindowState) ||
                candidateWindow.slots !== pagerWindowState.slots &&
                candidateWindow.slots.getOrNull(candidateWindow.initialWindowIndex)?.page !==
                pagerWindowState.slots.getOrNull(pagerState.currentPage)?.page
            ) {
                pagerWindowState = candidateWindow
                windowStateRef.value = candidateWindow
                if (pagerState.currentPage != candidateWindow.initialWindowIndex ||
                    abs(pagerState.currentPageOffsetFraction) > 0.001f
                ) {
                    pagerState.scrollToPage(candidateWindow.initialWindowIndex, 0f)
                }
                val resolvedSlot = candidateWindow.slots.getOrNull(candidateWindow.initialWindowIndex)
                if (resolvedSlot != null && !resolvedSlot.isPlaceholder) {
                    pendingTargetPage = resolvedSlot.pageIndexInChapter
                }
            } else if (currentSlot != null && !currentSlot.isPlaceholder) {
                pendingTargetPage = currentSlot.pageIndexInChapter
            }
        }

        // Continuous 3-chapter stream for ReadingFlowMode.scroll ([center-1, center, center+1])
        val scrollStreamItems = remember(
            currentChapterIndex,
            composedPages,
            prevChapterPages,
            nextChapterPages,
            contentHeightPx,
            paragraphSpacingPx,
            book.chapters,
        ) {
            val out = ArrayList<ScrollStreamItem>()
            fun appendChapterToStream(chIdx: Int, pages: List<ComposedPage>) {
                if (pages.isEmpty()) return
                if (chIdx > 0) {
                    val chTitle = book.chapters.getOrNull(chIdx)?.title ?: "第 ${chIdx + 1} 章"
                    out.add(
                        ScrollStreamItem.ChapterDivider(
                            chapterIndex = chIdx,
                            chapterTitle = chTitle,
                            pageCountInChapter = pages.size,
                        )
                    )
                }
                pages.forEachIndexed { pIdx, p ->
                    val maxBottom = p.elements.maxOfOrNull { it.yOffset + it.height } ?: contentHeightPx.toFloat()
                    val naturalPx = (maxBottom.roundToInt() + paragraphSpacingPx)
                        .coerceIn((64f * density).roundToInt(), contentHeightPx)
                    out.add(
                        ScrollStreamItem.PageBlock(
                            chapterIndex = chIdx,
                            pageIndexInChapter = pIdx,
                            pageCountInChapter = pages.size,
                            page = p,
                            naturalHeightPx = naturalPx,
                        )
                    )
                }
            }
            if (currentChapterIndex > 0 && !prevChapterPages.isNullOrEmpty() && composedPages.isNotEmpty()) {
                appendChapterToStream(currentChapterIndex - 1, prevChapterPages)
            }
            if (composedPages.isNotEmpty()) {
                appendChapterToStream(currentChapterIndex, composedPages)
            }
            if (currentChapterIndex < chapterCount - 1 && !nextChapterPages.isNullOrEmpty() && composedPages.isNotEmpty()) {
                appendChapterToStream(currentChapterIndex + 1, nextChapterPages)
            }
            out
        }

        var scrollAnchorKey by remember(book.id) { mutableStateOf<String?>(null) }
        var scrollAnchorOffset by remember(book.id) { mutableIntStateOf(0) }
        var appliedScrollStream by remember(book.id) { mutableStateOf<List<ScrollStreamItem>>(emptyList()) }

        // Maintain seamless scroll position across 3-chapter stream updates & handle explicit jumps / reflow in Scroll mode
        LaunchedEffect(
            style.readingFlow,
            scrollStreamItems,
            externalJumpEpoch,
            pendingReflowTarget,
            scrollListState.isScrollInProgress,
        ) {
            if (style.readingFlow.isPaged || scrollStreamItems.isEmpty()) return@LaunchedEffect

            if (handledScrollJumpEpoch != externalJumpEpoch) {
                val currCount = composedPages.size
                if (currCount > 0) {
                    val resolvedPage = if (pendingTargetPage >= 99999) {
                        currCount - 1
                    } else {
                        pendingTargetPage.coerceIn(0, currCount - 1)
                    }
                    val targetKey = "scroll-ch-$currentChapterIndex-p-$resolvedPage"
                    val targetIdx = scrollStreamItems.indexOfFirst { it.key == targetKey }
                    if (targetIdx >= 0) {
                        scrollListState.scrollToItem(targetIdx, 0)
                        scrollAnchorKey = targetKey
                        scrollAnchorOffset = 0
                        pendingTargetPage = resolvedPage
                        handledScrollJumpEpoch = externalJumpEpoch
                        pendingReflowTarget = null
                        appliedScrollStream = scrollStreamItems
                    }
                }
                return@LaunchedEffect
            }

            val reflowTarget = pendingReflowTarget
            if (reflowTarget != null && reflowTarget.first == currentChapterIndex && composedPages.isNotEmpty()) {
                val resolvedPage = reflowTarget.second.coerceIn(0, composedPages.size - 1)
                val targetKey = "scroll-ch-$currentChapterIndex-p-$resolvedPage"
                val targetIdx = scrollStreamItems.indexOfFirst { it.key == targetKey }
                if (targetIdx >= 0) {
                    scrollListState.scrollToItem(targetIdx, 0)
                    scrollAnchorKey = targetKey
                    scrollAnchorOffset = 0
                    pendingTargetPage = resolvedPage
                    pendingReflowTarget = null
                    appliedScrollStream = scrollStreamItems
                    return@LaunchedEffect
                }
            }

            // Compensate scroll index if prevChapter prepended items while idle (before scroll observer runs)
            if (appliedScrollStream !== scrollStreamItems && !scrollListState.isScrollInProgress) {
                val savedKey = scrollAnchorKey
                if (savedKey != null) {
                    val visibleKey = scrollStreamItems.getOrNull(scrollListState.firstVisibleItemIndex)?.key
                    if (visibleKey != savedKey) {
                        val newIdx = scrollStreamItems.indexOfFirst { it.key == savedKey }
                        if (newIdx >= 0) {
                            scrollListState.scrollToItem(newIdx, scrollAnchorOffset)
                        }
                    }
                }
                appliedScrollStream = scrollStreamItems
            }

            // Re-center 3-chapter scroll stream when user scrolls into prev/next chapter and settles
            if (!scrollListState.isScrollInProgress && appliedScrollStream === scrollStreamItems) {
                val visibleItem = scrollStreamItems.getOrNull(scrollListState.firstVisibleItemIndex)
                if (visibleItem != null) {
                    scrollAnchorKey = visibleItem.key
                    scrollAnchorOffset = scrollListState.firstVisibleItemScrollOffset
                    pendingTargetPage = visibleItem.pageIndexInChapter
                    if (visibleItem.chapterIndex != currentChapterIndex) {
                        currentChapterIndex = visibleItem.chapterIndex
                    }
                }
            }
        }

        // Keep anchor updated live during settled user scroll so prepends never hijack savedKey
        val visibleScrollItem by remember(scrollStreamItems) {
            derivedStateOf {
                scrollStreamItems.getOrNull(scrollListState.firstVisibleItemIndex)
            }
        }
        LaunchedEffect(scrollListState.firstVisibleItemIndex, scrollListState.firstVisibleItemScrollOffset) {
            if (!style.readingFlow.isPaged &&
                appliedScrollStream === scrollStreamItems &&
                handledScrollJumpEpoch == externalJumpEpoch &&
                pendingReflowTarget == null
            ) {
                val item = scrollStreamItems.getOrNull(scrollListState.firstVisibleItemIndex)
                if (item != null) {
                    scrollAnchorKey = item.key
                    scrollAnchorOffset = scrollListState.firstVisibleItemScrollOffset
                }
            }
        }

        // Sync target position cleanly when switching ReadingFlowMode (cover / slide / curl / scroll)
        var previousFlowIsPaged by remember { mutableStateOf(style.readingFlow.isPaged) }
        LaunchedEffect(style.readingFlow) {
            if (style.readingFlow.isPaged && abs(pagerState.currentPageOffsetFraction) > 0.001f) {
                pagerState.scrollToPage(pagerState.currentPage, 0f)
            }
            if (previousFlowIsPaged != style.readingFlow.isPaged) {
                previousFlowIsPaged = style.readingFlow.isPaged
                val slot = pagerWindowState.slots.getOrNull(pagerState.currentPage)
                val activeCh = if (!style.readingFlow.isPaged) {
                    slot?.chapterIndex ?: currentChapterIndex
                } else {
                    visibleScrollItem?.chapterIndex ?: currentChapterIndex
                }.coerceIn(0, chapterCount - 1)
                val chPages = chapterPagesMap[activeCh]
                val activePage = if (!chPages.isNullOrEmpty() && anchorChapterIndex == activeCh && anchorCharOffset >= 0) {
                    NativeStaticLayoutPager.findPageIndexForCharOffset(chPages, anchorCharOffset)
                } else if (!style.readingFlow.isPaged) {
                    slot?.takeIf { !it.isPlaceholder }?.pageIndexInChapter ?: pendingTargetPage
                } else {
                    visibleScrollItem?.pageIndexInChapter ?: pendingTargetPage
                }
                currentChapterIndex = activeCh
                pendingTargetPage = activePage.coerceAtLeast(0)
                externalJumpEpoch += 1
            }
        }

        val activePagerSlot = pagerWindowState.slots.getOrNull(pagerState.currentPage)
        val displayChapterIndex = if (style.readingFlow.isPaged) {
            activePagerSlot?.chapterIndex ?: currentChapterIndex
        } else {
            visibleScrollItem?.chapterIndex ?: currentChapterIndex
        }
        val pageCount = if (style.readingFlow.isPaged) {
            if (activePagerSlot != null && !activePagerSlot.isPlaceholder) {
                activePagerSlot.pageCountInChapter.coerceAtLeast(1)
            } else {
                composedPages.size.coerceAtLeast(1)
            }
        } else {
            (visibleScrollItem?.pageCountInChapter ?: composedPages.size).coerceAtLeast(1)
        }
        val currentPageIndex = if (style.readingFlow.isPaged) {
            val rawPage = if (activePagerSlot != null && !activePagerSlot.isPlaceholder) {
                activePagerSlot.pageIndexInChapter
            } else {
                pendingTargetPage
            }
            rawPage.coerceIn(0, pageCount - 1)
        } else {
            (visibleScrollItem?.pageIndexInChapter ?: pendingTargetPage).coerceIn(0, pageCount - 1)
        }

        val displayChapterTitle = book.chapters.getOrNull(displayChapterIndex)?.title ?: book.title

        val isScrollAtBookEnd by remember(style.readingFlow.isPaged, chapterCount, scrollStreamItems) {
            derivedStateOf {
                !style.readingFlow.isPaged &&
                    chapterCount > 0 &&
                    scrollStreamItems.isNotEmpty() &&
                    scrollStreamItems.lastOrNull()?.chapterIndex == chapterCount - 1 &&
                    !scrollListState.canScrollForward
            }
        }

        val overallProgress = remember(
            displayChapterIndex,
            currentPageIndex,
            pageCount,
            chapterCount,
            isScrollAtBookEnd,
        ) {
            computeOverallReaderProgress(
                displayChapterIndex = displayChapterIndex,
                currentPageIndex = currentPageIndex,
                pageCount = pageCount,
                chapterCount = chapterCount,
                isScrollAtBookEnd = isScrollAtBookEnd,
            )
        }

        // Capture true reading character offset whenever settled and layout matches current style
        LaunchedEffect(
            displayChapterIndex,
            currentPageIndex,
            composedPages,
            pagerState.isScrollInProgress,
            scrollListState.isScrollInProgress,
            handledJumpEpoch,
            handledScrollJumpEpoch,
            externalJumpEpoch,
            pendingReflowTarget,
            currentTargetSignature,
            style.readingFlow.isPaged,
        ) {
            val settled = if (style.readingFlow.isPaged) {
                !pagerState.isScrollInProgress && handledJumpEpoch == externalJumpEpoch
            } else {
                !scrollListState.isScrollInProgress && handledScrollJumpEpoch == externalJumpEpoch
            }
            val upToDate = chapterPaginatedSigMap[displayChapterIndex]?.startsWith(paginationSignature) == true
            if (settled && upToDate && pendingReflowTarget == null) {
                val pageObj = if (style.readingFlow.isPaged) {
                    activePagerSlot?.takeIf { !it.isPlaceholder }?.page
                } else {
                    (visibleScrollItem as? ScrollStreamItem.PageBlock)?.page
                } ?: chapterPagesMap[displayChapterIndex]?.getOrNull(currentPageIndex)
                if (pageObj != null) {
                    anchorChapterIndex = displayChapterIndex
                    anchorCharOffset = pageObj.startCharOffset
                }
            }
        }

        // Report reading progress in real time
        LaunchedEffect(displayChapterIndex, currentPageIndex, pageCount, composedPages.size, overallProgress) {
            if (composedPages.isNotEmpty()) {
                onProgressChanged(displayChapterIndex, currentPageIndex, pageCount, overallProgress)
            }
        }

        var programmaticTurnCount by remember(book.id) { mutableIntStateOf(0) }
        var activeTurnTargetSlot by remember(book.id) { mutableIntStateOf(-1) }

        fun goToPreviousPageOrChapter() {
            if (style.readingFlow.isPaged) {
                val baseSlot = if (programmaticTurnCount > 0 && activeTurnTargetSlot >= 0) {
                    activeTurnTargetSlot
                } else {
                    pagerState.currentPage
                }
                val prevSlot = baseSlot - 1
                if (prevSlot >= 0) {
                    activeTurnTargetSlot = prevSlot
                    scope.launch {
                        programmaticTurnCount += 1
                        try {
                            pagerState.animateScrollToPage(
                                page = prevSlot,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 460f,
                                ),
                            )
                        } finally {
                            programmaticTurnCount = (programmaticTurnCount - 1).coerceAtLeast(0)
                            if (programmaticTurnCount == 0) activeTurnTargetSlot = -1
                        }
                    }
                } else if (currentChapterIndex > 0) {
                    requestExternalJump(currentChapterIndex - 1, 99999)
                }
            } else {
                scope.launch {
                    val stepPx = contentHeightPx * 0.84f
                    if (scrollListState.canScrollBackward) {
                        programmaticTurnCount += 1
                        try {
                            scrollListState.animateScrollBy(
                                value = -stepPx,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 420f,
                                ),
                            )
                        } finally {
                            programmaticTurnCount = (programmaticTurnCount - 1).coerceAtLeast(0)
                        }
                    } else if (currentChapterIndex > 0) {
                        requestExternalJump(currentChapterIndex - 1, 99999)
                    }
                }
            }
        }

        fun goToNextPageOrChapter() {
            if (style.readingFlow.isPaged) {
                val totalWindowSlots = pagerWindowState.slots.size
                val baseSlot = if (programmaticTurnCount > 0 && activeTurnTargetSlot >= 0) {
                    activeTurnTargetSlot
                } else {
                    pagerState.currentPage
                }
                val nextSlot = baseSlot + 1
                if (nextSlot < totalWindowSlots) {
                    activeTurnTargetSlot = nextSlot
                    scope.launch {
                        programmaticTurnCount += 1
                        try {
                            pagerState.animateScrollToPage(
                                page = nextSlot,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 460f,
                                ),
                            )
                        } finally {
                            programmaticTurnCount = (programmaticTurnCount - 1).coerceAtLeast(0)
                            if (programmaticTurnCount == 0) activeTurnTargetSlot = -1
                        }
                    }
                } else if (currentChapterIndex < chapterCount - 1) {
                    requestExternalJump(currentChapterIndex + 1, 0)
                }
            } else {
                scope.launch {
                    val stepPx = contentHeightPx * 0.84f
                    if (scrollListState.canScrollForward) {
                        programmaticTurnCount += 1
                        try {
                            scrollListState.animateScrollBy(
                                value = stepPx,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 420f,
                                ),
                            )
                        } finally {
                            programmaticTurnCount = (programmaticTurnCount - 1).coerceAtLeast(0)
                        }
                    } else if (currentChapterIndex < chapterCount - 1) {
                        requestExternalJump(currentChapterIndex + 1, 0)
                    }
                }
            }
        }

        fun toggleCurrentPageBookmark() {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            bookmarkPulseSeq += 1
            val currentPageObj = if (style.readingFlow.isPaged) {
                activePagerSlot?.page ?: composedPages.getOrNull(currentPageIndex)
            } else {
                (visibleScrollItem as? ScrollStreamItem.PageBlock)?.page ?: composedPages.getOrNull(currentPageIndex)
            }
            val existingBookmark = book.bookmarks.firstOrNull {
                it.chapterIndex == displayChapterIndex && it.page == currentPageIndex
            }
            if (existingBookmark != null) {
                onRemoveBookmark(existingBookmark.id)
            } else {
                val bm = BookBookmark(
                    id = "${book.id}_${displayChapterIndex}_$currentPageIndex",
                    chapterIndex = displayChapterIndex,
                    page = currentPageIndex,
                    pageCount = pageCount,
                    progress = overallProgress,
                    createdAtMillis = System.currentTimeMillis(),
                    snippet = currentPageObj?.plainSnippet ?: displayChapterTitle,
                )
                onToggleBookmark(bm)
            }
        }

        val isSideOverlayOpen = overlayState == ReaderOverlayState.Toc || overlayState == ReaderOverlayState.Settings

        // Track whether scroll list or pager was mid-fling at touch down so stopping a scroll doesn't pop Chrome
        var wasScrollingOnDown by remember { mutableStateOf(false) }

        // Keep a single persistent readerBackdrop bound to the live reader canvas (unless Power Save disables blur)
        // so rapidly toggling 正文 <-> 设置 <-> 目录 <-> 正文 never detaches BackdropNode.coordinates.

        val latestReverseTapPageTurn by rememberUpdatedState(style.reverseTapPageTurn)

        fun dispatchReaderTapLocalX(localX: Float) {
            if (activeImageViewer != null) return
            if (overlayState != ReaderOverlayState.None) {
                overlayState = ReaderOverlayState.None
                return
            }
            if (wasScrollingOnDown) {
                wasScrollingOnDown = false
                return
            }
            val w = screenWidthPx.toFloat().coerceAtLeast(1f)
            val ratio = (localX / w).coerceIn(0f, 1f)
            when {
                ratio < 0.30f -> {
                    if (latestReverseTapPageTurn) goToNextPageOrChapter()
                    else goToPreviousPageOrChapter()
                }
                ratio > 0.70f -> {
                    if (latestReverseTapPageTurn) goToPreviousPageOrChapter()
                    else goToNextPageOrChapter()
                }
                else -> {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    overlayState = ReaderOverlayState.Chrome
                }
            }
        }

        fun dispatchReaderTapRootX(rootX: Float) {
            val rootLeft = readerRootCoordsHolder[0]?.takeIf { it.isAttached }?.positionInRoot()?.x ?: 0f
            dispatchReaderTapLocalX(rootX - rootLeft)
        }

        // Update via SideEffect + holder arrays so Kotlin FunctionReference.equals() never pins stale closures
        val tapLocalXHolder = remember { arrayOf<(Float) -> Unit>({}) }
        val tapRootXHolder = remember { arrayOf<(Float) -> Unit>({}) }
        val stableTapRootX = remember<(Float) -> Unit> { { rootX -> tapRootXHolder[0](rootX) } }
        SideEffect {
            tapLocalXHolder[0] = ::dispatchReaderTapLocalX
            tapRootXHolder[0] = ::dispatchReaderTapRootX
        }

        val powerSaveActive = LocalPowerSaveMode.current
        var readerBackdropArmed by remember(book.id) {
            mutableStateOf(overlayState != ReaderOverlayState.None)
        }
        if (overlayState != ReaderOverlayState.None && !readerBackdropArmed) {
            readerBackdropArmed = true
        }
        val activeReaderBackdrop = if (!powerSaveActive && readerBackdropArmed) readerBackdrop else null

        CompositionLocalProvider(
            LocalGlassDarkTheme provides isPaperDark,
            LocalVitroBackdrop provides activeReaderBackdrop,
            LocalReadingStyle provides style,
        ) {
        // 1. Reader Canvas + Footer Layer (attaches persistent readerBackdrop once Chrome/overlay opens so initial reader launch & quick double-back avoid fullscreen blur overhead)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (activeReaderBackdrop != null) {
                        Modifier
                            .glassBackdrop(activeReaderBackdrop)
                            .background(readerPaper.background)
                    } else {
                        Modifier.background(readerPaper.background)
                    },
                )
                .pointerInput(style.readingFlow) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        wasScrollingOnDown = if (style.readingFlow.isPaged) {
                            pagerState.isScrollInProgress && programmaticTurnCount == 0
                        } else {
                            scrollListState.isScrollInProgress && programmaticTurnCount == 0
                        }
                    }
                }
                .pointerInput(style.readingFlow, style.reverseTapPageTurn) {
                    detectTapGestures(
                        onTap = { offset -> tapLocalXHolder[0](offset.x) },
                    )
                },
        ) {
            val pagesReady = composedPages.isNotEmpty()
            var showLoadingSpinner by remember(book.id, currentChapterIndex) { mutableStateOf(false) }
            LaunchedEffect(pagesReady, book.id, currentChapterIndex) {
                if (pagesReady) {
                    showLoadingSpinner = false
                } else {
                    delay(360L)
                    showLoadingSpinner = true
                }
            }
            val initiallyReady = remember(book.id) { pagesReady }
            val contentRevealAlphaState = remember(book.id) {
                Animatable(if (initiallyReady) 1f else 0f)
            }
            LaunchedEffect(pagesReady) {
                if (pagesReady && contentRevealAlphaState.value < 0.999f) {
                    contentRevealAlphaState.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
                    )
                } else if (!pagesReady && contentRevealAlphaState.value != 0f) {
                    contentRevealAlphaState.snapTo(0f)
                }
            }
            val contentRevealAlpha = contentRevealAlphaState.value

            if (!pagesReady) {
                if (showLoadingSpinner) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = readerPaper.text.copy(alpha = 0.7f), strokeWidth = 2.4.dp)
                    }
                }
            } else {
                var neighborPrewarmReady by remember(book.id) { mutableStateOf(false) }
                LaunchedEffect(book.id, immersiveReady) {
                    if (!neighborPrewarmReady) {
                        delay(if (immersiveReady) 120L else 280L)
                        neighborPrewarmReady = true
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (contentRevealAlpha < 0.999f) {
                                Modifier.graphicsLayer {
                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                    alpha = contentRevealAlpha
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                if (style.readingFlow.isPaged) {
                val isDarkPaper = readerPaper.background.luminance() < 0.5f
                val coverShadowBrush = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.26f),
                            Color.Black.copy(alpha = 0.09f),
                            Color.Transparent,
                        ),
                        startX = 0f,
                        endX = 1f,
                    )
                }
                val slideSeamBrush = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.10f),
                        ),
                        startX = 0f,
                        endX = 1f,
                    )
                }
                val curlUnderShadowBrush = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        startX = 0f,
                        endX = 1f,
                    )
                }
                val curlFrontShadowBrush = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.16f),
                        ),
                        startX = 0f,
                        endX = 1f,
                    )
                }
                val curlBackfaceBrush = remember(isDarkPaper) {
                    val highlightColor = if (isDarkPaper) {
                        Color.White.copy(alpha = 0.10f)
                    } else {
                        Color.White.copy(alpha = 0.52f)
                    }
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.18f),
                        0.28f to highlightColor,
                        0.72f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.22f),
                        startX = 0f,
                        endX = 1f,
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = if (neighborPrewarmReady) 1 else 0,
                    userScrollEnabled = overlayState == ReaderOverlayState.None,
                    modifier = Modifier.fillMaxSize(),
                ) { windowIdx ->
                    val slot = pagerWindowState.slots.getOrNull(windowIdx)
                    val page = slot?.page
                    // 将翻页偏移读取完全推迟至 graphicsLayer / drawWithContent 阶段，翻页全程零重组、零画刷分配
                    val staticPageZ = (10000 - windowIdx).toFloat()

                    val pageTurnModifier = when (style.readingFlow) {
                        ReadingFlowMode.cover -> {
                            Modifier
                                .zIndex(staticPageZ)
                                .graphicsLayer {
                                    val pageOffset = ((pagerState.currentPage - windowIdx) + pagerState.currentPageOffsetFraction)
                                        .coerceIn(-1f, 1f)
                                    // Pure translation avoids fractional scale changes on vector text layers,
                                    // eliminating per-frame glyph atlas re-rasterization while preserving cover depth shadow.
                                    translationX = if (pageOffset < 0f) pageOffset * size.width else 0f
                                    scaleX = 1f
                                    scaleY = 1f
                                }
                                .background(readerPaper.background)
                        }

                        ReadingFlowMode.slide -> Modifier.background(readerPaper.background)

                        ReadingFlowMode.curl -> {
                            Modifier
                                .zIndex(staticPageZ)
                                .graphicsLayer {
                                    val pageOffset = ((pagerState.currentPage - windowIdx) + pagerState.currentPageOffsetFraction)
                                        .coerceIn(-1f, 1f)
                                    translationX = pageOffset * size.width
                                }
                                .drawWithContent {
                                    val pageOffset = ((pagerState.currentPage - windowIdx) + pagerState.currentPageOffsetFraction)
                                        .coerceIn(-1f, 1f)
                                    if (pageOffset <= 0.002f) {
                                        drawRect(color = readerPaper.background)
                                        drawContent()
                                    } else if (pageOffset < 0.998f) {
                                        val p = pageOffset.coerceIn(0f, 1f)
                                        val sinP = sin(p * PI).toFloat()
                                        val foldX = (size.width * (1f - p)).coerceIn(0f, size.width)
                                        val curlWidth = (size.width * 0.25f * sinP)
                                            .coerceAtLeast(18f * density)
                                            .coerceAtMost(foldX)
                                        val backLeft = (foldX - curlWidth).coerceAtLeast(0f)

                                        if (foldX > 0.5f) {
                                            clipRect(left = 0f, top = 0f, right = foldX, bottom = size.height) {
                                                drawRect(color = readerPaper.background)
                                                this@drawWithContent.drawContent()
                                            }
                                        }

                                        val underShadowW = (46f * density * sinP).coerceAtLeast(14f * density)
                                        val shadowRight = (foldX + underShadowW).coerceAtMost(size.width)
                                        if (shadowRight > foldX) {
                                            val underRatio = ((shadowRight - foldX) / underShadowW).coerceIn(0f, 1f)
                                            withTransform({
                                                translate(left = foldX, top = 0f)
                                                scale(scaleX = underShadowW, scaleY = 1f, pivot = Offset.Zero)
                                            }) {
                                                drawRect(
                                                    brush = curlUnderShadowBrush,
                                                    topLeft = Offset.Zero,
                                                    size = Size(underRatio, size.height),
                                                    alpha = (1f - p * 0.35f).coerceIn(0f, 1f),
                                                )
                                            }
                                        }

                                        val frontShadowW = (24f * density * sinP).coerceAtLeast(8f * density)
                                        val frontShadowLeft = (backLeft - frontShadowW).coerceAtLeast(0f)
                                        if (backLeft > frontShadowLeft) {
                                            val frontRatio = ((backLeft - frontShadowLeft) / frontShadowW).coerceIn(0f, 1f)
                                            withTransform({
                                                translate(left = frontShadowLeft, top = 0f)
                                                scale(scaleX = frontShadowW, scaleY = 1f, pivot = Offset.Zero)
                                            }) {
                                                drawRect(
                                                    brush = curlFrontShadowBrush,
                                                    topLeft = Offset.Zero,
                                                    size = Size(frontRatio, size.height),
                                                    alpha = sinP.coerceIn(0f, 1f),
                                                )
                                            }
                                        }

                                        if (foldX > backLeft + 1f) {
                                            clipRect(left = backLeft, top = 0f, right = foldX, bottom = size.height) {
                                                drawRect(color = readerPaper.background)
                                                scale(
                                                    scaleX = -1f,
                                                    scaleY = 1f,
                                                    pivot = Offset((backLeft + foldX) * 0.5f, size.height * 0.5f),
                                                ) {
                                                    this@drawWithContent.drawContent()
                                                }
                                                drawRect(
                                                    color = readerPaper.background.copy(alpha = 0.86f),
                                                )
                                                val span = (foldX - backLeft).coerceAtLeast(1f)
                                                withTransform({
                                                    translate(left = backLeft, top = 0f)
                                                    scale(scaleX = span, scaleY = 1f, pivot = Offset.Zero)
                                                }) {
                                                    drawRect(
                                                        brush = curlBackfaceBrush,
                                                        topLeft = Offset.Zero,
                                                        size = Size(1f, size.height),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        ReadingFlowMode.scroll -> Modifier.background(readerPaper.background)
                    }

                    val pageOverlayModifier = when (style.readingFlow) {
                        ReadingFlowMode.cover -> Modifier.drawBehind {
                            val pageOffset = ((pagerState.currentPage - windowIdx) + pagerState.currentPageOffsetFraction)
                                .coerceIn(-1f, 1f)
                            if (pageOffset < -0.002f && pageOffset > -0.998f) {
                                val turnProgress = (1f + pageOffset).coerceIn(0f, 1f)
                                val upperRightX = (size.width * (-pageOffset)).coerceIn(0f, size.width)
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.13f * (-pageOffset)),
                                )
                                val shadowW = (34f * density).coerceAtLeast(16f)
                                val shadowEnd = (upperRightX + shadowW).coerceAtMost(size.width)
                                if (shadowEnd > upperRightX) {
                                    val fadeFactor = (1f - turnProgress * 0.25f).coerceIn(0.2f, 1f)
                                    val visibleRatio = ((shadowEnd - upperRightX) / shadowW).coerceIn(0f, 1f)
                                    withTransform({
                                        translate(left = upperRightX, top = 0f)
                                        scale(scaleX = shadowW, scaleY = 1f, pivot = Offset.Zero)
                                    }) {
                                        drawRect(
                                            brush = coverShadowBrush,
                                            topLeft = Offset.Zero,
                                            size = Size(visibleRatio, size.height),
                                            alpha = fadeFactor,
                                        )
                                    }
                                }
                            } else if (pageOffset > 0.002f && pageOffset < 0.998f) {
                                val edgeW = (1.2f * density).coerceAtLeast(1f)
                                drawRect(
                                    color = readerPaper.text.copy(alpha = 0.12f),
                                    topLeft = Offset((size.width - edgeW).coerceAtLeast(0f), 0f),
                                    size = Size(edgeW, size.height),
                                )
                            }
                        }

                        ReadingFlowMode.slide -> Modifier.drawBehind {
                            val pageOffset = ((pagerState.currentPage - windowIdx) + pagerState.currentPageOffsetFraction)
                                .coerceIn(-1f, 1f)
                            if (pageOffset > 0.002f && pageOffset < 0.998f) {
                                val seamW = (10f * density).coerceAtLeast(4f)
                                val seamLeft = (size.width - seamW).coerceAtLeast(0f)
                                withTransform({
                                    translate(left = seamLeft, top = 0f)
                                    scale(scaleX = seamW, scaleY = 1f, pivot = Offset.Zero)
                                }) {
                                    drawRect(
                                        brush = slideSeamBrush,
                                        topLeft = Offset.Zero,
                                        size = Size(1f, size.height),
                                    )
                                }
                            }
                        }

                        ReadingFlowMode.curl, ReadingFlowMode.scroll -> null
                    }

                    Box(modifier = pageTurnModifier.fillMaxSize()) {
                        if (page != null) {
                            StaticLayoutPageCanvas(
                                page = page,
                                style = style,
                                bodyPaint = bodyPaint,
                                titlePaint = titlePaint,
                                mutedColor = readerPaper.muted,
                                horizontalMarginPx = horizontalMarginPx,
                                topMarginPx = topMarginPx,
                                contentWidthPx = contentWidthPx,
                                loadImageBytes = loadImageBytes,
                                onImageDecoded = ::notifyImageDecoded,
                                hiddenImageSrc = activeImageViewer?.src,
                                onImageTapRootX = stableTapRootX,
                                onImageLongPress = ::openImageViewer,
                                cacheTextLayer = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                CircularProgressIndicator(
                                    color = readerPaper.muted.copy(alpha = 0.45f),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        if (pageOverlayModifier != null) {
                            Box(modifier = Modifier.fillMaxSize().then(pageOverlayModifier))
                        }
                    }
                }
            } else {
                // 滚动模式 (Seamless 3-chapter continuous Vertical Scroll stream like iOS Books)
                LazyColumn(
                    state = scrollListState,
                    userScrollEnabled = overlayState == ReaderOverlayState.None,
                    contentPadding = PaddingValues(
                        start = style.pageMargin.dp,
                        top = effectiveTopPadding,
                        end = style.pageMargin.dp,
                        bottom = safeBottom + 76.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = scrollStreamItems,
                        key = { it.key },
                        contentType = { item ->
                            when (item) {
                                is ScrollStreamItem.ChapterDivider -> "divider"
                                is ScrollStreamItem.PageBlock -> "page"
                            }
                        },
                    ) { item ->
                        when (item) {
                            is ScrollStreamItem.ChapterDivider -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 28.dp, bottom = 20.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(0.6.dp)
                                            .background(readerPaper.text.copy(alpha = 0.12f)),
                                    )
                                    Text(
                                        text = "第 ${item.chapterIndex + 1} 章",
                                        color = readerPaper.muted.copy(alpha = 0.68f),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(0.6.dp)
                                            .background(readerPaper.text.copy(alpha = 0.12f)),
                                    )
                                }
                            }

                            is ScrollStreamItem.PageBlock -> {
                                StaticLayoutPageCanvas(
                                    page = item.page,
                                    style = style,
                                    bodyPaint = bodyPaint,
                                    titlePaint = titlePaint,
                                    mutedColor = readerPaper.muted,
                                    horizontalMarginPx = 0,
                                    topMarginPx = 0,
                                    contentWidthPx = contentWidthPx,
                                    loadImageBytes = loadImageBytes,
                                    onImageDecoded = ::notifyImageDecoded,
                                    hiddenImageSrc = activeImageViewer?.src,
                                    onImageTapRootX = stableTapRootX,
                                    onImageLongPress = ::openImageViewer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(with(LocalDensity.current) { item.naturalHeightPx.toDp() }),
                                )
                            }
                        }
                    }

                    val lastStreamChapter = scrollStreamItems.lastOrNull()?.chapterIndex ?: currentChapterIndex
                    if (lastStreamChapter >= chapterCount - 1) {
                        item(key = "book-scroll-end-marker") {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                            ) {
                                Text(
                                    text = "— 全书完 —",
                                    color = readerPaper.muted.copy(alpha = 0.65f),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
            }
            }

            // 2. Minimalist Apple Books-inspired bottom typographic margin footer
            var holdFooterForSheetClose by remember { mutableStateOf(false) }
            LaunchedEffect(isSideOverlayOpen) {
                if (isSideOverlayOpen) {
                    holdFooterForSheetClose = true
                } else if (holdFooterForSheetClose) {
                    delay(190L)
                    holdFooterForSheetClose = false
                }
            }
            val footerAlphaState = animateFloatAsState(
                targetValue = if (overlayState == ReaderOverlayState.None && !holdFooterForSheetClose) 1f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "footerAlpha",
            )
            val footerVisible by remember {
                derivedStateOf { footerAlphaState.value > 0.01f }
            }
            val scrollFooterBackdrop = !style.readingFlow.isPaged
            val scrollFooterBgBrush = remember(scrollFooterBackdrop, readerPaper.background) {
                if (scrollFooterBackdrop) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            readerPaper.background.copy(alpha = 0.88f),
                            readerPaper.background,
                        ),
                    )
                } else {
                    null
                }
            }
            if (footerVisible) {
                val isBookmarked = book.bookmarks.any {
                    it.chapterIndex == displayChapterIndex && it.page == currentPageIndex
                }
                ReaderPaperFooter(
                    clockText = clockText,
                    isPaperDark = isPaperDark,
                    currentPage = currentPageIndex + 1,
                    pageCount = pageCount.coerceAtLeast(1),
                    currentChapter = displayChapterIndex + 1,
                    chapterCount = chapterCount.coerceAtLeast(1),
                    isBookmarked = isBookmarked,
                    readerPaper = readerPaper,
                    accentColor = effectiveGlassPalette.accentText,
                    horizontalPadding = maxOf(style.pageMargin.dp, 20.dp),
                    bottomPadding = maxOf(navBarBottom, 10.dp),
                    topFadePadding = if (scrollFooterBackdrop) 12.dp else 0.dp,
                    scrollFadeBrush = scrollFooterBgBrush,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { alpha = footerAlphaState.value },
                )
            }
        }

        // 3. Transparent tap-to-dismiss catcher when TOC or Settings half-screen panel is open
        // (No dark/blur scrim over upper half so live reader page stays 100% sharp during typography adjustments)
        if (isSideOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        overlayState = ReaderOverlayState.Chrome
                    },
            )
        }

        // 4. Apple Books 27.2 Top Corner Glass Orbs (cutout-safe, unclipped expand room)
        AnimatedVisibility(
            visible = overlayState == ReaderOverlayState.Chrome,
            enter = slideInVertically(
                initialOffsetY = { -it * 2 },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { -it * 2 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f),
            ) + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            val hasBookmarkHere = book.bookmarks.any {
                it.chapterIndex == displayChapterIndex && it.page == currentPageIndex
            }
            var bookmarkScaleTarget by remember { mutableFloatStateOf(1f) }
            LaunchedEffect(bookmarkPulseSeq) {
                if (bookmarkPulseSeq > 0) {
                    bookmarkScaleTarget = 1.24f
                    delay(110L)
                    bookmarkScaleTarget = 1f
                }
            }
            val bookmarkScale by animateFloatAsState(
                targetValue = bookmarkScaleTarget,
                animationSpec = spring(dampingRatio = 0.52f, stiffness = 520f),
                label = "bookmarkPulse",
            )
            val closeInteraction = remember { MutableInteractionSource() }
            val closeGlassState = rememberSafeGlassEffectState(interactionSource = closeInteraction, active = true)
            val bookmarkInteraction = remember { MutableInteractionSource() }
            val bookmarkGlassState = rememberSafeGlassEffectState(interactionSource = bookmarkInteraction, active = true)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = safeTop)
                    .then(
                        if (!LocalPowerSaveMode.current) {
                            Modifier.glassEffectContainer(spacing = 0.dp)
                        } else {
                            Modifier
                        },
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Top-left: 44dp Glass Close (X) orb
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .ios27GlassSurface(
                                shape = CircleShape,
                                palette = effectiveGlassPalette,
                                style = style,
                                variant = Ios27GlassVariant.ReaderControl,
                                backdrop = readerBackdrop,
                                isInteractive = true,
                                state = closeGlassState,
                                interactionSource = closeInteraction,
                                showContrastBorder = false,
                                darkPaperOverride = isPaperDark,
                                showFingerHighlight = false,
                            )
                            .clickable(
                                interactionSource = closeInteraction,
                                indication = null,
                            ) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onBack()
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "退出阅读",
                            tint = glass.text,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Top-right: 44dp Standalone Bookmark glass orb (pulse only on inner icon so Vitro rect stays synced)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .ios27GlassSurface(
                                shape = CircleShape,
                                palette = effectiveGlassPalette,
                                style = style,
                                variant = Ios27GlassVariant.ReaderControl,
                                backdrop = readerBackdrop,
                                isInteractive = true,
                                state = bookmarkGlassState,
                                interactionSource = bookmarkInteraction,
                                showContrastBorder = false,
                                darkPaperOverride = isPaperDark,
                                showFingerHighlight = true,
                            )
                            .clickable(
                                interactionSource = bookmarkInteraction,
                                indication = null,
                            ) {
                                toggleCurrentPageBookmark()
                            },
                    ) {
                        Icon(
                            imageVector = if (hasBookmarkHere) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (hasBookmarkHere) "移除书签" else "添加书签",
                            tint = if (hasBookmarkHere) effectiveGlassPalette.accentText else glass.text,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = bookmarkScale
                                    scaleY = bookmarkScale
                                },
                        )
                    }
                }
            }
        }

        // 5-7. Reader Morphing Sheet State, Bottom Main-Dock-Matched Bar, and Live Scrubbing Preview
        var tocOrbBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
        var aaPillBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
        var tocTapOriginBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
        var aaTapOriginBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

        var isScrubbingProgress by remember(book.id) { mutableStateOf(false) }
        var scrubFraction by remember(overallProgress) {
            mutableFloatStateOf(overallProgress.toFloat().coerceIn(0f, 1f))
        }

        fun resolveScrubTarget(fraction: Float): Pair<Int, Int> {
            val p = fraction.coerceIn(0f, 1f)
            if (chapterCount <= 1) {
                val targetP = (p * (pageCount - 1).coerceAtLeast(0)).roundToInt()
                return 0 to targetP
            }
            if (p >= 0.996f) {
                val lastCh = chapterCount - 1
                val lastKnown = chapterPagesMap[lastCh]
                return lastCh to ((lastKnown?.size?.minus(1))?.coerceAtLeast(0) ?: 99999)
            }
            val scaled = (p * chapterCount).coerceIn(0f, chapterCount - 0.0001f)
            val chIdx = scaled.toInt().coerceIn(0, chapterCount - 1)
            val inChFrac = (scaled - chIdx).coerceIn(0f, 1f)
            val knownCount = when (chIdx) {
                displayChapterIndex -> pageCount
                else -> chapterPagesMap[chIdx]?.size
            }
            val pageIdx = if (knownCount != null && knownCount > 0) {
                (inChFrac * (knownCount - 1).coerceAtLeast(0)).roundToInt()
            } else {
                0
            }
            return chIdx to pageIdx
        }

        LaunchedEffect(previousJumpPosition) {
            if (previousJumpPosition != null) {
                delay(5_000L)
                previousJumpPosition = null
            }
        }

        val tocTargetOpen = overlayState == ReaderOverlayState.Toc
        val settingsTargetOpen = overlayState == ReaderOverlayState.Settings

        var lastSideOverlay by remember { mutableStateOf(ReaderOverlayState.Toc) }
        val activeFrontOverlay = if (isSideOverlayOpen) overlayState else lastSideOverlay
        SideEffect {
            if (isSideOverlayOpen && lastSideOverlay != overlayState) {
                lastSideOverlay = overlayState
            }
        }

        val tocExpandedState = remember { MutableTransitionState(false) }
        tocExpandedState.targetState = tocTargetOpen
        val tocMorph = rememberReaderSheetMorphValues(tocExpandedState, labelPrefix = "readerTocSheet")
        var tocSheetVisible by remember { mutableStateOf(tocTargetOpen) }
        if (tocTargetOpen && !tocSheetVisible) {
            tocSheetVisible = true
        }
        LaunchedEffect(tocTargetOpen, tocMorph.isSettledClosed) {
            if (!tocTargetOpen && tocMorph.isSettledClosed) {
                tocSheetVisible = false
            }
        }

        val settingsExpandedState = remember { MutableTransitionState(false) }
        settingsExpandedState.targetState = settingsTargetOpen
        val settingsMorph = rememberReaderSheetMorphValues(settingsExpandedState, labelPrefix = "readerSettingsSheet")
        var settingsSheetVisible by remember { mutableStateOf(settingsTargetOpen) }
        if (settingsTargetOpen && !settingsSheetVisible) {
            settingsSheetVisible = true
        }
        LaunchedEffect(settingsTargetOpen, settingsMorph.isSettledClosed) {
            if (!settingsTargetOpen && settingsMorph.isSettledClosed) {
                settingsSheetVisible = false
            }
        }

        val anySheetVisible = tocSheetVisible || settingsSheetVisible

        LaunchedEffect(tocSheetVisible) {
            if (!tocSheetVisible) {
                tocTapOriginBoundsInRoot = null
            }
        }

        LaunchedEffect(settingsSheetVisible) {
            if (!settingsSheetVisible) {
                aaTapOriginBoundsInRoot = null
            }
        }

        val tocInteraction = remember { MutableInteractionSource() }
        val tocPressed by tocInteraction.collectIsPressedAsState()
        var tocDragging by remember { mutableStateOf(false) }

        val aaInteraction = remember { MutableInteractionSource() }
        val aaPressed by aaInteraction.collectIsPressedAsState()
        var aaDragging by remember { mutableStateOf(false) }

        val dockVisible = overlayState != ReaderOverlayState.None || anySheetVisible
        LaunchedEffect(dockVisible) {
            if (!dockVisible) {
                tocDragging = false
                aaDragging = false
            }
        }

        val isDockLight = !isPaperDark
        val dockActiveTint = if (isDockLight) {
            effectiveGlassPalette.accentText
        } else {
            lerpColor(effectiveGlassPalette.primarySoft, Color.White, 0.20f)
        }
        val dockInactiveInk = if (isDockLight) Color(0xFF44424A) else Color(0xFFE6E8F2)
        val dockInactiveSubInk = dockInactiveInk.copy(alpha = 0.78f)

        val aaAccentEngaged = settingsTargetOpen || aaPressed || aaDragging ||
            (settingsSheetVisible && !settingsMorph.isSettledClosed && settingsMorph.collapseMetric > 0.06f)
        val aaLabelColor by animateColorAsState(
            targetValue = if (aaAccentEngaged) dockActiveTint else dockInactiveInk,
            animationSpec = tween(
                durationMillis = if (aaAccentEngaged) 100 else 420,
                easing = FastOutSlowInEasing,
            ),
            label = "readerAaLabelColor",
        )
        val aaSubLabelColor by animateColorAsState(
            targetValue = if (aaAccentEngaged) dockActiveTint else dockInactiveSubInk,
            animationSpec = tween(
                durationMillis = if (aaAccentEngaged) 100 else 420,
                easing = FastOutSlowInEasing,
            ),
            label = "readerAaSubLabelColor",
        )

        val tocAccentEngaged = tocTargetOpen || tocPressed || tocDragging ||
            (tocSheetVisible && !tocMorph.isSettledClosed && tocMorph.collapseMetric > 0.06f)
        val tocIconColor by animateColorAsState(
            targetValue = if (tocAccentEngaged) dockActiveTint else dockInactiveInk,
            animationSpec = tween(
                durationMillis = if (tocAccentEngaged) 100 else 420,
                easing = FastOutSlowInEasing,
            ),
            label = "readerTocIconColor",
        )

        val aaDockReveal = when {
            !settingsSheetVisible || settingsMorph.isSettledClosed -> 1f
            settingsTargetOpen -> 0f
            else -> {
                val raw = ((0.12f - settingsMorph.collapseMetric) / 0.10f).coerceIn(0f, 1f)
                raw * raw * (3f - 2f * raw)
            }
        }
        val tocDockReveal = when {
            !tocSheetVisible || tocMorph.isSettledClosed -> 1f
            tocTargetOpen -> 0f
            else -> {
                val raw = ((0.12f - tocMorph.collapseMetric) / 0.10f).coerceIn(0f, 1f)
                raw * raw * (3f - 2f * raw)
            }
        }
        val tocOrbIconReveal = if (tocDockReveal > 0.004f) 1f else 0f
        val hideTocOrbGlass = tocSheetVisible && !tocMorph.isSettledClosed &&
            (tocTargetOpen || tocMorph.collapseMetric > 0.12f)
        val dockInteractive = overlayState != ReaderOverlayState.None

        // 5. Bottom Main-Dock-matched control cluster (carries 14dp overflow room for unclipped glass orb interactions)
        AnimatedVisibility(
            visible = dockVisible,
            enter = slideInVertically(
                initialOffsetY = { it * 2 },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it * 2 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f),
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (dockBottomDp - 14.dp).coerceAtLeast(0.dp)),
        ) {
            val activeFraction = if (isScrubbingProgress) scrubFraction else overallProgress.toFloat().coerceIn(0f, 1f)
            val (activePreviewCh, _) = if (isScrubbingProgress) {
                resolveScrubTarget(scrubFraction)
            } else {
                displayChapterIndex to currentPageIndex
            }
            val activeChapterTitle = book.chapters.getOrNull(activePreviewCh)?.title ?: displayChapterTitle

            ReaderAppleBooks272DockBar(
                style = style,
                isPaperDark = isPaperDark,
                backdrop = readerBackdrop,
                effectiveGlassPalette = effectiveGlassPalette,
                glass = glass,
                chapterTitle = activeChapterTitle,
                progress = activeFraction,
                dockInteractive = dockInteractive,
                tocTargetOpen = tocTargetOpen,
                settingsTargetOpen = settingsTargetOpen,
                hideTocOrbGlass = hideTocOrbGlass,
                tocDockReveal = tocDockReveal,
                tocOrbIconReveal = tocOrbIconReveal,
                aaDockReveal = aaDockReveal,
                aaAccentEngaged = aaAccentEngaged,
                tocIconColor = tocIconColor,
                aaLabelColor = aaLabelColor,
                aaSubLabelColor = aaSubLabelColor,
                tocInteraction = tocInteraction,
                tocPressed = tocPressed,
                tocDragging = tocDragging,
                onTocDraggingChanged = { tocDragging = it },
                aaInteraction = aaInteraction,
                aaPressed = aaPressed,
                aaDragging = aaDragging,
                onAaDraggingChanged = { aaDragging = it },
                onTocOrbBoundsChanged = { tocOrbBoundsInRoot = it },
                onAaPillBoundsChanged = { aaPillBoundsInRoot = it },
                onTapToc = { tapOrigin ->
                    if (!dockInteractive) return@ReaderAppleBooks272DockBar
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (overlayState == ReaderOverlayState.Toc) {
                        overlayState = ReaderOverlayState.Chrome
                    } else {
                        tocTapOriginBoundsInRoot = tapOrigin ?: tocOrbBoundsInRoot
                        overlayState = ReaderOverlayState.Toc
                    }
                },
                onScrubStart = {
                    if (dockInteractive) {
                        isScrubbingProgress = true
                    }
                },
                onScrubChange = { nextFraction ->
                    if (dockInteractive) {
                        scrubFraction = nextFraction.coerceIn(0f, 1f)
                    }
                },
                onScrubEnd = {
                    val (targetCh, targetPg) = resolveScrubTarget(scrubFraction)
                    isScrubbingProgress = false
                    if (targetCh != displayChapterIndex) {
                        previousJumpPosition = displayChapterIndex to currentPageIndex
                    }
                    if (targetCh != displayChapterIndex || targetPg != currentPageIndex) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        requestExternalJump(targetCh, targetPg)
                    }
                },
                onTapSettings = { tapOrigin ->
                    if (!dockInteractive) return@ReaderAppleBooks272DockBar
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (overlayState == ReaderOverlayState.Settings) {
                        overlayState = ReaderOverlayState.Chrome
                    } else {
                        aaTapOriginBoundsInRoot = tapOrigin ?: aaPillBoundsInRoot
                        overlayState = ReaderOverlayState.Settings
                    }
                },
            )
        }

        // 6. Dual independent concurrent Liquid Glass morph sheets (TOC & Settings expand/collapse strictly
        // from/to their own Dock anchors; direct cross-switch layers incoming panel above outgoing shrinking panel)
        if (anySheetVisible) {
            val themeSwatchColor = remember(palette) { resolveThemeReaderPalette(palette).background }
            val sheetBaseTint = remember(isPaperDark, style.glassClarityMode, style.glassOpacityLevel) {
                val op = style.glassOpacityLevel.coerceIn(0f, 1f)
                val baseA = when (style.glassClarityMode) {
                    GlassClarityMode.clear -> if (isPaperDark) 0.02f + op * 0.08f else 0.15f + op * 0.12f
                    GlassClarityMode.regular -> if (isPaperDark) 0.14f + op * 0.14f else 0.36f + op * 0.16f
                    GlassClarityMode.tinted -> if (isPaperDark) 0.34f + op * 0.18f else 0.58f + op * 0.18f
                }
                if (isPaperDark) {
                    Color(0xFF23252E).copy(alpha = (baseA + 0.22f).coerceIn(0.32f, 0.68f))
                } else {
                    Color(0xFFF8F9FC).copy(alpha = (baseA + 0.32f).coerceIn(0.48f, 0.86f))
                }
            }
            val dockOrbChromeTint = remember(isPaperDark, style.glassClarityMode, style.glassOpacityLevel) {
                val op = style.glassOpacityLevel.coerceIn(0f, 1f)
                if (!isPaperDark) {
                    val a = when (style.glassClarityMode) {
                        GlassClarityMode.clear -> 0.68f + op * 0.14f
                        GlassClarityMode.regular -> 0.80f + op * 0.10f
                        GlassClarityMode.tinted -> 0.87f + op * 0.07f
                    }
                    Color(0xFFECEDF3).copy(alpha = a.coerceIn(0.64f, 0.94f))
                } else {
                    val baseA = when (style.glassClarityMode) {
                        GlassClarityMode.clear -> 0.02f + op * 0.08f
                        GlassClarityMode.regular -> 0.14f + op * 0.14f
                        GlassClarityMode.tinted -> 0.34f + op * 0.18f
                    }
                    Color(0xFF2B2D38).copy(alpha = baseA.coerceIn(0.02f, 0.54f))
                }
            }
            val tocZIndex = if (activeFrontOverlay == ReaderOverlayState.Toc) 1f else 0f
            val settingsZIndex = if (activeFrontOverlay == ReaderOverlayState.Settings) 1f else 0f
            val hostMaxWidth = maxWidth
            val hostMaxHeight = maxHeight

            Box(modifier = Modifier.fillMaxSize()) {
                if (tocSheetVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(tocZIndex),
                    ) {
                        ReaderMorphingSheetHost(
                            isSettingsPanel = false,
                            targetOpen = tocTargetOpen,
                            morph = tocMorph,
                            restingOriginBoundsInRoot = tocOrbBoundsInRoot,
                            tapOriginBoundsInRoot = tocTapOriginBoundsInRoot,
                            rootCoordsHolder = readerRootCoordsHolder,
                            maxWidth = hostMaxWidth,
                            maxHeight = hostMaxHeight,
                            safeTop = safeTop,
                            dockBottomDp = dockBottomDp,
                            style = style,
                            isPaperDark = isPaperDark,
                            readerBackdrop = readerBackdrop,
                            effectiveGlassPalette = effectiveGlassPalette,
                            sheetBaseTint = sheetBaseTint,
                            dockOrbChromeTint = dockOrbChromeTint,
                            originIconColor = tocIconColor,
                            originSubLabelColor = tocIconColor,
                        ) {
                            ReaderTocFloatingPanel(
                                book = book,
                                currentChapterIndex = displayChapterIndex,
                                showBookmarks = tocShowsBookmarks,
                                isInteractive = tocTargetOpen,
                                onToggleBookmarks = {
                                    if (tocTargetOpen) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        tocShowsBookmarks = it
                                    }
                                },
                                palette = effectiveGlassPalette,
                                glass = glass,
                                onClosePanel = {
                                    if (tocTargetOpen) {
                                        overlayState = ReaderOverlayState.Chrome
                                    }
                                },
                                onSelectChapter = { idx ->
                                    if (tocTargetOpen) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        requestExternalJump(idx, 0)
                                        overlayState = ReaderOverlayState.None
                                    }
                                },
                                onSelectBookmark = { bm ->
                                    if (tocTargetOpen) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        requestExternalJump(bm.chapterIndex, bm.page)
                                        overlayState = ReaderOverlayState.None
                                    }
                                },
                                onRemoveBookmark = { bmId ->
                                    if (tocTargetOpen) {
                                        onRemoveBookmark(bmId)
                                    }
                                },
                            )
                        }
                    }
                }

                if (settingsSheetVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(settingsZIndex),
                    ) {
                        ReaderMorphingSheetHost(
                            isSettingsPanel = true,
                            targetOpen = settingsTargetOpen,
                            morph = settingsMorph,
                            restingOriginBoundsInRoot = aaPillBoundsInRoot,
                            tapOriginBoundsInRoot = aaTapOriginBoundsInRoot,
                            rootCoordsHolder = readerRootCoordsHolder,
                            maxWidth = hostMaxWidth,
                            maxHeight = hostMaxHeight,
                            safeTop = safeTop,
                            dockBottomDp = dockBottomDp,
                            style = style,
                            isPaperDark = isPaperDark,
                            readerBackdrop = readerBackdrop,
                            effectiveGlassPalette = effectiveGlassPalette,
                            sheetBaseTint = sheetBaseTint,
                            dockOrbChromeTint = dockOrbChromeTint,
                            originIconColor = aaLabelColor,
                            originSubLabelColor = aaSubLabelColor,
                        ) {
                            ReaderSettingsFloatingPanel(
                                style = style,
                                palette = effectiveGlassPalette,
                                themeSwatchColor = themeSwatchColor,
                                glass = glass,
                                isInteractive = settingsTargetOpen,
                                onClosePanel = {
                                    if (settingsTargetOpen) {
                                        overlayState = ReaderOverlayState.Chrome
                                    }
                                },
                                onUpdateStyle = { nextStyle ->
                                    if (settingsTargetOpen) {
                                        onUpdateStyle(nextStyle)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // 7. Live scrub target preview or cross-chapter jump confirmation/undo pill (rendered above Sheet & Dock)
        val previewTarget = if (isScrubbingProgress) resolveScrubTarget(scrubFraction) else null
        AnimatedVisibility(
            visible = overlayState != ReaderOverlayState.None && (isScrubbingProgress || (previousJumpPosition != null && !isSideOverlayOpen)),
            enter = fadeIn(tween(150)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(130)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp)
                .padding(bottom = dockBottomDp + 74.dp),
        ) {
            val previewShape = RoundedCornerShape(20.dp)
            if (previewTarget != null) {
                val (targetCh, targetPg) = previewTarget
                val targetChTitle = book.chapters.getOrNull(targetCh)?.title ?: "第 ${targetCh + 1} 章"
                val pct = (scrubFraction * 100f).roundToInt().coerceIn(0, 100)
                val targetTotalPages = if (targetCh == displayChapterIndex) pageCount else chapterPagesMap[targetCh]?.size
                CompositionLocalProvider(LocalVitroBackdrop provides null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .ios27GlassSurface(
                                shape = previewShape,
                                palette = effectiveGlassPalette,
                                style = style,
                                variant = Ios27GlassVariant.ReaderControl,
                                backdrop = readerBackdrop,
                                showContrastBorder = false,
                                darkPaperOverride = isPaperDark,
                                showFingerHighlight = false,
                            )
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(
                            text = "第 ${targetCh + 1}/$chapterCount 章 · $targetChTitle",
                            color = glass.text,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp),
                        )
                        Text(
                            text = if (targetTotalPages != null && targetTotalPages > 0) {
                                "${targetPg + 1}/$targetTotalPages 页 ($pct%)"
                            } else {
                                "$pct%"
                            },
                            color = effectiveGlassPalette.accentText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                        )
                    }
                }
            } else {
                previousJumpPosition?.let { (prevCh, prevPg) ->
                    val prevTitle = book.chapters.getOrNull(prevCh)?.title ?: "第 ${prevCh + 1} 章"
                    CompositionLocalProvider(LocalVitroBackdrop provides null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .ios27GlassSurface(
                                    shape = previewShape,
                                    palette = effectiveGlassPalette,
                                    style = style,
                                    variant = Ios27GlassVariant.ReaderControl,
                                    backdrop = readerBackdrop,
                                    isInteractive = true,
                                    showContrastBorder = false,
                                    darkPaperOverride = isPaperDark,
                                    showFingerHighlight = false,
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    previousJumpPosition = null
                                    requestExternalJump(prevCh, prevPg)
                                }
                                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "回到原位置",
                                tint = effectiveGlassPalette.accentText,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "回到第 ${prevCh + 1} 章 · $prevTitle",
                                color = glass.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp),
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { previousJumpPosition = null },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "收起提示",
                                    tint = glass.muted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Smooth shared-bounds fullscreen zoomable image viewer
        activeImageViewer?.let { viewerState ->
            FullscreenReaderImageViewer(
                viewerState = viewerState,
                isClosing = isImageViewerClosing,
                containerRootCoordsHolder = readerRootCoordsHolder,
                safeTop = safeTop,
                safeBottom = safeBottom,
                style = style,
                palette = effectiveGlassPalette,
                loadImageBytes = loadImageBytes,
                onSaveToGallery = onSaveImageToGallery,
                onRequestClose = { isImageViewerClosing = true },
                onDismissed = {
                    activeImageViewer = null
                    isImageViewerClosing = false
                },
            )
        }
        }
    }
}

@Composable
private fun ReaderPaperFooter(
    clockText: String,
    isPaperDark: Boolean,
    currentPage: Int,
    pageCount: Int,
    currentChapter: Int,
    chapterCount: Int,
    isBookmarked: Boolean,
    readerPaper: ReaderPalette,
    accentColor: Color,
    horizontalPadding: Dp,
    bottomPadding: Dp,
    topFadePadding: Dp,
    scrollFadeBrush: Brush?,
    modifier: Modifier = Modifier,
) {
    val inkColor = readerPaper.muted.copy(alpha = if (isPaperDark) 0.54f else 0.50f)
    val labelStyle = remember(inkColor) {
        TextStyle(
            color = inkColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.25.sp,
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (scrollFadeBrush != null) Modifier.background(scrollFadeBrush) else Modifier)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topFadePadding,
                bottom = bottomPadding,
            ),
    ) {
        Icon(
            imageVector = if (isPaperDark) Icons.Rounded.DarkMode else Icons.Rounded.WbSunny,
            contentDescription = null,
            tint = inkColor.copy(alpha = (inkColor.alpha * 0.85f).coerceIn(0f, 1f)),
            modifier = Modifier.size(10.5.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = clockText,
            style = labelStyle,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.82f),
                modifier = Modifier.size(11.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = "第 $currentChapter/$chapterCount 章  ·  $currentPage/$pageCount",
            style = labelStyle.copy(
                fontWeight = FontWeight.Normal,
                color = if (isBookmarked) accentColor.copy(alpha = 0.84f) else inkColor,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun dockRubberBand(offset: Float, limit: Float): Float {
    if (limit <= 0f) return 0f
    return (offset * limit) / (abs(offset) + limit)
}

private class ReaderSheetPanelGeometry {
    var startW by mutableFloatStateOf(0f)
    var startH by mutableFloatStateOf(0f)
    var endW by mutableFloatStateOf(0f)
    var endH by mutableFloatStateOf(0f)
    var curW by mutableFloatStateOf(0f)
    var curH by mutableFloatStateOf(0f)
    var curCornerPx by mutableFloatStateOf(0f)
}

private data class ReaderSheetMorphValues(
    val rawWidthT: Float,
    val rawHeightT: Float,
    val centerT: Float,
    val boundsT: Float,
    val collapseMetric: Float,
    val isSettledClosed: Boolean,
)

@Composable
private fun rememberReaderSheetMorphValues(
    expandedState: MutableTransitionState<Boolean>,
    labelPrefix: String,
): ReaderSheetMorphValues {
    val transition = rememberTransition(expandedState, label = labelPrefix)
    val rawWidthT by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                spring(dampingRatio = 0.75f, stiffness = 360f, visibilityThreshold = 0.001f)
            } else {
                spring(dampingRatio = 0.84f, stiffness = 720f, visibilityThreshold = 0.001f)
            }
        },
        label = "${labelPrefix}WidthT",
    ) { expanded -> if (expanded) 1f else 0f }

    val rawHeightT by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                spring(dampingRatio = 0.78f, stiffness = 275f, visibilityThreshold = 0.001f)
            } else {
                spring(dampingRatio = 0.84f, stiffness = 700f, visibilityThreshold = 0.001f)
            }
        },
        label = "${labelPrefix}HeightT",
    ) { expanded -> if (expanded) 1f else 0f }

    val centerT by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                spring(dampingRatio = 0.76f, stiffness = 620f, visibilityThreshold = 0.001f)
            } else {
                spring(dampingRatio = 0.84f, stiffness = 760f, visibilityThreshold = 0.001f)
            }
        },
        label = "${labelPrefix}CenterT",
    ) { expanded -> if (expanded) 1f else 0f }

    val boundsT = ((rawWidthT.coerceIn(0f, 1f) + rawHeightT.coerceIn(0f, 1f)) * 0.5f).coerceIn(0f, 1f)
    val collapseMetric = maxOf(boundsT, centerT.coerceIn(0f, 1f))
    val isSettledClosed = !expandedState.targetState &&
        !transition.targetState &&
        !transition.currentState &&
        !transition.isRunning &&
        collapseMetric <= 0.004f
    return ReaderSheetMorphValues(
        rawWidthT = rawWidthT,
        rawHeightT = rawHeightT,
        centerT = centerT,
        boundsT = boundsT,
        collapseMetric = collapseMetric,
        isSettledClosed = isSettledClosed,
    )
}

@Composable
private fun ReaderMorphingSheetHost(
    isSettingsPanel: Boolean,
    targetOpen: Boolean,
    morph: ReaderSheetMorphValues,
    restingOriginBoundsInRoot: Rect?,
    tapOriginBoundsInRoot: Rect?,
    rootCoordsHolder: Array<LayoutCoordinates?>,
    maxWidth: Dp,
    maxHeight: Dp,
    safeTop: Dp,
    dockBottomDp: Dp,
    style: ReadingStyle,
    isPaperDark: Boolean,
    readerBackdrop: io.github.itshedimisawi.vitro.GlassBackdrop,
    effectiveGlassPalette: AppPalette,
    sheetBaseTint: Color,
    dockOrbChromeTint: Color,
    originIconColor: Color,
    originSubLabelColor: Color,
    panelContent: @Composable () -> Unit,
) {
    val sheetPanelHeightDp = (maxHeight * 0.52f)
        .coerceIn(320.dp, 488.dp)
        .coerceAtMost((maxHeight - safeTop - dockBottomDp - 120.dp).coerceAtLeast(280.dp))
    val sheetWidthDp = minOf(352.dp, (maxWidth - 24.dp).coerceAtLeast(260.dp))
    val composeDensity = LocalDensity.current
    val sheetGeom = remember { ReaderSheetPanelGeometry() }
    val morphCornerSize = remember(sheetGeom) {
        object : CornerSize {
            override fun toPx(shapeSize: Size, density: Density): Float {
                val maxRadius = (minOf(shapeSize.width, shapeSize.height) * 0.5f).coerceAtLeast(0f)
                return sheetGeom.curCornerPx.coerceIn(0f, maxRadius)
            }
        }
    }
    val morphSheetShape = remember(morphCornerSize) {
        RoundedCornerShape(morphCornerSize)
    }
    val boundsT = morph.boundsT
    val collapseMetric = morph.collapseMetric
    val contentAlpha = if (targetOpen) {
        ((boundsT - 0.08f) / 0.30f).coerceIn(0f, 1f)
    } else {
        ((boundsT - 0.42f) / 0.34f).coerceIn(0f, 1f)
    }
    val anchorIconAlpha = if (targetOpen) {
        (1f - ((boundsT - 0.05f).coerceAtLeast(0f) / 0.19f)).coerceIn(0f, 1f)
    } else {
        // Strictly fades out 0.24f -> 0.13f BEFORE resting Dock glyph/orb reveals (0.12f -> 0.02f)
        val fadeIn = (1f - ((boundsT - 0.28f).coerceAtLeast(0f) / 0.14f)).coerceIn(0f, 1f)
        val fadeOutRaw = ((collapseMetric - 0.13f) / 0.11f).coerceIn(0f, 1f)
        val fadeOut = fadeOutRaw * fadeOutRaw * (3f - 2f * fadeOutRaw)
        fadeIn * fadeOut
    }
    val collapseFadeAlpha = if (targetOpen) {
        1f
    } else {
        val dockTakeoverRaw = ((0.12f - collapseMetric) / 0.10f).coerceIn(0f, 1f)
        val dockTakeover = dockTakeoverRaw * dockTakeoverRaw * (3f - 2f * dockTakeoverRaw)
        (1f - dockTakeover).coerceIn(0f, 1f)
    }
    val dockTintBlend = (1f - (boundsT / 0.35f)).coerceIn(0f, 1f)
    val morphTintOverride = lerpColor(sheetBaseTint, dockOrbChromeTint, dockTintBlend)
    val sheetConsumeInteraction = remember { MutableInteractionSource() }

    Layout(
        content = {
            // [0] Live Vitro glass background node — never scaled/translated via graphicsLayer;
            // permanent graphicsLayer alpha & clickable(enabled = targetOpen) prevent ModifierNodeChain churn on dismiss
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = collapseFadeAlpha
                    }
                    .shadow(
                        elevation = (16f * boundsT * collapseFadeAlpha).dp,
                        shape = morphSheetShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (isPaperDark) 0.42f else 0.14f),
                        spotColor = Color.Black.copy(alpha = if (isPaperDark) 0.42f else 0.14f),
                    )
                    .ios27GlassSurface(
                        shape = morphSheetShape,
                        palette = effectiveGlassPalette,
                        style = style,
                        variant = Ios27GlassVariant.ReaderSheet,
                        backdrop = readerBackdrop,
                        tintOverride = morphTintOverride,
                        showContrastBorder = false,
                        darkPaperOverride = isPaperDark,
                        isHidden = false,
                        showFingerHighlight = false,
                    )
                    .clickable(
                        enabled = targetOpen,
                        interactionSource = sheetConsumeInteraction,
                        indication = null,
                    ) {},
            )

            // [1] Sheet content scaled uniformly inside the morphing glass bubble (no header/exit unclip pop)
            Box(
                modifier = Modifier
                    .drawWithContent {
                        val targetW = sheetGeom.endW.coerceAtLeast(1f)
                        val targetH = sheetGeom.endH.coerceAtLeast(1f)
                        val fitScale = minOf(sheetGeom.curW / targetW, sheetGeom.curH / targetH)
                            .coerceIn(0.16f, 1.0f)
                        val insetPx = 1.dp.toPx()
                        val effW = (minOf(sheetGeom.curW, targetW * fitScale) - insetPx * 2f).coerceAtLeast(1f)
                        val effH = (minOf(sheetGeom.curH, targetH * fitScale) - insetPx * 2f).coerceAtLeast(1f)
                        val effCorner = (sheetGeom.curCornerPx - insetPx).coerceAtLeast(0f)
                        clipPath(
                            Path().apply {
                                val left = (size.width - effW) * 0.5f
                                val top = (size.height - effH) * 0.5f
                                addRoundRect(
                                    RoundRect(
                                        left = left,
                                        top = top,
                                        right = left + effW,
                                        bottom = top + effH,
                                        cornerRadius = CornerRadius(effCorner, effCorner),
                                    ),
                                )
                            },
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    .graphicsLayer {
                        val targetW = sheetGeom.endW.coerceAtLeast(1f)
                        val targetH = sheetGeom.endH.coerceAtLeast(1f)
                        val fitScale = minOf(sheetGeom.curW / targetW, sheetGeom.curH / targetH)
                            .coerceIn(0.16f, 1.0f)
                        alpha = contentAlpha
                        scaleX = fitScale
                        scaleY = fitScale
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                        shape = RoundedCornerShape(28.dp)
                        clip = true
                    },
            ) {
                CompositionLocalProvider(LocalVitroBackdrop provides null) {
                    panelContent()
                }
            }

            // [2] Origin glyph transitioning seamlessly out of / back into the orb or Aa pill, color-synced with Dock
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = anchorIconAlpha
                    val glyphScale = if (targetOpen) {
                        1f + 0.12f * (1f - anchorIconAlpha)
                    } else {
                        0.92f + 0.08f * anchorIconAlpha
                    }
                    scaleX = glyphScale
                    scaleY = glyphScale
                },
            ) {
                if (isSettingsPanel) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Aa",
                            color = originIconColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "字体",
                            color = originSubLabelColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        contentDescription = null,
                        tint = originIconColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val layoutW = constraints.maxWidth.coerceAtLeast(1)
        val layoutH = constraints.maxHeight.coerceAtLeast(1)
        val rootTopLeft = rootCoordsHolder[0]?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero

        val endW = with(composeDensity) { sheetWidthDp.toPx() }.coerceAtMost(layoutW - 16f).coerceAtLeast(1f)
        val endH = with(composeDensity) { sheetPanelHeightDp.toPx() }.coerceAtMost(layoutH - 32f).coerceAtLeast(1f)
        val endLeft = (layoutW - endW) * 0.5f
        val endBottom = (layoutH - with(composeDensity) { (dockBottomDp + 74.dp).toPx() })
            .coerceAtLeast(endH + 16f)
        val endTop = (endBottom - endH).coerceAtLeast(12f)
        val endCenterX = endLeft + endW * 0.5f
        val endCenterY = endTop + endH * 0.5f

        val rawOrigin = if (!targetOpen) {
            restingOriginBoundsInRoot ?: tapOriginBoundsInRoot
        } else {
            tapOriginBoundsInRoot ?: restingOriginBoundsInRoot
        }
        val orbFallbackPx = with(composeDensity) { 58.dp.toPx() }
        val startW = rawOrigin?.width?.coerceAtLeast(24f) ?: orbFallbackPx
        val startH = rawOrigin?.height?.coerceAtLeast(24f) ?: orbFallbackPx
        val startCenterX = if (rawOrigin != null) {
            rawOrigin.center.x - rootTopLeft.x
        } else if (isSettingsPanel) {
            endLeft + orbFallbackPx * 1.9f
        } else {
            endLeft + orbFallbackPx * 0.5f
        }
        val startCenterY = if (rawOrigin != null) {
            rawOrigin.center.y - rootTopLeft.y
        } else {
            layoutH - with(composeDensity) { dockBottomDp.toPx() } - with(composeDensity) { 31.dp.toPx() }
        }

        val curW = (startW + (endW - startW) * morph.rawWidthT).coerceAtLeast(16f)
        val curH = (startH + (endH - startH) * morph.rawHeightT).coerceAtLeast(16f)
        val centerX = startCenterX + (endCenterX - startCenterX) * morph.centerT
        val centerY = startCenterY + (endCenterY - startCenterY) * morph.centerT

        val startCornerPx = minOf(startW, startH) * 0.5f
        val endCornerPx = with(composeDensity) { 28.dp.toPx() }
        val curCornerPx = startCornerPx + (endCornerPx - startCornerPx) * boundsT

        sheetGeom.startW = startW
        sheetGeom.startH = startH
        sheetGeom.endW = endW
        sheetGeom.endH = endH
        sheetGeom.curW = curW
        sheetGeom.curH = curH
        sheetGeom.curCornerPx = curCornerPx

        val glassPlaceable = measurables[0].measure(
            Constraints.fixed(curW.roundToInt().coerceAtLeast(1), curH.roundToInt().coerceAtLeast(1)),
        )
        val contentPlaceable = measurables[1].measure(
            Constraints.fixed(endW.roundToInt().coerceAtLeast(1), endH.roundToInt().coerceAtLeast(1)),
        )
        val iconPlaceable = measurables.getOrNull(2)?.measure(
            Constraints.fixed(startW.roundToInt().coerceAtLeast(1), startH.roundToInt().coerceAtLeast(1)),
        )

        layout(layoutW, layoutH) {
            val glassX = (centerX - curW * 0.5f).roundToInt()
            val glassY = (centerY - curH * 0.5f).roundToInt()
            glassPlaceable.placeRelative(glassX, glassY)

            val contentX = (centerX - endW * 0.5f).roundToInt()
            val contentY = (centerY - endH * 0.5f).roundToInt()
            contentPlaceable.placeRelative(contentX, contentY)

            iconPlaceable?.let {
                val iconX = (centerX - startW * 0.5f).roundToInt()
                val iconY = (centerY - startH * 0.5f).roundToInt()
                it.placeRelative(iconX, iconY)
            }
        }
    }
}

@Composable
private fun ReaderAppleBooks272DockBar(
    style: ReadingStyle,
    isPaperDark: Boolean,
    backdrop: io.github.itshedimisawi.vitro.GlassBackdrop,
    effectiveGlassPalette: AppPalette,
    glass: ReaderGlassColors,
    chapterTitle: String,
    progress: Float,
    dockInteractive: Boolean = true,
    tocTargetOpen: Boolean = false,
    settingsTargetOpen: Boolean = false,
    hideTocOrbGlass: Boolean = false,
    tocDockReveal: Float = 1f,
    tocOrbIconReveal: Float = 1f,
    aaDockReveal: Float = 1f,
    aaAccentEngaged: Boolean = false,
    tocIconColor: Color,
    aaLabelColor: Color,
    aaSubLabelColor: Color,
    tocInteraction: MutableInteractionSource,
    tocPressed: Boolean,
    tocDragging: Boolean,
    onTocDraggingChanged: (Boolean) -> Unit,
    aaInteraction: MutableInteractionSource,
    aaPressed: Boolean,
    aaDragging: Boolean,
    onAaDraggingChanged: (Boolean) -> Unit,
    onTocOrbBoundsChanged: (Rect) -> Unit = {},
    onAaPillBoundsChanged: (Rect) -> Unit = {},
    onTapToc: (Rect?) -> Unit,
    onScrubStart: () -> Unit,
    onScrubChange: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onTapSettings: (Rect?) -> Unit,
) {
    val barHeight = 62.dp
    val orbSize = 58.dp
    val overflowPad = 14.dp
    val outerHeight = barHeight + overflowPad * 2
    val pillShape = RoundedCornerShape(31.dp)
    val actionGap = 10.dp

    val isLight = !isPaperDark
    val activeTint = if (isLight) {
        effectiveGlassPalette.accentText
    } else {
        lerpColor(effectiveGlassPalette.primarySoft, Color.White, 0.20f)
    }
    val chapterTitleInk = if (isLight) Color(0xFF1E1D23) else Color(0xFFF2F4FA)
    val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentDockInteractive by rememberUpdatedState(dockInteractive)
    val tocButtonInteractive = dockInteractive
    val aaButtonInteractive = dockInteractive
    val currentTocInteractive by rememberUpdatedState(tocButtonInteractive)
    val currentAaInteractive by rememberUpdatedState(aaButtonInteractive)
    val currentAllowTocRubber by rememberUpdatedState(!tocTargetOpen && tocDockReveal > 0.5f)
    val currentAllowAaRubber by rememberUpdatedState(!settingsTargetOpen && aaDockReveal > 0.5f)

    // Right TOC orb physics + persistent GlassEffectState (never reset when hideTocOrbGlass toggles)
    val tocGlassState = rememberSafeGlassEffectState(
        interactionSource = tocInteraction,
        active = true,
    )
    var tocRestingRect by remember { mutableStateOf<Rect?>(null) }
    var tocPointerPos by remember { mutableStateOf<Offset?>(null) }
    val tocLift by animateFloatAsState(
        targetValue = if ((tocPressed || tocDragging) && dockInteractive && !tocTargetOpen && tocDockReveal > 0.5f) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 460f),
        label = "dockTocLift",
    )
    val tocIconScale = 1f + 0.18f * tocLift
    val maxOrbPullPx = with(density) { 14.dp.toPx() }
    val orbCommitRadiusPx = with(density) { 44.dp.toPx() }
    val orbDragX = remember { Animatable(0f) }
    val orbDragY = remember { Animatable(0f) }
    val currentOnTapToc by rememberUpdatedState(onTapToc)
    val currentOnTocDraggingChanged by rememberUpdatedState(onTocDraggingChanged)

    // Left Aa Typography droplet pill physics
    var aaRestingRect by remember { mutableStateOf<Rect?>(null) }
    var aaPointerPos by remember { mutableStateOf<Offset?>(null) }
    val aaLift by animateFloatAsState(
        targetValue = if ((aaPressed || aaDragging) && dockInteractive && !settingsTargetOpen && aaDockReveal > 0.5f) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 460f),
        label = "dockAaLift",
    )
    val maxAaPullXPx = with(density) { 10.dp.toPx() }
    val maxAaPullYPx = with(density) { 14.dp.toPx() }
    val aaCommitRadiusPx = with(density) { 44.dp.toPx() }
    val aaDragX = remember { Animatable(0f) }
    val aaDragY = remember { Animatable(0f) }
    val currentOnTapSettings by rememberUpdatedState(onTapSettings)
    val currentOnAaDraggingChanged by rememberUpdatedState(onAaDraggingChanged)

    // Close-landing rebound ("抖一下"): when a sheet's glass blob lands back into its Dock button,
    // the button takes a brief compression impulse and springs elastically through ~1.05 back to 1.0.
    val aaLandScale = remember { Animatable(1f) }
    val tocLandScale = remember { Animatable(1f) }
    val aaLanded = aaDockReveal >= 0.15f
    val tocLanded = tocDockReveal >= 0.15f
    var aaLandArmed by remember { mutableStateOf(false) }
    var tocLandArmed by remember { mutableStateOf(false) }
    LaunchedEffect(aaLanded) {
        if (aaLanded && aaLandArmed) {
            aaLandScale.snapTo(0.90f)
            aaLandScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
                initialVelocity = 1.2f,
            )
        }
        aaLandArmed = true
    }
    LaunchedEffect(tocLanded) {
        if (tocLanded && tocLandArmed) {
            tocLandScale.snapTo(0.90f)
            tocLandScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
                initialVelocity = 1.2f,
            )
        }
        tocLandArmed = true
    }
    val aaShadowPaint = remember { AndroidPaint().apply { isAntiAlias = true } }
    val dockPowerSave = LocalPowerSaveMode.current
    val dockOpacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val lightDockTintOverride = remember(isLight, style.glassClarityMode, dockOpacity) {
        if (!isLight) {
            Color.Unspecified
        } else {
            val a = when (style.glassClarityMode) {
                GlassClarityMode.clear -> 0.68f + dockOpacity * 0.14f
                GlassClarityMode.regular -> 0.80f + dockOpacity * 0.10f
                GlassClarityMode.tinted -> 0.87f + dockOpacity * 0.07f
            }
            Color(0xFFECEDF3).copy(alpha = a.coerceIn(0.64f, 0.94f))
        }
    }
    val cachedPillShadowClipPath = remember { Path() }
    val cachedOrbShadowClipPath = remember { Path() }
    val cachedAmbientShadowBlurPx = with(density) { 14.dp.toPx() }.coerceAtLeast(1f)
    val cachedSpotShadowBlurPx = with(density) { 5.dp.toPx() }.coerceAtLeast(1f)
    val cachedDockAmbientShadowPaint = remember(cachedAmbientShadowBlurPx) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(cachedAmbientShadowBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val cachedDockSpotShadowPaint = remember(cachedSpotShadowBlurPx) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(cachedSpotShadowBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }

    val aaRestFill by animateColorAsState(
        targetValue = when {
            aaAccentEngaged && isLight -> Color.White.copy(alpha = 0.86f)
            aaAccentEngaged -> Color.White.copy(alpha = 0.22f)
            isLight -> Color.White.copy(alpha = 0.56f)
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = tween(
            durationMillis = if (aaAccentEngaged) 100 else 420,
            easing = FastOutSlowInEasing,
        ),
        label = "dockAaRestFill",
    )
    val aaLiftedFill = if (isLight) {
        Color.White.copy(alpha = 0.90f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    fun snapshotAaLiveBounds(): Rect? {
        val base = aaRestingRect ?: return null
        val padX = with(density) { 6.dp.toPx() } * aaLift
        val padY = with(density) { 7.dp.toPx() } * aaLift
        val dx = aaDragX.value
        val dy = aaDragY.value
        return Rect(
            left = base.left - padX + dx,
            top = base.top - padY + dy,
            right = base.right + padX + dx,
            bottom = base.bottom + padY + dy,
        )
    }

    fun snapshotTocLiveBounds(): Rect? {
        val base = tocRestingRect ?: return null
        val swellScale = 1f + 0.20f * tocLift
        val halfW = base.width * 0.5f * swellScale
        val halfH = base.height * 0.5f * swellScale
        val cx = base.center.x + orbDragX.value
        val cy = base.center.y + orbDragY.value
        return Rect(
            left = cx - halfW,
            top = cy - halfH,
            right = cx + halfW,
            bottom = cy + halfH,
        )
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    ) {
        val pillWidth = minOf(284.dp, (maxWidth - (orbSize + 18.dp)).coerceAtLeast(220.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(actionGap),
            modifier = Modifier
                .height(outerHeight),
        ) {
            // 1. Left standalone 58dp Liquid Glass orb: TOC & Bookmarks morph origin (matches Shelf "+" orb)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(orbSize)
                    .height(outerHeight)
                    .then(
                        if (!LocalPowerSaveMode.current) {
                            Modifier.glassEffectContainer(spacing = 0.dp)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(orbSize)
                        .onGloballyPositioned { coords ->
                            val r = coords.boundsInRoot()
                            tocRestingRect = r
                            onTocOrbBoundsChanged(r)
                        }
                        .offset {
                            IntOffset(
                                x = orbDragX.value.roundToInt(),
                                y = orbDragY.value.roundToInt(),
                            )
                        }
                        .graphicsLayer {
                            alpha = if (hideTocOrbGlass && backdrop != null) 1f else tocDockReveal.coerceIn(0f, 1f)
                            val landScale = tocLandScale.value
                            scaleX = landScale
                            scaleY = landScale
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "目录与书签"
                            role = Role.Button
                            onClick(label = "打开目录与书签") {
                                if (currentDockInteractive) {
                                    currentOnTapToc(tocRestingRect)
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                        .drawBehind {
                            if (!hideTocOrbGlass &&
                                tocDockReveal > 0.004f &&
                                isLight &&
                                !dockPowerSave &&
                                size.width > 1f &&
                                size.height > 1f &&
                                drawContext.canvas.nativeCanvas.isHardwareAccelerated
                            ) {
                                val r = size.height * 0.5f
                                val liftBoost = tocLift.coerceIn(0f, 1f)
                                cachedOrbShadowClipPath.reset()
                                cachedOrbShadowClipPath.addRoundRect(
                                    RoundRect(0f, 0f, size.width, size.height, r, r),
                                )
                                clipPath(cachedOrbShadowClipPath, clipOp = ClipOp.Difference) {
                                    drawIntoCanvas { canvas ->
                                        val ambientAlpha = 0.065f + 0.03f * liftBoost
                                        cachedDockAmbientShadowPaint.color = android.graphics.Color.argb(
                                            (ambientAlpha * 255).toInt().coerceIn(0, 255),
                                            0,
                                            0,
                                            0,
                                        )
                                        val ambientDy = 4.dp.toPx() + 2.dp.toPx() * liftBoost
                                        canvas.nativeCanvas.drawRoundRect(
                                            -1.dp.toPx(),
                                            ambientDy,
                                            size.width + 1.dp.toPx(),
                                            size.height + ambientDy,
                                            r,
                                            r,
                                            cachedDockAmbientShadowPaint,
                                        )
                                        val spotAlpha = 0.045f + 0.02f * liftBoost
                                        cachedDockSpotShadowPaint.color = android.graphics.Color.argb(
                                            (spotAlpha * 255).toInt().coerceIn(0, 255),
                                            0,
                                            0,
                                            0,
                                        )
                                        val spotDy = 1.5.dp.toPx() + 0.5.dp.toPx() * liftBoost
                                        canvas.nativeCanvas.drawRoundRect(
                                            0f,
                                            spotDy,
                                            size.width,
                                            size.height + spotDy,
                                            r,
                                            r,
                                            cachedDockSpotShadowPaint,
                                        )
                                    }
                                }
                            }
                        }
                        .ios27GlassSurface(
                            shape = CircleShape,
                            palette = effectiveGlassPalette,
                            style = style,
                            variant = Ios27GlassVariant.Chrome,
                            backdrop = backdrop,
                            tintOverride = lightDockTintOverride,
                            isInteractive = dockInteractive,
                            state = tocGlassState,
                            interactionSource = tocInteraction,
                            showContrastBorder = false,
                            darkPaperOverride = isPaperDark,
                            isHidden = hideTocOrbGlass,
                            showFingerHighlight = false,
                        )
                        .pointerInput(maxOrbPullPx, orbCommitRadiusPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (!currentTocInteractive) {
                                    return@awaitEachGesture
                                }
                                val press = PressInteraction.Press(down.position)
                                if (!tocInteraction.tryEmit(press)) {
                                    scope.launch { tocInteraction.emit(press) }
                                }
                                tocPointerPos = down.position
                                currentOnTocDraggingChanged(true)
                                var prevLocalX = down.position.x
                                var prevLocalY = down.position.y
                                var prevOffsetX = orbDragX.value
                                var prevOffsetY = orbDragY.value
                                var totalDx = 0f
                                var totalDy = 0f
                                var lifted = false
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                            ?: break
                                        if (!change.pressed) {
                                            lifted = true
                                            change.consume()
                                            break
                                        }
                                        tocPointerPos = change.position
                                        val curOffsetX = orbDragX.value
                                        val curOffsetY = orbDragY.value
                                        totalDx += (change.position.x + curOffsetX) - (prevLocalX + prevOffsetX)
                                        totalDy += (change.position.y + curOffsetY) - (prevLocalY + prevOffsetY)
                                        prevLocalX = change.position.x
                                        prevLocalY = change.position.y
                                        prevOffsetX = curOffsetX
                                        prevOffsetY = curOffsetY
                                        val pullX = if (currentAllowTocRubber) dockRubberBand(totalDx, maxOrbPullPx) else 0f
                                        val pullY = if (currentAllowTocRubber) dockRubberBand(totalDy, maxOrbPullPx) else 0f
                                        scope.launch {
                                            orbDragX.snapTo(pullX)
                                            orbDragY.snapTo(pullY)
                                        }
                                        change.consume()
                                    }
                                } finally {
                                    currentOnTocDraggingChanged(false)
                                    val endInteraction = if (lifted) {
                                        PressInteraction.Release(press)
                                    } else {
                                        PressInteraction.Cancel(press)
                                    }
                                    if (!tocInteraction.tryEmit(endInteraction)) {
                                        scope.launch { tocInteraction.emit(endInteraction) }
                                    }
                                    scope.launch {
                                        orbDragX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                                        )
                                    }
                                    scope.launch {
                                        orbDragY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                                        )
                                    }
                                }
                                if (lifted && hypot(totalDx, totalDy) <= orbCommitRadiusPx) {
                                    currentOnTapToc(snapshotTocLiveBounds())
                                }
                            }
                        },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        contentDescription = "目录与书签",
                        tint = tocIconColor,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                alpha = if (hideTocOrbGlass) 0f else 1f
                                scaleX = tocIconScale
                                scaleY = tocIconScale
                            },
                    )
                }
            }

            // 2. Right Main-Dock-matched 62dp Liquid Glass capsule inside 90dp unclipped overflow container
            //    (own isolated glass container: pressing the TOC orb never fuses a bridge seam to this capsule)
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(outerHeight)
                    .then(
                        if (!LocalPowerSaveMode.current) {
                            Modifier.glassEffectContainer(spacing = 0.dp)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(pillWidth)
                        .height(barHeight)
                        .drawBehind {
                            if (isLight &&
                                !dockPowerSave &&
                                size.width > 1f &&
                                size.height > 1f &&
                                drawContext.canvas.nativeCanvas.isHardwareAccelerated
                            ) {
                                val r = size.height * 0.5f
                                cachedPillShadowClipPath.reset()
                                cachedPillShadowClipPath.addRoundRect(
                                    RoundRect(0f, 0f, size.width, size.height, r, r),
                                )
                                clipPath(cachedPillShadowClipPath, clipOp = ClipOp.Difference) {
                                    drawIntoCanvas { canvas ->
                                        cachedDockAmbientShadowPaint.color = android.graphics.Color.argb(
                                            (0.065f * 255).toInt(),
                                            0,
                                            0,
                                            0,
                                        )
                                        val ambientDy = 4.dp.toPx()
                                        canvas.nativeCanvas.drawRoundRect(
                                            -1.dp.toPx(),
                                            ambientDy,
                                            size.width + 1.dp.toPx(),
                                            size.height + ambientDy,
                                            r,
                                            r,
                                            cachedDockAmbientShadowPaint,
                                        )
                                        cachedDockSpotShadowPaint.color = android.graphics.Color.argb(
                                            (0.045f * 255).toInt(),
                                            0,
                                            0,
                                            0,
                                        )
                                        val spotDy = 1.5.dp.toPx()
                                        canvas.nativeCanvas.drawRoundRect(
                                            0f,
                                            spotDy,
                                            size.width,
                                            size.height + spotDy,
                                            r,
                                            r,
                                            cachedDockSpotShadowPaint,
                                        )
                                    }
                                }
                            }
                        }
                        .ios27GlassSurface(
                            shape = pillShape,
                            palette = effectiveGlassPalette,
                            style = style,
                            variant = Ios27GlassVariant.Chrome,
                            backdrop = backdrop,
                            tintOverride = lightDockTintOverride,
                            isInteractive = false,
                            showContrastBorder = false,
                            darkPaperOverride = isPaperDark,
                            showFingerHighlight = false,
                        )
                        .padding(4.dp),
                ) {
                    // Reserve exact 72dp Aa droplet pill + 4dp spacing on the left
                    Spacer(modifier = Modifier.width(76.dp))

                    ReaderThickCapsuleProgressBar(
                        chapterTitle = chapterTitle,
                        percent = percent,
                        progress = progress,
                        activeTint = activeTint,
                        secondaryTint = effectiveGlassPalette.primary,
                        inkColor = chapterTitleInk,
                        isLight = isLight,
                        enabled = currentDockInteractive,
                        onDragStart = onScrubStart,
                        onProgressChanged = onScrubChange,
                        onDragEnd = onScrubEnd,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }

                // Leftmost slot: Main-Dock "在读" unclipped liquid droplet lens for Aa Typography & Display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .width(72.dp)
                        .height(barHeight - 8.dp)
                        .onGloballyPositioned { coords ->
                            val r = coords.boundsInRoot()
                            aaRestingRect = r
                            onAaPillBoundsChanged(r)
                        }
                        .graphicsLayer {
                            val landScale = aaLandScale.value
                            scaleX = landScale
                            scaleY = landScale
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "阅读排版与设置"
                            role = Role.Button
                            onClick(label = "打开阅读排版与设置") {
                                if (currentDockInteractive) {
                                    currentOnTapSettings(aaRestingRect)
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                        .pointerInput(maxAaPullXPx, maxAaPullYPx, aaCommitRadiusPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (!currentAaInteractive) {
                                    return@awaitEachGesture
                                }
                                val press = PressInteraction.Press(down.position)
                                if (!aaInteraction.tryEmit(press)) {
                                    scope.launch { aaInteraction.emit(press) }
                                }
                                aaPointerPos = down.position
                                currentOnAaDraggingChanged(true)
                                var totalDx = 0f
                                var totalDy = 0f
                                var prevX = down.position.x
                                var prevY = down.position.y
                                var lifted = false
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                            ?: break
                                        if (!change.pressed) {
                                            lifted = true
                                            change.consume()
                                            break
                                        }
                                        aaPointerPos = change.position
                                        totalDx += change.position.x - prevX
                                        totalDy += change.position.y - prevY
                                        prevX = change.position.x
                                        prevY = change.position.y
                                        val pullX = if (currentAllowAaRubber) dockRubberBand(totalDx, maxAaPullXPx) else 0f
                                        val pullY = if (currentAllowAaRubber) dockRubberBand(totalDy, maxAaPullYPx) else 0f
                                        scope.launch {
                                            aaDragX.snapTo(pullX)
                                            aaDragY.snapTo(pullY)
                                        }
                                        change.consume()
                                    }
                                } finally {
                                    currentOnAaDraggingChanged(false)
                                    val endInteraction = if (lifted) {
                                        PressInteraction.Release(press)
                                    } else {
                                        PressInteraction.Cancel(press)
                                    }
                                    if (!aaInteraction.tryEmit(endInteraction)) {
                                        scope.launch { aaInteraction.emit(endInteraction) }
                                    }
                                    scope.launch {
                                        aaDragX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                                        )
                                    }
                                    scope.launch {
                                        aaDragY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                                        )
                                    }
                                }
                                if (lifted && hypot(totalDx, totalDy) <= aaCommitRadiusPx) {
                                    currentOnTapSettings(snapshotAaLiveBounds())
                                }
                            }
                        },
                ) {
                    // Unclipped liquid droplet pill body + radial finger spotlight (cross-fades smoothly on collapse)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val reveal = aaDockReveal.coerceIn(0f, 1f)
                        if (reveal <= 0.005f) return@Canvas
                        val lift = aaLift.coerceIn(0f, 1.15f)
                        val clampedLift = lift.coerceIn(0f, 1f)
                        val padX = 6.dp.toPx() * lift
                        val padY = 7.dp.toPx() * lift
                        val dx = aaDragX.value
                        val dy = aaDragY.value
                        val left = -padX + dx
                        val top = -padY + dy
                        val curW = (size.width + padX * 2f).coerceAtLeast(1f)
                        val curH = (size.height + padY * 2f).coerceAtLeast(1f)
                        val cornerPx = minOf(curW, curH) * 0.5f

                        if (clampedLift > 0.01f) {
                            val blurPx = (9.dp.toPx() * clampedLift).coerceAtLeast(1f)
                            aaShadowPaint.color = Color.Black.copy(
                                alpha = (if (isLight) 0.14f else 0.34f) * clampedLift * reveal,
                            ).toArgb()
                            aaShadowPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                            val yOff = 2.5.dp.toPx() * clampedLift
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawRoundRect(
                                    left,
                                    top + yOff,
                                    left + curW,
                                    top + curH + yOff,
                                    cornerPx,
                                    cornerPx,
                                    aaShadowPaint,
                                )
                            }
                        }

                        val dropletPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = left,
                                    top = top,
                                    right = left + curW,
                                    bottom = top + curH,
                                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                                ),
                            )
                        }
                        val fillColor = lerpColor(aaRestFill, aaLiftedFill, clampedLift)
                        drawPath(
                            path = dropletPath,
                            color = fillColor.copy(alpha = fillColor.alpha * reveal),
                        )

                        if (clampedLift > 0.01f) {
                            val rawSpot = aaPointerPos ?: Offset(size.width * 0.5f, size.height * 0.5f)
                            val spotCenter = Offset(
                                x = rawSpot.x.coerceIn(left, left + curW),
                                y = rawSpot.y.coerceIn(top, top + curH),
                            )
                            val spotRadius = max(curW, curH) * 0.92f
                            val spotLift = clampedLift * reveal
                            clipPath(dropletPath) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = (if (isLight) 0.45f else 0.22f) * spotLift),
                                        0.48f to Color.Transparent,
                                        1f to Color.White.copy(alpha = (if (isLight) 0.18f else 0.10f) * spotLift),
                                    ),
                                    topLeft = Offset(left, top),
                                    size = Size(curW, curH),
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = (if (isLight) 0.68f else 0.42f) * spotLift),
                                            activeTint.copy(alpha = 0.18f * spotLift),
                                            Color.Transparent,
                                        ),
                                        center = spotCenter,
                                        radius = spotRadius,
                                    ),
                                    topLeft = Offset(left, top),
                                    size = Size(curW, curH),
                                )
                            }
                            // Finger-proximal specular gleam only (zero full-perimeter stroke border)
                            drawPath(
                                path = dropletPath,
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f * spotLift),
                                        Color.White.copy(alpha = 0.25f * spotLift),
                                        Color.Transparent,
                                    ),
                                    center = spotCenter,
                                    radius = spotRadius * 0.72f,
                                ),
                                style = Stroke(width = 1.4.dp.toPx()),
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = aaDockReveal.coerceIn(0f, 1f)
                            translationX = aaDragX.value
                            translationY = aaDragY.value
                            val labelScale = 1f + 0.16f * aaLift
                            scaleX = labelScale
                            scaleY = labelScale
                        },
                    ) {
                        Text(
                            text = "Aa",
                            color = aaLabelColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.4).sp,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "字体",
                            color = aaSubLabelColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.readerSpringPress(
    interactionSource: MutableInteractionSource,
    shape: Shape,
    haloColor: Color,
    targetScale: Float = 1.07f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 540f),
        label = "readerPressScale",
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "readerPressHalo",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            if (haloAlpha > 0.01f) {
                val outline = shape.createOutline(size, layoutDirection, this)
                val outlinePath = Path().apply { addOutline(outline) }
                clipPath(outlinePath) {
                    drawRect(color = haloColor.copy(alpha = haloColor.alpha * haloAlpha))
                }
            }
            drawContent()
        }
}

private class ReaderTocTreeIndex(
    val parentIndex: IntArray,
    val hasChildren: BooleanArray,
)

private fun buildReaderTocTreeIndex(chapters: List<com.squartor.reader.model.ReaderChapter>): ReaderTocTreeIndex {
    val n = chapters.size
    val parentIndex = IntArray(n) { -1 }
    val hasChildren = BooleanArray(n)
    val stack = IntArray(n)
    var top = 0
    for (i in 0 until n) {
        val d = chapters[i].tocDepth.coerceAtLeast(0)
        while (top > 0 && chapters[stack[top - 1]].tocDepth.coerceAtLeast(0) >= d) {
            top--
        }
        if (top > 0) {
            val parent = stack[top - 1]
            parentIndex[i] = parent
            hasChildren[parent] = true
        }
        stack[top++] = i
    }
    return ReaderTocTreeIndex(parentIndex = parentIndex, hasChildren = hasChildren)
}

@Composable
private fun ReaderTocFloatingPanel(
    book: BookEntry,
    currentChapterIndex: Int,
    showBookmarks: Boolean,
    isInteractive: Boolean = true,
    onToggleBookmarks: (Boolean) -> Unit,
    palette: AppPalette,
    glass: ReaderGlassColors,
    onClosePanel: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onSelectBookmark: (BookBookmark) -> Unit,
    onRemoveBookmark: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val tree = remember(book.chapters) { buildReaderTocTreeIndex(book.chapters) }

    fun activeAncestorsFor(chapterIdx: Int): Set<Int> {
        if (chapterIdx !in book.chapters.indices) return emptySet()
        val ancestors = HashSet<Int>(8)
        var cursor = if (tree.hasChildren[chapterIdx]) chapterIdx else tree.parentIndex[chapterIdx]
        while (cursor in book.chapters.indices) {
            if (tree.hasChildren[cursor]) {
                ancestors.add(cursor)
            }
            cursor = tree.parentIndex[cursor]
        }
        return ancestors
    }

    val expandedParents = remember(book.id, book.chapters.size) {
        mutableStateMapOf<Int, Boolean>().apply {
            activeAncestorsFor(currentChapterIndex).forEach { this[it] = true }
        }
    }
    var lastAutoExpandedChapter by remember(book.id, book.chapters.size) {
        mutableIntStateOf(currentChapterIndex)
    }

    val visibleChapterIndices by remember(book.chapters, tree) {
        derivedStateOf {
            val out = ArrayList<Int>(book.chapters.size.coerceAtLeast(16))
            var collapsedAncestorDepth = Int.MAX_VALUE
            for (i in book.chapters.indices) {
                val d = book.chapters[i].tocDepth.coerceAtLeast(0)
                if (d > collapsedAncestorDepth) {
                    continue
                }
                collapsedAncestorDepth = Int.MAX_VALUE
                out.add(i)
                if (tree.hasChildren[i] && expandedParents[i] != true) {
                    collapsedAncestorDepth = d
                }
            }
            out
        }
    }

    LaunchedEffect(currentChapterIndex, showBookmarks, tree) {
        if (lastAutoExpandedChapter != currentChapterIndex) {
            lastAutoExpandedChapter = currentChapterIndex
            expandedParents.clear()
            activeAncestorsFor(currentChapterIndex).forEach { expandedParents[it] = true }
        }
        if (!showBookmarks && currentChapterIndex in book.chapters.indices) {
            val visiblePos = visibleChapterIndices.indexOf(currentChapterIndex)
            if (visiblePos >= 0) {
                listState.scrollToItem((visiblePos - 2).coerceAtLeast(0))
            }
        }
    }

    val bookmarkCardShape = RoundedCornerShape(14.dp)
    val rowShape = RoundedCornerShape(11.dp)
    val cardBorderColor = ios27ContrastBorderColor(palette = palette)
    val pressHalo = if (glass.dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
    val headerScrimColor = glass.panelSurface
    val headerScrimBrush = remember(glass.dark, headerScrimColor) {
        Brush.verticalGradient(
            0.0f to headerScrimColor.copy(alpha = if (glass.dark) 0.34f else 0.42f),
            0.62f to headerScrimColor.copy(alpha = if (glass.dark) 0.14f else 0.18f),
            1.0f to Color.Transparent,
        )
    }
    val scrollFadeHeightCache = remember { floatArrayOf(-1f) }
    val scrollFadeBrushCache = remember { arrayOfNulls<Brush>(1) }
    val scrollFadeMaskModifier = Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val h = size.height.coerceAtLeast(1f)
            val topClearPx = 46.dp.toPx()
            val topOpaquePx = 64.dp.toPx()
            val bottomOpaquePx = (h - 14.dp.toPx()).coerceAtLeast(topOpaquePx + 1f)
            val brush = if (
                scrollFadeBrushCache[0] != null &&
                abs(h - scrollFadeHeightCache[0]) <= 0.5f
            ) {
                scrollFadeBrushCache[0]!!
            } else {
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    (topClearPx / h).coerceIn(0f, 1f) to Color.Transparent,
                    (topOpaquePx / h).coerceIn(0f, 1f) to Color.Black,
                    (bottomOpaquePx / h).coerceIn(0f, 1f) to Color.Black,
                    1.0f to Color.Transparent,
                    startY = 0f,
                    endY = h,
                ).also {
                    scrollFadeHeightCache[0] = h
                    scrollFadeBrushCache[0] = it
                }
            }
            drawRect(
                brush = brush,
                blendMode = BlendMode.DstIn,
            )
        }
    val softGlowPaint = remember { AndroidPaint().apply { isAntiAlias = true } }
    val softGlowColor = Color.Black.copy(alpha = if (glass.dark) 0.28f else 0.10f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        if (showBookmarks) {
            if (book.bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 14.dp, top = 60.dp, bottom = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                            contentDescription = null,
                            tint = glass.muted,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右上角书签按钮即可添加书签",
                            color = glass.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                val sortedBookmarks = remember(book.bookmarks) {
                    book.bookmarks.sortedByDescending { it.createdAtMillis }
                }
                LazyColumn(
                    userScrollEnabled = isInteractive,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(scrollFadeMaskModifier),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 58.dp, bottom = 18.dp),
                ) {
                    items(sortedBookmarks, key = { it.id }) { bm ->
                        val chTitle = book.chapters.getOrNull(bm.chapterIndex)?.title
                            ?: "第 ${bm.chapterIndex + 1} 章"
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(bookmarkCardShape)
                                .background(glass.innerCard)
                                .border(0.55.dp, cardBorderColor, bookmarkCardShape)
                                .clickable { onSelectBookmark(bm) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = palette.accentText,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chTitle,
                                    color = glass.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "第 ${bm.chapterIndex + 1} 章 · 第 ${bm.page + 1}/${bm.pageCount.coerceAtLeast(1)} 页",
                                    color = glass.muted,
                                    fontSize = 11.sp,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                )
                                if (bm.snippet.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bm.snippet,
                                        color = glass.text.copy(alpha = 0.80f),
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onRemoveBookmark(bm.id) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "删除书签",
                                    tint = glass.muted,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Collapsible nested TOC tree behind the floating shadowed header
            LazyColumn(
                state = listState,
                userScrollEnabled = isInteractive,
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollFadeMaskModifier),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 58.dp, bottom = 18.dp),
            ) {
                itemsIndexed(visibleChapterIndices, key = { _, chapterIdx -> chapterIdx }) { visiblePos, idx ->
                    val chapter = book.chapters[idx]
                    val selected = idx == currentChapterIndex
                    val depth = chapter.tocDepth.coerceIn(0, 4)
                    val indentDp = (depth * 14).dp
                    val isParent = tree.hasChildren[idx]
                    val isExpanded = expandedParents[idx] == true
                    val rowBg = if (selected) glass.pillSelected else Color.Transparent

                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f),
                        label = "tocParentArrowRotation",
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = indentDp)
                                .height(42.dp)
                                .clip(rowShape)
                                .background(rowBg),
                        ) {
                            if (isParent) {
                                val toggleInteraction = remember { MutableInteractionSource() }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 42.dp)
                                        .readerSpringPress(
                                            interactionSource = toggleInteraction,
                                            shape = CircleShape,
                                            haloColor = pressHalo,
                                            targetScale = 1.10f,
                                        )
                                        .clip(CircleShape)
                                        .semantics {
                                            contentDescription = if (isExpanded) "折叠子目录" else "展开子目录"
                                            role = Role.Button
                                        }
                                        .clickable(
                                            interactionSource = toggleInteraction,
                                            indication = null,
                                        ) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            expandedParents[idx] = !isExpanded
                                        },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (selected) palette.accentText else glass.muted,
                                        modifier = Modifier
                                            .size(19.dp)
                                            .graphicsLayer {
                                                rotationZ = arrowRotation
                                            },
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onSelectChapter(idx) }
                                    .padding(start = if (isParent) 2.dp else 10.dp, end = 10.dp),
                            ) {
                                if (!isParent) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.width(24.dp),
                                    ) {
                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accentText),
                                            )
                                        } else {
                                            Text(
                                                text = "${idx + 1}",
                                                color = glass.subtle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                style = TextStyle(fontFeatureSettings = "tnum"),
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Text(
                                    text = chapter.title,
                                    color = if (selected) palette.accentText else glass.text.copy(alpha = 0.92f),
                                    fontSize = 13.5.sp,
                                    fontWeight = when {
                                        selected -> FontWeight.SemiBold
                                        isParent && depth == 0 -> FontWeight.SemiBold
                                        isParent -> FontWeight.Medium
                                        else -> FontWeight.Normal
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )

                                if (selected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(palette.accentText.copy(alpha = 0.16f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "当前",
                                            color = palette.accentText,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }

                        val nextIdx = visibleChapterIndices.getOrNull(visiblePos + 1)
                        if (!selected && nextIdx != null && nextIdx != currentChapterIndex) {
                            val lineStart = if (isParent) 42.dp + indentDp else 40.dp + indentDp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = lineStart, end = 8.dp)
                                    .height(0.5.dp)
                                    .background(glass.line.copy(alpha = 0.38f)),
                            )
                        }
                    }
                }
            }
        }

        // Floating header anchored over soft vertical scrim: stadium-pill [目录 | 书签] + crisp Close (×) only
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(headerScrimBrush)
                .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
            ) {
                val selectedTabIndex = if (showBookmarks) 1 else 0
                val trackBg = if (glass.dark) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    Color.Black.copy(alpha = 0.055f)
                }
                val activePillFill = if (glass.dark) {
                    Color.White.copy(alpha = 0.24f)
                } else {
                    Color.White.copy(alpha = 0.94f)
                }

                // Authentic iOS 26 stadium-capsule segmented control matching Main Dock "在读" pill
                BoxWithConstraints(
                    modifier = Modifier
                        .width(184.dp)
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(trackBg)
                        .padding(3.dp),
                ) {
                    val segmentWidth = maxWidth / 2f
                    val indicatorOffset by animateDpAsState(
                        targetValue = segmentWidth * selectedTabIndex,
                        animationSpec = spring(dampingRatio = 0.76f, stiffness = 460f),
                        label = "tocHeaderPillOffset",
                    )

                    // Sliding frosted pill with soft 2D ambient glow (no border, no HWUI shadow flash)
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(segmentWidth)
                            .fillMaxHeight()
                            .drawBehind {
                                val blurPx = 5.dp.toPx().coerceAtLeast(1f)
                                val r = size.height * 0.5f
                                softGlowPaint.color = softGlowColor.toArgb()
                                softGlowPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.drawRoundRect(
                                        0f,
                                        1.5.dp.toPx(),
                                        size.width,
                                        size.height + 1.5.dp.toPx(),
                                        r,
                                        r,
                                        softGlowPaint,
                                    )
                                }
                            }
                            .clip(CircleShape)
                            .background(activePillFill)
                            .drawBehind {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = if (glass.dark) 0.14f else 0.48f),
                                        0.55f to Color.Transparent,
                                        1f to Color.Transparent,
                                    ),
                                )
                            },
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(false to "目录", true to "书签 (${book.bookmarks.size})").forEach { (isBmTab, label) ->
                            val active = showBookmarks == isBmTab
                            val tabInteraction = remember { MutableInteractionSource() }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .readerSpringPress(
                                        interactionSource = tabInteraction,
                                        shape = CircleShape,
                                        haloColor = pressHalo,
                                        targetScale = 1.06f,
                                    )
                                    .vitroFingerSpotlight(
                                        shape = CircleShape,
                                        accentColor = palette.accentText,
                                        isDarkSurface = glass.dark,
                                        maxRadiusDp = 50.dp,
                                        intensity = 0.90f,
                                    )
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = tabInteraction,
                                        indication = null,
                                    ) { onToggleBookmarks(isBmTab) },
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) glass.text else glass.muted,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                val closeInteraction = remember { MutableInteractionSource() }
                val closeFill = if (glass.dark) {
                    Color.White.copy(alpha = 0.16f)
                } else {
                    Color.White.copy(alpha = 0.88f)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .drawBehind {
                            val blurPx = 5.dp.toPx().coerceAtLeast(1f)
                            val r = size.width * 0.5f
                            softGlowPaint.color = softGlowColor.toArgb()
                            softGlowPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawRoundRect(
                                    0f,
                                    1.5.dp.toPx(),
                                    size.width,
                                    size.height + 1.5.dp.toPx(),
                                    r,
                                    r,
                                    softGlowPaint,
                                )
                            }
                        }
                        .readerSpringPress(
                            interactionSource = closeInteraction,
                            shape = CircleShape,
                            haloColor = pressHalo,
                            targetScale = 1.10f,
                        )
                        .vitroFingerSpotlight(
                            shape = CircleShape,
                            accentColor = palette.accentText,
                            isDarkSurface = glass.dark,
                            maxRadiusDp = 38.dp,
                            intensity = 0.95f,
                        )
                        .clip(CircleShape)
                        .background(closeFill)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = onClosePanel,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = glass.text,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderThickCapsuleProgressBar(
    chapterTitle: String,
    percent: Int,
    progress: Float,
    activeTint: Color,
    secondaryTint: Color,
    inkColor: Color,
    isLight: Boolean,
    enabled: Boolean = true,
    onDragStart: () -> Unit,
    onProgressChanged: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val barShape = RoundedCornerShape(27.dp)
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var scrubPointerPos by remember { mutableStateOf<Offset?>(null) }
    val latestWidthPx by rememberUpdatedState(trackWidthPx)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)

    val haptic = LocalHapticFeedback.current
    var lastHapticMs by remember { mutableLongStateOf(0L) }
    var lastHapticPct by remember { mutableIntStateOf(-1) }

    val fillAlphaBoost by animateFloatAsState(
        targetValue = if (isPressed || isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "thickProgressBoost",
    )
    val fillStartColor = activeTint.copy(
        alpha = (if (isLight) 0.14f else 0.24f) + (if (isLight) 0.08f else 0.10f) * fillAlphaBoost,
    )
    val fillEndColor = lerpColor(activeTint, secondaryTint, 0.35f).copy(
        alpha = (if (isLight) 0.24f else 0.36f) + (if (isLight) 0.10f else 0.12f) * fillAlphaBoost,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "阅读进度，$chapterTitle，$percent%"
            }
            .clip(barShape)
            .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!latestEnabled) {
                        return@awaitEachGesture
                    }
                    isPressed = true
                    scrubPointerPos = down.position
                    var dragMode = 0 // 0 = undecided, 1 = horizontal scrub, -1 = vertical cancel
                    var totalDx = 0f
                    var totalDy = 0f
                    var endedCleanly = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break
                            scrubPointerPos = change.position
                            val delta = change.positionChange()
                            if (!change.pressed) {
                                if (dragMode == 0 && !change.isConsumed) {
                                    change.consume()
                                    latestOnDragStart()
                                    val p = (change.position.x / latestWidthPx).coerceIn(0f, 1f)
                                    val pct = (p * 100f).roundToInt()
                                    val now = SystemClock.uptimeMillis()
                                    if (pct != lastHapticPct && now - lastHapticMs >= 90L) {
                                        lastHapticMs = now
                                        lastHapticPct = pct
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    latestOnProgressChanged(p)
                                    latestOnDragEnd()
                                } else if (dragMode == 1) {
                                    change.consume()
                                    isDragging = false
                                    latestOnDragEnd()
                                }
                                endedCleanly = true
                                break
                            }
                            if (change.isConsumed) {
                                dragMode = -1
                                endedCleanly = true
                                break
                            }
                            if (dragMode == 0) {
                                totalDx += delta.x
                                totalDy += delta.y
                                if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx) * 0.85f) {
                                    dragMode = -1
                                    endedCleanly = true
                                    break
                                } else if (abs(totalDx) > touchSlop * 0.55f && abs(totalDx) >= abs(totalDy)) {
                                    dragMode = 1
                                    isDragging = true
                                    change.consume()
                                    latestOnDragStart()
                                    val p = (change.position.x / latestWidthPx).coerceIn(0f, 1f)
                                    val pct = (p * 100f).roundToInt()
                                    lastHapticPct = pct
                                    lastHapticMs = SystemClock.uptimeMillis()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    latestOnProgressChanged(p)
                                }
                            } else if (dragMode == 1) {
                                change.consume()
                                val p = (change.position.x / latestWidthPx).coerceIn(0f, 1f)
                                val pct = (p * 100f).roundToInt()
                                val now = SystemClock.uptimeMillis()
                                if (pct != lastHapticPct && now - lastHapticMs >= 90L) {
                                    lastHapticMs = now
                                    lastHapticPct = pct
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                latestOnProgressChanged(p)
                            }
                        }
                    } finally {
                        isPressed = false
                        isDragging = false
                        if (dragMode == 1 && !endedCleanly) {
                            latestOnDragEnd()
                        }
                    }
                }
            }
            .vitroFingerSpotlight(
                shape = barShape,
                accentColor = activeTint,
                isDarkSurface = !isLight,
                enabled = enabled,
                pointerOverride = scrubPointerPos,
                pressedOverride = isPressed || isDragging,
                externalPointer = true,
                maxRadiusDp = 64.dp,
                intensity = 0.85f,
            )
            .drawBehind {
                val fillW = (size.width * safeProgress).coerceAtLeast(0f)
                if (fillW > 0.5f) {
                    if (safeProgress >= 0.992f) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(fillStartColor, fillEndColor),
                                startX = 0f,
                                endX = size.width.coerceAtLeast(1f),
                            ),
                            topLeft = Offset.Zero,
                            size = size,
                        )
                    } else {
                        val featherPx = minOf(18.dp.toPx(), size.width * 0.14f).coerceAtLeast(1f)
                        val featherEnd = (fillW + featherPx * 0.45f).coerceAtMost(size.width).coerceAtLeast(1f)
                        val solidEnd = (fillW - featherPx * 0.55f).coerceAtLeast(0f)
                        val solidStop = (solidEnd / featherEnd).coerceIn(0f, 0.97f)
                        val midStop = (fillW / featherEnd).coerceIn(solidStop + 0.005f, 0.99f)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to fillStartColor,
                                solidStop to fillEndColor,
                                midStop to fillEndColor.copy(alpha = fillEndColor.alpha * 0.34f),
                                1f to Color.Transparent,
                                startX = 0f,
                                endX = featherEnd,
                            ),
                            topLeft = Offset.Zero,
                            size = Size(featherEnd, size.height),
                        )
                    }
                }
            }
            .padding(horizontal = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = chapterTitle,
                color = inkColor.copy(alpha = if (isLight) 0.96f else 0.95f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$percent%",
                color = if (percent > 0) activeTint else inkColor.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                style = TextStyle(
                    fontFeatureSettings = "tnum",
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            )
        }
    }
}

@Composable
private fun ReaderSettingsFloatingPanel(
    style: ReadingStyle,
    palette: AppPalette,
    themeSwatchColor: Color,
    glass: ReaderGlassColors,
    isInteractive: Boolean = true,
    onClosePanel: () -> Unit,
    onUpdateStyle: (ReadingStyle) -> Unit,
) {
    val scrollState = rememberScrollState()
    val latestStyle by rememberUpdatedState(style)
    val latestOnUpdateStyle by rememberUpdatedState(onUpdateStyle)
    val haptic = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(18.dp)
    val cardBorderColor = ios27ContrastBorderColor(palette = palette)
    val pressHalo = if (glass.dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)

    val fontWeightLabel = remember(style.fontWeightValue) {
        when (style.fontWeightValue) {
            100 -> "极细 (100)"
            200 -> "特细 (200)"
            300 -> "细体 (300)"
            400 -> "常规 (400)"
            500 -> "中等 (500)"
            600 -> "半粗 (600)"
            700 -> "粗体 (700)"
            800 -> "特粗 (800)"
            900 -> "浓黑 (900)"
            else -> "${style.fontWeightValue}"
        }
    }

    val settingsFadeHeightCache = remember { floatArrayOf(-1f) }
    val settingsFadeBrushCache = remember { arrayOfNulls<Brush>(1) }
    val scrollFadeMaskModifier = Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val h = size.height.coerceAtLeast(1f)
            val topClearPx = 46.dp.toPx()
            val topOpaquePx = 62.dp.toPx()
            val bottomOpaquePx = (h - 14.dp.toPx()).coerceAtLeast(topOpaquePx + 1f)
            val brush = if (
                settingsFadeBrushCache[0] != null &&
                abs(h - settingsFadeHeightCache[0]) <= 0.5f
            ) {
                settingsFadeBrushCache[0]!!
            } else {
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    (topClearPx / h).coerceIn(0f, 1f) to Color.Transparent,
                    (topOpaquePx / h).coerceIn(0f, 1f) to Color.Black,
                    (bottomOpaquePx / h).coerceIn(0f, 1f) to Color.Black,
                    1.0f to Color.Transparent,
                    startY = 0f,
                    endY = h,
                ).also {
                    settingsFadeHeightCache[0] = h
                    settingsFadeBrushCache[0] = it
                }
            }
            drawRect(
                brush = brush,
                blendMode = BlendMode.DstIn,
            )
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollFadeMaskModifier)
                .verticalScroll(scrollState, enabled = isInteractive)
                .padding(start = 14.dp, end = 14.dp, top = 56.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "阅读纸张与日夜主题",
                color = glass.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(glass.innerCard)
                    .border(0.55.dp, cardBorderColor, cardShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val paperOptions = listOf(
                        ReaderBackgroundId.paper to "米白",
                        ReaderBackgroundId.green to "护眼",
                        ReaderBackgroundId.warm to "暖棕",
                        ReaderBackgroundId.black to "纯黑",
                        ReaderBackgroundId.theme to "主题",
                    )
                    paperOptions.forEach { (bgId, bgLabel) ->
                        val swatchColor = if (bgId == ReaderBackgroundId.theme) {
                            themeSwatchColor
                        } else {
                            readerPalettes[bgId]?.background ?: glass.innerCard
                        }
                        val selected = style.readerBackground == bgId
                        val swatchInteraction = remember { MutableInteractionSource() }
                        val swatchShape = RoundedCornerShape(12.dp)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .readerSpringPress(
                                    interactionSource = swatchInteraction,
                                    shape = swatchShape,
                                    haloColor = pressHalo,
                                    targetScale = 1.07f,
                                )
                                .clip(swatchShape)
                                .clickable(
                                    interactionSource = swatchInteraction,
                                    indication = null,
                                ) {
                                    val current = latestStyle
                                    val turnOffAutoNight = glass.dark &&
                                        current.autoNightPaper &&
                                        (bgId == ReaderBackgroundId.paper || bgId == ReaderBackgroundId.green)
                                    val nextStyle = current.copy(
                                        readerBackground = bgId,
                                        autoNightPaper = if (turnOffAutoNight) false else current.autoNightPaper,
                                    )
                                    if (nextStyle != current) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        latestOnUpdateStyle(nextStyle)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 2.2.dp else 0.8.dp,
                                        color = if (selected) palette.accentText else glass.line,
                                        shape = CircleShape,
                                    ),
                            ) {
                                if (selected) {
                                    val checkTint = if (swatchColor.luminance() < 0.5f) Color.White else Color(0xFF1D1D1F)
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = checkTint,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                            Text(
                                text = bgLabel,
                                color = if (selected) glass.text else glass.muted,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }

                ReaderInlineSwitchRow(
                    title = "日夜纸张自动联动",
                    subtitle = "夜间自动将浅色米白纸张切换为静谧暖棕暗底",
                    checked = style.autoNightPaper,
                    palette = palette,
                    glass = glass,
                    onCheckedChange = { checked ->
                        val current = latestStyle
                        if (current.autoNightPaper != checked) {
                            latestOnUpdateStyle(current.copy(autoNightPaper = checked))
                        }
                    },
                )
            }

            Text(
                text = "排版与翻页",
                color = glass.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(glass.innerCard)
                    .border(0.55.dp, cardBorderColor, cardShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "翻页方式",
                        color = glass.muted,
                        fontSize = 13.sp,
                    )
                    ReaderLiquidSegmentedPill(
                        options = listOf(
                            ReadingFlowMode.cover to "覆盖",
                            ReadingFlowMode.slide to "平移",
                            ReadingFlowMode.curl to "仿真",
                            ReadingFlowMode.scroll to "滚动",
                        ),
                        selected = style.readingFlow,
                        onSelect = { mode ->
                            val current = latestStyle
                            if (current.readingFlow != mode) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestOnUpdateStyle(current.copy(readingFlow = mode))
                            }
                        },
                        palette = palette,
                        glass = glass,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ReaderSheetSliderRow(
                    label = "字号",
                    valueLabel = "${style.fontSize.roundToInt()}",
                    value = style.fontSize,
                    min = 14f,
                    max = 32f,
                    step = 1f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = next.roundToInt().toFloat().coerceIn(14f, 32f)
                        val current = latestStyle
                        if (current.fontSize != stepped) {
                            latestOnUpdateStyle(current.copy(fontSize = stepped))
                        }
                    },
                )

                ReaderSheetSliderRow(
                    label = "字重",
                    valueLabel = fontWeightLabel,
                    value = style.fontWeightValue.toFloat(),
                    min = 100f,
                    max = 900f,
                    step = 100f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = ((next / 100f).roundToInt() * 100).coerceIn(100, 900)
                        val current = latestStyle
                        if (current.fontWeightValue != stepped) {
                            latestOnUpdateStyle(current.copy(fontWeightValue = stepped))
                        }
                    },
                )

                ReaderSheetSliderRow(
                    label = "行高",
                    valueLabel = String.format(Locale.US, "%.2f", style.lineHeight),
                    value = style.lineHeight,
                    min = 1.2f,
                    max = 2.6f,
                    step = 0.05f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = ((next * 20f).roundToInt() / 20f).coerceIn(1.2f, 2.6f)
                        val current = latestStyle
                        if (abs(current.lineHeight - stepped) > 0.001f) {
                            latestOnUpdateStyle(current.copy(lineHeight = stepped))
                        }
                    },
                )

                ReaderSheetSliderRow(
                    label = "左右边距",
                    valueLabel = "${style.pageMargin.roundToInt()}",
                    value = style.pageMargin,
                    min = 0f,
                    max = 52f,
                    step = 2f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = ((next / 2f).roundToInt() * 2).toFloat().coerceIn(0f, 52f)
                        val current = latestStyle
                        if (current.pageMargin != stepped) {
                            latestOnUpdateStyle(current.copy(pageMargin = stepped))
                        }
                    },
                )

                ReaderSheetSliderRow(
                    label = "上下边距",
                    valueLabel = "${style.verticalMargin.roundToInt()}",
                    value = style.verticalMargin,
                    min = 4f,
                    max = 96f,
                    step = 4f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = ((next / 4f).roundToInt() * 4).toFloat().coerceIn(4f, 96f)
                        val current = latestStyle
                        if (current.verticalMargin != stepped) {
                            latestOnUpdateStyle(current.copy(verticalMargin = stepped))
                        }
                    },
                )
            }

            Text(
                text = "iOS 27 Liquid Glass 玻璃风格",
                color = glass.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(glass.innerCard)
                    .border(0.55.dp, cardBorderColor, cardShape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReaderLiquidSegmentedPill(
                    options = listOf(
                        GlassClarityMode.clear to "通透 Clear",
                        GlassClarityMode.regular to "标准 Regular",
                        GlassClarityMode.tinted to "高对比 Tinted",
                    ),
                    selected = style.glassClarityMode,
                    onSelect = { clarity ->
                        val current = latestStyle
                        if (current.glassClarityMode != clarity) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            latestOnUpdateStyle(current.copy(glassClarityMode = clarity))
                        }
                    },
                    palette = palette,
                    glass = glass,
                    modifier = Modifier.fillMaxWidth(),
                )

                ReaderSheetSliderRow(
                    label = "玻璃雾面强度",
                    valueLabel = "${(style.glassOpacityLevel.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                    value = style.glassOpacityLevel.coerceIn(0f, 1f),
                    min = 0f,
                    max = 1f,
                    step = 0.05f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = ((next * 20f).roundToInt() / 20f).coerceIn(0f, 1f)
                        val current = latestStyle
                        if (abs(current.glassOpacityLevel - stepped) > 0.001f) {
                            latestOnUpdateStyle(current.copy(glassOpacityLevel = stepped))
                        }
                    },
                )
            }

            Text(
                text = "更多细调",
                color = glass.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(glass.innerCard)
                    .border(0.55.dp, cardBorderColor, cardShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                ReaderSheetSliderRow(
                    label = "段距",
                    valueLabel = "${style.paragraphSpacing.roundToInt()}",
                    value = style.paragraphSpacing,
                    min = 0f,
                    max = 30f,
                    step = 2f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = ((next / 2f).roundToInt() * 2).toFloat().coerceIn(0f, 30f)
                        val current = latestStyle
                        if (current.paragraphSpacing != stepped) {
                            latestOnUpdateStyle(current.copy(paragraphSpacing = stepped))
                        }
                    },
                )

                ReaderSheetSliderRow(
                    label = "字距",
                    valueLabel = String.format(Locale.US, "%.1f", style.letterSpacing),
                    value = style.letterSpacing,
                    min = 0f,
                    max = 2f,
                    step = 0.1f,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        val stepped = ((next * 10f).roundToInt() / 10f).coerceIn(0f, 2f)
                        val current = latestStyle
                        if (abs(current.letterSpacing - stepped) > 0.001f) {
                            latestOnUpdateStyle(current.copy(letterSpacing = stepped))
                        }
                    },
                )

                ReaderInlineSwitchRow(
                    title = "首行缩进",
                    subtitle = "正文段落开头自动缩进两个汉字",
                    checked = style.firstLineIndent,
                    palette = palette,
                    glass = glass,
                    onCheckedChange = { checked ->
                        val current = latestStyle
                        if (current.firstLineIndent != checked) {
                            latestOnUpdateStyle(current.copy(firstLineIndent = checked))
                        }
                    },
                )

                ReaderInlineSwitchRow(
                    title = "反转点击翻页",
                    subtitle = "左右点击翻页区域互换，方便单手握持",
                    checked = style.reverseTapPageTurn,
                    palette = palette,
                    glass = glass,
                    onCheckedChange = { checked ->
                        val current = latestStyle
                        if (current.reverseTapPageTurn != checked) {
                            latestOnUpdateStyle(current.copy(reverseTapPageTurn = checked))
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        val headerScrimColor = glass.panelSurface
        val headerScrimBrush = remember(glass.dark, headerScrimColor) {
            Brush.verticalGradient(
                0.0f to headerScrimColor.copy(alpha = if (glass.dark) 0.34f else 0.42f),
                0.62f to headerScrimColor.copy(alpha = if (glass.dark) 0.14f else 0.18f),
                1.0f to Color.Transparent,
            )
        }
        val softGlowPaint = remember { AndroidPaint().apply { isAntiAlias = true } }
        val softGlowColor = Color.Black.copy(alpha = if (glass.dark) 0.28f else 0.10f)

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(headerScrimBrush)
                .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
            ) {
                Text(
                    text = "版式与玻璃质感",
                    color = glass.text,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.dp),
                )

                val closeInteraction = remember { MutableInteractionSource() }
                val closeFill = if (glass.dark) {
                    Color.White.copy(alpha = 0.16f)
                } else {
                    Color.White.copy(alpha = 0.88f)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .drawBehind {
                            val blurPx = 5.dp.toPx().coerceAtLeast(1f)
                            val r = size.width * 0.5f
                            softGlowPaint.color = softGlowColor.toArgb()
                            softGlowPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawRoundRect(
                                    0f,
                                    1.5.dp.toPx(),
                                    size.width,
                                    size.height + 1.5.dp.toPx(),
                                    r,
                                    r,
                                    softGlowPaint,
                                )
                            }
                        }
                        .readerSpringPress(
                            interactionSource = closeInteraction,
                            shape = CircleShape,
                            haloColor = pressHalo,
                            targetScale = 1.10f,
                        )
                        .vitroFingerSpotlight(
                            shape = CircleShape,
                            accentColor = palette.accentText,
                            isDarkSurface = glass.dark,
                            maxRadiusDp = 38.dp,
                            intensity = 0.95f,
                        )
                        .clip(CircleShape)
                        .background(closeFill)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = onClosePanel,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = glass.text,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> ReaderLiquidSegmentedPill(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    palette: AppPalette,
    glass: ReaderGlassColors,
    modifier: Modifier = Modifier,
) {
    val count = options.size.coerceAtLeast(1)
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val trackBg = if (glass.dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.055f)
    val pressHalo = if (glass.dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.07f)
    val activePillFill = if (glass.dark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.92f)
    val softGlowPaint = remember { AndroidPaint().apply { isAntiAlias = true } }
    val softGlowColor = Color.Black.copy(alpha = if (glass.dark) 0.24f else 0.09f)

    BoxWithConstraints(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(trackBg)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / count
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.76f, stiffness = 460f),
            label = "readerSegmentedIndicatorOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .drawBehind {
                    val blurPx = 4.5.dp.toPx().coerceAtLeast(1f)
                    val r = size.height * 0.5f
                    softGlowPaint.color = softGlowColor.toArgb()
                    softGlowPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRoundRect(
                            0f,
                            1.5.dp.toPx(),
                            size.width,
                            size.height + 1.5.dp.toPx(),
                            r,
                            r,
                            softGlowPaint,
                        )
                    }
                }
                .clip(CircleShape)
                .background(activePillFill)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = if (glass.dark) 0.14f else 0.45f),
                            0.55f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    )
                },
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (option, label) ->
                val isSelected = option == selected
                val optionInteraction = remember { MutableInteractionSource() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .readerSpringPress(
                            interactionSource = optionInteraction,
                            shape = CircleShape,
                            haloColor = pressHalo,
                            targetScale = 1.05f,
                        )
                        .vitroFingerSpotlight(
                            shape = CircleShape,
                            accentColor = palette.accentText,
                            isDarkSurface = glass.dark,
                            maxRadiusDp = 50.dp,
                            intensity = 0.85f,
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = optionInteraction,
                            indication = null,
                        ) { onSelect(option) },
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) glass.text else glass.muted,
                        fontSize = 12.sp,
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
private fun ReaderSheetSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    min: Float,
    max: Float,
    step: Float,
    palette: AppPalette,
    glass: ReaderGlassColors,
    onValueChange: (Float) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val buttonBg = if (glass.dark) Color.White.copy(alpha = 0.11f) else Color.Black.copy(alpha = 0.06f)
    val pressHalo = if (glass.dark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)
    val btnShape = RoundedCornerShape(11.dp)
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val decInteraction = remember { MutableInteractionSource() }
    val incInteraction = remember { MutableInteractionSource() }
    val safeStep = step.coerceAtLeast(0.0001f)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = glass.muted,
                fontSize = 13.sp,
            )
            Text(
                text = valueLabel,
                color = glass.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .readerSpringPress(
                        interactionSource = decInteraction,
                        shape = btnShape,
                        haloColor = pressHalo,
                        targetScale = 1.08f,
                    )
                    .clip(btnShape)
                    .background(buttonBg)
                    .clickable(
                        interactionSource = decInteraction,
                        indication = null,
                    ) {
                        val stepCount = (((latestValue - min) / safeStep).roundToInt() - 1).coerceAtLeast(0)
                        val next = (min + stepCount * safeStep).coerceIn(min, max)
                        if (abs(next - latestValue) > safeStep * 0.1f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            latestOnValueChange(next)
                        }
                    },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "减少",
                    tint = glass.text.copy(alpha = 0.88f),
                    modifier = Modifier.size(17.dp),
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ReaderLiquidGlassSlider(
                    value = value.coerceIn(min, max),
                    min = min,
                    max = max,
                    step = safeStep,
                    palette = palette,
                    glass = glass,
                    onValueChange = { next ->
                        latestOnValueChange(next.coerceIn(min, max))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .readerSpringPress(
                        interactionSource = incInteraction,
                        shape = btnShape,
                        haloColor = pressHalo,
                        targetScale = 1.08f,
                    )
                    .clip(btnShape)
                    .background(buttonBg)
                    .clickable(
                        interactionSource = incInteraction,
                        indication = null,
                    ) {
                        val totalSteps = ((max - min) / safeStep).roundToInt().coerceAtLeast(1)
                        val stepCount = (((latestValue - min) / safeStep).roundToInt() + 1).coerceIn(0, totalSteps)
                        val next = (min + stepCount * safeStep).coerceIn(min, max)
                        if (abs(next - latestValue) > safeStep * 0.1f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            latestOnValueChange(next)
                        }
                    },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "增加",
                    tint = glass.text.copy(alpha = 0.88f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun ReaderLiquidGlassSlider(
    value: Float,
    min: Float,
    max: Float,
    step: Float,
    palette: AppPalette,
    glass: ReaderGlassColors,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val safeRange = (max - min).coerceAtLeast(0.0001f)
    val safeStep = step.coerceAtLeast(0.0001f)
    val totalSteps = (safeRange / safeStep).roundToInt().coerceAtLeast(1)
    val currentStep = (((value.coerceIn(min, max) - min) / safeStep).roundToInt()).coerceIn(0, totalSteps)
    val normalizedProgress = ((value.coerceIn(min, max) - min) / safeRange).coerceIn(0f, 1f)

    var sliderWidthPx by remember { mutableFloatStateOf(1f) }
    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticMs by remember { mutableLongStateOf(0L) }
    var lastEmittedStep by remember { mutableIntStateOf(currentStep) }
    SideEffect {
        if (!isDragging) {
            lastEmittedStep = currentStep
        }
    }

    val latestWidthPx by rememberUpdatedState(sliderWidthPx)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val trackInsetPx = with(density) { 14.dp.toPx() }
    val rimStroke = remember(density) { Stroke(width = with(density) { 1.dp.toPx() }) }

    val bubbleProgress by animateFloatAsState(
        targetValue = if (isPressed || isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 460f),
        label = "sliderLiquidBubble",
    )

    val trackBgColor = if (glass.dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.11f)
    val accentStart = palette.accentText
    val accentEnd = lerpColor(palette.primary, Color.White, if (glass.dark) 0.16f else 0.08f)

    fun emitAtPointerX(pointerX: Float) {
        val usableW = (latestWidthPx - trackInsetPx * 2f).coerceAtLeast(1f)
        val fraction = ((pointerX - trackInsetPx) / usableW).coerceIn(0f, 1f)
        val nextStep = (fraction * totalSteps).roundToInt().coerceIn(0, totalSteps)
        if (nextStep != lastEmittedStep) {
            lastEmittedStep = nextStep
            val quantized = (min + nextStep * safeStep).coerceIn(min, max)
            val now = SystemClock.uptimeMillis()
            if (now - lastHapticMs >= 90L) {
                lastHapticMs = now
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            latestOnValueChange(quantized)
        }
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .onSizeChanged { sliderWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(min, max, safeStep, totalSteps) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragMode = 0 // 0 = undecided, 1 = slider drag, -1 = yielded to verticalScroll
                    var totalDx = 0f
                    var totalDy = 0f
                    isPressed = true
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break
                            if (!change.pressed) {
                                val distSq = totalDx * totalDx + totalDy * totalDy
                                if (
                                    dragMode == 0 &&
                                    !change.isConsumed &&
                                    distSq <= touchSlop * touchSlop &&
                                    abs(totalDy) <= touchSlop * 0.85f
                                ) {
                                    change.consume()
                                    emitAtPointerX(change.position.x)
                                } else if (dragMode == 1) {
                                    change.consume()
                                }
                                break
                            }
                            if (change.isConsumed) {
                                dragMode = -1
                                isPressed = false
                                isDragging = false
                                break
                            }
                            val delta = change.positionChange()
                            if (dragMode == 0) {
                                totalDx += delta.x
                                totalDy += delta.y
                                val ax = abs(totalDx)
                                val ay = abs(totalDy)
                                // Strictly yield to verticalScroll when vertical motion exceeds 0.85x horizontal
                                if (ay > touchSlop * 0.65f && ay > ax * 0.85f) {
                                    dragMode = -1
                                    isPressed = false
                                    isDragging = false
                                    break
                                } else if (ax > touchSlop && ax > ay * 1.2f) {
                                    dragMode = 1
                                    isDragging = true
                                    change.consume()
                                    emitAtPointerX(change.position.x)
                                }
                            } else if (dragMode == 1) {
                                change.consume()
                                emitAtPointerX(change.position.x)
                            }
                        }
                    } finally {
                        isPressed = false
                        isDragging = false
                    }
                }
            }
            .drawBehind {
                val barH = 4.dp.toPx()
                val centerY = size.height * 0.5f
                val barTop = centerY - barH * 0.5f
                val usableW = (size.width - trackInsetPx * 2f).coerceAtLeast(1f)
                val barCorner = CornerRadius(barH * 0.5f, barH * 0.5f)

                drawRoundRect(
                    color = trackBgColor,
                    topLeft = Offset(trackInsetPx, barTop),
                    size = Size(usableW, barH),
                    cornerRadius = barCorner,
                )

                val thumbCenterX = trackInsetPx + usableW * normalizedProgress
                val activeW = (thumbCenterX - trackInsetPx).coerceAtLeast(0f)
                if (activeW > 0.5f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(accentStart.copy(alpha = 0.88f), accentEnd),
                            startX = trackInsetPx,
                            endX = (trackInsetPx + activeW).coerceAtLeast(trackInsetPx + 1f),
                        ),
                        topLeft = Offset(trackInsetPx, barTop),
                        size = Size(activeW, barH),
                        cornerRadius = barCorner,
                    )
                }

                val restRadius = 8.dp.toPx()
                val dragRadius = 13.dp.toPx()
                val thumbRadius = restRadius + (dragRadius - restRadius) * bubbleProgress

                if (bubbleProgress > 0.02f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentStart.copy(alpha = 0.25f * bubbleProgress),
                                Color.Transparent,
                            ),
                            center = Offset(thumbCenterX, centerY),
                            radius = thumbRadius * 1.65f,
                        ),
                        radius = thumbRadius * 1.65f,
                        center = Offset(thumbCenterX, centerY),
                    )
                }

                val shadowDy = (1.dp.toPx()) + (1.2.dp.toPx() * bubbleProgress)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.16f + 0.10f * bubbleProgress),
                    radius = thumbRadius,
                    center = Offset(thumbCenterX, centerY + shadowDy),
                )

                val topDroplet = Color.White.copy(alpha = if (glass.dark) 0.96f else 0.99f)
                val bottomDroplet = lerpColor(
                    Color.White.copy(alpha = if (glass.dark) 0.80f else 0.92f),
                    accentStart.copy(alpha = 0.34f),
                    0.50f * bubbleProgress,
                )
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(topDroplet, bottomDroplet),
                        startY = centerY - thumbRadius,
                        endY = centerY + thumbRadius,
                    ),
                    radius = thumbRadius,
                    center = Offset(thumbCenterX, centerY),
                )

                val rimAlpha = (0.45f + 0.48f * bubbleProgress).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = rimAlpha),
                            Color.White.copy(alpha = rimAlpha * 0.25f),
                        ),
                        startY = centerY - thumbRadius,
                        endY = centerY + thumbRadius,
                    ),
                    radius = (thumbRadius - rimStroke.width * 0.5f).coerceAtLeast(0.5f),
                    center = Offset(thumbCenterX, centerY),
                    style = rimStroke,
                )
            },
    )
}

@Composable
private fun ReaderInlineSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: AppPalette,
    glass: ReaderGlassColors,
    onCheckedChange: (Boolean) -> Unit,
) {
    val labelInteraction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        // Keep clickable scoped strictly to label text so it never races/double-fires with Ios27GlassToggle
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = labelInteraction,
                    indication = null,
                ) {
                    onCheckedChange(!checked)
                }
                .padding(vertical = 2.dp),
        ) {
            Text(
                text = title,
                color = glass.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = glass.muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Ios27GlassToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            palette = palette,
        )
    }
}

private class StaticTextLayerCacheKey {
    var page: ComposedPage? = null
    var style: ReadingStyle? = null
    var bodyTypeface: Any? = null
    var titleTypeface: Any? = null
    var bodyColor: Int = 0
    var titleColor: Int = 0
    var bodySize: Float = 0f
    var titleSize: Float = 0f
    var horizontalMarginPx: Int = -1
    var topMarginPx: Int = -1
    var contentWidthPx: Int = -1
    var widthPx: Int = -1
    var heightPx: Int = -1

    fun matchesAndUpdate(
        page: ComposedPage,
        style: ReadingStyle,
        bodyPaint: TextPaint,
        titlePaint: TextPaint,
        horizontalMarginPx: Int,
        topMarginPx: Int,
        contentWidthPx: Int,
        widthPx: Int,
        heightPx: Int,
    ): Boolean {
        val bTf = bodyPaint.typeface
        val tTf = titlePaint.typeface
        val bCol = bodyPaint.color
        val tCol = titlePaint.color
        val bSz = bodyPaint.textSize
        val tSz = titlePaint.textSize
        if (this.page === page &&
            this.style == style &&
            this.bodyTypeface === bTf &&
            this.titleTypeface === tTf &&
            this.bodyColor == bCol &&
            this.titleColor == tCol &&
            this.bodySize == bSz &&
            this.titleSize == tSz &&
            this.horizontalMarginPx == horizontalMarginPx &&
            this.topMarginPx == topMarginPx &&
            this.contentWidthPx == contentWidthPx &&
            this.widthPx == widthPx &&
            this.heightPx == heightPx
        ) {
            return true
        }
        this.page = page
        this.style = style
        this.bodyTypeface = bTf
        this.titleTypeface = tTf
        this.bodyColor = bCol
        this.titleColor = tCol
        this.bodySize = bSz
        this.titleSize = tSz
        this.horizontalMarginPx = horizontalMarginPx
        this.topMarginPx = topMarginPx
        this.contentWidthPx = contentWidthPx
        this.widthPx = widthPx
        this.heightPx = heightPx
        return false
    }
}

@Composable
private fun StaticLayoutPageCanvas(
    page: ComposedPage,
    style: ReadingStyle,
    bodyPaint: TextPaint,
    titlePaint: TextPaint,
    mutedColor: Color,
    horizontalMarginPx: Int,
    topMarginPx: Int,
    contentWidthPx: Int,
    loadImageBytes: suspend (String) -> ByteArray?,
    onImageDecoded: (String) -> Unit = {},
    hiddenImageSrc: String? = null,
    onImageTapRootX: (Float) -> Unit,
    onImageLongPress: (String, Rect?) -> Unit,
    cacheTextLayer: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val latestOnTapRootX by rememberUpdatedState(onImageTapRootX)
    val latestOnLongPress by rememberUpdatedState(onImageLongPress)
    val textLayer = if (cacheTextLayer) {
        rememberGraphicsLayer()
    } else {
        null
    }
    val layerCacheKey = remember(cacheTextLayer) {
        if (cacheTextLayer) StaticTextLayerCacheKey() else null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val wPx = size.width.roundToInt()
            val hPx = size.height.roundToInt()
            if (wPx <= 0 || hPx <= 0) return@Canvas

            if (textLayer != null && layerCacheKey != null) {
                val targetSize = IntSize(wPx, hPx)
                val upToDate = layerCacheKey.matchesAndUpdate(
                    page = page,
                    style = style,
                    bodyPaint = bodyPaint,
                    titlePaint = titlePaint,
                    horizontalMarginPx = horizontalMarginPx,
                    topMarginPx = topMarginPx,
                    contentWidthPx = contentWidthPx,
                    widthPx = wPx,
                    heightPx = hPx,
                ) && textLayer.size == targetSize
                if (!upToDate) {
                    textLayer.record(size = targetSize) {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            for (el in page.elements) {
                                if (el is PageElement.TextSlice) {
                                    NativeStaticLayoutPager.drawTextSlice(
                                        canvas = nativeCanvas,
                                        slice = el,
                                        contentWidthPx = contentWidthPx,
                                        xOffset = horizontalMarginPx.toFloat(),
                                        bodyPaint = bodyPaint,
                                        titlePaint = titlePaint,
                                        style = style,
                                        yShift = topMarginPx.toFloat(),
                                    )
                                }
                            }
                        }
                    }
                }
                drawLayer(textLayer)
            } else {
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    for (el in page.elements) {
                        if (el is PageElement.TextSlice) {
                            NativeStaticLayoutPager.drawTextSlice(
                                canvas = nativeCanvas,
                                slice = el,
                                contentWidthPx = contentWidthPx,
                                xOffset = horizontalMarginPx.toFloat(),
                                bodyPaint = bodyPaint,
                                titlePaint = titlePaint,
                                style = style,
                                yShift = topMarginPx.toFloat(),
                            )
                        }
                    }
                }
            }
        }

        // Inline EPUB / local images laid out proportionally on the page
        for (el in page.elements) {
            if (el is PageElement.ImageSlice) {
                key(el.imageSrc, el.yOffset) {
                    val topDp = with(density) { (el.yOffset + topMarginPx).toDp() }
                    val heightDp = with(density) { el.height.toDp() }
                    val marginDp = with(density) { horizontalMarginPx.toDp() }
                    val coordsHolder = remember(el.imageSrc, el.yOffset) { arrayOfNulls<LayoutCoordinates>(1) }

                    var decodeDone by remember(el.imageSrc) {
                        mutableStateOf(ReaderImageCache.getBitmap(el.imageSrc) != null)
                    }
                    val bmp by produceState<ImageBitmap?>(
                        initialValue = ReaderImageCache.getBitmap(el.imageSrc),
                        el.imageSrc,
                    ) {
                        val cached = ReaderImageCache.getBitmap(el.imageSrc)
                        if (cached != null) {
                            value = cached
                            decodeDone = true
                            return@produceState
                        }
                        val decoded = withContext(Dispatchers.IO) {
                            decodeReaderImageBitmap(el.imageSrc, loadImageBytes)
                        }
                        value = decoded
                        decodeDone = true
                        if (decoded != null) {
                            onImageDecoded(el.imageSrc)
                        }
                    }

                    val isHidden = hiddenImageSrc == el.imageSrc
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = marginDp, end = marginDp, top = topDp)
                            .fillMaxWidth()
                            .height(heightDp)
                            .onGloballyPositioned { coordsHolder[0] = it }
                            .graphicsLayer { alpha = if (isHidden) 0f else 1f }
                            .clip(RoundedCornerShape(10.dp))
                            .pointerInput(el.imageSrc) {
                                detectTapGestures(
                                    onTap = { localOffset ->
                                        val coords = coordsHolder[0]?.takeIf { it.isAttached }
                                        val rootX = coords?.localToRoot(localOffset)?.x ?: localOffset.x
                                        latestOnTapRootX(rootX)
                                    },
                                    onLongPress = {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        val rect = coordsHolder[0]?.takeIf { it.isAttached }?.boundsInRoot()
                                        latestOnLongPress(el.imageSrc, rect)
                                    },
                                )
                            },
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp!!,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (!decodeDone) {
                            Text("加载插图中…", color = mutedColor, fontSize = 12.sp)
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(0.6.dp, mutedColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            ) {
                                Text("图片暂不可用", color = mutedColor.copy(alpha = 0.72f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun decodeReaderImageBitmap(
    source: String,
    loadImageBytes: suspend (String) -> ByteArray?,
): ImageBitmap? {
    ReaderImageCache.getBitmap(source)?.let { return it }
    val ownedDeferred = CompletableDeferred<ImageBitmap?>()
    val existingDeferred = ReaderImageCache.getOrRegisterInFlight(source, ownedDeferred)
    if (existingDeferred != null) {
        val awaited = existingDeferred.await()
        if (awaited != null) return awaited
        ReaderImageCache.getBitmap(source)?.let { return it }
    }
    var decodedResult: ImageBitmap? = null
    return try {
        decodedResult = withContext(Dispatchers.IO) {
            val bytes = ReaderImageCache.getBytes(source) ?: when {
                source.startsWith("sq-epub://") -> loadImageBytes(source)
                source.startsWith("data:image", ignoreCase = true) -> {
                    val comma = source.indexOf(',')
                    if (comma > 0) Base64.decode(source.substring(comma + 1), Base64.DEFAULT) else null
                }
                source.startsWith("file://") -> File(source.removePrefix("file://")).takeIf { it.exists() }?.readBytes()
                else -> File(source).takeIf { it.exists() }?.readBytes() ?: loadImageBytes(source)
            }
            if (bytes != null && bytes.isNotEmpty()) {
                ReaderImageCache.putBytes(source, bytes)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                var sampleSize = 1
                if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                    NativeStaticLayoutPager.recordImageDimensions(
                        source,
                        boundsOpts.outWidth,
                        boundsOpts.outHeight,
                    )
                    val maxDim = 1080
                    while (boundsOpts.outWidth / sampleSize > maxDim ||
                        boundsOpts.outHeight / sampleSize > maxDim
                    ) {
                        sampleSize *= 2
                    }
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    if (boundsOpts.outMimeType.equals("image/jpeg", ignoreCase = true)) {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                if (bmp != null) {
                    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                        NativeStaticLayoutPager.recordImageDimensions(source, bmp.width, bmp.height)
                    }
                    bmp.asImageBitmap().also { ReaderImageCache.putBitmap(source, it) }
                } else null
            } else null
        }
        decodedResult
    } catch (ce: CancellationException) {
        throw ce
    } catch (_: Throwable) {
        null
    } finally {
        ReaderImageCache.finishInFlight(source, ownedDeferred, decodedResult)
    }
}

private fun computeFitRect(bounds: Rect, imageWidth: Float, imageHeight: Float): Rect {
    if (bounds.width <= 0f || bounds.height <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return bounds
    }
    val containerAspect = bounds.width / bounds.height
    val imageAspect = imageWidth / imageHeight
    val fitWidth: Float
    val fitHeight: Float
    if (imageAspect > containerAspect) {
        fitWidth = bounds.width
        fitHeight = bounds.width / imageAspect
    } else {
        fitHeight = bounds.height
        fitWidth = bounds.height * imageAspect
    }
    val left = bounds.left + (bounds.width - fitWidth) * 0.5f
    val top = bounds.top + (bounds.height - fitHeight) * 0.5f
    return Rect(left = left, top = top, right = left + fitWidth, bottom = top + fitHeight)
}

@Composable
private fun FullscreenReaderImageViewer(
    viewerState: ReaderImageViewerState,
    isClosing: Boolean,
    containerRootCoordsHolder: Array<LayoutCoordinates?>,
    safeTop: Dp,
    safeBottom: Dp,
    style: ReadingStyle,
    palette: AppPalette,
    loadImageBytes: suspend (String) -> ByteArray?,
    onSaveToGallery: (ByteArray, String) -> Unit,
    onRequestClose: () -> Unit,
    onDismissed: () -> Unit,
) {
    val imageSrc = viewerState.src
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val latestOnRequestClose by rememberUpdatedState(onRequestClose)
    val latestOnDismissed by rememberUpdatedState(onDismissed)

    val expandProgress = remember(imageSrc) { Animatable(0f) }
    var scale by remember(imageSrc) { mutableFloatStateOf(1f) }
    var offset by remember(imageSrc) { mutableStateOf(Offset.Zero) }
    var dragDismissY by remember(imageSrc) { mutableFloatStateOf(0f) }
    var closeStartProgress by remember(imageSrc) { mutableFloatStateOf(1f) }
    var closeStartScale by remember(imageSrc) { mutableFloatStateOf(1f) }
    var closeStartOffset by remember(imageSrc) { mutableStateOf(Offset.Zero) }
    var closeStartDismissY by remember(imageSrc) { mutableFloatStateOf(0f) }
    var zoomAnimJob by remember(imageSrc) { mutableStateOf<Job?>(null) }
    var dismissSnapJob by remember(imageSrc) { mutableStateOf<Job?>(null) }

    var loadDone by remember(imageSrc) {
        mutableStateOf(ReaderImageCache.getBitmap(imageSrc) != null)
    }

    val bmp by produceState<ImageBitmap?>(
        initialValue = ReaderImageCache.getBitmap(imageSrc),
        imageSrc,
    ) {
        val cached = ReaderImageCache.getBitmap(imageSrc)
        if (cached != null) {
            value = cached
            loadDone = true
            return@produceState
        }
        val decoded = withContext(Dispatchers.IO) { decodeReaderImageBitmap(imageSrc, loadImageBytes) }
        value = decoded
        loadDone = true
    }

    LaunchedEffect(imageSrc, isClosing) {
        if (!isClosing) {
            expandProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 360f,
                ),
            )
        } else {
            zoomAnimJob?.cancel()
            dismissSnapJob?.cancel()
            closeStartProgress = expandProgress.value.coerceAtLeast(0.01f)
            closeStartScale = scale
            closeStartOffset = offset
            closeStartDismissY = dragDismissY
            expandProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = 420f,
                ),
            )
            latestOnDismissed()
        }
    }

    val powerSaveViewer = LocalPowerSaveMode.current
    val rawViewerBackdrop = rememberGlassBackdrop()
    val viewerBackdrop = if (powerSaveViewer) null else rawViewerBackdrop

    CompositionLocalProvider(
        LocalGlassDarkTheme provides true,
        LocalVitroBackdrop provides viewerBackdrop,
        LocalReadingStyle provides style,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val fullBounds = Rect(0f, 0f, containerWidthPx, containerHeightPx)
            val rootOrigin = containerRootCoordsHolder[0]?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
            val localSourceBox = viewerState.sourceBounds?.let {
                Rect(
                    left = it.left - rootOrigin.x,
                    top = it.top - rootOrigin.y,
                    right = it.right - rootOrigin.x,
                    bottom = it.bottom - rootOrigin.y,
                )
            }

            val loadedBmp = bmp
            val targetRect = if (loadedBmp != null) {
                computeFitRect(fullBounds, loadedBmp.width.toFloat(), loadedBmp.height.toFloat())
            } else {
                val w = containerWidthPx * 0.72f
                val h = containerHeightPx * 0.45f
                Rect(
                    left = (containerWidthPx - w) * 0.5f,
                    top = (containerHeightPx - h) * 0.5f,
                    right = (containerWidthPx + w) * 0.5f,
                    bottom = (containerHeightPx + h) * 0.5f,
                )
            }
            val sourceRect = when {
                localSourceBox != null && loadedBmp != null ->
                    computeFitRect(localSourceBox, loadedBmp.width.toFloat(), loadedBmp.height.toFloat())
                localSourceBox != null -> localSourceBox
                else -> Rect(
                    left = targetRect.left + targetRect.width * 0.175f,
                    top = targetRect.top + targetRect.height * 0.175f,
                    right = targetRect.right - targetRect.width * 0.175f,
                    bottom = targetRect.bottom - targetRect.height * 0.175f,
                )
            }

            val dismissRangePx = with(density) { 260.dp.toPx() }.coerceAtLeast(1f)
            val cornerBasePx = with(density) { 10.dp.toPx() }
            val computeControlsAlpha = {
                val rawP = expandProgress.value
                val clampedP = rawP.coerceIn(0f, 1f)
                val tClose = if (isClosing) {
                    (clampedP / closeStartProgress.coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                } else {
                    1f
                }
                val activeDismissY = if (isClosing) closeStartDismissY * tClose else dragDismissY
                val pullRatio = (abs(activeDismissY) / dismissRangePx).coerceIn(0f, 1f)
                ((clampedP - 0.45f) / 0.55f).coerceIn(0f, 1f) *
                    (1f - pullRatio * 1.4f).coerceIn(0f, 1f)
            }
            val controlsVisible by remember(isClosing, dismissRangePx) {
                derivedStateOf { computeControlsAlpha() > 0.01f }
            }
            val controlsInteractive by remember(isClosing, dismissRangePx) {
                derivedStateOf { !isClosing && computeControlsAlpha() > 0.1f }
            }
            val dynamicViewerShape = remember(imageSrc, sourceRect, targetRect, isClosing) {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        val rawP = expandProgress.value
                        val clampedP = rawP.coerceIn(0f, 1f)
                        val curW = (sourceRect.width + (targetRect.width - sourceRect.width) * rawP).coerceAtLeast(1f)
                        val morphScale = (curW / targetRect.width.coerceAtLeast(1f)).coerceAtLeast(0.15f)
                        val rPx = (cornerBasePx * (1f - clampedP)) / morphScale
                        return if (rPx <= 0.5f) {
                            Outline.Rectangle(size.toRect())
                        } else {
                            Outline.Rounded(RoundRect(size.toRect(), CornerRadius(rPx, rPx)))
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (viewerBackdrop != null) {
                            Modifier.glassBackdrop(viewerBackdrop)
                        } else {
                            Modifier
                        },
                    )
                    .drawBehind {
                        val rawP = expandProgress.value
                        val clampedP = rawP.coerceIn(0f, 1f)
                        val tClose = if (isClosing) {
                            (clampedP / closeStartProgress.coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                        val activeDismissY = if (isClosing) closeStartDismissY * tClose else dragDismissY
                        val pullRatio = (abs(activeDismissY) / dismissRangePx).coerceIn(0f, 1f)
                        val scrimAlpha = (0.94f * clampedP * (1f - 0.52f * pullRatio)).coerceIn(0f, 0.94f)
                        drawRect(Color.Black.copy(alpha = scrimAlpha))
                    }
                    .pointerInput(imageSrc, isClosing) {
                        if (isClosing) return@pointerInput
                        val dismissThresholdPx = 120.dp.toPx()
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            zoomAnimJob?.cancel()
                            dismissSnapJob?.cancel()
                            var pastSlop = false
                            var modePullDismiss = false
                            var accumPan = Offset.Zero
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.isConsumed }) break
                                val pressedCount = event.changes.count { it.pressed }
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                if (!pastSlop) {
                                    accumPan += panChange
                                    val pinchTriggered = pressedCount >= 2 &&
                                        (abs(1f - zoomChange) > 0.015f || accumPan.getDistance() > touchSlop)
                                    if (pinchTriggered || accumPan.getDistance() > touchSlop) {
                                        pastSlop = true
                                        modePullDismiss = pressedCount == 1 &&
                                            scale <= 1.02f &&
                                            abs(accumPan.y) > abs(accumPan.x) * 1.1f &&
                                            accumPan.y > 0f
                                    }
                                }
                                if (pastSlop) {
                                    if (pressedCount >= 2) {
                                        modePullDismiss = false
                                        dragDismissY = 0f
                                    }
                                    if (modePullDismiss && scale <= 1.02f) {
                                        dragDismissY = (dragDismissY + panChange.y).coerceAtLeast(-24f)
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    } else if (pressedCount >= 2 || scale > 1.02f) {
                                        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
                                        scale = nextScale
                                        if (nextScale > 1.02f) {
                                            val maxPanX = (size.width * (nextScale - 1f) * 0.55f).coerceAtLeast(0f)
                                            val maxPanY = (size.height * (nextScale - 1f) * 0.55f).coerceAtLeast(0f)
                                            val nextOffset = offset + panChange
                                            offset = Offset(
                                                x = nextOffset.x.coerceIn(-maxPanX, maxPanX),
                                                y = nextOffset.y.coerceIn(-maxPanY, maxPanY),
                                            )
                                        } else {
                                            offset = Offset.Zero
                                        }
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (modePullDismiss) {
                                if (dragDismissY >= dismissThresholdPx) {
                                    latestOnRequestClose()
                                } else if (dragDismissY != 0f) {
                                    val startY = dragDismissY
                                    dismissSnapJob?.cancel()
                                    dismissSnapJob = scope.launch {
                                        animate(
                                            initialValue = startY,
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                                        ) { value, _ ->
                                            dragDismissY = value
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(imageSrc, isClosing) {
                        if (isClosing) return@pointerInput
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                val startScale = scale
                                val startOffset = offset
                                val endScale = if (startScale > 1.05f) 1f else 2.2f
                                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                val endOffset = if (endScale <= 1.05f) {
                                    Offset.Zero
                                } else {
                                    val raw = (center - tapOffset) * 0.7f
                                    val maxPanX = (size.width * (endScale - 1f) * 0.55f).coerceAtLeast(0f)
                                    val maxPanY = (size.height * (endScale - 1f) * 0.55f).coerceAtLeast(0f)
                                    Offset(
                                        x = raw.x.coerceIn(-maxPanX, maxPanX),
                                        y = raw.y.coerceIn(-maxPanY, maxPanY),
                                    )
                                }
                                zoomAnimJob?.cancel()
                                zoomAnimJob = scope.launch {
                                    animate(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                                    ) { p, _ ->
                                        scale = startScale + (endScale - startScale) * p
                                        offset = startOffset + (endOffset - startOffset) * p
                                    }
                                }
                            },
                            onTap = { latestOnRequestClose() },
                        )
                    },
            ) {
                when {
                    loadedBmp != null -> {
                        val targetW = targetRect.width.coerceAtLeast(1f)
                        val targetH = targetRect.height.coerceAtLeast(1f)
                        val targetCenterX = targetRect.left + targetW * 0.5f
                        val targetCenterY = targetRect.top + targetH * 0.5f
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = targetRect.left.roundToInt(),
                                        y = targetRect.top.roundToInt(),
                                    )
                                }
                                .size(
                                    width = with(density) { targetW.toDp() },
                                    height = with(density) { targetH.toDp() },
                                )
                                .graphicsLayer {
                                    val rawP = expandProgress.value
                                    val clampedP = rawP.coerceIn(0f, 1f)
                                    val tClose = if (isClosing) {
                                        (clampedP / closeStartProgress.coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                                    } else {
                                        1f
                                    }
                                    val activeScale = if (isClosing) 1f + (closeStartScale - 1f) * tClose else scale
                                    val activeOffset = if (isClosing) closeStartOffset * tClose else offset
                                    val activeDismissY = if (isClosing) closeStartDismissY * tClose else dragDismissY
                                    val pullRatio = (abs(activeDismissY) / dismissRangePx).coerceIn(0f, 1f)
                                    val pullScale = if (activeDismissY != 0f) 1f - 0.14f * pullRatio else 1f
                                    val totalScale = activeScale * pullScale

                                    val curLeft = sourceRect.left + (targetRect.left - sourceRect.left) * rawP
                                    val curTop = sourceRect.top + (targetRect.top - sourceRect.top) * rawP
                                    val curRight = sourceRect.right + (targetRect.right - sourceRect.right) * rawP
                                    val curBottom = sourceRect.bottom + (targetRect.bottom - sourceRect.bottom) * rawP
                                    val curWidthPx = (curRight - curLeft).coerceAtLeast(1f)
                                    val curHeightPx = (curBottom - curTop).coerceAtLeast(1f)
                                    val curCenterX = (curLeft + curRight) * 0.5f
                                    val curCenterY = (curTop + curBottom) * 0.5f

                                    scaleX = (curWidthPx / targetW) * totalScale
                                    scaleY = (curHeightPx / targetH) * totalScale
                                    translationX = (curCenterX - targetCenterX) + activeOffset.x
                                    translationY = (curCenterY - targetCenterY) + activeOffset.y + activeDismissY
                                    if (clampedP < 0.995f) {
                                        clip = true
                                        shape = dynamicViewerShape
                                    } else {
                                        clip = false
                                    }
                                },
                        ) {
                            Image(
                                bitmap = loadedBmp,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    !loadDone -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .graphicsLayer { alpha = expandProgress.value.coerceIn(0f, 1f) },
                        )
                    }
                    else -> {
                        Text(
                            text = "无法加载原图",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .graphicsLayer { alpha = expandProgress.value.coerceIn(0f, 1f) },
                        )
                    }
                }
            }

            if (controlsVisible) {
                val closeInteraction = remember { MutableInteractionSource() }
                val closeGlassState = rememberSafeGlassEffectState(
                    interactionSource = closeInteraction,
                    active = controlsInteractive,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = (safeTop - 6.dp).coerceAtLeast(0.dp))
                        .graphicsLayer { alpha = computeControlsAlpha() }
                        .then(
                            if (!LocalPowerSaveMode.current) {
                                Modifier.glassEffectContainer(spacing = 0.dp)
                            } else {
                                Modifier
                            },
                        )
                        .padding(16.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .ios27GlassSurface(
                                shape = CircleShape,
                                palette = palette,
                                style = style,
                                variant = Ios27GlassVariant.ReaderControl,
                                backdrop = viewerBackdrop,
                                isInteractive = controlsInteractive,
                                state = closeGlassState,
                                interactionSource = closeInteraction,
                                showContrastBorder = false,
                                darkPaperOverride = true,
                                showFingerHighlight = false,
                            )
                            .clickable(
                                interactionSource = closeInteraction,
                                indication = null,
                                enabled = controlsInteractive,
                                onClick = { latestOnRequestClose() },
                            ),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                    }
                }

                if (loadedBmp != null) {
                    val saveInteraction = remember { MutableInteractionSource() }
                    val saveGlassState = rememberSafeGlassEffectState(
                        interactionSource = saveInteraction,
                        active = controlsInteractive,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = (safeBottom - 6.dp).coerceAtLeast(0.dp))
                            .graphicsLayer { alpha = computeControlsAlpha() }
                            .then(
                                if (!LocalPowerSaveMode.current) {
                                    Modifier.glassEffectContainer(spacing = 0.dp)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .ios27GlassSurface(
                                    shape = RoundedCornerShape(24.dp),
                                    palette = palette,
                                    style = style,
                                    variant = Ios27GlassVariant.ReaderControl,
                                    backdrop = viewerBackdrop,
                                    isInteractive = controlsInteractive,
                                    state = saveGlassState,
                                    interactionSource = saveInteraction,
                                    showContrastBorder = false,
                                    darkPaperOverride = true,
                                    showFingerHighlight = false,
                                )
                                .clickable(
                                    interactionSource = saveInteraction,
                                    indication = null,
                                    enabled = controlsInteractive,
                                ) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    scopeSaveImage(scope, imageSrc, loadImageBytes, onSaveToGallery)
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SaveAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "保存原图到相册",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun scopeSaveImage(
    scope: CoroutineScope,
    imageSrc: String,
    loadImageBytes: suspend (String) -> ByteArray?,
    onSaveToGallery: (ByteArray, String) -> Unit,
) {
    val cleanSrc = imageSrc.substringBefore('?').substringBefore('#')
    val ext = cleanSrc.substringAfterLast('.', "jpg").lowercase().take(4).ifBlank { "jpg" }
    val cached = ReaderImageCache.getBytes(imageSrc)
    if (cached != null) {
        onSaveToGallery(cached, ext)
        return
    }
    scope.launch(Dispatchers.IO) {
        val loaded = loadImageBytes(imageSrc)
        if (loaded != null && loaded.isNotEmpty()) {
            ReaderImageCache.putBytes(imageSrc, loaded)
            withContext(Dispatchers.Main) {
                onSaveToGallery(loaded, ext)
            }
        }
    }
}

internal fun computeOverallReaderProgress(
    displayChapterIndex: Int,
    currentPageIndex: Int,
    pageCount: Int,
    chapterCount: Int,
    isScrollAtBookEnd: Boolean = false,
): Double {
    if (chapterCount <= 0) return 0.0
    if (isScrollAtBookEnd) return 1.0
    val safeChapter = displayChapterIndex.coerceIn(0, chapterCount - 1)
    val safePageCount = pageCount.coerceAtLeast(1)
    val safePage = currentPageIndex.coerceIn(0, safePageCount - 1)
    val isLastChapter = safeChapter >= chapterCount - 1
    if (isLastChapter && safePage >= safePageCount - 1) return 1.0
    if (chapterCount == 1) {
        return if (safePageCount <= 1) 1.0 else (safePage.toDouble() / (safePageCount - 1).toDouble()).coerceIn(0.0, 1.0)
    }
    val chapterFrac = if (safePageCount <= 1) 0.0 else safePage.toDouble() / (safePageCount - 1).toDouble()
    return ((safeChapter.toDouble() + chapterFrac) / chapterCount.toDouble()).coerceIn(0.0, 1.0)
}
