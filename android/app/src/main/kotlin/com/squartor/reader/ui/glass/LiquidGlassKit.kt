package com.squartor.reader.ui.glass

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint as AndroidPaint
import android.graphics.RuntimeShader
import android.graphics.Shader as AndroidShader
import android.os.Build
import android.view.Choreographer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.GlassClarityMode
import com.squartor.reader.model.ReadingStyle
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.GlassEffectState
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.rememberGlassEffectState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * 液态玻璃核心视觉调优配置（支持一键开启/关闭磨砂，支持随时无缝回退）：
 */
object LiquidGlassConfig {
    /**
     * 浅色模式磨砂开关：
     * - false：完全移除模糊，与深色模式保持 1:1 一致的纯净高锐折射（默认关闭模糊）；
     * - true：启用 iOS 27 式大半径高斯磨砂柔化背景。
     * 可随时修改此布尔值回退或重新启用。
     */
    const val ENABLE_LIGHT_MODE_FROST: Boolean = true

    /**
     * 浅色模式未启用磨砂时的白色柔光底色覆盖率（瀑布流区 / 白底区）。
     * 此时由于背景是锐利全彩书封，取 0.82f / 0.74f 既清透又保证深色文字的充足对比度。
     */
    const val LIGHT_COVER_WATERFALL_SHARP: Float = 0.82f
    const val LIGHT_COVER_SOLID_SHARP: Float = 0.74f

    /**
     * 浅色模式启用磨砂时的白色柔光底色覆盖率。
     * 经过调优取 0.62f（0.60f ~ 0.65f 黄金区间），在保证深色正文字体充足对比度（> 7:1）的同时，
     * 呈现温润细腻的液态玻璃折射漫射光泽。
     */
    const val LIGHT_COVER_WATERFALL_FROST: Float = 0.62f
    const val LIGHT_COVER_SOLID_FROST: Float = 0.72f

    val lightCoverWaterfall: Float
        get() = if (ENABLE_LIGHT_MODE_FROST) LIGHT_COVER_WATERFALL_FROST else LIGHT_COVER_WATERFALL_SHARP

    val lightCoverSolid: Float
        get() = if (ENABLE_LIGHT_MODE_FROST) LIGHT_COVER_SOLID_FROST else LIGHT_COVER_SOLID_SHARP
}

/**
 * iOS 26 / Squircle 连续曲率平滑圆角（基于 Apple HIG Continuous Curvature 样条曲线）：
 * 彻底消除标准圆角矩形直弧连接处的曲率二阶导数跳变（C2 不连续），呈现真正的超椭圆温润微质感。
 */
class SquircleShape(val cornerRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rPx = with(density) { cornerRadius.toPx() }
        val w = size.width
        val h = size.height
        val maxL = min(w, h) * 0.5f
        if (rPx <= 0f || maxL <= 0f) {
            return Outline.Rectangle(Rect(Offset.Zero, size))
        }

        val baseL = rPx * 1.528665f
        val scale = if (baseL > maxL) maxL / baseL else 1f
        val r = rPx * scale
        val l = r * 1.528665f

        val path = Path().apply {
            moveTo(w - l, 0f)

            // Top-right corner
            cubicTo(
                w - l + 0.192170f * r, 0f,
                w - l + 0.376825f * r, 0.052945f * r,
                w - l + 0.528665f * r, 0.131550f * r,
            )
            cubicTo(
                w - l + 0.716175f * r, 0.254925f * r,
                w - l + 0.921260f * r, 0.441408f * r,
                w - l + 1.087257f * r, 0.607405f * r,
            )
            cubicTo(
                w - l + 1.273740f * r, 0.812490f * r,
                w - l + 1.397115f * r, 1.000000f * r,
                w, l,
            )

            // Right edge down
            lineTo(w, h - l)

            // Bottom-right corner
            cubicTo(
                w, h - l + 0.192170f * r,
                w - 0.052945f * r, h - l + 0.376825f * r,
                w - 0.131550f * r, h - l + 0.528665f * r,
            )
            cubicTo(
                w - 0.254925f * r, h - l + 0.716175f * r,
                w - 0.441408f * r, h - l + 0.921260f * r,
                w - 0.607405f * r, h - l + 1.087257f * r,
            )
            cubicTo(
                w - 0.812490f * r, h - l + 1.273740f * r,
                w - 1.000000f * r, h - l + 1.397115f * r,
                w - l, h,
            )

            // Bottom edge left
            lineTo(l, h)

            // Bottom-left corner
            cubicTo(
                l - 0.192170f * r, h,
                l - 0.376825f * r, h - 0.052945f * r,
                l - 0.528665f * r, h - 0.131550f * r,
            )
            cubicTo(
                l - 0.716175f * r, h - 0.254925f * r,
                l - 0.921260f * r, h - 0.441408f * r,
                l - 1.087257f * r, h - 0.607405f * r,
            )
            cubicTo(
                l - 1.273740f * r, h - 0.812490f * r,
                l - 1.397115f * r, h - 1.000000f * r,
                0f, h - l,
            )

            // Left edge up
            lineTo(0f, l)

            // Top-left corner
            cubicTo(
                0f, l - 0.192170f * r,
                0.052945f * r, l - 0.376825f * r,
                0.131550f * r, l - 0.528665f * r,
            )
            cubicTo(
                0.254925f * r, l - 0.716175f * r,
                0.441408f * r, l - 0.921260f * r,
                0.607405f * r, l - 1.087257f * r,
            )
            cubicTo(
                0.812490f * r, l - 1.273740f * r,
                1.000000f * r, l - 1.397115f * r,
                l, 0f,
            )

            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return cornerRadius == other.cornerRadius
    }

    override fun hashCode(): Int = cornerRadius.hashCode()

    override fun toString(): String = "SquircleShape(cornerRadius=$cornerRadius)"
}

fun squircleShape(radius: Dp): Shape = SquircleShape(radius)

/**
 * 绘制阶段实时供应的单帧液态玻璃动态背景快照（包含预先绑定好 FILTER_MODE_LINEAR 的 BitmapShader），
 * 仅在 `drawWithContent` 绘制期按需读取，实现 0 次 Compose 重组与 120Hz/144Hz 满帧同步折射。
 */
data class LiquidGlassDynamicFrame(
    val bitmap: Bitmap,
    val shader: BitmapShader,
    // 与 bitmap 同帧生成的 1/4 分辨率大半径高斯磨砂纹理（iOS 27 式磨砂玻璃内部采样用）
    val hazeBitmap: Bitmap? = null,
    val hazeShader: BitmapShader? = null,
)

/**
 * 与屏幕像素坐标 (0, 0) ~ (screenWidthPx, screenHeightPx) 1:1 严格对齐的全局液态玻璃底层背景数据：
 * - `sharpScreenBitmap`：用于玻璃四周 16dp 曲面倒角做球面折射（`point - normal * depth`）与棱镜色散采样；
 * - `hazeScreenBitmap`：用于玻璃中心区域做柔和漫射采样；
 * - `dynamicFrameProvider`：可选的绘制期高刷帧纹理供应器（仅在半屏瀑布区域内的卡片 `drawWithContent` 中调用）。
 */
data class LiquidGlassBackdropData(
    val sharpScreenBitmap: Bitmap,
    val hazeScreenBitmap: Bitmap,
    val screenWidthPx: Float,
    val screenHeightPx: Float,
    val dynamicFrameProvider: (() -> LiquidGlassDynamicFrame)? = null,
) {
    val sharpShader: BitmapShader = BitmapShader(
        sharpScreenBitmap,
        AndroidShader.TileMode.CLAMP,
        AndroidShader.TileMode.CLAMP,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }
    }
    val hazeShader: BitmapShader = BitmapShader(
        hazeScreenBitmap,
        AndroidShader.TileMode.CLAMP,
        AndroidShader.TileMode.CLAMP,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }
    }
}

val LocalLiquidGlassBackdrop = compositionLocalOf<LiquidGlassBackdropData?> { null }

val LocalVitroBackdrop = compositionLocalOf<GlassBackdrop?> { null }

val LocalReadingStyle = compositionLocalOf { ReadingStyle() }

val LocalAmbientGlowColors = compositionLocalOf<Pair<Color, Color>?> { null }

val LocalAmbientGlowColorsState = staticCompositionLocalOf<State<Pair<Color, Color>>?> { null }

val LocalDockOffscreenHiddenState = staticCompositionLocalOf<State<Boolean>?> { null }

val LocalAmbientFlowSeconds = compositionLocalOf<FloatState?> { null }

val LocalPagerTransitioning = compositionLocalOf { false }

val LocalPagerTransitioningState = staticCompositionLocalOf<State<Boolean>?> { null }

val LocalRootHandlesBottomAmbient = staticCompositionLocalOf { false }

val LocalBookLaunchTracker = compositionLocalOf<((BookEntry, Rect?) -> Unit)?> { null }

val LocalPowerSaveMode = compositionLocalOf { false }

enum class Ios27GlassVariant {
    Chrome,
    FusibleCard,
    Card,
    SubtlePill,
    AccentPill,
    ReaderControl,
    ReaderSheet,
}

private fun optimizeIos27GlassForRuntime(
    glass: Glass,
    clarity: GlassClarityMode,
    opacity: Float,
    variant: Ios27GlassVariant,
    density: Float,
    powerSave: Boolean = false,
): Glass {
    // Keep blurPx strictly <= 16.8f px (blurSigma < 10.67) so Vitro runs its 2-FBO AGSL
    // `across` + `seen()` BlurTaps pipeline instead of Skia's ~10-FBO RenderEffect pyramid.
    val safeDensity = density.coerceAtLeast(1f)
    val maxFastBlurPx = 10.5f
    val blurPx = when (variant) {
        Ios27GlassVariant.Card,
        Ios27GlassVariant.FusibleCard -> 7.6f
        else -> when (clarity) {
            GlassClarityMode.clear -> when (variant) {
                Ios27GlassVariant.ReaderSheet -> (7.8f + opacity * 1.8f).coerceAtMost(maxFastBlurPx)
                else -> (7.2f + opacity * 1.4f).coerceAtMost(maxFastBlurPx)
            }
            GlassClarityMode.regular -> when (variant) {
                Ios27GlassVariant.ReaderSheet -> (8.8f + opacity * 1.7f).coerceAtMost(maxFastBlurPx)
                else -> (8.0f + opacity * 1.4f).coerceAtMost(maxFastBlurPx)
            }
            GlassClarityMode.tinted -> (9.2f + opacity * 1.3f).coerceAtMost(maxFastBlurPx)
        }
    }
    val fastBlurDp = (blurPx / safeDensity).dp
    // Zero resting dispersion eliminates 3x R/G/B `seen()` shader sampling (99-132 taps/px).
    val fastDispersion = 0f
    // Inline cards and pills skip Vitro's extra SDF shadow pass (`drawShadow`) and unify all
    // 11 GlassMaterial fields across Card/FusibleCard so GlassContainerNode merges them into 1 GlassGroup.
    val (fastShadowRadius, fastShadowOffset, fastShadowOpacity) = if (powerSave) {
        Triple(0.dp, 0.dp, 0f)
    } else {
        // All variants (including ReaderSheet, Card, Chrome) rely on frosted contrast + specular
        // hairline rims and skip Vitro's extra offscreen AGSL SDF shadow pass (`drawShadow`) so
        // rapid modal open/close transitions never trigger mid-animation SkSL shadow shader compilation or extra FBOs.
        Triple(0.dp, 0.dp, 0f)
    }
    return glass.copy(
        blurRadius = fastBlurDp,
        dispersion = fastDispersion,
        refractionHeight = if (powerSave) 0.dp else glass.refractionHeight,
        refractionAmount = if (powerSave) 0.dp else glass.refractionAmount,
        magnification = if (powerSave) 1f else glass.magnification,
        shadowRadius = fastShadowRadius,
        shadowOffset = fastShadowOffset,
        shadowOpacity = fastShadowOpacity,
        isInteractive = if (powerSave) false else glass.isInteractive,
    )
}

@Composable
fun rememberIos27Glass(
    style: ReadingStyle = LocalReadingStyle.current,
    palette: AppPalette,
    variant: Ios27GlassVariant = Ios27GlassVariant.Card,
    tintOverride: Color = Color.Unspecified,
    isInteractive: Boolean = false,
    appearanceOverride: GlassAppearance = GlassAppearance.Automatic,
): Glass {
    // Resolve Automatic to an explicit Dark/Light appearance so Vitro does not spawn per-frame
    // ToneReader + ImageReader GPU->CPU readbacks for known app themes.
    val envDarkTheme = LocalGlassDarkTheme.current
    val powerSave = LocalPowerSaveMode.current
    val density = LocalDensity.current.density.coerceAtLeast(1f)
    val resolvedAppearance = when (appearanceOverride) {
        GlassAppearance.Dark, GlassAppearance.Light -> appearanceOverride
        else -> if (envDarkTheme ?: !palette.isLight) GlassAppearance.Dark else GlassAppearance.Light
    }
    val raw = rememberRawIos27Glass(
        style = style,
        palette = palette,
        variant = variant,
        tintOverride = tintOverride,
        isInteractive = isInteractive,
        appearanceOverride = resolvedAppearance,
    )
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val clarity = style.glassClarityMode
    return remember(raw, clarity, opacity, variant, density, powerSave) {
        optimizeIos27GlassForRuntime(
            glass = raw,
            clarity = clarity,
            opacity = opacity,
            variant = variant,
            density = density,
            powerSave = powerSave,
        )
    }
}

@Composable
private fun rememberRawIos27Glass(
    style: ReadingStyle = LocalReadingStyle.current,
    palette: AppPalette,
    variant: Ios27GlassVariant = Ios27GlassVariant.Card,
    tintOverride: Color = Color.Unspecified,
    isInteractive: Boolean = false,
    appearanceOverride: GlassAppearance = GlassAppearance.Automatic,
): Glass {
    val envDarkTheme = LocalGlassDarkTheme.current
    val isDark = when (appearanceOverride) {
        GlassAppearance.Dark -> true
        GlassAppearance.Light -> false
        else -> envDarkTheme ?: !palette.isLight
    }
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val clarity = style.glassClarityMode

    return remember(
        clarity,
        opacity,
        isDark,
        palette.primary,
        variant,
        tintOverride,
        isInteractive,
        appearanceOverride,
    ) {
        val basePreset = if (clarity == GlassClarityMode.clear) Glass.Clear else Glass.Regular

        // Calibrate Light & Dark Mode strictly against Vitro's GlassShaders AGSL math:
        // In Dark Mode (`dark = 1`), `deep` lifts black via `veil * (0.19 * (1 - luma)^16)` and
        // illuminates the -45° specular rim multiplicatively via `deepVivid * (1 + edge * (0.35 + 0.12 / deepLit))`.
        // Crushing `brightness` below ~0.35 or applying heavy dark `tint`/`dimming` after edge lighting
        // extinguishes both body lift and specular rim, turning controls into flat black blobs on #000000.
        val baseBrightness = when (clarity) {
            GlassClarityMode.clear -> if (isDark) 0.42f + opacity * 0.22f else 0.020f + opacity * 0.022f
            GlassClarityMode.regular -> if (isDark) 0.56f + opacity * 0.24f else 0.034f + opacity * 0.032f
            GlassClarityMode.tinted -> if (isDark) 0.64f + opacity * 0.26f else 0.050f + opacity * 0.038f
        }
        val baseDimming = when (clarity) {
            GlassClarityMode.clear -> 0f
            GlassClarityMode.regular -> 0f
            GlassClarityMode.tinted -> if (isDark) 0f else 0.01f + opacity * 0.02f
        }
        val baseSaturation = when (clarity) {
            GlassClarityMode.clear -> if (isDark) 1.15f else 1.12f
            GlassClarityMode.regular -> if (isDark) 1.12f else 1.10f
            GlassClarityMode.tinted -> 1.06f
        }
        val baseHighlight = when (clarity) {
            GlassClarityMode.clear -> if (isDark) 0.72f else 0.68f
            GlassClarityMode.regular -> if (isDark) 0.64f else 0.60f
            GlassClarityMode.tinted -> if (isDark) 0.56f else 0.52f
        }
        val baseTintAlpha = ios27BaseTintAlpha(clarity = clarity, opacity = opacity, isDark = isDark)

        // Unify all 11 GlassMaterial fields across Chrome/SubtlePill/AccentPill/ReaderControl
        // so fused glassEffectContainer siblings share one GlassGroup without internal seams.
        // Restore true curved-edge lens refraction & subtle magnification while keeping blur fast.
        val refractionHeightDp = when (variant) {
            Ios27GlassVariant.Card,
            Ios27GlassVariant.FusibleCard -> 16.dp
            Ios27GlassVariant.ReaderSheet -> 22.dp
            else -> 14.dp
        }
        val refractionAmountDp = when (variant) {
            Ios27GlassVariant.Card,
            Ios27GlassVariant.FusibleCard -> 28.dp
            Ios27GlassVariant.ReaderSheet -> 38.dp
            else -> 24.dp
        }
        val magnification = when (variant) {
            Ios27GlassVariant.Card,
            Ios27GlassVariant.FusibleCard -> 1.03f
            Ios27GlassVariant.ReaderSheet -> 1.04f
            else -> 1.025f
        }
        val highlight = baseHighlight
        val brightness = when (variant) {
            Ios27GlassVariant.ReaderSheet -> if (isDark) {
                (baseBrightness + 0.08f).coerceAtMost(0.95f)
            } else {
                (baseBrightness + 0.010f).coerceAtMost(0.095f)
            }
            Ios27GlassVariant.AccentPill -> if (isDark) {
                (baseBrightness * 0.82f).coerceIn(0.36f, 0.74f)
            } else {
                baseBrightness
            }
            else -> baseBrightness
        }
        val dimming = when (variant) {
            Ios27GlassVariant.ReaderSheet -> if (isDark) 0.02f else baseDimming
            else -> baseDimming
        }
        val resolvedTint = when {
            tintOverride.isSpecified -> tintOverride
            variant == Ios27GlassVariant.AccentPill -> ios27AccentPillTint(
                palette = palette,
                baseTintAlpha = baseTintAlpha,
                opacity = opacity,
                isDark = isDark,
            )
            variant == Ios27GlassVariant.ReaderSheet -> ios27ReaderSheetTint(
                baseTintAlpha = baseTintAlpha,
                isDark = isDark,
            )
            else -> ios27ChromeTint(
                baseTintAlpha = baseTintAlpha,
                isDark = isDark,
            )
        }
        val shadowRadiusDp = when (variant) {
            Ios27GlassVariant.Card,
            Ios27GlassVariant.FusibleCard,
            Ios27GlassVariant.SubtlePill,
            Ios27GlassVariant.AccentPill -> 0.dp
            Ios27GlassVariant.ReaderSheet -> 16.dp
            else -> 14.dp
        }
        val shadowOffsetDp = when (variant) {
            Ios27GlassVariant.Card,
            Ios27GlassVariant.FusibleCard,
            Ios27GlassVariant.SubtlePill,
            Ios27GlassVariant.AccentPill -> 0.dp
            Ios27GlassVariant.ReaderSheet -> 6.dp
            else -> 4.dp
        }
        val shadowOpacity = when (variant) {
            Ios27GlassVariant.Card,
            Ios27GlassVariant.FusibleCard,
            Ios27GlassVariant.SubtlePill,
            Ios27GlassVariant.AccentPill -> 0f
            Ios27GlassVariant.ReaderSheet -> if (isDark) 0.34f else 0.14f
            else -> if (isDark) 0.26f else 0.10f
        }

        basePreset.copy(
            blurRadius = 10.dp,
            refractionHeight = refractionHeightDp,
            refractionAmount = refractionAmountDp,
            dispersion = 0f,
            magnification = magnification,
            saturation = baseSaturation,
            brightness = brightness,
            appearance = appearanceOverride,
            tint = resolvedTint,
            dimming = dimming,
            lightAngle = -45f,
            highlightIntensity = highlight,
            shadowRadius = shadowRadiusDp,
            shadowOffset = shadowOffsetDp,
            shadowOpacity = shadowOpacity,
            isInteractive = isInteractive,
        )
    }
}

@Composable
fun ios27ContrastBorderColor(
    palette: AppPalette,
    isSelected: Boolean = false,
    darkPaperOverride: Boolean? = null,
): Color {
    val isDark = darkPaperOverride ?: (LocalGlassDarkTheme.current ?: !palette.isLight)
    return when {
        isSelected && isDark -> palette.primarySoft.copy(alpha = 0.38f)
        isSelected && !isDark -> palette.primary.copy(alpha = 0.28f)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color(0xFF0E1117).copy(alpha = 0.055f)
    }
}

private fun ios27BaseTintAlpha(
    clarity: GlassClarityMode,
    opacity: Float,
    isDark: Boolean,
): Float = when (clarity) {
    GlassClarityMode.clear -> if (isDark) 0.02f + opacity * 0.08f else 0.15f + opacity * 0.12f
    GlassClarityMode.regular -> if (isDark) 0.14f + opacity * 0.14f else 0.36f + opacity * 0.16f
    GlassClarityMode.tinted -> if (isDark) 0.34f + opacity * 0.18f else 0.58f + opacity * 0.18f
}

private fun ios27ChromeTint(baseTintAlpha: Float, isDark: Boolean): Color {
    val baseColor = if (isDark) Color(0xFF2B2D38) else Color(0xFFFAFAFC)
    return baseColor.copy(
        alpha = baseTintAlpha.coerceIn(if (isDark) 0.02f else 0.15f, if (isDark) 0.54f else 0.78f),
    )
}

