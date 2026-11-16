package com.squartor.reader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.text.LineBreakConfig
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.StrikethroughSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.Base64
import android.util.LruCache
import com.squartor.reader.data.InlineStyleKind
import com.squartor.reader.data.InlineStyleSpan
import com.squartor.reader.model.ReadingStyle
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ReaderBlockItem(
    val kind: String,
    val text: String,
    val imageSrc: String? = null,
    val inlineSpans: List<InlineStyleSpan> = emptyList(),
    val align: String? = null,
)

sealed interface PageElement {
    val yOffset: Float
    val height: Float

    data class TextSlice(
        val text: CharSequence,
        val isTitle: Boolean,
        override val yOffset: Float,
        override val height: Float,
        val masterLayout: StaticLayout? = null,
        val startLine: Int = 0,
        val endLine: Int = 0,
        val lineTopPx: Int = 0,
        val lineBottomPx: Int = 0,
        val isQuote: Boolean = false,
        val align: String? = null,
        val quoteInsetPx: Float = 0f,
        val cachedWidthPx: Int = 0,
        val cachedColorArgb: Int = 0,
        val startCharOffset: Int = 0,
        val endCharOffset: Int = 0,
        val justifiedData: JustifiedSliceData? = null,
    ) : PageElement

    data class ImageSlice(
        val imageSrc: String,
        override val yOffset: Float,
        override val height: Float,
        val startCharOffset: Int = 0,
        val endCharOffset: Int = 0,
    ) : PageElement
}

data class ComposedPage(
    val chapterIndex: Int,
    val chapterTitle: String,
    val pageIndex: Int,
    val totalPagesInChapter: Int,
    val elements: List<PageElement>,
    val plainSnippet: String,
    val startCharOffset: Int = 0,
    val endCharOffset: Int = 0,
)

internal data class CjkKernAdjustment(
    val index: Int,
    val advanceEm: Float,
    val drawShiftEm: Float,
)

data class JustifiedDrawRun(
    val text: String,
    val x: Float,
    val styleBits: Int,
    val alphaFactor: Float,
)

data class JustifiedLineDecoration(
    val startX: Float,
    val endX: Float,
    val isUnderline: Boolean,
    val isStrikethrough: Boolean,
    val alphaFactor: Float,
)

data class JustifiedLineData(
    val lineIndex: Int,
    val baselineOffsetPx: Float,
    val runs: List<JustifiedDrawRun>,
    val decorations: List<JustifiedLineDecoration>? = null,
)

data class JustifiedSliceData(
    val lines: List<JustifiedLineData?>,
    val boldTypeface: Typeface?,
)

private class CjkPunctuationKernSpan(
    private val advanceEm: Float,
    private val drawShiftEm: Float,
) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (fm != null) {
            val pfm = paint.fontMetricsInt
            fm.ascent = pfm.ascent
            fm.descent = pfm.descent
            fm.top = pfm.top
            fm.bottom = pfm.bottom
            fm.leading = pfm.leading
        }
        return (paint.textSize * advanceEm).roundToInt().coerceAtLeast(1)
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val shiftedX = x + paint.textSize * drawShiftEm
        canvas.drawText(text, start, end, shiftedX, y.toFloat(), paint)
    }
}

private class CustomTypefaceStyleSpan(
    private val customTypeface: Typeface?,
    private val italicSkewX: Float = 0f,
) : MetricAffectingSpan() {
    override fun updateMeasureState(textPaint: TextPaint) {
        apply(textPaint)
    }

    override fun updateDrawState(tp: TextPaint) {
        apply(tp)
    }

    private fun apply(paint: TextPaint) {
        if (customTypeface != null) {
            paint.typeface = customTypeface
        }
        if (italicSkewX != 0f) {
            paint.textSkewX = italicSkewX
        }
    }
}

internal object ReaderPreloadCache {
    private data class BlocksKey(val bookId: String, val chapterIndex: Int)
    private data class PagesKey(val bookId: String, val chapterIndex: Int, val signature: String)

    private val lock = Any()
    private val blocksCache = object : LruCache<BlocksKey, List<ReaderBlockItem>>(24) {}
    private val pagesCache = object : LruCache<PagesKey, List<ComposedPage>>(24) {}

    private fun normalizeSignature(signature: String): String =
        if (signature.contains('#')) signature else "$signature#0"

    fun trimMemory(level: Int) {
        if (level < 20) return
        synchronized(lock) {
            if (level >= 60) {
                blocksCache.evictAll()
                pagesCache.evictAll()
            } else {
                blocksCache.trimToSize(10)
                pagesCache.trimToSize(10)
            }
        }
    }

    fun getBlocks(bookId: String, chapterIndex: Int): List<ReaderBlockItem>? =
        synchronized(lock) { blocksCache.get(BlocksKey(bookId, chapterIndex)) }

    fun putBlocks(bookId: String, chapterIndex: Int, blocks: List<ReaderBlockItem>) {
        synchronized(lock) { blocksCache.put(BlocksKey(bookId, chapterIndex), blocks) }
    }

    fun getPages(bookId: String, chapterIndex: Int, signature: String): List<ComposedPage>? =
        synchronized(lock) { pagesCache.get(PagesKey(bookId, chapterIndex, normalizeSignature(signature))) }

    fun putPages(bookId: String, chapterIndex: Int, signature: String, pages: List<ComposedPage>) {
        synchronized(lock) { pagesCache.put(PagesKey(bookId, chapterIndex, normalizeSignature(signature)), pages) }
    }

    fun buildPaginationSignature(
        bookId: String,
        style: ReadingStyle,
        topMarginPx: Int,
        contentWidthPx: Int,
        contentHeightPx: Int,
        textArgb: Int,
    ): String {
        return listOf(
            bookId,
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
            style.fontPath.orEmpty(),
            contentWidthPx,
            contentHeightPx,
            textArgb,
        ).joinToString("|")
    }
}

class NativeStaticLayoutPager(private val appContext: Context? = null) {

    fun paginate(
        chapterIndex: Int,
        chapterTitle: String,
        blocks: List<ReaderBlockItem>,
        style: ReadingStyle,
        contentWidthPx: Int,
        contentHeightPx: Int,
        density: Float,
        imageAspectRatios: Map<String, Float> = emptyMap(),
    ): List<ComposedPage> {
        val ctx = requireNotNull(appContext) { "Context is required for NativeStaticLayoutPager.paginate" }
        return paginateChapter(
            context = ctx,
            chapterIndex = chapterIndex,
            chapterTitle = chapterTitle,
            blocks = blocks,
            style = style,
            contentWidthPx = contentWidthPx,
            contentHeightPx = contentHeightPx,
            density = density,
            imageAspectRatios = imageAspectRatios,
        )
    }

