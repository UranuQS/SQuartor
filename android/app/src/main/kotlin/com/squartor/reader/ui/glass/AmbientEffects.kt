package com.squartor.reader.ui.glass

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient as AndroidLinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient as AndroidRadialGradient
import android.graphics.Rect as AndroidRect
import android.graphics.Shader as AndroidShader
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassBackdrop
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.cleanBookTitle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive as coroutineIsActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 内存级封面位图、柔焦位图与真实主色调缓存中枢：
 * - 在 IO 线程解码缩略图，提取高饱和主色调；
 * - 零逐帧 CPU 像素模糊，通过 GPU 原生 Canvas 与多级双线性硬件降采样实现 120Hz/144Hz 零掉帧封面瀑布流渲染。
 */
object CoverAssetCache {
    private val thumbCache = LruCache<String, ImageBitmap>(280)
    private val rawCoverBitmapCache = LruCache<String, Bitmap>(60)
    private val waterfallSharpTileCache = LruCache<String, Bitmap>(60)
    private val blurredCache = LruCache<String, ImageBitmap>(60)
    private val rawSharpBitmapCache = LruCache<String, Bitmap>(40)
    private val rawHazeBitmapCache = LruCache<String, Bitmap>(40)
    private val screenBackdropCache = LruCache<String, Pair<Bitmap, Bitmap>>(16)
    private val colorCache = ConcurrentHashMap<String, Color>()
    private val secondaryColorCache = ConcurrentHashMap<String, Color>()
    private val triColorCache = ConcurrentHashMap<String, Triple<Color, Color, Color>>()
    private val reactiveColorCache = mutableStateMapOf<String, Color>()
    private val reactiveSecondaryColorCache = mutableStateMapOf<String, Color>()
    private val reactiveTriColorCache = mutableStateMapOf<String, Triple<Color, Color, Color>>()
    private val cachedCoverPathByBookId = ConcurrentHashMap<String, String>()
    private val inFlightLoads = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val DEFAULT_AMBIENT_COLOR = Color(0xFFD4A359)

    // 兼容旧式按需帧读取的轻量单缓冲（已移除 8 槽 336px 环形像素拷贝与 CPU boxBlurInPlace）
    private val compactFrameLock = Any()
    private var compactDynamicBitmap: Bitmap? = null
    private var compactDynamicCanvas: AndroidCanvas? = null
    private var lastCompactIsLight: Boolean = false
    private var lastCompactBgArgb: Int = 0
    private var lastCompactW: Int = -1
    private var lastCompactH: Int = -1
    private var lastCompactBookIdsHash: Int = 0
    private var lastCompactVersionTick: Int = -1
    private var lastCompactFlowTime: Float = Float.NaN
    private var lastCompactDynamicFrame: LiquidGlassDynamicFrame? = null
    private var fallbackSharpTiles: List<Bitmap>? = null
    private var fallbackHazeTiles: List<Bitmap>? = null

    // 硬件主屏画布与离屏折射缓冲各自独立拥有专用 Paint 与渐变缓存，彻底杜绝跨尺寸覆写导致的每帧重建与状态竞争
    private val hwBitmapPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
    private val swBitmapPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
    private val hwClearPaint = AndroidPaint()
    private val swClearPaint = AndroidPaint()
    private val hwScrimPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    private val swScrimPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    private val hwDstRectF = android.graphics.RectF()
    private val swDstRectF = android.graphics.RectF()
    private val waterfallScrimStops = floatArrayOf(0.0f, 0.16f, 0.48f, 0.72f, 0.90f, 1.0f)
    private var cachedHwScrimIsLight: Boolean = false
    private var cachedHwScrimBottomPx: Int = -1
    private var cachedHwScrimBgArgb: Int = 0
    private var cachedHwScrimShader: AndroidLinearGradient? = null
    private var cachedSwScrimIsLight: Boolean = false
    private var cachedSwScrimBottomPx: Int = -1
    private var cachedSwScrimBgArgb: Int = 0
    private var cachedSwScrimShader: AndroidLinearGradient? = null

    var versionTick by mutableIntStateOf(0)
        private set

    fun getThumb(bookId: String): ImageBitmap? = thumbCache.get(bookId)

    fun putThumbIfAbsent(bookId: String, coverPath: String?, imageBitmap: ImageBitmap) {
        if (!coverPath.isNullOrBlank()) {
            cachedCoverPathByBookId.putIfAbsent(bookId, coverPath)
        }
        if (thumbCache.get(bookId) == null) {
            thumbCache.put(bookId, imageBitmap)
        }
    }

    fun getBlurred(bookId: String): ImageBitmap? = blurredCache.get(bookId) ?: run {
        val raw = rawCoverBitmapCache.get(bookId) ?: return null
        val lightBlurredBmp = createLightBlurredBitmap(raw, 180, 270)
        rawSharpBitmapCache.put(bookId, lightBlurredBmp)
        lightBlurredBmp.asImageBitmap().also { blurredCache.put(bookId, it) }
    }

    fun getRawCoverBitmap(bookId: String): Bitmap? = rawCoverBitmapCache.get(bookId)

    fun getWaterfallSharpTile(bookId: String): Bitmap? = waterfallSharpTileCache.get(bookId)

    fun getWaterfallHazeTile(bookId: String): Bitmap? = waterfallSharpTileCache.get(bookId)

    fun getRawSharpBackdrop(bookId: String): Bitmap? = rawSharpBitmapCache.get(bookId) ?: run {
        val raw = rawCoverBitmapCache.get(bookId) ?: return null
        createLightBlurredBitmap(raw, 180, 270).also {
            rawSharpBitmapCache.put(bookId, it)
            blurredCache.put(bookId, it.asImageBitmap())
        }
    }

    fun getRawHazeBackdrop(bookId: String): Bitmap? = rawHazeBitmapCache.get(bookId) ?: run {
        val raw = rawCoverBitmapCache.get(bookId) ?: return null
        createSmoothBlurredBitmap(raw, 96, 144).also { rawHazeBitmapCache.put(bookId, it) }
    }

    private fun ensureFallbackTiles(): Pair<List<Bitmap>, List<Bitmap>> {
        val existingSharp = fallbackSharpTiles
        val existingHaze = fallbackHazeTiles
        if (existingSharp != null && existingHaze != null) {
            return existingSharp to existingHaze
        }
        val palettes = listOf(
            intArrayOf(0xFF2563EB.toInt(), 0xFF38BDF8.toInt(), 0xFF1E1B4B.toInt()),
            intArrayOf(0xFFE11D48.toInt(), 0xFFFB7185.toInt(), 0xFF4C0519.toInt()),
            intArrayOf(0xFF7C3AED.toInt(), 0xFFC084FC.toInt(), 0xFF2E1065.toInt()),
            intArrayOf(0xFFD97706.toInt(), 0xFFFBBF24.toInt(), 0xFF451A03.toInt()),
            intArrayOf(0xFF059669.toInt(), 0xFF34D399.toInt(), 0xFF064E3B.toInt()),
            intArrayOf(0xFFDB2777.toInt(), 0xFFF472B6.toInt(), 0xFF500724.toInt()),
            intArrayOf(0xFF4F46E5.toInt(), 0xFF818CF8.toInt(), 0xFF1E1B4B.toInt()),
            intArrayOf(0xFF0891B2.toInt(), 0xFF22D3EE.toInt(), 0xFF083344.toInt()),
        )
        val sharpList = ArrayList<Bitmap>(palettes.size)
        val hazeList = ArrayList<Bitmap>(palettes.size)
        for (cols in palettes) {
            val tw = 112
            val th = 160
            val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            val c = AndroidCanvas(bmp)
            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
            val rectF = android.graphics.RectF(0f, 0f, tw.toFloat(), th.toFloat())
            p.shader = AndroidLinearGradient(
                0f, 0f, tw.toFloat(), th.toFloat(),
                cols, floatArrayOf(0f, 0.55f, 1f), AndroidShader.TileMode.CLAMP,
            )
            c.drawRoundRect(rectF, 12f, 12f, p)
            p.shader = null
            p.style = AndroidPaint.Style.STROKE
            p.strokeWidth = 2.2f
            p.color = 0x55FFFFFF
            c.drawRoundRect(android.graphics.RectF(1.5f, 1.5f, tw - 1.5f, th - 1.5f), 11f, 11f, p)
            bmp.setHasMipMap(true)
            sharpList.add(bmp)
            hazeList.add(bmp)
        }
        fallbackSharpTiles = sharpList
        fallbackHazeTiles = hazeList
        return sharpList to hazeList
    }

