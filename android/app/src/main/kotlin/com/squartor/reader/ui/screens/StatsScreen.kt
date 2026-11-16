package com.squartor.reader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squartor.reader.AmbientSubpageBackground
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.GlassClarityMode
import com.squartor.reader.model.ReadingStyle
import com.squartor.reader.model.cleanBookTitle
import com.squartor.reader.ui.glass.Ios27GlassVariant
import com.squartor.reader.ui.glass.Ios27SegmentedPill
import com.squartor.reader.ui.glass.IsolatedAmbientGlassContainer
import com.squartor.reader.ui.glass.LocalBookLaunchTracker
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.ios27GlassSurface
import com.squartor.reader.ui.glass.rememberSafeGlassEffectState
import com.squartor.reader.ui.glass.unclippedBoundsInRoot
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffectContainer
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ActivityMode(val label: String) {
    Week("周"),
    Month("月"),
    Year("年"),
}

private val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val BOOK_DISTRIBUTION_COLORS = listOf(
    Color(0xFFF27A45),
    Color(0xFF2563EB),
    Color(0xFF10B981),
    Color(0xFF8B5CF6),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF06B6D4),
    Color(0xFF64748B),
)

private fun dateKeyOf(date: LocalDate): String = date.format(DATE_KEY_FORMATTER)

private fun formatDurationLabel(seconds: Int): String {
    if (seconds <= 0) return "0 分钟"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}小时 ${minutes}分钟"
        hours > 0 -> "${hours}小时"
        minutes > 0 -> "${minutes}分钟"
        else -> "${seconds}秒"
    }
}

private fun formatShortDuration(seconds: Int): String {
    if (seconds <= 0) return "0m"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatMillisDate(millis: Long?): String {
    if (millis == null || millis <= 0L) return "未记录"
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> "${date.monthValue}月${date.dayOfMonth}日"
    }
}

private fun secondsForBookOnDate(
    bookStats: Map<String, Map<String, Int>>,
    bookId: String,
    dateKey: String,
): Int {
    bookStats[bookId]?.get(dateKey)?.let { return it }
    bookStats[dateKey]?.get(bookId)?.let { return it }
    return 0
}

private fun totalSecondsForBook(
    bookStats: Map<String, Map<String, Int>>,
    bookId: String,
): Int {
    val direct = bookStats[bookId]
    if (direct != null) {
        return direct.values.sum()
    }
    var sum = 0
    for ((_, inner) in bookStats) {
        sum += inner[bookId] ?: 0
    }
    return sum
}

internal val TabularNumStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
internal fun ios27GroupedSurfaceColor(
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
    elevated: Boolean = false,
): Color {
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val baseAlpha = when (style.glassClarityMode) {
        GlassClarityMode.clear -> if (palette.isLight) 0.44f + opacity * 0.22f else 0.38f + opacity * 0.22f
        GlassClarityMode.regular -> if (palette.isLight) 0.62f + opacity * 0.22f else 0.54f + opacity * 0.22f
        GlassClarityMode.tinted -> if (palette.isLight) 0.78f + opacity * 0.16f else 0.72f + opacity * 0.18f
    }
    val resolvedAlpha = (if (elevated) baseAlpha + 0.10f else baseAlpha).coerceIn(0.36f, 0.94f)
    return if (palette.isLight) {
        Color.White.copy(alpha = resolvedAlpha)
    } else {
        Color(0xFF1D1E24).copy(alpha = resolvedAlpha)
    }
}

@Composable
internal fun ios27SubtleChipColor(
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
): Color {
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val alpha = when (style.glassClarityMode) {
        GlassClarityMode.clear -> if (palette.isLight) 0.48f + opacity * 0.20f else 0.44f + opacity * 0.20f
        GlassClarityMode.regular -> if (palette.isLight) 0.64f + opacity * 0.20f else 0.58f + opacity * 0.20f
        GlassClarityMode.tinted -> if (palette.isLight) 0.80f + opacity * 0.14f else 0.72f + opacity * 0.16f
    }.coerceIn(0.40f, 0.94f)
    return if (palette.isLight) {
        Color.White.copy(alpha = alpha)
    } else {
        Color(0xFF282932).copy(alpha = alpha)
    }
}

@Composable
internal fun ios27GroupedEdgeBrush(
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
): Brush {
    val isLight = palette.isLight
    val clarityBoost = when (style.glassClarityMode) {
        GlassClarityMode.clear -> 0.04f
        GlassClarityMode.regular -> 0.02f
        GlassClarityMode.tinted -> 0f
    }
    return remember(isLight, clarityBoost, palette.line) {
        if (isLight) {
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = (0.76f + clarityBoost).coerceAtMost(0.90f)),
                0.45f to Color(0xFF0E1117).copy(alpha = 0.05f),
                1.0f to Color(0xFF0E1117).copy(alpha = 0.09f),
            )
        } else {
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = (0.18f + clarityBoost).coerceAtMost(0.28f)),
                0.45f to Color.White.copy(alpha = 0.07f),
                1.0f to Color.White.copy(alpha = 0.04f),
            )
        }
    }
}

@Composable
internal fun Modifier.ios27GroupedSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
    elevated: Boolean = false,
): Modifier {
    val surfaceColor = ios27GroupedSurfaceColor(palette = palette, style = style, elevated = elevated)
    val edgeBrush = ios27GroupedEdgeBrush(palette = palette, style = style)
    return this
        .clip(shape)
        .background(surfaceColor)
        .border(width = 0.5.dp, brush = edgeBrush, shape = shape)
}

@Composable
internal fun Ios27GroupedDivider(
    palette: AppPalette,
    startIndent: Dp = 52.dp,
    endIndent: Dp = 16.dp,
) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent, end = endIndent),
        thickness = 0.5.dp,
        color = palette.text.copy(alpha = if (palette.isLight) 0.085f else 0.11f),
    )
}

private enum class StatsTimeframe(
    val label: String,
    val eyebrowTitle: String,
    val avgLabel: String,
) {
    Week("本周", "本周累计", "本周日均"),
    Month("本月", "本月累计", "本月日均"),
    All("全部", "全部累计", "累计日均"),
}

@Immutable
private data class StatsThemeColors(
    val background: Color,
    val cardSurface: Color,
    val divider: Color,
    val canvasHairline: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val finishedGreen: Color,
    val unreadTrack: Color,
    val progressTrack: Color,
    val pressedRow: Color,
)

@Composable
private fun rememberStatsThemeColors(
    palette: AppPalette,
    isDark: Boolean,
): StatsThemeColors {
    return remember(palette, isDark) {
        StatsThemeColors(
            background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            cardSurface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            divider = if (isDark) Color(0xFF38383A).copy(alpha = 0.82f) else Color(0xFFC6C6C8).copy(alpha = 0.60f),
            canvasHairline = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f),
            primaryText = if (isDark) Color(0xFFF5F5F7) else Color(0xFF111113),
            secondaryText = if (isDark) Color(0xFF98989D) else Color(0xFF636366),
            accent = palette.accentText,
            finishedGreen = if (isDark) Color(0xFF30D158) else Color(0xFF34C759),
            unreadTrack = if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6),
            progressTrack = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
            pressedRow = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.045f),
        )
    }
}

private data class StatsRhythmBar(
    val label: String,
    val detailTitle: String,
    val seconds: Int,
    val isTodayOrCurrent: Boolean = false,
    val isFuture: Boolean = false,
)

private data class StatsRankedBook(
    val book: BookEntry,
    val periodSeconds: Int,
    val totalSeconds: Int,
    val progressRatio: Float,
)

private data class StatsPeriodSnapshot(
    val timeframe: StatsTimeframe,
    val periodSeconds: Int,
    val dailyAverageSeconds: Int,
    val chartAverageSeconds: Int,
    val activeDaysInPeriod: Int,
    val bars: List<StatsRhythmBar>,
    val defaultBarIndex: Int,
    val rankedBooks: List<StatsRankedBook>,
)

private fun formatCompactDuration(seconds: Int): String {
    if (seconds <= 0) return "0 分钟"
    val totalMinutes = seconds / 60
    if (totalMinutes == 0) return "${seconds} 秒"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> "${minutes} 分钟"
        minutes == 0 -> "${hours} 小时"
        else -> "${hours}小时${minutes}分"
    }
}

private fun formatTodayDelta(seconds: Int): String {
    if (seconds <= 0) return ""
    val totalMinutes = seconds / 60
    if (totalMinutes == 0) return "+${seconds} 秒"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> "+${minutes} 分"
        minutes == 0 -> "+${hours} 小时"
        else -> "+${hours} 小时 ${minutes} 分"
    }
}