private fun ios27ReaderSheetTint(baseTintAlpha: Float, isDark: Boolean): Color {
    val baseColor = if (isDark) Color(0xFF23252E) else Color(0xFFF8F9FC)
    val sheetAlpha = if (isDark) {
        (baseTintAlpha + 0.22f).coerceIn(0.32f, 0.68f)
    } else {
        (baseTintAlpha + 0.32f).coerceIn(0.48f, 0.86f)
    }
    return baseColor.copy(alpha = sheetAlpha)
}

private fun ios27AccentPillTint(
    palette: AppPalette,
    baseTintAlpha: Float,
    opacity: Float,
    isDark: Boolean,
): Color = if (isDark) {
    val jewelTint = lerpColor(palette.primary, palette.primarySoft, 0.28f)
    val accentAlpha = (0.28f + opacity * 0.16f).coerceIn(0.28f, 0.48f)
    jewelTint.copy(alpha = accentAlpha)
} else {
    val accentAlpha = (baseTintAlpha * 0.65f + 0.08f).coerceIn(0.14f, 0.58f)
    palette.primary.copy(alpha = accentAlpha)
}

/**
 * Returns (unselectedTint, selectedTint) for filter pills so tabs can animate smoothly
 * via `tintOverride` without changing [Ios27GlassVariant] or re-mounting `GlassNode`.
 */
@Composable
fun ios27FilterPillTints(
    palette: AppPalette,
    style: ReadingStyle,
): Pair<Color, Color> {
    val isDark = LocalGlassDarkTheme.current ?: !palette.isLight
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val baseTintAlpha = ios27BaseTintAlpha(style.glassClarityMode, opacity, isDark)
    return ios27ChromeTint(baseTintAlpha, isDark) to
        ios27AccentPillTint(palette, baseTintAlpha, opacity, isDark)
}

/**
 * Guards Vitro [GlassEffectState] against getting stuck enlarged (`scale > 1.0x`) when:
 * 1. An interactive glass surface detaches mid-press (e.g. `hidden = true`, menu/sheet swap, tab hide)
 *    before the release spring completes and re-attaches later with stale `hold > 0`.
 * 2. A pointer interaction cancels or gets stolen by a parent scroll/overlay without resetting `hold`.
 */
@Composable
fun rememberSafeGlassEffectState(
    interactionSource: InteractionSource? = null,
    active: Boolean = true,
    resetKey: Any? = Unit,
): GlassEffectState {
    var resetEpoch by remember(resetKey) { mutableIntStateOf(0) }
    var wasActive by remember(resetKey) { mutableStateOf(active) }
    if (wasActive != active) {
        wasActive = active
        resetEpoch++
    }

    val state = key(resetEpoch, resetKey) {
        rememberGlassEffectState()
    }

    var isInteractionPressed by remember(resetEpoch, resetKey) { mutableStateOf(false) }
    LaunchedEffect(interactionSource, active, resetEpoch, resetKey) {
        isInteractionPressed = false
        if (!active || interactionSource == null) return@LaunchedEffect
        var activePress: PressInteraction.Press? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    activePress = interaction
                    isInteractionPressed = true
                }
                is PressInteraction.Release -> {
                    if (activePress == null || interaction.press == activePress) {
                        activePress = null
                        isInteractionPressed = false
                    }
                }
                is PressInteraction.Cancel -> {
                    if (activePress == null || interaction.press == activePress) {
                        activePress = null
                        isInteractionPressed = false
                    }
                }
            }
        }
    }

    LaunchedEffect(active, interactionSource, isInteractionPressed, state) {
        if (!active || interactionSource == null) {
            isInteractionPressed = false
            return@LaunchedEffect
        }
        // 仅在手指已完全离开(isInteractionPressed == false)却因取消事件丢失导致 Vitro 永久卡在 isHeld==true
        // 超过 2.4s 时才执行兜底重置；通过 snapshotFlow 观察 state.isHeld，不在组合阶段读取动画状态。
        if (!isInteractionPressed) {
            snapshotFlow { state.isHeld }.collectLatest { held ->
                if (held) {
                    delay(2400L)
                    if (state.isHeld && !isInteractionPressed) {
                        resetEpoch++
                    }
                }
            }
        }
    }

    return state
}

private class VitroSpotlightRenderCache {
    var cachedShape: Shape? = null
    var cachedSize: Size = Size.Unspecified
    var cachedLayoutDirection: LayoutDirection? = null
    var cachedDensity: Float = -1f
    val outlinePath: Path = Path()
    val rimPath: Path = Path()
    var cachedRadiusPx: Float = -1f
    var cachedAccentColor: Color = Color.Unspecified
    var bloom1Brush: Brush? = null
    var bloom2Brush: Brush? = null
    var rimBrush: Brush? = null
}

val LocalHeroBookSlotAlpha = androidx.compose.runtime.staticCompositionLocalOf<(String) -> Float> { { 1f } }

@Composable
fun Modifier.vitroFingerSpotlight(
    shape: Shape = RoundedCornerShape(24.dp),
    accentColor: Color,
    isDarkSurface: Boolean,
    enabled: Boolean = true,
    pointerOverride: Offset? = null,
    pressedOverride: Boolean = false,
    externalPointer: Boolean = false,
    maxRadiusDp: Dp = 76.dp,
    intensity: Float = 1f,
    pointerProvider: (() -> Offset)? = null,
): Modifier {
    val currentEnabled by rememberUpdatedState(enabled)
    var pointerPos by remember { mutableStateOf(Offset.Unspecified) }
    var pointerActive by remember { mutableStateOf(false) }
    val glowAnim = remember { Animatable(0f) }
    val maxAlpha = intensity.coerceIn(0f, 1.35f)
    val renderCache = remember(isDarkSurface, maxAlpha) {
        VitroSpotlightRenderCache()
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            pointerActive = false
        }
    }

    val effectivePressed = enabled && if (externalPointer) pressedOverride else pointerActive
    LaunchedEffect(effectivePressed) {
        if (effectivePressed) {
            glowAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 55),
            )
        } else {
            glowAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 190),
            )
        }
    }

    val trackingModifier = if (externalPointer) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            val marginPx = 28.dp.toPx()
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (!currentEnabled) {
                        if (pointerActive) pointerActive = false
                        continue
                    }
                    val active = event.changes.firstOrNull { it.pressed }
                    if (active != null) {
                        val p = active.position
                        val inside = p.x >= -marginPx && p.x <= size.width + marginPx &&
                            p.y >= -marginPx && p.y <= size.height + marginPx
                        if (inside || pointerActive) {
                            pointerPos = p
                            pointerActive = inside
                        }
                    } else if (pointerActive) {
                        pointerActive = false
                    }
                }
            }
        }
    }

    return this
        .then(trackingModifier)
        .drawWithContent {
            drawContent()
            val rawGlow = glowAnim.value
            val alpha = (rawGlow * intensity).coerceIn(0f, 1.35f)
            val pos = if (externalPointer) {
                pointerProvider?.invoke() ?: pointerOverride ?: Offset.Unspecified
            } else {
                pointerPos
            }
            if (alpha <= 0.004f || pos == Offset.Unspecified || size.minDimension <= 1f) {
                return@drawWithContent
            }

            val minDim = size.minDimension
            val radiusPx = (minDim * 0.72f).coerceIn(38.dp.toPx(), maxRadiusDp.toPx())
            val rimRadiusPx = radiusPx * 1.10f
            val tintAccent = if (accentColor.isSpecified && accentColor != Color.White) {
                accentColor
            } else {
                Color(0xFF7B93C4)
            }
            if (renderCache.cachedSize != size ||
                renderCache.cachedLayoutDirection != layoutDirection ||
                renderCache.cachedDensity != density ||
                renderCache.cachedShape != shape
            ) {
                val outline = shape.createOutline(size, layoutDirection, this)
                renderCache.outlinePath.rewind()
                renderCache.outlinePath.addOutline(outline)
                renderCache.cachedSize = size
                renderCache.cachedLayoutDirection = layoutDirection
                renderCache.cachedDensity = density
                renderCache.cachedShape = shape
            }
            if (renderCache.cachedRadiusPx != radiusPx ||
                renderCache.cachedAccentColor != tintAccent ||
                renderCache.bloom1Brush == null
            ) {
                renderCache.cachedRadiusPx = radiusPx
                renderCache.cachedAccentColor = tintAccent
                if (isDarkSurface) {
                    renderCache.bloom1Brush = Brush.radialGradient(
                        0.0f to lerpColor(Color.White, tintAccent, 0.16f).copy(alpha = (0.40f * maxAlpha).coerceAtMost(0.54f)),
                        0.42f to lerpColor(Color.White, tintAccent, 0.22f).copy(alpha = (0.16f * maxAlpha).coerceAtMost(0.24f)),
                        1.0f to Color.Transparent,
                        center = Offset.Zero,
                        radius = radiusPx,
                    )
                    renderCache.bloom2Brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = (0.52f * maxAlpha).coerceAtMost(0.68f)),
                        0.52f to Color.White.copy(alpha = (0.16f * maxAlpha).coerceAtMost(0.24f)),
                        1.0f to Color.Transparent,
                        center = Offset.Zero,
                        radius = radiusPx * 0.56f,
                    )
                    renderCache.rimBrush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = (0.82f * maxAlpha).coerceAtMost(0.95f)),
                        0.52f to lerpColor(Color.White, tintAccent, 0.22f).copy(alpha = (0.34f * maxAlpha).coerceAtMost(0.46f)),
                        1.0f to Color.Transparent,
                        center = Offset.Zero,
                        radius = rimRadiusPx,
                    )
                } else {
                    renderCache.bloom1Brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = (0.76f * maxAlpha).coerceAtMost(0.90f)),
                        0.34f to lerpColor(Color.White, tintAccent, 0.26f).copy(alpha = (0.28f * maxAlpha).coerceAtMost(0.40f)),
                        0.70f to lerpColor(Color(0xFFDCE2EE), tintAccent, 0.22f).copy(alpha = (0.14f * maxAlpha).coerceAtMost(0.22f)),
                        1.0f to Color.Transparent,
                        center = Offset.Zero,
                        radius = radiusPx,
                    )
                    renderCache.bloom2Brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = (0.88f * maxAlpha).coerceAtMost(0.96f)),
                        0.55f to Color.White.copy(alpha = (0.26f * maxAlpha).coerceAtMost(0.38f)),
                        1.0f to Color.Transparent,
                        center = Offset.Zero,
                        radius = radiusPx * 0.50f,
                    )
                    renderCache.rimBrush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = (0.90f * maxAlpha).coerceAtMost(0.98f)),
                        0.45f to lerpColor(Color(0xFFC8D0E0), tintAccent, 0.34f).copy(alpha = (0.42f * maxAlpha).coerceAtMost(0.56f)),
                        1.0f to Color.Transparent,
                        center = Offset.Zero,
                        radius = rimRadiusPx,
                    )
                }
            }

            val glowT = rawGlow.coerceIn(0f, 1f)
            val outlinePath = renderCache.outlinePath
            val rimPath = renderCache.rimPath
            rimPath.rewind()
            rimPath.addPath(outlinePath, Offset(-pos.x, -pos.y))

            clipPath(outlinePath) {
                withTransform({ translate(left = pos.x, top = pos.y) }) {
                    renderCache.bloom1Brush?.let { bloom1 ->
                        drawCircle(
                            brush = bloom1,
                            radius = radiusPx,
                            center = Offset.Zero,
                            alpha = glowT,
                        )
                    }
                    renderCache.bloom2Brush?.let { bloom2 ->
                        drawCircle(
                            brush = bloom2,
                            radius = radiusPx * (if (isDarkSurface) 0.56f else 0.50f),
                            center = Offset.Zero,
                            alpha = glowT,
                        )
                    }
                    renderCache.rimBrush?.let { rim ->
                        drawPath(
                            path = rimPath,
                            brush = rim,
                            alpha = glowT,
                            style = Stroke(width = (if (isDarkSurface) 1.65.dp else 1.75.dp).toPx()),
                        )
                    }
                }
            }
        }
}

@Composable
fun Modifier.ios27GlassSurface(
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
    variant: Ios27GlassVariant = Ios27GlassVariant.Card,
    backdrop: GlassBackdrop? = LocalVitroBackdrop.current,
    tintOverride: Color = Color.Unspecified,
    isInteractive: Boolean = false,
    state: GlassEffectState? = null,
    interactionSource: InteractionSource? = null,
    showContrastBorder: Boolean = false,
    darkPaperOverride: Boolean? = null,
    shadowScale: Float = 1f,
    isHidden: Boolean = false,
    suspendVitroBlur: Boolean = false,
    showFingerHighlight: Boolean = isInteractive,
    fingerPointerOverride: Offset? = null,
    fingerPressedOverride: Boolean = false,
    externalFingerControlled: Boolean = false,
    fingerPointerProvider: (() -> Offset)? = null,
): Modifier {
    val isDark = darkPaperOverride ?: (LocalGlassDarkTheme.current ?: !palette.isLight)
    val powerSave = LocalPowerSaveMode.current
    val effectiveBackdrop = if (powerSave) null else backdrop
    val appearanceOverride = if (isDark) GlassAppearance.Dark else GlassAppearance.Light
    val glass = rememberIos27Glass(
        style = style,
        palette = palette,
        variant = variant,
        tintOverride = tintOverride,
        isInteractive = isInteractive,
        appearanceOverride = appearanceOverride,
    )
    val resolvedIsDarkSurface = darkPaperOverride ?: !palette.isLight
    val spotlightModifier = if (showFingerHighlight && !powerSave) {
        Modifier.vitroFingerSpotlight(
            shape = shape,
            accentColor = palette.primary,
            isDarkSurface = resolvedIsDarkSurface,
            enabled = !isHidden && !suspendVitroBlur,
            pointerOverride = fingerPointerOverride,
            pressedOverride = fingerPressedOverride,
            externalPointer = externalFingerControlled,
            pointerProvider = fingerPointerProvider,
        )
    } else {
        Modifier
    }
    val needFallbackVisual = effectiveBackdrop == null || (suspendVitroBlur && !isHidden)
    val needFallbackHairline = showContrastBorder ||
        variant == Ios27GlassVariant.Card ||
        variant == Ios27GlassVariant.FusibleCard ||
        variant == Ios27GlassVariant.ReaderSheet ||
        variant == Ios27GlassVariant.SubtlePill ||
        variant == Ios27GlassVariant.AccentPill
    val contrastBorder = ios27ContrastBorderColor(
        palette = palette,
        isSelected = variant == Ios27GlassVariant.AccentPill,
        darkPaperOverride = darkPaperOverride,
    )
    val rimBrush = if (needFallbackHairline) {
        remember(isDark, contrastBorder) {
            if (isDark) {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.14f),
                    0.45f to contrastBorder,
                    1.0f to Color.White.copy(alpha = 0.05f),
                )
            } else {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.24f),
                    0.45f to contrastBorder,
                    1.0f to contrastBorder,
                )
            }
        }
    } else {
        null
    }
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val fallbackAlpha = when (style.glassClarityMode) {
        GlassClarityMode.clear -> 0.56f + opacity * 0.20f
        GlassClarityMode.regular -> 0.74f + opacity * 0.16f
        GlassClarityMode.tinted -> 0.86f + opacity * 0.12f
    }.coerceIn(0.45f, 0.96f)
    val fallbackColor = when {
        tintOverride.isSpecified && tintOverride.alpha > 0.05f -> tintOverride
        variant == Ios27GlassVariant.AccentPill -> if (isDark) {
            lerpColor(palette.primary, palette.primarySoft, 0.28f).copy(alpha = 0.34f + opacity * 0.16f)
        } else {
            palette.primary.copy(alpha = 0.20f + opacity * 0.18f)
        }
        isDark -> Color(0xFF262832).copy(alpha = fallbackAlpha)
        else -> Color(0xFFFDFDFF).copy(alpha = fallbackAlpha)
    }
    if (effectiveBackdrop == null) {
        val borderMod = if (rimBrush != null) {
            Modifier.border(
                width = 0.5.dp,
                brush = rimBrush,
                shape = shape,
            )
        } else {
            Modifier
        }
        return this
            .clip(shape)
            .then(
                if (needFallbackVisual) {
                    Modifier
                        .background(fallbackColor, shape)
                        .then(borderMod)
                } else {
                    Modifier
                },
            )
            .then(spotlightModifier)
    }

    // Keep modifier node topology identical before .glassEffect so toggling suspendVitroBlur
    // never detaches/re-attaches GlassNode (which resets ShaderRenderer held size to Zero).
    val latestNeedFallback by rememberUpdatedState(needFallbackVisual)
    val latestFallbackColor by rememberUpdatedState(fallbackColor)
    val latestRimBrush by rememberUpdatedState(rimBrush)
    val fallbackOutlineCache = remember(shape) {
        object {
            var size: Size = Size.Unspecified
            var layoutDirection: LayoutDirection? = null
            var density: Float = -1f
            var outline: Outline? = null
            var strokePx: Float = -1f
            var stroke: Stroke? = null
        }
    }
    val stablePreGlassFallbackModifier = Modifier.drawBehind {
        if (!latestNeedFallback || size.width <= 1f || size.height <= 1f) return@drawBehind
        if (fallbackOutlineCache.outline == null ||
            fallbackOutlineCache.size != size ||
            fallbackOutlineCache.layoutDirection != layoutDirection ||
            fallbackOutlineCache.density != density
        ) {
            fallbackOutlineCache.outline = shape.createOutline(size, layoutDirection, this)
            fallbackOutlineCache.size = size
            fallbackOutlineCache.layoutDirection = layoutDirection
            fallbackOutlineCache.density = density
        }
        val outline = fallbackOutlineCache.outline ?: return@drawBehind
        drawOutline(outline = outline, color = latestFallbackColor)
        val brush = latestRimBrush
        if (brush != null) {
            val wPx = 0.5.dp.toPx()
            val stroke = fallbackOutlineCache.stroke?.takeIf { fallbackOutlineCache.strokePx == wPx }
                ?: Stroke(width = wPx).also {
                    fallbackOutlineCache.stroke = it
                    fallbackOutlineCache.strokePx = wPx
                }
            drawOutline(outline = outline, brush = brush, style = stroke)
        }
    }

    val resolvedState = state ?: if (isInteractive && !powerSave) {
        rememberSafeGlassEffectState(
            interactionSource = interactionSource,
            active = true,
        )
    } else {
        null
    }

    val effectiveGlass = if (isHidden || suspendVitroBlur) {
        Glass.Identity
    } else if (shadowScale < 0.999f) {
        val clampedShadow = shadowScale.coerceIn(0f, 1f)
        glass.copy(
            shadowRadius = glass.shadowRadius * clampedShadow,
            shadowOffset = glass.shadowOffset * clampedShadow,
            shadowOpacity = glass.shadowOpacity * clampedShadow,
        )
    } else {
        glass
    }
    return this
        .then(stablePreGlassFallbackModifier)
        .glassEffect(
            backdrop = effectiveBackdrop,
            glass = effectiveGlass,
            shape = shape,
            state = resolvedState,
            interactionSource = interactionSource,
        )
        .then(spotlightModifier)
}

private class Ios27DockElevationRenderCache {
    var cachedSize: Size = Size.Unspecified
    var cachedLayoutDirection: LayoutDirection? = null
    var cachedDensity: Float = -1f
    var cachedShape: Shape? = null
    var outline: Outline? = null
    val clipPath: Path = Path()
    var androidPath: android.graphics.Path? = null
    val insetRimPath: Path = Path()
    var cachedRimStrokePx: Float = -1f
    var hasUniformInsetPath: Boolean = false
    var cachedRimStroke: Stroke? = null
    var ambientBlurPx: Float = -1f
    var ambientPaint: AndroidPaint? = null
    var spotBlurPx: Float = -1f
    var spotPaint: AndroidPaint? = null
    var innerPillBlurPx: Float = -1f
    var innerPillPaint: AndroidPaint? = null

    fun resolveRimStroke(strokeWidthPx: Float): Stroke {
        val existing = cachedRimStroke
        if (existing != null && cachedRimStrokePx == strokeWidthPx) {
            return existing
        }
        return Stroke(width = strokeWidthPx).also {
            cachedRimStroke = it
        }
    }

    fun updateGeometry(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        rimStrokePx: Float,
    ) {
        if (cachedSize == size &&
            cachedLayoutDirection == layoutDirection &&
            cachedDensity == density.density &&
            cachedShape == shape &&
            cachedRimStrokePx == rimStrokePx
        ) {
            return
        }
        val resolvedOutline = shape.createOutline(size, layoutDirection, density)
        outline = resolvedOutline
        clipPath.rewind()
        clipPath.addOutline(resolvedOutline)
        androidPath = clipPath.asAndroidPath()

        insetRimPath.rewind()
        hasUniformInsetPath = false
        if (resolvedOutline is Outline.Rounded && rimStrokePx > 0f) {
            val rr = resolvedOutline.roundRect
            val half = rimStrokePx * 0.5f
            if (rr.width > rimStrokePx && rr.height > rimStrokePx) {
                insetRimPath.addRoundRect(
                    RoundRect(
                        left = rr.left + half,
                        top = rr.top + half,
                        right = rr.right - half,
                        bottom = rr.bottom - half,
                        topLeftCornerRadius = CornerRadius(
                            (rr.topLeftCornerRadius.x - half).coerceAtLeast(0f),
                            (rr.topLeftCornerRadius.y - half).coerceAtLeast(0f),
                        ),
                        topRightCornerRadius = CornerRadius(
                            (rr.topRightCornerRadius.x - half).coerceAtLeast(0f),
                            (rr.topRightCornerRadius.y - half).coerceAtLeast(0f),
                        ),
                        bottomRightCornerRadius = CornerRadius(
                            (rr.bottomRightCornerRadius.x - half).coerceAtLeast(0f),
                            (rr.bottomRightCornerRadius.y - half).coerceAtLeast(0f),
                        ),
                        bottomLeftCornerRadius = CornerRadius(
                            (rr.bottomLeftCornerRadius.x - half).coerceAtLeast(0f),
                            (rr.bottomLeftCornerRadius.y - half).coerceAtLeast(0f),
                        ),
                    ),
                )
                hasUniformInsetPath = true
            }
        }
        cachedSize = size
        cachedLayoutDirection = layoutDirection
        cachedDensity = density.density
        cachedShape = shape
        cachedRimStrokePx = rimStrokePx
    }