    /**
     * 在任意原生 AndroidCanvas 上绘制「左上到右下 -20° 斜向 4 列交错无限流动的半屏高清书封瀑布」：
     * - 严格覆盖屏幕上半部分（0% ~ 56% 高度），在浅色/深色主题下分别调优透明与明暗过渡曲线，
     *   让 GPU 图层清晰顺滑渲染并为上方 Vitro GlassBackdrop 供给饱满的真实折射色彩；
     * - 每块封面砖块按真实屏幕 Y 坐标做五次平滑淡出，杜绝底部封面切入切出时的亮度跳变。
     */
    fun drawDiagonalCoverWaterfallToCanvas(
        canvas: AndroidCanvas,
        w: Float,
        h: Float,
        bookIds: List<String>,
        flowTimeSeconds: Float,
        isLight: Boolean,
        bgColor: Color,
        useCompactTiles: Boolean = false,
        skipFullClear: Boolean = false,
    ) {
        val bgArgb = bgColor.toArgb()
        val waterfallBottomY = h * 0.56f
        val clearPaint = if (useCompactTiles) swClearPaint else hwClearPaint
        clearPaint.color = bgArgb
        canvas.drawRect(0f, 0f, w, if (skipFullClear) waterfallBottomY + 2f else h, clearPaint)
        if (w <= 2f || h <= 2f) return

        val (fbSharp, _) = ensureFallbackTiles()
        if (fbSharp.isEmpty() && bookIds.isEmpty()) return

        val bitmapPaint = if (useCompactTiles) swBitmapPaint else hwBitmapPaint
        val dstRectF = if (useCompactTiles) swDstRectF else hwDstRectF

        val saveCount = canvas.save()
        canvas.clipRect(0f, 0f, w, waterfallBottomY)

        // 围绕上半屏视觉中心旋转 -20°（左上到右下斜向排列）
        val pivotX = w * 0.50f
        val pivotY = h * 0.24f
        canvas.rotate(-20f, pivotX, pivotY)

        val cols = 4
        val gap = w * 0.032f
        val tileW = (w * 1.24f - gap * (cols - 1)) / cols
        val tileH = tileW * 1.42f
        val startX = pivotX - (cols * tileW + (cols - 1) * gap) * 0.5f
        val pitchY = (tileH + gap).coerceAtLeast(10f)
        val baseSpeedPxPerSec = pitchY * 0.14f
        val halfVisibleSpan = h * 0.64f

        val slotCount = bookIds.size
        val fadeStartScreenY = h * 0.30f
        val fadeEndScreenY = h * 0.53f
        val fadeSpan = (fadeEndScreenY - fadeStartScreenY).coerceAtLeast(1f)
        // 为浅色与深色主题调优封面透明度峰值与微边框反差（圆角微边框已预烘焙进离线 Tile，杜绝每帧斜向描边）
        val maxTileAlpha = if (isLight) 0.88f else 0.92f

        for (col in 0 until cols) {
            val colX = startX + col * (tileW + gap)
            val tileCenterOffsetX = (colX + tileW * 0.5f) - pivotX
            val dir = if (col % 2 == 0) 1f else -1f
            val speedMult = if (col % 2 == 0) 1.0f else 0.85f
            val colOffsetPx = col * pitchY * 0.37f
            val scrollPx = flowTimeSeconds * baseSpeedPxPerSec * speedMult * dir + colOffsetPx

            val minRow = kotlin.math.floor((-halfVisibleSpan - scrollPx) / pitchY).toInt() - 1
            val maxRow = kotlin.math.ceil((halfVisibleSpan - scrollPx) / pitchY).toInt() + 1

            for (row in minRow..maxRow) {
                val y = pivotY + row * pitchY + scrollPx
                if (y + tileH < pivotY - halfVisibleSpan || y > pivotY + halfVisibleSpan) continue

                val tileCenterOffsetY = (y + tileH * 0.5f) - pivotY
                val screenCenterY = pivotY + tileCenterOffsetX * (-0.34202015f) + tileCenterOffsetY * 0.9396926f
                if (screenCenterY >= fadeEndScreenY) continue

                val t = ((screenCenterY - fadeStartScreenY) / fadeSpan).coerceIn(0f, 1f)
                val smoothFade = 1f - t * t * (3f - 2f * t)
                val tileAlpha = (smoothFade * maxTileAlpha * 255f).roundToInt().coerceIn(0, 255)
                if (tileAlpha <= 2) continue

                val bmp: Bitmap = if (slotCount > 0) {
                    val slotIdx = Math.floorMod(col * 5 + row * 3, slotCount)
                    val id = bookIds[slotIdx]
                    val loaded = waterfallSharpTileCache.get(id)
                    if (loaded != null && !loaded.isRecycled) {
                        loaded
                    } else {
                        fbSharp[Math.floorMod(col * 3 + row, fbSharp.size)]
                    }
                } else {
                    fbSharp[Math.floorMod(col * 3 + row, fbSharp.size)]
                }

                bitmapPaint.alpha = tileAlpha
                dstRectF.set(colX, y, colX + tileW, y + tileH)
                canvas.drawBitmap(bmp, null, dstRectF, bitmapPaint)
            }
        }
        canvas.restoreToCount(saveCount)

        // 浅色/深色主题调优纵向对比度曲线（顶部轻量保可读性柔雾，底部 S 型丝滑溶入背景色，彻底消除生硬色带）
        val bottomPx = waterfallBottomY.roundToInt()
        val scrimPaint = if (useCompactTiles) swScrimPaint else hwScrimPaint
        var gradient = if (useCompactTiles) cachedSwScrimShader else cachedHwScrimShader
        val cacheHit = if (useCompactTiles) {
            gradient != null &&
                cachedSwScrimIsLight == isLight &&
                cachedSwScrimBottomPx == bottomPx &&
                cachedSwScrimBgArgb == bgArgb
        } else {
            gradient != null &&
                cachedHwScrimIsLight == isLight &&
                cachedHwScrimBottomPx == bottomPx &&
                cachedHwScrimBgArgb == bgArgb
        }
        if (!cacheHit) {
            val colors = if (isLight) {
                intArrayOf(
                    bgColor.copy(alpha = 0.22f).toArgb(),
                    0x00000000,
                    bgColor.copy(alpha = 0.14f).toArgb(),
                    bgColor.copy(alpha = 0.56f).toArgb(),
                    bgColor.copy(alpha = 0.90f).toArgb(),
                    bgArgb,
                )
            } else {
                intArrayOf(
                    bgColor.copy(alpha = 0.28f).toArgb(),
                    0x00000000,
                    bgColor.copy(alpha = 0.20f).toArgb(),
                    bgColor.copy(alpha = 0.64f).toArgb(),
                    bgColor.copy(alpha = 0.93f).toArgb(),
                    bgArgb,
                )
            }
            gradient = AndroidLinearGradient(
                0f,
                0f,
                0f,
                waterfallBottomY,
                colors,
                waterfallScrimStops,
                AndroidShader.TileMode.CLAMP,
            )
            if (useCompactTiles) {
                cachedSwScrimIsLight = isLight
                cachedSwScrimBottomPx = bottomPx
                cachedSwScrimBgArgb = bgArgb
                cachedSwScrimShader = gradient
            } else {
                cachedHwScrimIsLight = isLight
                cachedHwScrimBottomPx = bottomPx
                cachedHwScrimBgArgb = bgArgb
                cachedHwScrimShader = gradient
            }
        }
        scrimPaint.shader = gradient
        canvas.drawRect(0f, 0f, w, waterfallBottomY + 2f, scrimPaint)
    }

