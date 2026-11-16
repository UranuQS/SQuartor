package com.squartor.reader.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONObject
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class AppThemeId(val label: String, val seedColor: Color) {
    wallpaper("壁纸", Color(0xFF2F6FA8)),
    wine("酒红", Color(0xFFB00046)),
    blue("深蓝", Color(0xFF2F6FA8)),
    green("墨绿", Color(0xFF2E7D5F)),
    brown("暖棕", Color(0xFF9B522F)),
    purple("紫黑", Color(0xFF7D4CC2)),
    black("纯黑", Color(0xFF5B6472));

    companion object {
        fun fromName(name: String?): AppThemeId =
            entries.firstOrNull { it.name == name } ?: wine
    }
}

enum class ReaderBackgroundId(val label: String) {
    theme("主题"),
    warm("暖棕"),
    black("黑色"),
    paper("米白"),
    green("护眼");

    companion object {
        fun fromName(name: String?): ReaderBackgroundId =
            entries.firstOrNull { it.name == name } ?: warm
    }
}

enum class AppBrightnessMode(val label: String) {
    system("跟随系统"),
    scheduled("定时切换"),
    light("浅色"),
    dark("深色");

    companion object {
        fun fromName(name: String?): AppBrightnessMode =
            entries.firstOrNull { it.name == name } ?: system
    }
}

enum class ReadingFlowMode(val label: String) {
    cover("覆盖"),
    slide("平移"),
    curl("仿真"),
    scroll("滚动");

    val isPaged: Boolean
        get() = this != scroll

    companion object {
        val paged: ReadingFlowMode get() = cover

        fun fromName(name: String?, legacyPageTurnAnimation: Boolean = false): ReadingFlowMode =
            when (name) {
                "cover" -> cover
                "slide" -> slide
                "curl" -> curl
                "scroll" -> scroll
                "paged" -> if (legacyPageTurnAnimation) curl else cover
                else -> entries.firstOrNull { it.name == name } ?: cover
            }
    }
}

enum class GlassClarityMode(val label: String) {
    clear("通透 Clear"),
    regular("标准 Regular"),
    tinted("高对比 Tinted");

    companion object {
        fun fromName(name: String?): GlassClarityMode =
            entries.firstOrNull { it.name == name } ?: regular
    }
}

enum class AmbientGlowSource(val label: String) {
    bookCover("在读封面取色"),
    appTheme("应用主题色");

    companion object {
        fun fromName(name: String?): AmbientGlowSource =
            entries.firstOrNull { it.name == name } ?: bookCover
    }
}

@Immutable
data class AppPalette(
    val id: AppThemeId,
    val label: String,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardAlt: Color,
    val line: Color,
    val primary: Color,
    val primarySoft: Color,
    val text: Color,
    val muted: Color,
    val subtle: Color,
    val blueMuted: Color,
) {
    val isLight: Boolean get() = background.luminance() > 0.5f
    val accentText: Color get() = if (isLight) primary else primarySoft
}

@Immutable
data class ReaderPalette(
    val id: ReaderBackgroundId,
    val label: String,
    val background: Color,
    val text: Color,
    val muted: Color,
)