    fun ensureAmbientPaint(blurPx: Float): AndroidPaint {
        val existing = ambientPaint
        if (existing != null && ambientBlurPx == blurPx) return existing
        return AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(blurPx.coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
        }.also {
            ambientBlurPx = blurPx
            ambientPaint = it
        }
    }

    fun ensureSpotPaint(blurPx: Float): AndroidPaint {
        val existing = spotPaint
        if (existing != null && spotBlurPx == blurPx) return existing
        return AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(blurPx.coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
        }.also {
            spotBlurPx = blurPx
            spotPaint = it
        }
    }

    fun ensureInnerPillPaint(blurPx: Float): AndroidPaint {
        val existing = innerPillPaint
        if (existing != null && innerPillBlurPx == blurPx) return existing
        return AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(blurPx.coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
        }.also {
            innerPillBlurPx = blurPx
            innerPillPaint = it
        }
    }
}

@Composable
private fun rememberDockOpticalRimBrush(isDark: Boolean): Brush = remember(isDark) {
    if (isDark) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.24f),
            0.35f to Color.White.copy(alpha = 0.07f),
            1f to Color.White.copy(alpha = 0.03f),
        )
    } else {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.62f),
            0.36f to Color.Black.copy(alpha = 0.045f),
            1f to Color.Black.copy(alpha = 0.075f),
        )
    }
}

private fun DrawScope.drawNativeOutlineShadowPass(
    cache: Ios27DockElevationRenderCache,
    dy: Float,
    spreadX: Float,
    paint: AndroidPaint,
    nativeCanvas: android.graphics.Canvas,
) {
    val resolvedOutline = cache.outline
    if (resolvedOutline is Outline.Rounded) {
        val rr = resolvedOutline.roundRect
        val tlx = rr.topLeftCornerRadius.x
        val sameRadii = tlx == rr.topLeftCornerRadius.y &&
            tlx == rr.topRightCornerRadius.x &&
            tlx == rr.topRightCornerRadius.y &&
            tlx == rr.bottomLeftCornerRadius.x &&
            tlx == rr.bottomLeftCornerRadius.y &&
            tlx == rr.bottomRightCornerRadius.x &&
            tlx == rr.bottomRightCornerRadius.y
        if (sameRadii) {
            nativeCanvas.drawRoundRect(
                rr.left - spreadX,
                rr.top + dy,
                rr.right + spreadX,
                rr.bottom + dy,
                tlx,
                tlx,
                paint,
            )
            return
        }
    }
    val androidPath = cache.androidPath ?: return
    nativeCanvas.save()
    nativeCanvas.translate(0f, dy)
    nativeCanvas.drawPath(androidPath, paint)
    nativeCanvas.restore()
}

private fun DrawScope.drawIos27DockOuterShadows(
    cache: Ios27DockElevationRenderCache,
    shape: Shape,
    isDark: Boolean,
    shadowScale: Float,
) {
    val scale = shadowScale.coerceIn(0f, 1.5f)
    if (scale <= 0.004f || size.width <= 1f || size.height <= 1f) return
    val nativeCanvas = drawContext.canvas.nativeCanvas
    if (!nativeCanvas.isHardwareAccelerated) return

    val rimStrokePx = 0.65.dp.toPx()
    cache.updateGeometry(shape, size, layoutDirection, this, rimStrokePx)

    val ambientBlurPx = (if (isDark) 12.dp else 14.dp).toPx()
    val spotBlurPx = (if (isDark) 4.dp else 5.dp).toPx()
    val ambientPaint = cache.ensureAmbientPaint(ambientBlurPx)
    val spotPaint = cache.ensureSpotPaint(spotBlurPx)

    val ambientAlpha = (if (isDark) 0.20f else 0.065f) * scale
    val spotAlpha = (if (isDark) 0.14f else 0.045f) * scale
    ambientPaint.color = android.graphics.Color.argb((ambientAlpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
    spotPaint.color = android.graphics.Color.argb((spotAlpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)

    val ambientDy = (if (isDark) 3.5.dp else 4.dp).toPx()
    val spotDy = (if (isDark) 1.dp else 1.5.dp).toPx()
    val ambientSpreadX = (if (isDark) 0.5.dp else 1.dp).toPx()

    clipPath(cache.clipPath, clipOp = ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val hwCanvas = canvas.nativeCanvas
            drawNativeOutlineShadowPass(cache, ambientDy, ambientSpreadX, ambientPaint, hwCanvas)
            drawNativeOutlineShadowPass(cache, spotDy, 0f, spotPaint, hwCanvas)
        }
    }
}

private fun DrawScope.drawIos27DockOpticalRim(
    cache: Ios27DockElevationRenderCache,
    shape: Shape,
    rimBrush: Brush,
    rimAlpha: Float = 1f,
) {
    val alpha = rimAlpha.coerceIn(0f, 1f)
    if (alpha <= 0.004f || size.width <= 2f || size.height <= 2f) return
    val rimStrokePx = 0.65.dp.toPx()
    val stroke = cache.resolveRimStroke(rimStrokePx)
    cache.updateGeometry(shape, size, layoutDirection, this, rimStrokePx)
    if (cache.hasUniformInsetPath) {
        drawPath(
            path = cache.insetRimPath,
            brush = rimBrush,
            alpha = alpha,
            style = stroke,
        )
    } else {
        val outline = cache.outline ?: return
        clipPath(cache.clipPath) {
            drawOutline(
                outline = outline,
                brush = rimBrush,
                alpha = alpha,
                style = stroke,
            )
        }
    }
}

private fun DrawScope.drawIos27InnerPillRestingShadow(
    cache: Ios27DockElevationRenderCache,
    shape: Shape,
    isDark: Boolean,
    shadowScale: Float,
) {
    val scale = shadowScale.coerceIn(0f, 1.5f)
    if (scale <= 0.004f || size.width <= 1f || size.height <= 1f) return
    val nativeCanvas = drawContext.canvas.nativeCanvas
    if (!nativeCanvas.isHardwareAccelerated) return

    cache.updateGeometry(shape, size, layoutDirection, this, 0f)
    val paint = cache.ensureInnerPillPaint(3.5.dp.toPx())
    val restAlpha = (if (isDark) 0.18f else 0.055f) * scale
    paint.color = android.graphics.Color.argb((restAlpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
    val dy = 1.dp.toPx()

    clipPath(cache.clipPath, clipOp = ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            drawNativeOutlineShadowPass(cache, dy, 0f, paint, canvas.nativeCanvas)
        }
    }
}

/**
 * Resolves the milky frosted surface tint shared with `FlClashNavigationDock`
 * (`0xFFECEDF3` clarity-modulated frost in Light Mode, deep specular `ios27ChromeTint` in Dark Mode).
 */
@Composable
fun ios27DockMilkyTintColor(
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
): Color {
    val isDark = LocalGlassDarkTheme.current ?: !palette.isLight
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    return if (!isDark) {
        val a = when (style.glassClarityMode) {
            GlassClarityMode.clear -> 0.68f + opacity * 0.14f
            GlassClarityMode.regular -> 0.80f + opacity * 0.10f
            GlassClarityMode.tinted -> 0.87f + opacity * 0.07f
        }
        Color(0xFFECEDF3).copy(alpha = a.coerceIn(0.64f, 0.94f))
    } else {
        val baseTintAlpha = ios27BaseTintAlpha(style.glassClarityMode, opacity, isDark = true)
        ios27ChromeTint(baseTintAlpha, isDark = true)
    }
}

/**
 * Resolves the lifted resting inner capsule color shared with `FlClashNavigationDock`'s
 * selected lens (`Color.White` milky capsule in Light Mode, luminous ice frost in Dark Mode).
 */
@Composable
fun ios27DockRestingPillColor(
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
): Color {
    val isDark = LocalGlassDarkTheme.current ?: !palette.isLight
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    return if (!isDark) {
        val a = when (style.glassClarityMode) {
            GlassClarityMode.clear -> 0.84f + opacity * 0.08f
            GlassClarityMode.regular -> 0.88f + opacity * 0.07f
            GlassClarityMode.tinted -> 0.91f + opacity * 0.05f
        }
        Color.White.copy(alpha = a.coerceIn(0.82f, 0.96f))
    } else {
        val a = when (style.glassClarityMode) {
            GlassClarityMode.clear -> 0.14f + opacity * 0.08f
            GlassClarityMode.regular -> 0.19f + opacity * 0.10f
            GlassClarityMode.tinted -> 0.25f + opacity * 0.12f
        }
        Color(0xFFF0F2FA).copy(alpha = a.coerceIn(0.13f, 0.39f))
    }
}

/**
 * Reusable 3D frosted Dock-style card/container surface replicating `FlClashNavigationDock`:
 * milky `0xFFECEDF3` frost + `ClipOp.Difference` two-tier ambient & spot outer shadows +
 * `0.65.dp` top-bright vertical optical rim in Light Mode, and deep specular frost in Dark Mode.
 */
@Composable
fun Modifier.ios27ElevatedDockSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
    backdrop: GlassBackdrop? = LocalVitroBackdrop.current,
    shadowScale: Float = 1f,
    isInteractive: Boolean = false,
    interactionSource: InteractionSource? = null,
    showFingerHighlight: Boolean = isInteractive,
    tintOverride: Color = Color.Unspecified,
): Modifier {
    val isDark = LocalGlassDarkTheme.current ?: !palette.isLight
    val powerSave = LocalPowerSaveMode.current
    val effectiveBackdrop = if (powerSave) null else backdrop
    val opacity = style.glassOpacityLevel.coerceIn(0f, 1f)
    val effectiveTint = when {
        tintOverride.isSpecified -> tintOverride
        effectiveBackdrop != null -> ios27DockMilkyTintColor(palette = palette, style = style)
        isDark -> Color(0xFF262832).copy(alpha = (0.80f + opacity * 0.10f).coerceIn(0.76f, 0.92f))
        else -> Color(0xFFECEDF3).copy(alpha = (0.86f + opacity * 0.08f).coerceIn(0.84f, 0.95f))
    }
    val glassShape: CornerBasedShape = (shape as? CornerBasedShape)
        ?: if (shape == RectangleShape) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp)
    val shadowCache = remember { Ios27DockElevationRenderCache() }
    val rimCache = remember { Ios27DockElevationRenderCache() }
    val rimBrush = rememberDockOpticalRimBrush(isDark = isDark)
    val spotlightModifier = if (showFingerHighlight && !powerSave) {
        Modifier.vitroFingerSpotlight(
            shape = shape,
            accentColor = palette.primary,
            isDarkSurface = isDark,
            enabled = true,
        )
    } else {
        Modifier
    }

    return this
        .drawBehind {
            if (!powerSave && shadowScale > 0.004f) {
                drawIos27DockOuterShadows(
                    cache = shadowCache,
                    shape = shape,
                    isDark = isDark,
                    shadowScale = shadowScale,
                )
            }
        }
        .ios27GlassSurface(
            shape = glassShape,
            palette = palette,
            style = style,
            variant = Ios27GlassVariant.Chrome,
            backdrop = effectiveBackdrop,
            tintOverride = effectiveTint,
            isInteractive = isInteractive,
            interactionSource = interactionSource,
            showContrastBorder = false,
            shadowScale = 0f,
            showFingerHighlight = false,
        )
        .drawBehind {
            drawIos27DockOpticalRim(
                cache = rimCache,
                shape = shape,
                rimBrush = rimBrush,
                rimAlpha = 1f,
            )
        }
        .then(spotlightModifier)
}

/**
 * Lifted inner active capsule matching `FlClashNavigationDock`'s selected pill:
 * resting outer `ClipOp.Difference` drop shadow, bright `Color.White` / dark frost fill, and hairline edge.
 */
@Composable
fun Modifier.ios27LiftedInnerPillSurface(
    shape: Shape = CircleShape,
    palette: AppPalette,
    style: ReadingStyle = LocalReadingStyle.current,
    lifted: Boolean = true,
    shadowScale: Float = 1f,
    tintOverride: Color = Color.Unspecified,
): Modifier {
    if (!lifted) return this
    val isDark = LocalGlassDarkTheme.current ?: !palette.isLight
    val powerSave = LocalPowerSaveMode.current
    val clampedShadow = shadowScale.coerceIn(0f, 1.5f)
    val fillColor = if (tintOverride.isSpecified) {
        tintOverride
    } else {
        ios27DockRestingPillColor(palette = palette, style = style)
    }
    val edgeColor = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }
    val shadowCache = remember { Ios27DockElevationRenderCache() }
    return this
        .drawBehind {
            if (!powerSave && clampedShadow > 0.004f) {
                drawIos27InnerPillRestingShadow(
                    cache = shadowCache,
                    shape = shape,
                    isDark = isDark,
                    shadowScale = clampedShadow,
                )
            }
        }
        .clip(shape)
        .background(color = fillColor, shape = shape)
        .border(width = 0.5.dp, color = edgeColor, shape = shape)
}

/**
 * Android 13+ 原版 FlClash `liquid_glass.frag` 屏幕级双纹理球面折射与中心漫射着色器：
 * - 采样与屏幕坐标 1:1 对齐的 `uSharp` 与 `uHaze`，绝无任何图像拉伸或边缘钳制紫框；
 * - 中心区域（inset > uBevel）：融合统一的 `uTint` 与 `uHaze`，保证 5 大组件背景色调完全统一、文字极高可读性；
 * - 四周曲面倒角（0 < inset <= uBevel）：按球面透镜曲率 `depth = uRefraction * (1 - sqrt(1 - rise^2))`
 *   弯曲采样 `uSharp(screenPt + shift ± part)`，叠加左右曲率暗角 `EndShade`、上下高光带 `Rim Shine`、
 *   对角圆角高光弧 `ShineAround`、触控流光 `uGlow` 与 `0.85px` 亚像素玻璃亮边 `Hairline`。
 */
private const val AGSL_IOS26_SCREEN_REFRACT_SHADER = """
uniform shader uSharp;
uniform shader uHaze;
uniform float uHasBackdrop;
uniform float2 uSize;
uniform float2 uCardOrigin;
uniform float2 uScreenSize;
uniform float2 uSharpDim;
uniform float2 uHazeDim;
uniform float uCornerRadius;
uniform float uBevel;
uniform float uRefraction;
uniform half4 uTint;
uniform float uSaturation;
uniform float uContrast;
uniform float uRimStrength;
uniform float uHairline;
uniform float uEndShade;
uniform float uIsDark;
uniform float2 uGlowCenter;
uniform float uGlowReach;
uniform float uGlowStrength;
uniform float uDispersion;
uniform float uEnableFrost;
uniform float uLightCoverWaterfall;
uniform float uLightCoverSolid;

const float3 kLuma = float3(0.2126, 0.7152, 0.0722);
const float kLine = 1.8;
const float kLineCap = 0.298;
const float kRimTail = 12.0;

float roundedBoxDistance(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    float2 m = max(q, 0.0);
    float corner = (m.x <= 0.0 && m.y <= 0.0) ? 0.0 : pow(pow(m.x, 4.0) + pow(m.y, 4.0), 0.25);
    return corner + min(max(q.x, q.y), 0.0) - radius;
}

float2 roundedBoxNormal(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    float2 softQ = max(q, float2(0.0));
    float2 q3 = softQ * softQ * softQ;
    float lenQ3 = length(q3);
    float2 n;
    if (lenQ3 > 1e-4) {
        n = q3 / lenQ3;
    } else {
        float diff = q.x - q.y;
        float w = smoothstep(-4.0, 4.0, diff);
        n = normalize(mix(float2(0.0, 1.0), float2(1.0, 0.0), w));
    }
    return n * float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
}

half4 sampleSharpScreen(float2 screenPt) {
    float2 uv = clamp(screenPt / max(uScreenSize, float2(1.0)), float2(0.005), float2(0.995));
    float2 texPt = uv * uSharpDim;
    // 亚像素级 (±0.34 纹素半径) 5x 旋转网格超采样抗锯齿 (5x RGSS MSAA)：
    // 仅平滑亚像素斜边台阶，0 模糊、100% 保留玻璃内书籍封面的锐利细节！
    float2 o1 = float2(0.34, 0.14);
    float2 o2 = float2(-0.14, 0.34);
    half4 c0 = uSharp.eval(texPt) * 0.44;
    half4 c1 = (
        uSharp.eval(texPt + o1) +
        uSharp.eval(texPt - o1) +
        uSharp.eval(texPt + o2) +
        uSharp.eval(texPt - o2)
    ) * 0.14;
    return c0 + c1;
}

half4 sampleHazeScreen(float2 screenPt) {
    float2 uv = clamp(screenPt / max(uScreenSize, float2(1.0)), float2(0.005), float2(0.995));
    float2 texPt = uv * uHazeDim;
    // 低分辨率磨砂纹理放大时用 4 点帐篷采样，消除双线性放大的菱形棱纹，保证磨砂面如奶油般平滑
    float2 d1 = float2(0.5, 0.5);
    float2 d2 = float2(-0.5, 0.5);
    return (
        uHaze.eval(texPt + d1) +
        uHaze.eval(texPt - d1) +
        uHaze.eval(texPt + d2) +
        uHaze.eval(texPt - d2)
    ) * 0.25;
}

half4 main(float2 fragCoord) {
    float2 halfSize = uSize * 0.5;
    float halfMinor = min(halfSize.x, halfSize.y);
    float radius = min(uCornerRadius, halfMinor);
    float2 centered = fragCoord - halfSize;
    float dist = roundedBoxDistance(centered, halfSize, radius);
    if (dist > 0.5) {
        return half4(0.0);
    }

    float inset = max(-dist, 0.0);
    float bevel = max(min(uBevel, min(radius * 0.92, halfMinor * 0.5)), 1.0);
    float2 normal = roundedBoxNormal(centered, halfSize, max(radius, bevel * 1.05));
    float rise = 1.0 - clamp(inset / bevel, 0.0, 1.0);

    // 1. 1:1 FlClash liquid_glass.frag 球面透镜边缘折射（严格限制在 bevel <= 0.92 * radius 内，彻底消除 45° 对角线跳变与内框断层）
    float depth = min(uRefraction, halfMinor) * (1.0 - sqrt(max(1.0 - rise * rise, 0.0)));
    float2 shift = -normal * depth;

    float3 glass;
    float coverage;
    float screenYRatio = clamp((uCardOrigin.y + fragCoord.y) / max(uScreenSize.y, 1.0), 0.0, 1.0);
    float lowerSolidZone = smoothstep(0.42, 0.53, screenYRatio);
    float waterfallZone = 1.0 - lowerSolidZone;
    float edgeBehindLuma = uIsDark > 0.5 ? 0.12 : mix(0.45, 0.95, lowerSolidZone);

    if (uHasBackdrop > 0.5) {
        float2 refractPt = uCardOrigin + fragCoord + shift;
        half4 base = sampleSharpScreen(refractPt);

        // 仅在瀑布流可见区或触控抬起时施加棱镜色散，避免在底部纯色渐隐区（红圈区域）放大暗部残余色偏造成闪烁
        float effectiveDisp = uDispersion * mix(0.25, 1.0, waterfallZone);
        if (effectiveDisp > 0.05 && inset < bevel * 0.85) {
            float2 part = normal.yx * 0.50 * effectiveDisp
                * (0.60 + 0.40 * abs(normal.y) * max(1.0 - inset / (bevel * 0.65), 0.0))
                * (1.0 - smoothstep(bevel * 0.55, bevel * 0.85, inset));
            half4 redS = sampleSharpScreen(refractPt + part);
            half4 blueS = sampleSharpScreen(refractPt - part);
            base = half4(redS.r, base.g, blueS.b, 1.0);
        }

        // 双通道液态磨砂光学架构 (Dual-Channel Liquid Frosted Glass):
        // 通道 1 (边缘曲面 Bevel): 锐利折射 (uSharp) + 球面透镜偏转 (shift) + 棱镜色散 (dispersion)
        // 通道 2 (内部核心 Core): 柔和高斯磨砂漫射 (uHaze) + 动态底色融合，确保深浅色模式下文字极高对比度
        float bevelLens = rise * rise;
        float3 sharpRgb = float3(base.rgb);
        float3 hazeRgb = float3(sampleHazeScreen(refractPt).rgb);
        float frostFactor = uEnableFrost > 0.5 ? (1.0 - bevelLens) : 0.0;
        float3 mixedRgb = mix(sharpRgb, hazeRgb, frostFactor);
        float luma = dot(mixedRgb, kLuma);

        // 在暗部渐隐区平滑收束饱和度增益，防止刚进入底部渐隐区的暗淡封面被高倍饱和度放大而闪烁
        float lumaActivity = uIsDark > 0.5 ? smoothstep(0.03, 0.18, luma) : 1.0;
        float satBoost = mix(1.0, (uIsDark > 0.5 ? uSaturation : 1.42) + 0.22 * bevelLens, lumaActivity);
        float3 color = clamp(mix(float3(luma), mixedRgb, satBoost), float3(0.0), float3(1.0));
        // 左右曲率暗角仅用于深色模式；浅色模式下它会在卡片左右边缘压出一圈灰黑脏边
        if (uIsDark > 0.5) {
            color *= 1.0 - uEndShade * abs(normal.x) * exp(-inset / 6.0);
        }

        // 浅色模式：磨砂 + 白色柔光底色（瀑布流区 62% / 白底区 74%）；深色模式：磨砂 + 熏黑底色。
        // 磨砂已抹平封面的明暗细节，底色无需再压到 90% 也能保证可读性；曲面倒角处适度减薄，维持玻璃立体感。
        float3 targetTint = float3(uTint.rgb);
        float baseCover;
        float bevelClearFactor;
        if (uIsDark < 0.5) {
            targetTint = float3(0.975, 0.978, 0.988);
            baseCover = mix(uLightCoverWaterfall, uLightCoverSolid, lowerSolidZone);
            // 瀑布流区按「暗背景」处理 ⇒ 外沿用白色亮边而非深色描边；仅白底区使用浅灰微轮廓边
            edgeBehindLuma = mix(0.40, 0.95, lowerSolidZone);
            bevelClearFactor = uEnableFrost > 0.5 ? 0.08 : mix(0.12, 0.22, waterfallZone);
        } else {
            baseCover = mix(0.54, 0.52, lowerSolidZone);
            edgeBehindLuma = 0.12;
            bevelClearFactor = mix(0.24, 0.52, waterfallZone * lumaActivity);
        }

        float cover = baseCover * (1.0 - bevelClearFactor * bevelLens);
        coverage = 1.0;
        glass = color * (1.0 - cover) + targetTint * cover;
    } else {
        glass = float3(uTint.rgb);
        coverage = float(uTint.a);
    }

    // 2. FlClash LiquidGlassStyle.pill 上下边缘镜面高光折射带（仅限 1.6px 窄带，不向卡片内部扩散白光）
    float facing = normal.y * normal.y;
    float rimBand = clamp(1.0 - max(inset - 0.67, 0.0) / 1.6, 0.0, 1.0);
    float shine = min(uRimStrength * facing * rimBand * 0.36, 1.0);
    glass += (float3(coverage) - glass) * shine;

    // 3. iOS 26/27 背景自适应微细玻璃轮廓边
    float line = clamp((1.18 - inset) + 0.5, 0.0, 1.0);
    float brightRimBg = smoothstep(0.64, 0.86, edgeBehindLuma);
    float3 darkBgRim = mix(glass, float3(1.0), clamp(line * uHairline * 0.68, 0.0, 1.0));
    float3 lightBgRim = mix(glass, float3(0.08, 0.09, 0.13), clamp(line * 0.13, 0.0, 1.0));
    glass = mix(darkBgRim, lightBgRim, brightRimBg);

    // 3. 触控流光 (LiquidGlassGlow)
    if (uGlowStrength > 0.001) {
        float2 toGlow = fragCoord - uGlowCenter;
        float reach2 = max(uGlowReach * uGlowReach, 1.0);
        float glow = uGlowStrength * mix(exp(-dot(toGlow, toGlow) / reach2), 1.0, 0.55);
        glass = min(glass + float3(glow), float3(1.0));
    }

    return half4(half3(min(glass, float3(coverage))), half(coverage));
}
"""