    /**
     * 轻量兼容接口：Vitro 接管实时玻璃折射后不再需要每帧 CPU boxBlur 与 8 槽位 336w 环形拷贝；
     * 仅按需复用单一 144w 紧凑位图，确保旧调用方平滑过渡且零逐帧 CPU 像素处理开销。
     */
    fun getWaterfallDynamicFrame(
        bookIds: List<String>,
        flowTimeSeconds: Float,
        isLight: Boolean,
        bgColor: Color,
        screenAspect: Float = 0.5f,
    ): LiquidGlassDynamicFrame {
        synchronized(compactFrameLock) {
            val safeAspect = screenAspect.coerceIn(0.38f, 0.78f)
            val compactW = 144
            val compactH = (compactW / safeAspect).roundToInt().coerceIn(240, 360)
            val bgArgb = bgColor.toArgb()
            val bookHash = bookIds.hashCode()
            val tick = versionTick
            val existing = lastCompactDynamicFrame
            if (existing != null &&
                lastCompactIsLight == isLight &&
                lastCompactBgArgb == bgArgb &&
                lastCompactW == compactW &&
                lastCompactH == compactH &&
                lastCompactBookIdsHash == bookHash &&
                lastCompactVersionTick == tick &&
                abs(flowTimeSeconds - lastCompactFlowTime) < 0.0002f
            ) {
                return existing
            }

            var bmp = compactDynamicBitmap
            var canvas = compactDynamicCanvas
            if (bmp == null || canvas == null || bmp.width != compactW || bmp.height != compactH) {
                bmp = Bitmap.createBitmap(compactW, compactH, Bitmap.Config.ARGB_8888).apply {
                    setHasMipMap(true)
                }
                canvas = AndroidCanvas(bmp)
                compactDynamicBitmap = bmp
                compactDynamicCanvas = canvas
            }

            drawDiagonalCoverWaterfallToCanvas(
                canvas = canvas,
                w = compactW.toFloat(),
                h = compactH.toFloat(),
                bookIds = bookIds,
                flowTimeSeconds = flowTimeSeconds,
                isLight = isLight,
                bgColor = bgColor,
                useCompactTiles = true,
                skipFullClear = false,
            )
            val shader = BitmapShader(bmp, AndroidShader.TileMode.CLAMP, AndroidShader.TileMode.CLAMP).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    filterMode = BitmapShader.FILTER_MODE_LINEAR
                }
            }
            val dynamicFrame = LiquidGlassDynamicFrame(bmp, shader)
            lastCompactIsLight = isLight
            lastCompactBgArgb = bgArgb
            lastCompactW = compactW
            lastCompactH = compactH
            lastCompactBookIdsHash = bookHash
            lastCompactVersionTick = tick
            lastCompactFlowTime = flowTimeSeconds
            lastCompactDynamicFrame = dynamicFrame
            return dynamicFrame
        }
    }

    fun getWaterfallBackdropPair(
        bookIds: List<String>,
        flowTimeSeconds: Float,
        isLight: Boolean,
        bgColor: Color,
        screenAspect: Float = 0.5f,
    ): Pair<Bitmap, Bitmap> {
        val frame = getWaterfallDynamicFrame(
            bookIds = bookIds,
            flowTimeSeconds = flowTimeSeconds,
            isLight = isLight,
            bgColor = bgColor,
            screenAspect = screenAspect,
        )
        val staticCopy = frame.bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: frame.bitmap
        return staticCopy to staticCopy
    }

    /**
     * 获取与屏幕坐标 (0, 0) ~ (screenWidth, screenHeight) 1:1 严格对齐的全屏合成背景双纹理（Sharp 折射纹理 + Haze 漫射纹理）：
     * - 100% 铺满全屏高度（彻底消除 68% 高度处的水平明暗截断线！）；
     * - 叠加与 ReadingNowFullPageAmbient 1:1 同步的全屏平滑黑白/深浅色遮罩，使顶部、中部与底部「最近阅读」及「FlClash Dock 栏」均处于连续统一的柔焦氛围光场中。
     */
    fun getScreenBackdropPair(
        bookId: String?,
        isLight: Boolean,
        bgColor: Color,
        ambientColor: Color,
        coverHeightRatio: Float = 1.0f,
        screenAspect: Float = 0.5f,
    ): Pair<Bitmap, Bitmap> {
        val coverBmp = bookId?.let { rawSharpBitmapCache.get(it) ?: rawCoverBitmapCache.get(it) }
        val hasCover = coverBmp != null && coverBmp.width > 0 && coverBmp.height > 0
        val aspectKey = (screenAspect * 20).roundToInt() // 量化宽高比以便后台预热命中
        val cacheKey = "full_screen_v5:${bookId ?: "none"}:$hasCover:$isLight:${bgColor.toArgb()}:${ambientColor.toArgb()}:$aspectKey"
        synchronized(screenBackdropCache) {
            screenBackdropCache.get(cacheKey)?.let { return it }
        }

        val w = 216
        val h = (w / screenAspect.coerceIn(0.38f, 0.78f)).roundToInt().coerceIn(360, 560)
        val sharpScreen = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(sharpScreen)
        val bgArgb = bgColor.toArgb()
        canvas.drawColor(bgArgb)

        // 全屏 CenterCrop 绘制柔焦封面（若为无封面 TXT 则仅在下半屏叠加轻柔氛围光场），杜绝上半屏杂光
        if (coverBmp != null && coverBmp.width > 0 && coverBmp.height > 0) {
            val srcW = coverBmp.width
            val srcH = coverBmp.height
            val targetAspect = w.toFloat() / h.toFloat()
            val srcAspect = srcW.toFloat() / srcH.toFloat()
            val srcRect = if (srcAspect > targetAspect) {
                val cropW = (srcH * targetAspect).roundToInt()
                val left = (srcW - cropW) / 2
                AndroidRect(left, 0, left + cropW, srcH)
            } else {
                val cropH = (srcW / targetAspect).roundToInt()
                val top = (srcH - cropH) / 2
                AndroidRect(0, top, srcW, top + cropH)
            }
            val dstRect = AndroidRect(0, 0, w, h)
            val coverPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
                alpha = if (isLight) 232 else 220
            }
            canvas.drawBitmap(coverBmp, srcRect, dstRect, coverPaint)
        } else {
            val saveCount = canvas.save()
            canvas.clipRect(0f, h * 0.50f, w.toFloat(), h.toFloat())
            val bottomRadius = min(w * 0.65f, h * 0.28f).coerceAtLeast(12f)
            val radialPaintBottom = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    w * 0.54f,
                    h * 0.80f,
                    bottomRadius,
                    intArrayOf(
                        ambientColor.copy(alpha = if (isLight) 0.22f else 0.28f).toArgb(),
                        ambientColor.copy(alpha = if (isLight) 0.09f else 0.12f).toArgb(),
                        android.graphics.Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.58f, 1f),
                    AndroidShader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, h * 0.50f, w.toFloat(), h.toFloat(), radialPaintBottom)
            canvas.restoreToCount(saveCount)
        }

        // 叠加与 ReadingNowFullPageAmbient 1:1 严格一致的全屏平滑对比度遮罩（从顶到底丝滑过渡，绝不出现断层）
        val gradPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            shader = AndroidLinearGradient(
                0f,
                0f,
                0f,
                h.toFloat(),
                intArrayOf(
                    bgColor.copy(alpha = if (isLight) 0.16f else 0.20f).toArgb(),
                    bgColor.copy(alpha = if (isLight) 0.22f else 0.26f).toArgb(),
                    bgColor.copy(alpha = if (isLight) 0.42f else 0.48f).toArgb(),
                    bgColor.copy(alpha = if (isLight) 0.60f else 0.66f).toArgb(),
                    bgColor.copy(alpha = if (isLight) 0.72f else 0.76f).toArgb(),
                ),
                floatArrayOf(0.0f, 0.38f, 0.68f, 0.88f, 1.0f),
                AndroidShader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradPaint)

        // 多级双线性金字塔降采样生成丝滑柔焦纹理（纯原生硬件滤波，0 CPU 像素循环）
        val hazeW = 72
        val hazeH = (hazeW / screenAspect.coerceIn(0.38f, 0.78f)).roundToInt().coerceIn(120, 190)
        val tiny = Bitmap.createScaledBitmap(sharpScreen, 18, 28, true)
        val hazeScreen = Bitmap.createScaledBitmap(tiny, hazeW, hazeH, true)
        if (tiny !== sharpScreen && tiny !== hazeScreen) tiny.recycle()

        val pair = sharpScreen to hazeScreen
        synchronized(screenBackdropCache) {
            screenBackdropCache.put(cacheKey, pair)
        }
        return pair
    }

    /**
     * 为书架、统计、设置、阅读器浮层等无单本大封面背景的页面生成 1:1 屏幕对齐的纯下半屏微氛围光双纹理：
     * - 严格保持屏幕上半部分（0% ~ 52% 高度）零氛围光叠加，仅在下半屏叠加轻柔漫射光场供底部区域与 Dock 折射。
     */
    fun getSubpageBackdropPair(
        isLight: Boolean,
        bgColor: Color,
        primaryColor: Color,
        secondaryColor: Color,
        accentColor: Color,
        screenAspect: Float = 0.5f,
    ): Pair<Bitmap, Bitmap> {
        val aspectKey = (screenAspect * 20).roundToInt()
        val cacheKey = "subpage_v3:$isLight:${bgColor.toArgb()}:${primaryColor.toArgb()}:${secondaryColor.toArgb()}:${accentColor.toArgb()}:$aspectKey"
        synchronized(screenBackdropCache) {
            screenBackdropCache.get(cacheKey)?.let { return it }
        }

        val w = 216
        val h = (w / screenAspect.coerceIn(0.38f, 0.78f)).roundToInt().coerceIn(360, 560)
        val sharpScreen = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(sharpScreen)
        canvas.drawColor(bgColor.toArgb())

        fun drawOrb(cx: Float, cy: Float, radius: Float, color: Color, peakAlpha: Float) {
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    cx,
                    cy,
                    radius.coerceAtLeast(10f),
                    intArrayOf(
                        color.copy(alpha = peakAlpha).toArgb(),
                        color.copy(alpha = peakAlpha * 0.45f).toArgb(),
                        android.graphics.Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    AndroidShader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, h * 0.52f, w.toFloat(), h.toFloat(), paint)
        }

        val baseAlpha = if (isLight) 0.16f else 0.22f
        val saveCount = canvas.save()
        canvas.clipRect(0f, h * 0.52f, w.toFloat(), h.toFloat())
        // 仅在屏幕下半部分叠加少量柔光色带（上半部分严格零环境光）
        drawOrb(w * 0.28f, h * 0.74f, min(w * 0.58f, h * 0.20f), accentColor, baseAlpha * 0.78f)
        drawOrb(w * 0.76f, h * 0.86f, min(w * 0.62f, h * 0.24f), primaryColor, baseAlpha * 0.88f)
        canvas.restoreToCount(saveCount)

        val hazeW = 72
        val hazeH = (hazeW / screenAspect.coerceIn(0.38f, 0.78f)).roundToInt().coerceIn(120, 190)
        val tiny = Bitmap.createScaledBitmap(sharpScreen, 18, 28, true)
        val hazeScreen = Bitmap.createScaledBitmap(tiny, hazeW, hazeH, true)
        if (tiny !== sharpScreen && tiny !== hazeScreen) tiny.recycle()

        val pair = sharpScreen to hazeScreen
        synchronized(screenBackdropCache) {
            screenBackdropCache.put(cacheKey, pair)
        }
        return pair
    }

    private fun colorCacheKey(book: BookEntry): String = "${book.id}|${book.coverPath.orEmpty()}"

    fun trimMemory(level: Int) {
        if (level < 15) return
        synchronized(screenBackdropCache) {
            screenBackdropCache.evictAll()
        }
        if (level < 20) {
            blurredCache.trimToSize(16)
            return
        }
        blurredCache.evictAll()
        rawSharpBitmapCache.evictAll()
        rawHazeBitmapCache.evictAll()
        if (level >= 40) {
            rawCoverBitmapCache.trimToSize(20)
            waterfallSharpTileCache.trimToSize(28)
        } else {
            rawCoverBitmapCache.trimToSize(40)
        }
    }

    private fun invalidateBookCachesIfPathChanged(book: BookEntry) {
        val path = book.coverPath ?: return
        val prevPath = cachedCoverPathByBookId.put(book.id, path)
        if (prevPath != null && prevPath != path) {
            thumbCache.remove(book.id)
            rawCoverBitmapCache.remove(book.id)
            waterfallSharpTileCache.remove(book.id)
            blurredCache.remove(book.id)
            rawSharpBitmapCache.remove(book.id)
            rawHazeBitmapCache.remove(book.id)
            val prevKey = "${book.id}|$prevPath"
            colorCache.remove(prevKey)
            secondaryColorCache.remove(prevKey)
            triColorCache.remove(prevKey)
        }
    }

    private suspend fun publishLoadedBookState(
        colorKey: String,
        primary: Color?,
        secondary: Color?,
        triad: Triple<Color, Color, Color>?,
    ) {
        try {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                var changed = false
                if (primary != null && reactiveColorCache[colorKey] != primary) {
                    reactiveColorCache[colorKey] = primary
                    changed = true
                }
                if (secondary != null && reactiveSecondaryColorCache[colorKey] != secondary) {
                    reactiveSecondaryColorCache[colorKey] = secondary
                    changed = true
                }
                if (triad != null && reactiveTriColorCache[colorKey] != triad) {
                    reactiveTriColorCache[colorKey] = triad
                    changed = true
                }
                if (changed) {
                    versionTick++
                }
            }
        } catch (_: Throwable) {
            var changed = false
            if (primary != null && reactiveColorCache[colorKey] != primary) {
                reactiveColorCache[colorKey] = primary
                changed = true
            }
            if (secondary != null && reactiveSecondaryColorCache[colorKey] != secondary) {
                reactiveSecondaryColorCache[colorKey] = secondary
                changed = true
            }
            if (triad != null && reactiveTriColorCache[colorKey] != triad) {
                reactiveTriColorCache[colorKey] = triad
                changed = true
            }
            if (changed) {
                versionTick++
            }
        }
    }

    fun getAmbientColor(book: BookEntry, index: Int, palette: AppPalette): Color {
        if (!book.coverPath.isNullOrBlank()) {
            val cKey = colorCacheKey(book)
            val cached = reactiveColorCache[cKey] ?: colorCache[cKey]
            if (cached != null) return cached
        }
        return if (index % 2 == 0) palette.primary else palette.primarySoft
    }

    fun getAmbientColor(book: BookEntry?, fallback: Color = DEFAULT_AMBIENT_COLOR): Color {
        if (book == null || book.coverPath.isNullOrBlank()) return fallback
        val cKey = colorCacheKey(book)
        return reactiveColorCache[cKey] ?: colorCache[cKey] ?: fallback
    }

    fun getExtractedCoverColor(book: BookEntry?): Color? {
        if (book == null || book.coverPath.isNullOrBlank()) return null
        val cKey = colorCacheKey(book)
        return reactiveColorCache[cKey] ?: colorCache[cKey]
    }

    fun getSecondaryAmbientColor(book: BookEntry, index: Int, palette: AppPalette): Color {
        if (!book.coverPath.isNullOrBlank()) {
            val cKey = colorCacheKey(book)
            val cached = reactiveSecondaryColorCache[cKey]
                ?: secondaryColorCache[cKey]
                ?: reactiveColorCache[cKey]
                ?: colorCache[cKey]
            if (cached != null) return cached
        }
        return palette.primarySoft
    }

    fun getSecondaryAmbientColor(book: BookEntry?, fallback: Color = DEFAULT_AMBIENT_COLOR): Color {
        if (book == null || book.coverPath.isNullOrBlank()) return fallback
        val cKey = colorCacheKey(book)
        return reactiveSecondaryColorCache[cKey]
            ?: secondaryColorCache[cKey]
            ?: reactiveColorCache[cKey]
            ?: colorCache[cKey]
            ?: fallback
    }

    fun getDominantTriColors(book: BookEntry?, palette: AppPalette): Triple<Color, Color, Color> {
        if (book != null && !book.coverPath.isNullOrBlank()) {
            val cKey = colorCacheKey(book)
            val cached = reactiveTriColorCache[cKey] ?: triColorCache[cKey]
            if (cached != null) return cached
        }
        return Triple(palette.primary, palette.primarySoft, palette.accentText)
    }

    private fun isBookCached(book: BookEntry, includeExtendedAssets: Boolean): Boolean {
        val path = book.coverPath ?: return true
        if (cachedCoverPathByBookId[book.id] != path) return false
        if (thumbCache.get(book.id) == null) return false
        if (!includeExtendedAssets) return true
        val cKey = colorCacheKey(book)
        return waterfallSharpTileCache.get(book.id) != null &&
            colorCache.containsKey(cKey) &&
            triColorCache.containsKey(cKey) &&
            reactiveColorCache.containsKey(cKey) &&
            reactiveTriColorCache.containsKey(cKey)
    }

    suspend fun ensureLoaded(book: BookEntry) {
        loadInternal(book, includeExtendedAssets = true)
    }

    suspend fun preloadBooks(
        books: List<BookEntry>,
        includeExtendedAssets: Boolean = true,
    ) {
        if (books.isEmpty()) return
        val uncached = books.filter { !isBookCached(it, includeExtendedAssets) }
        if (uncached.isEmpty()) return
        val gate = Semaphore(1)
        coroutineScope {
            uncached.map { book ->
                async(Dispatchers.IO) {
                    currentCoroutineContext().ensureActive()
                    gate.withPermit {
                        currentCoroutineContext().ensureActive()
                        loadInternal(book, includeExtendedAssets = includeExtendedAssets)
                        yield()
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun loadInternal(
        book: BookEntry,
        includeExtendedAssets: Boolean = true,
    ): Boolean {
        val path = book.coverPath ?: return false
        invalidateBookCachesIfPathChanged(book)
        val cKey = colorCacheKey(book)
        val hasThumb = cachedCoverPathByBookId[book.id] == path && thumbCache.get(book.id) != null
        if (!includeExtendedAssets && hasThumb) {
            return false
        }
        if (hasThumb &&
            waterfallSharpTileCache.get(book.id) != null &&
            colorCache.containsKey(cKey) &&
            triColorCache.containsKey(cKey)
        ) {
            if (!reactiveColorCache.containsKey(cKey) || !reactiveTriColorCache.containsKey(cKey)) {
                publishLoadedBookState(cKey, colorCache[cKey], secondaryColorCache[cKey], triColorCache[cKey])
            }
            return false
        }

        val inflightKey = if (includeExtendedAssets) "$cKey|ext" else cKey
        val newDeferred = CompletableDeferred<Boolean>()
        val existing = inFlightLoads.putIfAbsent(inflightKey, newDeferred)
        if (existing != null) {
            return existing.await()
        }

        val loaded = withContext(NonCancellable + Dispatchers.IO) {
            try {
                val bmp = rawCoverBitmapCache.get(book.id)?.takeIf {
                    cachedCoverPathByBookId[book.id] == path && !it.isRecycled
                } ?: run {
                    val file = File(path)
                    if (!file.exists()) return@withContext false
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                    if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext false

                    val maxW = if (includeExtendedAssets) 540 else 300
                    val maxH = if (includeExtendedAssets) 800 else 440
                    var sampleSize = 1
                    while (opts.outWidth / sampleSize > maxW || opts.outHeight / sampleSize > maxH) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = if (includeExtendedAssets) {
                            Bitmap.Config.ARGB_8888
                        } else {
                            Bitmap.Config.RGB_565
                        }
                    }
                    BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return@withContext false
                }
                cachedCoverPathByBookId[book.id] = path
                if (thumbCache.get(book.id) == null) {
                    val thumbBmp = if (bmp.width > 320 || bmp.height > 460 || bmp.config == Bitmap.Config.ARGB_8888) {
                        val scale = minOf(
                            300f / bmp.width.coerceAtLeast(1),
                            440f / bmp.height.coerceAtLeast(1),
                            1f,
                        )
                        val tw = (bmp.width * scale).roundToInt().coerceAtLeast(1)
                        val th = (bmp.height * scale).roundToInt().coerceAtLeast(1)
                        val scaled = if (tw < bmp.width || th < bmp.height) {
                            Bitmap.createScaledBitmap(bmp, tw, th, true)
                        } else {
                            bmp
                        }
                        if (scaled.config != Bitmap.Config.RGB_565 && !scaled.hasAlpha()) {
                            scaled.copy(Bitmap.Config.RGB_565, false)?.also {
                                if (scaled !== bmp) scaled.recycle()
                            } ?: scaled
                        } else {
                            scaled
                        }
                    } else {
                        bmp
                    }
                    thumbCache.put(book.id, thumbBmp.asImageBitmap())
                }

                if (!includeExtendedAssets) {
                    return@withContext true
                }
                rawCoverBitmapCache.put(book.id, bmp)

                // 生成半屏斜向瀑布专用的轻量圆角封面砖块 (224x320) 并预烘焙双层微边框，降低纹理带宽并省去逐帧描边
                if (waterfallSharpTileCache.get(book.id) == null) {
                    val tileW = 224
                    val tileH = 320
                    val tileRadius = 20f
                    val sharpTile = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.ARGB_8888)
                    val tc = AndroidCanvas(sharpTile)
                    val maskPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
                    val tileRectF = android.graphics.RectF(0f, 0f, tileW.toFloat(), tileH.toFloat())
                    tc.drawRoundRect(tileRectF, tileRadius, tileRadius, maskPaint)
                    maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    val srcAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                    val dstAspect = tileW.toFloat() / tileH.toFloat()
                    val srcRect = if (srcAspect > dstAspect) {
                        val cw = (bmp.height * dstAspect).roundToInt()
                        val l = (bmp.width - cw) / 2
                        AndroidRect(l, 0, l + cw, bmp.height)
                    } else {
                        val ch = (bmp.width / dstAspect).roundToInt()
                        val t = (bmp.height - ch) / 2
                        AndroidRect(0, t, bmp.width, t + ch)
                    }
                    tc.drawBitmap(bmp, srcRect, tileRectF, maskPaint)
                    maskPaint.xfermode = null
                    maskPaint.style = AndroidPaint.Style.STROKE
                    maskPaint.strokeWidth = 1.6f
                    maskPaint.color = 0x1A000000
                    tc.drawRoundRect(android.graphics.RectF(0.8f, 0.8f, tileW - 0.8f, tileH - 0.8f), tileRadius - 0.5f, tileRadius - 0.5f, maskPaint)
                    maskPaint.strokeWidth = 1.2f
                    maskPaint.color = 0x26FFFFFF
                    tc.drawRoundRect(android.graphics.RectF(1.8f, 1.8f, tileW - 1.8f, tileH - 1.8f), tileRadius - 1.2f, tileRadius - 1.2f, maskPaint)
                    sharpTile.setHasMipMap(true)
                    waterfallSharpTileCache.put(book.id, sharpTile)
                }

                // 使用 24 色相桶聚类提取封面真实鲜明的主色调与次色调（过滤冷灰阴影与极暗杂色，避免发蓝发灰）
                val extractedPair = if (colorCache.containsKey(cKey) && secondaryColorCache.containsKey(cKey)) {
                    colorCache[cKey]!! to secondaryColorCache[cKey]!!
                } else {
                    extractVibrantColorPair(bmp)?.also { pair ->
                        colorCache[cKey] = pair.first
                        secondaryColorCache[cKey] = pair.second
                    }
                }
                val extractedTriad = if (triColorCache.containsKey(cKey)) {
                    triColorCache[cKey]
                } else {
                    extractDominantTriColor(bmp)?.also { triad ->
                        triColorCache[cKey] = triad
                    }
                }
                publishLoadedBookState(
                    colorKey = cKey,
                    primary = extractedPair?.first,
                    secondary = extractedPair?.second,
                    triad = extractedTriad,
                )
                true
            } catch (_: Throwable) {
                false
            } finally {
                inFlightLoads.remove(inflightKey, newDeferred)
            }
        }
        newDeferred.complete(loaded)
        return loaded
    }

    internal fun extractVibrantColorPair(bitmap: Bitmap): Pair<Color, Color>? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        val sampleW = min(bitmap.width, 36).coerceAtLeast(1)
        val sampleH = min(bitmap.height, 52).coerceAtLeast(1)
        val sampledBmp = if (bitmap.width != sampleW || bitmap.height != sampleH) {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        } else {
            bitmap
        }
        val pixels = IntArray(sampleW * sampleH)
        try {
            sampledBmp.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        } finally {
            if (sampledBmp !== bitmap && !sampledBmp.isRecycled) {
                sampledBmp.recycle()
            }
        }

        val bucketCount = 24
        val vWeights = DoubleArray(bucketCount)
        val vR = DoubleArray(bucketCount)
        val vG = DoubleArray(bucketCount)
        val vB = DoubleArray(bucketCount)
        val mWeights = DoubleArray(bucketCount)
        val mR = DoubleArray(bucketCount)
        val mG = DoubleArray(bucketCount)
        val mB = DoubleArray(bucketCount)
        val hsv = FloatArray(3)

        for (pixel in pixels) {
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < 128) continue
            val rInt = (pixel ushr 16) and 0xFF
            val gInt = (pixel ushr 8) and 0xFF
            val bInt = pixel and 0xFF
            val r = rInt / 255.0
            val g = gInt / 255.0
            val b = bInt / 255.0
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val chroma = maxC - minC
            val luma = 0.299 * r + 0.587 * g + 0.114 * b

            android.graphics.Color.RGBToHSV(rInt, gInt, bInt, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            val bucket = ((hsv[0] / 15f).toInt()).coerceIn(0, bucketCount - 1)

            if (sat >= 0.16f && chroma >= 0.11 && value >= 0.14f && luma > 0.07 && luma < 0.955) {
                val valFocus = (1.0 - abs(luma - 0.55) * 1.15).coerceIn(0.30, 1.0)
                val weight = chroma.pow(1.05) * (0.50 + 0.50 * sat) * (0.45 + 0.55 * valFocus)
                vWeights[bucket] += weight
                vR[bucket] += r * weight
                vG[bucket] += g * weight
                vB[bucket] += b * weight
            } else if (sat >= 0.06f && chroma >= 0.04 && value >= 0.10f && luma > 0.06 && luma < 0.965) {
                val valFocus = (1.0 - abs(luma - 0.55) * 1.10).coerceIn(0.25, 1.0)
                val weight = (0.15 + chroma * 1.2) * (0.30 + 0.70 * sat) * (0.45 + 0.55 * valFocus)
                mWeights[bucket] += weight
                mR[bucket] += r * weight
                mG[bucket] += g * weight
                mB[bucket] += b * weight
            }
        }

        val useVibrant = vWeights.any { it > 0.0 }
        val weights = if (useVibrant) vWeights else mWeights
        val bR = if (useVibrant) vR else mR
        val bG = if (useVibrant) vG else mG
        val bB = if (useVibrant) vB else mB

        if (!weights.any { it > 0.0 }) {
            return Color(0xFF64748B) to Color(0xFF94A3B8)
        }

        val neighborhood = DoubleArray(bucketCount) { b ->
            val prev = (b - 1 + bucketCount) % bucketCount
            val next = (b + 1) % bucketCount
            weights[b] + 0.42 * (weights[prev] + weights[next])
        }
        val primaryBucket = (0 until bucketCount)
            .filter { weights[it] > 0.0 }
            .maxByOrNull { neighborhood[it] }
            ?: return Color(0xFF64748B) to Color(0xFF94A3B8)

        fun bucketDist(a: Int, b: Int): Int {
            val d = abs(a - b)
            return min(d, bucketCount - d)
        }

        val primaryWeight = weights[primaryBucket]
        val secondaryBucket = (0 until bucketCount)
            .filter { bucketDist(it, primaryBucket) >= 2 && weights[it] > primaryWeight * 0.25 }
            .maxByOrNull { weights[it] }
            ?: (0 until bucketCount)
                .filter { it != primaryBucket && weights[it] > primaryWeight * 0.35 }
                .maxByOrNull { weights[it] }

        fun bucketToVibrantColor(bIdx: Int): Color {
            val wSum = weights[bIdx].coerceAtLeast(1e-6)
            val avgR = ((bR[bIdx] / wSum) * 255.0).roundToInt().coerceIn(0, 255)
            val avgG = ((bG[bIdx] / wSum) * 255.0).roundToInt().coerceIn(0, 255)
            val avgB = ((bB[bIdx] / wSum) * 255.0).roundToInt().coerceIn(0, 255)
            val outHsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(avgR, avgG, avgB, outHsv)
            outHsv[1] = (outHsv[1] * 0.72f).coerceIn(if (useVibrant) 0.24f else 0.16f, 0.50f)
            outHsv[2] = outHsv[2].coerceIn(0.60f, 0.86f)
            return Color(android.graphics.Color.HSVToColor(outHsv))
        }

        val primaryColor = bucketToVibrantColor(primaryBucket)
        val secondaryColor = if (secondaryBucket != null) {
            bucketToVibrantColor(secondaryBucket)
        } else {
            val companionHsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(
                (primaryColor.red * 255).roundToInt(),
                (primaryColor.green * 255).roundToInt(),
                (primaryColor.blue * 255).roundToInt(),
                companionHsv,
            )
            companionHsv[0] = (companionHsv[0] + 24f) % 360f
            companionHsv[1] = (companionHsv[1] * 0.82f).coerceIn(0.20f, 0.44f)
            companionHsv[2] = (companionHsv[2] * 0.92f).coerceIn(0.58f, 0.90f)
            Color(android.graphics.Color.HSVToColor(companionHsv))
        }

        return primaryColor to secondaryColor
    }

    internal fun extractDominantTriColor(bitmap: Bitmap): Triple<Color, Color, Color>? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        val sampleW = min(bitmap.width, 40).coerceAtLeast(1)
        val sampleH = min(bitmap.height, 56).coerceAtLeast(1)
        val sampledBmp = if (bitmap.width != sampleW || bitmap.height != sampleH) {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        } else {
            bitmap
        }
        val pixels = IntArray(sampleW * sampleH)
        try {
            sampledBmp.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        } finally {
            if (sampledBmp !== bitmap && !sampledBmp.isRecycled) {
                sampledBmp.recycle()
            }
        }

        val bucketCount = 24
        val cWeights = DoubleArray(bucketCount)
        val cR = DoubleArray(bucketCount)
        val cG = DoubleArray(bucketCount)
        val cB = DoubleArray(bucketCount)
        val nWeights = DoubleArray(bucketCount)
        val nR = DoubleArray(bucketCount)
        val nG = DoubleArray(bucketCount)
        val nB = DoubleArray(bucketCount)
        val hsv = FloatArray(3)

        for (pixel in pixels) {
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < 128) continue
            val rInt = (pixel ushr 16) and 0xFF
            val gInt = (pixel ushr 8) and 0xFF
            val bInt = pixel and 0xFF
            val r = rInt / 255.0
            val g = gInt / 255.0
            val b = bInt / 255.0
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val chroma = maxC - minC
            val luma = 0.299 * r + 0.587 * g + 0.114 * b

            if ((luma > 0.94 && chroma < 0.08) || (luma < 0.075 && chroma < 0.085)) {
                continue
            }

            android.graphics.Color.RGBToHSV(rInt, gInt, bInt, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            val bucket = ((hsv[0] / 15f).toInt()).coerceIn(0, bucketCount - 1)
            val midToneFactor = (1.0 - abs(luma - 0.54) * 0.65).coerceIn(0.45, 1.0)

            if (sat >= 0.09f && chroma >= 0.055 && value >= 0.11f) {
                val weight = (0.42 + 0.58 * sat.toDouble().coerceIn(0.0, 1.0)) * midToneFactor
                cWeights[bucket] += weight
                cR[bucket] += r * weight
                cG[bucket] += g * weight
                cB[bucket] += b * weight
            } else {
                val weight = (0.35 + 0.65 * chroma.coerceIn(0.0, 1.0)) * midToneFactor
                nWeights[bucket] += weight
                nR[bucket] += r * weight
                nG[bucket] += g * weight
                nB[bucket] += b * weight
            }
        }

        val hasColor = cWeights.any { it > 0.0 }
        val weights = if (hasColor) cWeights else nWeights
        val bR = if (hasColor) cR else nR
        val bG = if (hasColor) cG else nG
        val bB = if (hasColor) cB else nB

        if (!weights.any { it > 0.0 }) {
            return Triple(Color(0xFF5D769C), Color(0xFF6B8BAE), Color(0xFF7A6D9B))
        }

        val smoothed = DoubleArray(bucketCount) { b ->
            val prev = (b - 1 + bucketCount) % bucketCount
            val next = (b + 1) % bucketCount
            weights[b] + 0.36 * (weights[prev] + weights[next])
        }

        fun bucketDist(a: Int, b: Int): Int {
            val d = abs(a - b)
            return min(d, bucketCount - d)
        }

        val firstBucket = (0 until bucketCount)
            .filter { weights[it] > 0.0 }
            .maxByOrNull { smoothed[it] }
            ?: return Triple(Color(0xFF5D769C), Color(0xFF6B8BAE), Color(0xFF7A6D9B))
        val firstWeight = weights[firstBucket]

        val secondBucket = (0 until bucketCount)
            .filter { bucketDist(it, firstBucket) >= 2 && weights[it] > firstWeight * 0.12 }
            .maxByOrNull { smoothed[it] }
            ?: (0 until bucketCount)
                .filter { it != firstBucket && weights[it] > firstWeight * 0.15 }
                .maxByOrNull { smoothed[it] }

        val thirdBucket = (0 until bucketCount)
            .filter { b ->
                b != firstBucket &&
                    b != secondBucket &&
                    bucketDist(b, firstBucket) >= 2 &&
                    (secondBucket == null || bucketDist(b, secondBucket) >= 2) &&
                    weights[b] > firstWeight * 0.07
            }
            .maxByOrNull { smoothed[it] }
            ?: (0 until bucketCount)
                .filter { b -> b != firstBucket && b != secondBucket && weights[b] > firstWeight * 0.08 }
                .maxByOrNull { smoothed[it] }

        fun refineBucketColor(bIdx: Int): Color {
            val wSum = weights[bIdx].coerceAtLeast(1e-6)
            val avgR = ((bR[bIdx] / wSum) * 255.0).roundToInt().coerceIn(0, 255)
            val avgG = ((bG[bIdx] / wSum) * 255.0).roundToInt().coerceIn(0, 255)
            val avgB = ((bB[bIdx] / wSum) * 255.0).roundToInt().coerceIn(0, 255)
            val outHsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(avgR, avgG, avgB, outHsv)
            if (hasColor) {
                outHsv[1] = (outHsv[1] * 0.72f).coerceIn(0.18f, 0.48f)
                outHsv[2] = outHsv[2].coerceIn(0.52f, 0.84f)
            } else {
                outHsv[1] = outHsv[1].coerceIn(0.08f, 0.30f)
                outHsv[2] = outHsv[2].coerceIn(0.46f, 0.82f)
            }
            return Color(android.graphics.Color.HSVToColor(outHsv))
        }

        fun shiftColor(base: Color, hueDelta: Float, satFactor: Float, valFactor: Float): Color {
            val tmpHsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(
                (base.red * 255f).roundToInt().coerceIn(0, 255),
                (base.green * 255f).roundToInt().coerceIn(0, 255),
                (base.blue * 255f).roundToInt().coerceIn(0, 255),
                tmpHsv,
            )
            tmpHsv[0] = ((tmpHsv[0] + hueDelta) % 360f + 360f) % 360f
            tmpHsv[1] = (tmpHsv[1] * satFactor).coerceIn(0.15f, 0.45f)
            tmpHsv[2] = (tmpHsv[2] * valFactor).coerceIn(0.48f, 0.88f)
            return Color(android.graphics.Color.HSVToColor(tmpHsv))
        }

        val c1 = refineBucketColor(firstBucket)
        val c2 = if (secondBucket != null) refineBucketColor(secondBucket) else shiftColor(c1, 26f, 0.88f, 0.96f)
        val c3 = if (thirdBucket != null) refineBucketColor(thirdBucket) else shiftColor(c1, -28f, 0.82f, 0.90f)
        return Triple(c1, c2, c3)
    }

    private fun createLightBlurredBitmap(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        // 多级双线性金字塔降采样再升采样，无需 CPU 像素模糊循环即可得到丝滑冷暖色场
        val step1 = Bitmap.createScaledBitmap(source, 54, 81, true)
        val step2 = Bitmap.createScaledBitmap(step1, 20, 30, true)
        val out = Bitmap.createScaledBitmap(step2, targetW, targetH, true)
        if (step1 !== source && step1 !== out) step1.recycle()
        if (step2 !== step1 && step2 !== source && step2 !== out) step2.recycle()
        return out
    }

    private fun createSmoothBlurredBitmap(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val step1 = Bitmap.createScaledBitmap(source, 32, 48, true)
        val step2 = Bitmap.createScaledBitmap(step1, 10, 15, true)
        val out = Bitmap.createScaledBitmap(step2, targetW, targetH, true)
        if (step1 !== source && step1 !== out) step1.recycle()
        if (step2 !== step1 && step2 !== source && step2 !== out) step2.recycle()
        return out
    }
}