val darkAppPalettes: Map<AppThemeId, AppPalette> = mapOf(
    AppThemeId.wallpaper to AppPalette(
        id = AppThemeId.wallpaper,
        label = "壁纸",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        card = Color(0xFF1C1C1E),
        cardAlt = Color(0xFF2C2C2E),
        line = Color(0xFF38383A),
        primary = Color(0xFF2F6FA8),
        primarySoft = Color(0xFFA9D5FF),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF98989D),
        subtle = Color(0xFF636366),
        blueMuted = Color(0xFFA9D5FF),
    ),
    AppThemeId.wine to AppPalette(
        id = AppThemeId.wine,
        label = "酒红",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        card = Color(0xFF1C1C1E),
        cardAlt = Color(0xFF2C2C2E),
        line = Color(0xFF38383A),
        primary = Color(0xFFB00046),
        primarySoft = Color(0xFFFF9FBD),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF98989D),
        subtle = Color(0xFF636366),
        blueMuted = Color(0xFFA9D5FF),
    ),
    AppThemeId.blue to AppPalette(
        id = AppThemeId.blue,
        label = "深蓝",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        card = Color(0xFF1C1C1E),
        cardAlt = Color(0xFF2C2C2E),
        line = Color(0xFF38383A),
        primary = Color(0xFF2F6FA8),
        primarySoft = Color(0xFFA9D5FF),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF98989D),
        subtle = Color(0xFF636366),
        blueMuted = Color(0xFFA9D5FF),
    ),
    AppThemeId.green to AppPalette(
        id = AppThemeId.green,
        label = "墨绿",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        card = Color(0xFF1C1C1E),
        cardAlt = Color(0xFF2C2C2E),
        line = Color(0xFF38383A),
        primary = Color(0xFF2E7D5F),
        primarySoft = Color(0xFFA7E6CE),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF98989D),
        subtle = Color(0xFF636366),
        blueMuted = Color(0xFFB3D8FF),
    ),
    AppThemeId.brown to AppPalette(
        id = AppThemeId.brown,
        label = "暖棕",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        card = Color(0xFF1C1C1E),
        cardAlt = Color(0xFF2C2C2E),
        line = Color(0xFF38383A),
        primary = Color(0xFF9B522F),
        primarySoft = Color(0xFFFFC49F),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF98989D),
        subtle = Color(0xFF636366),
        blueMuted = Color(0xFFC2DDFF),
    ),
    AppThemeId.purple to AppPalette(
        id = AppThemeId.purple,
        label = "紫黑",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        card = Color(0xFF1C1C1E),
        cardAlt = Color(0xFF2C2C2E),
        line = Color(0xFF38383A),
        primary = Color(0xFF7D4CC2),
        primarySoft = Color(0xFFD1B2FF),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF98989D),
        subtle = Color(0xFF636366),
        blueMuted = Color(0xFFB9DCFF),
    ),
    AppThemeId.black to AppPalette(
        id = AppThemeId.black,
        label = "纯黑",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        card = Color(0xFF1C1C1E),
        cardAlt = Color(0xFF2C2C2E),
        line = Color(0xFF38383A),
        primary = Color(0xFF5B6472),
        primarySoft = Color(0xFFD5DDE9),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF98989D),
        subtle = Color(0xFF636366),
        blueMuted = Color(0xFFA9D5FF),
    ),
)

val readerPalettes: Map<ReaderBackgroundId, ReaderPalette> = mapOf(
    ReaderBackgroundId.theme to ReaderPalette(
        id = ReaderBackgroundId.theme,
        label = "主题",
        background = Color(0xFF1A1B26),
        text = Color(0xFFECEEF6),
        muted = Color(0xFFA2A6B8),
    ),
    ReaderBackgroundId.warm to ReaderPalette(
        id = ReaderBackgroundId.warm,
        label = "暖棕",
        background = Color(0xFF17100D),
        text = Color(0xFFEADFD8),
        muted = Color(0xFFB7A8A0),
    ),
    ReaderBackgroundId.black to ReaderPalette(
        id = ReaderBackgroundId.black,
        label = "黑色",
        background = Color(0xFF000000),
        text = Color(0xFFDEDEDE),
        muted = Color(0xFF9A9A9A),
    ),
    ReaderBackgroundId.paper to ReaderPalette(
        id = ReaderBackgroundId.paper,
        label = "米白",
        background = Color(0xFFF2E5CF),
        text = Color(0xFF30241B),
        muted = Color(0xFF806B58),
    ),
    ReaderBackgroundId.green to ReaderPalette(
        id = ReaderBackgroundId.green,
        label = "护眼",
        background = Color(0xFFDFEAD9),
        text = Color(0xFF223026),
        muted = Color(0xFF667564),
    ),
)