private fun dateMapForBook(
    bookStats: Map<String, Map<String, Int>>,
    bookId: String,
): Map<String, Int> {
    val direct = bookStats[bookId]
    if (direct != null) return direct
    val fallback = LinkedHashMap<String, Int>()
    for ((dateKey, inner) in bookStats) {
        val sec = inner[bookId] ?: 0
        if (sec > 0) fallback[dateKey] = (fallback[dateKey] ?: 0) + sec
    }
    return fallback
}

private fun computeReadingStreaks(
    dailyTotals: Map<String, Int>,
    today: LocalDate,
): Pair<Int, Int> {
    val todaySec = dailyTotals[dateKeyOf(today)] ?: 0
    var currentStreak = 0
    var cursor = if (todaySec > 0) today else today.minusDays(1)
    while ((dailyTotals[dateKeyOf(cursor)] ?: 0) > 0) {
        currentStreak += 1
        cursor = cursor.minusDays(1)
    }

    val sortedActiveDates = dailyTotals
        .filterValues { it > 0 }
        .keys
        .mapNotNull { runCatching { LocalDate.parse(it, DATE_KEY_FORMATTER) }.getOrNull() }
        .sorted()

    var bestStreak = currentStreak
    var run = 0
    var prev: LocalDate? = null
    for (date in sortedActiveDates) {
        run = if (prev != null && prev.plusDays(1) == date) run + 1 else 1
        if (run > bestStreak) bestStreak = run
        prev = date
    }
    return currentStreak to bestStreak
}