private val AMBIENT_LAMP_OUTER_STOPS = floatArrayOf(0.15f, 0.85f)
private val AMBIENT_LAMP_INNER_STOPS = floatArrayOf(0.1f, 0.9f)
private val AMBIENT_LAMP_SCREEN_XFERMODE = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
private val LIGHT_FALLBACK_COVER_BRUSH = Brush.linearGradient(
    colors = listOf(Color(0xFFF2EFE9), Color(0xFFE6E2DA))
)
private val DARK_FALLBACK_COVER_BRUSH = Brush.linearGradient(
    colors = listOf(Color(0xFF26292E), Color(0xFF1C1E22))
)

/**
 * 环绕式圆角矩形双色温氛围灯修饰符（360° Perimeter Ambilight）：
 * 采用 GPU 加速的圆角矩形高斯模糊光场（BlurMaskFilter + LinearGradient），
 * 使用 `ModifierNodeElement` 缓存 Paint / BlurMaskFilter / LinearGradient，零逐帧对象分配，
 * 让组件的左、右、上、下四周边缘以及四个圆角均匀散发氛围光，并支持向下延展柔光照亮底部 Dock 栏。
 */
fun Modifier.ambientLamp(
    color: Color,
    isLight: Boolean,
    radiusDp: Dp,
    centerOffsetFraction: Offset = Offset.Zero,
    secondaryColor: Color = color,
    verticalReachMultiplier: Float = 1.0f,
): Modifier = this then AmbientLampElement(
    color = color,
    isLight = isLight,
    radiusDp = radiusDp,
    centerOffsetFraction = centerOffsetFraction,
    secondaryColor = secondaryColor,
    verticalReachMultiplier = verticalReachMultiplier,
)