/**
 * iOS 26 / Flutter SpringSimulation 闭式解析解阻尼谐振子（Closed-Form Analytical Damped Harmonic Oscillator）：
 * - 直接对真实流逝时间求解二阶常微分方程的解析解（欠阻尼、临界阻尼、过阻尼），
 *   杜绝显式欧拉迭代的能量发散、高频抖动与不同刷新率（60Hz/120Hz/144Hz）下的步进漂移；
 * - 保持与现有调用点 100% 兼容的 springTo(newTarget, durationMs, bounce) API 签名；
 * - 动画停止条件收敛到亚像素阈值，并在静止时自动挂起，消除空转开销。
 */
internal class SimpleSpring(initial: Float) {
    var value by mutableFloatStateOf(initial)
        private set
    var target: Float = initial
        private set
    var velocity: Float = 0f
        private set
    var isActive: Boolean = false
        private set

    private var stiffness: Double = 400.0
    private var dampingRatio: Double = 0.82
    private var damping: Double = 28.0

    private var startOffset: Double = 0.0
    private var startVelocity: Double = 0.0
    private var elapsedSec: Double = 0.0

    fun springTo(
        newTarget: Float,
        durationMs: Int = 320,
        bounce: Double = 0.18,
        velocityOverride: Float? = null,
    ) {
        val effectiveVel = velocityOverride ?: velocity
        if (isActive &&
            abs(newTarget - target) < 1e-5f &&
            velocityOverride == null
        ) {
            return
        }
        val dist = abs(newTarget - value)
        if (dist < 0.0005f && abs(effectiveVel) < 0.005f) {
            value = newTarget
            target = newTarget
            velocity = 0f
            isActive = false
            return
        }

        val durSec = (durationMs / 1000.0).coerceAtLeast(0.02)
        val wn = 2.0 * PI / durSec
        stiffness = wn * wn
        dampingRatio = if (bounce >= 0.0) (1.0 - bounce).coerceIn(0.05, 1.0) else 1.0
        damping = 2.0 * dampingRatio * wn

        startOffset = (value - newTarget).toDouble()
        startVelocity = effectiveVel.toDouble()
        velocity = effectiveVel
        target = newTarget
        elapsedSec = 0.0
        isActive = true
    }

    fun jumpTo(newValue: Float) {
        value = newValue
        target = newValue
        velocity = 0f
        startOffset = 0.0
        startVelocity = 0.0
        elapsedSec = 0.0
        isActive = false
    }

    fun step(dtSeconds: Float): Boolean {
        if (!isActive) return false
        val dt = dtSeconds.coerceIn(0.0005f, 0.12f).toDouble()
        elapsedSec += dt
        val t = elapsedSec

        val k = stiffness
        val zeta = dampingRatio
        val wn = sqrt(k)

        val x0 = startOffset
        val v0 = startVelocity

        val disp: Double
        val vel: Double

        if (zeta < 0.9999) {
            // 欠阻尼 (Underdamped): e^(-zeta * wn * t) * (c1 * cos(wd * t) + c2 * sin(wd * t))
            val wd = wn * sqrt(1.0 - zeta * zeta)
            val a = zeta * wn
            val c1 = x0
            val c2 = (v0 + a * x0) / wd
            val env = exp(-a * t)
            val cosWt = cos(wd * t)
            val sinWt = sin(wd * t)
            disp = env * (c1 * cosWt + c2 * sinWt)
            vel = env * ((c2 * wd - c1 * a) * cosWt - (c1 * wd + c2 * a) * sinWt)
        } else if (zeta <= 1.0001) {
            // 临界阻尼 (Critically Damped): (c1 + c2 * t) * e^(-wn * t)
            val r = -wn
            val c1 = x0
            val c2 = v0 - r * x0
            val env = exp(r * t)
            disp = (c1 + c2 * t) * env
            vel = (c2 + r * (c1 + c2 * t)) * env
        } else {
            // 过阻尼 (Overdamped): c1 * e^(r1 * t) + c2 * e^(r2 * t)
            val root = sqrt(zeta * zeta - 1.0)
            val r1 = -wn * (zeta - root)
            val r2 = -wn * (zeta + root)
            val c2 = (v0 - r1 * x0) / (r2 - r1)
            val c1 = x0 - c2
            val e1 = exp(r1 * t)
            val e2 = exp(r2 * t)
            disp = c1 * e1 + c2 * e2
            vel = c1 * r1 * e1 + c2 * r2 * e2
        }

        val x = target.toDouble() + disp
        value = x.toFloat()
        velocity = vel.toFloat()

        if (abs(disp) < 0.0008 && abs(vel) < 0.008) {
            value = target
            velocity = 0f
            isActive = false
            return false
        }
        return true
    }
}

private const val RUBBER_BAND_RESISTANCE_FACTOR = 1.35f

private fun rubberBand2D(overshoot: Float, limit: Float): Float {
    if (limit <= 0f) return 0f
    val resistance = (limit * RUBBER_BAND_RESISTANCE_FACTOR).coerceAtLeast(1f)
    return limit * tanh((overshoot / resistance).toDouble()).toFloat()
}

/**
 * 1:1 对应 FlClash `LiquidGlassStyle.pill` 的基底色（Light: 0x93F4F4F4 = 57.6% alpha, Dark: 48% alpha）：
 * 配合 shader 内部的 5 点自适应对比度（clash）与 2.1x 饱和度漫射，既通透折射又保证文字清晰！
 */
fun unifiedLiquidGlassTint(isLight: Boolean, alphaOverride: Float? = null): Color {
    return if (isLight) {
        Color(0xFFF4F4F4).copy(alpha = alphaOverride ?: 0.78f)
    } else {
        Color(0xFF181920).copy(alpha = alphaOverride ?: 0.52f)
    }
}

private val dummy1x1Bitmap: Bitmap by lazy {
    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.TRANSPARENT)
    }
}

private val dummy1x1Shader: BitmapShader by lazy {
    BitmapShader(dummy1x1Bitmap, AndroidShader.TileMode.CLAMP, AndroidShader.TileMode.CLAMP)
}

/**
 * 1:1 屏幕坐标对齐的 iOS 26 / FlClash 双区光学液态玻璃卡片修饰符：
 * - 自动读取 `LocalLiquidGlassBackdrop` 中与屏幕 1:1 对齐的合成背景双纹理（直接复用缓存的 BitmapShader，零对象分配）；
 * - 中心阅读区保持丝滑高对比度磨砂，外圈 24dp 曲面倒角实时折射流转正下方的冷暖色带光影；
 * - 边框与底部 Dock 完全统一为全周均匀的 1.15px 亚像素微细玻璃边！
 */
@Composable
fun Modifier.liquidGlassCard(
    palette: AppPalette,
    cornerRadius: Dp = 22.dp,
    backdropBook: BookEntry? = null,
    localBackdropMode: Boolean = false,
    tintAlphaOverride: Float? = null,
    refractionDp: Dp = 36.dp,
    bevelDp: Dp = 18.dp,
    dispersion: Float = 0f,
    originOverride: Offset? = null,
    dragOffsetPx: Offset = Offset.Zero,
    glowCenterPx: Offset? = null,
    glowStrength: Float = 0f,
): Modifier {
    val isLight = palette.isLight
    val baseTint = unifiedLiquidGlassTint(isLight, tintAlphaOverride)
    val backdrop = LocalLiquidGlassBackdrop.current
    val powerSave = LocalPowerSaveMode.current
    var cardOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    val agslShader = remember(powerSave) {
        if (!powerSave && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(AGSL_IOS26_SCREEN_REFRACT_SHADER)
            } catch (_: Throwable) {
                null
            }
        } else null
    }
    val reusableAgslPaint = remember { AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG) }
    val outerClipPath = remember { Path() }
    val fallbackCardPath = remember { Path() }
    val density = LocalDensity.current
    val shadowBlurPx = with(density) { 6.5.dp.toPx() }
    val reusableShadowPaint = remember(shadowBlurPx, powerSave) {
        if (powerSave) {
            null
        } else {
            AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                maskFilter = BlurMaskFilter(shadowBlurPx, BlurMaskFilter.Blur.NORMAL)
            }
        }
    }

    return this
        .then(
            if (originOverride == null) {
                Modifier.onGloballyPositioned { coords ->
                    cardOriginInRoot = coords.positionInRoot()
                }
            } else {
                Modifier
            }
        )
        .drawWithContent {
            val w = size.width
            val h = size.height
            val rPx = cornerRadius.toPx().coerceAtMost(min(w, h) * 0.5f)
            val bevelPx = bevelDp.toPx().coerceAtMost(min(w, h) * 0.45f)
            val refractionPx = refractionDp.toPx()
            val cr = CornerRadius(rPx, rPx)
            val effectiveOrigin = (originOverride ?: cardOriginInRoot) + dragOffsetPx
            val effectiveGlowCenter = glowCenterPx ?: Offset(w * 0.35f, h * 0.18f)
            val effectiveGlowStrength = if (glowStrength > 0.001f) glowStrength else 0f

            // 0. 硬件加速画布下绘制 iOS 26/27 卡片外沿微環境悬浮投影（裁切在圆角矩形外，绝不污染半透明玻璃内部）
            if (!powerSave &&
                reusableShadowPaint != null &&
                w > 0f &&
                h > 0f &&
                drawContext.canvas.nativeCanvas.isHardwareAccelerated
            ) {
                outerClipPath.reset()
                outerClipPath.addRoundRect(RoundRect(Rect(0f, 0f, w, h), cr))
                clipPath(outerClipPath, clipOp = ClipOp.Difference) {
                    drawIntoCanvas { canvas ->
                        val shadowAlpha = if (isLight) 0.04f else 0.20f
                        reusableShadowPaint.color = android.graphics.Color.argb(
                            (shadowAlpha * 255).toInt().coerceIn(0, 255),
                            0,
                            0,
                            0,
                        )
                        val shadowDy = 2.5.dp.toPx()
                        canvas.nativeCanvas.drawRoundRect(
                            0f,
                            shadowDy,
                            w,
                            h + shadowDy,
                            rPx,
                            rPx,
                            reusableShadowPaint,
                        )
                    }
                }
            }

            // 1. 绘制液态玻璃基底（Android 13+ 使用 AGSL 1:1 屏幕坐标折射 + Haze 漫射着色器）
            if (!powerSave &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                agslShader != null &&
                w > 0f &&
                h > 0f &&
                drawContext.canvas.nativeCanvas.isHardwareAccelerated
            ) {
                if (backdrop != null) {
                    val isUpperWaterfallCard = effectiveOrigin.y < backdrop.screenHeightPx * 0.60f
                    val dynamicFrame = if (isUpperWaterfallCard) backdrop.dynamicFrameProvider?.invoke() else null
                    val sharpBmp = dynamicFrame?.bitmap ?: backdrop.sharpScreenBitmap
                    val sharpShader = dynamicFrame?.shader ?: backdrop.sharpShader
                    val hazeBmp = dynamicFrame?.hazeBitmap ?: dynamicFrame?.bitmap ?: backdrop.hazeScreenBitmap
                    val hazeShader = dynamicFrame?.hazeShader ?: dynamicFrame?.shader ?: backdrop.hazeShader
                    agslShader.setInputShader("uSharp", sharpShader)
                    agslShader.setInputShader("uHaze", hazeShader)
                    agslShader.setFloatUniform("uHasBackdrop", 1f)
                    agslShader.setFloatUniform("uScreenSize", backdrop.screenWidthPx, backdrop.screenHeightPx)
                    agslShader.setFloatUniform("uSharpDim", sharpBmp.width.toFloat(), sharpBmp.height.toFloat())
                    agslShader.setFloatUniform("uHazeDim", hazeBmp.width.toFloat(), hazeBmp.height.toFloat())
                } else {
                    agslShader.setInputShader("uSharp", dummy1x1Shader)
                    agslShader.setInputShader("uHaze", dummy1x1Shader)
                    agslShader.setFloatUniform("uHasBackdrop", 0f)
                    agslShader.setFloatUniform("uScreenSize", max(w, 1f), max(h, 1f))
                    agslShader.setFloatUniform("uSharpDim", 1f, 1f)
                    agslShader.setFloatUniform("uHazeDim", 1f, 1f)
                }

                agslShader.setFloatUniform("uSize", w, h)
                agslShader.setFloatUniform("uCardOrigin", effectiveOrigin.x, effectiveOrigin.y)
                agslShader.setFloatUniform("uCornerRadius", rPx)
                agslShader.setFloatUniform("uBevel", bevelPx)
                agslShader.setFloatUniform("uRefraction", refractionPx)
                agslShader.setFloatUniform(
                    "uTint",
                    baseTint.red,
                    baseTint.green,
                    baseTint.blue,
                    baseTint.alpha,
                )
                agslShader.setFloatUniform("uSaturation", if (isLight) 1.85f else 1.75f)
                agslShader.setFloatUniform("uContrast", 0.72f)
                agslShader.setFloatUniform("uRimStrength", if (isLight) 0.45f else 0.32f)
                agslShader.setFloatUniform("uHairline", 0.34f)
                agslShader.setFloatUniform("uEndShade", if (isLight) 0.035f else 0.05f)
                agslShader.setFloatUniform("uIsDark", if (isLight) 0f else 1f)
                agslShader.setFloatUniform("uGlowCenter", effectiveGlowCenter.x, effectiveGlowCenter.y)
                agslShader.setFloatUniform("uGlowReach", max(w, h) * 0.55f)
                agslShader.setFloatUniform("uGlowStrength", effectiveGlowStrength)
                agslShader.setFloatUniform("uDispersion", dispersion)
                agslShader.setFloatUniform("uEnableFrost", if (LiquidGlassConfig.ENABLE_LIGHT_MODE_FROST) 1f else 0f)
                agslShader.setFloatUniform("uLightCoverWaterfall", LiquidGlassConfig.lightCoverWaterfall)
                agslShader.setFloatUniform("uLightCoverSolid", LiquidGlassConfig.lightCoverSolid)

                drawIntoCanvas { canvas ->
                    reusableAgslPaint.shader = agslShader
                    canvas.nativeCanvas.drawRoundRect(0f, 0f, w, h, rPx, rPx, reusableAgslPaint)
                }
            } else if (w > 0f && h > 0f) {
                // Android 12 或软件截屏画布的双区光学玻璃模拟（省电模式降级为纯色轻质磨砂面板）
                if (!powerSave && backdrop != null && drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                    fallbackCardPath.reset()
                    fallbackCardPath.addRoundRect(RoundRect(Rect(0f, 0f, w, h), cr))
                    clipPath(fallbackCardPath) {
                        val sharpBmp = backdrop.sharpScreenBitmap
                        val hazeBmp = backdrop.hazeScreenBitmap
                        val sw = backdrop.screenWidthPx.coerceAtLeast(1f)
                        val sh = backdrop.screenHeightPx.coerceAtLeast(1f)

                        drawIntoCanvas { canvas ->
                            val nc = canvas.nativeCanvas
                            val satFilter = android.graphics.ColorMatrixColorFilter(
                                android.graphics.ColorMatrix().apply {
                                    setSaturation(if (isLight) 1.65f else 1.55f)
                                }
                            )

                            // 1a) 中心阅读区采样丝滑 Haze 纹理
                            val hazeMatrix = AndroidMatrix().apply {
                                setScale(
                                    sw / hazeBmp.width.toFloat().coerceAtLeast(1f),
                                    sh / hazeBmp.height.toFloat().coerceAtLeast(1f),
                                )
                                postTranslate(-effectiveOrigin.x, -effectiveOrigin.y)
                            }
                            val hazePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
                                colorFilter = satFilter
                                shader = backdrop.hazeShader.apply { setLocalMatrix(hazeMatrix) }
                            }
                            nc.drawRoundRect(0f, 0f, w, h, rPx, rPx, hazePaint)
                            drawRoundRect(color = baseTint, cornerRadius = cr)

                            // 1b) 外圈倒角向内弯曲折射 Sharp 色带
                            val refractMatrix = AndroidMatrix().apply {
                                setScale(
                                    sw / sharpBmp.width.toFloat().coerceAtLeast(1f),
                                    sh / sharpBmp.height.toFloat().coerceAtLeast(1f),
                                )
                                postTranslate(-effectiveOrigin.x, -effectiveOrigin.y)
                                postScale(1.15f, 1.15f, w * 0.5f, h * 0.5f)
                            }
                            val refractPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
                                style = AndroidPaint.Style.STROKE
                                strokeWidth = 18.dp.toPx()
                                alpha = if (isLight) 150 else 135
                                colorFilter = satFilter
                                shader = backdrop.sharpShader.apply { setLocalMatrix(refractMatrix) }
                            }
                            nc.drawRoundRect(0f, 0f, w, h, rPx, rPx, refractPaint)
                        }
                    }
                } else {
                    drawRoundRect(color = baseTint, cornerRadius = cr)
                }

                val strokeW = 0.6.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isLight) 0.18f else 0.08f),
                    topLeft = Offset(strokeW * 0.5f, strokeW * 0.5f),
                    size = Size(w - strokeW, h - strokeW),
                    cornerRadius = cr,
                    style = Stroke(width = strokeW),
                )
            }

            // 2. 绘制卡片内部内容（不再叠加多余的对角线渐变描边，保持与图5 Dock 完全一致的干净均匀细边！）
            drawContent()
        }
}

/**
 * 为大卡片（「本周累计阅读」、「在读书籍大卡」）及横向列表卡片（「最近阅读」）提供与日夜按钮 / Dock 同源的 120Hz 液态水滴形变物理：
 * - 按压瞬间液态膨胀（`1.046x` / `1.055x`）并联动增强倒角折射与棱镜色散；
 * - 拖动时根据拉力方向动态移动反向缩放锚点（`transformOrigin`），产生高达 `8.5%` 的顺向拉伸与 `4.2%` 的正交体积守恒挤压；
 * - 松手回弹时触发速度耦合的 `jellySpring` 果冻弹性振荡，彻底消除刚体平移的割裂感！
 */