private fun buildStatsPeriodSnapshot(
    timeframe: StatsTimeframe,
    today: LocalDate,
    books: List<BookEntry>,
    dailyTotals: Map<String, Int>,
    bookStats: Map<String, Map<String, Int>>,
): StatsPeriodSnapshot {
    val weekdayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    val zoneId = ZoneId.systemDefault()

    val bars: List<StatsRhythmBar>
    val periodSeconds: Int
    val dailyAvgSeconds: Int
    val chartAvgSeconds: Int
    val activeDaysInPeriod: Int
    val defaultBarIndex: Int
    val rangeStartMillis: Long
    val periodBookSeconds: (String) -> Int

    when (timeframe) {
        StatsTimeframe.Week -> {
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            val elapsedDays = today.dayOfWeek.value.coerceIn(1, 7)
            val weekKeys = HashSet<String>(7)
            bars = (0 until 7).map { offset ->
                val day = monday.plusDays(offset.toLong())
                val future = day.isAfter(today)
                val key = dateKeyOf(day)
                if (!future) weekKeys.add(key)
                val sec = if (future) 0 else (dailyTotals[key] ?: 0)
                val title = if (day == today) {
                    "今天"
                } else {
                    "周${weekdayNames[offset]} ${day.monthValue}月${day.dayOfMonth}日"
                }
                StatsRhythmBar(
                    label = weekdayNames[offset],
                    detailTitle = title,
                    seconds = sec,
                    isTodayOrCurrent = day == today,
                    isFuture = future,
                )
            }
            periodSeconds = bars.sumOf { it.seconds }
            activeDaysInPeriod = bars.count { !it.isFuture && it.seconds > 0 }
            dailyAvgSeconds = periodSeconds / elapsedDays
            chartAvgSeconds = dailyAvgSeconds
            defaultBarIndex = (today.dayOfWeek.value - 1).coerceIn(0, 6)
            rangeStartMillis = monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
            periodBookSeconds = { bookId ->
                val map = dateMapForBook(bookStats, bookId)
                weekKeys.sumOf { map[it] ?: 0 }
            }
        }

        StatsTimeframe.Month -> {
            val ym = YearMonth.from(today)
            val daysInMonth = ym.lengthOfMonth()
            val elapsedDays = today.dayOfMonth.coerceIn(1, daysInMonth)
            val monthPrefix = String.format(Locale.US, "%04d-%02d-", ym.year, ym.monthValue)
            bars = (1..daysInMonth).map { dayNum ->
                val day = ym.atDay(dayNum)
                val future = day.isAfter(today)
                val sec = if (future) 0 else (dailyTotals[dateKeyOf(day)] ?: 0)
                val axisTick = if (dayNum == 1 || dayNum == 8 || dayNum == 15 || dayNum == 22 || dayNum == daysInMonth) {
                    "${dayNum}日"
                } else {
                    ""
                }
                val title = if (day == today) {
                    "今天"
                } else {
                    "${day.monthValue}月${day.dayOfMonth}日"
                }
                StatsRhythmBar(
                    label = axisTick,
                    detailTitle = title,
                    seconds = sec,
                    isTodayOrCurrent = day == today,
                    isFuture = future,
                )
            }
            periodSeconds = bars.sumOf { it.seconds }
            activeDaysInPeriod = bars.count { !it.isFuture && it.seconds > 0 }
            dailyAvgSeconds = periodSeconds / elapsedDays
            chartAvgSeconds = dailyAvgSeconds
            defaultBarIndex = (today.dayOfMonth - 1).coerceIn(0, bars.lastIndex)
            rangeStartMillis = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            periodBookSeconds = { bookId ->
                val map = dateMapForBook(bookStats, bookId)
                map.entries.sumOf { if (it.key.startsWith(monthPrefix)) it.value else 0 }
            }
        }

        StatsTimeframe.All -> {
            val currentYm = YearMonth.from(today)
            val monthItems = (11 downTo 0).map { back ->
                val ym = currentYm.minusMonths(back.toLong())
                val prefix = String.format(Locale.US, "%04d-%02d-", ym.year, ym.monthValue)
                val sum = dailyTotals.entries.sumOf { if (it.key.startsWith(prefix)) it.value else 0 }
                StatsRhythmBar(
                    label = if (back == 0) "本月" else "${ym.monthValue}月",
                    detailTitle = "${ym.year}年${ym.monthValue}月",
                    seconds = sum,
                    isTodayOrCurrent = back == 0,
                    isFuture = false,
                )
            }
            bars = monthItems
            periodSeconds = dailyTotals.values.sum()
            val earliestDate = dailyTotals
                .filterValues { it > 0 }
                .keys
                .mapNotNull { runCatching { LocalDate.parse(it, DATE_KEY_FORMATTER) }.getOrNull() }
                .minOrNull()
            val elapsedSpanDays = if (earliestDate != null) {
                ((today.toEpochDay() - earliestDate.toEpochDay()) + 1L).toInt().coerceAtLeast(1)
            } else {
                1
            }
            activeDaysInPeriod = dailyTotals.count { it.value > 0 }
            dailyAvgSeconds = periodSeconds / elapsedSpanDays
            val nonZeroMonths = monthItems.count { it.seconds > 0 }.coerceAtLeast(1)
            chartAvgSeconds = monthItems.sumOf { it.seconds } / nonZeroMonths
            defaultBarIndex = monthItems.lastIndex
            rangeStartMillis = Long.MIN_VALUE
            periodBookSeconds = { bookId -> totalSecondsForBook(bookStats, bookId) }
        }
    }

    val rankedBooks = books.mapNotNull { book ->
        val pSec = periodBookSeconds(book.id)
        val tSec = totalSecondsForBook(bookStats, book.id)
        val touchedInRange = when (timeframe) {
            StatsTimeframe.All -> book.lastReadAtMillis != null || book.progress > 0.0 || tSec > 0
            else -> pSec > 0 || ((book.lastReadAtMillis ?: Long.MIN_VALUE) >= rangeStartMillis)
        }
        if (!touchedInRange) {
            null
        } else {
            StatsRankedBook(
                book = book,
                periodSeconds = pSec,
                totalSeconds = tSec,
                progressRatio = book.progress.toFloat().coerceIn(0f, 1f),
            )
        }
    }.sortedWith(
        compareByDescending<StatsRankedBook> { it.periodSeconds }
            .thenByDescending { it.totalSeconds }
            .thenByDescending { it.book.lastReadAtMillis ?: 0L }
            .thenByDescending { it.progressRatio },
    )

    return StatsPeriodSnapshot(
        timeframe = timeframe,
        periodSeconds = periodSeconds,
        dailyAverageSeconds = dailyAvgSeconds,
        chartAverageSeconds = chartAvgSeconds,
        activeDaysInPeriod = activeDaysInPeriod,
        bars = bars,
        defaultBarIndex = defaultBarIndex,
        rankedBooks = rankedBooks,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    books: List<BookEntry>,
    dailyTotals: Map<String, Int>,
    bookStats: Map<String, Map<String, Int>>,
    palette: AppPalette,
    onOpenBook: (BookEntry) -> Unit,
    onOpenDetail: () -> Unit,
    sharedBackdrop: GlassBackdrop? = null,
    @Suppress("UNUSED_PARAMETER") requireDockBackdrop: Boolean = false,
    isVisible: Boolean = true,
) {
    val isDark = !palette.isLight
    val colors = rememberStatsThemeColors(palette = palette, isDark = isDark)
    val today = remember { LocalDate.now() }
    var timeframeOrdinal by rememberSaveable { mutableIntStateOf(0) }
    val timeframe = StatsTimeframe.entries[timeframeOrdinal.coerceIn(0, StatsTimeframe.entries.lastIndex)]
    var selectedBarIndex by remember(timeframe) { mutableStateOf<Int?>(null) }

    val statsCache = remember {
        object {
            var lastTimeframe: StatsTimeframe? = null
            var lastBooks: List<BookEntry>? = null
            var lastDailyTotals: Map<String, Int>? = null
            var lastBookStats: Map<String, Map<String, Int>>? = null
            var snapshot: StatsPeriodSnapshot? = null
            var currentStreakDays: Int = 0
            var todaySeconds: Int = 0
            var allTimeSeconds: Int = 0
            var activeDaysAllTime: Int = 0
            var finishedCount: Int = 0
            var inProgressCount: Int = 0
            var unreadCount: Int = 0
        }
    }
    if (isVisible || statsCache.snapshot == null) {
        if (statsCache.snapshot == null ||
            statsCache.lastTimeframe != timeframe ||
            statsCache.lastBooks !== books ||
            statsCache.lastDailyTotals !== dailyTotals ||
            statsCache.lastBookStats !== bookStats
        ) {
            val nextSnapshot = buildStatsPeriodSnapshot(
                timeframe = timeframe,
                today = today,
                books = books,
                dailyTotals = dailyTotals,
                bookStats = bookStats,
            )
            val (nextStreakDays, _) = if (statsCache.snapshot == null || statsCache.lastDailyTotals !== dailyTotals) {
                computeReadingStreaks(dailyTotals, today)
            } else {
                statsCache.currentStreakDays to 0
            }
            val nextTodaySec = dailyTotals[dateKeyOf(today)] ?: 0
            val nextAllSec = dailyTotals.values.sum()
            val nextActiveDays = dailyTotals.count { it.value > 0 }
            val nextFinished = books.count { it.progress >= 0.999 }
            val nextInProgress = books.count { book ->
                book.progress < 0.999 && (book.progress > 0.001 || totalSecondsForBook(bookStats, book.id) > 0)
            }
            val nextUnread = (books.size - nextFinished - nextInProgress).coerceAtLeast(0)
            statsCache.lastTimeframe = timeframe
            statsCache.lastBooks = books
            statsCache.lastDailyTotals = dailyTotals
            statsCache.lastBookStats = bookStats
            statsCache.snapshot = nextSnapshot
            statsCache.currentStreakDays = nextStreakDays
            statsCache.todaySeconds = nextTodaySec
            statsCache.allTimeSeconds = nextAllSec
            statsCache.activeDaysAllTime = nextActiveDays
            statsCache.finishedCount = nextFinished
            statsCache.inProgressCount = nextInProgress
            statsCache.unreadCount = nextUnread
        }
    }
    val snapshot = statsCache.snapshot!!
    val currentStreakDays = statsCache.currentStreakDays
    val todaySeconds = statsCache.todaySeconds
    val allTimeSeconds = statsCache.allTimeSeconds
    val activeDaysAllTime = statsCache.activeDaysAllTime
    val finishedCount = statsCache.finishedCount
    val inProgressCount = statsCache.inProgressCount
    val unreadCount = statsCache.unreadCount

    val statusTopInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val cutoutTopInset = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusTopInset, cutoutTopInset)
    val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val layoutDirection = LocalLayoutDirection.current
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val startExtraInset = cutoutPadding.calculateLeftPadding(layoutDirection)
    val endExtraInset = cutoutPadding.calculateRightPadding(layoutDirection)

    val listState = rememberLazyListState()
    val bookLaunchTracker = LocalBookLaunchTracker.current
    val readingStyle = LocalReadingStyle.current
    val powerSave = LocalPowerSaveMode.current
    val haptics = LocalHapticFeedback.current

    val vitroBackdrop = sharedBackdrop ?: rememberGlassBackdrop()
    val activeBackdrop = if (powerSave) null else vitroBackdrop

    val displayRankedBooks = remember(snapshot.rankedBooks) { snapshot.rankedBooks.take(10) }

    CompositionLocalProvider(
        LocalVitroBackdrop provides activeBackdrop,
        LocalGlassDarkTheme provides isDark,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            // Layer 1: Isolate ambient bottom glow from Stats backdrop so Dock stays crisp neutral glass.
            IsolatedAmbientGlassContainer(
                backdrop = activeBackdrop,
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
                            start = 20.dp + startExtraInset,
                            top = topInset + 74.dp,
                            end = 20.dp + endExtraInset,
                            bottom = bottomInset.coerceAtLeast(24.dp) + 116.dp,
                        ),
                    ) {
                        item(key = "hero_summary") {
                            StatsEditorialHero(
                                timeframe = timeframe,
                                periodSeconds = snapshot.periodSeconds,
                                todaySeconds = todaySeconds,
                                allTimeSeconds = allTimeSeconds,
                                dailyAverageSeconds = snapshot.dailyAverageSeconds,
                                currentStreakDays = currentStreakDays,
                                finishedCount = finishedCount,
                                colors = colors,
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        item(key = "rhythm_chart") {
                            StatsRhythmChartSection(
                                timeframe = timeframe,
                                bars = snapshot.bars,
                                defaultBarIndex = snapshot.defaultBarIndex,
                                selectedBarIndex = selectedBarIndex,
                                chartAverageSeconds = snapshot.chartAverageSeconds,
                                colors = colors,
                                isDark = isDark,
                                powerSave = powerSave,
                                onSelectBar = { idx -> selectedBarIndex = idx },
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        item(key = "books_breakdown") {
                            StatsBooksBreakdownSection(
                                timeframe = timeframe,
                                rankedBooks = displayRankedBooks,
                                totalCount = snapshot.rankedBooks.size,
                                palette = palette,
                                colors = colors,
                                onLaunchBook = { book, coverRect ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (bookLaunchTracker != null) {
                                        bookLaunchTracker(book, coverRect)
                                    } else {
                                        onOpenBook(book)
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        item(key = "library_summary") {
                            StatsLibrarySummarySection(
                                inProgressCount = inProgressCount,
                                finishedCount = finishedCount,
                                unreadCount = unreadCount,
                                totalBooksCount = books.size,
                                colors = colors,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item(key = "detail_entry") {
                            StatsDetailDisclosureCard(
                                activeDaysAllTime = activeDaysAllTime,
                                colors = colors,
                                onOpenDetail = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenDetail()
                                },
                            )
                        }
                    }
                }
            }

            // Subtle top scroll scrim under floating Liquid Glass bar so status bar stays legible while content refracts beneath
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topInset + 64.dp)
                    .background(
                        Brush.verticalGradient(
                            0.00f to colors.background.copy(alpha = if (isDark) 0.86f else 0.92f),
                            0.55f to colors.background.copy(alpha = if (isDark) 0.44f else 0.52f),
                            1.00f to Color.Transparent,
                        ),
                    ),
            )

            // Layer 2: Floating Liquid Glass Header (ONLY Liquid Glass surface on StatsScreen)
            StatsTopFloatingBar(
                timeframe = timeframe,
                palette = palette,
                style = readingStyle,
                backdrop = activeBackdrop,
                colors = colors,
                isDark = isDark,
                onSelectTimeframe = { next ->
                    if (next.ordinal != timeframeOrdinal) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        timeframeOrdinal = next.ordinal
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp + startExtraInset,
                        top = topInset + 10.dp,
                        end = 20.dp + endExtraInset,
                    ),
            )
        }
    }
}

@Composable
private fun StatsCleanSurface(
    colors: StatsThemeColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardSurface),
    ) {
        content()
    }
}

@Composable
private fun StatsHairlineDivider(
    color: Color,
    startIndent: Dp = 0.dp,
    endIndent: Dp = 0.dp,
) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent, end = endIndent),
        thickness = 0.5.dp,
        color = color,
    )
}

@Composable
private fun StatsVerticalHairline(color: Color) {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .fillMaxHeight()
            .padding(vertical = 2.dp)
            .background(color),
    )
}