private fun mixColors(a: Color, b: Color, amount: Float): Color {
    val inv = 1f - amount
    return Color(
        red = (a.red * inv + b.red * amount).coerceIn(0f, 1f),
        green = (a.green * inv + b.green * amount).coerceIn(0f, 1f),
        blue = (a.blue * inv + b.blue * amount).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

/**
 * Resolves the `主题` reader paper preset from the current [AppPalette] so it always stays
 * visibly distinct from both pure black (`黑色`) and warm espresso (`暖棕`) in dark mode,
 * as well as warm parchment (`米白`) and eye-care green (`护眼`) in light mode.
 */
fun resolveThemeReaderPalette(palette: AppPalette): ReaderPalette {
    return if (palette.isLight) {
        ReaderPalette(
            id = ReaderBackgroundId.theme,
            label = "主题",
            background = mixColors(Color(0xFFEDF1F8), palette.primary, 0.09f),
            text = Color(0xFF1A1C26),
            muted = mixColors(Color(0xFF606474), palette.primary, 0.16f),
        )
    } else {
        ReaderPalette(
            id = ReaderBackgroundId.theme,
            label = "主题",
            background = mixColors(Color(0xFF151620), palette.primary, 0.12f),
            text = mixColors(Color(0xFFEEF0F7), palette.primarySoft, 0.08f),
            muted = mixColors(Color(0xFFA0A4B6), palette.primarySoft, 0.14f),
        )
    }
}

fun buildPaletteFromSeed(
    seed: Color,
    isLight: Boolean,
    label: String,
    id: AppThemeId,
): AppPalette {
    val hsl = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (seed.red * 255).roundToInt().coerceIn(0, 255),
        (seed.green * 255).roundToInt().coerceIn(0, 255),
        (seed.blue * 255).roundToInt().coerceIn(0, 255),
        hsl,
    )
    hsl[1] = (hsl[1] * 0.78f).coerceIn(0.18f, 0.44f)
    hsl[2] = hsl[2].coerceIn(0.46f, 0.68f)
    val normalizedPrimary = Color(android.graphics.Color.HSVToColor(hsl))
    return if (isLight) {
        AppPalette(
            id = id,
            label = label,
            background = Color(0xFFF5F5F7),
            surface = Color(0xFFFFFFFF),
            card = Color(0xFFFFFFFF),
            cardAlt = Color(0xFFEEEEF0),
            line = Color(0xFFE5E5EA),
            primary = normalizedPrimary,
            primarySoft = mixColors(normalizedPrimary, Color.White, 0.38f),
            text = Color(0xFF0E0E12),
            muted = Color(0xFF282830),
            subtle = Color(0xFF50505A),
            blueMuted = Color(0xFF007AFF),
        )
    } else {
        AppPalette(
            id = id,
            label = label,
            background = Color(0xFF000000),
            surface = Color(0xFF121214),
            card = Color(0xFF1C1C1E),
            cardAlt = Color(0xFF2C2C2E),
            line = Color(0xFF38383A),
            primary = normalizedPrimary,
            primarySoft = mixColors(normalizedPrimary, Color.White, 0.46f),
            text = Color(0xFFF5F5F7),
            muted = Color(0xFFB4B4BC),
            subtle = Color(0xFF82828A),
            blueMuted = Color(0xFF64D2FF),
        )
    }
}

@Immutable
data class ReadingStyle(
    val brightnessMode: AppBrightnessMode = AppBrightnessMode.system,
    val dayStartHour: Int = 7,
    val nightStartHour: Int = 22,
    val autoNightPaper: Boolean = true,
    val appTheme: AppThemeId = AppThemeId.wine,
    val customThemeColorValue: Int? = null,
    val readerBackground: ReaderBackgroundId = ReaderBackgroundId.warm,
    val fontSize: Float = 20f,
    val lineHeight: Float = 1.85f,
    val paragraphSpacing: Float = 14f,
    val letterSpacing: Float = 0.4f,
    val pageMargin: Float = 28f,
    val verticalMargin: Float = 48f,
    val readingFlow: ReadingFlowMode = ReadingFlowMode.paged,
    val reverseTapPageTurn: Boolean = false,
    val firstLineIndent: Boolean = true,
    val dimJapaneseText: Boolean = false,
    val pageTurnAnimation: Boolean = false,
    val fontWeightValue: Int = 400,
    val fontName: String? = null,
    val fontPath: String? = null,
    val appFontName: String? = null,
    val appFontPath: String? = null,
    val glassClarityMode: GlassClarityMode = GlassClarityMode.regular,
    val glassOpacityLevel: Float = 0.5f,
    val ambientGlowSource: AmbientGlowSource = AmbientGlowSource.bookCover,
) {
    val fontWeight: FontWeight
        get() = FontWeight(fontWeightValue.coerceIn(100, 900))

    val readerPalette: ReaderPalette
        get() = readerPalettes[readerBackground] ?: readerPalettes[ReaderBackgroundId.warm]!!

    fun isScheduledLight(nowHour: Int = LocalTime.now().hour): Boolean {
        return if (dayStartHour < nightStartHour) {
            nowHour in dayStartHour until nightStartHour
        } else if (dayStartHour > nightStartHour) {
            nowHour >= dayStartHour || nowHour < nightStartHour
        } else {
            true
        }
    }

    /** Returns true when this style resolves to a light palette for the given device dark state. */
    fun isEffectiveLight(isSystemDark: Boolean): Boolean = when (brightnessMode) {
        AppBrightnessMode.light -> true
        AppBrightnessMode.dark -> false
        AppBrightnessMode.scheduled -> isScheduledLight()
        AppBrightnessMode.system -> !isSystemDark
    }

    /** Cycles cleanly across system, light, and dark with an immediate flip out of system/scheduled. */
    fun nextCycleBrightnessMode(isSystemDark: Boolean): AppBrightnessMode = when (brightnessMode) {
        AppBrightnessMode.system -> if (isSystemDark) AppBrightnessMode.light else AppBrightnessMode.dark
        AppBrightnessMode.light -> if (isSystemDark) AppBrightnessMode.dark else AppBrightnessMode.system
        AppBrightnessMode.dark -> if (isSystemDark) AppBrightnessMode.system else AppBrightnessMode.light
        AppBrightnessMode.scheduled -> if (isScheduledLight()) AppBrightnessMode.dark else AppBrightnessMode.light
    }

    fun resolvePalette(isLight: Boolean, dynamicSeedColor: Color? = null): AppPalette {
        val seed = customThemeColorValue?.let { Color(it) }
            ?: if (appTheme == AppThemeId.wallpaper && dynamicSeedColor != null) dynamicSeedColor
            else appTheme.seedColor
        if (!isLight && customThemeColorValue == null && appTheme != AppThemeId.wallpaper) {
            return darkAppPalettes[appTheme] ?: darkAppPalettes[AppThemeId.wine]!!
        }
        return buildPaletteFromSeed(
            seed = seed,
            isLight = isLight,
            label = appTheme.label,
            id = appTheme,
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("brightnessMode", brightnessMode.name)
        put("dayStartHour", dayStartHour)
        put("nightStartHour", nightStartHour)
        put("autoNightPaper", autoNightPaper)
        put("appTheme", appTheme.name)
        put("customThemeColorValue", customThemeColorValue ?: JSONObject.NULL)
        put("readerBackground", readerBackground.name)
        put("fontSize", fontSize.toDouble())
        put("lineHeight", lineHeight.toDouble())
        put("paragraphSpacing", paragraphSpacing.toDouble())
        put("letterSpacing", letterSpacing.toDouble())
        put("pageMargin", pageMargin.toDouble())
        put("verticalMargin", verticalMargin.toDouble())
        put("readingFlow", readingFlow.name)
        put("reverseTapPageTurn", reverseTapPageTurn)
        put("firstLineIndent", firstLineIndent)
        put("dimJapaneseText", dimJapaneseText)
        put("pageTurnAnimation", pageTurnAnimation)
        put("fontWeightValue", fontWeightValue)
        put("fontName", fontName ?: JSONObject.NULL)
        put("fontPath", fontPath ?: JSONObject.NULL)
        put("appFontName", appFontName ?: JSONObject.NULL)
        put("appFontPath", appFontPath ?: JSONObject.NULL)
        put("glassClarityMode", glassClarityMode.name)
        put("glassOpacityLevel", glassOpacityLevel.toDouble())
        put("ambientGlowSource", ambientGlowSource.name)
    }

    companion object {
        fun fromJson(obj: JSONObject): ReadingStyle = ReadingStyle(
            brightnessMode = AppBrightnessMode.fromName(obj.optString("brightnessMode")),
            dayStartHour = obj.optInt("dayStartHour", 7).coerceIn(0, 23),
            nightStartHour = obj.optInt("nightStartHour", 22).coerceIn(0, 23),
            autoNightPaper = obj.optBoolean("autoNightPaper", true),
            appTheme = AppThemeId.fromName(obj.optString("appTheme")),
            customThemeColorValue = if (obj.isNull("customThemeColorValue") || !obj.has("customThemeColorValue")) null else obj.optInt("customThemeColorValue"),
            readerBackground = ReaderBackgroundId.fromName(obj.optString("readerBackground")),
            fontSize = obj.optDouble("fontSize", 20.0).toFloat().coerceIn(14f, 32f),
            lineHeight = obj.optDouble("lineHeight", 1.85).toFloat().coerceIn(1.2f, 2.6f),
            paragraphSpacing = obj.optDouble("paragraphSpacing", 14.0).toFloat().coerceIn(0f, 30f),
            letterSpacing = obj.optDouble("letterSpacing", 0.4).toFloat().coerceIn(0f, 2f),
            pageMargin = obj.optDouble("pageMargin", 28.0).toFloat().coerceIn(0f, 52f),
            verticalMargin = obj.optDouble("verticalMargin", 48.0).toFloat().coerceIn(4f, 96f),
            readingFlow = ReadingFlowMode.fromName(
                name = obj.optString("readingFlow"),
                legacyPageTurnAnimation = obj.optBoolean("pageTurnAnimation", false),
            ),
            reverseTapPageTurn = obj.optBoolean("reverseTapPageTurn", false),
            firstLineIndent = obj.optBoolean("firstLineIndent", true),
            dimJapaneseText = obj.optBoolean("dimJapaneseText", false),
            pageTurnAnimation = obj.optBoolean("pageTurnAnimation", false),
            fontWeightValue = obj.optInt("fontWeightValue", 400).coerceIn(100, 900),
            fontName = obj.optString("fontName").takeIf { it.isNotEmpty() && !obj.isNull("fontName") },
            fontPath = obj.optString("fontPath").takeIf { it.isNotEmpty() && !obj.isNull("fontPath") },
            appFontName = obj.optString("appFontName").takeIf { it.isNotEmpty() && !obj.isNull("appFontName") },
            appFontPath = obj.optString("appFontPath").takeIf { it.isNotEmpty() && !obj.isNull("appFontPath") },
            glassClarityMode = GlassClarityMode.fromName(obj.optString("glassClarityMode")),
            glassOpacityLevel = obj.optDouble("glassOpacityLevel", 0.5).let {
                if (it.isNaN()) 0.5f else it.toFloat().coerceIn(0f, 1f)
            },
            ambientGlowSource = AmbientGlowSource.fromName(obj.optString("ambientGlowSource")),
        )
    }
}

data class CloudSyncSettings(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val username: String = "",
    val password: String = "",
    val lastUploadAtMillis: Long? = null,
    val lastDownloadAtMillis: Long? = null,
) {
    val configured: Boolean
        get() = endpoint.trim().isNotEmpty() && username.trim().isNotEmpty() && password.isNotEmpty()

    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("endpoint", endpoint)
        put("username", username)
        put("password", password)
        put("lastUploadAt", lastUploadAtMillis?.let { formatIso8601(it) } ?: JSONObject.NULL)
        put("lastDownloadAt", lastDownloadAtMillis?.let { formatIso8601(it) } ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(obj: JSONObject): CloudSyncSettings = CloudSyncSettings(
            enabled = obj.optBoolean("enabled", false),
            endpoint = obj.optString("endpoint", ""),
            username = obj.optString("username", ""),
            password = obj.optString("password", ""),
            lastUploadAtMillis = if (obj.isNull("lastUploadAt")) null else parseIso8601(obj.optString("lastUploadAt")),
            lastDownloadAtMillis = if (obj.isNull("lastDownloadAt")) null else parseIso8601(obj.optString("lastDownloadAt")),
        )
    }
}