@Composable
fun Modifier.liquidGlassInteractiveCard(
    palette: AppPalette,
    cornerRadius: Dp = 22.dp,
    maxPullDp: Dp = 18.dp,
    pressScaleTarget: Float = 1.046f,
    stretchRatio: Float = 0.085f,
    verticalOnly: Boolean = false,
    onClick: () -> Unit,
): Modifier {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)

    val offsetX = remember { SimpleSpring(0f) }
    val offsetY = remember { SimpleSpring(0f) }
    val pressScale = remember { SimpleSpring(1f) }
    val lift = remember { SimpleSpring(0f) }
    val jellyX = remember { SimpleSpring(0f) }
    val jellyY = remember { SimpleSpring(0f) }
    val glowAlpha = remember { SimpleSpring(0f) }
    var glowCenter by remember { mutableStateOf<Offset?>(null) }
    var layoutOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    val maxPullPx = with(density) { maxPullDp.toPx() }
    val clickThresholdPx = with(density) { 28.dp.toPx() }
    val choreographer = remember { Choreographer.getInstance() }
    var loopRunning by remember { mutableStateOf(false) }
    var lastNanos by remember { mutableStateOf(0L) }

    val frameCallback = remember(maxPullPx) {
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val dt = if (lastNanos == 0L) 0.008f else ((frameTimeNanos - lastNanos) / 1_000_000_000.0).toFloat()
                lastNanos = frameTimeNanos
                offsetX.step(dt)
                offsetY.step(dt)
                pressScale.step(dt)
                lift.step(dt)
                jellyX.step(dt)
                jellyY.step(dt)
                glowAlpha.step(dt)

                // 当位置弹簧运动时，根据回弹/拖拽速度耦合驱动果冻弹性形变振荡
                if (offsetX.isActive || offsetY.isActive) {
                    val vxNorm = (abs(offsetX.velocity) / (maxPullPx * 10f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val vyNorm = (abs(offsetY.velocity) / (maxPullPx * 10f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val jxTarget = vxNorm * 0.045f
                    val jyTarget = vyNorm * 0.045f
                    if (abs(jxTarget - jellyX.target) > 0.003f) {
                        jellyX.springTo(jxTarget, durationMs = 360, bounce = 0.36)
                    }
                    if (abs(jyTarget - jellyY.target) > 0.003f) {
                        jellyY.springTo(jyTarget, durationMs = 360, bounce = 0.36)
                    }
                } else if (jellyX.target != 0f || jellyY.target != 0f) {
                    jellyX.springTo(0f, durationMs = 420, bounce = 0.38)
                    jellyY.springTo(0f, durationMs = 420, bounce = 0.38)
                }

                val anyActive = offsetX.isActive || offsetY.isActive || pressScale.isActive ||
                    lift.isActive || jellyX.isActive || jellyY.isActive || glowAlpha.isActive
                if (anyActive) {
                    choreographer.postFrameCallback(this)
                } else {
                    loopRunning = false
                    lastNanos = 0L
                }
            }
        }
    }

    fun ensureSpringLoop() {
        if (!loopRunning) {
            loopRunning = true
            lastNanos = 0L
            choreographer.postFrameCallback(frameCallback)
        }
    }

    DisposableEffect(frameCallback) {
        onDispose {
            loopRunning = false
            choreographer.removeFrameCallback(frameCallback)
        }
    }

    return this
        .onGloballyPositioned { coords ->
            layoutOriginInRoot = coords.positionInRoot()
        }
        .pointerInput(verticalOnly, maxPullPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startPos = down.position
                var pointerId = down.id
                var lastRawDx = 0f
                var lastRawDy = 0f
                var handedOffToHorizontalScroll = false
                var committed = true

                glowCenter = startPos
                pressScale.springTo(pressScaleTarget, durationMs = 190, bounce = 0.25)
                lift.springTo(1f, durationMs = 190, bounce = 0.18)
                glowAlpha.springTo(0.08f, durationMs = 140, bounce = 0.0)
                ensureSpringLoop()

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                    if (change == null || !change.pressed) {
                        committed = change != null
                        break
                    }
                    pointerId = change.id
                    glowCenter = change.position
                    lastRawDx = change.position.x - startPos.x
                    lastRawDy = change.position.y - startPos.y

                    if (verticalOnly) {
                        if (!handedOffToHorizontalScroll &&
                            abs(lastRawDx) > viewConfiguration.touchSlop * 1.15f &&
                            abs(lastRawDx) > abs(lastRawDy) * 1.15f
                        ) {
                            handedOffToHorizontalScroll = true
                            offsetY.springTo(0f, durationMs = 300, bounce = 0.26)
                            pressScale.springTo(1f, durationMs = 260, bounce = 0.20)
                            lift.springTo(0f, durationMs = 260, bounce = 0.12)
                            glowAlpha.springTo(0f, durationMs = 240, bounce = 0.0)
                            ensureSpringLoop()
                        }
                        if (!handedOffToHorizontalScroll) {
                            val pulledY = rubberBand2D(lastRawDy, maxPullPx)
                            offsetY.springTo(pulledY, durationMs = 65, bounce = 0.0)
                            ensureSpringLoop()
                            if (abs(lastRawDy) > viewConfiguration.touchSlop * 0.8f) {
                                change.consume()
                            }
                        }
                    } else {
                        val pulledX = rubberBand2D(lastRawDx, maxPullPx)
                        val pulledY = rubberBand2D(lastRawDy, maxPullPx)
                        offsetX.springTo(pulledX, durationMs = 65, bounce = 0.0)
                        offsetY.springTo(pulledY, durationMs = 65, bounce = 0.0)
                        ensureSpringLoop()
                        change.consume()
                    }
                }

                val rawDist = hypot(lastRawDx, lastRawDy)
                offsetX.springTo(0f, durationMs = 430, bounce = 0.34)
                offsetY.springTo(0f, durationMs = 430, bounce = 0.34)
                pressScale.springTo(1f, durationMs = 380, bounce = 0.28)
                lift.springTo(0f, durationMs = 340, bounce = 0.15)
                glowAlpha.springTo(0f, durationMs = 320, bounce = 0.0)
                ensureSpringLoop()

                if (committed && !handedOffToHorizontalScroll && rawDist < clickThresholdPx) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnClick()
                }
            }
        }
        .graphicsLayer {
            val dx = offsetX.value
            val dy = offsetY.value
            val signedX = (dx / maxPullPx.coerceAtLeast(1f)).coerceIn(-1f, 1f)
            val signedY = (dy / maxPullPx.coerceAtLeast(1f)).coerceIn(-1f, 1f)
            val normX = abs(signedX)
            val normY = abs(signedY)

            // 动态反向锚点（复刻 FlClash _PressTransform：拉向右侧时左侧锚定，水滴向右侧延展）
            transformOrigin = TransformOrigin(
                pivotFractionX = (0.5f - signedX * 0.26f).coerceIn(0.18f, 0.82f),
                pivotFractionY = (0.5f - signedY * 0.26f).coerceIn(0.18f, 0.82f),
            )

            // 顺向水滴拉伸 + 正交方向体积守恒挤压 + 松手果冻振荡 (jellyX / jellyY)
            val totalStretchX = normX * stretchRatio + jellyX.value
            val totalStretchY = normY * stretchRatio + jellyY.value
            val sx = 1f + totalStretchX - totalStretchY * 0.48f
            val sy = 1f + totalStretchY - totalStretchX * 0.48f
            scaleX = pressScale.value * sx
            scaleY = pressScale.value * sy
            translationX = dx
            translationY = dy
        }
        .liquidGlassCard(
            palette = palette,
            cornerRadius = cornerRadius,
            refractionDp = 42.dp,
            bevelDp = 19.dp,
            dispersion = 2.6f,
            originOverride = layoutOriginInRoot,
            dragOffsetPx = Offset(offsetX.value, offsetY.value),
            glowCenterPx = glowCenter,
            glowStrength = glowAlpha.value,
        )
        .clip(squircleShape(cornerRadius))
}

/**
 * 「最近阅读」横向滑动容器（纯正顺滑左右滚动，无上下拉扯形变、无截断）：
 */
@Composable
fun LiquidGlassHorizontalGallery(
    modifier: Modifier = Modifier,
    contentPaddingHorizontal: Dp = 20.dp,
    itemSpacing: Dp = 12.dp,
    verticalAllowance: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = contentPaddingHorizontal, vertical = verticalAllowance),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/**
 * 1:1 复刻图 6、图 7 的 iOS 液态玻璃可拖拽跟手圆形按钮（用于右上角日夜切换按钮）
 */
@Composable
fun LiquidGlassDraggableCircleButton(
    palette: AppPalette,
    size: Dp,
    backdropBook: BookEntry? = null,
    revealProgress: Float = 1f,
    onClickWithCenterInRoot: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    pressScaleTarget: Float = 1.13f,
    showFingerHighlight: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val isLight = palette.isLight
    val vitroBackdrop = LocalVitroBackdrop.current
    val readingStyle = LocalReadingStyle.current
    val currentOnClick by rememberUpdatedState(onClickWithCenterInRoot)

    var baseCenterInRoot by remember { mutableStateOf(Offset.Zero) }
    val offsetX = remember { SimpleSpring(0f) }
    val offsetY = remember { SimpleSpring(0f) }
    val lift = remember { SimpleSpring(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var rawFingerPos by remember { mutableStateOf(Offset.Unspecified) }
    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) pressScaleTarget else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 520f,
        ),
        label = "themeOrbPressScale",
    )

    val choreographer = remember { Choreographer.getInstance() }
    var loopRunning by remember { mutableStateOf(false) }
    var lastNanos by remember { mutableStateOf(0L) }

    val frameCallback = remember {
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val dt = if (lastNanos == 0L) 0.008f else ((frameTimeNanos - lastNanos) / 1_000_000_000.0).toFloat()
                lastNanos = frameTimeNanos
                offsetX.step(dt)
                offsetY.step(dt)
                lift.step(dt)
                if (offsetX.isActive || offsetY.isActive || lift.isActive) {
                    choreographer.postFrameCallback(this)
                } else {
                    loopRunning = false
                    lastNanos = 0L
                }
            }
        }
    }

    fun ensureSpringLoop() {
        if (!loopRunning) {
            loopRunning = true
            lastNanos = 0L
            choreographer.postFrameCallback(frameCallback)
        }
    }

    DisposableEffect(frameCallback) {
        onDispose {
            loopRunning = false
            choreographer.removeFrameCallback(frameCallback)
        }
    }

    val maxPullPx = with(density) { 46.dp.toPx() }
    val cancelThresholdPx = with(density) { 80.dp.toPx() }
    val baseSizePx = with(density) { size.toPx().coerceAtLeast(1f) }
    val ambientBlurPx = with(density) { 10.dp.toPx() }
    val spotBlurPx = with(density) { 4.5.dp.toPx() }
    val cachedAmbientShadowPaint = remember(ambientBlurPx) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(ambientBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val cachedSpotShadowPaint = remember(spotBlurPx) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(spotBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val cachedOrbClipPath = remember { Path() }
    val lightOrbRimBrush = remember {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.62f),
            0.38f to Color.Black.copy(alpha = 0.045f),
            1.0f to Color.Black.copy(alpha = 0.075f),
        )
    }

    val clampedReveal = revealProgress.coerceIn(0f, 1f)
    val powerSave = LocalPowerSaveMode.current
    val effectiveVitroBackdrop = if (powerSave) null else vitroBackdrop
    val dragInteractionSource = remember { MutableInteractionSource() }
    val pointerInOrbProvider = remember(baseSizePx) {
        {
            val p = rawFingerPos
            if (p == Offset.Unspecified) {
                Offset.Unspecified
            } else {
                val scaledPx = baseSizePx * pressScaleState.value
                val insetShift = (baseSizePx - scaledPx) * 0.5f
                Offset(
                    x = p.x - offsetX.value - insetShift,
                    y = p.y - offsetY.value - insetShift,
                )
            }
        }
    }

    val vitroModifier = if (effectiveVitroBackdrop != null || powerSave) {
        Modifier.ios27GlassSurface(
            shape = CircleShape,
            palette = palette,
            style = readingStyle,
            variant = Ios27GlassVariant.Chrome,
            backdrop = effectiveVitroBackdrop,
            // Keep MotionStretch off while dragging layout .offset/.layout bounds so ThemeOrb
            // enlarges like a smooth droplet and fuses cleanly with zero +/-14% jelly twitch.
            isInteractive = false,
            state = null,
            interactionSource = dragInteractionSource,
            showContrastBorder = powerSave,
            showFingerHighlight = showFingerHighlight && !powerSave,
            fingerPressedOverride = isPressed,
            externalFingerControlled = true,
            fingerPointerProvider = pointerInOrbProvider,
        )
    } else {
        Modifier
            .liquidGlassCard(
                palette = palette,
                cornerRadius = 999.dp,
                backdropBook = backdropBook,
                tintAlphaOverride = if (isLight) {
                    (0.80f - 0.16f * lift.value).coerceIn(0.60f, 0.84f)
                } else {
                    (0.48f - 0.14f * lift.value).coerceIn(0.28f, 0.54f)
                },
                refractionDp = 14.dp,
                bevelDp = 10.dp,
                dispersion = 3.5f * lift.value,
                originOverride = Offset(
                    x = baseCenterInRoot.x - baseSizePx * 0.5f + offsetX.value -
                        (baseSizePx * (pressScaleState.value - 1f)) * 0.5f,
                    y = baseCenterInRoot.y - baseSizePx * 0.5f + offsetY.value -
                        (baseSizePx * (pressScaleState.value - 1f)) * 0.5f,
                ),
                dragOffsetPx = Offset.Zero,
                glowStrength = 0.06f * lift.value,
            )
            .clip(CircleShape)
            .then(
                if (showFingerHighlight) {
                    Modifier.vitroFingerSpotlight(
                        shape = CircleShape,
                        accentColor = palette.primary,
                        isDarkSurface = !isLight,
                        enabled = true,
                        pressedOverride = isPressed,
                        externalPointer = true,
                        pointerProvider = pointerInOrbProvider,
                    )
                } else {
                    Modifier
                },
            )
    }

    // Outer container keeps fixed layout footprint while inner glass node expands smoothly via .layout
    // centered on the orb and translates via .offset for clean glassEffectContainer fusion.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                baseCenterInRoot = Offset(
                    x = pos.x + coords.size.width * 0.5f,
                    y = pos.y + coords.size.height * 0.5f,
                )
            }
            .pointerInput(dragInteractionSource, maxPullPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val pressInteraction = PressInteraction.Press(startPos)
                    dragInteractionSource.tryEmit(pressInteraction)
                    var pointerId = down.id
                    var lastRawDx = 0f
                    var lastRawDy = 0f
                    var committed = false

                    rawFingerPos = startPos
                    isPressed = true

                    try {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lift.springTo(1f, durationMs = 190, bounce = 0.12)
                        ensureSpringLoop()

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                committed = change != null
                                break
                            }
                            pointerId = change.id
                            rawFingerPos = change.position
                            lastRawDx = change.position.x - startPos.x
                            lastRawDy = change.position.y - startPos.y

                            val pulledX = rubberBand2D(lastRawDx, maxPullPx)
                            val pulledY = rubberBand2D(lastRawDy, maxPullPx)
                            offsetX.springTo(pulledX, durationMs = 60, bounce = 0.0)
                            offsetY.springTo(pulledY, durationMs = 60, bounce = 0.0)
                            ensureSpringLoop()
                            change.consume()
                        }
                    } finally {
                        isPressed = false
                        dragInteractionSource.tryEmit(
                            if (committed) PressInteraction.Release(pressInteraction)
                            else PressInteraction.Cancel(pressInteraction),
                        )
                    }

                    val releaseCenter = Offset(
                        x = baseCenterInRoot.x + offsetX.value,
                        y = baseCenterInRoot.y + offsetY.value,
                    )
                    val rawDist = hypot(lastRawDx, lastRawDy)

                    offsetX.springTo(0f, durationMs = 360, bounce = 0.30)
                    offsetY.springTo(0f, durationMs = 360, bounce = 0.30)
                    lift.springTo(0f, durationMs = 300, bounce = 0.10)
                    ensureSpringLoop()

                    if (committed && rawDist < cancelThresholdPx) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnClick(releaseCenter)
                    }
                }
            }
            .graphicsLayer {
                alpha = clampedReveal
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset {
                    IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt())
                }
                .layout { measurable, _ ->
                    val basePx = size.roundToPx().coerceAtLeast(1)
                    val scaledPx = (basePx * pressScaleState.value).roundToInt().coerceAtLeast(1)
                    val placeable = measurable.measure(Constraints.fixed(scaledPx, scaledPx))
                    layout(basePx, basePx) {
                        val inset = ((basePx - scaledPx) * 0.5f).roundToInt()
                        placeable.placeRelative(inset, inset)
                    }
                }
                .drawBehind {
                    if (powerSave || clampedReveal <= 0.01f || !drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                        return@drawBehind
                    }
                    val w = this.size.width
                    val h = this.size.height
                    if (w <= 1f || h <= 1f) return@drawBehind
                    val r = min(w, h) * 0.5f
                    val liftT = lift.value.coerceIn(0f, 1f)
                    cachedOrbClipPath.reset()
                    cachedOrbClipPath.addOval(Rect(0f, 0f, w, h))
                    clipPath(cachedOrbClipPath, clipOp = ClipOp.Difference) {
                        drawIntoCanvas { canvas ->
                            val ambientAlpha = if (isLight) (0.075f + 0.055f * liftT) else (0.22f + 0.14f * liftT)
                            val ambientDy = 2.2.dp.toPx() + 3.0.dp.toPx() * liftT
                            cachedAmbientShadowPaint.color = android.graphics.Color.argb(
                                (ambientAlpha * 255).toInt().coerceIn(0, 255),
                                0,
                                0,
                                0,
                            )
                            canvas.nativeCanvas.drawCircle(
                                w * 0.5f,
                                h * 0.5f + ambientDy,
                                r * 0.96f,
                                cachedAmbientShadowPaint,
                            )
                            val spotAlpha = if (isLight) (0.045f + 0.04f * liftT) else (0.14f + 0.10f * liftT)
                            val spotDy = 1.0.dp.toPx() + 1.5.dp.toPx() * liftT
                            cachedSpotShadowPaint.color = android.graphics.Color.argb(
                                (spotAlpha * 255).toInt().coerceIn(0, 255),
                                0,
                                0,
                                0,
                            )
                            canvas.nativeCanvas.drawCircle(
                                w * 0.5f,
                                h * 0.5f + spotDy,
                                r * 0.98f,
                                cachedSpotShadowPaint,
                            )
                        }
                    }
                }
                .then(if (clampedReveal > 0.01f) vitroModifier else Modifier)
                .drawWithContent {
                    drawContent()
                    if (isLight && !powerSave && clampedReveal > 0.01f && effectiveVitroBackdrop != null) {
                        val strokePx = 0.6.dp.toPx()
                        val r = (this.size.minDimension - strokePx) * 0.5f
                        if (r > 1f) {
                            drawCircle(
                                brush = lightOrbRimBrush,
                                radius = r,
                                style = Stroke(width = strokePx),
                            )
                        }
                    }
                },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val revealScale = 0.55f + 0.45f * clampedReveal
                        val dx = offsetX.value
                        val dy = offsetY.value
                        val pullFraction = (hypot(dx, dy) / maxPullPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val stretchX = 1f + abs(dx) / maxPullPx.coerceAtLeast(1f) * 0.08f - abs(dy) / maxPullPx.coerceAtLeast(1f) * 0.03f
                        val stretchY = 1f + abs(dy) / maxPullPx.coerceAtLeast(1f) * 0.08f - abs(dx) / maxPullPx.coerceAtLeast(1f) * 0.03f
                        val baseS = pressScaleState.value * revealScale * (1f + pullFraction * 0.03f)
                        scaleX = baseS * stretchX
                        scaleY = baseS * stretchY
                    },
                content = content,
            )
        }
    }
}


/**
 * iOS 26/27 磨砂高阶浮层模态对话框容器：
 * - 在独立 Dialog Window 中不挂载嵌套 Vitro `glassBackdrop`/`glassEffect`（规避 RenderNode 循环引用 SIGSEGV），
 *   改为高质感磨砂渐变层 + 顶部高光釉面 + 镜面细边框 + 柔和阴影；
 * - 强制将 `LocalVitroBackdrop` 与 `LocalLiquidGlassBackdrop` 置空，防止子控件跨 Window 继承或共享 Backdrop。
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    palette: AppPalette,
    cornerRadius: Dp = 28.dp,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable BoxScope.() -> Unit,
) {
    val powerSave = LocalPowerSaveMode.current
    val density = LocalDensity.current
    val isLight = palette.isLight
    val dialogShape = RoundedCornerShape(cornerRadius)

    val sheetBrush = remember(isLight) {
        if (isLight) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f),
                    Color(0xFFFAFAFC).copy(alpha = 0.94f),
                    Color(0xFFF3F4F8).copy(alpha = 0.93f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF23252E).copy(alpha = 0.94f),
                    Color(0xFF1A1C24).copy(alpha = 0.93f),
                    Color(0xFF14161D).copy(alpha = 0.92f),
                ),
            )
        }
    }
    val sheenBrush = remember(isLight) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = if (isLight) 0.42f else 0.08f),
            0.36f to Color.Transparent,
        )
    }
    val borderBrush = remember(isLight) {
        if (isLight) {
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.48f),
                0.45f to Color.White.copy(alpha = 0.14f),
                1.0f to Color.Black.copy(alpha = 0.05f),
            )
        } else {
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.16f),
                0.45f to Color.White.copy(alpha = 0.06f),
                1.0f to Color.White.copy(alpha = 0.03f),
            )
        }
    }
    val shadowModifier = if (powerSave) {
        Modifier
    } else {
        Modifier.graphicsLayer {
            shadowElevation = with(density) { 24.dp.toPx() }
            shape = dialogShape
            clip = false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        CompositionLocalProvider(
            LocalVitroBackdrop provides null,
            LocalLiquidGlassBackdrop provides null,
            LocalGlassDarkTheme provides !isLight,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .then(shadowModifier)
                    .clip(dialogShape)
                    .background(sheetBrush)
                    .background(sheenBrush)
                    .border(
                        width = if (isLight) 0.8.dp else 0.75.dp,
                        brush = borderBrush,
                        shape = dialogShape,
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

@Stable
private class VitroPopoverPanelGeometry {
    var startW by mutableFloatStateOf(1f)
    var startH by mutableFloatStateOf(1f)
    var endW by mutableFloatStateOf(1f)
    var endH by mutableFloatStateOf(1f)
    var curW by mutableFloatStateOf(1f)
    var curH by mutableFloatStateOf(1f)
    var anchoredBottom by mutableStateOf(false)
}

/**
 * Popover 菜单选项数据契约。
 */