@Composable
private fun StatsSectionHeader(
    title: String,
    trailing: String,
    colors: StatsThemeColors,
    trailingColor: Color = colors.secondaryText,
    trailingEmphasized: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.secondaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
        )
        if (trailing.isNotEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = trailing,
                color = trailingColor,
                fontSize = 12.5.sp,
                fontWeight = if (trailingEmphasized) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TabularNumStyle,
            )
        }
    }
}

@Composable
private fun StatsTopFloatingBar(
    timeframe: StatsTimeframe,
    palette: AppPalette,
    style: ReadingStyle,
    backdrop: GlassBackdrop?,
    colors: StatsThemeColors,
    isDark: Boolean,
    onSelectTimeframe: (StatsTimeframe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(999.dp)
    val barHeight = 44.dp
    val slotWidth = 50.dp
    val density = LocalDensity.current
    val slotWidthPx = with(density) { slotWidth.toPx() }
    val thumbIndex by animateFloatAsState(
        targetValue = timeframe.ordinal.toFloat(),
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 440f),
        label = "statsTimeframeThumb",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left restrained Liquid Glass title pill
        Box(
            modifier = Modifier
                .height(barHeight)
                .ios27GlassSurface(
                    shape = pillShape,
                    palette = palette,
                    style = style,
                    variant = Ios27GlassVariant.Chrome,
                    backdrop = backdrop,
                    showContrastBorder = true,
                    showFingerHighlight = false,
                )
                .padding(horizontal = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalVitroBackdrop provides null) {
                Text(
                    text = "统计",
                    color = colors.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    maxLines = 1,
                )
            }
        }

        // Right Liquid Glass segmented filter pill (本周 / 本月 / 全部)
        Box(
            modifier = Modifier
                .height(barHeight)
                .width(slotWidth * StatsTimeframe.entries.size + 6.dp)
                .ios27GlassSurface(
                    shape = pillShape,
                    palette = palette,
                    style = style,
                    variant = Ios27GlassVariant.Chrome,
                    backdrop = backdrop,
                    showContrastBorder = true,
                    showFingerHighlight = false,
                ),
        ) {
            CompositionLocalProvider(LocalVitroBackdrop provides null) {
                val thumbBg = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.92f)
                val thumbBorder = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.06f)

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = slotWidthPx * thumbIndex
                                }
                                .width(slotWidth)
                                .fillMaxHeight()
                                .clip(pillShape)
                                .background(thumbBg)
                                .border(0.5.dp, thumbBorder, pillShape),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 3.dp),
                    ) {
                        StatsTimeframe.entries.forEach { option ->
                            val isSelected = option == timeframe
                            val itemInteraction = remember(option) { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .width(slotWidth)
                                    .fillMaxHeight()
                                    .clip(pillShape)
                                    .semantics {
                                        role = Role.Tab
                                        selected = isSelected
                                    }
                                    .clickable(
                                        interactionSource = itemInteraction,
                                        indication = null,
                                    ) { onSelectTimeframe(option) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = option.label,
                                    color = if (isSelected) colors.primaryText else colors.secondaryText,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
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
private fun StatsEditorialHero(
    timeframe: StatsTimeframe,
    periodSeconds: Int,
    todaySeconds: Int,
    allTimeSeconds: Int,
    dailyAverageSeconds: Int,
    currentStreakDays: Int,
    finishedCount: Int,
    colors: StatsThemeColors,
) {
    val totalMinutes = periodSeconds / 60
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val metricParts = remember(periodSeconds, totalMinutes, hours, mins) {
        when {
            hours > 0 && mins > 0 -> listOf(hours.toString() to "小时", mins.toString() to "分")
            hours > 0 -> listOf(hours.toString() to "小时")
            totalMinutes > 0 -> listOf(totalMinutes.toString() to "分钟")
            periodSeconds > 0 -> listOf(periodSeconds.toString() to "秒")
            else -> listOf("0" to "分钟")
        }
    }

    val noPadNumStyle = remember {
        TabularNumStyle.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
    }
    val noPadUnitStyle = remember {
        TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Tracked editorial eyebrow directly on canvas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = timeframe.eyebrowTitle,
                color = colors.secondaryText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.0.sp,
            )

            when {
                todaySeconds > 0 -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                        Text(
                            text = "今日 ${formatTodayDelta(todaySeconds)}",
                            color = colors.secondaryText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            style = TabularNumStyle,
                        )
                    }
                }

                timeframe != StatsTimeframe.All && allTimeSeconds > 0 -> {
                    Text(
                        text = "总计 ${formatCompactDuration(allTimeSeconds)}",
                        color = colors.secondaryText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = TabularNumStyle,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Unboxed typographic hero readout with strong numeral/unit weight contrast
        val heroReadoutDensity = LocalDensity.current
        val heroWindowWidthPx = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1)
        val defaultHeroReadoutWidthPx = remember(heroWindowWidthPx, heroReadoutDensity) {
            (heroWindowWidthPx - with(heroReadoutDensity) { 72.dp.roundToPx() }).coerceAtLeast(1)
        }
        var heroReadoutWidthPx by remember { mutableIntStateOf(defaultHeroReadoutWidthPx) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { sz ->
                    if (sz.width > 0 && heroReadoutWidthPx != sz.width) {
                        heroReadoutWidthPx = sz.width
                    }
                },
        ) {
            val fontScale = heroReadoutDensity.fontScale
            val maxWidth = with(heroReadoutDensity) { heroReadoutWidthPx.toDp() }
            val compactDisplay = maxWidth < 330.dp || fontScale > 1.15f
            val numFontSize = when {
                metricParts.size > 1 && compactDisplay -> 40.sp
                metricParts.size > 1 -> 46.sp
                compactDisplay -> 44.sp
                else -> 52.sp
            }
            val unitFontSize = if (compactDisplay || metricParts.size > 1) 16.sp else 17.5.sp

            Row(modifier = Modifier.fillMaxWidth()) {
                metricParts.forEachIndexed { idx, (numText, unitText) ->
                    if (idx > 0) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = numText,
                        color = colors.primaryText,
                        fontSize = numFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.6).sp,
                        style = noPadNumStyle,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unitText,
                        color = colors.secondaryText,
                        fontSize = unitFontSize,
                        fontWeight = FontWeight.Normal,
                        style = noPadUnitStyle,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        StatsHairlineDivider(color = colors.canvasHairline)
        Spacer(modifier = Modifier.height(14.dp))

        // Open 3-item metric strip separated only by ultra-light 0.5dp hairlines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatsHeroStripCell(
                label = "连续阅读",
                value = currentStreakDays.toString(),
                unit = "天",
                colors = colors,
                noPadNumStyle = noPadNumStyle,
                noPadUnitStyle = noPadUnitStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            )
            StatsVerticalHairline(color = colors.canvasHairline)
            StatsHeroStripCell(
                label = timeframe.avgLabel,
                value = formatShortDuration(dailyAverageSeconds),
                unit = null,
                colors = colors,
                noPadNumStyle = noPadNumStyle,
                noPadUnitStyle = noPadUnitStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            StatsVerticalHairline(color = colors.canvasHairline)
            StatsHeroStripCell(
                label = "已读完",
                value = finishedCount.toString(),
                unit = "本",
                colors = colors,
                noPadNumStyle = noPadNumStyle,
                noPadUnitStyle = noPadUnitStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun StatsHeroStripCell(
    label: String,
    value: String,
    unit: String?,
    colors: StatsThemeColors,
    noPadNumStyle: TextStyle,
    noPadUnitStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            color = colors.secondaryText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row {
            Text(
                text = value,
                color = colors.primaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = noPadNumStyle,
                modifier = Modifier.alignByBaseline(),
            )
            if (!unit.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    style = noPadUnitStyle,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun StatsRhythmChartSection(
    timeframe: StatsTimeframe,
    bars: List<StatsRhythmBar>,
    defaultBarIndex: Int,
    selectedBarIndex: Int?,
    chartAverageSeconds: Int,
    colors: StatsThemeColors,
    isDark: Boolean,
    powerSave: Boolean,
    onSelectBar: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val activeIdx = (selectedBarIndex ?: defaultBarIndex).coerceIn(0, (bars.size - 1).coerceAtLeast(0))
    val activeBar = bars.getOrNull(activeIdx)
    val hasAnyActivity = remember(bars) { bars.any { it.seconds > 0 } }
    val maxSeconds = remember(bars) {
        (bars.maxOfOrNull { it.seconds } ?: 0).coerceAtLeast(1)
    }

    var isScrubbing by remember(timeframe) { mutableStateOf(false) }
    var revealTriggered by remember(timeframe, powerSave) { mutableStateOf(powerSave) }
    LaunchedEffect(timeframe, powerSave) {
        revealTriggered = true
    }
    val animatedReveal by animateFloatAsState(
        targetValue = if (revealTriggered) 1f else 0.12f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 380f),
        label = "statsRhythmReveal",
    )
    val barReveal = if (powerSave) 1f else animatedReveal

    val avgDashEffect = remember(density) {
        with(density) {
            PathEffect.dashPathEffect(floatArrayOf(3.5.dp.toPx(), 3.5.dp.toPx()), 0f)
        }
    }

    val currentBars by rememberUpdatedState(bars)
    val currentActiveIdx by rememberUpdatedState(activeIdx)
    val currentOnSelectBar by rememberUpdatedState(onSelectBar)
    var canvasWidthPx by remember { mutableFloatStateOf(1f) }

    fun selectAtX(x: Float) {
        val count = currentBars.size
        if (count <= 0 || canvasWidthPx <= 1f) return
        val idx = ((x / canvasWidthPx) * count).toInt().coerceIn(0, count - 1)
        if (idx != currentActiveIdx) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            currentOnSelectBar(idx)
        }
    }

    val liveIndicatorText = if (activeBar != null) {
        val secText = if (activeBar.seconds > 0) formatCompactDuration(activeBar.seconds) else "无记录"
        "${activeBar.detailTitle} · $secText"
    } else {
        ""
    }
    val liveIndicatorActive = activeBar != null && (activeBar.seconds > 0 || isScrubbing)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsSectionHeader(
            title = "阅读节律",
            trailing = liveIndicatorText,
            colors = colors,
            trailingColor = if (liveIndicatorActive) colors.accent else colors.secondaryText,
            trailingEmphasized = isScrubbing || (activeBar?.seconds ?: 0) > 0,
        )

        StatsCleanSurface(colors = colors) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (hasAnyActivity) "峰值 ${formatShortDuration(maxSeconds)}" else "暂无阅读记录",
                        color = colors.secondaryText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        style = TabularNumStyle,
                    )

                    if (chartAverageSeconds > 0 && hasAnyActivity) {
                        val avgCaption = if (timeframe == StatsTimeframe.All) {
                            "月均 ${formatShortDuration(chartAverageSeconds)}"
                        } else {
                            "日均 ${formatShortDuration(chartAverageSeconds)}"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(2.dp),
                            ) {
                                val cy = size.height * 0.5f
                                drawLine(
                                    color = colors.accent.copy(alpha = if (isDark) 0.72f else 0.68f),
                                    start = Offset(0f, cy),
                                    end = Offset(size.width, cy),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = avgDashEffect,
                                )
                            }
                            Text(
                                text = avgCaption,
                                color = colors.secondaryText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                style = TabularNumStyle,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val chartDescription = remember(timeframe, activeBar, maxSeconds, chartAverageSeconds) {
                    val activeDesc = activeBar?.let { "${it.detailTitle} ${formatCompactDuration(it.seconds)}" } ?: ""
                    "阅读节律图，${timeframe.label}最高 ${formatShortDuration(maxSeconds)}，平均 ${formatShortDuration(chartAverageSeconds)}。当前选中 $activeDesc"
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .semantics { contentDescription = chartDescription }
                        .onSizeChanged { canvasWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(timeframe, bars.size) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPos = down.position
                                var pointerId = down.id
                                var scrubbing = false
                                var yieldedVertical = false
                                val touchSlop = viewConfiguration.touchSlop

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                            ?: event.changes.firstOrNull()
                                        if (change == null || !change.pressed) {
                                            if (change != null && !scrubbing && !yieldedVertical) {
                                                selectAtX(change.position.x)
                                            }
                                            break
                                        }
                                        pointerId = change.id
                                        val dx = change.position.x - startPos.x
                                        val dy = change.position.y - startPos.y
                                        if (!scrubbing && !yieldedVertical) {
                                            if (hypot(dx, dy) > touchSlop * 0.5f) {
                                                if (abs(dy) > abs(dx) * 1.5f && abs(dy) > touchSlop) {
                                                    yieldedVertical = true
                                                    break
                                                }
                                                scrubbing = true
                                                isScrubbing = true
                                                selectAtX(change.position.x)
                                            }
                                        }
                                        if (scrubbing) {
                                            change.consume()
                                            selectAtX(change.position.x)
                                        }
                                    }
                                } finally {
                                    isScrubbing = false
                                }
                            }
                        },
                ) {
                    val count = bars.size
                    if (count <= 0) return@Canvas

                    val topPad = 8.dp.toPx()
                    val bottomPad = 2.dp.toPx()
                    val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                    val plotBottom = topPad + plotH
                    val slotW = size.width / count
                    val barW = when {
                        count <= 7 -> (slotW * 0.36f).coerceIn(12.dp.toPx(), 18.dp.toPx())
                        count <= 14 -> (slotW * 0.44f).coerceIn(8.dp.toPx(), 13.dp.toPx())
                        else -> (slotW * 0.54f).coerceIn(3.5.dp.toPx(), 6.5.dp.toPx())
                    }
                    val topRadius = barW * 0.5f
                    val barCornerRadius = CornerRadius(topRadius, topRadius)

                    // Whisper-thin horizontal guide rules (top, mid, baseline)
                    val guideColor = colors.divider.copy(alpha = 0.42f)
                    val ruleStroke = 0.5.dp.toPx()
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, topPad),
                        end = Offset(size.width, topPad),
                        strokeWidth = ruleStroke,
                    )
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, topPad + plotH * 0.5f),
                        end = Offset(size.width, topPad + plotH * 0.5f),
                        strokeWidth = ruleStroke,
                    )
                    drawLine(
                        color = colors.divider.copy(alpha = 0.85f),
                        start = Offset(0f, plotBottom),
                        end = Offset(size.width, plotBottom),
                        strokeWidth = 0.65.dp.toPx(),
                    )

                    clipRect(left = 0f, top = 0f, right = size.width, bottom = plotBottom) {
                        val activeBarColor = colors.accent
                        val restingBarColor = colors.accent.copy(alpha = if (isDark) 0.25f else 0.20f)
                        val selectedZeroColor = colors.accent.copy(alpha = 0.55f)
                        val zeroTickColor = colors.divider.copy(alpha = 0.72f)
                        val futureTickColor = colors.divider.copy(alpha = 0.30f)
                        val tickHeightPx = 2.dp.toPx()
                        val tickCorner = CornerRadius(tickHeightPx * 0.5f, tickHeightPx * 0.5f)

                        bars.forEachIndexed { idx, bar ->
                            val centerX = idx * slotW + slotW * 0.5f
                            val barLeft = centerX - barW * 0.5f
                            val isSelected = idx == activeIdx

                            if (!bar.isFuture && bar.seconds > 0) {
                                val ratio = (bar.seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
                                val minBarH = max(topRadius * 1.4f, 4.dp.toPx())
                                val targetH = max(plotH * ratio, minBarH)
                                val animH = (targetH * barReveal).coerceAtLeast(minBarH * 0.6f)
                                val barTop = (plotBottom - animH).coerceAtLeast(topPad)
                                val drawHeight = (plotBottom - barTop) + topRadius

                                drawRoundRect(
                                    color = if (isSelected) activeBarColor else restingBarColor,
                                    topLeft = Offset(barLeft, barTop),
                                    size = Size(barW, drawHeight),
                                    cornerRadius = barCornerRadius,
                                )
                            } else {
                                val tickColor = when {
                                    isSelected -> selectedZeroColor
                                    bar.isFuture -> futureTickColor
                                    else -> zeroTickColor
                                }
                                drawRoundRect(
                                    color = tickColor,
                                    topLeft = Offset(barLeft, plotBottom - tickHeightPx),
                                    size = Size(barW, tickHeightPx),
                                    cornerRadius = tickCorner,
                                )
                            }
                        }

                        // Crisp average dashed baseline across active plot
                        if (chartAverageSeconds > 0 && hasAnyActivity) {
                            val avgRatio = (chartAverageSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
                            val avgY = (plotBottom - plotH * avgRatio * barReveal).coerceIn(topPad, plotBottom - 2.dp.toPx())
                            drawLine(
                                color = colors.accent.copy(alpha = if (isDark) 0.60f else 0.52f),
                                start = Offset(0f, avgY),
                                end = Offset(size.width, avgY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = avgDashEffect,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (timeframe) {
                    StatsTimeframe.Week -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            bars.forEachIndexed { idx, bar ->
                                val highlighted = idx == activeIdx
                                val showTodayDot = idx == defaultBarIndex && selectedBarIndex != null && selectedBarIndex != defaultBarIndex
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = bar.label,
                                        color = if (highlighted) colors.accent else colors.secondaryText,
                                        fontSize = 11.sp,
                                        fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                    )
                                    Box(
                                        modifier = Modifier.height(3.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (showTodayDot) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.secondaryText.copy(alpha = 0.7f)),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StatsTimeframe.Month -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val totalDays = bars.size
                            listOf(1, 8, 15, 22, totalDays).forEach { dayNum ->
                                Text(
                                    text = "${dayNum}日",
                                    color = colors.secondaryText,
                                    fontSize = 11.sp,
                                    style = TabularNumStyle,
                                )
                            }
                        }
                    }

                    StatsTimeframe.All -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            bars.forEachIndexed { idx, bar ->
                                val highlighted = idx == activeIdx
                                val showTick = idx % 2 == 1 || idx == 0 || idx == bars.lastIndex
                                Text(
                                    text = if (showTick) bar.label else "",
                                    color = if (highlighted) colors.accent else colors.secondaryText,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    style = TabularNumStyle,
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
private fun StatsBooksBreakdownSection(
    timeframe: StatsTimeframe,
    rankedBooks: List<StatsRankedBook>,
    totalCount: Int,
    palette: AppPalette,
    colors: StatsThemeColors,
    onLaunchBook: (BookEntry, Rect?) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsSectionHeader(
            title = "阅读分布",
            trailing = if (totalCount > 0) "共 ${totalCount} 本" else "",
            colors = colors,
        )

        StatsCleanSurface(colors = colors) {
            if (rankedBooks.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${timeframe.label}暂无阅读足迹",
                            color = colors.primaryText,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "打开书架中的书籍阅读后，时长与进度会同步呈现在这里",
                            color = colors.secondaryText,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    rankedBooks.forEachIndexed { idx, item ->
                        StatsRankedBookRow(
                            item = item,
                            palette = palette,
                            colors = colors,
                            onLaunchBook = { rect -> onLaunchBook(item.book, rect) },
                        )
                        if (idx < rankedBooks.lastIndex) {
                            // 16dp row pad + 38dp cover + 14dp spacer = 68dp flush with book title start
                            StatsHairlineDivider(
                                color = colors.divider,
                                startIndent = 68.dp,
                                endIndent = 0.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRankedBookRow(
    item: StatsRankedBook,
    palette: AppPalette,
    colors: StatsThemeColors,
    onLaunchBook: (Rect?) -> Unit,
) {
    val interactionSource = remember(item.book.id) { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "statsBookRowPress",
    )
    var coverRect by remember(item.book.id) { mutableStateOf<Rect?>(null) }
    val progressPct = (item.progressRatio * 100f).roundToInt().coerceIn(0, 100)
    val isFinished = item.progressRatio >= 0.999f
    val rightMetric = if (item.periodSeconds > 0) {
        formatCompactDuration(item.periodSeconds)
    } else {
        formatMillisDate(item.book.lastReadAtMillis)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(colors.pressedRow.copy(alpha = colors.pressedRow.alpha * pressedAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onLaunchBook(coverRect) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(
            book = item.book,
            palette = palette,
            modifier = Modifier.onGloballyPositioned { coverRect = it.unclippedBoundsInRoot() },
            width = 38.dp,
            height = 52.dp,
            cornerRadius = 6.dp,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cleanBookTitle(item.book.title),
                    color = colors.primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = rightMetric,
                    color = if (item.periodSeconds > 0) colors.primaryText else colors.secondaryText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    style = TabularNumStyle,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.progressTrack),
                ) {
                    if (item.progressRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.progressRatio.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isFinished) colors.finishedGreen else colors.accent),
                        )
                    }
                }
                Text(
                    text = "${progressPct}%",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    style = TabularNumStyle,
                    modifier = Modifier.widthIn(min = 34.dp),
                )
            }
        }
    }
}

private data class LibraryStatusRowData(
    val label: String,
    val count: Int,
    val dotColor: Color,
)

@Composable
private fun StatsLibrarySummarySection(
    inProgressCount: Int,
    finishedCount: Int,
    unreadCount: Int,
    totalBooksCount: Int,
    colors: StatsThemeColors,
) {
    val rows = remember(inProgressCount, finishedCount, unreadCount, colors) {
        listOf(
            LibraryStatusRowData("在读", inProgressCount, colors.accent),
            LibraryStatusRowData("已读", finishedCount, colors.finishedGreen),
            LibraryStatusRowData("未读", unreadCount, colors.unreadTrack),
        )
    }
    val activeSegments = remember(rows) { rows.filter { it.count > 0 } }
    val minSegmentWeight = remember(totalBooksCount) {
        max(1f, totalBooksCount * 0.025f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsSectionHeader(
            title = "藏书概览",
            trailing = "共 ${totalBooksCount} 本",
            colors = colors,
        )

        StatsCleanSurface(colors = colors) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    if (activeSegments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.progressTrack),
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            activeSegments.forEach { seg ->
                                Box(
                                    modifier = Modifier
                                        .weight(max(seg.count.toFloat(), minSegmentWeight))
                                        .fillMaxHeight()
                                        .background(seg.dotColor),
                                )
                            }
                        }
                    }
                }

                StatsHairlineDivider(
                    color = colors.divider,
                    startIndent = 16.dp,
                    endIndent = 0.dp,
                )

                rows.forEachIndexed { idx, row ->
                    val pct = if (totalBooksCount > 0) {
                        ((row.count.toFloat() / totalBooksCount.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(row.dotColor),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = row.label,
                            color = colors.primaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${row.count} 本",
                            color = colors.primaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TabularNumStyle,
                        )
                        if (totalBooksCount > 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${pct}%",
                                color = colors.secondaryText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.End,
                                style = TabularNumStyle,
                                modifier = Modifier.widthIn(min = 34.dp),
                            )
                        }
                    }
                    if (idx < rows.lastIndex) {
                        // 16dp row pad + 8dp dot + 12dp spacer = 36dp flush with row label start
                        StatsHairlineDivider(
                            color = colors.divider,
                            startIndent = 36.dp,
                            endIndent = 0.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsDetailDisclosureCard(
    activeDaysAllTime: Int,
    colors: StatsThemeColors,
    onOpenDetail: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "statsDetailDisclosurePress",
    )
    val trailingSummary = if (activeDaysAllTime > 0) "累计 ${activeDaysAllTime} 天" else "查看"

    StatsCleanSurface(colors = colors) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(colors.pressedRow.copy(alpha = colors.pressedRow.alpha * pressedAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onOpenDetail,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "详细统计与里程碑",
                color = colors.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trailingSummary,
                color = colors.secondaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                style = TabularNumStyle,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.secondaryText.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private data class ActivityBarItem(
    val label: String,
    val detailTitle: String,
    val seconds: Int,
)

private data class BookTimeSlice(
    val book: BookEntry,
    val seconds: Int,
    val ratio: Float,
    val color: Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsDetailScreen(
    books: List<BookEntry>,
    dailyTotals: Map<String, Int>,
    bookStats: Map<String, Map<String, Int>>,
    palette: AppPalette,
    onBack: () -> Unit,
    onOpenBook: (BookEntry) -> Unit,
) {
    var activityMode by rememberSaveable { mutableStateOf(ActivityMode.Week) }
    var selectedBarIndex by remember(activityMode) { mutableIntStateOf(-1) }
    val today = remember { LocalDate.now() }

    val barItems = remember(activityMode, dailyTotals, today) {
        when (activityMode) {
            ActivityMode.Week -> {
                val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                val names = listOf("一", "二", "三", "四", "五", "六", "日")
                (0 until 7).map { i ->
                    val d = monday.plusDays(i.toLong())
                    val sec = dailyTotals[dateKeyOf(d)] ?: 0
                    ActivityBarItem(
                        label = names[i],
                        detailTitle = "${d.monthValue}月${d.dayOfMonth}日 (周${names[i]})",
                        seconds = sec,
                    )
                }
            }
            ActivityMode.Month -> {
                val ym = YearMonth.from(today)
                (1..ym.lengthOfMonth()).map { day ->
                    val d = ym.atDay(day)
                    val sec = dailyTotals[dateKeyOf(d)] ?: 0
                    ActivityBarItem(
                        label = if (day == 1 || day % 5 == 0) "$day" else "",
                        detailTitle = "${ym.monthValue}月${day}日",
                        seconds = sec,
                    )
                }
            }
            ActivityMode.Year -> {
                val year = today.year
                (1..12).map { month ->
                    val ym = YearMonth.of(year, month)
                    var sum = 0
                    for (day in 1..ym.lengthOfMonth()) {
                        sum += dailyTotals[dateKeyOf(ym.atDay(day))] ?: 0
                    }
                    ActivityBarItem(
                        label = "${month}月",
                        detailTitle = "${year}年${month}月",
                        seconds = sum,
                    )
                }
            }
        }
    }

    val activeBarIdx = if (selectedBarIndex in barItems.indices) {
        selectedBarIndex
    } else {
        when (activityMode) {
            ActivityMode.Week -> (today.dayOfWeek.value - 1).coerceIn(0, barItems.lastIndex)
            ActivityMode.Month -> (today.dayOfMonth - 1).coerceIn(0, barItems.lastIndex)
            ActivityMode.Year -> (today.monthValue - 1).coerceIn(0, barItems.lastIndex)
        }
    }

    // Milestones calculation
    val activeDaysCount = remember(dailyTotals) { dailyTotals.count { it.value > 0 } }
    val maxDaySeconds = remember(dailyTotals) { dailyTotals.values.maxOrNull() ?: 0 }
    val longestStreak = remember(dailyTotals) {
        val sortedDates = dailyTotals
            .filter { it.value > 0 }
            .keys
            .mapNotNull { runCatching { LocalDate.parse(it, DATE_KEY_FORMATTER) }.getOrNull() }
            .sorted()
        var best = 0
        var current = 0
        var prev: LocalDate? = null
        for (d in sortedDates) {
            if (prev != null && prev.plusDays(1) == d) {
                current += 1
            } else {
                current = 1
            }
            if (current > best) best = current
            prev = d
        }
        best
    }
    val finishedBooksCount = remember(books) { books.count { it.progress >= 0.999 } }

    // Per-book reading time ratio distribution
    val bookSlices = remember(books, bookStats) {
        val pairs = books.map { book ->
            val recorded = totalSecondsForBook(bookStats, book.id)
            val fallback = if (recorded == 0 && book.progress > 0) {
                (book.progress * 1800).roundToInt()
            } else {
                recorded
            }
            book to fallback
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        val total = pairs.sumOf { it.second }.coerceAtLeast(1)
        pairs.mapIndexed { idx, (book, sec) ->
            BookTimeSlice(
                book = book,
                seconds = sec,
                ratio = (sec.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                color = BOOK_DISTRIBUTION_COLORS[idx % BOOK_DISTRIBUTION_COLORS.size],
            )
        }
    }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnBack by rememberUpdatedState(onBack)
    val bookLaunchTracker = LocalBookLaunchTracker.current
    val powerSave = LocalPowerSaveMode.current
    val vitroBackdrop = if (powerSave) null else LocalVitroBackdrop.current
    val listState = rememberLazyListState()
    val backInteraction = remember { MutableInteractionSource() }
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }
    }

    var edgeOffsetPx by remember { mutableFloatStateOf(0f) }
    var edgeSettleJob by remember { mutableStateOf<Job?>(null) }

    val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalVitroBackdrop provides vitroBackdrop) {
    val detailWindowWidthPx = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1)
    var detailWidthPx by remember { mutableIntStateOf(detailWindowWidthPx) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { sz ->
                if (sz.width > 0 && detailWidthPx != sz.width) {
                    detailWidthPx = sz.width
                }
            },
    ) {
        val fullWidthPx = detailWidthPx.toFloat().coerceAtLeast(1f)
        val edgeZonePx = with(density) { 32.dp.toPx() }
        val minFlingDragPx = with(density) { 10.dp.toPx() }
        val shadowWidthDp = 16.dp
        val shadowWidthPx = with(density) { shadowWidthDp.toPx() }
        val swipeProgress = (edgeOffsetPx / fullWidthPx).coerceIn(0f, 1f)

        if (edgeOffsetPx > 1f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(shadowWidthDp)
                    .offset { IntOffset((edgeOffsetPx - shadowWidthPx).roundToInt(), 0) }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(
                                    alpha = (if (palette.isLight) 0.10f else 0.34f) * (1f - swipeProgress),
                                ),
                            ),
                        ),
                    ),
            )
        }

        // Unified swipeable container keeping Vitro LocalVitroBackdrop coordinates aligned during edge swipe
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(edgeOffsetPx.roundToInt(), 0) }
                .pointerInput(fullWidthPx) {
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
                            val shouldDismiss = edgeOffsetPx > fullWidthPx * 0.30f ||
                                (vx > 700f && edgeOffsetPx > minFlingDragPx)
                            if (shouldDismiss) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnBack()
                            } else {
                                val startVal = edgeOffsetPx
                                edgeSettleJob = scope.launch {
                                    animate(
                                        initialValue = startVal,
                                        targetValue = 0f,
                                        initialVelocity = vx,
                                        animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f),
                                    ) { value, _ -> edgeOffsetPx = value }
                                    edgeOffsetPx = 0f
                                }
                            }
                        }
                    }
                },
        ) {
            // Dedicated backdrop layer: background + ambient glow + LazyColumn only so sibling scrims & Vitro chrome never self-reference
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (vitroBackdrop != null) {
                            Modifier.glassBackdrop(vitroBackdrop)
                        } else {
                            Modifier
                        },
                    )
                    .background(palette.background),
            ) {
                AmbientSubpageBackground(palette = palette)

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = topInset + 58.dp,
                        end = 20.dp,
                        bottom = bottomInset.coerceAtLeast(24.dp) + 56.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                // Large iOS 27 Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "详细统计",
                            color = palette.text,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                        )
                        Text(
                            text = "阅读趋势 · 里程碑 · 书籍分布",
                            color = palette.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Animated Activity Bar Chart Card with Continuous Horizontal Scrubbing & Dynamic Callout
                item {
                    val periodTotal = remember(barItems) { barItems.sumOf { it.seconds } }
                    val maxBarSec = remember(barItems) { max(barItems.maxOfOrNull { it.seconds } ?: 0, 1800) }
                    val activeBar = barItems.getOrNull(activeBarIdx)

                    var chartWidthPx by remember { mutableFloatStateOf(1f) }
                    var badgeWidthPx by remember { mutableFloatStateOf(0f) }
                    var isScrubbingBars by remember { mutableStateOf(false) }

                    val currentActiveIdx by rememberUpdatedState(activeBarIdx)
                    val currentBarCount by rememberUpdatedState(barItems.size)

                    fun updateBarSelectionAt(xPx: Float) {
                        if (currentBarCount <= 0 || chartWidthPx <= 1f) return
                        val idx = ((xPx / chartWidthPx) * currentBarCount).toInt()
                            .coerceIn(0, currentBarCount - 1)
                        if (idx != currentActiveIdx) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedBarIndex = idx
                        }
                    }

                    val targetBadgeCenterPx = if (barItems.isNotEmpty()) {
                        ((activeBarIdx + 0.5f) / barItems.size.toFloat()) * chartWidthPx
                    } else {
                        0f
                    }
                    val clampedBadgeLeftPx = (targetBadgeCenterPx - badgeWidthPx * 0.5f)
                        .coerceIn(0f, (chartWidthPx - badgeWidthPx).coerceAtLeast(0f))
                    val animatedBadgeLeftPx by animateFloatAsState(
                        targetValue = clampedBadgeLeftPx,
                        animationSpec = spring(dampingRatio = 0.80f, stiffness = 360f),
                        label = "barCalloutX",
                    )
                    val badgeScale by animateFloatAsState(
                        targetValue = if (isScrubbingBars) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = 0.76f, stiffness = 400f),
                        label = "barCalloutScale",
                    )

                    val chartCardShape = RoundedCornerShape(24.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .ios27GroupedSurface(shape = chartCardShape, palette = palette)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "阅读趋势 (${activityMode.label})",
                                color = palette.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(ios27SubtleChipColor(palette))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "合计 ${formatDurationLabel(periodTotal)}",
                                    color = palette.accentText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TabularNumStyle,
                                )
                            }
                        }

                        // Dynamic value callout badge above the selected bar
                        if (activeBar != null) {
                            val calloutShape = RoundedCornerShape(12.dp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .offset { IntOffset(animatedBadgeLeftPx.roundToInt(), 0) }
                                        .graphicsLayer {
                                            scaleX = badgeScale
                                            scaleY = badgeScale
                                        }
                                        .onSizeChanged { badgeWidthPx = it.width.toFloat() }
                                        .ios27GroupedSurface(
                                            shape = calloutShape,
                                            palette = palette,
                                            elevated = true,
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (activeBar.seconds > 0) palette.primary else palette.subtle),
                                    )
                                    Text(
                                        text = activeBar.detailTitle,
                                        color = palette.text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = formatDurationLabel(activeBar.seconds),
                                        color = if (activeBar.seconds > 0) palette.accentText else palette.muted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TabularNumStyle,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        // Scrubbable Animated Bar Chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .onSizeChanged { chartWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                                .pointerInput(activityMode, barItems.size) {
                                    detectTapGestures { offset ->
                                        updateBarSelectionAt(offset.x)
                                    }
                                }
                                .pointerInput(activityMode, barItems.size) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            isScrubbingBars = true
                                            updateBarSelectionAt(offset.x)
                                        },
                                        onDragEnd = { isScrubbingBars = false },
                                        onDragCancel = { isScrubbingBars = false },
                                    ) { change, _ ->
                                        change.consume()
                                        updateBarSelectionAt(change.position.x)
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(
                                if (activityMode == ActivityMode.Month) 3.dp else 8.dp,
                            ),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            barItems.forEachIndexed { index, item ->
                                val ratio = (item.seconds.toFloat() / maxBarSec.toFloat()).coerceIn(0.04f, 1f)
                                val animatedRatioState = animateFloatAsState(
                                    targetValue = if (item.seconds > 0) ratio else 0.04f,
                                    animationSpec = spring(dampingRatio = 0.80f, stiffness = 360f),
                                    label = "barHeight",
                                )
                                val isSelected = index == activeBarIdx
                                val barCorner = if (activityMode == ActivityMode.Month) 4.dp else 7.dp
                                val barColor = when {
                                    isSelected -> palette.primary
                                    item.seconds > 0 -> palette.accentText.copy(alpha = 0.58f)
                                    else -> palette.line.copy(alpha = 0.30f)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.BottomCenter,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(if (activityMode == ActivityMode.Month) 0.85f else 0.65f)
                                                .fillMaxHeight()
                                                .drawBehind {
                                                    val barH = (size.height * animatedRatioState.value.coerceIn(0.04f, 1f))
                                                        .coerceAtLeast(1f)
                                                    val rPx = barCorner.toPx().coerceAtMost(min(size.width, barH) * 0.5f)
                                                    drawRoundRect(
                                                        color = barColor,
                                                        topLeft = Offset(0f, size.height - barH),
                                                        size = Size(size.width, barH),
                                                        cornerRadius = CornerRadius(rPx, rPx),
                                                    )
                                                },
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier.height(14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (item.label.isNotEmpty()) {
                                            Text(
                                                text = item.label,
                                                color = if (isSelected) palette.accentText else palette.muted,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                style = TabularNumStyle,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Reading Milestone Cards (2x2 Grid)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "阅读里程碑",
                            color = palette.text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MilestoneCard(
                                icon = Icons.Rounded.LocalFireDepartment,
                                title = "最长连续阅读",
                                value = "$longestStreak 天",
                                tint = Color(0xFFF97316),
                                palette = palette,
                                modifier = Modifier.weight(1f),
                            )
                            MilestoneCard(
                                icon = Icons.Rounded.Schedule,
                                title = "累计活跃天数",
                                value = "$activeDaysCount 天",
                                tint = Color(0xFF3B82F6),
                                palette = palette,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MilestoneCard(
                                icon = Icons.Rounded.EmojiEvents,
                                title = "单日最高时长",
                                value = formatShortDuration(maxDaySeconds),
                                tint = Color(0xFFEAB308),
                                palette = palette,
                                modifier = Modifier.weight(1f),
                            )
                            MilestoneCard(
                                icon = Icons.Rounded.WorkspacePremium,
                                title = "已读完书籍",
                                value = "$finishedBooksCount 本",
                                tint = Color(0xFF10B981),
                                palette = palette,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Per-Book Reading Time Ratio Distribution
                item {
                    val ratioCardShape = RoundedCornerShape(24.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .ios27GroupedSurface(shape = ratioCardShape, palette = palette)
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "书籍阅读时长占比",
                                color = palette.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "共 ${bookSlices.size} 本",
                                color = palette.muted,
                                fontSize = 12.sp,
                                style = TabularNumStyle,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (bookSlices.isEmpty()) {
                            Text(
                                text = "暂无分书籍阅读时长统计",
                                color = palette.muted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                        } else {
                            // Stacked proportional bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                bookSlices.take(8).forEach { slice ->
                                    Box(
                                        modifier = Modifier
                                            .weight(slice.ratio.coerceAtLeast(0.03f))
                                            .fillMaxHeight()
                                            .background(slice.color),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            bookSlices.forEachIndexed { index, slice ->
                                if (index > 0) {
                                    Ios27GroupedDivider(
                                        palette = palette,
                                        startIndent = 54.dp,
                                        endIndent = 0.dp,
                                    )
                                }
                                BookRatioRow(
                                    slice = slice,
                                    palette = palette,
                                    onLaunchBook = { coverRect ->
                                        if (bookLaunchTracker != null) {
                                            bookLaunchTracker(slice.book, coverRect)
                                        } else {
                                            onOpenBook(slice.book)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                }
            }

            // Bottom gesture-bar safe fade scrim so scrolling content clears the system home indicator cleanly
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomInset.coerceAtLeast(24.dp) + 18.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to palette.background.copy(alpha = 0.68f),
                            1f to palette.background.copy(alpha = 0.94f),
                        ),
                    ),
            )

            // Top floating-chrome & status-bar fade scrim so scrolled titles never collide with the clock or 周/月/年 pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topInset + 72.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to palette.background.copy(alpha = 0.96f),
                            0.62f to palette.background.copy(alpha = 0.84f),
                            1f to Color.Transparent,
                        ),
                    ),
            )

            // Floating Vitro Navigation Chrome (Back Circle + Compact Scrolled Title + 周/月/年 Ios27SegmentedPill)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = topInset + 10.dp, end = 18.dp)
                    .glassEffectContainer(spacing = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val backGlassState = rememberSafeGlassEffectState(interactionSource = backInteraction, active = true)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .ios27GlassSurface(
                            shape = CircleShape,
                            palette = palette,
                            variant = Ios27GlassVariant.Chrome,
                            backdrop = vitroBackdrop,
                            isInteractive = true,
                            state = backGlassState,
                            interactionSource = backInteraction,
                            showContrastBorder = false,
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

                Spacer(modifier = Modifier.width(10.dp))

                AnimatedVisibility(
                    visible = isScrolled,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(140)),
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .ios27GlassSurface(
                                shape = RoundedCornerShape(999.dp),
                                palette = palette,
                                variant = Ios27GlassVariant.Chrome,
                                backdrop = vitroBackdrop,
                                showContrastBorder = false,
                                showFingerHighlight = true,
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "详细统计",
                            color = palette.text,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Floating iOS 27 Segmented ActivityMode switch (周 / 月 / 年)
                Ios27SegmentedPill(
                    options = remember { ActivityMode.entries.map { it to it.label } },
                    selected = activityMode,
                    onSelect = { next ->
                        if (activityMode != next) {
                            activityMode = next
                        }
                    },
                    palette = palette,
                    modifier = Modifier.width(128.dp),
                    backdrop = vitroBackdrop,
                )
            }
        }
    }
    }
}

@Composable
private fun MilestoneCard(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color,
    palette: AppPalette,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .ios27GroupedSurface(shape = cardShape, palette = palette)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(19.dp),
            )
            Text(
                text = title,
                color = palette.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = value,
            color = palette.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            style = TabularNumStyle,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun BookRatioRow(
    slice: BookTimeSlice,
    palette: AppPalette,
    onLaunchBook: (Rect?) -> Unit,
) {
    val pct = (slice.ratio * 100f).roundToInt().coerceIn(1, 100)
    val animatedWidthState = animateFloatAsState(
        targetValue = slice.ratio.coerceIn(0.03f, 1f),
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 360f),
        label = "bookRatioBar",
    )
    var coverRect by remember(slice.book.id) { mutableStateOf<Rect?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onLaunchBook(coverRect) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(
            book = slice.book,
            palette = palette,
            modifier = Modifier.onGloballyPositioned { coverRect = it.unclippedBoundsInRoot() },
            width = 42.dp,
            height = 58.dp,
            cornerRadius = 7.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cleanBookTitle(slice.book.title),
                    color = palette.text,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatDurationLabel(slice.seconds)} · $pct%",
                    color = palette.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    style = TabularNumStyle,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.line.copy(alpha = 0.28f))
                    .drawBehind {
                        val barW = (size.width * animatedWidthState.value.coerceIn(0.03f, 1f))
                            .coerceAtLeast(size.height)
                        val r = size.height * 0.5f
                        drawRoundRect(
                            color = slice.color,
                            topLeft = Offset.Zero,
                            size = Size(barW, size.height),
                            cornerRadius = CornerRadius(r, r),
                        )
                    },
            )
        }
    }
}
