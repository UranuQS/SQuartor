package com.squartor.reader.ui.screens

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Immutable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListPrefetchScope
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridPrefetchScope
import androidx.compose.foundation.lazy.grid.LazyGridPrefetchStrategy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squartor.reader.AmbientSubpageBackground
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.DEFAULT_SHELF_LABEL
import com.squartor.reader.model.DEFAULT_SHELF_NAME
import com.squartor.reader.model.ShelfBookBlock
import com.squartor.reader.model.cleanBookTitle
import com.squartor.reader.ui.glass.CoverAssetCache
import com.squartor.reader.ui.glass.Ios27GlassVariant
import com.squartor.reader.ui.glass.IsolatedAmbientGlassContainer
import com.squartor.reader.ui.glass.LiquidGlassDialog
import com.squartor.reader.ui.glass.LocalBookLaunchTracker
import com.squartor.reader.ui.glass.LocalHeroBookSlotAlpha
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.VitroGlassPopoverItem
import com.squartor.reader.ui.glass.VitroGlassPopoverMenu
import com.squartor.reader.ui.glass.ios27FilterPillTints
import com.squartor.reader.ui.glass.ios27GlassSurface
import com.squartor.reader.ui.glass.rememberSafeGlassEffectState
import com.squartor.reader.ui.glass.unclippedBoundsInRoot
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassEffectState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

internal fun frostedSurfaceColor(palette: AppPalette): Color =
    if (palette.isLight) Color.White.copy(alpha = 0.68f)
    else Color(0xFF202026).copy(alpha = 0.52f)

internal fun frostedPillColor(palette: AppPalette): Color =
    if (palette.isLight) Color.White.copy(alpha = 0.76f)
    else Color(0xFF26262E).copy(alpha = 0.62f)

internal fun frostedModalColor(palette: AppPalette): Color =
    if (palette.isLight) Color(0xFFF7F7FA).copy(alpha = 0.96f)
    else Color(0xFF1A1A20).copy(alpha = 0.95f)

private enum class ShelfViewMode(val label: String) {
    Grid3("三列宫格"),
    ListView("列表视图"),
}

private enum class ShelfSortMode(val label: String) {
    Recent("按最近阅读"),
    Title("按书名排序"),
    Imported("按导入时间"),
    Progress("按阅读进度"),
}

private object SpineCreaseBitmapCache {
    private var cachedLightBitmap: ImageBitmap? = null
    private var cachedDarkBitmap: ImageBitmap? = null

    fun get(isLight: Boolean): ImageBitmap {
        val existing = if (isLight) cachedLightBitmap else cachedDarkBitmap
        if (existing != null) return existing
        val width = 64
        val bmp = android.graphics.Bitmap.createBitmap(width, 1, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val colors = if (isLight) {
            intArrayOf(
                android.graphics.Color.argb(31, 0, 0, 0),
                android.graphics.Color.argb(41, 255, 255, 255),
                android.graphics.Color.TRANSPARENT,
            )
        } else {
            intArrayOf(
                android.graphics.Color.argb(61, 0, 0, 0),
                android.graphics.Color.argb(18, 255, 255, 255),
                android.graphics.Color.TRANSPARENT,
            )
        }
        val paint = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                0f,
                0f,
                width.toFloat(),
                0f,
                colors,
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 1f, paint)
        return bmp.asImageBitmap().also {
            if (isLight) cachedLightBitmap = it else cachedDarkBitmap = it
        }
    }
}

private object SeriesVolumeMoreBadgeCache {
    private var cachedPx = -1
    private var cachedBitmap: ImageBitmap? = null

    fun get(sizePx: Int, strokePx: Float): ImageBitmap {
        val clampedPx = sizePx.coerceAtLeast(16)
        cachedBitmap?.let { existing ->
            if (cachedPx == clampedPx) return existing
        }
        val bmp = android.graphics.Bitmap.createBitmap(
            clampedPx,
            clampedPx,
            android.graphics.Bitmap.Config.ARGB_8888,
        )
        val canvas = android.graphics.Canvas(bmp)
        val half = clampedPx * 0.5f
        val radius = (half - strokePx * 0.5f).coerceAtLeast(1f)
        val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.FILL
            color = android.graphics.Color.argb((0.62f * 255f).roundToInt(), 0x0E, 0x10, 0x16)
        }
        canvas.drawCircle(half, half, radius, fillPaint)
        val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = strokePx
            color = android.graphics.Color.argb((0.22f * 255f).roundToInt(), 0xFF, 0xFF, 0xFF)
        }
        canvas.drawCircle(half, half, radius, strokePaint)
        val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.FILL
            color = android.graphics.Color.WHITE
        }
        val dotRadius = clampedPx * (1.25f / 24f)
        val dotSpread = clampedPx * (4.1f / 24f)
        canvas.drawCircle(half - dotSpread, half, dotRadius, dotPaint)
        canvas.drawCircle(half, half, dotRadius, dotPaint)
        canvas.drawCircle(half + dotSpread, half, dotRadius, dotPaint)
        return bmp.asImageBitmap().also {
            cachedPx = clampedPx
            cachedBitmap = it
        }
    }
}

private val SHELF_GRID_MAIN_SHAPE = RoundedCornerShape(12.dp)
private val SHELF_CARD_SHAPE = RoundedCornerShape(20.dp)
private val SHELF_LIST_COVER_SHAPE = RoundedCornerShape(8.dp)
private val SERIES_TILE_COVER_SHAPE = RoundedCornerShape(12.dp)
private val SERIES_VOLUME_ROW_SHAPE = RoundedCornerShape(14.dp)

private object CoverBitmapCache {
    private val cache = object : LruCache<String, ImageBitmap>(192) {}

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, bitmap: ImageBitmap) {
        synchronized(cache) { cache.put(key, bitmap) }
    }

    fun trimMemory(level: Int) {
        if (level < 15) return
        synchronized(cache) {
            when {
                level >= 40 -> cache.evictAll()
                level >= 20 -> cache.trimToSize(48)
                else -> cache.trimToSize(96)
            }
        }
    }
}

private class SeriesModalLiveHolder {
    @Volatile
    var boundsT: Float = 0f

    @Volatile
    var contentAlpha: Float = 0f

    @Volatile
    var collapsedToGrid: Boolean = false

    @Volatile
    var lastSeriesKey: String? = null

    @Volatile
    var lastStackRect: Rect? = null

    val activeOverlayKeyState = mutableStateOf<String?>(null)

    val openTileRects = mutableMapOf<String, Rect>()
}

@Composable
private fun IsolatedRecomposeScope(content: @Composable () -> Unit) {
    content()
}

@Composable
private fun LazyGridItemScope.ShelfPlacementGridItemBox(
    suppressPlacementState: State<Boolean>,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.animateItem(
            placementSpec = spring(
                dampingRatio = 0.88f,
                stiffness = 480f,
            ),
        ),
    ) {
        content()
    }
}

@Composable
private fun LazyItemScope.ShelfPlacementListItemBox(
    suppressPlacementState: State<Boolean>,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.animateItem(
            placementSpec = spring(
                dampingRatio = 0.88f,
                stiffness = 480f,
            ),
        ),
    ) {
        content()
    }
}