data class VitroGlassPopoverItem(
    val label: String,
    val selected: Boolean = false,
    val icon: ImageVector? = null,
    val tint: Color = Color.Unspecified,
    val onClick: () -> Unit,
)

@Stable
internal class VitroPopoverEntry(
    val token: Any,
    visible: Boolean,
    anchorBoundsInRoot: Rect?,
    items: List<VitroGlassPopoverItem>,
    palette: AppPalette,
    style: ReadingStyle,
    onDismiss: () -> Unit,
    backdrop: GlassBackdrop?,
    width: Dp,
    anchorCornerDp: Dp,
    anchorIcon: ImageVector?,
    anchorIconSize: Dp,
    onPresentingChanged: ((Boolean) -> Unit)? = null,
    onOrbVisibilityChanged: ((Float) -> Unit)? = null,
) {
    var visible by mutableStateOf(visible)
    var anchorBoundsInRoot by mutableStateOf(anchorBoundsInRoot)
    var items by mutableStateOf(items)
    var palette by mutableStateOf(palette)
    var style by mutableStateOf(style)
    var onDismiss by mutableStateOf(onDismiss)
    var backdrop by mutableStateOf(backdrop)
    var width by mutableStateOf(width)
    var anchorCornerDp by mutableStateOf(anchorCornerDp)
    var anchorIcon by mutableStateOf(anchorIcon)
    var anchorIconSize by mutableStateOf(anchorIconSize)
    var onPresentingChanged by mutableStateOf(onPresentingChanged)
    var onOrbVisibilityChanged by mutableStateOf(onOrbVisibilityChanged)
}

/**
 * 根级 Vitro Popover 浮层宿主状态：将嵌套在 `.glassBackdrop(...)` 内部的 Popover 菜单
 * 安全提升到 Backdrop 容器外部绘制，彻底避免 Vitro `drawInsidePanes()` 自引遮挡与 GPU  stalls。
 */
@Stable
class VitroPopoverHostState internal constructor() {
    internal val entries = mutableStateListOf<VitroPopoverEntry>()

    internal fun upsert(
        token: Any,
        visible: Boolean,
        anchorBoundsInRoot: Rect?,
        items: List<VitroGlassPopoverItem>,
        palette: AppPalette,
        style: ReadingStyle,
        onDismiss: () -> Unit,
        backdrop: GlassBackdrop?,
        width: Dp,
        anchorCornerDp: Dp,
        anchorIcon: ImageVector?,
        anchorIconSize: Dp,
        onPresentingChanged: ((Boolean) -> Unit)?,
        onOrbVisibilityChanged: ((Float) -> Unit)?,
    ) {
        val existing = entries.firstOrNull { it.token === token }
        if (existing != null) {
            existing.visible = visible
            if (anchorBoundsInRoot != null && anchorBoundsInRoot != Rect.Zero) {
                existing.anchorBoundsInRoot = anchorBoundsInRoot
            }
            existing.items = items
            existing.palette = palette
            existing.style = style
            existing.onDismiss = onDismiss
            existing.backdrop = backdrop
            existing.width = width
            existing.anchorCornerDp = anchorCornerDp
            existing.anchorIcon = anchorIcon
            existing.anchorIconSize = anchorIconSize
            existing.onPresentingChanged = onPresentingChanged
            existing.onOrbVisibilityChanged = onOrbVisibilityChanged
            if (visible && items.isNotEmpty()) {
                onPresentingChanged?.invoke(true)
                onOrbVisibilityChanged?.invoke(0f)
            }
        } else if (visible && items.isNotEmpty()) {
            onPresentingChanged?.invoke(true)
            onOrbVisibilityChanged?.invoke(0f)
            entries.add(
                VitroPopoverEntry(
                    token = token,
                    visible = true,
                    anchorBoundsInRoot = anchorBoundsInRoot,
                    items = items,
                    palette = palette,
                    style = style,
                    onDismiss = onDismiss,
                    backdrop = backdrop,
                    width = width,
                    anchorCornerDp = anchorCornerDp,
                    anchorIcon = anchorIcon,
                    anchorIconSize = anchorIconSize,
                    onPresentingChanged = onPresentingChanged,
                    onOrbVisibilityChanged = onOrbVisibilityChanged,
                ),
            )
        } else if (!visible) {
            onOrbVisibilityChanged?.invoke(1f)
            onPresentingChanged?.invoke(false)
        }
    }

    internal fun remove(token: Any) {
        entries.filter { it.token === token }.forEach { entry ->
            entry.onOrbVisibilityChanged?.invoke(1f)
            entry.onPresentingChanged?.invoke(false)
        }
        entries.removeAll { it.token === token }
    }

    internal fun settleIfClosed(token: Any) {
        val entry = entries.firstOrNull { it.token === token } ?: return
        if (!entry.visible) {
            entry.onOrbVisibilityChanged?.invoke(1f)
            entry.onPresentingChanged?.invoke(false)
            entries.remove(entry)
        }
    }
}

@Composable
fun rememberVitroPopoverHostState(): VitroPopoverHostState =
    remember { VitroPopoverHostState() }

val LocalVitroPopoverHost = staticCompositionLocalOf<VitroPopoverHostState?> { null }

/**
 * 根级 Vitro Popover 叠加宿主：必须放置在 `.glassBackdrop(rootBackdrop)` 容器之外的顶层 `Box` 中。
 */
@Composable
fun VitroPopoverOverlayHost(
    hostState: VitroPopoverHostState,
    fallbackBackdrop: GlassBackdrop? = null,
) {
    val entries = hostState.entries
    if (entries.isEmpty()) return

    Box(modifier = Modifier.fillMaxSize()) {
        for (entry in entries) {
            key(entry.token) {
                VitroGlassPopoverMenuCore(
                    visible = entry.visible,
                    anchorBoundsInRoot = entry.anchorBoundsInRoot,
                    items = entry.items,
                    palette = entry.palette,
                    readingStyle = entry.style,
                    onDismiss = entry.onDismiss,
                    backdrop = entry.backdrop ?: fallbackBackdrop ?: LocalVitroBackdrop.current,
                    width = entry.width,
                    anchorCornerDp = entry.anchorCornerDp,
                    anchorIcon = entry.anchorIcon,
                    anchorIconSize = entry.anchorIconSize,
                    animateOnEnter = true,
                    onPresentingChanged = entry.onPresentingChanged,
                    onOrbVisibilityChanged = entry.onOrbVisibilityChanged,
                    onSettled = { hostState.settleIfClosed(entry.token) },
                )
            }
        }
    }
}

/**
 * YumeBox Vitro-Safe 窗内展开液态玻璃 Popover 菜单：
 * - 严禁使用 PopupWindow / DropdownMenu（避免跨 Window 破坏 Vitro Backdrop 坐标与模糊共享）；
 * - 当 `LocalVitroPopoverHost.current != null` 时自动委托到 Backdrop 外部的 `VitroPopoverOverlayHost` 绘制，
 *   避免 Vitro `drawInsidePanes()` 将玻璃面板覆盖到菜单文字上方；
 * - 严禁对 `.glassEffect` 节点施加 `graphicsLayer` 缩放/平移，通过 `Layout` 驱动宽/高/位置/圆角展开；
 * - 仅对预测量内层选项 `Column` 与退场按钮图标拷贝施加轻量级过渡 + 淡入淡出。
 */
@Composable
fun VitroGlassPopoverMenu(
    visible: Boolean,
    anchorBoundsInRoot: Rect?,
    items: List<VitroGlassPopoverItem>,
    palette: AppPalette,
    onDismiss: () -> Unit,
    backdrop: GlassBackdrop? = LocalVitroBackdrop.current,
    width: Dp = 204.dp,
    anchorCornerDp: Dp = 22.dp,
    anchorIcon: ImageVector? = null,
    anchorIconSize: Dp = 20.dp,
    onPresentingChanged: ((Boolean) -> Unit)? = null,
    onOrbVisibilityChanged: ((Float) -> Unit)? = null,
) {
    val readingStyle = LocalReadingStyle.current
    val host = LocalVitroPopoverHost.current
    if (host != null) {
        val token = remember { Any() }
        if (visible && items.isNotEmpty()) {
            onPresentingChanged?.invoke(true)
            onOrbVisibilityChanged?.invoke(0f)
        }
        SideEffect {
            host.upsert(
                token = token,
                visible = visible,
                anchorBoundsInRoot = anchorBoundsInRoot,
                items = items,
                palette = palette,
                style = readingStyle,
                onDismiss = onDismiss,
                backdrop = backdrop,
                width = width,
                anchorCornerDp = anchorCornerDp,
                anchorIcon = anchorIcon,
                anchorIconSize = anchorIconSize,
                onPresentingChanged = onPresentingChanged,
                onOrbVisibilityChanged = onOrbVisibilityChanged,
            )
        }
        DisposableEffect(host, token) {
            onDispose {
                host.remove(token)
            }
        }
        return
    }

    VitroGlassPopoverMenuCore(
        visible = visible,
        anchorBoundsInRoot = anchorBoundsInRoot,
        items = items,
        palette = palette,
        readingStyle = readingStyle,
        onDismiss = onDismiss,
        backdrop = backdrop,
        width = width,
        anchorCornerDp = anchorCornerDp,
        anchorIcon = anchorIcon,
        anchorIconSize = anchorIconSize,
        animateOnEnter = false,
        onPresentingChanged = onPresentingChanged,
        onOrbVisibilityChanged = onOrbVisibilityChanged,
        onSettled = null,
    )
}