private data class AmbientLampElement(
    val color: Color,
    val isLight: Boolean,
    val radiusDp: Dp,
    val centerOffsetFraction: Offset,
    val secondaryColor: Color,
    val verticalReachMultiplier: Float,
) : ModifierNodeElement<AmbientLampNode>() {
    override fun create(): AmbientLampNode = AmbientLampNode(
        color = color,
        isLight = isLight,
        radiusDp = radiusDp,
        centerOffsetFraction = centerOffsetFraction,
        secondaryColor = secondaryColor,
        verticalReachMultiplier = verticalReachMultiplier,
    )

    override fun update(node: AmbientLampNode) {
        node.updateParams(
            color = color,
            isLight = isLight,
            radiusDp = radiusDp,
            centerOffsetFraction = centerOffsetFraction,
            secondaryColor = secondaryColor,
            verticalReachMultiplier = verticalReachMultiplier,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ambientLamp"
        properties["color"] = color
        properties["isLight"] = isLight
        properties["radiusDp"] = radiusDp
        properties["verticalReachMultiplier"] = verticalReachMultiplier
    }
}

private class AmbientLampNode(
    private var color: Color,
    private var isLight: Boolean,
    private var radiusDp: Dp,
    private var centerOffsetFraction: Offset,
    private var secondaryColor: Color,
    private var verticalReachMultiplier: Float,
) : Modifier.Node(), DrawModifierNode {

    private val outerPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.FILL
    }
    private val innerPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.FILL
    }
    private val outerColors = IntArray(2)
    private val innerColors = IntArray(2)

    private var cachedDensity = -1f
    private var cachedWidth = -1f
    private var cachedHeight = -1f
    private var cachedIsLight = false
    private var cachedColorArgb = 0
    private var cachedSecondaryArgb = 0
    private var cachedReach = -1f

    fun updateParams(
        color: Color,
        isLight: Boolean,
        radiusDp: Dp,
        centerOffsetFraction: Offset,
        secondaryColor: Color,
        verticalReachMultiplier: Float,
    ) {
        if (this.color != color ||
            this.isLight != isLight ||
            this.radiusDp != radiusDp ||
            this.centerOffsetFraction != centerOffsetFraction ||
            this.secondaryColor != secondaryColor ||
            this.verticalReachMultiplier != verticalReachMultiplier
        ) {
            this.color = color
            this.isLight = isLight
            this.radiusDp = radiusDp
            this.centerOffsetFraction = centerOffsetFraction
            this.secondaryColor = secondaryColor
            this.verticalReachMultiplier = verticalReachMultiplier
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val w = size.width
        val h = size.height
        if (w > 1f && h > 1f) {
            val densityVal = density
            val cornerPx = 24.dp.toPx()
            val extraBottomPx = ((verticalReachMultiplier - 1f).coerceAtLeast(0f) * 68.dp.toPx())
            val spreadInner = 3.dp.toPx()
            val spreadOuter = 10.dp.toPx()

            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                // 在日夜切换截屏的软件画布（!isHardwareAccelerated）上跳过昂贵的 CPU BlurMaskFilter，使截屏耗时从 ~100ms 降至 <1ms
                if (!nc.isHardwareAccelerated) return@drawIntoCanvas

                if (cachedDensity != densityVal) {
                    cachedDensity = densityVal
                    outerPaint.maskFilter = BlurMaskFilter(50.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    innerPaint.maskFilter = BlurMaskFilter(28.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }

                val c1Argb = color.toArgb()
                val c2Argb = secondaryColor.toArgb()
                if (cachedWidth != w ||
                    cachedHeight != h ||
                    cachedIsLight != isLight ||
                    cachedColorArgb != c1Argb ||
                    cachedSecondaryArgb != c2Argb ||
                    cachedReach != verticalReachMultiplier
                ) {
                    cachedWidth = w
                    cachedHeight = h
                    cachedIsLight = isLight
                    cachedColorArgb = c1Argb
                    cachedSecondaryArgb = c2Argb
                    cachedReach = verticalReachMultiplier

                    val innerAlpha = if (isLight) 0.28f else 0.36f
                    val outerAlpha = if (isLight) 0.16f else 0.24f
                    outerColors[0] = color.copy(alpha = outerAlpha).toArgb()
                    outerColors[1] = secondaryColor.copy(alpha = outerAlpha).toArgb()
                    innerColors[0] = color.copy(alpha = innerAlpha).toArgb()
                    innerColors[1] = secondaryColor.copy(alpha = innerAlpha).toArgb()

                    outerPaint.shader = AndroidLinearGradient(
                        -spreadOuter,
                        0f,
                        w + spreadOuter,
                        h + extraBottomPx,
                        outerColors,
                        AMBIENT_LAMP_OUTER_STOPS,
                        AndroidShader.TileMode.CLAMP,
                    )
                    innerPaint.shader = AndroidLinearGradient(
                        0f,
                        0f,
                        w,
                        h,
                        innerColors,
                        AMBIENT_LAMP_INNER_STOPS,
                        AndroidShader.TileMode.CLAMP,
                    )
                    val xfermode = if (!isLight) AMBIENT_LAMP_SCREEN_XFERMODE else null
                    outerPaint.xfermode = xfermode
                    innerPaint.xfermode = xfermode
                }

                // 1) 外层远场漫射光环
                nc.drawRoundRect(
                    -spreadOuter,
                    -spreadOuter * 0.85f,
                    w + spreadOuter,
                    h + spreadOuter + extraBottomPx,
                    cornerPx + spreadOuter,
                    cornerPx + spreadOuter,
                    outerPaint,
                )

                // 2) 内层贴边环绕氛围光
                nc.drawRoundRect(
                    -spreadInner,
                    -spreadInner * 0.8f,
                    w + spreadInner,
                    h + spreadInner + extraBottomPx * 0.35f,
                    cornerPx + spreadInner,
                    cornerPx + spreadInner,
                    innerPaint,
                )
            }
        }
        drawContent()
    }
}

/**
 * Android 12+ 原生 RenderNode 硬件级高斯模糊修饰符（在独立 RenderThread 执行，0 CPU 开销）。
 */
fun Modifier.hardwareBlur(radiusDp: Dp): Modifier = this.graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radiusDp.value > 0f) {
        val px = radiusDp.toPx()
        renderEffect = android.graphics.RenderEffect.createBlurEffect(
            px,
            px,
            android.graphics.Shader.TileMode.DECAL,
        ).asComposeRenderEffect()
    }
}