    companion object {
        private const val ASSET_VARIABLE_FONT = "fonts/NotoSansSC-VF.ttf"
        private const val FULLWIDTH_INDENT = "\u3000\u3000"
        internal const val WORD_JOINER = '\u2060'
        private const val STYLE_BOLD = 1 shl 0
        private const val STYLE_ITALIC = 1 shl 1
        private const val STYLE_UNDERLINE = 1 shl 2
        private const val STYLE_STRIKETHROUGH = 1 shl 3
        private const val STYLE_CODE = 1 shl 4

        // Line-start prohibited punctuation (CLREQ / JIS X 4051 kinsoku)
        private val lineStartProhibitedChars = setOf(
            '、', '，', '。', '．', '·', '；', '：', '？', '！', '‽', '％', '‰',
            '）', '］', '｝', '〉', '》', '」', '』', '】', '〕', '〗', '〙', '〛',
            '”', '’', '›', '»', 'ゝ', 'ゞ', 'ー',
            'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ッ', 'ャ', 'ュ', 'ョ', 'ヮ', 'ヵ', 'ヶ',
            'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'っ', 'ゃ', 'ゅ', 'ょ', 'ゎ', '～',
        )

        // Line-end prohibited opening punctuation
        private val lineEndProhibitedChars = setOf(
            '（', '［', '｛', '〈', '《', '「', '『', '【', '〔', '〖', '〘', '〚',
            '“', '‘', '‹', '«', '￥', '＄',
        )

        // Fullwidth punctuation with ~0.44em trailing dead space on the right side
        private val rightSparseCjkPunctuation = setOf(
            '，', '。', '、', '；', '：', '）', '］', '｝', '〉', '》', '」', '』', '】', '〕', '”', '’',
        )

        // Fullwidth punctuation with ~0.44em leading dead space on the left side
        private val leftSparseCjkPunctuation = setOf(
            '（', '［', '｛', '〈', '《', '「', '『', '【', '〔', '“', '‘',
        )

        private val allFullwidthClusterPunctuation = rightSparseCjkPunctuation +
            leftSparseCjkPunctuation +
            setOf('？', '！')

        private val typefaceCache = object : LruCache<String, Typeface>(24) {}
        private val aspectRatioCache = object : LruCache<String, Float>(256) {}

        private val inlineImgTagRegex = Regex(
            """<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["'][^>]*>""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Inserts Unicode Word Joiners (\u2060) to enforce CJK kinsoku rules across all Android API levels
         * (including API 26-32 where LineBreakConfig is unavailable) and remaps InlineStyleSpan offsets.
         */
        internal fun applyCjkKinsokuWithSpans(
            text: String,
            spans: List<InlineStyleSpan> = emptyList(),
        ): Pair<String, List<InlineStyleSpan>> {
            if (text.length <= 1) return text to spans
            val sb = StringBuilder(text.length + 16)
            val charPositions = IntArray(text.length)
            var consecutiveBoundRun = 0

            for (i in text.indices) {
                val ch = text[i]
                val prev = if (i > 0) text[i - 1] else '\u0000'

                val bindWithPrev = if (i > 0 &&
                    prev != WORD_JOINER &&
                    ch != WORD_JOINER &&
                    !prev.isWhitespace() &&
                    prev != '\u3000' &&
                    consecutiveBoundRun < 3
                ) {
                    lineStartProhibitedChars.contains(ch) ||
                        lineEndProhibitedChars.contains(prev) ||
                        (ch == '—' && prev == '—') ||
                        (ch == '…' && prev == '…')
                } else {
                    false
                }

                if (bindWithPrev) {
                    sb.append(WORD_JOINER)
                    consecutiveBoundRun++
                } else {
                    consecutiveBoundRun = 0
                }

                charPositions[i] = sb.length
                sb.append(ch)
            }

            if (sb.length == text.length) return text to spans
            val outText = sb.toString()
            if (spans.isEmpty()) return outText to emptyList()

            val remapped = spans.mapNotNull { span ->
                val s = span.start.coerceIn(0, text.length)
                val e = span.end.coerceIn(0, text.length)
                if (e <= s) {
                    null
                } else {
                    val newStart = charPositions[s]
                    val newEnd = charPositions[e - 1] + 1
                    if (newEnd > newStart) span.copy(start = newStart, end = newEnd) else null
                }
            }
            return outText to remapped
        }

        /**
         * Computes non-distorting advance compressions for consecutive CJK fullwidth punctuation
         * (CLREQ / Apple Books contextual punctuation spacing), skipping invisible Word Joiners.
         */
        internal fun computeCjkKernAdjustments(text: CharSequence): List<CjkKernAdjustment> {
            if (text.length < 2) return emptyList()
            val adjustments = mutableListOf<CjkKernAdjustment>()
            var i = 0
            while (i < text.length) {
                val curr = text[i]
                if (curr == WORD_JOINER) {
                    i++
                    continue
                }
                var nextIdx = i + 1
                while (nextIdx < text.length && text[nextIdx] == WORD_JOINER) {
                    nextIdx++
                }
                if (nextIdx < text.length) {
                    val next = text[nextIdx]
                    if (rightSparseCjkPunctuation.contains(curr) &&
                        allFullwidthClusterPunctuation.contains(next)
                    ) {
                        // Trim trailing right sidebearing on the first mark (e.g., ”。 / ）， / ：“)
                        adjustments.add(
                            CjkKernAdjustment(
                                index = i,
                                advanceEm = 0.60f,
                                drawShiftEm = 0f,
                            )
                        )
                    } else if (leftSparseCjkPunctuation.contains(curr) &&
                        leftSparseCjkPunctuation.contains(next)
                    ) {
                        // Tuck consecutive opening marks (e.g., “《 / 「『 / （《) by trimming second mark's left sidebearing
                        adjustments.add(
                            CjkKernAdjustment(
                                index = nextIdx,
                                advanceEm = 0.62f,
                                drawShiftEm = -0.36f,
                            )
                        )
                        i = nextIdx
                        continue
                    }
                }
                i++
            }
            return adjustments
        }

        fun recordImageDimensions(imageSrc: String, width: Int, height: Int) {
            if (imageSrc.isNotBlank() && width > 0 && height > 0) {
                val ratio = (width.toFloat() / height.toFloat()).coerceIn(0.2f, 5.0f)
                synchronized(aspectRatioCache) {
                    aspectRatioCache.put(imageSrc, ratio)
                }
            }
        }

        fun getCachedAspectRatio(imageSrc: String): Float? {
            if (imageSrc.isBlank()) return null
            synchronized(aspectRatioCache) {
                return aspectRatioCache.get(imageSrc)
            }
        }

        fun resolveTypeface(
            context: Context,
            customFontPath: String?,
            weight: Int,
        ): Typeface {
            val clampedWeight = weight.coerceIn(100, 900)
            val cleanCustomPath = customFontPath?.trim()?.takeIf { it.isNotEmpty() }
            val cacheKey = "${cleanCustomPath ?: ASSET_VARIABLE_FONT}|$clampedWeight"
            synchronized(typefaceCache) {
                typefaceCache.get(cacheKey)?.let { return it }
            }

            val loaded: Typeface = try {
                if (cleanCustomPath != null) {
                    val file = File(cleanCustomPath)
                    if (file.exists() && file.canRead() && file.length() > 0L) {
                        Typeface.Builder(file)
                            .setFontVariationSettings("'wght' $clampedWeight")
                            .build()
                            ?: Typeface.createFromFile(file)
                    } else {
                        buildAssetVariableTypeface(context, clampedWeight)
                    }
                } else {
                    buildAssetVariableTypeface(context, clampedWeight)
                }
            } catch (_: Throwable) {
                try {
                    buildAssetVariableTypeface(context, clampedWeight)
                } catch (_: Throwable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Typeface.create(Typeface.DEFAULT, clampedWeight, false)
                    } else {
                        Typeface.DEFAULT
                    }
                }
            }

            synchronized(typefaceCache) {
                typefaceCache.put(cacheKey, loaded)
            }
            return loaded
        }

        private fun buildAssetVariableTypeface(context: Context, weight: Int): Typeface {
            val built = Typeface.Builder(context.assets, ASSET_VARIABLE_FONT)
                .setFontVariationSettings("'wght' ${weight.coerceIn(100, 900)}")
                .build()
            if (built != null) return built
            val base = Typeface.createFromAsset(context.assets, ASSET_VARIABLE_FONT)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Typeface.create(base, weight.coerceIn(100, 900), false)
            } else {
                base
            }
        }

        fun createBodyPaint(
            context: Context,
            style: ReadingStyle,
            density: Float,
            textColorArgb: Int,
        ): TextPaint {
            val scaledSizePx = (style.fontSize.coerceIn(14f, 32f) * density * 1.06f)
                .coerceAtLeast(12f)
            val weight = style.fontWeightValue.coerceIn(100, 900)
            return TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                textSize = scaledSizePx
                typeface = resolveTypeface(context, style.fontPath, weight)
                color = textColorArgb
                letterSpacing = (style.letterSpacing / style.fontSize.coerceAtLeast(10f))
                    .coerceIn(0f, 0.25f)
            }
        }

        fun createTitlePaint(
            context: Context,
            style: ReadingStyle,
            density: Float,
            textColorArgb: Int,
        ): TextPaint {
            val scaledSizePx = (style.fontSize.coerceIn(14f, 32f) * density * 1.40f)
                .coerceAtLeast(16f)
            val titleWeight = (style.fontWeightValue + 200).coerceIn(600, 900)
            return TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                textSize = scaledSizePx
                typeface = resolveTypeface(context, style.fontPath, titleWeight)
                color = textColorArgb
                letterSpacing = (style.letterSpacing / style.fontSize.coerceAtLeast(10f))
                    .coerceIn(0f, 0.25f)
            }
        }

        fun buildStaticLayout(
            text: CharSequence,
            paint: TextPaint,
            widthPx: Int,
            lineHeightMultiplier: Float,
            isTitle: Boolean,
            alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        ): StaticLayout {
            val safeWidth = widthPx.coerceAtLeast(64)
            val fm = paint.fontMetrics
            val naturalLineHeight = (fm.descent - fm.ascent).coerceAtLeast(1f)
            val effectiveMultiplier = if (isTitle) {
                1.35f
            } else {
                lineHeightMultiplier.coerceIn(1.2f, 2.6f)
            }
            val targetLineHeight = paint.textSize * effectiveMultiplier
            val extraSpacing = (targetLineHeight - naturalLineHeight).coerceAtLeast(0f)

            val builder = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, safeWidth)
                .setAlignment(alignment)
                .setLineSpacing(extraSpacing, 1.0f)
                .setIncludePad(false)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)

            val shouldJustify = !isTitle && alignment == Layout.Alignment.ALIGN_NORMAL
            applyBreakStrategyAndJustification(builder, shouldJustify)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val wordStyle = if (isTitle) {
                    LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
                } else {
                    LineBreakConfig.LINE_BREAK_WORD_STYLE_NONE
                }
                val lineBreakConfig = LineBreakConfig.Builder()
                    .setLineBreakStyle(LineBreakConfig.LINE_BREAK_STYLE_STRICT)
                    .setLineBreakWordStyle(wordStyle)
                    .build()
                builder.setLineBreakConfig(lineBreakConfig)
            }

            return builder.build()
        }

        private fun applyBreakStrategyAndJustification(
            builder: StaticLayout.Builder,
            justify: Boolean,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                if (justify) {
                    builder.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD)
                }
            } else {
                applyLegacyBreakStrategyAndJustification(builder, justify)
            }
        }

        @SuppressLint("WrongConstant")
        private fun applyLegacyBreakStrategyAndJustification(
            builder: StaticLayout.Builder,
            justify: Boolean,
        ) {
            builder.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            if (justify) {
                builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            }
        }

        private val quoteBarPaintLocal = ThreadLocal.withInitial {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
        }
        private val quoteBarRectLocal = ThreadLocal.withInitial { RectF() }
        private val justifiedScratchPaintLocal = ThreadLocal.withInitial {
            TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        }
        private val justifiedDecoPaintLocal = ThreadLocal.withInitial {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.BUTT
            }
        }
        private val monospaceBoldTypeface: Typeface by lazy {
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        fun drawTextSlice(
            canvas: Canvas,
            slice: PageElement.TextSlice,
            contentWidthPx: Int,
            xOffset: Float,
            bodyPaint: TextPaint,
            titlePaint: TextPaint,
            style: ReadingStyle,
            yShift: Float = 0f,
        ) {
            if (slice.text.isEmpty() || contentWidthPx <= 0) return
            val paint = if (slice.isTitle) titlePaint else bodyPaint
            val insetPx = if (slice.isQuote) slice.quoteInsetPx else 0f
            val effectiveWidth = (contentWidthPx - insetPx.roundToInt()).coerceAtLeast(64)
            val alignment = resolveLayoutAlignment(slice.align)
            val sliceTop = slice.yOffset + yShift

            if (slice.isQuote && insetPx > 2f) {
                val barPaint = quoteBarPaintLocal.get()!!.apply {
                    color = applyAlpha(paint.color, 0.24f)
                }
                val barWidth = (insetPx * 0.20f).coerceIn(2f, 5f)
                val barLeft = xOffset + 1f
                val barRect = quoteBarRectLocal.get()!!.apply {
                    set(
                        barLeft,
                        sliceTop + 2f,
                        barLeft + barWidth,
                        (sliceTop + slice.height - 2f).coerceAtLeast(sliceTop + 4f),
                    )
                }
                canvas.drawRoundRect(barRect, barWidth * 0.5f, barWidth * 0.5f, barPaint)
            }

            val textX = xOffset + insetPx
            val master = slice.masterLayout
            if (master != null && slice.cachedWidthPx == effectiveWidth) {
                val justifiedData = slice.justifiedData
                if (justifiedData != null && justifiedData.lines.size == (slice.endLine - slice.startLine + 1)) {
                    drawSliceWithJustification(
                        canvas = canvas,
                        master = master,
                        slice = slice,
                        justifiedData = justifiedData,
                        paint = paint,
                        effectiveWidth = effectiveWidth,
                        textX = textX,
                        sliceTop = sliceTop,
                    )
                } else {
                    master.paint.color = paint.color
                    val topClip = if (slice.startLine <= 0) sliceTop - 4f else sliceTop
                    val bottomClip = if (slice.endLine >= master.lineCount - 1) {
                        sliceTop + slice.height + 4f
                    } else {
                        sliceTop + slice.height
                    }
                    canvas.save()
                    canvas.clipRect(
                        textX - 8f,
                        topClip,
                        textX + effectiveWidth + 8f,
                        bottomClip,
                    )
                    canvas.translate(textX, sliceTop - slice.lineTopPx.toFloat())
                    master.draw(canvas)
                    canvas.restore()
                }
            } else {
                val fallbackLayout = buildStaticLayout(
                    text = slice.text,
                    paint = paint,
                    widthPx = effectiveWidth,
                    lineHeightMultiplier = style.lineHeight,
                    isTitle = slice.isTitle,
                    alignment = alignment,
                )
                canvas.save()
                canvas.translate(textX, sliceTop)
                fallbackLayout.draw(canvas)
                canvas.restore()
            }
        }

        private fun drawSliceWithJustification(
            canvas: Canvas,
            master: StaticLayout,
            slice: PageElement.TextSlice,
            justifiedData: JustifiedSliceData,
            paint: TextPaint,
            effectiveWidth: Int,
            textX: Float,
            sliceTop: Float,
        ) {
            val scratchPaint = justifiedScratchPaintLocal.get()!!
            scratchPaint.set(paint)
            scratchPaint.isUnderlineText = false
            scratchPaint.isStrikeThruText = false
            var activeStyleBits = -1
            var activeAlpha = -1f

            val lines = justifiedData.lines
            val count = lines.size
            var i = 0
            while (i < count) {
                val lineData = lines[i]
                if (lineData != null) {
                    val baselineY = sliceTop + lineData.baselineOffsetPx
                    val runs = lineData.runs
                    for (r in 0 until runs.size) {
                        val run = runs[r]
                        if (run.styleBits != activeStyleBits) {
                            activeStyleBits = run.styleBits
                            scratchPaint.typeface = when {
                                (activeStyleBits and STYLE_CODE) != 0 -> {
                                    if ((activeStyleBits and STYLE_BOLD) != 0) {
                                        monospaceBoldTypeface
                                    } else {
                                        Typeface.MONOSPACE
                                    }
                                }
                                (activeStyleBits and STYLE_BOLD) != 0 ->
                                    justifiedData.boldTypeface ?: paint.typeface
                                else -> paint.typeface
                            }
                            scratchPaint.textSize = if ((activeStyleBits and STYLE_CODE) != 0) {
                                paint.textSize * 0.92f
                            } else {
                                paint.textSize
                            }
                            scratchPaint.textSkewX = if ((activeStyleBits and STYLE_ITALIC) != 0) {
                                -0.18f
                            } else {
                                paint.textSkewX
                            }
                        }
                        if (run.alphaFactor != activeAlpha) {
                            activeAlpha = run.alphaFactor
                            scratchPaint.color = if (activeAlpha < 0.999f) {
                                applyAlpha(paint.color, activeAlpha)
                            } else {
                                paint.color
                            }
                        }
                        canvas.drawText(run.text, textX + run.x, baselineY, scratchPaint)
                    }

                    val decos = lineData.decorations
                    if (decos != null && decos.isNotEmpty()) {
                        val decoPaint = justifiedDecoPaintLocal.get()!!
                        decoPaint.strokeWidth = (paint.textSize * 0.055f).coerceAtLeast(1.2f)
                        val underlineY = baselineY + paint.textSize * 0.13f
                        val strikeY = baselineY - paint.textSize * 0.29f
                        for (d in 0 until decos.size) {
                            val seg = decos[d]
                            decoPaint.color = if (seg.alphaFactor < 0.999f) {
                                applyAlpha(paint.color, seg.alphaFactor)
                            } else {
                                paint.color
                            }
                            if (seg.isUnderline) {
                                canvas.drawLine(
                                    textX + seg.startX,
                                    underlineY,
                                    textX + seg.endX,
                                    underlineY,
                                    decoPaint,
                                )
                            }
                            if (seg.isStrikethrough) {
                                canvas.drawLine(
                                    textX + seg.startX,
                                    strikeY,
                                    textX + seg.endX,
                                    strikeY,
                                    decoPaint,
                                )
                            }
                        }
                    }
                    i++
                } else {
                    val runStartOffset = i
                    var runEndOffset = i
                    while (runEndOffset + 1 < count && lines[runEndOffset + 1] == null) {
                        runEndOffset++
                    }
                    val rStart = slice.startLine + runStartOffset
                    val rEnd = slice.startLine + runEndOffset
                    val topClip = if (rStart <= 0) {
                        sliceTop - 4f
                    } else {
                        sliceTop + (master.getLineTop(rStart) - slice.lineTopPx).toFloat()
                    }
                    val bottomClip = if (rEnd >= master.lineCount - 1) {
                        sliceTop + slice.height + 4f
                    } else {
                        sliceTop + (master.getLineBottom(rEnd) - slice.lineTopPx).toFloat()
                    }
                    master.paint.color = paint.color
                    canvas.save()
                    canvas.clipRect(
                        textX - 8f,
                        topClip,
                        textX + effectiveWidth + 8f,
                        bottomClip,
                    )
                    canvas.translate(textX, sliceTop - slice.lineTopPx.toFloat())
                    master.draw(canvas)
                    canvas.restore()
                    i = runEndOffset + 1
                }
            }
        }

        private fun resolveLayoutAlignment(align: String?): Layout.Alignment {
            return when (align?.lowercase()) {
                "center" -> Layout.Alignment.ALIGN_CENTER
                "right" -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_NORMAL
            }
        }

        fun findPageIndexForCharOffset(
            pages: List<ComposedPage>,
            charOffset: Int,
        ): Int {
            if (pages.isEmpty() || charOffset <= 0) return 0
            val idx = pages.indexOfLast { it.startCharOffset <= charOffset }
            return if (idx >= 0) idx.coerceIn(0, pages.lastIndex) else 0
        }

        fun paginateChapter(
            context: Context,
            chapterIndex: Int,
            chapterTitle: String,
            blocks: List<ReaderBlockItem>,
            style: ReadingStyle,
            contentWidthPx: Int,
            contentHeightPx: Int,
            density: Float,
            imageAspectRatios: Map<String, Float> = emptyMap(),
            textColorArgb: Int = android.graphics.Color.WHITE,
        ): List<ComposedPage> {
            val safeWidth = contentWidthPx.coerceAtLeast(120)
            val safeHeight = contentHeightPx.coerceAtLeast(180)
            val cleanChapterTitle = chapterTitle.trim().ifEmpty { "第 ${chapterIndex + 1} 章" }

            val bodyPaint = createBodyPaint(context, style, density, textColorArgb)
            val titlePaint = createTitlePaint(context, style, density, textColorArgb)
            val boldBodyTypeface = resolveTypeface(
                context = context,
                customFontPath = style.fontPath,
                weight = (style.fontWeightValue + 260).coerceIn(600, 900),
            )
            val paginationScratch = PaginationScratch(bodyPaint, boldBodyTypeface)
            val paragraphSpacingPx = (style.paragraphSpacing.coerceIn(0f, 30f) * density)
                .coerceAtLeast(0f)
            val titleBottomSpacingPx = max(paragraphSpacingPx * 1.45f, 12f * density)
            val defaultQuoteInsetPx = 14f * density

            val expandedBlocks = normalizeAndExpandBlocks(cleanChapterTitle, blocks)
            if (expandedBlocks.isEmpty()) {
                val fallbackLayout = buildStaticLayout(
                    text = cleanChapterTitle,
                    paint = titlePaint,
                    widthPx = safeWidth,
                    lineHeightMultiplier = style.lineHeight,
                    isTitle = true,
                )
                val fallbackHeight = fallbackLayout.height.toFloat().coerceAtLeast(titlePaint.textSize * 1.4f)
                val fallbackTitle = PageElement.TextSlice(
                    text = cleanChapterTitle,
                    isTitle = true,
                    yOffset = 0f,
                    height = fallbackHeight,
                    masterLayout = fallbackLayout,
                    startLine = 0,
                    endLine = (fallbackLayout.lineCount - 1).coerceAtLeast(0),
                    lineTopPx = 0,
                    lineBottomPx = fallbackLayout.height,
                    cachedWidthPx = safeWidth,
                    cachedColorArgb = textColorArgb,
                )
                return listOf(
                    ComposedPage(
                        chapterIndex = chapterIndex,
                        chapterTitle = cleanChapterTitle,
                        pageIndex = 0,
                        totalPagesInChapter = 1,
                        elements = listOf(fallbackTitle),
                        plainSnippet = cleanChapterTitle,
                    )
                )
            }

            val rawPages = mutableListOf<Triple<List<PageElement>, Int, Int>>()
            var currentElements = mutableListOf<PageElement>()
            var currentY = 0f
            var pageMinOffset = Int.MAX_VALUE
            var pageMaxOffset = 0
            var chapterLogicalOffset = 0

            fun recordPageSpan(startOffset: Int, endOffset: Int) {
                if (startOffset < pageMinOffset) pageMinOffset = startOffset
                if (endOffset > pageMaxOffset) pageMaxOffset = endOffset
            }

            fun flushCurrentPage() {
                if (currentElements.isNotEmpty()) {
                    val safeStart = if (pageMinOffset == Int.MAX_VALUE) 0 else pageMinOffset
                    val safeEnd = max(safeStart, pageMaxOffset)
                    rawPages.add(Triple(currentElements.toList(), safeStart, safeEnd))
                    currentElements = mutableListOf()
                    currentY = 0f
                    pageMinOffset = Int.MAX_VALUE
                    pageMaxOffset = 0
                }
            }

            for (block in expandedBlocks) {
                val kindLower = block.kind.trim().lowercase()
                val isImageBlock = kindLower == "image" ||
                    (!block.imageSrc.isNullOrBlank() && block.text.isBlank())

                if (isImageBlock) {
                    val src = (block.imageSrc ?: block.text).trim()
                    if (src.isEmpty()) continue

                    val imgStartOffset = chapterLogicalOffset
                    val imgEndOffset = imgStartOffset + 1
                    chapterLogicalOffset = imgEndOffset

                    val aspect = resolveImageAspectRatio(src, imageAspectRatios)
                    val desiredHeight = (safeWidth.toFloat() / aspect)
                        .coerceIn(safeHeight * 0.18f, safeHeight * 0.96f)

                    val remaining = safeHeight - currentY
                    if (currentElements.isNotEmpty() && desiredHeight > remaining) {
                        if (remaining >= max(desiredHeight * 0.68f, 96f * density)) {
                            val fittedHeight = remaining.coerceAtLeast(72f * density)
                            currentElements.add(
                                PageElement.ImageSlice(
                                    imageSrc = src,
                                    yOffset = currentY,
                                    height = fittedHeight,
                                    startCharOffset = imgStartOffset,
                                    endCharOffset = imgEndOffset,
                                )
                            )
                            recordPageSpan(imgStartOffset, imgEndOffset)
                            flushCurrentPage()
                            continue
                        } else {
                            flushCurrentPage()
                        }
                    }

                    val finalHeight = min(desiredHeight, safeHeight.toFloat())
                    val centeredY = if (currentElements.isEmpty() && finalHeight >= safeHeight * 0.60f) {
                        ((safeHeight - finalHeight) * 0.5f).coerceAtLeast(0f)
                    } else {
                        currentY
                    }
                    currentElements.add(
                        PageElement.ImageSlice(
                            imageSrc = src,
                            yOffset = centeredY,
                            height = finalHeight,
                            startCharOffset = imgStartOffset,
                            endCharOffset = imgEndOffset,
                        )
                    )
                    recordPageSpan(imgStartOffset, imgEndOffset)
                    currentY = centeredY + finalHeight + paragraphSpacingPx
                    if (currentY >= safeHeight - 4f || centeredY > 0f) {
                        flushCurrentPage()
                    }
                    continue
                }

                val isTitle = kindLower == "title" ||
                    kindLower == "heading" ||
                    kindLower == "h1" ||
                    kindLower == "h2" ||
                    kindLower == "h3" ||
                    kindLower == "subtitle"
                val isQuote = kindLower == "quote"
                val blockAlign = block.align
                val layoutAlignment = resolveLayoutAlignment(blockAlign)
                val quoteInsetPx = if (isQuote) defaultQuoteInsetPx else 0f
                val layoutWidthPx = (safeWidth - quoteInsetPx.roundToInt()).coerceAtLeast(64)

                val normalizedBlockText = block.text
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                val hasSingleLine = !normalizedBlockText.contains('\n')
                val rawParagraphs = if (hasSingleLine) listOf(normalizedBlockText) else normalizedBlockText.split('\n')

                var paraOffsetInBlock = 0
                for (rawPara in rawParagraphs) {
                    val paraStartInBlock = paraOffsetInBlock
                    val paraEndInBlock = paraOffsetInBlock + rawPara.length
                    paraOffsetInBlock = paraEndInBlock + 1

                    var leadTrim = 0
                    while (leadTrim < rawPara.length && isBlankOrFullwidthSpace(rawPara[leadTrim])) {
                        leadTrim++
                    }
                    var endTrim = rawPara.length
                    while (endTrim > leadTrim && isBlankOrFullwidthSpace(rawPara[endTrim - 1])) {
                        endTrim--
                    }
                    if (endTrim <= leadTrim) continue
                    val trimmed = rawPara.substring(leadTrim, endTrim)

                    val shouldIndent = !isTitle &&
                        style.firstLineIndent &&
                        layoutAlignment == Layout.Alignment.ALIGN_NORMAL
                    val indentPrefixLen = if (shouldIndent) FULLWIDTH_INDENT.length else 0
                    val baseText = if (shouldIndent) "$FULLWIDTH_INDENT$trimmed" else trimmed

                    // Map block-level inline spans into this paragraph's baseText coordinates
                    val localSpans = if (block.inlineSpans.isEmpty()) {
                        emptyList()
                    } else {
                        val trimmedLen = trimmed.length
                        block.inlineSpans.mapNotNull { sp ->
                            val relStart = (sp.start - paraStartInBlock - leadTrim).coerceIn(0, trimmedLen)
                            val relEnd = (sp.end - paraStartInBlock - leadTrim).coerceIn(0, trimmedLen)
                            if (relEnd > relStart) {
                                sp.copy(
                                    start = relStart + indentPrefixLen,
                                    end = relEnd + indentPrefixLen,
                                )
                            } else {
                                null
                            }
                        }
                    }

                    val (kinsokuText, kinsokuSpans) = applyCjkKinsokuWithSpans(baseText, localSpans)
                    val logicalOffsetAtKinsokuIdx = IntArray(kinsokuText.length + 1)
                    run {
                        var logicalCount = 0
                        var nonJoinerCount = 0
                        for (k in 0 until kinsokuText.length) {
                            logicalOffsetAtKinsokuIdx[k] = logicalCount
                            val ch = kinsokuText[k]
                            if (ch != WORD_JOINER) {
                                if (nonJoinerCount >= indentPrefixLen) {
                                    logicalCount++
                                }
                                nonJoinerCount++
                            }
                        }
                        logicalOffsetAtKinsokuIdx[kinsokuText.length] = logicalCount
                    }
                    val paraBaseOffset = chapterLogicalOffset
                    chapterLogicalOffset += trimmed.length + 1

                    val kernAdjustments = computeCjkKernAdjustments(kinsokuText)
                    val dimJapanese = !isTitle && style.dimJapaneseText && isLikelyJapaneseParagraph(trimmed)
                    val quoteDimAlpha = if (isQuote && !dimJapanese) 0.88f else 1f

                    val formattedText: CharSequence = if (
                        kinsokuSpans.isEmpty() &&
                        kernAdjustments.isEmpty() &&
                        !dimJapanese &&
                        quoteDimAlpha >= 0.999f
                    ) {
                        kinsokuText
                    } else {
                        SpannableString(kinsokuText).apply {
                            if (dimJapanese) {
                                setSpan(
                                    ForegroundColorSpan(applyAlpha(textColorArgb, 0.38f)),
                                    0,
                                    length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            } else if (quoteDimAlpha < 0.999f) {
                                setSpan(
                                    ForegroundColorSpan(applyAlpha(textColorArgb, quoteDimAlpha)),
                                    0,
                                    length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                )
                            }

                            for (sp in kinsokuSpans) {
                                val s = sp.start.coerceIn(0, length)
                                val e = sp.end.coerceIn(0, length)
                                if (e <= s) continue
                                when (sp.kind) {
                                    InlineStyleKind.BOLD -> setSpan(
                                        CustomTypefaceStyleSpan(customTypeface = boldBodyTypeface),
                                        s,
                                        e,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                    )
                                    InlineStyleKind.ITALIC -> setSpan(
                                        CustomTypefaceStyleSpan(customTypeface = null, italicSkewX = -0.18f),
                                        s,
                                        e,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                    )
                                    InlineStyleKind.UNDERLINE -> setSpan(
                                        UnderlineSpan(),
                                        s,
                                        e,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                    )
                                    InlineStyleKind.STRIKETHROUGH -> setSpan(
                                        StrikethroughSpan(),
                                        s,
                                        e,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                    )
                                    InlineStyleKind.CODE -> {
                                        setSpan(
                                            TypefaceSpan("monospace"),
                                            s,
                                            e,
                                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                        )
                                        setSpan(
                                            RelativeSizeSpan(0.92f),
                                            s,
                                            e,
                                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                        )
                                    }
                                }
                            }

                            for (adj in kernAdjustments) {
                                if (adj.index in 0 until length) {
                                    setSpan(
                                        CjkPunctuationKernSpan(
                                            advanceEm = adj.advanceEm,
                                            drawShiftEm = adj.drawShiftEm,
                                        ),
                                        adj.index,
                                        adj.index + 1,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                                    )
                                }
                            }
                        }
                    }

                    val paint = if (isTitle) titlePaint else bodyPaint
                    val layout = buildStaticLayout(
                        text = formattedText,
                        paint = paint,
                        widthPx = layoutWidthPx,
                        lineHeightMultiplier = style.lineHeight,
                        isTitle = isTitle,
                        alignment = layoutAlignment,
                    )

                    val totalLines = layout.lineCount
                    if (totalLines <= 0) continue

                    val alphaFactor = when {
                        dimJapanese -> 0.38f
                        quoteDimAlpha < 0.999f -> quoteDimAlpha
                        else -> 1f
                    }
                    val canJustifyParagraph = !isTitle &&
                        layoutAlignment == Layout.Alignment.ALIGN_NORMAL &&
                        totalLines > 1 &&
                        containsCjkOrFullwidth(kinsokuText)
                    val kernByIndex = if (canJustifyParagraph && kernAdjustments.isNotEmpty()) {
                        HashMap<Int, CjkKernAdjustment>(kernAdjustments.size * 2).apply {
                            for (adj in kernAdjustments) {
                                put(adj.index, adj)
                            }
                        }
                    } else {
                        null
                    }

                    // Keep titles paired with at least one body line on the same page
                    if (isTitle && currentElements.isNotEmpty()) {
                        val minTitleSpace = layout.height + (bodyPaint.textSize * style.lineHeight)
                        if (currentY + min(minTitleSpace, safeHeight * 0.45f) > safeHeight) {
                            flushCurrentPage()
                        }
                    }

                    var startLine = 0
                    while (startLine < totalLines) {
                        val availableHeight = safeHeight - currentY
                        val firstLineHeight = (layout.getLineBottom(startLine) - layout.getLineTop(startLine)).toFloat()

                        if (firstLineHeight > availableHeight && currentElements.isNotEmpty()) {
                            flushCurrentPage()
                            continue
                        }

                        var endLine = startLine
                        while (endLine + 1 < totalLines) {
                            val nextHeight = (layout.getLineBottom(endLine + 1) - layout.getLineTop(startLine)).toFloat()
                            if (nextHeight <= (safeHeight - currentY) + 0.5f) {
                                endLine++
                            } else {
                                break
                            }
                        }

                        val charStart = layout.getLineStart(startLine)
                        val charEnd = layout.getLineEnd(endLine)
                        val sliceStartOffset = paraBaseOffset + logicalOffsetAtKinsokuIdx[charStart.coerceIn(0, kinsokuText.length)]
                        val sliceEndOffset = max(
                            sliceStartOffset,
                            paraBaseOffset + logicalOffsetAtKinsokuIdx[charEnd.coerceIn(0, kinsokuText.length)],
                        )
                        val sliceSeq = trimTrailingLineBreaks(
                            formattedText.subSequence(charStart, charEnd)
                        )
                        val lineTopPx = layout.getLineTop(startLine)
                        val lineBottomPx = layout.getLineBottom(endLine)
                        val sliceHeight = (lineBottomPx - lineTopPx).toFloat().coerceAtLeast(1f)

                        if (sliceSeq.isNotEmpty()) {
                            val justifiedData = if (canJustifyParagraph) {
                                buildJustifiedSliceData(
                                    kinsokuText = kinsokuText,
                                    layout = layout,
                                    startLine = startLine,
                                    endLine = endLine,
                                    lineTopPx = lineTopPx,
                                    effectiveWidth = layoutWidthPx,
                                    basePaint = bodyPaint,
                                    boldBodyTypeface = boldBodyTypeface,
                                    kinsokuSpans = kinsokuSpans,
                                    kernByIndex = kernByIndex,
                                    alphaFactor = alphaFactor,
                                    hasLeadingIndent = shouldIndent,
                                    scratch = paginationScratch,
                                )
                            } else {
                                null
                            }
                            currentElements.add(
                                PageElement.TextSlice(
                                    text = sliceSeq,
                                    isTitle = isTitle,
                                    yOffset = currentY,
                                    height = sliceHeight,
                                    masterLayout = layout,
                                    startLine = startLine,
                                    endLine = endLine,
                                    lineTopPx = lineTopPx,
                                    lineBottomPx = lineBottomPx,
                                    isQuote = isQuote,
                                    align = blockAlign,
                                    quoteInsetPx = quoteInsetPx,
                                    cachedWidthPx = layoutWidthPx,
                                    cachedColorArgb = paint.color,
                                    startCharOffset = sliceStartOffset,
                                    endCharOffset = sliceEndOffset,
                                    justifiedData = justifiedData,
                                )
                            )
                            recordPageSpan(sliceStartOffset, sliceEndOffset)
                        }

                        currentY += sliceHeight
                        val reachedParagraphEnd = (endLine == totalLines - 1)
                        if (reachedParagraphEnd) {
                            currentY += if (isTitle) titleBottomSpacingPx else paragraphSpacingPx
                        } else {
                            flushCurrentPage()
                        }
                        startLine = endLine + 1
                    }
                }

                // If block also carried an inline image alongside text
                if (!block.imageSrc.isNullOrBlank() && block.text.isNotBlank()) {
                    val src = block.imageSrc.trim()
                    val imgStartOffset = chapterLogicalOffset
                    val imgEndOffset = imgStartOffset + 1
                    chapterLogicalOffset = imgEndOffset
                    val aspect = resolveImageAspectRatio(src, imageAspectRatios)
                    val desiredHeight = (safeWidth.toFloat() / aspect)
                        .coerceIn(safeHeight * 0.18f, safeHeight * 0.86f)
                    if (currentElements.isNotEmpty() && currentY + desiredHeight > safeHeight) {
                        flushCurrentPage()
                    }
                    val finalHeight = min(desiredHeight, safeHeight.toFloat())
                    currentElements.add(
                        PageElement.ImageSlice(
                            imageSrc = src,
                            yOffset = currentY,
                            height = finalHeight,
                            startCharOffset = imgStartOffset,
                            endCharOffset = imgEndOffset,
                        )
                    )
                    recordPageSpan(imgStartOffset, imgEndOffset)
                    currentY += finalHeight + paragraphSpacingPx
                }
            }

            flushCurrentPage()

            if (rawPages.isEmpty()) {
                val fallbackLayout = buildStaticLayout(
                    text = cleanChapterTitle,
                    paint = titlePaint,
                    widthPx = safeWidth,
                    lineHeightMultiplier = style.lineHeight,
                    isTitle = true,
                )
                rawPages.add(
                    Triple(
                        listOf(
                            PageElement.TextSlice(
                                text = cleanChapterTitle,
                                isTitle = true,
                                yOffset = 0f,
                                height = fallbackLayout.height.toFloat().coerceAtLeast(titlePaint.textSize * 1.4f),
                                masterLayout = fallbackLayout,
                                startLine = 0,
                                endLine = (fallbackLayout.lineCount - 1).coerceAtLeast(0),
                                lineTopPx = 0,
                                lineBottomPx = fallbackLayout.height,
                                cachedWidthPx = safeWidth,
                                cachedColorArgb = textColorArgb,
                            )
                        ),
                        0,
                        cleanChapterTitle.length,
                    )
                )
            }

            val totalPages = rawPages.size
            return rawPages.mapIndexed { idx, (elements, pageStart, pageEnd) ->
                ComposedPage(
                    chapterIndex = chapterIndex,
                    chapterTitle = cleanChapterTitle,
                    pageIndex = idx,
                    totalPagesInChapter = totalPages,
                    elements = elements,
                    plainSnippet = buildPageSnippet(elements, cleanChapterTitle),
                    startCharOffset = pageStart,
                    endCharOffset = pageEnd,
                )
            }
        }

        private fun normalizeAndExpandBlocks(
            chapterTitle: String,
            blocks: List<ReaderBlockItem>,
        ): List<ReaderBlockItem> {
            val out = mutableListOf<ReaderBlockItem>()
            val cleanTitle = chapterTitle.trim()
            val normTitle = cleanTitle.replace(Regex("""[\s\u3000()（）\[\]【】]+"""), "")

            val isImageOnlyChapter = blocks.isNotEmpty() && blocks.all {
                it.kind.equals("image", ignoreCase = true) || (!it.imageSrc.isNullOrBlank() && it.text.isBlank())
            }

            if (cleanTitle.isNotBlank() && !isImageOnlyChapter) {
                out.add(ReaderBlockItem(kind = "title", text = cleanTitle))
            }

            var skippedDuplicateTitle = false
            for ((idx, block) in blocks.withIndex()) {
                val text = block.text.trim()
                val normText = text.replace(Regex("""[\s\u3000()（）\[\]【】]+"""), "")
                if (!skippedDuplicateTitle && idx < 4 && normTitle.isNotEmpty() && normText == normTitle && block.imageSrc.isNullOrBlank()) {
                    skippedDuplicateTitle = true
                    continue
                }
                if (block.kind.equals("image", ignoreCase = true) || !block.imageSrc.isNullOrBlank()) {
                    out.add(block)
                    continue
                }
                val matches = inlineImgTagRegex.findAll(text).toList()
                if (matches.isEmpty()) {
                    appendBlockSplitIfHuge(block, out)
                } else {
                    var cursor = 0
                    for (match in matches) {
                        if (match.range.first > cursor) {
                            val before = text.substring(cursor, match.range.first).trim()
                            if (before.isNotEmpty()) {
                                out.add(ReaderBlockItem(kind = block.kind, text = before, align = block.align))
                            }
                        }
                        val src = match.groupValues.getOrNull(1)?.trim().orEmpty()
                        if (src.isNotEmpty()) {
                            out.add(ReaderBlockItem(kind = "image", text = "", imageSrc = src, align = block.align))
                        }
                        cursor = match.range.last + 1
                    }
                    if (cursor < text.length) {
                        val after = text.substring(cursor).trim()
                        if (after.isNotEmpty()) {
                            out.add(ReaderBlockItem(kind = block.kind, text = after, align = block.align))
                        }
                    }
                }
            }
            return out
        }

        private fun appendBlockSplitIfHuge(
            block: ReaderBlockItem,
            out: MutableList<ReaderBlockItem>,
        ) {
            val text = block.text
            if (text.length <= 2600 || !block.imageSrc.isNullOrBlank()) {
                out.add(block)
                return
            }
            var start = 0
            val n = text.length
            while (start < n) {
                val remaining = n - start
                if (remaining <= 2600) {
                    val tail = text.substring(start)
                    if (tail.isNotBlank()) {
                        out.add(sliceBlock(block, start, n))
                    }
                    break
                }
                val minSplit = start + 1000
                val maxSplit = min(start + 2200, n - 1)
                var splitAt = -1
                for (i in maxSplit downTo minSplit) {
                    val ch = text[i]
                    if (ch == '\n' || ch == '。' || ch == '！' || ch == '？' || ch == '；' ||
                        ch == '.' || ch == '!' || ch == '?' || ch == ';'
                    ) {
                        var endIdx = i + 1
                        while (endIdx < n && (text[endIdx] == '”' || text[endIdx] == '"' ||
                                text[endIdx] == '’' || text[endIdx] == '」' || text[endIdx] == '』' ||
                                text[endIdx] == '）' || text[endIdx] == ')')
                        ) {
                            endIdx++
                        }
                        splitAt = endIdx
                        break
                    }
                }
                if (splitAt <= start) {
                    for (i in maxSplit downTo minSplit) {
                        if (text[i].isWhitespace()) {
                            splitAt = i + 1
                            break
                        }
                    }
                }
                if (splitAt <= start) {
                    splitAt = min(start + 2000, n)
                }
                if (text.substring(start, splitAt).isNotBlank()) {
                    out.add(sliceBlock(block, start, splitAt))
                }
                start = splitAt
            }
        }

        private fun sliceBlock(
            block: ReaderBlockItem,
            start: Int,
            end: Int,
        ): ReaderBlockItem {
            val subText = block.text.substring(start, end)
            val subSpans = if (block.inlineSpans.isEmpty()) {
                emptyList()
            } else {
                block.inlineSpans.mapNotNull { sp ->
                    val s = (sp.start - start).coerceIn(0, subText.length)
                    val e = (sp.end - start).coerceIn(0, subText.length)
                    if (e > s) sp.copy(start = s, end = e) else null
                }
            }
            return block.copy(text = subText, inlineSpans = subSpans)
        }

        private fun resolveImageAspectRatio(
            imageSrc: String,
            overrides: Map<String, Float>,
        ): Float {
            overrides[imageSrc]?.let { return it.coerceIn(0.25f, 4.0f) }
            getCachedAspectRatio(imageSrc)?.let { return it.coerceIn(0.25f, 4.0f) }

            val decodedRatio: Float? = try {
                when {
                    imageSrc.startsWith("file://", ignoreCase = true) -> {
                        val path = UriOrFilePath(imageSrc.removePrefix("file://"))
                        decodeBoundsRatioFromFile(path)
                    }
                    imageSrc.startsWith("/") -> {
                        decodeBoundsRatioFromFile(imageSrc)
                    }
                    imageSrc.startsWith("data:image/", ignoreCase = true) -> {
                        val commaIdx = imageSrc.indexOf(',')
                        if (commaIdx > 0) {
                            val base64Part = imageSrc.substring(commaIdx + 1)
                            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                            if (opts.outWidth > 0 && opts.outHeight > 0) {
                                opts.outWidth.toFloat() / opts.outHeight.toFloat()
                            } else null
                        } else null
                    }
                    else -> null
                }
            } catch (_: Throwable) {
                null
            }

            if (decodedRatio != null && decodedRatio > 0f) {
                val clamped = decodedRatio.coerceIn(0.25f, 4.0f)
                synchronized(aspectRatioCache) {
                    aspectRatioCache.put(imageSrc, clamped)
                }
                return clamped
            }
            return 0.75f
        }

        private fun UriOrFilePath(raw: String): String {
            return try {
                java.net.URLDecoder.decode(raw, "UTF-8")
            } catch (_: Throwable) {
                raw
            }
        }

        private fun decodeBoundsRatioFromFile(path: String): Float? {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            return if (opts.outWidth > 0 && opts.outHeight > 0) {
                opts.outWidth.toFloat() / opts.outHeight.toFloat()
            } else {
                null
            }
        }

        private fun buildPageSnippet(
            elements: List<PageElement>,
            fallbackTitle: String,
        ): String {
            val sb = StringBuilder()
            for (element in elements) {
                if (element is PageElement.TextSlice) {
                    val cleaned = trimLeadingTrailingBlank(
                        element.text.toString().replace(WORD_JOINER.toString(), "")
                    ).replace(Regex("\\s+"), " ")
                    if (cleaned.isNotEmpty()) {
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(cleaned)
                        if (sb.length >= 96) break
                    }
                }
            }
            val result = sb.toString().trim()
            return if (result.isEmpty()) {
                fallbackTitle
            } else if (result.length > 90) {
                result.substring(0, 90) + "…"
            } else {
                result
            }
        }

        private fun trimLeadingTrailingBlank(input: String): String {
            var start = 0
            var end = input.length
            while (start < end && isBlankOrFullwidthSpace(input[start])) {
                start++
            }
            while (end > start && isBlankOrFullwidthSpace(input[end - 1])) {
                end--
            }
            return if (start == 0 && end == input.length) input else input.substring(start, end)
        }

        private fun trimTrailingLineBreaks(seq: CharSequence): CharSequence {
            var end = seq.length
            while (end > 0 && (seq[end - 1] == '\n' || seq[end - 1] == '\r')) {
                end--
            }
            return if (end == seq.length) seq else seq.subSequence(0, end)
        }

        private fun isBlankOrFullwidthSpace(ch: Char): Boolean {
            return ch.isWhitespace() || ch == '\u3000' || ch == '\u00A0' || ch == '\uFEFF'
        }

        private fun applyAlpha(colorArgb: Int, factor: Float): Int {
            val origAlpha = (colorArgb ushr 24) and 0xFF
            val newAlpha = (origAlpha * factor).roundToInt().coerceIn(0, 255)
            return (colorArgb and 0x00FFFFFF) or (newAlpha shl 24)
        }

        private class PaginationScratch(
            private val baseBodyPaint: TextPaint,
            private val boldTypeface: Typeface,
        ) {
            val styledPaint = TextPaint(baseBodyPaint)
            val noLsPaint = TextPaint(baseBodyPaint).apply { letterSpacing = 0f }
            val bounds = Rect()

            fun prepareStyledPaint(styleBits: Int) {
                styledPaint.set(baseBodyPaint)
                if ((styleBits and STYLE_CODE) != 0) {
                    styledPaint.typeface = if ((styleBits and STYLE_BOLD) != 0) {
                        monospaceBoldTypeface
                    } else {
                        Typeface.MONOSPACE
                    }
                    styledPaint.textSize = baseBodyPaint.textSize * 0.92f
                } else if ((styleBits and STYLE_BOLD) != 0) {
                    styledPaint.typeface = boldTypeface
                }
                if ((styleBits and STYLE_ITALIC) != 0) {
                    styledPaint.textSkewX = -0.18f
                }
            }

            fun prepareNoLsPaint(styleBits: Int) {
                prepareStyledPaint(styleBits)
                noLsPaint.set(styledPaint)
                noLsPaint.letterSpacing = 0f
            }
        }

        private class JustifyToken(
            val text: String,
            val advanceWidth: Float,
            val drawShiftPx: Float,
            val styleBits: Int,
            val isSpace: Boolean,
            val isLatinWord: Boolean,
            val hasKern: Boolean,
            val firstCodePoint: Int,
            val lastCodePoint: Int,
        )

        private fun buildJustifiedSliceData(
            kinsokuText: String,
            layout: StaticLayout,
            startLine: Int,
            endLine: Int,
            lineTopPx: Int,
            effectiveWidth: Int,
            basePaint: TextPaint,
            boldBodyTypeface: Typeface,
            kinsokuSpans: List<InlineStyleSpan>,
            kernByIndex: Map<Int, CjkKernAdjustment>?,
            alphaFactor: Float,
            hasLeadingIndent: Boolean,
            scratch: PaginationScratch,
        ): JustifiedSliceData? {
            val lineCount = endLine - startLine + 1
            if (lineCount <= 0) return null
            var anyJustified = false
            val resultLines = ArrayList<JustifiedLineData?>(lineCount)
            for (lineIdx in startLine..endLine) {
                val lineData = buildJustifiedLineData(
                    kinsokuText = kinsokuText,
                    layout = layout,
                    lineIdx = lineIdx,
                    lineTopPx = lineTopPx,
                    effectiveWidth = effectiveWidth,
                    basePaint = basePaint,
                    kinsokuSpans = kinsokuSpans,
                    kernByIndex = kernByIndex,
                    alphaFactor = alphaFactor,
                    hasLeadingIndent = hasLeadingIndent,
                    scratch = scratch,
                )
                if (lineData != null) anyJustified = true
                resultLines.add(lineData)
            }
            return if (anyJustified) {
                JustifiedSliceData(
                    lines = resultLines,
                    boldTypeface = boldBodyTypeface,
                )
            } else {
                null
            }
        }

        private fun buildJustifiedLineData(
            kinsokuText: String,
            layout: StaticLayout,
            lineIdx: Int,
            lineTopPx: Int,
            effectiveWidth: Int,
            basePaint: TextPaint,
            kinsokuSpans: List<InlineStyleSpan>,
            kernByIndex: Map<Int, CjkKernAdjustment>?,
            alphaFactor: Float,
            hasLeadingIndent: Boolean,
            scratch: PaginationScratch,
        ): JustifiedLineData? {
            if (lineIdx >= layout.lineCount - 1) return null
            val lineStart = layout.getLineStart(lineIdx)
            val lineEnd = layout.getLineEnd(lineIdx)
            if (lineEnd <= lineStart || lineEnd > kinsokuText.length) return null
            if (kinsokuText[lineEnd - 1] == '\n') return null

            var effectiveEnd = lineEnd
            while (effectiveEnd > lineStart) {
                val ch = kinsokuText[effectiveEnd - 1]
                if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == WORD_JOINER) {
                    effectiveEnd--
                } else {
                    break
                }
            }
            if (effectiveEnd <= lineStart) return null
            if (!containsCjkOrFullwidth(kinsokuText, lineStart, effectiveEnd)) return null

            var cursor = lineStart
            var indentPx = 0f
            if (lineIdx == 0 && hasLeadingIndent) {
                var consumedIndent = 0
                while (cursor < effectiveEnd && consumedIndent < FULLWIDTH_INDENT.length) {
                    val ch = kinsokuText[cursor]
                    if (ch == WORD_JOINER) {
                        cursor++
                    } else if (ch == '\u3000') {
                        consumedIndent++
                        cursor++
                    } else {
                        break
                    }
                }
                if (consumedIndent > 0) {
                    indentPx = basePaint.measureText(kinsokuText, lineStart, cursor)
                }
            }
            while (cursor < effectiveEnd && kinsokuText[cursor] == WORD_JOINER) {
                cursor++
            }
            if (cursor >= effectiveEnd) return null

            val tokens = ArrayList<JustifyToken>(effectiveEnd - cursor)
            var totalAdvance = 0f
            var anyUnderline = false
            var anyStrikethrough = false

            var i = cursor
            while (i < effectiveEnd) {
                val ch = kinsokuText[i]
                if (ch == WORD_JOINER) {
                    i++
                    continue
                }

                var styleBits = 0
                var nextBoundary = effectiveEnd
                if (kinsokuSpans.isNotEmpty()) {
                    for (sIdx in 0 until kinsokuSpans.size) {
                        val sp = kinsokuSpans[sIdx]
                        if (i in sp.start until sp.end) {
                            styleBits = styleBits or when (sp.kind) {
                                InlineStyleKind.BOLD -> STYLE_BOLD
                                InlineStyleKind.ITALIC -> STYLE_ITALIC
                                InlineStyleKind.UNDERLINE -> STYLE_UNDERLINE
                                InlineStyleKind.STRIKETHROUGH -> STYLE_STRIKETHROUGH
                                InlineStyleKind.CODE -> STYLE_CODE
                            }
                            if (sp.end < nextBoundary) nextBoundary = sp.end
                        } else if (sp.start in (i + 1) until nextBoundary) {
                            nextBoundary = sp.start
                        }
                    }
                }
                if ((styleBits and STYLE_UNDERLINE) != 0) anyUnderline = true
                if ((styleBits and STYLE_STRIKETHROUGH) != 0) anyStrikethrough = true
                scratch.prepareStyledPaint(styleBits)

                if (ch == ' ' || ch == '\t') {
                    val adv = scratch.styledPaint.measureText(" ")
                    totalAdvance += adv
                    tokens.add(
                        JustifyToken(
                            text = " ",
                            advanceWidth = adv,
                            drawShiftPx = 0f,
                            styleBits = styleBits,
                            isSpace = true,
                            isLatinWord = false,
                            hasKern = false,
                            firstCodePoint = ' '.code,
                            lastCodePoint = ' '.code,
                        )
                    )
                    i++
                    continue
                }

                val kernAdj = kernByIndex?.get(i)
                if (kernAdj != null) {
                    val clusterEnd = advanceGraphemeCluster(kinsokuText, i, nextBoundary)
                    val tokText = kinsokuText.substring(i, clusterEnd)
                    val adv = (scratch.styledPaint.textSize * kernAdj.advanceEm).roundToInt().coerceAtLeast(1).toFloat()
                    val shiftPx = scratch.styledPaint.textSize * kernAdj.drawShiftEm
                    totalAdvance += adv
                    tokens.add(
                        JustifyToken(
                            text = tokText,
                            advanceWidth = adv,
                            drawShiftPx = shiftPx,
                            styleBits = styleBits,
                            isSpace = false,
                            isLatinWord = false,
                            hasKern = true,
                            firstCodePoint = Character.codePointAt(tokText, 0),
                            lastCodePoint = Character.codePointBefore(tokText, tokText.length),
                        )
                    )
                    i = clusterEnd
                    continue
                }

                if (isLatinWordCoreChar(ch)) {
                    var wordEnd = i + 1
                    while (wordEnd < nextBoundary && kernByIndex?.get(wordEnd) == null) {
                        val wch = kinsokuText[wordEnd]
                        if (wch == WORD_JOINER) break
                        if (isLatinWordCoreChar(wch)) {
                            wordEnd++
                        } else if (
                            (wch == '-' || wch == '\'' || wch == '’' || wch == '.' || wch == '/') &&
                            wordEnd + 1 < nextBoundary &&
                            isLatinWordCoreChar(kinsokuText[wordEnd + 1])
                        ) {
                            wordEnd++
                        } else {
                            break
                        }
                    }
                    val tokText = kinsokuText.substring(i, wordEnd)
                    val adv = scratch.styledPaint.measureText(tokText)
                    totalAdvance += adv
                    tokens.add(
                        JustifyToken(
                            text = tokText,
                            advanceWidth = adv,
                            drawShiftPx = 0f,
                            styleBits = styleBits,
                            isSpace = false,
                            isLatinWord = true,
                            hasKern = false,
                            firstCodePoint = Character.codePointAt(tokText, 0),
                            lastCodePoint = Character.codePointBefore(tokText, tokText.length),
                        )
                    )
                    i = wordEnd
                    continue
                }

                val clusterEnd = advanceGraphemeCluster(kinsokuText, i, nextBoundary)
                val tokText = kinsokuText.substring(i, clusterEnd)
                val adv = scratch.styledPaint.measureText(tokText)
                totalAdvance += adv
                tokens.add(
                    JustifyToken(
                        text = tokText,
                        advanceWidth = adv,
                        drawShiftPx = 0f,
                        styleBits = styleBits,
                        isSpace = false,
                        isLatinWord = false,
                        hasKern = false,
                        firstCodePoint = Character.codePointAt(tokText, 0),
                        lastCodePoint = Character.codePointBefore(tokText, tokText.length),
                    )
                )
                i = clusterEnd
            }

            if (tokens.size < 2) return null

            var excessLeft = 0f
            val firstTok = tokens[0]
            if (!(lineIdx == 0 && indentPx > 0f) &&
                firstTok.drawShiftPx == 0f &&
                !firstTok.isSpace &&
                firstTok.text.length == 1 &&
                leftSparseCjkPunctuation.contains(firstTok.text[0])
            ) {
                scratch.prepareNoLsPaint(firstTok.styleBits)
                val rawAdv = scratch.noLsPaint.measureText(firstTok.text)
                scratch.noLsPaint.getTextBounds(firstTok.text, 0, firstTok.text.length, scratch.bounds)
                excessLeft = (scratch.bounds.left - scratch.noLsPaint.textSize * 0.06f)
                    .coerceIn(0f, rawAdv * 0.42f)
            }

            var excessRight = 0f
            val lastTok = tokens[tokens.lastIndex]
            if (lastTok.drawShiftPx == 0f &&
                !lastTok.hasKern &&
                !lastTok.isSpace &&
                lastTok.text.length == 1 &&
                rightSparseCjkPunctuation.contains(lastTok.text[0])
            ) {
                scratch.prepareNoLsPaint(lastTok.styleBits)
                val rawAdv = scratch.noLsPaint.measureText(lastTok.text)
                scratch.noLsPaint.getTextBounds(lastTok.text, 0, lastTok.text.length, scratch.bounds)
                if (scratch.bounds.right > 0) {
                    excessRight = (rawAdv - scratch.bounds.right - scratch.noLsPaint.textSize * 0.06f)
                        .coerceIn(0f, rawAdv * 0.42f)
                }
            }

            val naturalSpan = indentPx - excessLeft + totalAdvance
            val targetSpan = effectiveWidth.toFloat() + excessRight
            val slack = targetSpan - naturalSpan
            if (slack <= 0.5f || slack > effectiveWidth * 0.42f) return null

            val weights = FloatArray(tokens.size)
            var totalWeight = 0f
            for (k in 0 until tokens.size) {
                val curr = tokens[k]
                val w = when {
                    k == tokens.lastIndex -> 0f
                    curr.isSpace -> 1.5f
                    tokens[k + 1].isSpace -> 0f
                    curr.isLatinWord && tokens[k + 1].isLatinWord -> 0f
                    (curr.lastCodePoint == '—'.code && tokens[k + 1].firstCodePoint == '—'.code) ||
                        (curr.lastCodePoint == '…'.code && tokens[k + 1].firstCodePoint == '…'.code) -> 0f
                    curr.hasKern -> 0f
                    isTrailingAttachedCodePoint(tokens[k + 1].firstCodePoint) -> 0f
                    isLeadingAttachedCodePoint(curr.lastCodePoint) -> 0f
                    else -> 1.0f
                }
                weights[k] = w
                totalWeight += w
            }

            if (totalWeight <= 0.001f) {
                for (k in 0 until tokens.lastIndex) {
                    val curr = tokens[k]
                    if (curr.isSpace || tokens[k + 1].isSpace) continue
                    if (curr.isLatinWord && tokens[k + 1].isLatinWord) continue
                    if ((curr.lastCodePoint == '—'.code && tokens[k + 1].firstCodePoint == '—'.code) ||
                        (curr.lastCodePoint == '…'.code && tokens[k + 1].firstCodePoint == '…'.code)
                    ) {
                        continue
                    }
                    if (curr.hasKern) continue
                    weights[k] = 0.5f
                    totalWeight += 0.5f
                }
                if (totalWeight <= 0.001f) return null
            }

            val unitExtra = slack / totalWeight
            if (unitExtra > basePaint.textSize * 0.65f) return null

            val runs = ArrayList<JustifiedDrawRun>(tokens.size)
            var accText: StringBuilder? = null
            var accStartX = 0f
            var accStyleBits = 0
            var prevGap = 0f
            var prevHasKern = false

            fun flushAcc() {
                val sb = accText ?: return
                if (sb.isNotEmpty()) {
                    runs.add(
                        JustifiedDrawRun(
                            text = sb.toString(),
                            x = accStartX,
                            styleBits = accStyleBits,
                            alphaFactor = alphaFactor,
                        )
                    )
                }
                accText = null
            }

            var cursorX = indentPx - excessLeft
            for (k in 0 until tokens.size) {
                val tok = tokens[k]
                val gapAfter = weights[k] * unitExtra
                if (!tok.isSpace) {
                    val tokenX = cursorX + tok.drawShiftPx
                    val canMerge = accText != null &&
                        prevGap == 0f &&
                        !prevHasKern &&
                        !tok.hasKern &&
                        tok.drawShiftPx == 0f &&
                        tok.styleBits == accStyleBits &&
                        !(k == 1 && excessLeft > 0f) &&
                        k < tokens.lastIndex
                    if (canMerge) {
                        accText!!.append(tok.text)
                    } else {
                        flushAcc()
                        if (tok.hasKern || tok.drawShiftPx != 0f) {
                            runs.add(
                                JustifiedDrawRun(
                                    text = tok.text,
                                    x = tokenX,
                                    styleBits = tok.styleBits,
                                    alphaFactor = alphaFactor,
                                )
                            )
                        } else {
                            accText = StringBuilder(tok.text)
                            accStartX = tokenX
                            accStyleBits = tok.styleBits
                        }
                    }
                } else {
                    flushAcc()
                }
                prevGap = gapAfter
                prevHasKern = tok.hasKern
                cursorX += tok.advanceWidth + gapAfter
            }
            flushAcc()

            val decos = if (anyUnderline || anyStrikethrough) {
                buildLineDecorations(tokens, weights, unitExtra, indentPx - excessLeft, alphaFactor, anyUnderline, anyStrikethrough)
            } else {
                null
            }

            return JustifiedLineData(
                lineIndex = lineIdx,
                baselineOffsetPx = (layout.getLineBaseline(lineIdx) - lineTopPx).toFloat(),
                runs = runs,
                decorations = decos,
            )
        }

        private fun buildLineDecorations(
            tokens: List<JustifyToken>,
            weights: FloatArray,
            unitExtra: Float,
            startCursorX: Float,
            alphaFactor: Float,
            anyUnderline: Boolean,
            anyStrikethrough: Boolean,
        ): List<JustifiedLineDecoration>? {
            val out = ArrayList<JustifiedLineDecoration>(4)
            if (anyUnderline) {
                collectDecorationSegments(tokens, weights, unitExtra, startCursorX, alphaFactor, STYLE_UNDERLINE, true, out)
            }
            if (anyStrikethrough) {
                collectDecorationSegments(tokens, weights, unitExtra, startCursorX, alphaFactor, STYLE_STRIKETHROUGH, false, out)
            }
            return out.takeIf { it.isNotEmpty() }
        }

        private fun collectDecorationSegments(
            tokens: List<JustifyToken>,
            weights: FloatArray,
            unitExtra: Float,
            startCursorX: Float,
            alphaFactor: Float,
            styleFlag: Int,
            isUnderline: Boolean,
            out: MutableList<JustifiedLineDecoration>,
        ) {
            var cx = startCursorX
            var inSpan = false
            var segStart = 0f
            var segEnd = 0f
            for (k in 0 until tokens.size) {
                val tok = tokens[k]
                val active = (tok.styleBits and styleFlag) != 0
                if (active) {
                    if (!inSpan) {
                        inSpan = true
                        segStart = cx
                    }
                    segEnd = cx + tok.advanceWidth
                } else if (inSpan) {
                    out.add(
                        JustifiedLineDecoration(
                            startX = segStart,
                            endX = segEnd,
                            isUnderline = isUnderline,
                            isStrikethrough = !isUnderline,
                            alphaFactor = alphaFactor,
                        )
                    )
                    inSpan = false
                }
                cx += tok.advanceWidth + weights[k] * unitExtra
            }
            if (inSpan) {
                out.add(
                    JustifiedLineDecoration(
                        startX = segStart,
                        endX = segEnd,
                        isUnderline = isUnderline,
                        isStrikethrough = !isUnderline,
                        alphaFactor = alphaFactor,
                    )
                )
            }
        }

        private fun advanceGraphemeCluster(
            text: String,
            start: Int,
            limit: Int,
        ): Int {
            var end = start + Character.charCount(Character.codePointAt(text, start))
            while (end < limit) {
                if (text[end] == WORD_JOINER) break
                val nextCp = Character.codePointAt(text, end)
                val type = Character.getType(nextCp)
                if (
                    type == Character.NON_SPACING_MARK.toInt() ||
                    type == Character.COMBINING_SPACING_MARK.toInt() ||
                    type == Character.ENCLOSING_MARK.toInt() ||
                    nextCp in 0xFE00..0xFE0F ||
                    nextCp in 0xE0100..0xE01EF
                ) {
                    end += Character.charCount(nextCp)
                } else if (nextCp == 0x200D) {
                    end += Character.charCount(nextCp)
                    if (end < limit && text[end] != WORD_JOINER) {
                        end += Character.charCount(Character.codePointAt(text, end))
                    }
                } else {
                    break
                }
            }
            return end
        }

        private fun isLatinWordCoreChar(ch: Char): Boolean {
            return ch.code < 0x2E80 && (Character.isLetterOrDigit(ch) || ch == '_')
        }

        private fun isTrailingAttachedCodePoint(cp: Int): Boolean {
            if (cp <= 0xFFFF) {
                val ch = cp.toChar()
                if (lineStartProhibitedChars.contains(ch)) return true
                return ch == ',' || ch == '.' || ch == '!' || ch == '?' ||
                    ch == ':' || ch == ';' || ch == ')' || ch == ']' || ch == '}' || ch == '%'
            }
            return false
        }

        private fun isLeadingAttachedCodePoint(cp: Int): Boolean {
            if (cp <= 0xFFFF) {
                val ch = cp.toChar()
                if (lineEndProhibitedChars.contains(ch)) return true
                return ch == '(' || ch == '[' || ch == '{'
            }
            return false
        }

        private fun containsCjkOrFullwidth(
            seq: CharSequence,
            start: Int = 0,
            end: Int = seq.length,
        ): Boolean {
            var i = start.coerceAtLeast(0)
            val limit = end.coerceAtMost(seq.length)
            while (i < limit) {
                val cp = Character.codePointAt(seq, i)
                if (isCjkOrFullwidthCodePoint(cp)) return true
                i += Character.charCount(cp)
            }
            return false
        }

        private fun isCjkOrFullwidthCodePoint(cp: Int): Boolean {
            return (cp in 0x2E80..0x9FFF) ||
                (cp in 0xAC00..0xD7AF) ||
                (cp in 0xF900..0xFAFF) ||
                (cp in 0xFE10..0xFE6F) ||
                (cp in 0xFF00..0xFFEF) ||
                (cp in 0x20000..0x2FA1F)
        }

        private fun isLikelyJapaneseParagraph(text: String): Boolean {
            var kana = 0
            var visible = 0
            for (i in 0 until text.length) {
                val code = text[i].code
                if (text[i].isWhitespace() || code == 0x3000) continue
                visible++
                if (code in 0x3040..0x309F || code in 0x30A0..0x30FF || code in 0xFF66..0xFF9D) {
                    kana++
                }
            }
            if (visible == 0) return false
            val ratio = kana.toFloat() / visible.toFloat()
            return kana >= 6 && ratio >= 0.25f
        }
    }
}