@Composable
private fun VitroGlassPopoverMenuCore(
    visible: Boolean,
    anchorBoundsInRoot: Rect?,
    items: List<VitroGlassPopoverItem>,
    palette: AppPalette,
    readingStyle: ReadingStyle,
    onDismiss: () -> Unit,
    backdrop: GlassBackdrop?,
    width: Dp,
    anchorCornerDp: Dp,
    anchorIcon: ImageVector?,
    anchorIconSize: Dp,
    animateOnEnter: Boolean,
    onPresentingChanged: ((Boolean) -> Unit)?,
    onOrbVisibilityChanged: ((Float) -> Unit)? = null,
    onSettled: (() -> Unit)?,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnPresentingChanged by rememberUpdatedState(onPresentingChanged)
    val currentOnOrbVisibilityChanged by rememberUpdatedState(onOrbVisibilityChanged)

    var overlayOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var lastValidAnchorBounds by remember { mutableStateOf<Rect?>(null) }
    if (anchorBoundsInRoot != null && anchorBoundsInRoot != Rect.Zero) {
        lastValidAnchorBounds = anchorBoundsInRoot
    }

    val transitionState = remember { MutableTransitionState(initialState = !animateOnEnter && visible) }
    transitionState.targetState = visible
    val transition = rememberTransition(transitionState, label = "vitroPopoverTransition")

    // iOS Liquid Glass Popover dual-spring decoupling:
    // Opening: center leaps decisively out of the anchor orb while width/height balloon behind it.
    // Closing: accelerates briskly back into the anchor orb to drive the kinetic landing recoil.
    val centerT by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = if (targetState) 0.76f else 0.80f,
                stiffness = if (targetState) 620f else 680f,
                visibilityThreshold = 0.001f,
            )
        },
        label = "vitroPopoverCenterT",
    ) { target -> if (target) 1f else 0f }

    val rawWidthT by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = if (targetState) 0.75f else 0.80f,
                stiffness = if (targetState) 360f else 640f,
                visibilityThreshold = 0.001f,
            )
        },
        label = "vitroPopoverWidthT",
    ) { target -> if (target) 1f else 0f }

    val rawHeightT by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = if (targetState) 0.78f else 0.80f,
                stiffness = if (targetState) 275f else 620f,
                visibilityThreshold = 0.001f,
            )
        },
        label = "vitroPopoverHeightT",
    ) { target -> if (target) 1f else 0f }

    val widthMorphT = rawWidthT.coerceAtLeast(0f)
    val heightMorphT = rawHeightT.coerceAtLeast(0f)
    val widthProgress = rawWidthT.coerceIn(0f, 1f)
    val heightProgress = rawHeightT.coerceIn(0f, 1f)
    val centerProgress = centerT.coerceIn(0f, 1f)
    val boundsT = maxOf(widthProgress, heightProgress)
    val collapseMetric = maxOf(boundsT, centerProgress)

    // Menu text fades out faster on close so items dissolve cleanly before orb shrinks to anchor.
    // Strict zero-concurrency `+` handoff: on close `[2]` anchorIcon fades IN 0.44->0.28, eases to
    // exact anchor center and fades completely OUT to 0f across 0.24->0.13, BEFORE Dock's resting orb
    // reveals 0f->1f across 0.12->0.01 (eliminating any overlapping offset `++` double crosshair).
    val iconAlignT = if (visible) {
        0f
    } else {
        val rawAlign = ((0.28f - collapseMetric) / 0.15f).coerceIn(0f, 1f)
        rawAlign * rawAlign * (3f - 2f * rawAlign)
    }
    val popoverIconAlpha = if (visible) {
        (1f - boundsT / 0.28f).coerceIn(0f, 1f)
    } else {
        val fadeIn = (1f - ((boundsT - 0.28f).coerceAtLeast(0f) / 0.16f)).coerceIn(0f, 1f)
        val fadeOutRaw = ((collapseMetric - 0.13f) / 0.11f).coerceIn(0f, 1f)
        val fadeOut = fadeOutRaw * fadeOutRaw * (3f - 2f * fadeOutRaw)
        fadeIn * fadeOut
    }
    val minMorphProgress = minOf(widthProgress, heightProgress)
    val contentFade = if (visible) {
        ((minMorphProgress - 0.30f) / 0.42f).coerceIn(0f, 1f)
    } else {
        ((minMorphProgress - 0.36f) / 0.34f).coerceIn(0f, 1f)
    }

    var closingSettledOut by remember { mutableStateOf(false) }
    if (visible) {
        closingSettledOut = false
    }

    val isSettledClosed = !visible &&
        !transition.targetState &&
        !transition.currentState &&
        !transition.isRunning &&
        collapseMetric <= 0.004f
    val arrivedAtStart = !visible && (
        isSettledClosed || (
            centerProgress <= 0.004f &&
                widthProgress <= 0.004f &&
                heightProgress <= 0.004f
            )
        )
    // Keep active until closingSettledOut flips alongside onPresentingChanged(false)
    val active = items.isNotEmpty() && !closingSettledOut

    // Continuous handoff between Popover surface and underlying Dock `+` orb after `[2]` is 0f:
    // Dock orb stays at 0f until collapseMetric <= 0.12f, then crossfades cleanly 0f -> 1f across
    // 0.12f -> 0.01f while Popover surface/elevation crossfade 1f -> 0f.
    val orbVisibilityFraction: Float
    val popoverSurfaceAlpha: Float
    val popoverElevationFactor: Float
    when {
        items.isEmpty() -> {
            orbVisibilityFraction = 1f
            popoverSurfaceAlpha = 0f
            popoverElevationFactor = 0f
        }
        visible -> {
            orbVisibilityFraction = 0f
            popoverSurfaceAlpha = 1f
            popoverElevationFactor = 1f
        }
        else -> {
            val dockRampRaw = ((0.12f - collapseMetric) / 0.11f).coerceIn(0f, 1f)
            val dockTakeover = dockRampRaw * dockRampRaw * (3f - 2f * dockRampRaw)
            orbVisibilityFraction = if (arrivedAtStart) 1f else dockTakeover
            popoverSurfaceAlpha = if (arrivedAtStart) 0f else (1f - dockTakeover).coerceIn(0f, 1f)
            popoverElevationFactor = (1f - orbVisibilityFraction).coerceIn(0f, 1f)
        }
    }

    if (visible && items.isNotEmpty()) {
        currentOnPresentingChanged?.invoke(true)
        currentOnOrbVisibilityChanged?.invoke(0f)
    }

    if (active) {
        SideEffect {
            if (visible && items.isNotEmpty()) {
                currentOnPresentingChanged?.invoke(true)
            } else if (!visible && (items.isEmpty() || arrivedAtStart)) {
                currentOnOrbVisibilityChanged?.invoke(1f)
                currentOnPresentingChanged?.invoke(false)
            }
            currentOnOrbVisibilityChanged?.invoke(orbVisibilityFraction)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentOnOrbVisibilityChanged?.invoke(1f)
        }
    }

    LaunchedEffect(
        visible,
        arrivedAtStart,
        isSettledClosed,
        items.isEmpty(),
    ) {
        if (visible && items.isNotEmpty()) {
            currentOnPresentingChanged?.invoke(true)
            currentOnOrbVisibilityChanged?.invoke(0f)
        } else if (!visible && (items.isEmpty() || arrivedAtStart)) {
            currentOnOrbVisibilityChanged?.invoke(1f)
            currentOnPresentingChanged?.invoke(false)
            closingSettledOut = true
            onSettled?.invoke()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                if (pos != overlayOriginInRoot) {
                    overlayOriginInRoot = pos
                }
            },
    ) {
        if (!active) return@Box

        BackHandler(enabled = visible) {
            currentOnDismiss()
        }

        val panelGeometry = remember { VitroPopoverPanelGeometry() }
        val clipPath = remember { Path() }
        val glassState = if (backdrop != null) rememberGlassEffectState() else null
        val scrimInteraction = remember { MutableInteractionSource() }

        val curCornerDp = lerpFloat(anchorCornerDp.value, 24f, boundsT).coerceAtLeast(4f).dp
        val glassShape = RoundedCornerShape(curCornerDp)
        val isLight = palette.isLight
        val isDark = LocalGlassDarkTheme.current ?: !isLight
        val opacity = readingStyle.glassOpacityLevel.coerceIn(0f, 1f)
        val baseTintAlpha = ios27BaseTintAlpha(readingStyle.glassClarityMode, opacity, isDark)
        val rawTintT = ((boundsT - 0.14f) / 0.56f).coerceIn(0f, 1f)
        val smoothTintT = rawTintT * rawTintT * (3f - 2f * rawTintT)
        val powerSave = LocalPowerSaveMode.current
        val usingFallback = backdrop == null || powerSave
        val isFromLargeOrb = anchorCornerDp > 25.dp
        val dockShadowCache = remember { Ios27DockElevationRenderCache() }
        val dockRimCache = remember { Ios27DockElevationRenderCache() }
        val dockLightRimBrush = rememberDockOpticalRimBrush(isDark = false)
        val morphTint = if (usingFallback) {
            val restingFallbackAlpha = when (readingStyle.glassClarityMode) {
                GlassClarityMode.clear -> 0.56f + opacity * 0.20f
                GlassClarityMode.regular -> 0.74f + opacity * 0.16f
                GlassClarityMode.tinted -> 0.86f + opacity * 0.12f
            }.coerceIn(0.45f, 0.96f)
            val restingFallback = when {
                isDark -> Color(0xFF262832).copy(alpha = restingFallbackAlpha)
                isFromLargeOrb -> Color(0xFFECEDF3).copy(alpha = (0.86f + opacity * 0.08f).coerceIn(0.84f, 0.95f))
                else -> Color(0xFFFDFDFF).copy(alpha = restingFallbackAlpha)
            }
            val expandedFallback = if (isDark) {
                Color(0xFF23252E).copy(alpha = (restingFallbackAlpha + 0.10f).coerceAtMost(0.95f))
            } else {
                Color(0xFFF8F9FC).copy(alpha = (restingFallbackAlpha + 0.08f).coerceAtMost(0.97f))
            }
            lerpColor(restingFallback, expandedFallback, smoothTintT)
        } else if (!isDark && isFromLargeOrb) {
            val dockBaseTint = ios27DockMilkyTintColor(palette = palette, style = readingStyle)
            val menuTint = ios27ReaderSheetTint(baseTintAlpha, isDark = false)
            lerpColor(dockBaseTint, menuTint, smoothTintT)
        } else if (smoothTintT <= 0.001f) {
            Color.Unspecified
        } else {
            val chromeTint = ios27ChromeTint(baseTintAlpha, isDark)
            val menuTint = ios27ReaderSheetTint(baseTintAlpha, isDark)
            lerpColor(chromeTint, menuTint, smoothTintT)
        }
        val view = LocalView.current
        val coroutineScope = rememberCoroutineScope()

        val rowTops = remember(items.size) { FloatArray(items.size) { -1f } }
        val rowHeights = remember(items.size) { FloatArray(items.size) { 0f } }
        val panelLeftRef = remember { floatArrayOf(0f) }
        val panelTopRef = remember { floatArrayOf(0f) }

        var isMenuTouching by remember { mutableStateOf(false) }
        var menuOverlayPointer by remember { mutableStateOf(Offset.Unspecified) }
        var hoveredIndex by remember { mutableStateOf<Int?>(null) }
        val highlightAlphaAnim = remember { Animatable(0f) }
        val highlightTopAnim = remember { Animatable(0f) }
        val highlightHeightAnim = remember { Animatable(0f) }
        val dragPullX = remember { Animatable(0f) }
        val dragPullY = remember { Animatable(0f) }

        LaunchedEffect(visible, items.size) {
            if (!visible) {
                isMenuTouching = false
                hoveredIndex = null
                launch { highlightAlphaAnim.animateTo(0f, tween(durationMillis = 75)) }
                launch { dragPullX.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 520f)) }
                launch { dragPullY.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 520f)) }
            }
        }

        LaunchedEffect(hoveredIndex, isMenuTouching) {
            val idx = hoveredIndex
            if (isMenuTouching && idx != null && idx in items.indices) {
                val targetTop = rowTops[idx]
                val targetHeight = rowHeights[idx]
                if (targetTop >= 0f && targetHeight > 1f) {
                    val jumpImmediate = highlightAlphaAnim.value < 0.06f
                    if (jumpImmediate) {
                        highlightTopAnim.snapTo(targetTop)
                        highlightHeightAnim.snapTo(targetHeight)
                    } else {
                        launch {
                            highlightTopAnim.animateTo(
                                targetTop,
                                spring(dampingRatio = 0.80f, stiffness = 680f),
                            )
                        }
                        launch {
                            highlightHeightAnim.animateTo(
                                targetHeight,
                                spring(dampingRatio = 0.80f, stiffness = 680f),
                            )
                        }
                    }
                    highlightAlphaAnim.animateTo(1f, tween(durationMillis = 65))
                }
            } else {
                highlightAlphaAnim.animateTo(0f, tween(durationMillis = 155))
            }
        }

        fun resolveHoveredRow(overlayPos: Offset): Int? {
            if (items.isEmpty()) return null
            val colX = overlayPos.x - (panelLeftRef[0] + dragPullX.value)
            val colY = overlayPos.y - (panelTopRef[0] + dragPullY.value)
            val w = panelGeometry.endW
            val h = panelGeometry.endH
            val edgeOutX = with(density) { 42.dp.toPx() }
            val edgeOutY = with(density) { 28.dp.toPx() }
            if (colX < -edgeOutX || colX > w + edgeOutX || colY < -edgeOutY || colY > h + edgeOutY) {
                return null
            }
            for (i in items.indices) {
                val top = rowTops[i]
                val rowH = rowHeights[i]
                if (top >= 0f && rowH > 0f && colY >= top - 3f && colY <= top + rowH + 3f) {
                    return i
                }
            }
            if (colY in 0f..h) {
                val firstTop = rowTops.firstOrNull { it >= 0f } ?: return null
                if (colY < firstTop) return 0
                return items.lastIndex
            }
            return null
        }

        val marginPx = with(density) { 12.dp.toPx() }
        val targetWidthPx = with(density) { width.roundToPx().coerceAtLeast(120) }
        val fallbackAnchorPx = with(density) { 44.dp.toPx() }

        val panelLocalPointer = if (menuOverlayPointer != Offset.Unspecified) {
            Offset(
                x = menuOverlayPointer.x - (panelLeftRef[0] + dragPullX.value),
                y = menuOverlayPointer.y - (panelTopRef[0] + dragPullY.value),
            )
        } else {
            Offset.Unspecified
        }

        Layout(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (visible && items.isNotEmpty()) {
                        Modifier.pointerInput(visible, items) {
                            val touchSlopPx = with(density) { 8.dp.toPx() }
                            val maxPullXPx = with(density) { 12.dp.toPx() }
                            val maxPullYPx = with(density) { 12.dp.toPx() }
                            awaitEachGesture {
                                val firstDownEvent = awaitPointerEvent(PointerEventPass.Main)
                                val down = firstDownEvent.changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                                val startPos = down.position
                                val pLeft = panelLeftRef[0]
                                val pTop = panelTopRef[0]
                                val pRight = pLeft + panelGeometry.curW
                                val pBottom = pTop + panelGeometry.curH
                                val hitPadding = with(density) { 6.dp.toPx() }
                                val insidePanel = startPos.x >= pLeft - hitPadding &&
                                    startPos.x <= pRight + hitPadding &&
                                    startPos.y >= pTop - hitPadding &&
                                    startPos.y <= pBottom + hitPadding
                                if (!insidePanel) {
                                    // Consume outside tap directly here because this fillMaxSize Layout
                                    // intercepts pointer events above sibling boxes underneath.
                                    down.consume()
                                    val pointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null) {
                                            currentOnDismiss()
                                            break
                                        }
                                        change.consume()
                                        if (!change.pressed) {
                                            currentOnDismiss()
                                            break
                                        }
                                    }
                                    return@awaitEachGesture
                                }

                                down.consume()
                                val pointerId = down.id
                                menuOverlayPointer = startPos
                                isMenuTouching = true
                                var currentHover = resolveHoveredRow(startPos)
                                hoveredIndex = currentHover
                                var maxTravelPx = 0f

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val active = event.changes.firstOrNull { it.id == pointerId }
                                        ?: event.changes.firstOrNull()
                                        ?: break

                                    if (!active.pressed) {
                                        active.consume()
                                        val releaseHover = resolveHoveredRow(active.position)
                                        isMenuTouching = false
                                        hoveredIndex = null
                                        coroutineScope.launch {
                                            dragPullX.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                        coroutineScope.launch {
                                            dragPullY.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 420f))
                                        }
                                        if (releaseHover != null && releaseHover in items.indices) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            currentOnDismiss()
                                            items[releaseHover].onClick()
                                        } else if (maxTravelPx <= touchSlopPx) {
                                            currentOnDismiss()
                                        }
                                        break
                                    }

                                    active.consume()
                                    val pos = active.position
                                    menuOverlayPointer = pos
                                    val dx = pos.x - startPos.x
                                    val dy = pos.y - startPos.y
                                    maxTravelPx = maxOf(maxTravelPx, kotlin.math.hypot(dx, dy))

                                    // Elastic rubber-band displacement clamped smoothly under maxPullPx
                                    val rawPullX = maxPullXPx * kotlin.math.tanh(dx * 0.18f / maxPullXPx.coerceAtLeast(1f))
                                    val rawPullY = maxPullYPx * kotlin.math.tanh(dy * 0.11f / maxPullYPx.coerceAtLeast(1f))
                                    coroutineScope.launch {
                                        dragPullX.snapTo(rawPullX)
                                        dragPullY.snapTo(rawPullY)
                                    }

                                    val nextHover = resolveHoveredRow(pos)
                                    if (nextHover != currentHover) {
                                        currentHover = nextHover
                                        hoveredIndex = nextHover
                                        if (nextHover != null) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            content = {
                // [0] Vitro 玻璃背景面板：仅接受 Layout 精确尺寸与坐标，绝不挂载 graphicsLayer scale/translation；
                // 在大球起点（isFromLargeOrb）下同步绘制 Dock 乳霜 tint、ClipOp.Difference 双层投影与 0.65dp 光学描边，
                // 并在退场尾段与底层 `+` 球做连续交叉淡出，彻底消除回落光环闪灭。
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = popoverSurfaceAlpha }
                        .drawBehind {
                            if (!powerSave && !isDark && isFromLargeOrb && popoverElevationFactor > 0.004f) {
                                drawIos27DockOuterShadows(
                                    cache = dockShadowCache,
                                    shape = glassShape,
                                    isDark = false,
                                    shadowScale = popoverElevationFactor,
                                )
                            }
                        }
                        .ios27GlassSurface(
                            shape = glassShape,
                            palette = palette,
                            style = readingStyle,
                            variant = Ios27GlassVariant.Chrome,
                            backdrop = backdrop,
                            tintOverride = morphTint,
                            isInteractive = false,
                            state = glassState,
                            showContrastBorder = usingFallback,
                            shadowScale = 0f,
                            showFingerHighlight = true,
                            fingerPointerOverride = panelLocalPointer,
                            fingerPressedOverride = isMenuTouching && boundsT > 0.45f,
                            externalFingerControlled = true,
                        )
                        .drawBehind {
                            if (!usingFallback && !isDark && isFromLargeOrb) {
                                drawIos27DockOpticalRim(
                                    cache = dockRimCache,
                                    shape = glassShape,
                                    rimBrush = dockLightRimBrush,
                                    rimAlpha = popoverElevationFactor,
                                )
                            }
                        },
                )

                // [1] 预测量菜单项容器：外层按实时 curW/curH 裁剪至气泡轮廓，内层包含滑动液态高亮胶囊
                Box(
                    modifier = Modifier.drawWithContent {
                        clipPath.reset()
                        val w = panelGeometry.curW.coerceAtLeast(1f)
                        val h = panelGeometry.curH.coerceAtLeast(1f)
                        val r = curCornerDp.toPx().coerceAtMost(min(w, h) * 0.5f)
                        clipPath.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(r, r)))
                        clipPath(clipPath) {
                            this@drawWithContent.drawContent()
                        }
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .width(width)
                            .graphicsLayer {
                                val curH = panelGeometry.curH
                                val endH = panelGeometry.endH
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = 0f,
                                    pivotFractionY = if (panelGeometry.anchoredBottom) 1f else 0f,
                                )
                                translationX = 0f
                                translationY = if (panelGeometry.anchoredBottom) {
                                    curH - endH
                                } else {
                                    0f
                                }
                                val scale = lerpFloat(0.86f, 1.0f, contentFade)
                                scaleX = scale
                                scaleY = scale
                                alpha = contentFade
                            }
                            .drawBehind {
                                val hlAlpha = highlightAlphaAnim.value
                                val hlHeight = highlightHeightAnim.value
                                val hlTop = highlightTopAnim.value
                                if (hlAlpha > 0.01f && hlHeight > 2f) {
                                    // iOS 26/27 原生同心圆角选中胶囊：外框 24dp - 6dp = 18dp 同心圆角，无突兀描边，边距与每行严格居中对齐
                                    val hInset = 6.dp.toPx()
                                    val vInset = 1.dp.toPx()
                                    val pillRect = Rect(
                                        left = hInset,
                                        top = hlTop + vInset,
                                        right = (size.width - hInset).coerceAtLeast(hInset + 4f),
                                        bottom = hlTop + hlHeight - vInset,
                                    )
                                    val pillRadius = minOf(18.dp.toPx(), (pillRect.height * 0.44f).coerceAtLeast(10f))
                                    val pillRr = RoundRect(pillRect, CornerRadius(pillRadius, pillRadius))
                                    val pillPath = Path().apply { addRoundRect(pillRr) }
                                    val fillBrush = if (isLight) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.065f * hlAlpha),
                                                Color.Black.copy(alpha = 0.085f * hlAlpha),
                                            ),
                                            startY = pillRect.top,
                                            endY = pillRect.bottom,
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.16f * hlAlpha),
                                                Color.White.copy(alpha = 0.11f * hlAlpha),
                                            ),
                                            startY = pillRect.top,
                                            endY = pillRect.bottom,
                                        )
                                    }
                                    drawPath(path = pillPath, brush = fillBrush)
                                }
                            },
                    ) {
                        Spacer(modifier = Modifier.height(5.dp))
                        val dividerColor = palette.text.copy(alpha = if (isLight) 0.08f else 0.12f)
                        items.forEachIndexed { index, item ->
                            val isRowHovered = isMenuTouching && hoveredIndex == index
                            val itemColor = when {
                                item.tint.isSpecified -> item.tint
                                isRowHovered || item.selected -> palette.accentText
                                else -> palette.text
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .onGloballyPositioned { coords ->
                                        if (index in rowTops.indices) {
                                            rowTops[index] = coords.positionInParent().y
                                            rowHeights[index] = coords.size.height.toFloat()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (item.icon != null) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = itemColor,
                                        modifier = Modifier.size(19.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = item.label,
                                    color = itemColor,
                                    fontSize = 14.5.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (item.selected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = palette.accentText,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            if (index < items.lastIndex) {
                                val hideDividerForHover = isMenuTouching &&
                                    (hoveredIndex == index || hoveredIndex == index + 1)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(0.5.dp)
                                        .background(if (hideDividerForHover) Color.Transparent else dividerColor),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }

                // [2] 可选退场锚点图标拷贝（随中心轨迹移动、轻量缩放并与原按钮 1:1 尺寸对齐）
                if (anchorIcon != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(anchorIconSize + 6.dp)
                            .graphicsLayer {
                                val curW = panelGeometry.curW
                                val expandRatio = (curW / panelGeometry.startW.coerceAtLeast(1f)).coerceIn(0.88f, 1.24f)
                                val iconScale = lerpFloat(1f, expandRatio, boundsT * (1f - iconAlignT))
                                scaleX = iconScale
                                scaleY = iconScale
                                alpha = popoverIconAlpha
                            },
                    ) {
                        Icon(
                            imageVector = anchorIcon,
                            contentDescription = null,
                            tint = palette.text,
                            modifier = Modifier.size(anchorIconSize),
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            val overlayW = constraints.maxWidth.coerceAtLeast(1)
            val overlayH = constraints.maxHeight.coerceAtLeast(1)
            val overlayWf = overlayW.toFloat()
            val overlayHf = overlayH.toFloat()

            val effectiveWidthPx = targetWidthPx.coerceAtMost((overlayW - (marginPx * 2f).roundToInt()).coerceAtLeast(120))
            val maxMenuHeightPx = (overlayH - (marginPx * 2f).roundToInt()).coerceAtLeast(120)
            val contentPlaceable = measurables[1].measure(
                Constraints(
                    minWidth = effectiveWidthPx,
                    maxWidth = effectiveWidthPx,
                    minHeight = 0,
                    maxHeight = maxMenuHeightPx,
                ),
            )
            val endW = contentPlaceable.width.toFloat().coerceAtLeast(1f)
            val endH = contentPlaceable.height.toFloat().coerceAtLeast(1f)

            val rawAnchor = anchorBoundsInRoot ?: lastValidAnchorBounds
            val startRect = if (rawAnchor != null && rawAnchor != Rect.Zero) {
                rawAnchor.translate(-overlayOriginInRoot.x, -overlayOriginInRoot.y)
            } else {
                Rect(
                    left = overlayWf - marginPx - fallbackAnchorPx,
                    top = marginPx + fallbackAnchorPx,
                    right = overlayWf - marginPx,
                    bottom = marginPx + fallbackAnchorPx * 2f,
                )
            }
            val startW = startRect.width.coerceAtLeast(16f)
            val startH = startRect.height.coerceAtLeast(16f)

            val maxEndLeft = (overlayWf - marginPx - endW).coerceAtLeast(marginPx)
            val maxEndTop = (overlayHf - marginPx - endH).coerceAtLeast(marginPx)
            val anchoredRight = startRect.center.x >= overlayWf * 0.5f
            val anchoredBottom = startRect.center.y >= overlayHf * 0.55f
            val endLeft = if (anchoredRight) {
                (startRect.right - endW).coerceIn(marginPx, maxEndLeft)
            } else {
                startRect.left.coerceIn(marginPx, maxEndLeft)
            }
            val endTop = if (anchoredBottom) {
                (startRect.bottom - endH).coerceIn(marginPx, maxEndTop)
            } else {
                startRect.top.coerceIn(marginPx, maxEndTop)
            }

            // Dual-spring Popover choreography:
            // Center leaps first out of the anchor orb (`centerT`), while width & height fluidly balloon behind it.
            val curW = lerpFloat(startW, endW, widthMorphT)
                .coerceIn(16f, (overlayWf - 8f).coerceAtLeast(16f))
            val curH = lerpFloat(startH, endH, heightMorphT)
                .coerceIn(16f, (overlayHf - 8f).coerceAtLeast(16f))
            val endCx = endLeft + endW * 0.5f
            val endCy = endTop + endH * 0.5f
            val rawCurCx = lerpFloat(startRect.center.x, endCx, centerT)
            val rawCurCy = lerpFloat(startRect.center.y, endCy, centerT)

            val curLeft = (rawCurCx - curW * 0.5f).coerceIn(4f, (overlayWf - curW - 4f).coerceAtLeast(4f))
            val curTop = (rawCurCy - curH * 0.5f).coerceIn(4f, (overlayHf - curH - 4f).coerceAtLeast(4f))
            val curCx = curLeft + curW * 0.5f
            val curCy = curTop + curH * 0.5f

            panelGeometry.startW = startW
            panelGeometry.startH = startH
            panelGeometry.endW = endW
            panelGeometry.endH = endH
            panelGeometry.curW = curW
            panelGeometry.curH = curH
            panelGeometry.anchoredBottom = anchoredBottom
            panelLeftRef[0] = curLeft
            panelTopRef[0] = curTop

            val glassPlaceable = measurables[0].measure(
                Constraints.fixed(
                    curW.roundToInt().coerceAtLeast(1),
                    curH.roundToInt().coerceAtLeast(1),
                ),
            )
            val iconPlaceable = measurables.getOrNull(2)?.measure(Constraints())

            layout(overlayW, overlayH) {
                val x = (curLeft + dragPullX.value).roundToInt()
                val y = (curTop + dragPullY.value).roundToInt()
                glassPlaceable.placeRelative(x, y)
                contentPlaceable.placeRelative(x, y)
                if (iconPlaceable != null) {
                    val iconCx = lerpFloat(curCx, startRect.center.x, iconAlignT)
                    val iconCy = lerpFloat(curCy, startRect.center.y, iconAlignT)
                    iconPlaceable.placeRelative(
                        (iconCx + dragPullX.value - iconPlaceable.width * 0.5f).roundToInt(),
                        (iconCy + dragPullY.value - iconPlaceable.height * 0.5f).roundToInt(),
                    )
                }
            }
        }
    }
}

/**
 * iOS 27 Beta 1 Liquid Glass 开关（51x31dp）：
 * - 半透明胶囊轨道 + 发光白玻璃水滴滑块 + 顶部镜面高光；
 * - 按压或滑动切换时滑块横向弹性延展，支持点击与左右轻扫。
 */
@Composable
fun Ios27GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    palette: AppPalette,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isLight = palette.isLight
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)

    var isPressed by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val targetProgress = dragFraction ?: if (checked) 1f else 0f
    val thumbProgressState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
        label = "ios27ToggleProgress",
    )
    val stretchFactorState = animateFloatAsState(
        targetValue = if (isPressed || dragFraction != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 520f),
        label = "ios27ToggleStretch",
    )

    val effectiveOn = (dragFraction?.let { it >= 0.5f } ?: checked)
    val trackColorState = animateColorAsState(
        targetValue = when {
            effectiveOn -> palette.primary.copy(alpha = if (enabled) 0.88f else 0.40f)
            isLight -> Color(0xFF787880).copy(alpha = if (enabled) 0.20f else 0.10f)
            else -> Color(0xFF787880).copy(alpha = if (enabled) 0.36f else 0.16f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "ios27ToggleTrackColor",
    )

    val trackWidth = 51.dp
    val trackHeight = 31.dp
    val thumbHeightPx = with(density) { 27.dp.toPx() }
    val thumbStrokeWidthPx = with(density) { 0.85.dp.toPx() }
    val thumbRimStroke = remember(density) { Stroke(width = thumbStrokeWidthPx) }
    val thumbHighlightBrush = remember(thumbHeightPx) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.96f),
            0.55f to Color.White.copy(alpha = 0.22f),
            1.0f to Color(0xFF0E1117).copy(alpha = 0.06f),
            startY = 0f,
            endY = thumbHeightPx,
        )
    }
    val trackRimColor = if (effectiveOn) {
        Color.White.copy(alpha = if (isLight) 0.28f else 0.22f)
    } else {
        palette.text.copy(alpha = if (isLight) 0.08f else 0.14f)
    }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .drawBehind {
                val trackCorner = CornerRadius(size.height * 0.5f, size.height * 0.5f)
                drawRoundRect(
                    color = trackColorState.value,
                    size = size,
                    cornerRadius = trackCorner,
                )
                val stretch = stretchFactorState.value
                val progress = thumbProgressState.value.coerceIn(0f, 1f)
                val insetPx = 2.dp.toPx()
                val thumbH = (size.height - insetPx * 2f).coerceAtLeast(1f)
                val thumbW = (27f + 6f * stretch).dp.toPx()
                val maxOffsetPx = (size.width - insetPx * 2f - thumbW).coerceAtLeast(0f)
                val thumbLeft = insetPx + maxOffsetPx * progress
                val thumbTop = insetPx
                val thumbCorner = CornerRadius(thumbH * 0.5f, thumbH * 0.5f)
                // 轻微柔光投影
                drawRoundRect(
                    color = Color.Black.copy(alpha = if (isLight) 0.12f else 0.28f),
                    topLeft = Offset(thumbLeft, thumbTop + 1.5.dp.toPx()),
                    size = Size(thumbW, thumbH),
                    cornerRadius = thumbCorner,
                )
                // 玻璃白核心
                drawRoundRect(
                    color = Color.White.copy(alpha = if (enabled) (0.98f - 0.08f * stretch) else 0.72f),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(thumbW, thumbH),
                    cornerRadius = thumbCorner,
                )
                // 顶部镜面高光弧线
                withTransform({ translate(left = thumbLeft, top = thumbTop) }) {
                    drawRoundRect(
                        brush = thumbHighlightBrush,
                        topLeft = Offset(thumbStrokeWidthPx * 0.5f, thumbStrokeWidthPx * 0.5f),
                        size = Size(
                            (thumbW - thumbStrokeWidthPx).coerceAtLeast(1f),
                            (thumbH - thumbStrokeWidthPx).coerceAtLeast(1f),
                        ),
                        cornerRadius = thumbCorner,
                        style = thumbRimStroke,
                    )
                }
            }
            .border(0.5.dp, trackRimColor, CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val trackPx = size.width.toFloat().coerceAtLeast(1f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    var pointerId = down.id
                    var dragging = false
                    var aborted = false
                    isPressed = true

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            aborted = change == null || change.isConsumed
                            break
                        }
                        pointerId = change.id
                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        if (!dragging) {
                            if (abs(dx) > viewConfiguration.touchSlop * 0.55f && abs(dx) >= abs(dy)) {
                                dragging = true
                            } else if (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx) * 1.2f) {
                                aborted = true
                                break
                            }
                        }
                        if (dragging) {
                            change.consume()
                            val base = if (currentChecked) 1f else 0f
                            dragFraction = (base + dx / (trackPx * 0.55f)).coerceIn(0f, 1f)
                        }
                    }

                    val finalDrag = dragFraction
                    dragFraction = null
                    isPressed = false

                    if (!aborted) {
                        val nextChecked = if (dragging && finalDrag != null) {
                            finalDrag >= 0.5f
                        } else {
                            !currentChecked
                        }
                        if (nextChecked != currentChecked) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnCheckedChange(nextChecked)
                        }
                    }
                }
            },
    )
}