/**
 * 统一的书籍封面组件（支持异步磁盘加载、圆角裁切与无封面 TXT 精致占位）。
 */
@Composable
fun BookCoverView(
    book: BookEntry,
    palette: AppPalette,
    width: Dp,
    height: Dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier,
    coverVersionTick: Int? = null,
) {
    val hasCoverPath = !book.coverPath.isNullOrBlank()
    val tick = coverVersionTick ?: CoverAssetCache.versionTick
    val thumb = if (hasCoverPath) {
        remember(book.id, book.coverPath, tick) {
            CoverAssetCache.getThumb(book.id)
        }
    } else {
        null
    }
    if (hasCoverPath) {
        LaunchedEffect(book.id, book.coverPath) {
            CoverAssetCache.ensureLoaded(book)
        }
    }

    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .background(if (palette.isLight) Color(0xFFE8E6E1) else Color(0xFF232528)),
    ) {
        if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // 精致 TXT / 无封面占位
            val accent = CoverAssetCache.getAmbientColor(book, 0, palette)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (palette.isLight) LIGHT_FALLBACK_COVER_BRUSH else DARK_FALLBACK_COVER_BRUSH)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(width = 5.dp, height = height)
                        .background(accent.copy(alpha = 0.65f))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                ) {
                    Text(
                        text = cleanBookTitle(book.title),
                        color = palette.text,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = book.author,
                        color = palette.muted,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 共享帧驱动时钟：用于底部 Gemini 边角动态氛围光；若根节点已注入 LocalAmbientFlowSeconds，则直接复用同一相位，
 * 自动在非激活/省电/检查模式/测试时钟下暂停。
 */
internal fun LayoutCoordinates.unclippedBoundsInRoot(): Rect {
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    if (w <= 0f || h <= 0f) return Rect.Zero
    val topLeft = localToRoot(Offset.Zero)
    val bottomRight = localToRoot(Offset(w, h))
    return Rect(
        left = minOf(topLeft.x, bottomRight.x),
        top = minOf(topLeft.y, bottomRight.y),
        right = maxOf(topLeft.x, bottomRight.x),
        bottom = maxOf(topLeft.y, bottomRight.y),
    )
}

@Composable
internal fun rememberAmbientFlowSeconds(isActive: Boolean): FloatState {
    val shared = LocalAmbientFlowSeconds.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerSave = LocalPowerSaveMode.current
    val isInspectionMode = LocalInspectionMode.current
    val isRobolectric = remember { "robolectric".equals(Build.FINGERPRINT, ignoreCase = true) }
    val localState = remember { mutableFloatStateOf(0f) }
    val shouldDrive = isActive && shared == null && !powerSave && !isInspectionMode && !isRobolectric
    LaunchedEffect(lifecycleOwner, shouldDrive) {
        if (!shouldDrive) return@LaunchedEffect
        val isTestClock = coroutineContext[MonotonicFrameClock]?.let { clock ->
            clock::class.java.name.contains("Test", ignoreCase = true)
        } == true
        if (isTestClock) return@LaunchedEffect
        val cycleSec = 8000.0
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastNanos = 0L
            var accSeconds = localState.floatValue.toDouble()
            while (coroutineIsActive) {
                withFrameNanos { frameNanos ->
                    if (lastNanos != 0L) {
                        val dtSec = ((frameNanos - lastNanos).coerceIn(0L, 100_000_000L)) / 1_000_000_000.0
                        accSeconds = (accSeconds + dtSec) % cycleSec
                        // Advance every native VSync frame (no artificial FPS cap) so waterfall
                        // and ambient shaders match the full display refresh rate.
                        localState.floatValue = accSeconds.toFloat()
                    }
                    lastNanos = frameNanos
                }
            }
        }
    }
    return shared ?: localState
}

private const val GEMINI_GAUSSIAN_MASK_PX = 160

private val GEMINI_GAUSSIAN_MASK_BITMAP: ImageBitmap by lazy {
    val bmp = Bitmap.createBitmap(
        GEMINI_GAUSSIAN_MASK_PX,
        GEMINI_GAUSSIAN_MASK_PX,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = AndroidCanvas(bmp)
    val center = GEMINI_GAUSSIAN_MASK_PX * 0.5f
    val colors = intArrayOf(
        android.graphics.Color.argb(255, 255, 255, 255),
        android.graphics.Color.argb((255f * 0.90f).roundToInt(), 255, 255, 255),
        android.graphics.Color.argb((255f * 0.72f).roundToInt(), 255, 255, 255),
        android.graphics.Color.argb((255f * 0.46f).roundToInt(), 255, 255, 255),
        android.graphics.Color.argb((255f * 0.22f).roundToInt(), 255, 255, 255),
        android.graphics.Color.argb((255f * 0.075f).roundToInt(), 255, 255, 255),
        android.graphics.Color.argb((255f * 0.015f).roundToInt(), 255, 255, 255),
        android.graphics.Color.TRANSPARENT,
    )
    val positions = floatArrayOf(0.00f, 0.15f, 0.30f, 0.48f, 0.66f, 0.82f, 0.94f, 1.00f)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        shader = AndroidRadialGradient(
            center,
            center,
            center,
            colors,
            positions,
            AndroidShader.TileMode.CLAMP,
        )
    }
    canvas.drawCircle(center, center, center, paint)
    bmp.asImageBitmap()
}

private fun deriveGeminiCornerTone(
    source: Color,
    isLight: Boolean,
    isEdgeBridge: Boolean,
    hsvScratch: FloatArray = FloatArray(3),
): Color {
    android.graphics.Color.RGBToHSV(
        (source.red * 255f).roundToInt().coerceIn(0, 255),
        (source.green * 255f).roundToInt().coerceIn(0, 255),
        (source.blue * 255f).roundToInt().coerceIn(0, 255),
        hsvScratch,
    )
    val origSat = hsvScratch[1]
    val isChromatic = origSat >= 0.08f
    if (isLight) {
        // Light mode: calm airy pastel tint so bottom corners stay soft and luminous on white canvas
        val baseSat = if (isChromatic) (origSat * 0.85f).coerceIn(0.28f, 0.48f) else 0.14f
        hsvScratch[1] = if (isEdgeBridge) (baseSat * 0.82f).coerceAtLeast(0.10f) else baseSat
        hsvScratch[2] = 1.00f
    } else {
        // Dark mode: rich deep midnight tone matching Gemini dark corner glow visibility on OLED/black
        // (compensated for calmed seed saturation <= 0.48 so corners read deep chromatic, not black/grey)
        val baseSat = if (isChromatic) {
            maxOf(origSat * 1.28f, 0.56f).coerceIn(0.56f, 0.78f)
        } else {
            0.20f
        }
        val baseVal = if (isChromatic) 0.62f else 0.42f
        hsvScratch[1] = baseSat
        hsvScratch[2] = if (isEdgeBridge) (baseVal * 0.84f).coerceAtLeast(0.32f) else baseVal
    }
    return Color(android.graphics.Color.HSVToColor(hsvScratch))
}

internal class GeminiCornerToneState(val isLight: Boolean) {
    private val hsvScratch = FloatArray(3)
    private var lastPrimaryArgb: Int = 0
    private var hasCached: Boolean = false
    lateinit var cornerColorFilter: ColorFilter
        private set
    lateinit var bottomBridgeColorFilter: ColorFilter
        private set

    fun ensureFilters(primaryColor: Color): Pair<ColorFilter, ColorFilter> {
        val argb = primaryColor.toArgb()
        if (!hasCached || argb != lastPrimaryArgb) {
            val corner = deriveGeminiCornerTone(primaryColor, isLight = isLight, isEdgeBridge = false, hsvScratch = hsvScratch)
            val bridge = deriveGeminiCornerTone(primaryColor, isLight = isLight, isEdgeBridge = true, hsvScratch = hsvScratch)
            cornerColorFilter = ColorFilter.tint(corner, BlendMode.SrcIn)
            bottomBridgeColorFilter = ColorFilter.tint(bridge, BlendMode.SrcIn)
            lastPrimaryArgb = argb
            hasCached = true
        }
        return cornerColorFilter to bottomBridgeColorFilter
    }
}

/**
 * 预构建 Gemini 风格底边/边角单主色动态氛围光画笔缓存：
 * - 浅色模式自动生成明亮通透的天空浅色调（如天蓝色）；
 * - 深色模式自动生成饱满沉稳的深邃明度（如深蓝色）；
 * 避免渲染循环内逐帧分配。
 */
internal class GeminiCornerGlowBrushCache(
    val isLight: Boolean,
    primaryColor: Color,
) {
    private val cornerColor = deriveGeminiCornerTone(primaryColor, isLight = isLight, isEdgeBridge = false)
    private val bottomBridgeColor = deriveGeminiCornerTone(primaryColor, isLight = isLight, isEdgeBridge = true)

    internal val cornerColorFilter: ColorFilter = ColorFilter.tint(cornerColor, BlendMode.SrcIn)
    internal val bottomBridgeColorFilter: ColorFilter = ColorFilter.tint(bottomBridgeColor, BlendMode.SrcIn)

    val cornerLeftBrush: Brush by lazy {
        createGaussianCornerBrush(
            color = cornerColor,
            peakAlpha = if (isLight) 0.36f else 0.52f,
        )
    }
    val cornerRightBrush: Brush by lazy {
        createGaussianCornerBrush(
            color = cornerColor,
            peakAlpha = if (isLight) 0.31f else 0.44f,
        )
    }
    val bottomTideBrush: Brush by lazy {
        createGaussianCornerBrush(
            color = bottomBridgeColor,
            peakAlpha = if (isLight) 0.19f else 0.30f,
        )
    }

    private fun createGaussianCornerBrush(color: Color, peakAlpha: Float): Brush {
        return Brush.radialGradient(
            0.00f to color.copy(alpha = peakAlpha),
            0.15f to color.copy(alpha = peakAlpha * 0.90f),
            0.30f to color.copy(alpha = peakAlpha * 0.72f),
            0.48f to color.copy(alpha = peakAlpha * 0.46f),
            0.66f to color.copy(alpha = peakAlpha * 0.22f),
            0.82f to color.copy(alpha = peakAlpha * 0.075f),
            0.94f to color.copy(alpha = peakAlpha * 0.015f),
            1.00f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f,
        )
    }
}

/**
 * 共享纯底角 Gemini 氛围光画布（常驻于 HorizontalPager 底层及子页 Backdrop）。
 */
/**
 * Renders foreground [content] cleanly over [ambientBackground] on screen while feeding only
 * [paletteBackgroundColor] + [content] (excluding the bottom ambient corner glow) into [backdrop].
 * Keeps floating Liquid Glass surfaces (Dock, Chrome) pure neutral glass without bottom corner glow
 * staining their interior, while continuously refracting scrolling cards and covers.
 */
@Composable
internal fun IsolatedAmbientGlassContainer(
    backdrop: GlassBackdrop?,
    paletteBackgroundColor: Color,
    isolateBottomGlow: Boolean = true,
    renderAmbientBackground: Boolean = true,
    transparentBottomOnPagerTransition: Boolean = true,
    skipCornerGlowAndIsolation: Boolean = LocalRootHandlesBottomAmbient.current,
    ambientBackground: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pagerTransitioningState = LocalPagerTransitioningState.current
    val staticPagerTransitioning = LocalPagerTransitioning.current
    val dockHiddenState = LocalDockOffscreenHiddenState.current
    val isDockHidden = dockHiddenState?.value == true
    val captureToLayer = backdrop != null && isolateBottomGlow && !skipCornerGlowAndIsolation
    val contentLayer = rememberGraphicsLayer()
    val topFadeHeightCache = remember { floatArrayOf(-1f) }
    val topFadeBrushCache = remember(paletteBackgroundColor) { arrayOfNulls<Brush>(1) }
    Box(modifier = modifier.fillMaxSize()) {
        // 0. When NOT isolating into `backdrop` (`backdrop == null` or `!isolateBottomGlow`), draw ambient
        //    directly beneath `content()` so visual output matches identically with zero extra GraphicsLayer.
        if (renderAmbientBackground && !captureToLayer && !skipCornerGlowAndIsolation) {
            ambientBackground()
        }
        // 1. PERMANENT UNCONDITIONAL SLOT for `content()` — NEVER moves or unmounts across `backdrop` or
        //    `dockHidden` toggles (prevents full Shelf/Pager subtree rebuilds & bitmap re-uploads).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null && (!isolateBottomGlow || skipCornerGlowAndIsolation)) {
                        Modifier.glassBackdrop(backdrop)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (skipCornerGlowAndIsolation && renderAmbientBackground) {
                        Modifier.drawBehind {
                            val h = size.height
                            if (h > 1f && size.width > 1f) {
                                val fadeEndY = (h * 0.56f).coerceAtLeast(1f)
                                val brush = if (topFadeBrushCache[0] != null && kotlin.math.abs(fadeEndY - topFadeHeightCache[0]) <= 0.5f) {
                                    topFadeBrushCache[0]!!
                                } else {
                                    Brush.verticalGradient(
                                        0.00f to paletteBackgroundColor,
                                        0.84f to paletteBackgroundColor,
                                        1.00f to Color.Transparent,
                                        startY = 0f,
                                        endY = fadeEndY,
                                    ).also {
                                        topFadeHeightCache[0] = fadeEndY
                                        topFadeBrushCache[0] = it
                                    }
                                }
                                drawRect(
                                    brush = brush,
                                    size = Size(size.width, fadeEndY),
                                )
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .drawWithContent {
                    if (captureToLayer) {
                        contentLayer.record {
                            this@drawWithContent.drawContent()
                        }
                    } else {
                        this@drawWithContent.drawContent()
                    }
                },
        ) {
            content()
        }
        // 2. Capture clean [neutral background + foreground content] into Vitro `backdrop`.
        if (captureToLayer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdrop(backdrop)
                    .drawBehind {
                        val transitioningNow = transparentBottomOnPagerTransition &&
                            (pagerTransitioningState?.value ?: staticPagerTransitioning)
                        val bgHeight = if (transitioningNow) size.height * 0.56f else size.height
                        drawRect(
                            color = paletteBackgroundColor,
                            size = Size(size.width, bgHeight),
                        )
                        drawLayer(contentLayer)
                    },
            )
        }
        // 3. Lower ambient glow + foreground re-composite (skipped at draw-time during pager
        //    transitions without removing/re-adding slots, avoiding recomposition spikes).
        if (captureToLayer && renderAmbientBackground && !isDockHidden) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val transitioningNow = transparentBottomOnPagerTransition &&
                            (pagerTransitioningState?.value ?: staticPagerTransitioning)
                        if (!transitioningNow) {
                            clipRect(top = size.height * 0.57f) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    },
            ) {
                ambientBackground()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val transitioningNow = transparentBottomOnPagerTransition &&
                            (pagerTransitioningState?.value ?: staticPagerTransitioning)
                        if (!transitioningNow) {
                            clipRect(top = size.height * 0.57f) {
                                drawLayer(contentLayer)
                            }
                        }
                    },
            )
        }
    }
}