private fun smoothstepSeries(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun lerpRectBounds(a: Rect, b: Rect, t: Float): Rect {
    val f = t.coerceIn(0f, 1f)
    return Rect(
        left = lerpSeriesFloat(a.left, b.left, f),
        top = lerpSeriesFloat(a.top, b.top, f),
        right = lerpSeriesFloat(a.right, b.right, f),
        bottom = lerpSeriesFloat(a.bottom, b.bottom, f),
    )
}

internal fun trimShelfCoverCache(level: Int) {
    CoverBitmapCache.trimMemory(level)
}

@Composable
fun BookCover(
    book: BookEntry,
    palette: AppPalette,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
    height: Dp = 136.dp,
    cornerRadius: Dp = 12.dp,
) {
    val coverPath = book.coverPath?.takeIf { it.isNotBlank() }
    val initialCached = remember(book.id, coverPath) {
        CoverAssetCache.getThumb(book.id) ?: coverPath?.let { CoverBitmapCache.get(it) }
    }

    if (coverPath == null) {
        BookCoverFrame(
            palette = palette,
            modifier = modifier,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            showPlaceholderBacking = true,
        ) {
            TypographicCoverPlaceholder(
                book = book,
                palette = palette,
                isCompact = width < 72.dp || height < 96.dp,
            )
        }
        return
    }

    if (initialCached != null) {
        BookCoverFrame(
            palette = palette,
            modifier = modifier,
            width = width,
            height = height,
            cornerRadius = cornerRadius,
            showPlaceholderBacking = false,
        ) {
            Image(
                bitmap = initialCached,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    AsyncBookCoverContent(
        book = book,
        coverPath = coverPath,
        palette = palette,
        modifier = modifier,
        width = width,
        height = height,
        cornerRadius = cornerRadius,
    )
}

@Composable
private fun BookCoverFrame(
    palette: AppPalette,
    modifier: Modifier,
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
    showPlaceholderBacking: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLight = palette.isLight
    val clipModifier = if (cornerRadius > 0.dp) {
        val shape = when (cornerRadius) {
            12.dp -> SHELF_GRID_MAIN_SHAPE
            8.dp -> SHELF_LIST_COVER_SHAPE
            else -> remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
        }
        Modifier.clip(shape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .then(clipModifier)
            .then(
                if (showPlaceholderBacking) {
                    Modifier.background(
                        if (palette.isLight) Color(0xFFEAE8E3) else Color(0xFF1F1F25),
                    )
                } else {
                    Modifier
                },
            )
            .drawWithContent {
                drawContent()
                val w = size.width
                val creaseW = 6.dp.toPx().coerceAtMost(w)
                if (w > 0.5f && creaseW > 0.5f) {
                    drawImage(
                        image = SpineCreaseBitmapCache.get(isLight),
                        dstSize = androidx.compose.ui.unit.IntSize(
                            width = creaseW.roundToInt().coerceAtLeast(1),
                            height = size.height.roundToInt().coerceAtLeast(1),
                        ),
                    )
                }
            },
        content = content,
    )
}

@Composable
private fun AsyncBookCoverContent(
    book: BookEntry,
    coverPath: String,
    palette: AppPalette,
    modifier: Modifier,
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
) {
    val density = LocalDensity.current
    val targetWidthPx = with(density) { width.roundToPx().coerceIn(96, 720) }
    val targetHeightPx = with(density) { height.roundToPx().coerceIn(136, 960) }
    var loadFailed by remember(book.id, coverPath) { mutableStateOf(false) }

    val syncCached = remember(book.id, coverPath) {
        CoverAssetCache.getThumb(book.id)
            ?: CoverBitmapCache.get(coverPath)?.also {
                CoverAssetCache.putThumbIfAbsent(book.id, coverPath, it)
            }
    }
    val loaded by produceState<ImageBitmap?>(initialValue = syncCached, book.id, coverPath) {
        val globalCached = CoverAssetCache.getThumb(book.id)
        if (globalCached != null) {
            loadFailed = false
            value = globalCached
            return@produceState
        }
        val cached = CoverBitmapCache.get(coverPath)
        if (cached != null) {
            CoverAssetCache.putThumbIfAbsent(book.id, coverPath, cached)
            loadFailed = false
            value = cached
            return@produceState
        }
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(coverPath)
                if (!file.exists() || !file.isFile) return@runCatching null
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return@runCatching null
                val minSampleWidthPx = (targetWidthPx * 0.82f).toInt().coerceAtLeast(1)
                val minSampleHeightPx = (targetHeightPx * 0.82f).toInt().coerceAtLeast(1)
                var sampleSize = 1
                while (
                    boundsOpts.outWidth / (sampleSize * 2) >= minSampleWidthPx &&
                    boundsOpts.outHeight / (sampleSize * 2) >= minSampleHeightPx
                ) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                val rawDecoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    ?: return@runCatching null
                val maxCapW = (targetWidthPx * 1.2f).roundToInt().coerceAtLeast(120)
                val maxCapH = (targetHeightPx * 1.2f).roundToInt().coerceAtLeast(168)
                val boundedBmp = if (rawDecoded.width > maxCapW || rawDecoded.height > maxCapH) {
                    val scale = minOf(
                        maxCapW.toFloat() / rawDecoded.width.coerceAtLeast(1),
                        maxCapH.toFloat() / rawDecoded.height.coerceAtLeast(1),
                    )
                    val dstW = (rawDecoded.width * scale).roundToInt().coerceAtLeast(1)
                    val dstH = (rawDecoded.height * scale).roundToInt().coerceAtLeast(1)
                    android.graphics.Bitmap.createScaledBitmap(rawDecoded, dstW, dstH, true).also {
                        if (it !== rawDecoded) rawDecoded.recycle()
                    }
                } else {
                    rawDecoded
                }
                boundedBmp.asImageBitmap().also {
                    CoverBitmapCache.put(coverPath, it)
                    CoverAssetCache.putThumbIfAbsent(book.id, coverPath, it)
                }
            }.getOrNull()
        }
        if (decoded != null) {
            loadFailed = false
            value = decoded
        } else {
            loadFailed = true
            value = null
        }
    }

    val isLoading = loaded == null && !loadFailed
    val coverAlpha = remember(book.id, coverPath) {
        Animatable(if (syncCached != null) 1f else 0f)
    }
    LaunchedEffect(book.id, coverPath, loaded != null) {
        if (loaded != null) {
            if (coverAlpha.value < 0.999f) {
                coverAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                )
            }
        } else if (coverAlpha.value != 0f) {
            coverAlpha.snapTo(0f)
        }
    }
    val effectiveAlpha = coverAlpha.value
    val showPlaceholderBacking = loadFailed || loaded == null || effectiveAlpha < 0.999f

    BookCoverFrame(
        palette = palette,
        modifier = modifier,
        width = width,
        height = height,
        cornerRadius = cornerRadius,
        showPlaceholderBacking = showPlaceholderBacking,
    ) {
        if (loadFailed) {
            TypographicCoverPlaceholder(
                book = book,
                palette = palette,
                isCompact = width < 72.dp || height < 96.dp,
            )
        } else {
            if (isLoading || effectiveAlpha < 0.999f) {
                CoverSkeletonShimmer(palette = palette)
            }
            loaded?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (effectiveAlpha < 0.999f) {
                                Modifier.graphicsLayer { alpha = effectiveAlpha }
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun CoverSkeletonShimmer(palette: AppPalette) {
    val baseColor = if (palette.isLight) Color(0xFFEBE9E4) else Color(0xFF22222A)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseColor),
    )
}

@Composable
private fun TypographicCoverPlaceholder(
    book: BookEntry,
    palette: AppPalette,
    isCompact: Boolean,
) {
    val displayTitle = remember(book.title) { cleanBookTitle(book.title) }
    val isSingleAsciiToken = remember(displayTitle) {
        displayTitle.isNotEmpty() && displayTitle.none { it.isWhitespace() || it.code > 127 }
    }
    val bgTop = if (palette.isLight) {
        Color(0xFFFAF8F5)
    } else {
        Color(0xFF25252C)
    }
    val bgBottom = if (palette.isLight) {
        Color(0xFFEDEAE4)
    } else {
        Color(0xFF1B1B21)
    }
    val spineColor = if (palette.isLight) {
        palette.primary.copy(alpha = 0.82f)
    } else {
        palette.accentText.copy(alpha = 0.76f)
    }
    val spineWidth = if (isCompact) 5.dp else 8.dp
    val coverBgBrush = remember(bgTop, bgBottom) {
        Brush.linearGradient(listOf(bgTop, bgBottom))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(coverBgBrush),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(spineWidth)
                .background(spineColor),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = spineWidth + (if (isCompact) 5.dp else 8.dp),
                    top = if (isCompact) 8.dp else 12.dp,
                    end = if (isCompact) 5.dp else 8.dp,
                    bottom = if (isCompact) 6.dp else 10.dp,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = displayTitle,
                color = palette.text.copy(alpha = 0.92f),
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                lineHeight = if (isCompact) 13.5.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (isCompact) 2 else if (isSingleAsciiToken) 1 else 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (!isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = book.author.ifBlank { "佚名" },
                        color = palette.muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = book.formatLabel,
                        color = palette.accentText.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private val SERIES_SUFFIX_PATTERNS = listOf(
    Regex("""[\s\-_:：·.]*[(（\[]?\s*(?:第|卷|vol(?:ume)?\.?\s*)\s*[\d一二三四五六七八九十百千万零〇两]+\s*(?:卷|册|集|话|章|部)?\s*[)）\]]?\s*$""", RegexOption.IGNORE_CASE),
    Regex("""[\s\-_:：·.]*第?\s*[\d一二三四五六七八九十百千万零〇两]+\s*(?:卷|册|集)\s*(?:番外|外传|外傳|短篇|特典|if线|if線|bd|dvd).*$""", RegexOption.IGNORE_CASE),
    Regex("""[\s\-_:：·.]+(?:番外|外传|外傳|短篇|特典|if线|if線|后日谈|後日談).*$""", RegexOption.IGNORE_CASE),
    Regex("""[\s\-_:：·.]+[\d一二三四五六七八九十百千万零〇两]{1,4}\s*(?:[(（][^)）]*[)）])?\s*$""", RegexOption.IGNORE_CASE),
)

private val VOLUME_EXTRACT_PATTERNS = listOf(
    Regex("""(?:第|卷|vol(?:ume)?\.?\s*)\s*([0-9]+)""", RegexOption.IGNORE_CASE),
    Regex("""([0-9]+)\s*(?:卷|册|集)\s*$""", RegexOption.IGNORE_CASE),
    Regex("""(?:第|卷)\s*([一二三四五六七八九十百千万零〇两]+)\s*(?:卷|册|集)?"""),
    Regex("""([一二三四五六七八九十百千万零〇两]+)\s*(?:卷|册|集)\s*$"""),
    Regex("""[\s\-_:：·.]([0-9]{1,3})\s*$"""),
)

private fun chineseDigitValue(ch: Char): Int = when (ch) {
    '零', '〇' -> 0
    '一', '壹' -> 1
    '二', '两', '兩', '贰', '貳' -> 2
    '三', '叁', '參' -> 3
    '四', '肆' -> 4
    '五', '伍' -> 5
    '六', '陆', '陸' -> 6
    '七', '柒' -> 7
    '八', '捌' -> 8
    '九', '玖' -> 9
    else -> -1
}

private fun chineseUnitValue(ch: Char): Int = when (ch) {
    '十', '拾' -> 10
    '百', '佰' -> 100
    '千', '仟' -> 1000
    '万', '萬' -> 10000
    else -> -1
}

internal fun parseChineseInt(raw: String): Int? {
    if (raw.isEmpty()) return null
    raw.toIntOrNull()?.let { return it }
    var total = 0
    var current = 0
    var hasMatched = false
    for (ch in raw) {
        if (ch == '廿') {
            hasMatched = true
            total += 20
            current = 0
            continue
        }
        val d = chineseDigitValue(ch)
        if (d >= 0) {
            current = d
            hasMatched = true
            continue
        }
        val u = chineseUnitValue(ch)
        if (u > 0) {
            hasMatched = true
            val mult = if (current == 0) 1 else current
            total += mult * u
            current = 0
        }
    }
    total += current
    return if (hasMatched && total > 0) total else null
}

private fun extractVolumeNumber(title: String): Int? {
    val cleaned = cleanBookTitle(title)
    for (pattern in VOLUME_EXTRACT_PATTERNS) {
        val match = pattern.find(cleaned) ?: continue
        val group = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: continue
        parseChineseInt(group)?.let { return it }
    }
    return null
}

private val MULTI_SPACE_REGEX = Regex("""\s+""")
private val SERIES_KEY_STRIP_REGEX = Regex("""[\s\-_·・:：,，.。!！?？\[\]【】()（）]+""")

private fun normalizeSeriesBaseTitle(rawTitle: String): String {
    val cleaned = cleanBookTitle(rawTitle).trim()
    var s = cleaned
    for (regex in SERIES_SUFFIX_PATTERNS) {
        s = s.replace(regex, "").trim()
    }
    s = s.replace(MULTI_SPACE_REGEX, " ").trim()
    return s.ifEmpty { cleaned }
}

private fun normalizeSeriesKey(baseTitle: String): String =
    baseTitle.lowercase()
        .replace(SERIES_KEY_STRIP_REGEX, "")

fun buildShelfBookBlocks(books: List<BookEntry>): List<ShelfBookBlock> {
    if (books.isEmpty()) return emptyList()

    val groups = linkedMapOf<String, MutableList<BookEntry>>()
    val groupTitles = linkedMapOf<String, String>()
    val ungroupedBooks = mutableListOf<BookEntry>()
    val cleanTitleCache = HashMap<String, String>(books.size * 2)
    val volumeCache = HashMap<String, Int?>(books.size * 2)

    fun cachedCleanTitle(book: BookEntry): String =
        cleanTitleCache.getOrPut(book.id) { cleanBookTitle(book.title) }

    fun cachedVolume(book: BookEntry): Int? =
        if (volumeCache.containsKey(book.id)) volumeCache[book.id]
        else extractVolumeNumber(book.title).also { volumeCache[book.id] = it }

    for (book in books) {
        val rawOverride = book.seriesOverride?.trim()?.takeIf { it.isNotEmpty() }
        if (rawOverride == "__ungrouped__") {
            ungroupedBooks.add(book)
            continue
        }
        val baseTitle = if (rawOverride != null) {
            normalizeSeriesBaseTitle(rawOverride)
        } else {
            normalizeSeriesBaseTitle(book.title)
        }
        val normalizedKey = normalizeSeriesKey(baseTitle)
        val key = "series::$normalizedKey"
        groups.getOrPut(key) { mutableListOf() }.add(book)
        if (rawOverride != null && baseTitle == rawOverride) {
            groupTitles[key] = rawOverride
        } else {
            groupTitles.putIfAbsent(key, baseTitle)
        }
    }

    val comparator = Comparator<BookEntry> { a, b ->
        val ao = a.seriesOrder
        val bo = b.seriesOrder
        if (ao != null && bo != null && ao != bo) return@Comparator ao.compareTo(bo)
        if (ao != null || bo != null) return@Comparator if (ao != null) -1 else 1
        val av = cachedVolume(a)
        val bv = cachedVolume(b)
        if (av != null && bv != null && av != bv) return@Comparator av.compareTo(bv)
        if (av != null || bv != null) return@Comparator if (av != null) -1 else 1
        cachedCleanTitle(a).compareTo(cachedCleanTitle(b))
    }

    val blocks = mutableListOf<ShelfBookBlock>()

    for ((key, groupBooks) in groups) {
        if (groupBooks.size > 1 || groupBooks.first().seriesOverride != null) {
            val sorted = if (groupBooks.size > 1) groupBooks.sortedWith(comparator) else groupBooks
            val title = if (sorted.size > 1) {
                groupTitles[key] ?: normalizeSeriesBaseTitle(sorted.first().title)
            } else {
                cachedCleanTitle(sorted.first())
            }
            val author = sorted.firstOrNull { it.author.isNotBlank() }?.author ?: "未知作者"
            blocks.add(
                ShelfBookBlock(
                    key = key,
                    title = title,
                    author = author,
                    books = sorted,
                ),
            )
        } else {
            val single = groupBooks.first()
            blocks.add(
                ShelfBookBlock(
                    key = "single::${single.id}",
                    title = cachedCleanTitle(single),
                    author = single.author,
                    books = listOf(single),
                ),
            )
        }
    }

    for (single in ungroupedBooks) {
        blocks.add(
            ShelfBookBlock(
                key = "single::${single.id}",
                title = cachedCleanTitle(single),
                author = single.author,
                books = listOf(single),
            ),
        )
    }

    return blocks
}

internal fun refreshShelfBlocksIfStructureUnchanged(
    prevBooks: List<BookEntry>,
    newBooks: List<BookEntry>,
    prevBlocks: List<ShelfBookBlock>,
): List<ShelfBookBlock>? {
    if (prevBooks === newBooks) return prevBlocks
    if (prevBooks.size != newBooks.size) return null
    for (i in prevBooks.indices) {
        val a = prevBooks[i]
        val b = newBooks[i]
        if (a.id != b.id ||
            a.title != b.title ||
            a.author != b.author ||
            a.seriesOverride != b.seriesOverride ||
            a.seriesOrder != b.seriesOrder
        ) {
            return null
        }
    }
    if (newBooks.isEmpty()) return prevBlocks
    val byId = HashMap<String, BookEntry>(newBooks.size * 2)
    for (book in newBooks) {
        byId[book.id] = book
    }
    var anyBlockChanged = false
    val updated = ArrayList<ShelfBookBlock>(prevBlocks.size)
    for (block in prevBlocks) {
        var blockChanged = false
        for (oldBook in block.books) {
            if (byId[oldBook.id] != oldBook) {
                blockChanged = true
                break
            }
        }
        if (!blockChanged) {
            updated.add(block)
        } else {
            anyBlockChanged = true
            updated.add(
                block.copy(
                    books = block.books.map { oldBook -> byId[oldBook.id] ?: oldBook },
                ),
            )
        }
    }
    return if (anyBlockChanged) updated else prevBlocks
}

@Immutable
private data class ShelfTabSwitchKey(
    val tabIndex: Int,
    val selectedTab: String,
    val viewMode: ShelfViewMode,
    val isEmpty: Boolean,
)

@Immutable
data class ShelfBatchActions(
    val onSelectAll: () -> Unit,
    val onMoveToShelf: () -> Unit,
    val onMergeSeries: () -> Unit,
    val onDelete: () -> Unit,
    val onExit: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
private class TransitionGatedGridPrefetchStrategy(
    private val isBusy: () -> Boolean,
    private val delegate: LazyGridPrefetchStrategy = LazyGridPrefetchStrategy(),
) : LazyGridPrefetchStrategy {
    override val prefetchScheduler: PrefetchScheduler?
        get() = delegate.prefetchScheduler

    override fun LazyGridPrefetchScope.onScroll(delta: Float, layoutInfo: LazyGridLayoutInfo) {
        if (!isBusy()) with(delegate) { onScroll(delta, layoutInfo) }
    }

    override fun LazyGridPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyGridLayoutInfo) {
        if (!isBusy()) with(delegate) { onVisibleItemsUpdated(layoutInfo) }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        if (!isBusy()) with(delegate) { onNestedPrefetch(firstVisibleItemIndex) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class TransitionGatedListPrefetchStrategy(
    private val isBusy: () -> Boolean,
    private val delegate: LazyListPrefetchStrategy = LazyListPrefetchStrategy(),
) : LazyListPrefetchStrategy {
    override val prefetchScheduler: PrefetchScheduler?
        get() = delegate.prefetchScheduler

    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        if (!isBusy()) with(delegate) { onScroll(delta, layoutInfo) }
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
        if (!isBusy()) with(delegate) { onVisibleItemsUpdated(layoutInfo) }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        if (!isBusy()) with(delegate) { onNestedPrefetch(firstVisibleItemIndex) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ShelfScreen(
    books: List<BookEntry>,
    shelves: List<String>,
    palette: AppPalette,
    onOpenBook: (BookEntry) -> Unit,
    onImportFilesClick: () -> Unit,
    onImportFolderClick: () -> Unit,
    onDeleteBooks: (Set<String>) -> Unit,
    onMoveBooksToShelf: (Set<String>, String?) -> Unit,
    onGroupSeries: (Set<String>, String) -> Unit,
    onRemoveFromSeries: (String) -> Unit,
    onUpdateBookMeta: (String, String, String) -> Unit,
    onAddShelf: (String) -> Unit,
    onDeleteShelf: (String) -> Unit,
    onSelectionModeChanged: (Boolean) -> Unit = {},
    onBatchStateChanged: (active: Boolean, allSelected: Boolean, hasSelection: Boolean) -> Unit = { _, _, _ -> },
    onRegisterBatchActions: ((ShelfBatchActions?) -> Unit)? = null,
    onSeriesSheetOpenChanged: (Boolean) -> Unit = {},
    onSeriesSheetRenderedChanged: (Boolean) -> Unit = {},
    accelerateSeriesClose: Boolean = false,
    accelerateSeriesCloseState: State<Boolean>? = null,
    isReaderOverlayActive: Boolean = false,
    isReaderOverlayActiveState: State<Boolean>? = null,
    suspendChromeBlurState: State<Boolean>? = null,
    onRegisterSeriesDismiss: ((() -> Unit)?) -> Unit = {},
    externalSearchToken: Int = 0,
    externalCreateShelfToken: Int = 0,
    externalEnterBatchToken: Int = 0,
    isActive: Boolean = true,
    isVisible: Boolean = true,
    isVisibleState: State<Boolean>? = null,
    sharedBackdrop: GlassBackdrop? = null,
    requireDockBackdrop: Boolean = false,
    requireDockBackdropState: State<Boolean>? = null,
    onRegisterBoundsResolver: (((String) -> Rect?) -> Unit)? = null,
    onRegisterClosingAlphaResolver: (((String) -> Float) -> Unit)? = null,
    onRegisterClipBoundsResolver: (((String) -> Rect?) -> Unit)? = null,
) {
    val latestAccelerateSeriesCloseParam = rememberUpdatedState(accelerateSeriesClose)
    val resolvedAccelerateSeriesCloseState = remember(accelerateSeriesCloseState) {
        accelerateSeriesCloseState ?: latestAccelerateSeriesCloseParam
    }
    val latestIsReaderOverlayActiveParam = rememberUpdatedState(isReaderOverlayActive)
    val resolvedIsReaderOverlayActiveState = remember(isReaderOverlayActiveState) {
        isReaderOverlayActiveState ?: latestIsReaderOverlayActiveParam
    }
    val latestSuspendChromeBlurParam = rememberUpdatedState(false)
    val resolvedSuspendChromeBlurState = remember(suspendChromeBlurState) {
        suspendChromeBlurState ?: latestSuspendChromeBlurParam
    }
    val latestIsVisibleParam = rememberUpdatedState(isVisible)
    val resolvedIsVisibleState = remember(isVisibleState) {
        isVisibleState ?: latestIsVisibleParam
    }
    val latestRequireDockBackdropParam = rememberUpdatedState(requireDockBackdrop)
    val resolvedRequireDockBackdropState = remember(requireDockBackdropState) {
        requireDockBackdropState ?: latestRequireDockBackdropParam
    }
    val haptics = LocalHapticFeedback.current
    val readingStyle = LocalReadingStyle.current
    val launchTracker = LocalBookLaunchTracker.current
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val powerSave = LocalPowerSaveMode.current
    val rawScreenBackdrop = sharedBackdrop ?: rememberGlassBackdrop()
    val screenBackdrop = if (powerSave) null else rawScreenBackdrop

    var selectedTab by rememberSaveable { mutableStateOf(DEFAULT_SHELF_NAME) }
    var viewModeName by rememberSaveable { mutableStateOf(ShelfViewMode.Grid3.name) }
    val viewMode = remember(viewModeName) {
        ShelfViewMode.entries.firstOrNull { it.name == viewModeName } ?: ShelfViewMode.Grid3
    }
    var sortMode by rememberSaveable { mutableStateOf(ShelfSortMode.Recent) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedBookIds by remember { mutableStateOf(setOf<String>()) }
    var forceSelectionMode by remember { mutableStateOf(false) }
    val isSelectionMode = forceSelectionMode || selectedBookIds.isNotEmpty()
    val searchSlotHeightDp = 60.dp
    val targetSearchVisible = searchExpanded && !isSelectionMode
    val searchExpandAnim = remember { Animatable(if (targetSearchVisible) 1f else 0f) }
    LaunchedEffect(targetSearchVisible, isActive) {
        if (!isActive && !targetSearchVisible) {
            searchExpandAnim.snapTo(0f)
        } else {
            searchExpandAnim.animateTo(
                targetValue = if (targetSearchVisible) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = if (targetSearchVisible) 440f else 480f,
                    visibilityThreshold = 0.001f,
                ),
            )
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortPopoverPresenting by remember { mutableStateOf(false) }
    var morePopoverPresenting by remember { mutableStateOf(false) }
    // Continuous 0..1 presence of the header [≡]/[⋯] orbs driven by each popover's own handoff curve
    // (read only in the draw phase, never recomposes the header or regroups sibling glass nodes).
    var sortOrbVisibility by remember { mutableFloatStateOf(1f) }
    var moreOrbVisibility by remember { mutableFloatStateOf(1f) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }
    val createShelfVisibleState = remember { MutableTransitionState(false) }
    createShelfVisibleState.targetState = showCreateShelfDialog
    val createShelfRendered = createShelfVisibleState.currentState || createShelfVisibleState.targetState
    var shelfPendingDelete by remember { mutableStateOf<String?>(null) }
    val sortAnchorCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val moreAnchorCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    var sortAnchorRect by remember { mutableStateOf<Rect?>(null) }
    var moreAnchorRect by remember { mutableStateOf<Rect?>(null) }
    val activeSeriesBlockKeyState = remember { mutableStateOf<String?>(null) }
    val retainedSeriesBlockState = remember { mutableStateOf<ShelfBookBlock?>(null) }
    var seriesOriginRect by remember { mutableStateOf<Rect?>(null) }
    val activeSeriesBoundsT = remember { mutableFloatStateOf(0f) }
    val activeSeriesSlotAlpha = remember { mutableFloatStateOf(1f) }
    val seriesLiveState = remember { SeriesModalLiveHolder() }
    val seriesActionTargetBookState = remember { mutableStateOf<BookEntry?>(null) }
    val shelfCoverCoords = remember { mutableMapOf<String, LayoutCoordinates>() }
    val shelfBlockOriginCoords = remember { mutableMapOf<String, LayoutCoordinates>() }
    val seriesTileCoverCoords = remember { mutableMapOf<String, LayoutCoordinates>() }
    val seriesModalViewportCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }

    val currentRetainedSeriesBlock by rememberUpdatedState(retainedSeriesBlockState.value)
    val currentActiveSeriesKey by rememberUpdatedState(activeSeriesBlockKeyState.value)
    val shelfOrderCooldownState = remember { mutableStateOf(false) }
    val shelfEffectiveSuspendBlurState = remember(resolvedSuspendChromeBlurState) {
        derivedStateOf {
            resolvedSuspendChromeBlurState.value
        }
    }
    val shelfPrefetchBusyProvider = remember {
        {
            resolvedIsReaderOverlayActiveState.value ||
                currentActiveSeriesKey != null ||
                currentRetainedSeriesBlock != null ||
                shelfOrderCooldownState.value
        }
    }
    DisposableEffect(onRegisterSeriesDismiss) {
        onRegisterSeriesDismiss { activeSeriesBlockKeyState.value = null }
        onDispose { onRegisterSeriesDismiss(null) }
    }

    DisposableEffect(onRegisterBoundsResolver, onRegisterClosingAlphaResolver, onRegisterClipBoundsResolver) {
        onRegisterBoundsResolver?.invoke { bookId ->
            val seriesBlock = retainedSeriesBlockState.value
            if (seriesBlock != null) {
                val liveTileRect = seriesTileCoverCoords[bookId]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()
                val frozenTileRect = seriesLiveState.openTileRects[bookId]
                val stackRect = shelfBlockOriginCoords[seriesBlock.key]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()
                    ?: seriesLiveState.lastStackRect
                if (activeSeriesBlockKeyState.value != null) {
                    liveTileRect ?: frozenTileRect ?: stackRect
                } else {
                    val sourceTile = frozenTileRect ?: liveTileRect
                    val collapseWeight = smoothstepSeries(seriesLiveState.boundsT / 0.90f)
                    when {
                        sourceTile != null && stackRect != null -> lerpRectBounds(stackRect, sourceTile, collapseWeight)
                        stackRect != null -> stackRect
                        else -> sourceTile
                    }
                }
            } else if (seriesLiveState.collapsedToGrid) {
                val trackedStackRect = seriesLiveState.lastSeriesKey
                    ?.let { shelfBlockOriginCoords[it]?.takeIf { c -> c.isAttached }?.unclippedBoundsInRoot() }
                    ?: seriesLiveState.lastStackRect
                trackedStackRect ?: shelfCoverCoords[bookId]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()
            } else {
                shelfCoverCoords[bookId]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()
            }
        }
        onRegisterClosingAlphaResolver?.invoke { _ ->
            val seriesBlock = retainedSeriesBlockState.value
            when {
                activeSeriesBlockKeyState.value != null -> 1f
                seriesBlock != null -> ((seriesLiveState.boundsT - 0.14f) / 0.42f).coerceIn(0f, 1f)
                seriesLiveState.collapsedToGrid -> 0f
                else -> 1f
            }
        }
        onRegisterClipBoundsResolver?.invoke { _ ->
            val seriesBlock = retainedSeriesBlockState.value
            if (seriesBlock != null && activeSeriesBlockKeyState.value != null) {
                seriesModalViewportCoords[0]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()
            } else {
                null
            }
        }
        onDispose { }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            searchExpanded = false
            searchQuery = ""
            showSortMenu = false
            showMoreMenu = false
            sortPopoverPresenting = false
            morePopoverPresenting = false
            sortOrbVisibility = 1f
            moreOrbVisibility = 1f
            activeSeriesBlockKeyState.value = null
            retainedSeriesBlockState.value = null
            seriesLiveState.activeOverlayKeyState.value = null
            seriesOriginRect = null
            activeSeriesBoundsT.floatValue = 0f
            activeSeriesSlotAlpha.floatValue = 1f
            seriesActionTargetBookState.value = null
            seriesTileCoverCoords.clear()
            seriesLiveState.collapsedToGrid = false
            seriesLiveState.lastSeriesKey = null
            seriesLiveState.lastStackRect = null
            seriesLiveState.openTileRects.clear()
            shelfCoverCoords.entries.removeAll { !it.value.isAttached }
            shelfBlockOriginCoords.entries.removeAll { !it.value.isAttached }
            focusManager.clearFocus(force = true)
            softwareKeyboardController?.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            shelfCoverCoords.clear()
            shelfBlockOriginCoords.clear()
            seriesTileCoverCoords.clear()
            searchExpanded = false
            searchQuery = ""
            showSortMenu = false
            showMoreMenu = false
            sortPopoverPresenting = false
            morePopoverPresenting = false
            sortOrbVisibility = 1f
            moreOrbVisibility = 1f
            focusManager.clearFocus(force = true)
        }
    }

    var handledExternalEnterBatchToken by remember { mutableIntStateOf(externalEnterBatchToken) }
    LaunchedEffect(externalEnterBatchToken) {
        if (externalEnterBatchToken != handledExternalEnterBatchToken) {
            handledExternalEnterBatchToken = externalEnterBatchToken
            showSortMenu = false
            showMoreMenu = false
            sortPopoverPresenting = false
            morePopoverPresenting = false
            sortOrbVisibility = 1f
            moreOrbVisibility = 1f
            searchExpanded = false
            focusManager.clearFocus(force = true)
            forceSelectionMode = true
        }
    }

    LaunchedEffect(externalSearchToken) {
        if (externalSearchToken > 0) {
            showSortMenu = false
            showMoreMenu = false
            forceSelectionMode = false
            selectedBookIds = emptySet()
            searchExpanded = true
        }
    }

    LaunchedEffect(externalCreateShelfToken) {
        if (externalCreateShelfToken > 0) {
            showSortMenu = false
            showMoreMenu = false
            showCreateShelfDialog = true
        }
    }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = showSortMenu || showMoreMenu) {
        showSortMenu = false
        showMoreMenu = false
    }

    BackHandler(enabled = isSelectionMode) {
        forceSelectionMode = false
        selectedBookIds = emptySet()
    }

    BackHandler(enabled = searchExpanded && !isSelectionMode) {
        searchQuery = ""
        searchExpanded = false
        focusManager.clearFocus()
    }

    fun launchBook(book: BookEntry, coverBounds: Rect?) {
        if (activeSeriesBlockKeyState.value != null && coverBounds != null && coverBounds.width >= 8f && coverBounds.height >= 8f) {
            seriesLiveState.openTileRects[book.id] = coverBounds
            seriesLiveState.collapsedToGrid = false
        } else if (activeSeriesBlockKeyState.value == null) {
            seriesLiveState.collapsedToGrid = false
        }
        if (launchTracker != null) {
            launchTracker(book, coverBounds)
        } else {
            onOpenBook(book)
        }
    }

    var editingBook by remember { mutableStateOf<BookEntry?>(null) }
    var showMoveShelfSheet by remember { mutableStateOf(false) }
    var showGroupSeriesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val customShelves = remember(shelves) {
        shelves.filter {
            it.isNotBlank() &&
                it != DEFAULT_SHELF_NAME &&
                it != DEFAULT_SHELF_LABEL &&
                it != "全部" &&
                it != "在读" &&
                it != "未读" &&
                it != "已读"
        }.distinct()
    }

    val tabs = remember(customShelves) {
        listOf(DEFAULT_SHELF_NAME, "在读", "未读") + customShelves
    }

    LaunchedEffect(tabs, selectedTab) {
        if (selectedTab !in tabs) {
            selectedTab = DEFAULT_SHELF_NAME
        }
    }

    val tabFilteredBooks = remember(books, selectedTab) {
        when (selectedTab) {
            DEFAULT_SHELF_NAME, DEFAULT_SHELF_LABEL -> books
            "在读" -> books.filter { it.progress > 0.0 && it.progress < 0.999 }
            "未读" -> books.filter { it.progress <= 0.0 }
            else -> books.filter { it.shelfName == selectedTab }
        }
    }

    val filteredBooks = remember(tabFilteredBooks, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) tabFilteredBooks
        else tabFilteredBooks.filter { book ->
            val effectiveSeries = book.seriesOverride
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it != "__ungrouped__" }
            cleanBookTitle(book.title).contains(q, ignoreCase = true) ||
                book.title.contains(q, ignoreCase = true) ||
                book.author.contains(q, ignoreCase = true) ||
                (effectiveSeries?.contains(q, ignoreCase = true) == true) ||
                normalizeSeriesBaseTitle(book.title).contains(q, ignoreCase = true)
        }
    }

    val cachedAllBlocksRef = remember { arrayOfNulls<Pair<List<BookEntry>, List<ShelfBookBlock>>>(1) }
    val allBlocks = remember(books) {
        val cached = cachedAllBlocksRef[0]
        val blocks = if (cached != null) {
            refreshShelfBlocksIfStructureUnchanged(cached.first, books, cached.second)
                ?: buildShelfBookBlocks(books)
        } else {
            buildShelfBookBlocks(books)
        }
        cachedAllBlocksRef[0] = books to blocks
        blocks
    }
    val allBlocksByKey = remember(allBlocks) { allBlocks.associateBy { it.key } }

    val cachedFilteredBlocksRef = remember { arrayOfNulls<Pair<List<BookEntry>, List<ShelfBookBlock>>>(1) }
    val sortedBlocks = remember(allBlocks, filteredBooks, books, sortMode) {
        val rawBlocks = if (filteredBooks === books) {
            allBlocks
        } else {
            val cached = cachedFilteredBlocksRef[0]
            val blocks = if (cached != null) {
                refreshShelfBlocksIfStructureUnchanged(cached.first, filteredBooks, cached.second)
                    ?: buildShelfBookBlocks(filteredBooks)
            } else {
                buildShelfBookBlocks(filteredBooks)
            }
            cachedFilteredBlocksRef[0] = filteredBooks to blocks
            blocks
        }
        when (sortMode) {
            ShelfSortMode.Recent -> rawBlocks.sortedByDescending {
                it.lastReadAtMillis ?: it.importedAtMillis
            }
            ShelfSortMode.Title -> rawBlocks.sortedBy { it.title }
            ShelfSortMode.Imported -> rawBlocks.sortedByDescending { it.importedAtMillis }
            ShelfSortMode.Progress -> rawBlocks.sortedByDescending { it.progress }
        }
    }

    val preparedThumbCache = remember { LruCache<String, ImageBitmap>(128) }
    val prewarmSeriesBooks = remember(sortedBlocks) {
        buildList {
            for (block in sortedBlocks) {
                if (block.books.size > 1) {
                    for (book in block.books.take(24)) {
                        if (!book.coverPath.isNullOrBlank()) {
                            add(book)
                        }
                        if (size >= 96) return@buildList
                    }
                }
            }
        }
    }
    val prewarmIdentitySignature = remember(prewarmSeriesBooks) {
        prewarmSeriesBooks
            .map { "${it.id}|${it.coverPath}" }
            .sorted()
    }

    val sortedFlatBooks = remember(filteredBooks, sortMode, viewMode) {
        if (viewMode != ShelfViewMode.ListView) {
            emptyList()
        } else {
            when (sortMode) {
                ShelfSortMode.Recent -> filteredBooks.sortedByDescending {
                    it.lastReadAtMillis ?: it.importedAtMillis
                }
                ShelfSortMode.Title -> filteredBooks.sortedBy { cleanBookTitle(it.title) }
                ShelfSortMode.Imported -> filteredBooks.sortedByDescending { it.importedAtMillis }
                ShelfSortMode.Progress -> filteredBooks.sortedByDescending { it.progress }
            }
        }
    }

    val latestAllBlocksByKeyState = rememberUpdatedState(allBlocksByKey)
    val activeSeriesBlockState = remember {
        derivedStateOf {
            activeSeriesBlockKeyState.value?.let { key -> latestAllBlocksByKeyState.value[key] }
        }
    }
    val reportedSeriesOpen = remember { arrayOfNulls<Boolean>(1) }
    val reportedSeriesRendered = remember { arrayOfNulls<Boolean>(1) }
    IsolatedRecomposeScope {
        val activeSeriesBlock = activeSeriesBlockState.value
        if (activeSeriesBlock != null) {
            retainedSeriesBlockState.value = activeSeriesBlock
            seriesLiveState.collapsedToGrid = false
            seriesLiveState.lastSeriesKey = activeSeriesBlock.key
        }
        val currentRetainedKey = retainedSeriesBlockState.value?.key
        if (seriesLiveState.activeOverlayKeyState.value != currentRetainedKey) {
            seriesLiveState.activeOverlayKeyState.value = currentRetainedKey
        }
        val isSeriesSheetLogicallyOpen = activeSeriesBlock != null
        val seriesDrawerRendered = isSeriesSheetLogicallyOpen || retainedSeriesBlockState.value != null
        SideEffect {
            if (reportedSeriesOpen[0] != isSeriesSheetLogicallyOpen) {
                reportedSeriesOpen[0] = isSeriesSheetLogicallyOpen
                onSeriesSheetOpenChanged(isSeriesSheetLogicallyOpen)
            }
        }
        SideEffect {
            if (reportedSeriesRendered[0] != seriesDrawerRendered) {
                reportedSeriesRendered[0] = seriesDrawerRendered
                onSeriesSheetRenderedChanged(seriesDrawerRendered)
            }
        }
    }

    val batchAllVisibleIds = remember(filteredBooks) { filteredBooks.map { it.id }.toSet() }
    val batchAllSelected = batchAllVisibleIds.isNotEmpty() && batchAllVisibleIds.all { it in selectedBookIds }
    val batchHasSelection = selectedBookIds.isNotEmpty()
    val currentBatchAllSelected by rememberUpdatedState(batchAllSelected)
    val currentBatchAllVisibleIds by rememberUpdatedState(batchAllVisibleIds)
    val currentSelectedBookIds by rememberUpdatedState(selectedBookIds)
    val currentOnSelectionModeChanged by rememberUpdatedState(onSelectionModeChanged)
    val currentOnBatchStateChanged by rememberUpdatedState(onBatchStateChanged)
    LaunchedEffect(isSelectionMode, batchAllSelected, batchHasSelection) {
        currentOnSelectionModeChanged(isSelectionMode)
        currentOnBatchStateChanged(isSelectionMode, batchAllSelected, batchHasSelection)
    }
    DisposableEffect(onRegisterBatchActions) {
        onRegisterBatchActions?.invoke(
            ShelfBatchActions(
                onSelectAll = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedBookIds = if (currentBatchAllSelected) emptySet() else currentBatchAllVisibleIds
                },
                onMoveToShelf = {
                    if (currentSelectedBookIds.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showMoveShelfSheet = true
                    }
                },
                onMergeSeries = {
                    if (currentSelectedBookIds.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showGroupSeriesDialog = true
                    }
                },
                onDelete = {
                    if (currentSelectedBookIds.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDeleteConfirmDialog = true
                    }
                },
                onExit = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    forceSelectionMode = false
                    selectedBookIds = emptySet()
                },
            ),
        )
        onDispose {
            onRegisterBatchActions?.invoke(null)
        }
    }

    val sortedOrderSignature = remember(sortedBlocks) {
        buildString {
            for (b in sortedBlocks) {
                append(b.key)
                append('|')
            }
        }
    }
    var lastSortedOrderSignature by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sortedOrderSignature) {
        val prev = lastSortedOrderSignature
        lastSortedOrderSignature = sortedOrderSignature
        if (prev != null && prev != sortedOrderSignature) {
            shelfOrderCooldownState.value = true
            delay(560L)
            shelfOrderCooldownState.value = false
        }
    }

    fun toggleBlockSelection(block: ShelfBookBlock) {
        val ids = block.books.map { it.id }.toSet()
        val allSelected = ids.all { it in selectedBookIds }
        selectedBookIds = if (allSelected) {
            selectedBookIds - ids
        } else {
            selectedBookIds + ids
        }
    }

    val tabGridStates = remember { mutableMapOf<String, LazyGridState>() }
    val tabListStates = remember { mutableMapOf<String, LazyListState>() }
    val currentGridState = tabGridStates.getOrPut("${selectedTab}::${viewMode.name}") { LazyGridState() }
    val currentListState = tabListStates.getOrPut(selectedTab) { LazyListState() }
    val isScrolled by remember(viewMode, currentGridState, currentListState) {
        derivedStateOf {
            if (viewMode == ShelfViewMode.ListView) {
                currentListState.firstVisibleItemIndex > 0 || currentListState.firstVisibleItemScrollOffset > 28
            } else {
                currentGridState.firstVisibleItemIndex > 0 || currentGridState.firstVisibleItemScrollOffset > 28
            }
        }
    }
    val statusTopInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val cutoutTopInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusTopInset, cutoutTopInset)
    val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val topEdgeClearance = topInset + 10.dp + 44.dp + 8.dp + 44.dp + 10.dp
    val bottomClearanceDp = (bottomInset.value * 0.65f + 104f).coerceIn(108f, 132f).dp
    val gridContentPadding = remember(topEdgeClearance, bottomClearanceDp) {
        object : PaddingValues {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 20.dp
            override fun calculateTopPadding() =
                topEdgeClearance + 4.dp + searchSlotHeightDp * searchExpandAnim.value.coerceIn(0f, 1f)
            override fun calculateRightPadding(layoutDirection: LayoutDirection) = 20.dp
            override fun calculateBottomPadding() =
                bottomClearanceDp + searchSlotHeightDp * searchExpandAnim.value.coerceIn(0f, 1f)
        }
    }
    val accentOnGlass = if (palette.isLight) {
        lerpColor(palette.accentText, Color.Black, 0.18f)
    } else {
        lerpColor(palette.accentText, Color.White, 0.10f)
    }

    val shelfSwitchKey = remember(tabs, selectedTab, viewMode, filteredBooks.isEmpty()) {
        ShelfTabSwitchKey(
            tabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
            selectedTab = selectedTab,
            viewMode = viewMode,
            isEmpty = filteredBooks.isEmpty(),
        )
    }
    val blocksByTabCache = remember { HashMap<String, List<ShelfBookBlock>>(4) }
    val flatBooksByTabCache = remember { HashMap<String, List<BookEntry>>(4) }
    if (filteredBooks.isNotEmpty()) {
        blocksByTabCache[selectedTab] = sortedBlocks
        flatBooksByTabCache[selectedTab] = sortedFlatBooks
    }
    val latestSortedBlocksState = rememberUpdatedState(sortedBlocks)
    val latestSortedFlatBooksState = rememberUpdatedState(sortedFlatBooks)
    val latestShelfSwitchKeyState = rememberUpdatedState(shelfSwitchKey)
    val readSeriesBoundsT = remember { { activeSeriesBoundsT.floatValue } }
    val readSeriesSlotAlpha = remember { { activeSeriesSlotAlpha.floatValue } }

    val suspendShelfChromeBlurState = remember(resolvedSuspendChromeBlurState) {
        derivedStateOf {
            resolvedSuspendChromeBlurState.value
        }
    }
    val suppressGridPlacementState = remember(resolvedIsReaderOverlayActiveState) {
        derivedStateOf {
            resolvedIsReaderOverlayActiveState.value ||
                activeSeriesBlockState.value != null ||
                retainedSeriesBlockState.value != null
        }
    }
    // Keep screenBackdrop permanently attached when !powerSave so transitions between tabs never
    // tear down or rebuild the 1080x2400 BackdropNode GraphicsLayer (MainTabsPagerHost already
    // gates offscreen drawWithContent when idle).
    val activeScreenBackdrop = screenBackdrop
    val latestPrewarmSeriesBooks by rememberUpdatedState(prewarmSeriesBooks)
    val latestIsScrolled by rememberUpdatedState(isScrolled)
    var lastPrewarmedSignature by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(prewarmIdentitySignature) {
        if (prewarmIdentitySignature.isEmpty() || prewarmIdentitySignature == lastPrewarmedSignature) {
            return@LaunchedEffect
        }
        delay(320L)
        while (suspendShelfChromeBlurState.value || latestIsScrolled) {
            delay(120L)
        }
        lastPrewarmedSignature = prewarmIdentitySignature
        val candidates = latestPrewarmSeriesBooks
        val missingThumbs = candidates.filter { CoverAssetCache.getThumb(it.id) == null }
        if (missingThumbs.isNotEmpty()) {
            CoverAssetCache.preloadBooks(missingThumbs, includeExtendedAssets = false)
        }
        var preparedInBatch = 0
        for (book in candidates) {
            if (suspendShelfChromeBlurState.value) break
            val thumb = CoverAssetCache.getThumb(book.id) ?: continue
            if (preparedThumbCache.get(book.id) === thumb) continue
            runCatching { thumb.prepareToDraw() }
            preparedThumbCache.put(book.id, thumb)
            preparedInBatch++
            if (preparedInBatch >= 2) {
                preparedInBatch = 0
                withFrameNanos { }
            }
        }
    }
    val headerBackdrop = screenBackdrop
    val tabPillBackdrop = screenBackdrop

    CompositionLocalProvider(
        LocalVitroBackdrop provides activeScreenBackdrop,
        LocalGlassDarkTheme provides !palette.isLight,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. 书架独立页面 Backdrop 隔离底层氛围光 + 透明滚动层，保持 Dock / Chrome 纯净磨砂质感
            IsolatedAmbientGlassContainer(
                backdrop = activeScreenBackdrop,
                paletteBackgroundColor = palette.background,
                ambientBackground = {
                    AmbientSubpageBackground(
                        palette = palette,
                        topOnlyWhenPagerTransitioning = true,
                        isAnimating = isActive &&
                            activeSeriesBlockKeyState.value == null &&
                            retainedSeriesBlockState.value == null &&
                            !resolvedIsReaderOverlayActiveState.value,
                    )
                },
            ) {
            AnimatedContent(
                targetState = shelfSwitchKey,
                contentKey = { "${it.selectedTab}::${it.viewMode}::${it.isEmpty}" },
                transitionSpec = {
                    val tabDir = targetState.tabIndex.compareTo(initialState.tabIndex)
                    if (tabDir != 0) {
                        val slideSpec = spring<IntOffset>(
                            dampingRatio = 0.88f,
                            stiffness = 420f,
                        )
                        if (tabDir > 0) {
                            (slideInHorizontally(slideSpec) { w -> (w * 0.22f).roundToInt() } +
                                fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally(slideSpec) { w -> -(w * 0.16f).roundToInt() } +
                                    fadeOut(animationSpec = tween(150)))
                        } else {
                            (slideInHorizontally(slideSpec) { w -> -(w * 0.22f).roundToInt() } +
                                fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally(slideSpec) { w -> (w * 0.16f).roundToInt() } +
                                    fadeOut(animationSpec = tween(150)))
                        }
                    } else {
                        (fadeIn(animationSpec = tween(210)) +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(210))) togetherWith
                            fadeOut(animationSpec = tween(140))
                    }
                },
                label = "shelfTabContentSwitch",
                modifier = Modifier
                    .fillMaxSize(),
            ) { renderTarget ->
                val targetGridState = tabGridStates.getOrPut("${renderTarget.selectedTab}::${renderTarget.viewMode.name}") {
                    LazyGridState(
                        prefetchStrategy = TransitionGatedGridPrefetchStrategy(shelfPrefetchBusyProvider),
                    )
                }
                val targetListState = tabListStates.getOrPut(renderTarget.selectedTab) {
                    LazyListState(
                        prefetchStrategy = TransitionGatedListPrefetchStrategy(shelfPrefetchBusyProvider),
                    )
                }
                if (renderTarget.isEmpty) {
                    val emptyPlaceholderDensity = LocalDensity.current
                    EmptyShelfPlaceholder(
                        palette = palette,
                        backdrop = null,
                        isAllShelf = renderTarget.selectedTab == DEFAULT_SHELF_NAME,
                        hasSearchQuery = searchQuery.isNotBlank(),
                        onClearSearch = { searchQuery = "" },
                        onImportFiles = onImportFilesClick,
                        onImportFolder = onImportFolderClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topEdgeClearance, bottom = bottomClearanceDp)
                            .offset {
                                val shiftPx = with(emptyPlaceholderDensity) { searchSlotHeightDp.toPx() } *
                                    searchExpandAnim.value.coerceIn(0f, 1f)
                                IntOffset(0, shiftPx.roundToInt())
                            },
                    )
                } else {
                    when (renderTarget.viewMode) {
                        ShelfViewMode.Grid3 -> {
                            LazyVerticalGrid(
                                state = targetGridState,
                                columns = GridCells.Fixed(3),
                                contentPadding = gridContentPadding,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                val gridBlocks = if (renderTarget == latestShelfSwitchKeyState.value) {
                                    latestSortedBlocksState.value
                                } else {
                                    blocksByTabCache[renderTarget.selectedTab] ?: emptyList()
                                }
                                items(gridBlocks, key = { it.key }) { block ->
                                    val blockSelected = block.books.all { it.id in selectedBookIds }
                                    val partiallySelected = !blockSelected && block.books.any { it.id in selectedBookIds }
                                    val onCoverPlacedItem = remember(block.key, block.books.firstOrNull()?.id, block.books.size) {
                                        { coords: LayoutCoordinates ->
                                            shelfBlockOriginCoords[block.key] = coords
                                            if (block.books.size == 1) {
                                                shelfCoverCoords[block.books.first().id] = coords
                                            }
                                        }
                                    }
                                    val onClickItem = remember(block, isSelectionMode) {
                                        { bounds: Rect? ->
                                            if (isSelectionMode) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                toggleBlockSelection(block)
                                            } else if (block.books.size > 1) {
                                                seriesOriginRect = bounds
                                                activeSeriesBlockKeyState.value = block.key
                                            } else {
                                                launchBook(block.books.first(), bounds)
                                            }
                                        }
                                    }
                                    val onLongClickItem = remember(block) {
                                        {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            toggleBlockSelection(block)
                                        }
                                    }
                                    ShelfPlacementGridItemBox(
                                        suppressPlacementState = suppressGridPlacementState,
                                    ) {
                                        ShelfGrid3Item(
                                            block = block,
                                            palette = palette,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = blockSelected,
                                            isPartial = partiallySelected,
                                            activeSeriesOverlayKeyState = seriesLiveState.activeOverlayKeyState,
                                            seriesSlotAlpha = readSeriesSlotAlpha,
                                            onCoverPlaced = onCoverPlacedItem,
                                            onClick = onClickItem,
                                            onLongClick = onLongClickItem,
                                        )
                                    }
                                }
                            }
                        }

                        ShelfViewMode.ListView -> {
                            LazyColumn(
                                state = targetListState,
                                contentPadding = gridContentPadding,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                val listBooks = if (renderTarget == latestShelfSwitchKeyState.value) {
                                    latestSortedFlatBooksState.value
                                } else {
                                    flatBooksByTabCache[renderTarget.selectedTab] ?: emptyList()
                                }
                                items(listBooks, key = { it.id }) { book ->
                                    val selected = book.id in selectedBookIds
                                    val onCoverPlacedItem = remember(book.id) {
                                        { coords: LayoutCoordinates ->
                                            shelfCoverCoords[book.id] = coords
                                        }
                                    }
                                    val onClickItem = remember(book, isSelectionMode, selected) {
                                        { bounds: Rect? ->
                                            if (isSelectionMode) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedBookIds = if (selected) {
                                                    selectedBookIds - book.id
                                                } else {
                                                    selectedBookIds + book.id
                                                }
                                            } else {
                                                launchBook(book, bounds)
                                            }
                                        }
                                    }
                                    val onLongClickItem = remember(book.id, selected) {
                                        {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedBookIds = if (selected) {
                                                selectedBookIds - book.id
                                            } else {
                                                selectedBookIds + book.id
                                            }
                                        }
                                    }
                                    val onEditMetaItem = remember(book) {
                                        { editingBook = book }
                                    }
                                    ShelfPlacementListItemBox(
                                        suppressPlacementState = suppressGridPlacementState,
                                    ) {
                                        ShelfListRowItem(
                                            book = book,
                                            palette = palette,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selected,
                                            onCoverPlaced = onCoverPlacedItem,
                                            onClick = onClickItem,
                                            onLongClick = onLongClickItem,
                                            onEditMeta = onEditMetaItem,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // 2. 悬浮在 Backdrop 之上的 iOS 27 顶部玻璃控制栏 + 搜索滤书条 + 平滑胶囊标签栏
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = topInset + 10.dp),
            ) {
                // Every header glass node ([书架] capsule, [🔍], [≡], [⋯]) is a standalone Vitro glass scope:
                // no shared glassEffectContainer / GlassGroup regroup and no key(...) remount, so neither popover
                // presentation nor captureScreenBackdrop flips can tear down or regroup sibling header orbs.
                run {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(44.dp)
                            .zIndex(2f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val topPillHeight = 44.dp
                        val titleFontSp = 20.5.sp
                        val titleHorizPad = 15.dp
                        val onExitSelectionClick = remember(haptics) {
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                forceSelectionMode = false
                                selectedBookIds = emptySet()
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(min = 44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isSelectionMode) {
                                FrostedCircleIconButton(
                                    icon = Icons.Rounded.Close,
                                    contentDescription = "退出多选",
                                    palette = palette,
                                    backdrop = headerBackdrop,
                                    size = topPillHeight,
                                    suspendBlurState = shelfEffectiveSuspendBlurState,
                                    onClick = onExitSelectionClick,
                                )
                            }

                            Box(
                                modifier = Modifier.height(topPillHeight),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                IsolatedRecomposeScope {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .ios27GlassSurface(
                                                shape = CircleShape,
                                                palette = palette,
                                                style = readingStyle,
                                                variant = Ios27GlassVariant.Chrome,
                                                backdrop = headerBackdrop,
                                                isInteractive = false,
                                                suspendVitroBlur = shelfEffectiveSuspendBlurState.value,
                                                showContrastBorder = false,
                                                showFingerHighlight = false,
                                            ),
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = titleHorizPad),
                                ) {
                                    Text(
                                        text = if (isSelectionMode) "已选 ${selectedBookIds.size} 本" else "书架",
                                        color = palette.text,
                                        fontSize = titleFontSp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.4).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (!isSelectionMode) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(11.dp)
                                                .background(palette.text.copy(alpha = if (palette.isLight) 0.16f else 0.22f)),
                                        )
                                        Text(
                                            text = "${filteredBooks.size} 本",
                                            color = palette.text.copy(alpha = if (palette.isLight) 0.66f else 0.72f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            modifier = Modifier.heightIn(min = 44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!isSelectionMode) {
                                val onSearchButtonClick = remember<() -> Unit>(
                                    haptics,
                                    focusManager,
                                    softwareKeyboardController,
                                ) {
                                    {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (searchExpanded) {
                                            searchQuery = ""
                                            searchExpanded = false
                                            focusManager.clearFocus(force = true)
                                            softwareKeyboardController?.hide()
                                        } else {
                                            searchExpanded = true
                                        }
                                    }
                                }
                                val onSortAnchorPlaced = remember {
                                    { coords: LayoutCoordinates -> sortAnchorCoords[0] = coords }
                                }
                                val sortOrbVisibilityProvider = remember {
                                    { if (showSortMenu) 0f else sortOrbVisibility }
                                }
                                val onSortButtonClick = remember {
                                    {
                                        sortAnchorRect = sortAnchorCoords[0]?.takeIf { it.isAttached }?.boundsInRoot()
                                        showMoreMenu = false
                                        showSortMenu = true
                                    }
                                }
                                FrostedCircleIconButton(
                                    icon = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                                    contentDescription = if (searchExpanded) "收起搜索" else "搜索书架",
                                    palette = palette,
                                    backdrop = headerBackdrop,
                                    size = topPillHeight,
                                    tint = if (searchExpanded || searchQuery.isNotBlank()) accentOnGlass else palette.text,
                                    animateIconChange = true,
                                    suspendBlurState = shelfEffectiveSuspendBlurState,
                                    onClick = onSearchButtonClick,
                                )

                                Box(
                                    modifier = Modifier.onPlaced(onSortAnchorPlaced),
                                ) {
                                    FrostedCircleIconButton(
                                        icon = Icons.AutoMirrored.Rounded.Sort,
                                        contentDescription = "排序方式",
                                        palette = palette,
                                        backdrop = headerBackdrop,
                                        size = topPillHeight,
                                        hidden = sortPopoverPresenting || showSortMenu,
                                        orbVisibility = sortOrbVisibilityProvider,
                                        suspendBlurState = shelfEffectiveSuspendBlurState,
                                        onClick = onSortButtonClick,
                                    )
                                }
                            }

                            val onMoreAnchorPlaced = remember {
                                { coords: LayoutCoordinates -> moreAnchorCoords[0] = coords }
                            }
                            val moreOrbVisibilityProvider = remember {
                                { if (showMoreMenu) 0f else moreOrbVisibility }
                            }
                            val onMoreButtonClick = remember {
                                {
                                    moreAnchorRect = moreAnchorCoords[0]?.takeIf { it.isAttached }?.boundsInRoot()
                                    showSortMenu = false
                                    showMoreMenu = true
                                }
                            }
                            Box(
                                modifier = Modifier.onPlaced(onMoreAnchorPlaced),
                            ) {
                                FrostedCircleIconButton(
                                    icon = Icons.Rounded.MoreHoriz,
                                    contentDescription = "更多操作",
                                    palette = palette,
                                    backdrop = headerBackdrop,
                                    size = topPillHeight,
                                    hidden = morePopoverPresenting || showMoreMenu,
                                    orbVisibility = moreOrbVisibilityProvider,
                                    suspendBlurState = shelfEffectiveSuspendBlurState,
                                    onClick = onMoreButtonClick,
                                )
                            }
                        }
                    }
                }

                // Expandable Apple Books Style Search Filter Pill strictly synced 1:1 with grid offset via searchExpandAnim
                val showSearchSlot = targetSearchVisible || searchExpandAnim.value > 0.001f
                if (showSearchSlot) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val frac = searchExpandAnim.value.coerceIn(0f, 1f)
                                val visibleHeight = (placeable.height * frac).roundToInt().coerceAtLeast(0)
                                layout(placeable.width, visibleHeight) {
                                    placeable.placeRelative(0, visibleHeight - placeable.height)
                                }
                            },
                    ) {
                        val searchInteraction = remember { MutableInteractionSource() }
                        val searchGlassState = rememberSafeGlassEffectState(
                            interactionSource = searchInteraction,
                            active = targetSearchVisible,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 6.dp, start = 20.dp, end = 20.dp)
                                .graphicsLayer {
                                    alpha = (searchExpandAnim.value / 0.85f).coerceIn(0f, 1f)
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .ios27GlassSurface(
                                        shape = CircleShape,
                                        palette = palette,
                                        style = readingStyle,
                                        variant = Ios27GlassVariant.Chrome,
                                        backdrop = headerBackdrop,
                                        isInteractive = true,
                                        suspendVitroBlur = shelfEffectiveSuspendBlurState.value,
                                        showContrastBorder = false,
                                        state = searchGlassState,
                                        interactionSource = searchInteraction,
                                    )
                                    .clickable(
                                        interactionSource = searchInteraction,
                                        indication = null,
                                        onClick = { runCatching { focusRequester.requestFocus() } },
                                    )
                                    .padding(horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = palette.text.copy(alpha = if (palette.isLight) 0.56f else 0.65f),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "搜索书名、作者或系列…",
                                            color = palette.text.copy(alpha = if (palette.isLight) 0.45f else 0.52f),
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = palette.text,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        cursorBrush = SolidColor(accentOnGlass),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                focusManager.clearFocus(force = true)
                                                softwareKeyboardController?.hide()
                                            },
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                    )
                                }
                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty(),
                                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.72f),
                                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.72f),
                                ) {
                                    val clearSearchInteraction = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = clearSearchInteraction,
                                                indication = null,
                                            ) { searchQuery = "" },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "清除搜索词",
                                            tint = palette.text.copy(alpha = 0.68f),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unified iOS 27 Liquid Glass Capsule Track aligned with header left/right edges (20.dp)
                val (_, accentPillTint) = ios27FilterPillTints(palette, readingStyle)
                val isDarkGlass = !palette.isLight
                val glassOpacity = readingStyle.glassOpacityLevel.coerceIn(0f, 1f)
                val trackTintOverride = remember(palette, isDarkGlass, glassOpacity) {
                    if (isDarkGlass) {
                        Color.White.copy(alpha = (0.06f + 0.05f * glassOpacity).coerceIn(0.05f, 0.14f))
                    } else {
                        Color.White.copy(alpha = (0.28f + 0.14f * glassOpacity).coerceIn(0.24f, 0.50f))
                    }
                }
                val activeInnerCapsuleColor = remember(palette, accentPillTint, isDarkGlass) {
                    if (isDarkGlass) {
                        lerpColor(Color(0xFF2D3440), accentPillTint.copy(alpha = 1f), 0.28f).copy(alpha = 0.80f)
                    } else {
                        lerpColor(Color.White, accentPillTint.copy(alpha = 1f), 0.20f).copy(alpha = 0.88f)
                    }
                }
                val activeInnerBorderColor = remember(isDarkGlass) {
                    if (isDarkGlass) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.92f)
                }
                val selectedPillTextColor = if (palette.isLight) {
                    accentOnGlass
                } else {
                    lerpColor(palette.primarySoft, Color.White, 0.68f)
                }
                val unselectedPillTextColor = palette.text.copy(alpha = if (palette.isLight) 0.72f else 0.84f)
                val filterScrollState = rememberScrollState()
                val tabBounds = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
                val pillLeftAnim = remember { Animatable(0f) }
                val pillWidthAnim = remember { Animatable(0f) }
                var pillInitialized by remember { mutableStateOf(false) }

                LaunchedEffect(selectedTab, tabs) {
                    snapshotFlow { tabBounds[selectedTab] }.collectLatest { bounds ->
                        if (bounds == null) return@collectLatest
                        val (targetLeft, targetWidth) = bounds
                        if (targetWidth <= 0f) return@collectLatest
                        if (!pillInitialized) {
                            pillLeftAnim.snapTo(targetLeft)
                            pillWidthAnim.snapTo(targetWidth)
                            pillInitialized = true
                        } else {
                            coroutineScope {
                                launch {
                                    pillLeftAnim.animateTo(
                                        targetValue = targetLeft,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
                                    )
                                }
                                launch {
                                    pillWidthAnim.animateTo(
                                        targetValue = targetWidth,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
                                    )
                                }
                            }
                        }
                        val viewport = filterScrollState.viewportSize
                        if (viewport > 0 && filterScrollState.maxValue > 0) {
                            val edgePad = targetWidth * 0.25f
                            val desiredMinScroll = (targetLeft + targetWidth + edgePad - viewport).roundToInt()
                            val desiredMaxScroll = (targetLeft - edgePad).roundToInt()
                            val curScroll = filterScrollState.value
                            val scrollTarget = when {
                                curScroll < desiredMinScroll -> desiredMinScroll.coerceIn(0, filterScrollState.maxValue)
                                curScroll > desiredMaxScroll -> desiredMaxScroll.coerceIn(0, filterScrollState.maxValue)
                                else -> curScroll
                            }
                            if (scrollTarget != curScroll) {
                                filterScrollState.animateScrollTo(scrollTarget)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(44.dp),
                ) {
                    IsolatedRecomposeScope {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .ios27GlassSurface(
                                    shape = CircleShape,
                                    palette = palette,
                                    style = readingStyle,
                                    variant = Ios27GlassVariant.Chrome,
                                    backdrop = tabPillBackdrop,
                                    tintOverride = trackTintOverride,
                                    isInteractive = false,
                                    suspendVitroBlur = shelfEffectiveSuspendBlurState.value,
                                    showContrastBorder = false,
                                    showFingerHighlight = false,
                                ),
                        )
                    }
                    val capsuleDensity = LocalDensity.current
                    val capsuleWindowWidthPx = LocalWindowInfo.current.containerSize.width
                    val defaultTrackWidthPx = remember(capsuleWindowWidthPx, capsuleDensity) {
                        (capsuleWindowWidthPx - with(capsuleDensity) { 46.dp.roundToPx() }).coerceAtLeast(1)
                    }
                    var trackInnerWidthPx by remember { mutableIntStateOf(defaultTrackWidthPx) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 3.dp)
                            .clip(CircleShape)
                            .onSizeChanged { sz ->
                                if (sz.width > 0 && trackInnerWidthPx != sz.width) {
                                    trackInnerWidthPx = sz.width
                                }
                            },
                    ) {
                        val trackInnerWidth = with(capsuleDensity) { trackInnerWidthPx.toDp() }
                        val fairTabMinWidth = if (tabs.isNotEmpty()) {
                            (trackInnerWidth / tabs.size).coerceAtLeast(0.dp)
                        } else {
                            0.dp
                        }
                        CompositionLocalProvider(LocalOverscrollFactory provides null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .horizontalScroll(filterScrollState)
                                    .defaultMinSize(minWidth = trackInnerWidth)
                                    .drawBehind {
                                        val insetPx = 3.dp.toPx()
                                        val pillH = (size.height - insetPx * 2f).coerceAtLeast(1f)
                                        val currentBounds = tabBounds[selectedTab]
                                        val pillLeft = if (pillInitialized) pillLeftAnim.value else (currentBounds?.first ?: 0f)
                                        val pillWidth = if (pillInitialized) pillWidthAnim.value else (currentBounds?.second ?: 0f)
                                        if (pillWidth > 2f) {
                                            val r = pillH * 0.5f
                                            drawRoundRect(
                                                color = activeInnerCapsuleColor,
                                                topLeft = Offset(pillLeft, insetPx),
                                                size = Size(pillWidth, pillH),
                                                cornerRadius = CornerRadius(r, r),
                                            )
                                            val strokePx = 0.85.dp.toPx()
                                            drawRoundRect(
                                                color = activeInnerBorderColor,
                                                topLeft = Offset(pillLeft + strokePx * 0.5f, insetPx + strokePx * 0.5f),
                                                size = Size(
                                                    (pillWidth - strokePx).coerceAtLeast(1f),
                                                    (pillH - strokePx).coerceAtLeast(1f),
                                                ),
                                                cornerRadius = CornerRadius(r, r),
                                                style = Stroke(width = strokePx),
                                            )
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                tabs.forEach { tab ->
                                    key(tab) {
                                        val isSelected = selectedTab == tab
                                        val selectProgressState = animateFloatAsState(
                                            targetValue = if (isSelected) 1f else 0f,
                                            animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f),
                                            label = "shelfTabSelectProgress",
                                        )
                                        val count = when (tab) {
                                            DEFAULT_SHELF_NAME -> books.size
                                            "在读" -> books.count { it.progress > 0.0 && it.progress < 0.999 }
                                            "未读" -> books.count { it.progress <= 0.0 }
                                            else -> books.count { it.shelfName == tab }
                                        }
                                        val label = if (tab == DEFAULT_SHELF_NAME) "$DEFAULT_SHELF_LABEL $count" else "$tab $count"
                                        val isCustom = tab in customShelves
                                        val tabInteraction = remember(tab) { MutableInteractionSource() }
                                        val isTabPressed by tabInteraction.collectIsPressedAsState()
                                        val tabPressScale by animateFloatAsState(
                                            targetValue = if (isTabPressed) 0.95f else 1f,
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 580f),
                                            label = "shelfTabPressScale",
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .defaultMinSize(minWidth = fairTabMinWidth)
                                                .onPlaced { coords ->
                                                    tabBounds[tab] = coords.positionInParent().x to coords.size.width.toFloat()
                                                }
                                                .graphicsLayer {
                                                    scaleX = tabPressScale
                                                    scaleY = tabPressScale
                                                }
                                                .combinedClickable(
                                                    interactionSource = tabInteraction,
                                                    indication = null,
                                                    onClick = {
                                                        if (selectedTab != tab) {
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                        selectedTab = tab
                                                        forceSelectionMode = false
                                                        selectedBookIds = emptySet()
                                                    },
                                                    onLongClick = {
                                                        if (isCustom) {
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            shelfPendingDelete = tab
                                                        }
                                                    },
                                                )
                                                .padding(horizontal = 13.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                modifier = Modifier.graphicsLayer {
                                                    val s = 1f + 0.025f * selectProgressState.value
                                                    scaleX = s
                                                    scaleY = s
                                                },
                                            ) {
                                                ShelfTabPillText(
                                                    label = label,
                                                    isSelected = isSelected,
                                                    selectedColor = selectedPillTextColor,
                                                    unselectedColor = unselectedPillTextColor,
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

            // 4. 独立窗口内 VitroGlassPopoverMenu 悬浮层（处于滚动容器外部，保持非空 items 以支撑收起形变动画）
            val sortMenuItems = remember(sortMode, haptics) {
                ShelfSortMode.entries.map { mode ->
                    VitroGlassPopoverItem(
                        label = mode.label,
                        selected = sortMode == mode,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sortMode = mode
                        },
                    )
                }
            }
            val headerCornerDp = 22.dp
            VitroGlassPopoverMenu(
                visible = showSortMenu,
                anchorBoundsInRoot = sortAnchorRect,
                items = sortMenuItems,
                palette = palette,
                onDismiss = { showSortMenu = false },
                backdrop = activeScreenBackdrop,
                width = 188.dp,
                anchorCornerDp = headerCornerDp,
                anchorIcon = Icons.AutoMirrored.Rounded.Sort,
                anchorIconSize = 19.dp,
                onPresentingChanged = { sortPopoverPresenting = it },
                onOrbVisibilityChanged = { sortOrbVisibility = it },
            )

            val moreMenuItems = remember(viewMode, selectedTab, customShelves, haptics) {
                buildList {
                    ShelfViewMode.entries.forEach { mode ->
                        val modeIcon = when (mode) {
                            ShelfViewMode.Grid3 -> Icons.Rounded.GridView
                            ShelfViewMode.ListView -> Icons.AutoMirrored.Rounded.ViewList
                        }
                        add(
                            VitroGlassPopoverItem(
                                label = mode.label,
                                selected = viewMode == mode,
                                icon = modeIcon,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModeName = mode.name
                                },
                            )
                        )
                    }
                    if (selectedTab in customShelves) {
                        add(
                            VitroGlassPopoverItem(
                                label = "删除「$selectedTab」",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = Color(0xFFEF4444),
                                onClick = {
                                    shelfPendingDelete = selectedTab
                                },
                            )
                        )
                    }
                }
            }
            VitroGlassPopoverMenu(
                visible = showMoreMenu,
                anchorBoundsInRoot = moreAnchorRect,
                items = moreMenuItems,
                palette = palette,
                onDismiss = { showMoreMenu = false },
                backdrop = activeScreenBackdrop,
                width = 204.dp,
                anchorCornerDp = headerCornerDp,
                anchorIcon = Icons.Rounded.MoreHoriz,
                anchorIconSize = 19.dp,
                onPresentingChanged = { morePopoverPresenting = it },
                onOrbVisibilityChanged = { moreOrbVisibility = it },
            )
        }

        IsolatedRecomposeScope {
            val activeSeriesBlock = activeSeriesBlockState.value
            val retainedSeriesBlock = retainedSeriesBlockState.value
            var renamingSeriesBlock by remember { mutableStateOf<ShelfBookBlock?>(null) }
            var seriesDrawerGridMode by rememberSaveable { mutableStateOf(true) }
            val isSeriesOpen = activeSeriesBlock != null
            LaunchedEffect(isSeriesOpen, activeSeriesBlockKeyState.value) {
                if (!isSeriesOpen && retainedSeriesBlockState.value == null) {
                    seriesTileCoverCoords.clear()
                }
            }
            BackHandler(enabled = isSeriesOpen) {
                activeSeriesBlockKeyState.value = null
            }
            BackHandler(enabled = !isSeriesOpen && retainedSeriesBlock != null) {
                // Absorb extra rapid back presses while SeriesModal collapse animation completes.
            }

            // Clean untinted Liquid Glass Series modal with GPU-only spring pop-out morph from tapped stack card
            if (retainedSeriesBlock != null) {
                val drawerBlock = retainedSeriesBlock
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val readingStyle = LocalReadingStyle.current
            val maxCardHeight = (configuration.screenHeightDp * 0.62f).coerceIn(260f, 520f).dp
            var overlayOriginInRoot by remember(drawerBlock.key) { mutableStateOf(Offset.Zero) }
            var capturedOriginRect by remember(drawerBlock.key) { mutableStateOf(seriesOriginRect) }
            val scrimInteraction = remember { MutableInteractionSource() }
            val sheetInteraction = remember { MutableInteractionSource() }
            val panelGeometry = remember(drawerBlock.key) { SeriesModalPanelGeometry() }
            val stableGlassShape = remember { RoundedCornerShape(28.dp) }

            LaunchedEffect(drawerBlock.key) {
                val missingThumbs = drawerBlock.books.filter { CoverAssetCache.getThumb(it.id) == null }
                if (missingThumbs.isNotEmpty()) {
                    CoverAssetCache.preloadBooks(missingThumbs, includeExtendedAssets = false)
                }
                val thumbsToWarm = drawerBlock.books.take(15)
                var preparedInBatch = 0
                for (book in thumbsToWarm) {
                    val thumb = CoverAssetCache.getThumb(book.id) ?: continue
                    if (preparedThumbCache.get(book.id) === thumb) continue
                    runCatching { thumb.prepareToDraw() }
                    preparedThumbCache.put(book.id, thumb)
                    preparedInBatch++
                    if (preparedInBatch >= 2) {
                        preparedInBatch = 0
                        withFrameNanos { }
                    }
                }
            }

            LaunchedEffect(isSeriesOpen, drawerBlock.key, seriesOriginRect, sortedBlocks) {
                val liveOrigin = shelfBlockOriginCoords[drawerBlock.key]
                    ?.takeIf { it.isAttached }
                    ?.unclippedBoundsInRoot()
                    ?: seriesOriginRect
                if (liveOrigin != null && liveOrigin.width >= 8f && liveOrigin.height >= 8f) {
                    capturedOriginRect = liveOrigin
                    seriesLiveState.lastStackRect = liveOrigin
                }
            }

            val accelerateClose = resolvedAccelerateSeriesCloseState.value
            val transitionState = remember(drawerBlock.key) { MutableTransitionState(initialState = false) }
            transitionState.targetState = isSeriesOpen
            val transition = rememberTransition(transitionState, label = "seriesModalPopTransition")

            val centerTState = transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        spring(
                            dampingRatio = 0.80f,
                            stiffness = 680f,
                            visibilityThreshold = 0.004f,
                        )
                    } else {
                        spring(
                            dampingRatio = 0.88f,
                            stiffness = if (accelerateClose) 580f else 620f,
                            visibilityThreshold = 0.002f,
                        )
                    }
                },
                label = "seriesModalCenterT",
            ) { target -> if (target) 1f else 0f }

            val rawWidthTState = transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        spring(
                            dampingRatio = 0.78f,
                            stiffness = 460f,
                            visibilityThreshold = 0.004f,
                        )
                    } else {
                        spring(
                            dampingRatio = 0.88f,
                            stiffness = if (accelerateClose) 450f else 480f,
                            visibilityThreshold = 0.002f,
                        )
                    }
                },
                label = "seriesModalWidthT",
            ) { target -> if (target) 1f else 0f }

            val rawHeightTState = transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        spring(
                            dampingRatio = 0.80f,
                            stiffness = 400f,
                            visibilityThreshold = 0.004f,
                        )
                    } else {
                        spring(
                            dampingRatio = 0.88f,
                            stiffness = if (accelerateClose) 400f else 430f,
                            visibilityThreshold = 0.002f,
                        )
                    }
                },
                label = "seriesModalHeightT",
            ) { target -> if (target) 1f else 0f }

            LaunchedEffect(drawerBlock.key, transition, isSeriesOpen) {
                snapshotFlow {
                    maxOf(
                        rawWidthTState.value.coerceIn(0f, 1f),
                        rawHeightTState.value.coerceIn(0f, 1f),
                    )
                }.collect { boundsT ->
                    val slotAlpha = if (isSeriesOpen) {
                        if (boundsT > 0.001f || drawerBlock.books.isNotEmpty()) 0f else 1f
                    } else {
                        if (boundsT <= 0.04f) 1f else 0f
                    }
                    activeSeriesBoundsT.floatValue = boundsT
                    activeSeriesSlotAlpha.floatValue = slotAlpha
                }
            }

            DisposableEffect(drawerBlock.key) {
                onDispose {
                    activeSeriesBoundsT.floatValue = 0f
                    activeSeriesSlotAlpha.floatValue = 1f
                    seriesLiveState.boundsT = 0f
                    seriesLiveState.contentAlpha = 0f
                }
            }

            LaunchedEffect(isSeriesOpen, drawerBlock.key, transition) {
                if (isSeriesOpen) return@LaunchedEffect
                snapshotFlow {
                    val activeMag = maxOf(
                        abs(rawWidthTState.value),
                        abs(rawHeightTState.value),
                        abs(centerTState.value),
                    )
                    activeMag <= 0.003f || (
                        !transition.targetState &&
                            !transition.currentState &&
                            transitionState.isIdle &&
                            !transition.isRunning
                        )
                }.collectLatest { done ->
                    if (done) {
                        activeSeriesBoundsT.floatValue = 0f
                        activeSeriesSlotAlpha.floatValue = 1f
                        seriesLiveState.collapsedToGrid = true
                        seriesTileCoverCoords.clear()
                        seriesOriginRect = null
                        retainedSeriesBlockState.value = null
                    }
                }
            }

            val scrimBaseAlpha = if (palette.isLight) 0.24f else 0.50f
            val originCornerDp = if ((capturedOriginRect?.width ?: 100f) < with(density) { 72.dp.toPx() }) 8f else 12f
            val originCornerPx = with(density) { originCornerDp.dp.toPx() }
            val targetCornerPx = with(density) { 28.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPlaced { coords ->
                        val pos = coords.positionInRoot()
                        if (pos != overlayOriginInRoot) {
                            overlayOriginInRoot = pos
                        }
                    },
            ) {
                // Scrim behind Liquid Glass card (draw-phase alpha read: zero recomposition per frame)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val widthProgress = rawWidthTState.value.coerceIn(0f, 1f)
                            val heightProgress = rawHeightTState.value.coerceIn(0f, 1f)
                            val boundsT = maxOf(widthProgress, heightProgress)
                            val scrimProgress = if (isSeriesOpen) {
                                (boundsT * 1.15f).coerceIn(0f, 1f)
                            } else {
                                ((boundsT - 0.58f) / 0.42f).coerceIn(0f, 1f)
                            }
                            val alpha = scrimBaseAlpha * scrimProgress
                            if (alpha > 0.001f) {
                                drawRect(Color.Black.copy(alpha = alpha))
                            }
                        }
                        .clickable(
                            interactionSource = scrimInteraction,
                            indication = null,
                            onClick = { activeSeriesBlockKeyState.value = null },
                        ),
                )

                val hMarginPx = with(density) { 16.dp.toPx() }
                val safeTopPx = with(density) { (topInset + 14.dp).toPx() }
                val safeBottomInsetPx = with(density) { (bottomInset + 22.dp).toPx() }
                val maxWidthPx = with(density) { 412.dp.roundToPx() }
                val maxHeightPx = with(density) { maxCardHeight.roundToPx() }
                val minHeightPx = with(density) { 200.dp.roundToPx() }
                val fallbackCoverWPx = with(density) { 104.dp.toPx() }

                Layout(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        // Single fixed-size panel container measured ONCE at target (endW, endH);
                        // GPU graphicsLayer handles all spring morphing without relayout or GlassNode shader rebuilds.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp)
                                .graphicsLayer {
                                    val c = centerTState.value
                                    val rawW = rawWidthTState.value
                                    val rawH = rawHeightTState.value
                                    val widthMorphT = rawW
                                    val heightMorphT = rawH
                                    val boundsT = maxOf(rawW.coerceIn(0f, 1f), rawH.coerceIn(0f, 1f))
                                    seriesLiveState.boundsT = boundsT

                                    val endW = panelGeometry.endW.coerceAtLeast(1f)
                                    val endH = panelGeometry.endH.coerceAtLeast(1f)
                                    val overlayW = panelGeometry.overlayW.coerceAtLeast(1f)
                                    val overlayH = panelGeometry.overlayH.coerceAtLeast(1f)

                                    val curW = lerpSeriesFloat(panelGeometry.startW, endW, widthMorphT)
                                        .coerceIn(16f, (overlayW - 8f).coerceAtLeast(16f))
                                    val curH = lerpSeriesFloat(panelGeometry.startH, endH, heightMorphT)
                                        .coerceIn(16f, (overlayH - 8f).coerceAtLeast(16f))
                                    val sx = (curW / endW).coerceAtLeast(0.01f)
                                    val sy = (curH / endH).coerceAtLeast(0.01f)

                                    val rawCurCx = lerpSeriesFloat(panelGeometry.startCx, panelGeometry.endCx, c)
                                    val rawCurCy = lerpSeriesFloat(panelGeometry.startCy, panelGeometry.endCy, c)
                                    val curCx = rawCurCx.coerceIn(
                                        4f + curW * 0.5f,
                                        (overlayW - 4f - curW * 0.5f).coerceAtLeast(4f + curW * 0.5f),
                                    )
                                    val curCy = rawCurCy.coerceIn(
                                        4f + curH * 0.5f,
                                        (overlayH - 4f - curH * 0.5f).coerceAtLeast(4f + curH * 0.5f),
                                    )

                                    transformOrigin = TransformOrigin.Center
                                    scaleX = sx
                                    scaleY = sy
                                    translationX = curCx - panelGeometry.endCx
                                    translationY = curCy - panelGeometry.endCy

                                    val isMorphing = abs(rawW - 1f) > 0.002f ||
                                        abs(rawH - 1f) > 0.002f ||
                                        abs(c - 1f) > 0.002f
                                    if (isMorphing) {
                                        val desiredCornerPx = lerpSeriesFloat(
                                            originCornerPx,
                                            targetCornerPx,
                                            boundsT,
                                        ).coerceAtLeast(1f)
                                        // Equal rx == ry lets HWUI use native Outline.setRoundRect instead of Outline.setPath offscreen masking.
                                        val safeMinScale = minOf(sx, sy).coerceAtLeast(0.08f)
                                        val uniformRadius = ((((desiredCornerPx / safeMinScale) * 0.5f).roundToInt()) * 2f).coerceAtLeast(1f)
                                        shape = SeriesModalMorphCornerShape(uniformRadius)
                                        clip = true
                                    } else {
                                        shape = stableGlassShape
                                        clip = false
                                    }
                                },
                        ) {
                            // [0] Clean untinted Liquid Glass surface at fixed target size & shape
                            val modalGridState = remember(drawerBlock.key) {
                                LazyGridState(
                                    prefetchStrategy = TransitionGatedGridPrefetchStrategy(
                                        isBusy = {
                                            !isSeriesOpen || resolvedIsReaderOverlayActiveState.value
                                        },
                                    ),
                                )
                            }
                            val modalListState = remember(drawerBlock.key) {
                                LazyListState(
                                    prefetchStrategy = TransitionGatedListPrefetchStrategy(
                                        isBusy = {
                                            !isSeriesOpen || resolvedIsReaderOverlayActiveState.value
                                        },
                                    ),
                                )
                            }
                            val isModalContentScrolled by remember(drawerBlock.key, seriesDrawerGridMode) {
                                derivedStateOf {
                                    if (seriesDrawerGridMode) {
                                        modalGridState.firstVisibleItemIndex > 0 || modalGridState.firstVisibleItemScrollOffset > 6
                                    } else {
                                        modalListState.firstVisibleItemIndex > 0 || modalListState.firstVisibleItemScrollOffset > 6
                                    }
                                }
                            }
                            val isSeriesSettled by remember(drawerBlock.key, isSeriesOpen) {
                                derivedStateOf {
                                    isSeriesOpen &&
                                        rawWidthTState.value >= 0.96f &&
                                        rawHeightTState.value >= 0.96f &&
                                        centerTState.value >= 0.96f
                                }
                            }
                            val modalGlassBackdrop: GlassBackdrop? = null
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .drawWithContent {
                                        val boundsT = maxOf(
                                            rawWidthTState.value.coerceIn(0f, 1f),
                                            rawHeightTState.value.coerceIn(0f, 1f),
                                        )
                                        if (boundsT > 0.002f) {
                                            drawContent()
                                        }
                                    }
                                .graphicsLayer {
                                        val rawW = rawWidthTState.value
                                        val rawH = rawHeightTState.value
                                        val boundsT = maxOf(rawW.coerceIn(0f, 1f), rawH.coerceIn(0f, 1f))
                                        val cardAlpha = if (isSeriesOpen) {
                                            (boundsT / 0.14f).coerceIn(0f, 1f)
                                        } else {
                                            ((boundsT - 0.14f) / 0.18f).coerceIn(0f, 1f)
                                        }
                                        compositingStrategy = if (cardAlpha < 0.999f && cardAlpha > 0.001f) {
                                            CompositingStrategy.ModulateAlpha
                                        } else {
                                            CompositingStrategy.Auto
                                        }
                                        alpha = cardAlpha
                                    }
                                    .ios27GlassSurface(
                                        shape = stableGlassShape,
                                        palette = palette,
                                        style = readingStyle,
                                        variant = Ios27GlassVariant.ReaderSheet,
                                        backdrop = modalGlassBackdrop,
                                        isInteractive = false,
                                        state = null,
                                        showContrastBorder = true,
                                        shadowScale = 0f,
                                    )
                                    .clickable(
                                        interactionSource = sheetInteraction,
                                        indication = null,
                                        onClick = {},
                                    ),
                            )

                            // [1] Series header + volume grid/list clipped cleanly inside stableGlassShape
                            CompositionLocalProvider(
                                LocalVitroBackdrop provides null,
                                LocalGlassDarkTheme provides !palette.isLight,
                            ) {
                                val modalDensity = LocalDensity.current
                                val floatingFadeHeightPx = remember(modalDensity) {
                                    mutableFloatStateOf(with(modalDensity) { 68.dp.toPx() })
                                }
                                val floatingTopClearanceDp = remember { 68.dp }
                                val scrimBaseColor = if (palette.isLight) {
                                    lerpColor(palette.surface, Color.White, 0.52f)
                                } else {
                                    lerpColor(palette.surface, Color(0xFF10131A), 0.58f)
                                }
                                val seriesHeaderScrimBrush = remember(scrimBaseColor, palette.isLight) {
                                    if (palette.isLight) {
                                        Brush.verticalGradient(
                                            0.0f to scrimBaseColor.copy(alpha = 0.97f),
                                            0.52f to scrimBaseColor.copy(alpha = 0.90f),
                                            0.78f to scrimBaseColor.copy(alpha = 0.62f),
                                            0.92f to scrimBaseColor.copy(alpha = 0.24f),
                                            1.0f to Color.Transparent,
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            0.0f to scrimBaseColor.copy(alpha = 0.96f),
                                            0.52f to scrimBaseColor.copy(alpha = 0.88f),
                                            0.78f to scrimBaseColor.copy(alpha = 0.58f),
                                            0.92f to scrimBaseColor.copy(alpha = 0.22f),
                                            1.0f to Color.Transparent,
                                        )
                                    }
                                }
                                    val fadeCachePx = remember { floatArrayOf(-1f) }
                                    val cachedDstInBrush = remember { arrayOfNulls<Brush>(1) }
                                    val applyScrollFadeMask = isSeriesSettled && isModalContentScrolled
                                    Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp)
                                        .onPlaced { seriesModalViewportCoords[0] = it }
                                        .drawWithContent {
                                            val minMorphProgress = minOf(
                                                rawWidthTState.value.coerceIn(0f, 1f),
                                                rawHeightTState.value.coerceIn(0f, 1f),
                                            )
                                            val visAlpha = if (isSeriesOpen) {
                                                ((minMorphProgress - 0.46f) / 0.24f).coerceIn(0f, 1f)
                                            } else {
                                                ((minMorphProgress - 0.76f) / 0.22f).coerceIn(0f, 1f)
                                            }
                                            if (visAlpha > 0.004f) {
                                                drawContent()
                                            }
                                        }
                                        .graphicsLayer {
                                            val rawW = rawWidthTState.value
                                            val rawH = rawHeightTState.value
                                            val c = centerTState.value
                                            val isMorphing = abs(rawW - 1f) > 0.002f ||
                                                abs(rawH - 1f) > 0.002f ||
                                                abs(c - 1f) > 0.002f
                                            shape = stableGlassShape
                                            clip = !isMorphing
                                            val widthProgress = rawW.coerceIn(0f, 1f)
                                            val heightProgress = rawH.coerceIn(0f, 1f)
                                            val minMorphProgress = minOf(widthProgress, heightProgress)
                                            val computedAlpha = if (isSeriesOpen) {
                                                ((minMorphProgress - 0.46f) / 0.24f).coerceIn(0f, 1f)
                                            } else {
                                                ((minMorphProgress - 0.76f) / 0.22f).coerceIn(0f, 1f)
                                            }
                                            seriesLiveState.contentAlpha = computedAlpha
                                            compositingStrategy = if (computedAlpha < 0.999f && computedAlpha > 0.001f) {
                                                CompositingStrategy.ModulateAlpha
                                            } else {
                                                CompositingStrategy.Auto
                                            }
                                            alpha = computedAlpha
                                        },
                                ) {
                                    AnimatedContent(
                                        targetState = seriesDrawerGridMode,
                                        transitionSpec = {
                                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180))) togetherWith
                                                fadeOut(tween(120))
                                        },
                                        label = "seriesDrawerViewSwitch",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                compositingStrategy = if (applyScrollFadeMask) {
                                                    CompositingStrategy.Offscreen
                                                } else {
                                                    CompositingStrategy.Auto
                                                }
                                            }
                                            .drawWithContent {
                                                drawContent()
                                                if (applyScrollFadeMask) {
                                                    val fadeZonePx = (floatingFadeHeightPx.floatValue * 1.06f)
                                                        .coerceAtLeast(62.dp.toPx())
                                                        .coerceAtMost(size.height * 0.45f)
                                                    if (fadeZonePx > 1f && size.height > 1f) {
                                                        val brush = if (
                                                            cachedDstInBrush[0] != null &&
                                                            abs(fadeZonePx - fadeCachePx[0]) <= 0.5f
                                                        ) {
                                                            cachedDstInBrush[0]!!
                                                        } else {
                                                            Brush.verticalGradient(
                                                                0.0f to Color.Black.copy(alpha = 0.06f),
                                                                0.55f to Color.Black.copy(alpha = 0.18f),
                                                                0.82f to Color.Black.copy(alpha = 0.60f),
                                                                1.0f to Color.Black,
                                                                startY = 0f,
                                                                endY = fadeZonePx,
                                                            ).also {
                                                                fadeCachePx[0] = fadeZonePx
                                                                cachedDstInBrush[0] = it
                                                            }
                                                        }
                                                        drawRect(
                                                            brush = brush,
                                                            blendMode = BlendMode.DstIn,
                                                        )
                                                    }
                                                }
                                            },
                                    ) { useGrid ->
                                        if (useGrid) {
                                            LazyVerticalGrid(
                                                state = modalGridState,
                                                columns = GridCells.Fixed(3),
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                contentPadding = PaddingValues(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    top = floatingTopClearanceDp,
                                                    bottom = 16.dp,
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                items(drawerBlock.books, key = { it.id }) { volume ->
                                                    val onCoverPlacedVolume = remember(volume.id) {
                                                        { coords: LayoutCoordinates ->
                                                            seriesTileCoverCoords[volume.id] = coords
                                                            if (activeSeriesBlockKeyState.value != null && seriesLiveState.boundsT >= 0.96f) {
                                                                val rect = coords.unclippedBoundsInRoot()
                                                                if (rect.width >= 8f && rect.height >= 8f) {
                                                                    seriesLiveState.openTileRects[volume.id] = rect
                                                                }
                                                            }
                                                        }
                                                    }
                                                    val onOpenVolume = remember(volume) {
                                                        { bounds: Rect? ->
                                                            // Keep series card open so closing volume zooms back accurately into this slot
                                                            launchBook(volume, bounds)
                                                        }
                                                    }
                                                    val onOpenVolumeActions = remember(volume) {
                                                        { seriesActionTargetBookState.value = volume }
                                                    }
                                                    SeriesVolumeGridTile(
                                                        book = volume,
                                                        palette = palette,
                                                        accentOnGlass = accentOnGlass,
                                                        coverAlpha = 1f,
                                                        onCoverPlaced = onCoverPlacedVolume,
                                                        onOpen = onOpenVolume,
                                                        onOpenActions = onOpenVolumeActions,
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                state = modalListState,
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                contentPadding = PaddingValues(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    top = floatingTopClearanceDp,
                                                    bottom = 16.dp,
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                itemsIndexed(drawerBlock.books, key = { _, volume -> volume.id }) { _, volume ->
                                                    val dismissIfSmall = drawerBlock.books.size <= 2
                                                    val onCoverPlacedVolume = remember(volume.id) {
                                                        { coords: LayoutCoordinates ->
                                                            seriesTileCoverCoords[volume.id] = coords
                                                            if (activeSeriesBlockKeyState.value != null && seriesLiveState.boundsT >= 0.96f) {
                                                                val rect = coords.unclippedBoundsInRoot()
                                                                if (rect.width >= 8f && rect.height >= 8f) {
                                                                    seriesLiveState.openTileRects[volume.id] = rect
                                                                }
                                                            }
                                                        }
                                                    }
                                                    val onOpenVolume = remember(volume) {
                                                        { bounds: Rect? ->
                                                            // Keep series card open so closing volume zooms back accurately into this row
                                                            launchBook(volume, bounds)
                                                        }
                                                    }
                                                    val onEditMetaVolume = remember(volume) {
                                                        { editingBook = volume }
                                                    }
                                                    val onRemoveVolume = remember(volume.id, dismissIfSmall) {
                                                        {
                                                            onRemoveFromSeries(volume.id)
                                                            if (dismissIfSmall) {
                                                                activeSeriesBlockKeyState.value = null
                                                            }
                                                        }
                                                    }
                                                    SeriesVolumeRow(
                                                        book = volume,
                                                        palette = palette,
                                                        accentOnGlass = accentOnGlass,
                                                        coverAlpha = 1f,
                                                        onCoverPlaced = onCoverPlacedVolume,
                                                        onOpen = onOpenVolume,
                                                        onEditMeta = onEditMetaVolume,
                                                        onRemoveFromSeries = onRemoveVolume,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Floating Header + vertical gradient shadow over scrolling covers (no flat hard clip)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                            .background(seriesHeaderScrimBrush)
                                            .padding(top = 14.dp, start = 16.dp, end = 16.dp, bottom = 22.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onSizeChanged { sz ->
                                                    if (sz.height > 0) {
                                                        val targetPx = sz.height + with(modalDensity) { 24.dp.toPx() }
                                                        if (floatingFadeHeightPx.floatValue != targetPx) {
                                                            floatingFadeHeightPx.floatValue = targetPx
                                                        }
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = drawerBlock.title,
                                                    color = palette.text,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${drawerBlock.author} · ${drawerBlock.books.size} 卷 · 平均 ${(drawerBlock.progress * 100).roundToInt()}%",
                                                    color = palette.text.copy(alpha = if (palette.isLight) 0.76f else 0.84f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                FrostedCircleIconButton(
                                                    icon = if (seriesDrawerGridMode) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                                    contentDescription = if (seriesDrawerGridMode) "切换列表视图" else "切换网格视图",
                                                    palette = palette,
                                                    backdrop = null,
                                                    size = 34.dp,
                                                    showContrastBorder = false,
                                                    animateIconChange = true,
                                                    onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        seriesDrawerGridMode = !seriesDrawerGridMode
                                                    },
                                                )
                                                FrostedCircleIconButton(
                                                    icon = Icons.Rounded.Edit,
                                                    contentDescription = "重命名系列",
                                                    palette = palette,
                                                    backdrop = null,
                                                    size = 34.dp,
                                                    tint = palette.text,
                                                    showContrastBorder = false,
                                                    onClick = { renamingSeriesBlock = drawerBlock },
                                                )
                                                FrostedCircleIconButton(
                                                    icon = Icons.Rounded.Close,
                                                    contentDescription = "关闭",
                                                    palette = palette,
                                                    backdrop = null,
                                                    size = 34.dp,
                                                    showContrastBorder = false,
                                                    onClick = { activeSeriesBlockKeyState.value = null },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // [2] Lead series cover cross-fade preserving exact 0.71 shelf cover crop & fading smoothly onto tile
                            if (drawerBlock.books.isNotEmpty()) {
                                val leadBook = drawerBlock.books.first()
                                val leadTargetW = capturedOriginRect?.width?.takeIf { it >= 8f }
                                    ?: with(density) { 104.dp.toPx() }
                                val leadTargetH = capturedOriginRect?.height?.takeIf { it >= 8f }
                                    ?: (leadTargetW / 0.71f)
                                val leadWidthDp = with(density) { leadTargetW.toDp() }
                                val leadHeightDp = with(density) { leadTargetH.toDp() }
                                Box(
                                    modifier = Modifier.matchParentSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    BookCover(
                                        book = leadBook,
                                        palette = palette,
                                        width = leadWidthDp,
                                        height = leadHeightDp,
                                        cornerRadius = 0.dp,
                                        modifier = Modifier
                                            .requiredSize(width = leadWidthDp, height = leadHeightDp)
                                            .graphicsLayer {
                                                val rawW = rawWidthTState.value
                                                val rawH = rawHeightTState.value
                                                val widthMorphT = rawW.coerceAtLeast(0f)
                                                val heightMorphT = rawH.coerceAtLeast(0f)
                                                val boundsT = maxOf(rawW.coerceIn(0f, 1f), rawH.coerceIn(0f, 1f))

                                                val endW = panelGeometry.endW.coerceAtLeast(1f)
                                                val endH = panelGeometry.endH.coerceAtLeast(1f)
                                                val overlayW = panelGeometry.overlayW.coerceAtLeast(1f)
                                                val overlayH = panelGeometry.overlayH.coerceAtLeast(1f)
                                                val startW = panelGeometry.startW.coerceAtLeast(1f)
                                                val startH = panelGeometry.startH.coerceAtLeast(1f)

                                                val curW = lerpSeriesFloat(startW, endW, widthMorphT)
                                                    .coerceIn(16f, (overlayW - 8f).coerceAtLeast(16f))
                                                val curH = lerpSeriesFloat(startH, endH, heightMorphT)
                                                    .coerceIn(16f, (overlayH - 8f).coerceAtLeast(16f))
                                                val sx = (curW / endW).coerceAtLeast(0.01f)
                                                val sy = (curH / endH).coerceAtLeast(0.01f)
                                                val uniformScreenScale = maxOf(
                                                    curW / leadTargetW.coerceAtLeast(1f),
                                                    curH / leadTargetH.coerceAtLeast(1f),
                                                )

                                                val leadAlpha = if (isSeriesOpen) {
                                                    (1f - ((boundsT - 0.28f) / 0.26f)).coerceIn(0f, 1f)
                                                } else {
                                                    ((0.88f - boundsT) / 0.18f).coerceIn(0f, 1f)
                                                }
                                                compositingStrategy = if (leadAlpha < 0.999f && leadAlpha > 0.001f) {
                                                    CompositingStrategy.ModulateAlpha
                                                } else {
                                                    CompositingStrategy.Auto
                                                }
                                                scaleX = (uniformScreenScale / sx).coerceAtLeast(0.01f)
                                                scaleY = (uniformScreenScale / sy).coerceAtLeast(0.01f)
                                                alpha = leadAlpha
                                            },
                                    )
                                }
                            }
                        }
                    },
                ) { measurables, constraints ->
                    val overlayW = constraints.maxWidth.coerceAtLeast(1)
                    val overlayH = constraints.maxHeight.coerceAtLeast(1)
                    val overlayWf = overlayW.toFloat()
                    val overlayHf = overlayH.toFloat()

                    val targetW = min(
                        (overlayWf * 0.92f).roundToInt(),
                        min(maxWidthPx, (overlayW - (hMarginPx * 2f).roundToInt()).coerceAtLeast(180)),
                    ).coerceAtLeast(180)
                    val availH = (overlayH - (safeTopPx + safeBottomInsetPx).roundToInt()).coerceAtLeast(180)
                    val cappedMaxH = min(maxHeightPx, availH).coerceAtLeast(min(minHeightPx, availH))

                    val panelPlaceable = measurables[0].measure(
                        Constraints(
                            minWidth = targetW,
                            maxWidth = targetW,
                            minHeight = 0,
                            maxHeight = cappedMaxH,
                        ),
                    )
                    val endW = panelPlaceable.width.toFloat().coerceAtLeast(1f)
                    val endH = panelPlaceable.height.toFloat().coerceAtLeast(1f)

                    val rawOrigin = capturedOriginRect?.takeIf { it.width >= 8f && it.height >= 8f }
                    val fallbackH = fallbackCoverWPx / 0.71f
                    val startRect = if (rawOrigin != null) {
                        rawOrigin.translate(-overlayOriginInRoot.x, -overlayOriginInRoot.y)
                    } else {
                        Rect(
                            left = (overlayWf - fallbackCoverWPx) * 0.5f,
                            top = (overlayHf - fallbackH) * 0.5f,
                            right = (overlayWf + fallbackCoverWPx) * 0.5f,
                            bottom = (overlayHf + fallbackH) * 0.5f,
                        )
                    }
                    val startW = startRect.width.coerceAtLeast(16f)
                    val startH = startRect.height.coerceAtLeast(16f)

                    val safeBottom = (overlayHf - safeBottomInsetPx).coerceAtLeast(safeTopPx + 1f)
                    val usableCenterY = (safeTopPx + safeBottom) * 0.5f
                    val endLeft = ((overlayWf - endW) * 0.5f).coerceIn(8f, (overlayWf - endW - 8f).coerceAtLeast(8f))
                    val endTop = (usableCenterY - endH * 0.5f).coerceIn(
                        safeTopPx.coerceAtMost((overlayHf - endH - 8f).coerceAtLeast(8f)),
                        (safeBottom - endH).coerceAtLeast(safeTopPx),
                    )
                    val placedEndLeft = endLeft.roundToInt()
                    val placedEndTop = endTop.roundToInt()

                    panelGeometry.overlayW = overlayWf
                    panelGeometry.overlayH = overlayHf
                    panelGeometry.startCx = startRect.center.x
                    panelGeometry.startCy = startRect.center.y
                    panelGeometry.startW = startW
                    panelGeometry.startH = startH
                    panelGeometry.endCx = placedEndLeft + endW * 0.5f
                    panelGeometry.endCy = placedEndTop + endH * 0.5f
                    panelGeometry.endW = endW
                    panelGeometry.endH = endH

                    layout(overlayW, overlayH) {
                        panelPlaceable.placeRelative(placedEndLeft, placedEndTop)
                    }
                }
            }
        }

        // Single Volume Action Dialog from Series Grid Tile
        seriesActionTargetBookState.value?.let { actionVolume ->
            val parentBlock = retainedSeriesBlock ?: activeSeriesBlock
            LiquidGlassDialog(
                onDismissRequest = { seriesActionTargetBookState.value = null },
                palette = palette,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = cleanBookTitle(actionVolume.title),
                        color = palette.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${actionVolume.safeCurrentChapter.title} · ${(actionVolume.progress * 100).roundToInt()}%",
                        color = palette.text.copy(alpha = if (palette.isLight) 0.62f else 0.70f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ShelfDialogPillButton(
                        label = if (actionVolume.progress > 0.0) "继续阅读" else "开始阅读",
                        palette = palette,
                        isPrimary = true,
                        onClick = {
                            val volumeBounds = seriesTileCoverCoords[actionVolume.id]
                                ?.takeIf { it.isAttached }
                                ?.unclippedBoundsInRoot()
                            seriesActionTargetBookState.value = null
                            launchBook(actionVolume, volumeBounds)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ShelfDialogPillButton(
                        label = "编辑书籍信息",
                        palette = palette,
                        isPrimary = false,
                        onClick = {
                            seriesActionTargetBookState.value = null
                            editingBook = actionVolume
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ShelfDialogPillButton(
                        label = "移出当前系列",
                        palette = palette,
                        isPrimary = false,
                        isDanger = true,
                        onClick = {
                            seriesActionTargetBookState.value = null
                            onRemoveFromSeries(actionVolume.id)
                            if ((parentBlock?.books?.size ?: 0) <= 2) {
                                activeSeriesBlockKeyState.value = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Rename Series Dialog (Vitro iOS 27 Glass)
        renamingSeriesBlock?.let { seriesBlock ->
            var newSeriesName by remember(seriesBlock.key) { mutableStateOf(seriesBlock.title) }
            LiquidGlassDialog(
                onDismissRequest = { renamingSeriesBlock = null },
                palette = palette,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "重命名系列分组",
                        color = palette.text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    ShelfGlassTextField(
                        value = newSeriesName,
                        onValueChange = { newSeriesName = it },
                        label = "系列名称",
                        palette = palette,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        ShelfDialogPillButton(
                            label = "取消",
                            palette = palette,
                            isPrimary = false,
                            onClick = { renamingSeriesBlock = null },
                        )
                        ShelfDialogPillButton(
                            label = "保存",
                            palette = palette,
                            isPrimary = true,
                            onClick = {
                                val trimmed = newSeriesName.trim()
                                if (trimmed.isNotEmpty()) {
                                    onGroupSeries(
                                        seriesBlock.books.map { it.id }.toSet(),
                                        trimmed,
                                    )
                                }
                                renamingSeriesBlock = null
                                activeSeriesBlockKeyState.value = null
                            },
                        )
                    }
                }
            }
        }
        }

        // Edit Book Metadata Dialog (Vitro iOS 27 Glass)
        editingBook?.let { target ->
            var titleInput by remember(target.id) { mutableStateOf(target.title) }
            var authorInput by remember(target.id) { mutableStateOf(target.author) }

            LiquidGlassDialog(
                onDismissRequest = { editingBook = null },
                palette = palette,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "编辑书籍信息",
                        color = palette.text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    ShelfGlassTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = "书名",
                        palette = palette,
                    )
                    ShelfGlassTextField(
                        value = authorInput,
                        onValueChange = { authorInput = it },
                        label = "作者",
                        palette = palette,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        ShelfDialogPillButton(
                            label = "取消",
                            palette = palette,
                            isPrimary = false,
                            onClick = { editingBook = null },
                        )
                        ShelfDialogPillButton(
                            label = "保存",
                            palette = palette,
                            isPrimary = true,
                            onClick = {
                                onUpdateBookMeta(
                                    target.id,
                                    titleInput.trim().ifEmpty { target.title },
                                    authorInput.trim().ifEmpty { target.author },
                                )
                                editingBook = null
                            },
                        )
                    }
                }
            }
        }

        // Create Shelf In-Window Liquid Glass Modal (spring pop-out, live backdrop refraction)
        if (createShelfRendered) {
            var shelfNameInput by remember { mutableStateOf("") }
            LaunchedEffect(showCreateShelfDialog) {
                if (showCreateShelfDialog) {
                    shelfNameInput = ""
                }
            }
            BackHandler(enabled = showCreateShelfDialog) {
                showCreateShelfDialog = false
            }
            val createShelfTransition = updateTransition(
                transitionState = createShelfVisibleState,
                label = "shelfCreateDialogTransition",
            )
            val modalScaleProgress by createShelfTransition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        spring(dampingRatio = 0.78f, stiffness = 440f)
                    } else {
                        spring(dampingRatio = 0.96f, stiffness = 560f)
                    }
                },
                label = "shelfCreateModalScale",
            ) { shown -> if (shown) 1f else 0f }
            val modalAlpha by createShelfTransition.animateFloat(
                transitionSpec = {
                    tween(durationMillis = if (targetState) 180 else 140)
                },
                label = "shelfCreateModalAlpha",
            ) { shown -> if (shown) 1f else 0f }
            val scrimInteraction = remember { MutableInteractionSource() }
            val cardInteraction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(40f)
                    .background(
                        Color.Black.copy(alpha = (if (palette.isLight) 0.24f else 0.44f) * modalAlpha.coerceIn(0f, 1f)),
                    )
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                    ) {
                        showCreateShelfDialog = false
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .imePadding()
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val s = 0.86f + (1f - 0.86f) * modalScaleProgress
                            scaleX = s
                            scaleY = s
                            alpha = modalAlpha.coerceIn(0f, 1f)
                        }
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                        ) {}
                        .ios27GlassSurface(
                            shape = RoundedCornerShape(28.dp),
                            palette = palette,
                            style = readingStyle,
                            variant = Ios27GlassVariant.ReaderSheet,
                            backdrop = activeScreenBackdrop,
                            isInteractive = false,
                            showContrastBorder = false,
                        )
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                ) {
                    CompositionLocalProvider(
                        LocalVitroBackdrop provides null,
                        LocalGlassDarkTheme provides !palette.isLight,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "新建书架分类",
                                color = palette.text,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            ShelfGlassTextField(
                                value = shelfNameInput,
                                onValueChange = { shelfNameInput = it },
                                label = "书架名称",
                                placeholder = "例如：轻小说、推理、待看",
                                palette = palette,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            ) {
                                ShelfDialogPillButton(
                                    label = "取消",
                                    palette = palette,
                                    isPrimary = false,
                                    onClick = { showCreateShelfDialog = false },
                                )
                                ShelfDialogPillButton(
                                    label = "创建",
                                    palette = palette,
                                    isPrimary = true,
                                    onClick = {
                                        val trimmed = shelfNameInput.trim()
                                        if (trimmed.isNotEmpty()) {
                                            onAddShelf(trimmed)
                                            selectedTab = trimmed
                                        }
                                        showCreateShelfDialog = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete Custom Shelf Confirmation Dialog (Vitro iOS 27 Glass)
        shelfPendingDelete?.let { targetShelf ->
            LiquidGlassDialog(
                onDismissRequest = { shelfPendingDelete = null },
                palette = palette,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "删除书架分类",
                        color = palette.text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "确定删除书架「$targetShelf」吗？书架内的书籍不会被删除，将回到全部书籍中。",
                        color = palette.text.copy(alpha = if (palette.isLight) 0.68f else 0.74f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        ShelfDialogPillButton(
                            label = "取消",
                            palette = palette,
                            isPrimary = false,
                            onClick = { shelfPendingDelete = null },
                        )
                        ShelfDialogPillButton(
                            label = "删除",
                            palette = palette,
                            isPrimary = true,
                            isDanger = true,
                            onClick = {
                                onDeleteShelf(targetShelf)
                                if (selectedTab == targetShelf) {
                                    selectedTab = DEFAULT_SHELF_NAME
                                }
                                shelfPendingDelete = null
                            },
                        )
                    }
                }
            }
        }

        // Move Selected Books to Shelf Sheet (Vitro iOS 27 Glass)
        if (showMoveShelfSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val sheetBg = if (palette.isLight) {
                Color(0xFFF6F7FB).copy(alpha = 0.92f)
            } else {
                Color(0xFF171822).copy(alpha = 0.92f)
            }
            val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

            ModalBottomSheet(
                onDismissRequest = { showMoveShelfSheet = false },
                sheetState = sheetState,
                shape = sheetShape,
                containerColor = Color.Transparent,
                dragHandle = null,
            ) {
                CompositionLocalProvider(
                    LocalVitroBackdrop provides null,
                    LocalGlassDarkTheme provides !palette.isLight,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(sheetShape)
                            .background(sheetBg)
                            .border(
                                width = 0.5.dp,
                                color = if (palette.isLight) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.12f),
                                shape = sheetShape,
                            ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(36.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(palette.text.copy(alpha = if (palette.isLight) 0.18f else 0.24f)),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "移动 ${selectedBookIds.size} 本书至书架",
                                color = palette.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            ShelfMoveOptionCard(
                                label = "默认书架（移出自定义分类）",
                                palette = palette,
                                backdrop = null,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onMoveBooksToShelf(selectedBookIds, null)
                                    forceSelectionMode = false
                                    selectedBookIds = emptySet()
                                    showMoveShelfSheet = false
                                },
                            )

                            customShelves.forEach { shelf ->
                                ShelfMoveOptionCard(
                                    label = shelf,
                                    palette = palette,
                                    backdrop = null,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onMoveBooksToShelf(selectedBookIds, shelf)
                                        forceSelectionMode = false
                                        selectedBookIds = emptySet()
                                        showMoveShelfSheet = false
                                    },
                                )
                            }

                            val newShelfInteraction = remember { MutableInteractionSource() }
                            val newShelfGlassState = rememberSafeGlassEffectState(
                                interactionSource = newShelfInteraction,
                                active = showMoveShelfSheet,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .ios27GlassSurface(
                                        shape = CircleShape,
                                        palette = palette,
                                        style = readingStyle,
                                        variant = Ios27GlassVariant.AccentPill,
                                        backdrop = null,
                                        isInteractive = true,
                                        showContrastBorder = false,
                                        state = newShelfGlassState,
                                        interactionSource = newShelfInteraction,
                                    )
                                    .clickable(
                                        interactionSource = newShelfInteraction,
                                        indication = null,
                                        onClick = {
                                            showMoveShelfSheet = false
                                            showCreateShelfDialog = true
                                        },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = accentOnGlass, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "新建书架",
                                    color = accentOnGlass,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Group Selected Books as Series Dialog (Vitro iOS 27 Glass)
        if (showGroupSeriesDialog) {
            val defaultSeriesName = remember(selectedBookIds, books) {
                val firstSelected = books.firstOrNull { it.id in selectedBookIds }
                if (firstSelected != null) normalizeSeriesBaseTitle(firstSelected.title) else ""
            }
            var seriesNameInput by remember(defaultSeriesName) { mutableStateOf(defaultSeriesName) }

            LiquidGlassDialog(
                onDismissRequest = { showGroupSeriesDialog = false },
                palette = palette,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "合并为系列丛书",
                        color = palette.text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "将已选的 ${selectedBookIds.size} 本书归入同一个系列封面堆叠：",
                        color = palette.text.copy(alpha = if (palette.isLight) 0.68f else 0.74f),
                        fontSize = 13.sp,
                    )
                    ShelfGlassTextField(
                        value = seriesNameInput,
                        onValueChange = { seriesNameInput = it },
                        label = "系列名称",
                        palette = palette,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        ShelfDialogPillButton(
                            label = "取消",
                            palette = palette,
                            isPrimary = false,
                            onClick = { showGroupSeriesDialog = false },
                        )
                        ShelfDialogPillButton(
                            label = "合并",
                            palette = palette,
                            isPrimary = true,
                            onClick = {
                                val trimmed = seriesNameInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onGroupSeries(selectedBookIds, trimmed)
                                    forceSelectionMode = false
                                    selectedBookIds = emptySet()
                                }
                                showGroupSeriesDialog = false
                            },
                        )
                    }
                }
            }
        }

        // Delete Selected Books Confirmation Dialog (Vitro iOS 27 Glass)
        if (showDeleteConfirmDialog) {
            LiquidGlassDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                palette = palette,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "删除所选书籍",
                        color = palette.text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "确定从书架中移除已选择的 ${selectedBookIds.size} 本书籍吗？",
                        color = palette.text.copy(alpha = if (palette.isLight) 0.68f else 0.74f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        ShelfDialogPillButton(
                            label = "取消",
                            palette = palette,
                            isPrimary = false,
                            onClick = { showDeleteConfirmDialog = false },
                        )
                        ShelfDialogPillButton(
                            label = "确认删除",
                            palette = palette,
                            isPrimary = true,
                            isDanger = true,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeleteBooks(selectedBookIds)
                                forceSelectionMode = false
                                selectedBookIds = emptySet()
                                showDeleteConfirmDialog = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private val AlwaysZeroFloatProvider: () -> Float = { 0f }
private val AlwaysOneFloatProvider: () -> Float = { 1f }

@Composable
private fun FrostedCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    size: Dp = 38.dp,
    tint: Color = palette.text,
    hidden: Boolean = false,
    orbVisibility: () -> Float = AlwaysOneFloatProvider,
    suspendBlurState: State<Boolean>? = null,
    showContrastBorder: Boolean = false,
    animateIconChange: Boolean = false,
    onClick: () -> Unit,
) {
    val readingStyle = LocalReadingStyle.current
    val usesLiveVitro = backdrop != null && !LocalPowerSaveMode.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentHidden by rememberUpdatedState(hidden)
    val currentOrbVisibility by rememberUpdatedState(orbVisibility)
    val hideOrbGlass by remember {
        derivedStateOf { currentOrbVisibility() <= 0.01f }
    }
    val orbLandScale = remember { Animatable(1f) }
    val orbLanded by remember {
        derivedStateOf { currentOrbVisibility() >= 0.15f }
    }
    var orbLandArmed by remember { mutableStateOf(false) }
    LaunchedEffect(orbLanded) {
        if (orbLanded) {
            if (orbLandArmed) {
                orbLandScale.snapTo(0.86f)
                orbLandScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.36f, stiffness = 460f),
                    initialVelocity = 1.6f,
                )
            }
            orbLandArmed = false
        } else {
            orbLandArmed = true
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val glassState = rememberSafeGlassEffectState(
        interactionSource = interaction,
        active = true,
    )
    // Keep GlassNode and modifier chain structure warm/invariant across Popover hide/unhide.
    // When live Vitro is active and hideOrbGlass == true, keep outer alpha = 1f and pass isHidden = true
    // (Glass.Identity) so GlassNode.draw() calls clear(pane) instead of leaving a stale orb ghost in backdrop,
    // while hiding the inner Icon at alpha = 0f.
    val buttonModifier = Modifier
        .size(size)
        .graphicsLayer {
            alpha = if (hideOrbGlass && usesLiveVitro) {
                1f
            } else {
                currentOrbVisibility().coerceIn(0f, 1f)
            }
            val landScale = orbLandScale.value
            scaleX = landScale
            scaleY = landScale
        }
        .ios27GlassSurface(
            shape = CircleShape,
            palette = palette,
            style = readingStyle,
            variant = Ios27GlassVariant.Chrome,
            backdrop = backdrop,
            isInteractive = true,
            isHidden = hideOrbGlass,
            suspendVitroBlur = suspendBlurState?.value == true,
            showContrastBorder = showContrastBorder,
            state = glassState,
            interactionSource = interaction,
        )
        // Track pointer release directly until physical finger-up (never cancel on edge exit while stretching)
        // so Vitro executes its bouncy ReleaseSpring smoothly when stretched orbs are released.
        .pointerInput(interaction) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (currentHidden) return@awaitEachGesture
                val startPos = down.position
                var pointerId = down.id
                var maxDistPx = 0f
                var committed = false
                val press = PressInteraction.Press(startPos)
                interaction.tryEmit(press)
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            committed = change != null
                            break
                        }
                        pointerId = change.id
                        val dist = hypot(change.position.x - startPos.x, change.position.y - startPos.y)
                        if (dist > maxDistPx) maxDistPx = dist
                        if (maxDistPx > viewConfiguration.touchSlop * 0.45f) {
                            change.consume()
                        }
                    }
                } finally {
                    interaction.tryEmit(
                        if (committed) PressInteraction.Release(press)
                        else PressInteraction.Cancel(press),
                    )
                }
                if (committed && !currentHidden && maxDistPx <= viewConfiguration.touchSlop) {
                    currentOnClick()
                }
            }
        }
        .semantics(mergeDescendants = true) {
            role = Role.Button
            onClick(label = contentDescription) {
                if (!currentHidden) {
                    currentOnClick()
                    true
                } else {
                    false
                }
            }
        }
    Box(
        modifier = buttonModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (!animateIconChange) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = if (hideOrbGlass) 0f else 1f
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(19.dp),
                )
            }
        } else {
            val iconTwist by animateFloatAsState(
                targetValue = if (icon == Icons.Rounded.Close) 90f else 0f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 480f,
                ),
                label = "frostedOrbIconTwist",
            )
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(durationMillis = 190)) +
                        scaleIn(
                            initialScale = 0.56f,
                            animationSpec = spring(dampingRatio = 0.74f, stiffness = 540f),
                        )) togetherWith
                        (fadeOut(animationSpec = tween(durationMillis = 130)) +
                            scaleOut(
                                targetScale = 0.56f,
                                animationSpec = tween(durationMillis = 130),
                            ))
                },
                label = "frostedCircleOrbIcon",
                modifier = Modifier.graphicsLayer {
                    alpha = if (hideOrbGlass) 0f else 1f
                    rotationZ = iconTwist
                },
            ) { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

private class ShelfCardLayerCacheKey {
    var bookId: String? = null
    var coverPath: String? = null
    var volumeCount: Int = -1
    var isLight: Boolean = false
    var palettePrimaryValue: ULong = 0uL
    var paletteBgValue: ULong = 0uL
    var paletteAccentTextValue: ULong = 0uL
    var isSelectionMode: Boolean = false
    var isSelected: Boolean = false
    var isPartial: Boolean = false
    var thumbReady: Boolean = false
    var settled: Boolean = false
    var widthPx: Int = -1
    var heightPx: Int = -1

    fun invalidate() {
        bookId = null
        settled = false
    }

    fun matchesAndUpdate(
        bookId: String,
        coverPath: String?,
        volumeCount: Int,
        isLight: Boolean,
        palette: AppPalette,
        isSelectionMode: Boolean,
        isSelected: Boolean,
        isPartial: Boolean,
        thumbReady: Boolean,
        settled: Boolean,
        widthPx: Int,
        heightPx: Int,
    ): Boolean {
        val primaryVal = palette.primary.value
        val bgVal = palette.background.value
        val accentVal = palette.accentText.value
        if (
            this.bookId == bookId &&
            this.coverPath == coverPath &&
            this.volumeCount == volumeCount &&
            this.isLight == isLight &&
            this.palettePrimaryValue == primaryVal &&
            this.paletteBgValue == bgVal &&
            this.paletteAccentTextValue == accentVal &&
            this.isSelectionMode == isSelectionMode &&
            this.isSelected == isSelected &&
            this.isPartial == isPartial &&
            this.thumbReady == thumbReady &&
            this.settled == settled &&
            this.widthPx == widthPx &&
            this.heightPx == heightPx
        ) {
            return true
        }
        this.bookId = bookId
        this.coverPath = coverPath
        this.volumeCount = volumeCount
        this.isLight = isLight
        this.palettePrimaryValue = primaryVal
        this.paletteBgValue = bgVal
        this.paletteAccentTextValue = accentVal
        this.isSelectionMode = isSelectionMode
        this.isSelected = isSelected
        this.isPartial = isPartial
        this.thumbReady = thumbReady
        this.settled = settled
        this.widthPx = widthPx
        this.heightPx = heightPx
        return false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShelfGrid3Item(
    block: ShelfBookBlock,
    palette: AppPalette,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isPartial: Boolean,
    activeSeriesOverlayKeyState: State<String?>? = null,
    seriesSlotAlpha: () -> Float = AlwaysOneFloatProvider,
    onCoverPlaced: ((LayoutCoordinates) -> Unit)? = null,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
) {
    val density = LocalDensity.current
    val isLight = palette.isLight
    val leadBook = block.books.first()
    val volumeCount = block.books.size
    val heroSlotAlpha = LocalHeroBookSlotAlpha.current
    val coverPath = leadBook.coverPath?.takeIf { it.isNotBlank() }
    val isCoverAssetReady by remember(leadBook.id, coverPath) {
        derivedStateOf {
            CoverAssetCache.versionTick
            coverPath == null ||
                CoverAssetCache.getThumb(leadBook.id) != null ||
                CoverBitmapCache.get(coverPath) != null
        }
    }
    var isCardCacheSettled by remember(leadBook.id, coverPath) {
        mutableStateOf(isCoverAssetReady)
    }
    LaunchedEffect(leadBook.id, coverPath, isCoverAssetReady) {
        if (isCoverAssetReady) {
            if (!isCardCacheSettled) {
                // Allow AsyncBookCoverContent's 220ms fade-in to reach alpha 1f before freezing into GraphicsLayer.
                kotlinx.coroutines.delay(360L)
                isCardCacheSettled = true
            }
        } else {
            isCardCacheSettled = false
        }
    }
    val cardLayer: GraphicsLayer = rememberGraphicsLayer()
    val layerCacheKey = remember { ShelfCardLayerCacheKey() }
    val targetSelectScale = if (isSelectionMode && (isSelected || isPartial)) 0.94f else 1f
    val selectionScaleAnim = remember { Animatable(targetSelectScale) }
    LaunchedEffect(targetSelectScale) {
        if (kotlin.math.abs(selectionScaleAnim.value - targetSelectScale) > 0.001f) {
            selectionScaleAnim.animateTo(
                targetValue = targetSelectScale,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
            )
        }
    }
    val coverCoordsHolder = remember(leadBook.id) { arrayOfNulls<LayoutCoordinates>(1) }
    val itemInteraction = remember { MutableInteractionSource() }
    val hairline = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectionScaleAnim.value < 0.999f) {
                    Modifier.graphicsLayer {
                        val s = selectionScaleAnim.value
                        scaleX = s
                        scaleY = s
                    }
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                interactionSource = itemInteraction,
                indication = null,
                onClick = { onClick(coverCoordsHolder[0]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()) },
                onLongClick = onLongClick,
            ),
    ) {
        val backCornerPx = with(density) { 11.dp.toPx() }
        val mainCardShape = SHELF_GRID_MAIN_SHAPE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.71f)
                .drawWithContent {
                    val slotAlpha = if (volumeCount > 1) {
                        if (activeSeriesOverlayKeyState?.value == block.key) {
                            seriesSlotAlpha().coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                    } else {
                        heroSlotAlpha(leadBook.id).coerceIn(0f, 1f)
                    }
                    if (slotAlpha <= 0.002f) return@drawWithContent
                    val wPx = size.width.roundToInt()
                    val hPx = size.height.roundToInt()
                    if (wPx <= 0 || hPx <= 0) return@drawWithContent
                    if (!isCardCacheSettled && slotAlpha >= 0.999f) {
                        layerCacheKey.invalidate()
                        this@drawWithContent.drawContent()
                        return@drawWithContent
                    }
                    val padPx = if (volumeCount > 1) 12.dp.roundToPx() else 0
                    val targetSize = IntSize(wPx + padPx * 2, hPx + padPx * 2)
                    val upToDate = if (isCardCacheSettled) {
                        layerCacheKey.matchesAndUpdate(
                            bookId = leadBook.id,
                            coverPath = coverPath,
                            volumeCount = volumeCount,
                            isLight = isLight,
                            palette = palette,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            isPartial = isPartial,
                            thumbReady = isCoverAssetReady,
                            settled = true,
                            widthPx = wPx,
                            heightPx = hPx,
                        ) && cardLayer.size == targetSize
                    } else {
                        layerCacheKey.invalidate()
                        false
                    }
                    if (!upToDate) {
                        cardLayer.record(size = targetSize) {
                            if (padPx > 0) {
                                translate(left = padPx.toFloat(), top = padPx.toFloat()) {
                                    this@drawWithContent.drawContent()
                                }
                            } else {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
                    cardLayer.alpha = slotAlpha
                    if (padPx > 0) {
                        translate(left = -padPx.toFloat(), top = -padPx.toFloat()) {
                            drawLayer(cardLayer)
                        }
                    } else {
                        drawLayer(cardLayer)
                    }
                }
                .then(
                    if (volumeCount > 1) {
                        Modifier.drawBehind {
                            fun drawBackCard(
                                padStartDp: Dp,
                                padBottomDp: Dp,
                                offsetXDp: Dp,
                                offsetYDp: Dp,
                                cardScale: Float,
                                angleDeg: Float,
                                fillColor: Color,
                            ) {
                                val padStart = padStartDp.toPx()
                                val padBottom = padBottomDp.toPx()
                                val w = (size.width - padStart).coerceAtLeast(1f)
                                val h = (size.height - padBottom).coerceAtLeast(1f)
                                val cx = padStart + w * 0.5f + offsetXDp.toPx()
                                val cy = h * 0.5f + offsetYDp.toPx()
                                translate(left = cx - w * 0.5f, top = cy - h * 0.5f) {
                                    rotate(degrees = angleDeg, pivot = Offset(w * 0.5f, h * 0.5f)) {
                                        scale(
                                            scaleX = cardScale,
                                            scaleY = cardScale,
                                            pivot = Offset(w * 0.5f, h * 0.5f),
                                        ) {
                                            val cornerRadius = CornerRadius(backCornerPx, backCornerPx)
                                            drawRoundRect(
                                                color = fillColor,
                                                topLeft = Offset.Zero,
                                                size = Size(w, h),
                                                cornerRadius = cornerRadius,
                                            )
                                        }
                                    }
                                }
                            }
                            if (volumeCount > 2) {
                                drawBackCard(
                                    padStartDp = 10.dp,
                                    padBottomDp = 10.dp,
                                    offsetXDp = 6.dp,
                                    offsetYDp = (-6).dp,
                                    cardScale = 0.90f,
                                    angleDeg = 3.2f,
                                    fillColor = if (isLight) {
                                        Color(0xFFDCDDE6).copy(alpha = 0.72f)
                                    } else {
                                        Color(0xFF2E303D).copy(alpha = 0.72f)
                                    },
                                )
                            }
                            drawBackCard(
                                padStartDp = 6.dp,
                                padBottomDp = 6.dp,
                                offsetXDp = 3.dp,
                                offsetYDp = (-3).dp,
                                cardScale = 0.95f,
                                angleDeg = 1.6f,
                                fillColor = if (isLight) {
                                    Color(0xFFE7E8F0).copy(alpha = 0.85f)
                                } else {
                                    Color(0xFF3A3C4C).copy(alpha = 0.82f)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.5.dp, hairline, mainCardShape),
            ) {
                BookCover(
                    book = leadBook,
                    palette = palette,
                    width = 112.dp,
                    height = 158.dp,
                    cornerRadius = 12.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .onPlaced { coords ->
                            coverCoordsHolder[0] = coords
                            onCoverPlaced?.invoke(coords)
                        },
                )
            }

            // 轻量半透明系列卷数角标 ("26 卷")
            if (volumeCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = if (isLight) Color(0xFF161820).copy(alpha = 0.72f)
                            else Color(0xFF0E1016).copy(alpha = 0.78f),
                            shape = CircleShape,
                        )
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "$volumeCount 卷",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Selection indicator overlay
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.56f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.26f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected || isPartial) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.RadioButtonUnchecked
                        },
                        contentDescription = null,
                        tint = if (isSelected) palette.accentText else Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = block.title,
            color = palette.text,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        val progressPct = (block.progress * 100).roundToInt()
        Text(
            text = if (volumeCount > 1) {
                "共 $volumeCount 卷 · $progressPct%"
            } else if (progressPct > 0) {
                "已读 $progressPct%"
            } else {
                "未读"
            },
            color = palette.text.copy(alpha = if (isLight) 0.58f else 0.64f),
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShelfListRowItem(
    book: BookEntry,
    palette: AppPalette,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onCoverPlaced: ((LayoutCoordinates) -> Unit)? = null,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
    onEditMeta: () -> Unit,
) {
    val isLight = palette.isLight
    val progressPct = (book.progress * 100).roundToInt()
    val targetSelectScale = if (isSelectionMode && isSelected) 0.975f else 1f
    val selectionScaleAnim = remember { Animatable(targetSelectScale) }
    LaunchedEffect(targetSelectScale) {
        if (kotlin.math.abs(selectionScaleAnim.value - targetSelectScale) > 0.001f) {
            selectionScaleAnim.animateTo(
                targetValue = targetSelectScale,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
            )
        }
    }
    val rowInteraction = remember { MutableInteractionSource() }
    val coverCoordsHolder = remember(book.id) { arrayOfNulls<LayoutCoordinates>(1) }
    val accentOnGlass = if (isLight) {
        lerpColor(palette.accentText, Color.Black, 0.20f)
    } else {
        lerpColor(palette.accentText, Color.White, 0.10f)
    }
    val cardSurface = frostedSurfaceColor(palette)
    val cardHairline = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f)
    val rowBorder = if (isSelectionMode && isSelected) {
        accentOnGlass.copy(alpha = 0.55f)
    } else {
        cardHairline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectionScaleAnim.value < 0.999f) {
                    Modifier.graphicsLayer {
                        val s = selectionScaleAnim.value
                        scaleX = s
                        scaleY = s
                    }
                } else {
                    Modifier
                },
            )
            .clip(SHELF_CARD_SHAPE)
            .background(cardSurface)
            .border(0.5.dp, rowBorder, SHELF_CARD_SHAPE)
            .combinedClickable(
                interactionSource = rowInteraction,
                indication = null,
                onClick = { onClick(coverCoordsHolder[0]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()) },
                onLongClick = onLongClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val listCoverShape = SHELF_LIST_COVER_SHAPE
        val heroSlotAlpha = LocalHeroBookSlotAlpha.current
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 76.dp)
                .graphicsLayer { alpha = heroSlotAlpha(book.id) }
                .border(0.5.dp, cardHairline, listCoverShape)
                .onPlaced { coords ->
                    coverCoordsHolder[0] = coords
                    onCoverPlaced?.invoke(coords)
                },
        ) {
            BookCover(
                book = book,
                palette = palette,
                width = 54.dp,
                height = 76.dp,
                cornerRadius = 8.dp,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val cleanTitle = remember(book.title) { cleanBookTitle(book.title) }
            Text(
                text = cleanTitle,
                color = palette.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${book.author.ifBlank { "未知作者" }} · ${book.formatLabel} · ${book.chapters.size} 章",
                color = palette.text.copy(alpha = if (isLight) 0.62f else 0.68f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val trackColor = palette.text.copy(alpha = if (isLight) 0.12f else 0.16f)
                val progressFraction = book.progress.toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .drawBehind {
                            val r = size.height * 0.5f
                            val radius = CornerRadius(r, r)
                            drawRoundRect(color = trackColor, cornerRadius = radius)
                            if (progressFraction > 0.005f) {
                                drawRoundRect(
                                    color = accentOnGlass,
                                    size = Size(size.width * progressFraction, size.height),
                                    cornerRadius = radius,
                                )
                            }
                        },
                )
                Text(
                    text = "$progressPct%",
                    color = palette.text.copy(alpha = if (isLight) 0.68f else 0.76f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "已选中" else "未选中",
                    tint = if (isSelected) accentOnGlass else palette.muted,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                IconButton(
                    onClick = onEditMeta,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "编辑书籍",
                        tint = palette.text.copy(alpha = 0.64f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Suppress("unused")
private fun extractSeriesVolumeBadge(book: BookEntry, fallbackIndex: Int): String {
    book.seriesOrder?.takeIf { it > 0 }?.let { return "#$it" }
    val clean = cleanBookTitle(book.title)
    val normalized = buildString(clean.length) {
        for (ch in clean) {
            when (ch) {
                in '０'..'９' -> append(('0' + (ch - '０')))
                else -> append(ch)
            }
        }
    }
        .replace(Regex("""[（(\[【][^）)\]】]*[）)\]】]\s*$"""), "")
        .trim()
    val explicitMatch = Regex("""第\s*(\d{1,4})\s*([卷册集话回章节])""").find(normalized)
    if (explicitMatch != null) {
        return "第${explicitMatch.groupValues[1]}${explicitMatch.groupValues[2]}"
    }
    val tailMatch = Regex("""(\d{1,4})\s*([卷册集话回])?$""").find(normalized)
    if (tailMatch != null) {
        val unit = tailMatch.groupValues[2].ifEmpty { "卷" }
        return "第${tailMatch.groupValues[1]}$unit"
    }
    val chineseMatch = Regex(
        """(?:第\s*([一二三四五六七八九十百千万零〇两兩壹贰貳叁參肆伍陆陸柒捌玖拾佰仟萬廿]+)\s*[卷册集话回章节部]|卷\s*([一二三四五六七八九十百千万零〇两兩壹贰貳叁參肆伍陆陸柒捌玖拾佰仟萬廿]+)|([一二三四五六七八九十百千万零〇两兩壹贰貳叁參肆伍陆陸柒捌玖拾佰仟萬廿]+)\s*[卷册集]$)""",
    ).find(normalized)
    if (chineseMatch != null) {
        val rawNum = chineseMatch.groupValues[1]
            .ifEmpty { chineseMatch.groupValues[2] }
            .ifEmpty { chineseMatch.groupValues[3] }
        parseChineseInt(rawNum)?.let { return "#$it" }
    }
    val volMatch = Regex("""\bvol(?:ume)?\.?\s*(\d{1,4})\b""", RegexOption.IGNORE_CASE).find(normalized)
    if (volMatch != null) {
        val num = volMatch.groupValues[1].toIntOrNull()
        return if (num != null && num > 0) "#$num" else "#${volMatch.groupValues[1]}"
    }
    val lower = normalized.lowercase()
    return when {
        "番外" in normalized || "extra" in lower -> "番外"
        "外传" in normalized || "外傳" in normalized -> "外传"
        "特典" in normalized -> "特典"
        "短篇" in normalized -> "短篇"
        "后日谈" in normalized || "後日談" in normalized -> "后日谈"
        normalized.endsWith("序") || "序章" in normalized -> "序章"
        "终章" in normalized || "終章" in normalized -> "终章"
        else -> "#${fallbackIndex + 1}"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeriesVolumeGridTile(
    book: BookEntry,
    palette: AppPalette,
    accentOnGlass: Color,
    coverAlpha: Float = 1f,
    onCoverPlaced: ((LayoutCoordinates) -> Unit)? = null,
    onOpen: (Rect?) -> Unit,
    onOpenActions: () -> Unit,
) {
    val isLight = palette.isLight
    val progressPct = (book.progress * 100).roundToInt()
    val coverCoordsHolder = remember(book.id) { arrayOfNulls<LayoutCoordinates>(1) }
    val heroSlotAlpha = LocalHeroBookSlotAlpha.current
    // Matches Hero layer [2]'s landing corner (~106.dp * 0.112f clamped to 7..13dp ≈ 12.dp) so the exit lands seamlessly.
    val coverShape = SERIES_TILE_COVER_SHAPE
    val cleanTitle = remember(book.title) { cleanBookTitle(book.title) }
    val hairline = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f)
    val tileInteraction = remember { MutableInteractionSource() }
    val actionInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = tileInteraction,
                indication = null,
                onClick = { onOpen(coverCoordsHolder[0]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()) },
                onLongClick = onOpenActions,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.71f)
                .graphicsLayer { alpha = heroSlotAlpha(book.id) }
                // Hairline fades in lockstep with the cover (coverAlpha) so no unblended dark outline pops in/out
                // when the Hero cover finishes landing on this tile.
                .border(0.5.dp, hairline.copy(alpha = hairline.alpha * coverAlpha.coerceIn(0f, 1f)), coverShape)
                .clip(coverShape)
                .onPlaced { coords ->
                    coverCoordsHolder[0] = coords
                    onCoverPlaced?.invoke(coords)
                },
        ) {
            BookCover(
                book = book,
                palette = palette,
                width = 104.dp,
                height = 146.dp,
                cornerRadius = 0.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (coverAlpha < 0.999f) {
                            Modifier.graphicsLayer { alpha = coverAlpha }
                        } else {
                            Modifier
                        },
                    ),
            )
            val badgeDensity = LocalDensity.current
            val badgeBitmap = remember(badgeDensity) {
                val px = with(badgeDensity) { 24.dp.roundToPx() }
                val stroke = with(badgeDensity) { 0.5.dp.toPx() }
                SeriesVolumeMoreBadgeCache.get(px, stroke)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .drawBehind {
                        val a = coverAlpha.coerceIn(0f, 1f)
                        if (a > 0.002f) {
                            drawImage(
                                image = badgeBitmap,
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    size.width.roundToInt().coerceAtLeast(1),
                                    size.height.roundToInt().coerceAtLeast(1),
                                ),
                                alpha = a,
                            )
                        }
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "分卷操作"
                    }
                    .clickable(
                        interactionSource = actionInteraction,
                        indication = null,
                        onClick = onOpenActions,
                    ),
            )
            if (book.progress > 0.0) {
                val progressFraction = book.progress.toFloat().coerceIn(0.06f, 1f)
                val primaryColor = palette.primary
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .drawBehind {
                            val a = coverAlpha.coerceIn(0f, 1f)
                            if (a > 0.002f) {
                                drawRect(color = Color.Black.copy(alpha = 0.34f * a))
                                drawRect(
                                    color = primaryColor.copy(alpha = a),
                                    size = Size(size.width * progressFraction, size.height),
                                )
                            }
                        },
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = cleanTitle,
            color = palette.text,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = when {
                progressPct >= 100 -> "已读完"
                progressPct > 0 -> "已读 $progressPct%"
                else -> "未读"
            },
            color = if (progressPct in 1..99) {
                accentOnGlass
            } else {
                palette.text.copy(alpha = if (isLight) 0.56f else 0.64f)
            },
            fontSize = 10.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun SeriesVolumeRow(
    book: BookEntry,
    palette: AppPalette,
    accentOnGlass: Color,
    coverAlpha: Float = 1f,
    onCoverPlaced: ((LayoutCoordinates) -> Unit)? = null,
    onOpen: (Rect?) -> Unit,
    onEditMeta: () -> Unit,
    onRemoveFromSeries: () -> Unit,
) {
    val isLight = palette.isLight
    val progressPct = (book.progress * 100).roundToInt()
    val coverCoordsHolder = remember(book.id) { arrayOfNulls<LayoutCoordinates>(1) }
    val heroSlotAlpha = LocalHeroBookSlotAlpha.current
    val rowInteraction = remember { MutableInteractionSource() }
    val rowShape = SERIES_VOLUME_ROW_SHAPE
    val cleanTitle = remember(book.title) { cleanBookTitle(book.title) }
    val rowSurface = if (isLight) {
        Color.White.copy(alpha = 0.56f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val rowBorder = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowSurface, rowShape)
            .border(width = 0.5.dp, color = rowBorder, shape = rowShape)
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                onClick = { onOpen(coverCoordsHolder[0]?.takeIf { it.isAttached }?.unclippedBoundsInRoot()) },
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 50.dp)
                .graphicsLayer { alpha = heroSlotAlpha(book.id) }
                .onPlaced { coords ->
                    coverCoordsHolder[0] = coords
                    onCoverPlaced?.invoke(coords)
                },
        ) {
            BookCover(
                book = book,
                palette = palette,
                width = 36.dp,
                height = 50.dp,
                cornerRadius = 7.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (coverAlpha < 0.999f) {
                            Modifier.graphicsLayer { alpha = coverAlpha }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = cleanTitle,
                color = palette.text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (progressPct > 0) "已读 $progressPct%" else "未读",
                    color = if (progressPct in 1..99) {
                        accentOnGlass
                    } else {
                        palette.text.copy(alpha = if (isLight) 0.58f else 0.66f)
                    },
                    fontSize = 11.sp,
                    fontWeight = if (progressPct in 1..99) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = "· ${book.safeCurrentChapter.title}",
                    color = palette.text.copy(alpha = if (isLight) 0.54f else 0.62f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            onClick = onEditMeta,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "编辑信息",
                tint = palette.text.copy(alpha = 0.68f),
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(
            onClick = onRemoveFromSeries,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.RemoveCircleOutline,
                contentDescription = "移出系列",
                tint = palette.text.copy(alpha = 0.68f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun MultiSelectActionButton(
    icon: ImageVector,
    label: String,
    palette: AppPalette,
    tint: Color = palette.text,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "multiSelectBtnScale",
    )
    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (pressed) palette.text.copy(alpha = if (palette.isLight) 0.08f else 0.14f) else Color.Transparent,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ShelfMoveOptionCard(
    label: String,
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    onClick: () -> Unit,
) {
    val readingStyle = LocalReadingStyle.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .ios27GlassSurface(
                shape = RoundedCornerShape(16.dp),
                palette = palette,
                style = readingStyle,
                variant = Ios27GlassVariant.Card,
                backdrop = backdrop,
                isInteractive = false,
                showContrastBorder = false,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = palette.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ShelfGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    palette: AppPalette,
    placeholder: String? = null,
) {
    val isLight = palette.isLight
    val accent = if (isLight) lerpColor(palette.accentText, Color.Black, 0.16f) else palette.accentText
    var isFocused by remember { mutableStateOf(false) }
    val trayBg = if (isLight) Color.Black.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.085f)
    val trayRim = if (isFocused) {
        accent.copy(alpha = if (isLight) 0.55f else 0.65f)
    } else {
        if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.10f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = if (isFocused) accent else palette.text.copy(alpha = if (isLight) 0.64f else 0.70f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trayBg)
                .border(
                    width = if (isFocused) 1.dp else 0.5.dp,
                    color = trayRim,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        color = palette.muted.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = palette.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                )
            }
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                val clearInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = clearInteraction,
                            indication = null,
                        ) { onValueChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "清空",
                        tint = palette.text.copy(alpha = 0.50f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfDialogPillButton(
    label: String,
    palette: AppPalette,
    isPrimary: Boolean,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 520f),
        label = "shelfDialogPillPress",
    )

    val variant = if (isPrimary) Ios27GlassVariant.AccentPill else Ios27GlassVariant.SubtlePill
    val dangerTint = Color(0xFFEF4444)
    val tintOverride = if (isPrimary && isDanger) {
        dangerTint.copy(alpha = if (isLight) 0.18f else 0.26f)
    } else {
        Color.Unspecified
    }
    val textColor = when {
        isPrimary && isDanger -> if (isLight) lerpColor(dangerTint, Color.Black, 0.15f) else Color(0xFFFF6B6B)
        isPrimary -> if (isLight) lerpColor(palette.accentText, Color.Black, 0.18f) else palette.accentText
        else -> palette.text.copy(alpha = if (isLight) 0.72f else 0.78f)
    }

    // Never inherit LocalVitroBackdrop inside dialog/card buttons (prevents RenderThread nested glass SIGSEGV).
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .ios27GlassSurface(
                shape = CircleShape,
                palette = palette,
                style = readingStyle,
                variant = variant,
                backdrop = null,
                tintOverride = tintOverride,
                isInteractive = false,
                showContrastBorder = false,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.5.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyShelfPlaceholder(
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    isAllShelf: Boolean,
    hasSearchQuery: Boolean,
    onClearSearch: () -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    val accentOnGlass = if (isLight) lerpColor(palette.accentText, Color.Black, 0.18f) else palette.accentText

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .ios27GlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    palette = palette,
                    style = readingStyle,
                    variant = Ios27GlassVariant.Card,
                    backdrop = backdrop,
                    isInteractive = false,
                    showContrastBorder = false,
                )
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .ios27GlassSurface(
                        shape = CircleShape,
                        palette = palette,
                        style = readingStyle,
                        variant = Ios27GlassVariant.AccentPill,
                        backdrop = null,
                        isInteractive = false,
                        showContrastBorder = false,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasSearchQuery) Icons.Rounded.Search else Icons.Rounded.LibraryAdd,
                    contentDescription = null,
                    tint = accentOnGlass,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = when {
                    hasSearchQuery -> "未找到匹配的书籍"
                    isAllShelf -> "导入你的第一本书"
                    else -> "当前书架分类暂无书籍"
                },
                color = palette.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when {
                    hasSearchQuery -> "试试切换其他书架标签，或调整书名与作者关键词。"
                    isAllShelf -> "支持 EPUB 与 TXT 本地直读，自动识别系列分卷并保留插画与原书排版。"
                    else -> "长按任意书籍即可将它移动到当前自定义书架分类。"
                },
                color = palette.text.copy(alpha = if (isLight) 0.64f else 0.70f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (hasSearchQuery) {
                ShelfDialogPillButton(
                    label = "清除搜索条件",
                    palette = palette,
                    isPrimary = true,
                    onClick = onClearSearch,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val primaryInteraction = remember { MutableInteractionSource() }
                    val primaryPressed by primaryInteraction.collectIsPressedAsState()
                    val primaryScale by animateFloatAsState(
                        targetValue = if (primaryPressed) 0.97f else 1f,
                        animationSpec = spring(dampingRatio = 0.80f, stiffness = 520f),
                        label = "emptyImportFilesPress",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .graphicsLayer {
                                scaleX = primaryScale
                                scaleY = primaryScale
                            }
                            .ios27GlassSurface(
                                shape = CircleShape,
                                palette = palette,
                                style = readingStyle,
                                variant = Ios27GlassVariant.AccentPill,
                                backdrop = null,
                                isInteractive = false,
                                showContrastBorder = false,
                            )
                            .clickable(
                                interactionSource = primaryInteraction,
                                indication = null,
                                onClick = onImportFiles,
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = accentOnGlass, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("导入文件", color = accentOnGlass, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    val secondaryInteraction = remember { MutableInteractionSource() }
                    val secondaryPressed by secondaryInteraction.collectIsPressedAsState()
                    val secondaryScale by animateFloatAsState(
                        targetValue = if (secondaryPressed) 0.97f else 1f,
                        animationSpec = spring(dampingRatio = 0.80f, stiffness = 520f),
                        label = "emptyImportFolderPress",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .graphicsLayer {
                                scaleX = secondaryScale
                                scaleY = secondaryScale
                            }
                            .ios27GlassSurface(
                                shape = CircleShape,
                                palette = palette,
                                style = readingStyle,
                                variant = Ios27GlassVariant.SubtlePill,
                                backdrop = null,
                                isInteractive = false,
                                showContrastBorder = false,
                            )
                            .clickable(
                                interactionSource = secondaryInteraction,
                                indication = null,
                                onClick = onImportFolder,
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = palette.text, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("扫描文件夹", color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfTabPillText(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 200),
        label = "shelfTabTextColor",
    )
    Text(
        text = label,
        color = animatedColor,
        fontSize = 13.5.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
    )
}

private class SeriesModalPanelGeometry {
    var overlayW by mutableFloatStateOf(1f)
    var overlayH by mutableFloatStateOf(1f)
    var startCx by mutableFloatStateOf(0.5f)
    var startCy by mutableFloatStateOf(0.5f)
    var startW by mutableFloatStateOf(1f)
    var startH by mutableFloatStateOf(1f)
    var endCx by mutableFloatStateOf(0.5f)
    var endCy by mutableFloatStateOf(0.5f)
    var endW by mutableFloatStateOf(1f)
    var endH by mutableFloatStateOf(1f)
}

private class SeriesModalMorphCornerShape(
    private val radius: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val clamped = radius.coerceAtMost(minOf(size.width, size.height) * 0.5f)
        return Outline.Rounded(
            RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                cornerRadius = CornerRadius(clamped, clamped),
            ),
        )
    }

    override fun equals(other: Any?): Boolean =
        other is SeriesModalMorphCornerShape && other.radius == radius

    override fun hashCode(): Int = radius.hashCode()
}

private fun lerpSeriesFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