/**
 * iOS 27 Beta 1 精密液态玻璃滑块（44dp 触控热区）：
 * - 4dp 纤细半透明轨道，无 M3 断轨间隙与大圆环；
 * - 22x14dp 精致横向胶囊白玻璃滑块，拖拽时轻盈悬浮放大并增强高光通透度。
 */
@Composable
fun Ios27PrecisionSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    palette: AppPalette,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color = palette.primary,
) {
    val density = LocalDensity.current
    val isLight = palette.isLight
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val currentRange by rememberUpdatedState(valueRange)

    var isActive by remember { mutableStateOf(false) }
    val dragLiftState = animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "ios27SliderLift",
    )

    val span = (valueRange.endInclusive - valueRange.start).takeIf { abs(it) > 1e-6f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val inactiveTrackColor = if (isLight) {
        Color(0xFF1C1C1E).copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.16f)
    }
    val thumbRimColor = if (isLight) {
        Color(0xFF0E1117).copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }

    val restThumbW = 22.dp
    val restThumbH = 14.dp
    val activeThumbW = 27.dp
    val activeThumbH = 17.5.dp
    val thumbPadPx = with(density) { (activeThumbW * 0.5f).toPx() }
    val thumbStrokeWidthPx = with(density) { 0.7.dp.toPx() }
    val thumbRimStroke = remember(density) { Stroke(width = thumbStrokeWidthPx) }
    val refHighlightH = with(density) { (activeThumbH - 1.4.dp).toPx().coerceAtLeast(1f) }
    val thumbHighlightBrush = remember(refHighlightH) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.95f),
            0.6f to Color.White.copy(alpha = 0.18f),
            1.0f to Color.Transparent,
            startY = 0f,
            endY = refHighlightH,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 140.dp)
            .height(44.dp)
            .pointerInput(valueRange) {
                fun valueForX(x: Float): Float {
                    val trackStart = thumbPadPx
                    val trackEnd = (size.width.toFloat() - thumbPadPx).coerceAtLeast(trackStart + 1f)
                    val trackLen = (trackEnd - trackStart).coerceAtLeast(1f)
                    val r = currentRange
                    val s = (r.endInclusive - r.start).takeIf { abs(it) > 1e-6f } ?: 1f
                    val f = ((x - trackStart) / trackLen).coerceIn(0f, 1f)
                    return r.start + f * s
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    var pointerId = down.id
                    var dragging = false
                    var aborted = false
                    var lastEmitted = currentValue
                    isActive = true

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            aborted = change == null || change.isConsumed
                            if (change != null && !dragging && !aborted) {
                                val next = valueForX(change.position.x)
                                if (next != lastEmitted) {
                                    currentOnValueChange(next)
                                }
                            }
                            break
                        }
                        pointerId = change.id
                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        if (!dragging) {
                            if (abs(dx) > viewConfiguration.touchSlop * 0.45f && abs(dx) >= abs(dy) * 0.85f) {
                                dragging = true
                            } else if (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx) * 1.2f) {
                                aborted = true
                                break
                            }
                        }
                        if (dragging) {
                            change.consume()
                            val next = valueForX(change.position.x)
                            if (next != lastEmitted) {
                                lastEmitted = next
                                currentOnValueChange(next)
                            }
                        }
                    }

                    isActive = false
                    if (!aborted) {
                        currentOnFinished?.invoke()
                    }
                }
            }
            .drawBehind {
                val lift = dragLiftState.value
                val trackH = 4.dp.toPx()
                val trackStart = thumbPadPx
                val trackEnd = (size.width - thumbPadPx).coerceAtLeast(trackStart + 1f)
                val trackLen = (trackEnd - trackStart).coerceAtLeast(1f)
                val centerY = size.height * 0.5f
                val trackTop = centerY - trackH * 0.5f
                val trackCorner = CornerRadius(trackH * 0.5f, trackH * 0.5f)

                // 底层半透明导轨
                drawRoundRect(
                    color = inactiveTrackColor,
                    topLeft = Offset(trackStart, trackTop),
                    size = Size(trackLen, trackH),
                    cornerRadius = trackCorner,
                )

                // 已激活进度的平滑导轨（与滑块无缝衔接，无断轨）
                val thumbCenterX = trackStart + trackLen * fraction
                val activeWidth = (thumbCenterX - trackStart).coerceAtLeast(0f)
                if (activeWidth > 0.5f) {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.88f),
                        topLeft = Offset(trackStart, trackTop),
                        size = Size(activeWidth, trackH),
                        cornerRadius = trackCorner,
                    )
                }

                // 精致横向白玻璃胶囊滑块（按压/拖拽时柔和放大抬升）
                val thumbW = lerpFloat(restThumbW.toPx(), activeThumbW.toPx(), lift)
                val thumbH = lerpFloat(restThumbH.toPx(), activeThumbH.toPx(), lift)
                val thumbLeft = thumbCenterX - thumbW * 0.5f
                val thumbTop = centerY - thumbH * 0.5f
                val thumbCorner = CornerRadius(thumbH * 0.5f, thumbH * 0.5f)

                drawRoundRect(
                    color = Color.Black.copy(alpha = (if (isLight) 0.12f else 0.30f) + 0.08f * lift),
                    topLeft = Offset(thumbLeft, thumbTop + (1.2f + 1.4f * lift).dp.toPx()),
                    size = Size(thumbW, thumbH),
                    cornerRadius = thumbCorner,
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.98f - 0.14f * lift),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(thumbW, thumbH),
                    cornerRadius = thumbCorner,
                )
                drawRoundRect(
                    color = thumbRimColor,
                    topLeft = Offset(thumbLeft + thumbStrokeWidthPx * 0.5f, thumbTop + thumbStrokeWidthPx * 0.5f),
                    size = Size((thumbW - thumbStrokeWidthPx).coerceAtLeast(1f), (thumbH - thumbStrokeWidthPx).coerceAtLeast(1f)),
                    cornerRadius = thumbCorner,
                    style = thumbRimStroke,
                )
                // 顶部镜面高光条
                val innerW = (thumbW - thumbStrokeWidthPx * 2f).coerceAtLeast(1f)
                val innerH = (thumbH - thumbStrokeWidthPx * 2f).coerceAtLeast(1f)
                withTransform({
                    translate(left = thumbLeft + thumbStrokeWidthPx, top = thumbTop + thumbStrokeWidthPx)
                    scale(scaleX = 1f, scaleY = innerH / refHighlightH, pivot = Offset.Zero)
                }) {
                    drawRoundRect(
                        brush = thumbHighlightBrush,
                        topLeft = Offset.Zero,
                        size = Size(innerW, refHighlightH),
                        cornerRadius = CornerRadius(thumbCorner.x, refHighlightH * 0.5f),
                    )
                }
            },
    )
}

/**
 * iOS 27 Beta 1 磨砂玻璃分段胶囊切换器（零实心 Primary 色块）：
 * - 外层为低对比半透明胶囊；
 * - 选中指示器为带有顶部高光镜面棱边的磨砂玻璃滑块，随索引弹簧滑动。
 */
@Composable
fun <T> Ios27SegmentedPill(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    palette: AppPalette,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = LocalVitroBackdrop.current,
) {
    if (options.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val readingStyle = LocalReadingStyle.current
    val isLight = palette.isLight
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    val animatedIndexState = animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "ios27SegmentLens",
    )

    val opacity = readingStyle.glassOpacityLevel.coerceIn(0f, 1f)
    val trackBg = if (isLight) {
        Color(0xFF14161D).copy(alpha = 0.055f + opacity * 0.025f)
    } else {
        Color.White.copy(alpha = 0.085f + opacity * 0.035f)
    }
    val trackBorder = palette.text.copy(alpha = if (isLight) 0.04f else 0.065f)
    val lensBg = if (isLight) {
        Color.White.copy(alpha = 0.76f + opacity * 0.16f)
    } else {
        Color.White.copy(alpha = 0.16f + opacity * 0.10f)
    }
    val lensRimBrush = remember(isLight) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = if (isLight) 0.42f else 0.18f),
            0.5f to Color.White.copy(alpha = if (isLight) 0.12f else 0.05f),
            1.0f to Color.White.copy(alpha = if (isLight) 0.20f else 0.08f),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .height(42.dp)
            .clip(CircleShape)
            .background(trackBg, CircleShape)
            .border(0.5.dp, trackBorder, CircleShape)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / options.size

        // 磨砂玻璃滑动指示胶囊
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = size.width * animatedIndexState.value
                }
                .width(segmentWidth)
                .fillMaxHeight()
                .drawBehind {
                    if (isLight) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.05f),
                            topLeft = Offset(0f, 1.dp.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(size.height * 0.5f, size.height * 0.5f),
                        )
                    }
                }
                .clip(CircleShape)
                .background(lensBg, CircleShape)
                .border(0.65.dp, lensRimBrush, CircleShape),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (item, label) ->
                val isSelected = item == selected
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(item)
                            }
                        }
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) palette.text else palette.muted,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun inverseRubberBand(offset: Float, limit: Float): Float {
    if (limit <= 0f || abs(offset) < 0.01f) return 0f
    val resistance = (limit * RUBBER_BAND_RESISTANCE_FACTOR).coerceAtLeast(1f)
    val ratio = (offset / limit).coerceIn(-0.96f, 0.96f).toDouble()
    return (0.5 * resistance * ln((1.0 + ratio) / (1.0 - ratio))).toFloat()
}

/**
 * Vitro 兼容的 120Hz 液态玻璃可拖拽交互卡片修饰符：
 * - 默认（`useLayoutOffset = false`）：`gestureModifier` 置于 `.graphicsLayer { scaleX; scaleY; translationX; translationY }` 之前以保持指针坐标系恒定，
 *   配合 `.drawWithContent` 同步刷新折射采样并禁用 `isInteractive` 内部 `MotionStretch` / 指针回调，消除 `.offset` 引发的布局抖动与抽搐；
 * - 顶部融合药丸（`useLayoutOffset = true`）：`gestureModifier` 置于 `.offset { IntOffset(...) }` 之前且同样关闭 Vitro `isInteractive`，
 *   在不产生 `MotionStretch` 震荡的前提下同步更新布局边界以支持 `glassEffectContainer` 融合；
 * - 按压即触发弹性水滴微放大（大卡片 `1.028x`、小药丸 `1.06x`）与实时手指高光（`vitroFingerSpotlight`），采用 `tanh` 双曲弹性阻尼曲线与柔和弹簧回弹；
 * - 扣除 `touchSlop` 死区后再累加橡皮筋位移，并在任意 `change.isConsumed` 时立即让渡给父级滚动并回弹；
 * - 松手结合 `VelocityTracker` 释放速度与双轴 `Animatable` 弹簧平滑回弹，拖拽锁定后取消 `onClick`。
 */
@Composable
fun Modifier.vitroInteractiveCard(
    shape: CornerBasedShape = RoundedCornerShape(26.dp),
    variant: Ios27GlassVariant = Ios27GlassVariant.Card,
    palette: AppPalette,
    style: ReadingStyle? = null,
    backdrop: GlassBackdrop? = null,
    verticalOnly: Boolean = false,
    maxPullDp: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    useLayoutOffset: Boolean = false,
    pressScaleTarget: Float? = null,
    showFingerHighlight: Boolean = true,
): Modifier {
    val resolvedStyle = style ?: LocalReadingStyle.current
    val powerSave = LocalPowerSaveMode.current
    val resolvedBackdrop = if (powerSave) null else (backdrop ?: LocalVitroBackdrop.current)
    val haptics = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val cardInteractionSource = remember { MutableInteractionSource() }

    if (powerSave) {
        val clickModifier = if (currentOnClick != null) {
            Modifier.clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnClick?.invoke()
                },
            )
        } else {
            Modifier
        }
        return this
            .then(clickModifier)
            .ios27GlassSurface(
                shape = shape,
                palette = palette,
                style = resolvedStyle,
                variant = variant,
                backdrop = null,
                isInteractive = false,
                state = null,
                interactionSource = cardInteractionSource,
                showContrastBorder = false,
                showFingerHighlight = false,
            )
    }

    val density = LocalDensity.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val maxPullPx = with(density) { maxPullDp.toPx().coerceAtLeast(0f) }
    val clickThresholdPx = maxOf(touchSlop * 1.2f, with(density) { 18.dp.toPx() })
    val scope = rememberCoroutineScope()
    val dragOffsetX = remember { mutableFloatStateOf(0f) }
    val dragOffsetY = remember { mutableFloatStateOf(0f) }
    var cardSizePx by remember { mutableStateOf(IntSize.Zero) }
    var rawFingerPos by remember { mutableStateOf(Offset.Unspecified) }
    var fingerPressed by remember { mutableStateOf(false) }
    var snapBackJob by remember { mutableStateOf<Job?>(null) }

    val resolvedPressTarget = pressScaleTarget ?: run {
        if (cardSizePx.width > 0 && cardSizePx.height > 0) {
            val minDimDp = min(cardSizePx.width, cardSizePx.height) / density.density
            val maxDimDp = max(cardSizePx.width, cardSizePx.height) / density.density
            if (minDimDp <= 58f || maxDimDp <= 190f) 1.06f else 1.028f
        } else {
            1.028f
        }
    }
    val pressScaleState = animateFloatAsState(
        targetValue = if (fingerPressed) resolvedPressTarget else 1f,
        animationSpec = spring(
            dampingRatio = 0.64f,
            stiffness = 480f,
        ),
        label = "vitroInteractiveCardPressScale",
    )

    val fingerPointerProvider = remember {
        {
            val p = rawFingerPos
            if (p == Offset.Unspecified) {
                Offset.Unspecified
            } else {
                val s = pressScaleState.value.coerceAtLeast(0.5f)
                val cx = cardSizePx.width * 0.5f
                val cy = cardSizePx.height * 0.5f
                Offset(
                    x = cx + (p.x - dragOffsetX.floatValue - cx) / s,
                    y = cy + (p.y - dragOffsetY.floatValue - cy) / s,
                )
            }
        }
    }

    fun triggerSnapBack(initialVelocityX: Float = 0f, initialVelocityY: Float = 0f) {
        val startX = dragOffsetX.floatValue
        val startY = dragOffsetY.floatValue
        if (abs(startX) < 0.25f && abs(startY) < 0.25f &&
            abs(initialVelocityX) < 20f && abs(initialVelocityY) < 20f
        ) {
            dragOffsetX.floatValue = 0f
            dragOffsetY.floatValue = 0f
            return
        }
        snapBackJob?.cancel()
        snapBackJob = scope.launch {
            val snapSpec = spring<Float>(
                dampingRatio = 0.68f,
                stiffness = 360f,
                visibilityThreshold = 0.25f,
            )
            val jobX = launch {
                Animatable(startX).animateTo(
                    targetValue = 0f,
                    animationSpec = snapSpec,
                    initialVelocity = initialVelocityX,
                ) {
                    dragOffsetX.floatValue = value
                }
            }
            val jobY = launch {
                Animatable(startY).animateTo(
                    targetValue = 0f,
                    animationSpec = snapSpec,
                    initialVelocity = initialVelocityY,
                ) {
                    dragOffsetY.floatValue = value
                }
            }
            jobX.join()
            jobY.join()
            dragOffsetX.floatValue = 0f
            dragOffsetY.floatValue = 0f
        }
    }

    val semanticsModifier = if (onClick != null) {
        Modifier.semantics(mergeDescendants = false) {
            role = Role.Button
            onClick {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnClick?.invoke()
                true
            }
        }
    } else {
        Modifier
    }

    val gestureModifier = Modifier.pointerInput(verticalOnly, maxPullPx, touchSlop) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            snapBackJob?.cancel()
            val velocityTracker = VelocityTracker().apply {
                addPosition(down.uptimeMillis, down.position)
            }
            val startPos = down.position
            var pointerId = down.id
            var maxMoved = 0f
            var handedOff = false
            var committed = true
            var dragLocked = false
            var slopAnchorX = 0f
            var slopAnchorY = 0f
            var pressClosed = false
            val baseDeltaX = inverseRubberBand(dragOffsetX.floatValue, maxPullPx)
            val baseDeltaY = inverseRubberBand(dragOffsetY.floatValue, maxPullPx)
            val press = PressInteraction.Press(startPos)
            cardInteractionSource.tryEmit(press)
            rawFingerPos = startPos
            fingerPressed = true

            fun handOffGesture() {
                handedOff = true
                fingerPressed = false
                if (!pressClosed) {
                    cardInteractionSource.tryEmit(PressInteraction.Cancel(press))
                    pressClosed = true
                }
            }

            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == pointerId }
                        ?: event.changes.firstOrNull()
                    if (change == null || !change.pressed) {
                        committed = change != null
                        break
                    }
                    pointerId = change.id
                    if (change.isConsumed) {
                        handOffGesture()
                        break
                    }

                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val dx = change.position.x - startPos.x
                    val dy = change.position.y - startPos.y
                    val dist = hypot(dx, dy)
                    if (dist > maxMoved) maxMoved = dist

                    rawFingerPos = change.position

                    var consumedHere = false
                    if (verticalOnly) {
                        if (!dragLocked && abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.05f) {
                            handOffGesture()
                            break
                        } else if (maxPullPx > 0f && (dragLocked || abs(dy) > touchSlop)) {
                            if (!dragLocked) {
                                dragLocked = true
                                slopAnchorY = sign(dy) * min(abs(dy), touchSlop)
                            }
                            val effectiveDy = dy - slopAnchorY
                            dragOffsetY.floatValue = rubberBand2D(baseDeltaY + effectiveDy, maxPullPx)
                            change.consume()
                            consumedHere = true
                        }
                    } else if (maxPullPx > 0f && (dragLocked || dist > touchSlop)) {
                        if (!dragLocked) {
                            dragLocked = true
                            val slopScale = if (dist > 0f) min(dist, touchSlop) / dist else 0f
                            slopAnchorX = dx * slopScale
                            slopAnchorY = dy * slopScale
                        }
                        val effectiveDx = dx - slopAnchorX
                        val effectiveDy = dy - slopAnchorY
                        dragOffsetX.floatValue = rubberBand2D(baseDeltaX + effectiveDx, maxPullPx)
                        dragOffsetY.floatValue = rubberBand2D(baseDeltaY + effectiveDy, maxPullPx)
                        change.consume()
                        consumedHere = true
                    }

                    if (!consumedHere) {
                        val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                        val finalChange = finalEvent.changes.firstOrNull { it.id == pointerId }
                            ?: finalEvent.changes.firstOrNull()
                        if (finalChange == null || !finalChange.pressed) {
                            committed = finalChange != null
                            break
                        }
                        if (finalChange.isConsumed) {
                            handOffGesture()
                            break
                        }
                    }
                }
            } finally {
                fingerPressed = false
                if (!pressClosed) {
                    cardInteractionSource.tryEmit(
                        if (committed && !handedOff) PressInteraction.Release(press)
                        else PressInteraction.Cancel(press),
                    )
                    pressClosed = true
                }
                val releaseVelocity = if (committed && !handedOff && dragLocked) {
                    velocityTracker.calculateVelocity()
                } else {
                    Velocity.Zero
                }
                val snapVx = if (verticalOnly) 0f else (releaseVelocity.x * 0.30f).coerceIn(-1400f, 1400f)
                val snapVy = (releaseVelocity.y * 0.30f).coerceIn(-1400f, 1400f)
                triggerSnapBack(initialVelocityX = snapVx, initialVelocityY = snapVy)
            }

            if (committed && !handedOff && !dragLocked && maxMoved < clickThresholdPx) {
                currentOnClick?.let { click ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    click()
                }
            }
        }
    }

    val motionModifier = if (useLayoutOffset) {
        Modifier
            .offset {
                IntOffset(
                    dragOffsetX.floatValue.roundToInt(),
                    dragOffsetY.floatValue.roundToInt(),
                )
            }
            .graphicsLayer {
                val s = pressScaleState.value
                scaleX = s
                scaleY = s
            }
    } else {
        Modifier
            .graphicsLayer {
                val s = pressScaleState.value
                scaleX = s
                scaleY = s
                translationX = dragOffsetX.floatValue
                translationY = dragOffsetY.floatValue
            }
            .drawWithContent {
                val tx = dragOffsetX.floatValue
                val ty = dragOffsetY.floatValue
                val s = pressScaleState.value
                if (tx.isFinite() && ty.isFinite() && s.isFinite()) {
                    drawContent()
                } else {
                    drawContent()
                }
            }
    }

    return this
        .onSizeChanged { cardSizePx = it }
        .then(semanticsModifier)
        .then(gestureModifier)
        .then(motionModifier)
        .ios27GlassSurface(
            shape = shape,
            palette = palette,
            style = resolvedStyle,
            variant = variant,
            backdrop = resolvedBackdrop,
            // Always disable Vitro's internal MotionStretch & onPointerEvent here so
            // card drag displacement never oscillates against GlassNode local coordinates.
            isInteractive = false,
            state = null,
            interactionSource = cardInteractionSource,
            showContrastBorder = false,
            showFingerHighlight = showFingerHighlight,
            fingerPressedOverride = fingerPressed,
            externalFingerControlled = true,
            fingerPointerProvider = fingerPointerProvider,
        )
}