@Composable
internal fun GeminiCornerAmbientCanvas(
    palette: AppPalette,
    primaryColor: Color = palette.primary,
    primaryColorProvider: (() -> Color)? = null,
    flowTimeProvider: () -> Float,
    topLimitFraction: Float,
    modifier: Modifier = Modifier,
) {
    val isLight = palette.isLight
    val toneState = remember(isLight) { GeminiCornerToneState(isLight = isLight) }
    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val resolvedColor = primaryColorProvider?.invoke() ?: primaryColor
        val (cornerFilter, bridgeFilter) = toneState.ensureFilters(resolvedColor)
        drawGeminiCornerAmbientGlowTextures(
            isLight = isLight,
            cornerColorFilter = cornerFilter,
            bottomBridgeColorFilter = bridgeFilter,
            flowSeconds = flowTimeProvider(),
            topLimitFraction = topLimitFraction,
        )
    }
}

/**
 * Gemini 风格底角上漫动态氛围光：单主色调光源分别锚定左下角、右下角与底边线下沿，
 * 沿左右两侧边角柔顺上漫形成 U 形弧碗光晕，同时保持屏幕中部通透，严格止步于 topLimitFraction 之上。
 */
internal fun DrawScope.drawGeminiCornerAmbientGlow(
    brushCache: GeminiCornerGlowBrushCache,
    flowSeconds: Float,
    topLimitFraction: Float,
) {
    drawGeminiCornerAmbientGlowTextures(
        isLight = brushCache.isLight,
        cornerColorFilter = brushCache.cornerColorFilter,
        bottomBridgeColorFilter = brushCache.bottomBridgeColorFilter,
        flowSeconds = flowSeconds,
        topLimitFraction = topLimitFraction,
    )
}

internal fun DrawScope.drawGeminiCornerAmbientGlowTextures(
    isLight: Boolean,
    cornerColorFilter: ColorFilter,
    bottomBridgeColorFilter: ColorFilter,
    flowSeconds: Float,
    topLimitFraction: Float,
) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return

    val maskBitmap = GEMINI_GAUSSIAN_MASK_BITMAP
    val invMaskSize = 1f / GEMINI_GAUSSIAN_MASK_PX.toFloat()

    val leftPeakAlpha = if (isLight) 0.36f else 0.52f
    val rightPeakAlpha = if (isLight) 0.31f else 0.44f
    val bridgePeakAlpha = if (isLight) 0.19f else 0.30f

    val safeTopY = h * topLimitFraction.coerceIn(0.50f, 0.74f)
    val p1 = flowSeconds * 0.42f
    val p2 = flowSeconds * 0.33f
    val p3 = flowSeconds * 0.27f

    val breathe1 = 0.95f + 0.05f * sin(p1 * 1.15f)
    val breathe2 = 0.95f + 0.05f * cos(p2 * 1.07f)
    val breathe3 = 0.94f + 0.06f * sin(p3 * 1.23f)

    // Plume 1: Bottom-left corner rising smoothly up the left edge & flanking Dock left
    val cy1 = h * (1.015f + 0.012f * cos(p1 * 0.83f))
    val reach1 = (cy1 - safeTopY).coerceAtLeast(h * 0.16f)
    val ry1 = (reach1 * (0.94f + 0.06f * ((sin(p1 * 1.13f) + 1f) * 0.5f))).coerceAtLeast(1f)
    val rx1 = (w * (0.74f + 0.07f * sin(p1 * 0.65f)) * breathe1).coerceAtLeast(1f)
    val cx1 = w * (0.04f + 0.14f * ((sin(p1) + 1f) * 0.5f))
    withTransform({
        translate(left = cx1 - rx1, top = cy1 - ry1)
        scale(scaleX = rx1 * 2f * invMaskSize, scaleY = ry1 * 2f * invMaskSize, pivot = Offset.Zero)
    }) {
        drawImage(
            image = maskBitmap,
            alpha = (leftPeakAlpha * (0.90f + 0.10f * sin(p1 * 0.95f))).coerceIn(0f, 1f),
            colorFilter = cornerColorFilter,
            filterQuality = FilterQuality.Low,
        )
    }

    // Plume 2: Bottom-right corner rising counter-phase up the right edge & flanking Dock right
    val cy2 = h * (1.015f + 0.012f * sin(p2 * 0.91f))
    val reach2 = (cy2 - safeTopY).coerceAtLeast(h * 0.16f)
    val ry2 = (reach2 * (0.92f + 0.07f * ((cos(p2 * 1.07f) + 1f) * 0.5f))).coerceAtLeast(1f)
    val rx2 = (w * (0.72f + 0.07f * cos(p2 * 0.61f)) * breathe2).coerceAtLeast(1f)
    val cx2 = w * (0.96f - 0.14f * ((cos(p2) + 1f) * 0.5f))
    withTransform({
        translate(left = cx2 - rx2, top = cy2 - ry2)
        scale(scaleX = rx2 * 2f * invMaskSize, scaleY = ry2 * 2f * invMaskSize, pivot = Offset.Zero)
    }) {
        drawImage(
            image = maskBitmap,
            alpha = (rightPeakAlpha * (0.88f + 0.12f * cos(p2 * 0.88f))).coerceIn(0f, 1f),
            colorFilter = cornerColorFilter,
            filterQuality = FilterQuality.Low,
        )
    }

    // Plume 3: Bottom-horizon flowing tide bridging both corners and softly cradling around Dock
    val cy3 = h * (1.045f + 0.010f * cos(p3 * 0.79f))
    val reach3 = (cy3 - safeTopY).coerceAtLeast(h * 0.16f)
    val ry3 = (reach3 * (0.70f + 0.08f * ((cos(p3 * 1.19f) + 1f) * 0.5f))).coerceAtLeast(1f)
    val rx3 = (w * (0.90f + 0.06f * sin(p3 * 0.71f)) * breathe3).coerceAtLeast(1f)
    val cx3 = w * (0.50f + 0.24f * sin(p3 + 1.25f))
    withTransform({
        translate(left = cx3 - rx3, top = cy3 - ry3)
        scale(scaleX = rx3 * 2f * invMaskSize, scaleY = ry3 * 2f * invMaskSize, pivot = Offset.Zero)
    }) {
        drawImage(
            image = maskBitmap,
            alpha = (bridgePeakAlpha * (0.88f + 0.12f * sin(p3 * 1.05f))).coerceIn(0f, 1f),
            colorFilter = bottomBridgeColorFilter,
            filterQuality = FilterQuality.Low,
        )
    }
}
