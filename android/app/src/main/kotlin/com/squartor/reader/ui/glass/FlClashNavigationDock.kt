package com.squartor.reader.ui.glass

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.GlassClarityMode
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.glassEffectContainer
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

// 仅作用于凸透镜内部选中图标/文字层（uTexture 为 Compose 离屏内容纹理）：
// 中心 100% 锐利，边缘 11.2dp 随透镜升降做纯净球面折光弯曲与微磨砂（无红青色散杂边）。
@Language("AGSL")
private const val AGSL_FLCLASH_LENS_CONTENT_SHADER = """
uniform shader uTexture;
uniform float2 uLensOrigin;
uniform float2 uLensSize;
uniform float uLift;
uniform float uIsDark;
uniform float uDensity;

float roundedBoxDistance(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

float2 roundedBoxNormal(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    float2 softQ = max(q, float2(0.0));
    float lenQ = length(softQ);
    float2 n;
    if (lenQ > 1e-3) {
        n = softQ / lenQ;
    } else {
        float diff = q.x - q.y;
        float w = smoothstep(-4.0, 4.0, diff);
        n = normalize(mix(float2(0.0, 1.0), float2(1.0, 0.0), w));
    }
    return n * float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
}

half4 sampleFrostedContent(float2 pt, float2 tangent, float frost) {
    if (frost <= 0.15) {
        return uTexture.eval(pt);
    }
    half4 c0 = uTexture.eval(pt) * 0.50;
    half4 c1 = (uTexture.eval(pt + tangent * frost) + uTexture.eval(pt - tangent * frost)) * 0.25;
    return c0 + c1;
}

half4 main(float2 fragCoord) {
    if (uLift <= 0.005) {
        return uTexture.eval(fragCoord);
    }
    float dpr = max(uDensity, 1.0);
    float2 halfSize = uLensSize * 0.5;
    float halfMinor = min(halfSize.x, halfSize.y);
    float2 centered = (fragCoord - uLensOrigin) - halfSize;
    float dist = roundedBoxDistance(centered, halfSize, halfMinor);
    if (dist > 0.5) {
        return half4(0.0);
    }

    float inset = max(-dist, 0.0);
    float bevel = max(min(11.2 * dpr, halfMinor * 0.5), 1.0);
    float2 normal = roundedBoxNormal(centered, halfSize, max(halfMinor, bevel * 1.05));
    float rise = 1.0 - clamp(inset / bevel, 0.0, 1.0);

    float depth = min(14.0 * dpr * uLift, halfMinor) * (1.0 - sqrt(max(1.0 - rise * rise, 0.0)));
    float2 shift = -normal * depth;
    float frost = (2.2 * dpr * uLift) * rise * rise;
    float2 tangent = float2(-normal.y, normal.x);

    half4 base = sampleFrostedContent(fragCoord + shift, tangent, frost);

    float brightness = uIsDark > 0.5 ? (1.0 + 0.30 * uLift) : 1.0;
    float outA = float(base.a);
    float3 unpremul = outA > 1e-4 ? float3(base.rgb) / outA : float3(0.0);
    float3 lit = clamp(unpremul * brightness, float3(0.0), float3(1.0));
    return half4(half3(lit * outA), half(outA));
}
"""

data class DockDestination(
    val outlined: ImageVector,
    val filled: ImageVector,
    val label: String,
)

/**
 * 底部 Dock 右侧上下文独立动作圆球描述。
 */
data class DockContextualAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val onAnchorBoundsChanged: ((Rect) -> Unit)? = null,
    val hideOrb: Boolean = false,
    val orbVisibilityProvider: (() -> Float)? = null,
)

val defaultDockDestinations = listOf(
    DockDestination(Icons.Outlined.AutoStories, Icons.Filled.AutoStories, "在读"),
    DockDestination(Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Filled.LibraryBooks, "书架"),
    DockDestination(Icons.Outlined.Insights, Icons.Filled.Insights, "统计"),
    DockDestination(Icons.Outlined.Settings, Icons.Filled.Settings, "设置"),
)

private data class SpringSpec(
    val mass: Double = 1.0,
    val stiffness: Double,
    val dampingRatio: Double,
) {
    val damping: Double = 2.0 * dampingRatio * sqrt(stiffness * mass)

    companion object {
        fun withDurationAndBounce(durationMs: Int, bounce: Double = 0.0): SpringSpec {
            val durationSec = durationMs / 1000.0
            val stiffness = (2.0 * PI / durationSec) * (2.0 * PI / durationSec)
            val dampingRatio = if (bounce >= 0.0) (1.0 - bounce).coerceIn(0.05, 1.0) else 1.0
            return SpringSpec(mass = 1.0, stiffness = stiffness, dampingRatio = dampingRatio)
        }
    }
}

private class DockSpring(initial: Float) {
    var value by mutableFloatStateOf(initial)
        private set
    var target: Float = initial
        private set
    var velocity: Float = 0f
        private set
    var acceleration: Float = 0f
        private set

    private var spec: SpringSpec? = null
    private var startOffset: Double = 0.0
    private var startVelocity: Double = 0.0
    private var elapsedSec: Double = 0.0

    var isActive: Boolean = false
        private set

    var onActive: (() -> Unit)? = null

    fun springTo(newTarget: Float, springSpec: SpringSpec, velocityOverride: Float? = null) {
        val effectiveVel = velocityOverride ?: velocity
        if (isActive &&
            spec == springSpec &&
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
            acceleration = 0f
            isActive = false
            return
        }
        startOffset = (value - newTarget).toDouble()
        startVelocity = effectiveVel.toDouble()
        velocity = effectiveVel
        target = newTarget
        spec = springSpec
        elapsedSec = 0.0
        isActive = true
        onActive?.invoke()
    }

    fun jumpTo(newValue: Float) {
        value = newValue
        target = newValue
        velocity = 0f
        acceleration = 0f
        startOffset = 0.0
        startVelocity = 0.0
        elapsedSec = 0.0
        isActive = false
    }

    fun step(dtSeconds: Float): Boolean {
        if (!isActive) return false
        val s = spec ?: return false
        val dt = dtSeconds.coerceIn(0.0005f, 0.12f).toDouble()
        elapsedSec += dt
        val t = elapsedSec

        val m = s.mass
        val k = s.stiffness
        val c = s.damping
        val zeta = s.dampingRatio
        val wn = sqrt(k / m)

        val x0 = startOffset
        val v0 = startVelocity

        val disp: Double
        val vel: Double
        if (zeta < 1.0 - 1e-6) {
            val wd = wn * sqrt(1.0 - zeta * zeta)
            val decay = exp(-zeta * wn * t)
            val a = x0
            val b = (zeta * wn * x0 + v0) / wd
            val cosWt = cos(wd * t)
            val sinWt = sin(wd * t)
            disp = decay * (a * cosWt + b * sinWt)
            vel = decay * ((-zeta * wn * a + b * wd) * cosWt - (zeta * wn * b + a * wd) * sinWt)
        } else if (zeta <= 1.0 + 1e-6) {
            val r = -wn
            val c1 = x0
            val c2 = v0 - r * x0
            val env = exp(r * t)
            disp = (c1 + c2 * t) * env
            vel = (c2 + r * (c1 + c2 * t)) * env
        } else {
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
        val acc = (-k * disp - c * vel) / m

        value = x.toFloat()
        velocity = vel.toFloat()
        acceleration = acc.toFloat()

        if (abs(disp) < 0.0008 && abs(vel) < 0.008) {
            value = target
            velocity = 0f
            acceleration = 0f
            isActive = false
            return false
        }
        return true
    }
}

private val trackSpring = SpringSpec.withDurationAndBounce(120, bounce = 0.0)
private val pressSpring = SpringSpec(mass = 1.0, stiffness = 420.0, dampingRatio = 0.80)
private val followSpring = SpringSpec(mass = 1.0, stiffness = 920.0, dampingRatio = 0.88)
private val liftUpSpring = SpringSpec.withDurationAndBounce(160, bounce = 0.08)
private val liftDownSpring = SpringSpec.withDurationAndBounce(240, bounce = 0.0)
private val travelSpring = SpringSpec.withDurationAndBounce(360, bounce = 0.16)
private val jellySpring = SpringSpec(mass = 1.0, stiffness = 220.0, dampingRatio = 0.62)
private val glowInSpring = SpringSpec.withDurationAndBounce(100, bounce = 0.0)
private val glowOutSpring = SpringSpec.withDurationAndBounce(420, bounce = 0.0)

private const val BAR_HEIGHT_DP = 62f
private const val ORB_SIZE_DP = 58f
private const val LENS_OVERFLOW_PAD_DP = 14f
private const val TOTAL_CONTAINER_HEIGHT_DP = BAR_HEIGHT_DP + LENS_OVERFLOW_PAD_DP * 2f
private const val BAR_PADDING_DP = 4f
private const val PRESS_GROWTH = 0.125f
private const val MAX_PRESS_GROWTH_DP = 14f
private const val LENS_GROWTH_DP = 9f
private const val LENS_MAGNIFY = 0.20f
private const val LIFT_SNAP = 0.96f
private const val LIFT_RELEASE = 0.035f
private const val JELLY_SPEED = 3.0f
private const val JELLY_STRETCH = 0.18f
private const val JELLY_BRAKE = 25.0f
private const val JELLY_SQUASH = 0.13f
private const val GLOW_REACH_DP = 44f
private const val GLOW_GROWTH = 4f
private const val GLOW_PEAK = 0.24f
private const val OVERDRAG = 0.35f
private const val PULL_LIMIT = 7f / 32f
private const val PULL_STRETCH = 0.45f

private fun rubberBand(overshoot: Float, limit: Float): Float {
    if (limit <= 0f) return 0f
    val pull = 1f - 1f / (abs(overshoot) * 0.55f / limit + 1f)
    return limit * pull * sign(overshoot)
}

private fun saturate(v: Float): Float = (1.0 - 2.0 / (exp(2.0 * v.toDouble()) + 1.0)).toFloat()

/**
 * 基于 Vitro 0.1.0 (`glassEffectContainer(spacing = 10.dp)` + `glassEffect`) 的 iOS 27 底部液态玻璃导航栏：
 * - 底部主 4-Tab 胶囊与右侧上下文动作圆球同处一个 `glassEffectContainer(spacing = 10.dp)`，随显隐和近距离交互产生液态融合与分离；
 * - 保留 120Hz/144Hz 物理阻尼弹性滑块与跟手 scrub 拖拽、微放大光学折射与色散、分段触感反馈；
 * - 无 Vitro 上下文时优雅退化为 iOS 27 高对比磨砂胶囊。
 */
@Composable
fun FlClashNavigationDock(
    selectedIndex: Int,
    palette: AppPalette,
    hidden: Boolean,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    destinations: List<DockDestination> = defaultDockDestinations,
    contextualAction: DockContextualAction? = null,
    onOffscreenSettledChanged: ((Boolean) -> Unit)? = null,
    backdrop: GlassBackdrop? = null,
    batchMode: Boolean = false,
    batchAllSelected: Boolean = false,
    batchHasSelection: Boolean = false,
    onBatchSelectAll: () -> Unit = {},
    onBatchMoveToShelf: () -> Unit = {},
    onBatchMergeSeries: () -> Unit = {},
    onBatchDelete: () -> Unit = {},
    onBatchExit: () -> Unit = {},
    importInProgress: Boolean = false,
    importProgress: Float = 0f,
    importJustCompleted: Boolean = false,
    unmountWhenOffscreen: Boolean = true,
    suspendGlassBlur: Boolean = false,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val isLight = palette.isLight
    val powerSave = LocalPowerSaveMode.current
    val vitroBackdrop = if (powerSave) null else (backdrop ?: LocalVitroBackdrop.current)
    val readingStyle = LocalReadingStyle.current
    val lastIndex = max(0, destinations.size - 1)
    val safeSelected = selectedIndex.coerceIn(0, lastIndex)
    val currentSelected by rememberUpdatedState(safeSelected)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentOnOffscreenSettledChanged by rememberUpdatedState(onOffscreenSettledChanged)
    val currentBatchMode by rememberUpdatedState(batchMode)
    val currentBatchSelectAll by rememberUpdatedState(onBatchSelectAll)
    val currentBatchMoveToShelf by rememberUpdatedState(onBatchMoveToShelf)
    val currentBatchMergeSeries by rememberUpdatedState(onBatchMergeSeries)
    val currentBatchDelete by rememberUpdatedState(onBatchDelete)
    val currentBatchExit by rememberUpdatedState(onBatchExit)

    val batchProgressState = animateFloatAsState(
        targetValue = if (batchMode) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium),
        label = "dockBatchMode",
    )

    val lens = remember { DockSpring(safeSelected.toFloat()) }
    val lift = remember { DockSpring(0f) }
    val swell = remember { DockSpring(0f) }
    val stretch = remember { DockSpring(0f) }
    val jelly = remember { DockSpring(0f) }
    val glow = remember { DockSpring(0f) }
    val glowSpread = remember { DockSpring(0f) }

    var glowAt by remember { mutableStateOf(Offset.Zero) }
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    var dropping by remember { mutableStateOf(false) }

    LaunchedEffect(batchMode) {
        if (batchMode) {
            pressedIndex = null
            dropping = false
            lift.springTo(0f, liftDownSpring)
            swell.springTo(0f, pressSpring)
            stretch.springTo(0f, pressSpring)
            glow.springTo(0f, glowOutSpring)
            glowSpread.springTo(0f, glowOutSpring)
        }
    }

    val lensContentShader = remember(powerSave) {
        if (!powerSave && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(AGSL_FLCLASH_LENS_CONTENT_SHADER)
            } catch (_: Throwable) {
                null
            }
        } else null
    }

    LaunchedEffect(safeSelected) {
        if (pressedIndex == null && lens.target != safeSelected.toFloat()) {
            dropping = true
            lift.springTo(1f, liftUpSpring)
            lens.springTo(safeSelected.toFloat(), travelSpring)
        }
    }

    val choreographer = remember { Choreographer.getInstance() }
    val loopState = remember { DockSpringLoopState() }

    val frameCallback = remember {
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val dt = if (loopState.lastFrameNanos == 0L) {
                    0.008f
                } else {
                    ((frameTimeNanos - loopState.lastFrameNanos) / 1_000_000_000.0).toFloat()
                }
                loopState.lastFrameNanos = frameTimeNanos

                lens.step(dt)
                lift.step(dt)
                swell.step(dt)
                stretch.step(dt)
                jelly.step(dt)
                glow.step(dt)
                glowSpread.step(dt)

                if (lens.isActive || jelly.isActive) {
                    val v = lens.velocity
                    val braking = max(0f, -lens.acceleration * sign(v))
                    val jellyTarget = JELLY_STRETCH * saturate(abs(v) / JELLY_SPEED) -
                        JELLY_SQUASH * saturate(braking / JELLY_BRAKE)
                    if (abs(jellyTarget - jelly.target) > 0.001f) {
                        jelly.springTo(jellyTarget, jellySpring)
                    }
                }

                if (dropping && pressedIndex == null && lift.value >= LIFT_SNAP) {
                    if (abs(lens.target - lens.value) <= LIFT_RELEASE) {
                        dropping = false
                        lift.springTo(0f, liftDownSpring)
                    }
                }

                val anyActive = lens.isActive || lift.isActive || swell.isActive ||
                    stretch.isActive || jelly.isActive || glow.isActive || glowSpread.isActive ||
                    (dropping && lift.value > 0.001f)
                if (anyActive) {
                    choreographer.postFrameCallback(this)
                } else {
                    loopState.running = false
                    loopState.lastFrameNanos = 0L
                }
            }
        }
    }

    val ensureSpringLoop: () -> Unit = remember {
        {
            if (!loopState.running) {
                loopState.running = true
                loopState.lastFrameNanos = 0L
                choreographer.postFrameCallback(frameCallback)
            }
        }
    }

    DisposableEffect(frameCallback) {
        lens.onActive = ensureSpringLoop
        lift.onActive = ensureSpringLoop
        swell.onActive = ensureSpringLoop
        stretch.onActive = ensureSpringLoop
        jelly.onActive = ensureSpringLoop
        glow.onActive = ensureSpringLoop
        glowSpread.onActive = ensureSpringLoop

        if (lens.isActive || lift.isActive || swell.isActive ||
            stretch.isActive || jelly.isActive || glow.isActive || glowSpread.isActive
        ) {
            ensureSpringLoop()
        }

        onDispose {
            loopState.running = false
            loopState.lastFrameNanos = 0L
            choreographer.removeFrameCallback(frameCallback)
            lens.onActive = null
            lift.onActive = null
            swell.onActive = null
            stretch.onActive = null
            jelly.onActive = null
            glow.onActive = null
            glowSpread.onActive = null
        }
    }

    fun performTickHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun performConfirmHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!view.performHapticFeedback(HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun performEdgeHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun performLimitHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    val slideProgressState = animateFloatAsState(
        targetValue = if (hidden) 1.25f else 0f,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = if (hidden) 560f else 480f,
            visibilityThreshold = 0.002f,
        ),
        label = "dockSlide",
    )
    val isSlideVisible by remember {
        derivedStateOf { slideProgressState.value <= 0.05f }
    }
    val currentPowerSaveState = rememberUpdatedState(powerSave)
    val currentSuspendGlassBlurState = rememberUpdatedState(suspendGlassBlur)
    val isDockGlassSuspended by remember {
        derivedStateOf {
            currentSuspendGlassBlurState.value || currentPowerSaveState.value
        }
    }
    val slideSettledOffscreen by remember {
        derivedStateOf { slideProgressState.value >= 1.18f }
    }
    val isOffscreenSettled = hidden && slideSettledOffscreen
    LaunchedEffect(isOffscreenSettled) {
        currentOnOffscreenSettledChanged?.invoke(isOffscreenSettled)
    }
    DisposableEffect(Unit) {
        onDispose {
            currentOnOffscreenSettledChanged?.invoke(true)
        }
    }

    val dockAppearance = if (isLight) GlassAppearance.Light else GlassAppearance.Dark
    val opacity = readingStyle.glassOpacityLevel.coerceIn(0f, 1f)
    // 浅/深色模式均保持纯净中性磨砂底色，阻隔底层氛围光透入或染脏 Dock 胶囊与 `+` 圆球，同时保留滑动书封/卡片经过时的玻璃折射
    val dockTintOverride = remember(isLight, readingStyle.glassClarityMode, opacity) {
        if (!isLight) {
            val a = when (readingStyle.glassClarityMode) {
                GlassClarityMode.clear -> 0.06f + opacity * 0.14f
                GlassClarityMode.regular -> 0.14f + opacity * 0.20f
                GlassClarityMode.tinted -> 0.24f + opacity * 0.24f
            }
            Color(0xFF1D1E23).copy(alpha = a.coerceIn(0.06f, 0.48f))
        } else {
            val a = when (readingStyle.glassClarityMode) {
                GlassClarityMode.clear -> 0.18f + opacity * 0.18f
                GlassClarityMode.regular -> 0.28f + opacity * 0.26f
                GlassClarityMode.tinted -> 0.42f + opacity * 0.28f
            }
            Color(0xFFFAFAFC).copy(alpha = a.coerceIn(0.18f, 0.70f))
        }
    }
    val rawPillGlass = rememberIos27Glass(
        style = readingStyle,
        palette = palette,
        variant = Ios27GlassVariant.Chrome,
        isInteractive = false,
        appearanceOverride = dockAppearance,
        tintOverride = dockTintOverride,
    )
    val pillGlass = remember(rawPillGlass, isLight) {
        rawPillGlass.withCleanDockFrostedLook(isLight = isLight)
    }
    // 右侧动作圆球与主胶囊采用同一 GlassMaterial 参数，确保 Vitro glassEffectContainer 自动合并同组渲染管线
    val rawActionGlass = rememberIos27Glass(
        style = readingStyle,
        palette = palette,
        variant = Ios27GlassVariant.Chrome,
        isInteractive = true,
        appearanceOverride = dockAppearance,
        tintOverride = dockTintOverride,
    )
    val actionGlass = remember(rawActionGlass, isLight) {
        rawActionGlass.withCleanDockFrostedLook(isLight = isLight)
    }
    // 落地弹性缩放期间临时关闭 MotionStretch，避免居中的 Layout 尺寸变化触发横向果冻拉伸
    val staticActionGlass = remember(actionGlass) { actionGlass.interactive(false) }
    val pillGlassState = rememberSafeGlassEffectState(active = vitroBackdrop != null)
    val actionInteractionSource = remember { MutableInteractionSource() }
    val actionPressed by actionInteractionSource.collectIsPressedAsState()

    val pillShape = RoundedCornerShape((BAR_HEIGHT_DP * 0.5f).dp)
    val fallbackBorderColor = ios27ContrastBorderColor(palette = palette)
    val fallbackDockBg = remember(isLight, palette) {
        if (isLight) Color(0xFFECEDF3).copy(alpha = 0.92f) else Color(0xFF262832).copy(alpha = 0.86f)
    }

    // 预缓存所有绘制对象，保证 120Hz 静止与拖拽期间零 Path / Paint / Brush 分配
    val cachedShadowClipPath = remember { Path() }
    val cachedInactiveClipPath = remember { Path() }
    val cachedActiveClipPath = remember { Path() }
    val cachedInnerCorePath = remember { Path() }
    val cachedPillShadowClipPath = remember { Path() }
    val cachedOrbShadowClipPath = remember { Path() }
    val cachedShadowBlurPx = with(density) { 6.dp.toPx() }
    val cachedAmbientShadowBlurPx = with(density) { 14.dp.toPx() }
    val cachedSpotShadowBlurPx = with(density) { 5.dp.toPx() }
    val cachedRestingLensShadowBlurPx = with(density) { 3.5.dp.toPx() }
    val rimStrokeWidthPx = with(density) { 0.65.dp.toPx() }
    val progressRingStrokePx = with(density) { 2.6.dp.toPx() }
    val progressGlowStrokePx = with(density) { 5.dp.toPx() }
    val cachedLensShadowPaint = remember(cachedShadowBlurPx) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(cachedShadowBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
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
    val cachedRestingLensShadowPaint = remember(cachedRestingLensShadowBlurPx) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(cachedRestingLensShadowBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val cachedRimStroke = remember(rimStrokeWidthPx) {
        Stroke(width = rimStrokeWidthPx)
    }
    val cachedProgressArcStroke = remember(progressRingStrokePx) {
        Stroke(width = progressRingStrokePx, cap = StrokeCap.Round)
    }
    val cachedProgressGlowStroke = remember(progressGlowStrokePx) {
        Stroke(width = progressGlowStrokePx, cap = StrokeCap.Round)
    }
    val lightOpticalRimBrush = remember {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.62f),
            0.36f to Color.Black.copy(alpha = 0.045f),
            1f to Color.Black.copy(alpha = 0.075f),
        )
    }
    val darkOpticalRimBrush = remember {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.22f),
            0.38f to Color.White.copy(alpha = 0.055f),
            1f to Color.White.copy(alpha = 0.12f),
        )
    }
    val opticalRimBrush = if (isLight) lightOpticalRimBrush else darkOpticalRimBrush
    val cachedUnitGlowBrush = remember {
        Brush.radialGradient(
            0f to Color.White,
            1f to Color.Transparent,
            center = Offset.Zero,
            radius = 1f,
        )
    }

    val atRestCapsuleFill = remember(isLight, readingStyle.glassClarityMode, opacity) {
        if (isLight) {
            val a = when (readingStyle.glassClarityMode) {
                GlassClarityMode.clear -> 0.84f + opacity * 0.08f
                GlassClarityMode.regular -> 0.88f + opacity * 0.07f
                GlassClarityMode.tinted -> 0.91f + opacity * 0.05f
            }
            Color.White.copy(alpha = a.coerceIn(0.82f, 0.96f))
        } else {
            val a = when (readingStyle.glassClarityMode) {
                GlassClarityMode.clear -> 0.14f + opacity * 0.08f
                GlassClarityMode.regular -> 0.19f + opacity * 0.10f
                GlassClarityMode.tinted -> 0.25f + opacity * 0.12f
            }
            Color(0xFFF0F2FA).copy(alpha = a.coerceIn(0.13f, 0.39f))
        }
    }

    val orbRingProgressAnim = remember { Animatable(0f) }
    val orbRingAlphaAnim = remember { Animatable(0f) }
    var showImportCompletedCheck by remember { mutableStateOf(false) }
    var prevImportInProgress by remember { mutableStateOf(false) }
    val effectiveImportTarget = when {
        batchMode -> 0f
        importJustCompleted -> 1f
        importInProgress -> importProgress.coerceIn(0f, 1f).coerceAtLeast(0.04f)
        else -> 0f
    }
    LaunchedEffect(batchMode, importInProgress, importJustCompleted, effectiveImportTarget) {
        when {
            batchMode -> {
                prevImportInProgress = false
                showImportCompletedCheck = false
                orbRingAlphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                )
                orbRingProgressAnim.snapTo(0f)
            }
            importInProgress -> {
                if (!prevImportInProgress) {
                    prevImportInProgress = true
                    showImportCompletedCheck = false
                    orbRingProgressAnim.snapTo(0f)
                }
                launch {
                    orbRingAlphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                    )
                }
                orbRingProgressAnim.animateTo(
                    targetValue = maxOf(orbRingProgressAnim.value, effectiveImportTarget),
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                )
            }
            importJustCompleted -> {
                prevImportInProgress = false
                if (orbRingAlphaAnim.value < 0.99f) {
                    launch {
                        orbRingAlphaAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                        )
                    }
                }
                orbRingProgressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                )
                showImportCompletedCheck = true
            }
            else -> {
                prevImportInProgress = false
                showImportCompletedCheck = false
                if (orbRingAlphaAnim.value > 0.005f) {
                    orbRingAlphaAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    )
                }
                orbRingProgressAnim.snapTo(0f)
            }
        }
    }

    var lastAction by remember { mutableStateOf(contextualAction) }
    if (contextualAction != null) {
        lastAction = contextualAction
    }
    val orbVisible = batchMode || contextualAction != null
    val orbRevealState = animateFloatAsState(
        targetValue = if (orbVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium),
        label = "dockOrbReveal",
    )
    val hasOrbReveal by remember {
        derivedStateOf { orbRevealState.value > 0.02f }
    }
    if (!orbVisible && !hasOrbReveal && lastAction != null) {
        lastAction = null
    }
    var lastReportedOrbBounds by remember(contextualAction?.contentDescription) {
        mutableStateOf<Rect?>(null)
    }

    val isStretchCompressed by remember(stretch) {
        derivedStateOf { stretch.value > 3f }
    }
    val actionGapDpState = animateDpAsState(
        targetValue = if (actionPressed || isStretchCompressed) 6.dp else 10.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "dockActionGap",
    )

    val dockWindowWidthPx = LocalWindowInfo.current.containerSize.width.coerceAtLeast(1)
    var dockContainerWidthPx by remember { mutableIntStateOf(dockWindowWidthPx) }

    if (unmountWhenOffscreen && isOffscreenSettled) {
        return
    }
    val suppressOffscreenDraw = !unmountWhenOffscreen && isOffscreenSettled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { sz ->
                if (sz.width > 0 && dockContainerWidthPx != sz.width) {
                    dockContainerWidthPx = sz.width
                }
            }
            .offset {
                IntOffset(
                    x = 0,
                    y = ((slideProgressState.value * with(density) { 80.dp.toPx() }) + with(density) { LENS_OVERFLOW_PAD_DP.dp.toPx() }).roundToInt(),
                )
            }
            .drawWithContent {
                if (!suppressOffscreenDraw) {
                    drawContent()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val maxWidth = with(density) { dockContainerWidthPx.toDp() }
        val hasContextual = batchMode || contextualAction != null
        val targetPillWidth = if (hasContextual) {
            min(284.dp, (maxWidth - (ORB_SIZE_DP + 18f).dp).coerceAtLeast(220.dp))
        } else {
            min(336.dp, (maxWidth - 16.dp).coerceAtLeast(240.dp))
        }
        val animatedPillWidthState = animateDpAsState(
            targetValue = targetPillWidth,
            animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMedium),
            label = "dockPillWidth",
        )

        val barHeightPx = with(density) { BAR_HEIGHT_DP.dp.toPx() }
        val vPadPx = with(density) { LENS_OVERFLOW_PAD_DP.dp.toPx() }
        val padPx = with(density) { BAR_PADDING_DP.dp.toPx() }
        val lensGrowthPx = with(density) { LENS_GROWTH_DP.dp.toPx() }
        val innerHeightPx = max(1f, barHeightPx - padPx * 2f)
        val trackStartX = padPx * 2f
        val barTopPx = vPadPx
        val barBottomPx = vPadPx + barHeightPx

        Row(
            modifier = if (vitroBackdrop != null) {
                Modifier.glassEffectContainer(spacing = 10.dp)
            } else {
                Modifier
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // =====================================================================
            // 1. 底部主 4-Tab 液态玻璃胶囊（严禁对 .glassEffect 节点施加 graphicsLayer 缩放/平移）
            // =====================================================================
            Box(
                modifier = Modifier.layout { measurable, _ ->
                    val w = animatedPillWidthState.value.roundToPx().coerceAtLeast(1)
                    val h = TOTAL_CONTAINER_HEIGHT_DP.dp.roundToPx().coerceAtLeast(1)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) {
                        placeable.placeRelative(stretch.value.roundToInt(), 0)
                    }
                },
            ) {
                val computeLensRectPx = { containerWidthPx: Float ->
                    val safeW = max(1f, containerWidthPx)
                    val innerSpan = max(1f, safeW - padPx * 4f)
                    val pitchPx = innerSpan / max(1, destinations.size)
                    val lensBaseWidthPx = pitchPx + padPx * 2f
                    val curLifted = lift.value.coerceIn(0f, 1.08f)
                    val curGrowth = lensGrowthPx * 2f * curLifted
                    val curW = (lensBaseWidthPx + curGrowth) * (1f + jelly.value)
                    val jellyVert = 1f - max(0f, jelly.value) * 0.24f - min(0f, jelly.value) * 0.85f
                    val curH = (innerHeightPx + curGrowth) * jellyVert
                    val curLeft = trackStartX + (lens.value + 0.5f) * pitchPx - curW / 2f
                    val curTop = vPadPx + padPx + (innerHeightPx - curH) / 2f
                    Rect(
                        left = curLeft,
                        top = curTop,
                        right = curLeft + curW,
                        bottom = curTop + curH,
                    )
                }
                val isLensSettledAtRestNow = {
                    lift.value.coerceIn(0f, 1.08f) <= 0.005f && abs(lens.value - safeSelected) <= 0.01f
                }

                // A. Vitro 0.1.0 实时玻璃底基座（swell 仅触发 Measure/Layout，不在组合阶段读取动画值）
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .layout { measurable, _ ->
                            val swPx = (swell.value * 3.0f.dp.toPx()).roundToInt()
                            val shPx = (swell.value * 2.2f.dp.toPx()).roundToInt()
                            val w = (animatedPillWidthState.value.roundToPx() + swPx).coerceAtLeast(1)
                            val h = (BAR_HEIGHT_DP.dp.roundToPx() + shPx).coerceAtLeast(1)
                            val placeable = measurable.measure(Constraints.fixed(w, h))
                            layout(w, h) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .drawBehind {
                            if (vitroBackdrop != null && isDockGlassSuspended && size.width > 1f && size.height > 1f) {
                                val r = size.height * 0.5f
                                drawRoundRect(
                                    color = fallbackDockBg,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                                )
                                drawRoundRect(
                                    color = fallbackBorderColor,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                                    style = Stroke(width = 0.5.dp.toPx()),
                                )
                            }
                            val shadowSlideVis = (1f - (slideProgressState.value / 0.25f)).coerceIn(0f, 1f)
                            if (isLight &&
                                !powerSave &&
                                !isDockGlassSuspended &&
                                shadowSlideVis > 0.02f &&
                                size.width > 1f &&
                                size.height > 1f &&
                                drawContext.canvas.nativeCanvas.isHardwareAccelerated
                            ) {
                                val r = size.height * 0.5f
                                val swellBoost = swell.value.coerceIn(0f, 1f)
                                cachedPillShadowClipPath.reset()
                                cachedPillShadowClipPath.addRoundRect(
                                    RoundRect(0f, 0f, size.width, size.height, r, r),
                                )
                                clipPath(cachedPillShadowClipPath, clipOp = ClipOp.Difference) {
                                    drawIntoCanvas { canvas ->
                                        val ambientAlpha = (0.065f + 0.025f * swellBoost) * shadowSlideVis
                                        cachedDockAmbientShadowPaint.color = android.graphics.Color.argb(
                                            (ambientAlpha * 255).toInt().coerceIn(0, 255),
                                            0,
                                            0,
                                            0,
                                        )
                                        val ambientDy = 4.dp.toPx() + 2.dp.toPx() * swellBoost
                                        canvas.nativeCanvas.drawRoundRect(
                                            -1.dp.toPx(),
                                            ambientDy,
                                            size.width + 1.dp.toPx(),
                                            size.height + ambientDy,
                                            r,
                                            r,
                                            cachedDockAmbientShadowPaint,
                                        )

                                        val spotAlpha = (0.045f + 0.02f * swellBoost) * shadowSlideVis
                                        cachedDockSpotShadowPaint.color = android.graphics.Color.argb(
                                            (spotAlpha * 255).toInt().coerceIn(0, 255),
                                            0,
                                            0,
                                            0,
                                        )
                                        val spotDy = 1.5.dp.toPx() + 0.5.dp.toPx() * swellBoost
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
                        .then(
                            if (vitroBackdrop != null) {
                                // Keep pillGlass bound continuously so GlassContainerNode never tears down
                                // and reallocates the dock GlassGroup / ShaderRenderer across modal/reader exits.
                                Modifier.glassEffect(
                                    backdrop = vitroBackdrop,
                                    glass = if (isDockGlassSuspended) Glass.Identity else pillGlass,
                                    shape = pillShape,
                                    state = pillGlassState,
                                )
                            } else {
                                Modifier
                                    .clip(pillShape)
                                    .background(fallbackDockBg)
                                    .border(width = 0.5.dp, color = fallbackBorderColor, shape = pillShape)
                            },
                        ),
                ) {
                    if (vitroBackdrop != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (size.width > 2f && size.height > 2f) {
                                val inset = rimStrokeWidthPx * 0.5f
                                val r = (size.height - rimStrokeWidthPx).coerceAtLeast(1f) * 0.5f
                                drawRoundRect(
                                    brush = opticalRimBrush,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - rimStrokeWidthPx, size.height - rimStrokeWidthPx),
                                    cornerRadius = CornerRadius(r, r),
                                    style = cachedRimStroke,
                                )
                            }
                        }
                    }
                }

                // B. 滑动选区透镜壳体层（所有动画状态推迟至 Draw 阶段读取，零组合重建）
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val batchFade = (1f - batchProgressState.value.coerceIn(0f, 1f))
                    if (batchFade <= 0.005f) return@Canvas
                    val lifted = lift.value.coerceIn(0f, 1.08f)
                    val lensRectPx = computeLensRectPx(size.width)
                    val platterAlphaFactor = (1f - lifted * 0.78f).coerceIn(0.14f, 1f) * batchFade
                    val glowReachPx = GLOW_REACH_DP.dp.toPx() * (1f + GLOW_GROWTH * glowSpread.value)
                    val glowStrength = GLOW_PEAK * glow.value * glow.value * batchFade
                    val lensR = lensRectPx.height / 2f
                    val lensCorner = CornerRadius(lensR, lensR)

                    if (!isDockGlassSuspended &&
                        !powerSave &&
                        lensRectPx.width > 1f &&
                        lensRectPx.height > 1f &&
                        drawContext.canvas.nativeCanvas.isHardwareAccelerated
                    ) {
                        if (isLight && !isDockGlassSuspended && platterAlphaFactor > 0.01f) {
                            cachedShadowClipPath.reset()
                            cachedShadowClipPath.addRoundRect(RoundRect(lensRectPx, lensCorner))
                            clipPath(cachedShadowClipPath, clipOp = ClipOp.Difference) {
                                drawIntoCanvas { canvas ->
                                    val restAlpha = 0.055f * platterAlphaFactor
                                    cachedRestingLensShadowPaint.color = android.graphics.Color.argb(
                                        (restAlpha * 255).toInt().coerceIn(0, 255),
                                        0,
                                        0,
                                        0,
                                    )
                                    val restDy = 1.dp.toPx()
                                    canvas.nativeCanvas.drawRoundRect(
                                        lensRectPx.left,
                                        lensRectPx.top + restDy,
                                        lensRectPx.right,
                                        lensRectPx.bottom + restDy,
                                        lensR,
                                        lensR,
                                        cachedRestingLensShadowPaint,
                                    )
                                }
                            }
                        }
                        if (lifted > 0.01f) {
                            cachedShadowClipPath.reset()
                            cachedShadowClipPath.addRoundRect(RoundRect(lensRectPx, lensCorner))
                            clipPath(cachedShadowClipPath, clipOp = ClipOp.Difference) {
                                drawIntoCanvas { canvas ->
                                    val lensShadowAlpha = (if (isLight) 0.08f else 0.24f) * lifted * batchFade
                                    cachedLensShadowPaint.color = android.graphics.Color.argb(
                                        (lensShadowAlpha * 255).toInt().coerceIn(0, 255),
                                        0,
                                        0,
                                        0,
                                    )
                                    val dy = 6.dp.toPx() * lifted
                                    canvas.nativeCanvas.drawRoundRect(
                                        lensRectPx.left,
                                        lensRectPx.top + dy,
                                        lensRectPx.right,
                                        lensRectPx.bottom + dy,
                                        lensR,
                                        lensR,
                                        cachedLensShadowPaint,
                                    )
                                }
                            }
                        }
                    }

                    // 静止选区：无框线柔和磨砂/熏色玻璃胶囊，保留纯正连续液态质感
                    if (lensRectPx.width > 1f && lensRectPx.height > 1f) {
                        drawRoundRect(
                            color = atRestCapsuleFill.copy(alpha = atRestCapsuleFill.alpha * platterAlphaFactor),
                            topLeft = lensRectPx.topLeft,
                            size = lensRectPx.size,
                            cornerRadius = lensCorner,
                        )
                    }

                    // 抬升态柔和折射体与指尖光晕（移除拖拽透镜外框线与横截线）
                    if (lifted > 0.01f && lensRectPx.width > 1f && lensRectPx.height > 1f) {
                        cachedShadowClipPath.reset()
                        cachedShadowClipPath.addRoundRect(RoundRect(lensRectPx, lensCorner))
                        clipPath(cachedShadowClipPath) {
                            drawRoundRect(
                                color = if (isLight) {
                                    Color.White.copy(alpha = 0.20f * lifted * batchFade)
                                } else {
                                    Color(0xFF3E404C).copy(alpha = 0.26f * lifted * batchFade)
                                },
                                topLeft = lensRectPx.topLeft,
                                size = lensRectPx.size,
                                cornerRadius = lensCorner,
                            )
                            if (glowStrength > 0.002f && glowReachPx > 1f) {
                                withTransform({
                                    translate(left = glowAt.x, top = glowAt.y)
                                    scale(scaleX = glowReachPx, scaleY = glowReachPx, pivot = Offset.Zero)
                                }) {
                                    drawCircle(
                                        brush = cachedUnitGlowBrush,
                                        radius = 1f,
                                        center = Offset.Zero,
                                        alpha = glowStrength.coerceIn(0f, 1f),
                                    )
                                }
                            }
                        }
                    }
                }

                // C. 图标与手势跟手 Scrub 层 / 书架批量选择操作切换层
                val activeTint = if (isLight) palette.accentText else lerpColor(palette.primarySoft, Color.White, 0.20f)
                val inactiveColor = if (isLight) Color(0xFF44424A) else Color(0xFFE6E8F2)
                val dangerTint = if (isLight) Color(0xFFDC3545) else Color(0xFFFF6B76)

                AnimatedContent(
                    targetState = batchMode,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        (fadeIn(tween(durationMillis = 200)) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow),
                        )) togetherWith (fadeOut(tween(durationMillis = 150)) + scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(durationMillis = 150),
                        ))
                    },
                    label = "dockBatchContentMorph",
                ) { isBatch ->
                    if (isBatch) {
                        val batchInteractive = !hidden && isSlideVisible
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = (BAR_PADDING_DP * 2f).dp,
                                    vertical = (LENS_OVERFLOW_PAD_DP + BAR_PADDING_DP).dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DockBatchActionSlot(
                                icon = if (batchAllSelected) Icons.Rounded.CheckCircle else Icons.Rounded.SelectAll,
                                label = if (batchAllSelected) "取消全选" else "全选",
                                tint = if (batchAllSelected) activeTint else inactiveColor,
                                enabled = batchInteractive,
                                isHighlighted = batchAllSelected,
                                highlightTint = activeTint,
                                onClick = {
                                    performTickHaptic()
                                    currentBatchSelectAll()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                            DockBatchActionSlot(
                                icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                                label = "移至书架",
                                tint = inactiveColor,
                                enabled = batchInteractive && batchHasSelection,
                                onClick = {
                                    performConfirmHaptic()
                                    currentBatchMoveToShelf()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                            DockBatchActionSlot(
                                icon = Icons.Rounded.AutoAwesomeMotion,
                                label = "合并系列",
                                tint = inactiveColor,
                                enabled = batchInteractive && batchHasSelection,
                                onClick = {
                                    performConfirmHaptic()
                                    currentBatchMergeSeries()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                            DockBatchActionSlot(
                                icon = Icons.Rounded.DeleteOutline,
                                label = "删除",
                                tint = dangerTint,
                                enabled = batchInteractive && batchHasSelection,
                                mutedAlpha = 0.34f,
                                onClick = {
                                    performConfirmHaptic()
                                    currentBatchDelete()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(destinations.size, hidden, batchMode) {
                                    if (hidden || currentBatchMode) return@pointerInput
                            val bottomGestureGuardPx = 5.dp.toPx()
                            val upSwipeAbortThresholdPx = 18.dp.toPx()
                            val horizontalDragSlopPx = viewConfiguration.touchSlop * 0.55f
                            val edgeThresholdPx = 3.dp.toPx()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = true)
                                if (slideProgressState.value > 0.05f) {
                                    return@awaitEachGesture
                                }

                                val liveBarWidthPx = max(1f, size.width.toFloat())
                                val liveInnerSpan = max(1f, liveBarWidthPx - padPx * 4f)
                                val livePitchPx = liveInnerSpan / max(1, destinations.size)
                                val liveTrackStartX = padPx * 2f

                                fun livePositionAt(xPx: Float): Float {
                                    val pos = (xPx - liveTrackStartX) / livePitchPx - 0.5f
                                    if (pos < 0f) return rubberBand(pos, OVERDRAG)
                                    if (pos > lastIndex) return lastIndex + rubberBand(pos - lastIndex, OVERDRAG)
                                    return pos
                                }

                                fun liveIndexAt(pos: Float): Int = pos.roundToInt().coerceIn(0, lastIndex)

                                val startX = down.position.x
                                val startY = down.position.y

                                if (startX < 0f ||
                                    startX > liveBarWidthPx ||
                                    startY < barTopPx ||
                                    startY > barBottomPx - bottomGestureGuardPx
                                ) {
                                    return@awaitEachGesture
                                }

                                val tracker = VelocityTracker()
                                tracker.addPosition(down.uptimeMillis, down.position)
                                var dragging = false
                                var abortedByVerticalGesture = false
                                var lastX = startX
                                var wasOverdragging = false
                                var wasLimitReached = false

                                val initialIdx = liveIndexAt(livePositionAt(startX))
                                pressedIndex = initialIdx
                                dropping = false
                                swell.springTo(1f, pressSpring)
                                glowAt = down.position
                                glowSpread.jumpTo(0f)
                                glow.springTo(1f, glowInSpring)
                                lift.springTo(1f, liftUpSpring)
                                lens.springTo(initialIdx.toFloat(), travelSpring)

                                var pointerId = down.id
                                var committed = true
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                                    if (change == null) {
                                        committed = false
                                        break
                                    }
                                    if (change.isConsumed && !dragging) {
                                        abortedByVerticalGesture = true
                                        committed = false
                                        break
                                    }
                                    val dx = change.position.x
                                    val dy = change.position.y
                                    lastX = dx
                                    val rawDx = dx - startX
                                    val rawDy = dy - startY

                                    tracker.addPosition(change.uptimeMillis, change.position)

                                    if (!dragging) {
                                        if (abs(rawDx) > horizontalDragSlopPx && abs(rawDx) >= abs(rawDy) * 0.75f) {
                                            dragging = true
                                        } else if (rawDy < -upSwipeAbortThresholdPx && abs(rawDy) > abs(rawDx) * 1.6f) {
                                            abortedByVerticalGesture = true
                                            committed = false
                                            break
                                        }
                                    }

                                    if (!change.pressed) {
                                        committed = true
                                        break
                                    }
                                    pointerId = change.id
                                    glowAt = change.position
                                    if (dragging) {
                                        val overshoot = dx - dx.coerceIn(0f, liveBarWidthPx)
                                        val isOverdrag = abs(overshoot) > edgeThresholdPx
                                        if (isOverdrag && !wasOverdragging) {
                                            wasOverdragging = true
                                            performEdgeHaptic()
                                        } else if (!isOverdrag && wasOverdragging) {
                                            wasOverdragging = false
                                        }

                                        val maxPullPx = barHeightPx * PULL_LIMIT
                                        stretch.springTo(
                                            rubberBand(overshoot, maxPullPx),
                                            trackSpring,
                                        )

                                        val isAtLimit = abs(stretch.value) >= maxPullPx * 0.90f
                                        if (isAtLimit && !wasLimitReached) {
                                            wasLimitReached = true
                                            performLimitHaptic()
                                        } else if (!isAtLimit && wasLimitReached) {
                                            wasLimitReached = false
                                        }

                                        val pos = livePositionAt(dx)
                                        lens.springTo(pos, followSpring)
                                        val idx = liveIndexAt(pos)
                                        if (idx != pressedIndex) {
                                            performTickHaptic()
                                            pressedIndex = idx
                                        }
                                        change.consume()
                                    }
                                }

                                if (abortedByVerticalGesture) {
                                    pressedIndex = null
                                    dropping = true
                                    lens.springTo(currentSelected.toFloat(), travelSpring)
                                    lift.springTo(0f, liftDownSpring)
                                    glow.springTo(0f, glowOutSpring)
                                    glowSpread.springTo(1f, glowOutSpring)
                                    stretch.springTo(0f, pressSpring)
                                    swell.springTo(0f, pressSpring)
                                    return@awaitEachGesture
                                }

                                val currentPressed = pressedIndex
                                var releaseVelocityOverride: Float? = null
                                if (dragging && currentPressed != null && committed) {
                                    val vx = tracker.calculateVelocity().x
                                    val velSlots = (vx / livePitchPx).coerceIn(-12f, 12f)
                                    val endPos = livePositionAt(lastX)
                                    val deltaFromStart = endPos - initialIdx.toFloat()
                                    val lead = (vx / livePitchPx * 0.16f).coerceIn(-1.25f, 1.25f)
                                    var flingIdx = (endPos + lead).roundToInt().coerceIn(0, lastIndex)

                                    if (flingIdx == initialIdx) {
                                        if (abs(deltaFromStart) >= 0.22f && velSlots * deltaFromStart >= -0.4f) {
                                            flingIdx = (initialIdx + sign(deltaFromStart).toInt()).coerceIn(0, lastIndex)
                                        } else if (abs(velSlots) >= 1.35f && velSlots * deltaFromStart >= -0.05f) {
                                            flingIdx = (initialIdx + sign(velSlots).toInt()).coerceIn(0, lastIndex)
                                        }
                                    }

                                    if (flingIdx != currentPressed) {
                                        performTickHaptic()
                                        pressedIndex = flingIdx
                                    }
                                    releaseVelocityOverride = (velSlots * 0.65f + lens.velocity * 0.35f).coerceIn(-10f, 10f)
                                }

                                val finalIdx = pressedIndex
                                pressedIndex = null
                                dropping = true
                                val settleIdx = if (committed && finalIdx != null) finalIdx else currentSelected
                                lens.springTo(settleIdx.toFloat(), travelSpring, velocityOverride = releaseVelocityOverride)
                                if (lift.value >= LIFT_SNAP && abs(lens.target - lens.value) <= LIFT_RELEASE) {
                                    dropping = false
                                    lift.springTo(0f, liftDownSpring)
                                }
                                glow.springTo(0f, glowOutSpring)
                                glowSpread.springTo(1f, glowOutSpring)
                                stretch.springTo(0f, pressSpring)
                                swell.springTo(0f, pressSpring)

                                if (committed && finalIdx != null) {
                                    if (finalIdx != currentSelected) {
                                        performConfirmHaptic()
                                    } else {
                                        performTickHaptic()
                                    }
                                    currentOnSelected(finalIdx)
                                }
                            }
                        },
                ) {
                    // 外部未选中图标文字层（静止时跳过 clipPath 离屏裁切，保留语义节点 count = 2）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                if (isLensSettledAtRestNow()) {
                                    this@drawWithContent.drawContent()
                                } else {
                                    val lensRectPx = computeLensRectPx(size.width)
                                    val r = lensRectPx.height / 2f
                                    cachedInactiveClipPath.reset()
                                    cachedInactiveClipPath.addRoundRect(RoundRect(lensRectPx, CornerRadius(r, r)))
                                    clipPath(cachedInactiveClipPath, clipOp = ClipOp.Difference) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            }
                            .padding(
                                horizontal = (BAR_PADDING_DP * 2f).dp,
                                vertical = (LENS_OVERFLOW_PAD_DP + BAR_PADDING_DP).dp,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            destinations.forEachIndexed { idx, dest ->
                                val isSelected = idx == safeSelected
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .drawWithContent {
                                            if (!(isLensSettledAtRestNow() && idx == safeSelected)) {
                                                this@drawWithContent.drawContent()
                                            }
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) dest.filled else dest.outlined,
                                        contentDescription = dest.label,
                                        tint = inactiveColor,
                                        modifier = Modifier.size(23.dp),
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dest.label,
                                        color = inactiveColor,
                                        fontSize = 10.5.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    // 内部聚焦选中层：静止时跳过 clipPath，抬升/滑动时启用微放大纯净折射
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                if (isLensSettledAtRestNow()) {
                                    this@drawWithContent.drawContent()
                                } else {
                                    val lensRectPx = computeLensRectPx(size.width)
                                    val r = lensRectPx.height / 2f
                                    cachedActiveClipPath.reset()
                                    cachedActiveClipPath.addRoundRect(RoundRect(lensRectPx, CornerRadius(r, r)))
                                    clipPath(cachedActiveClipPath, clipOp = ClipOp.Intersect) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (!powerSave &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        lensContentShader != null
                                    ) {
                                        Modifier.graphicsLayer {
                                            val lifted = lift.value.coerceIn(0f, 1.08f)
                                            val lensRectPx = computeLensRectPx(size.width)
                                            if (lifted > 0.005f &&
                                                lensRectPx.width > 1f &&
                                                lensRectPx.height > 1f
                                            ) {
                                                lensContentShader.setFloatUniform("uLensOrigin", lensRectPx.left, lensRectPx.top)
                                                lensContentShader.setFloatUniform("uLensSize", lensRectPx.width, lensRectPx.height)
                                                lensContentShader.setFloatUniform("uLift", lifted)
                                                lensContentShader.setFloatUniform("uIsDark", if (isLight) 0f else 1f)
                                                lensContentShader.setFloatUniform("uDensity", this.density)
                                                renderEffect = AndroidRenderEffect.createRuntimeShaderEffect(
                                                    lensContentShader,
                                                    "uTexture",
                                                ).asComposeRenderEffect()
                                            } else {
                                                renderEffect = null
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(
                                    horizontal = (BAR_PADDING_DP * 2f).dp,
                                    vertical = (LENS_OVERFLOW_PAD_DP + BAR_PADDING_DP).dp,
                                ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                destinations.forEachIndexed { idx, dest ->
                                    val isSelected = idx == safeSelected
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .drawWithContent {
                                                if (!isLensSettledAtRestNow() || idx == safeSelected) {
                                                    this@drawWithContent.drawContent()
                                                }
                                            }
                                            .graphicsLayer {
                                                val magnifyScale = 1f + LENS_MAGNIFY * lift.value.coerceIn(0f, 1.08f)
                                                scaleX = magnifyScale
                                                scaleY = magnifyScale
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) dest.filled else dest.outlined,
                                            contentDescription = dest.label,
                                            tint = activeTint,
                                            modifier = Modifier.size(23.dp),
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dest.label,
                                            color = activeTint,
                                            fontSize = 10.5.sp,
                                            lineHeight = 12.sp,
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
                    }
                }
            }

            // =====================================================================
            // 2. 右侧上下文独立动作圆球（与主胶囊同组，支持常态 contextualAction 与批量选择 `×` 无缝变身）
            // =====================================================================
            val activeAction = contextualAction ?: if (hasOrbReveal) lastAction else null
            if (batchMode || activeAction != null || hasOrbReveal) {
                val actionGlassVisible by remember {
                    derivedStateOf { orbRevealState.value.coerceIn(0f, 1f) * ORB_SIZE_DP >= 4f }
                }
                val actionGlassAttached = if (unmountWhenOffscreen) {
                    !hidden && actionGlassVisible
                } else {
                    actionGlassVisible
                }
                val showOrbIcon by remember {
                    derivedStateOf { orbRevealState.value.coerceIn(0f, 1f) * ORB_SIZE_DP >= 24f }
                }
                val actionGlassState = rememberSafeGlassEffectState(
                    interactionSource = actionInteractionSource,
                    active = actionGlassAttached,
                )
                val orbCoroutineScope = rememberCoroutineScope()
                val orbDragX = remember { Animatable(0f) }
                val orbDragY = remember { Animatable(0f) }
                val latestActionOnClick by rememberUpdatedState(activeAction?.onClick)
                val latestOrbVisibilityProvider by rememberUpdatedState(activeAction?.orbVisibilityProvider)
                val orbHiddenFlag = !batchMode && (activeAction?.hideOrb == true)
                // Effective orb presence = reveal fade * popover handoff visibility. While the `+` popover is
                // presenting (hideOrb / orbVisibilityProvider -> 0f), the orb's Vitro glassEffect node must be
                // fully DETACHED so its pane (with the stale rasterized `+` icon) is dropped from the shared
                // rootBackdrop and can never be sampled/blurred into the opened popover's corner. On close, the
                // popover ramps orbVisibility 0f -> 1f across collapse 0.12f -> 0.01f, so the glass re-attaches at
                // <= 2% alpha (invisible) and the tail stays seamless.
                val effectiveOrbPresenceState = remember(batchMode, orbHiddenFlag) {
                    derivedStateOf {
                        val clamped = orbRevealState.value.coerceIn(0f, 1f)
                        val revealAlpha = ((clamped - 0.08f) / 0.35f).coerceIn(0f, 1f)
                        val presence = if (batchMode) {
                            1f
                        } else {
                            (latestOrbVisibilityProvider?.invoke() ?: if (orbHiddenFlag) 0f else 1f).coerceIn(0f, 1f)
                        }
                        (revealAlpha * presence).coerceIn(0f, 1f)
                    }
                }
                val orbGlassHidden by remember(effectiveOrbPresenceState) {
                    derivedStateOf { effectiveOrbPresenceState.value <= 0.01f }
                }
                val orbSlotActive = !hidden && isSlideVisible && orbVisible &&
                    (batchMode || activeAction != null)
                val actionEnabled = orbSlotActive &&
                    (batchMode || activeAction?.hideOrb != true)

                val orbLandScale = remember { Animatable(1f) }
                val isOrbLandBouncing by remember {
                    derivedStateOf { kotlin.math.abs(orbLandScale.value - 1f) > 0.002f }
                }
                val popoverOrbHidden by remember(batchMode, orbHiddenFlag) {
                    derivedStateOf {
                        !batchMode &&
                            (latestOrbVisibilityProvider?.invoke() ?: if (orbHiddenFlag) 0f else 1f) < 0.15f
                    }
                }
                val popoverOrbLanded = orbSlotActive && !popoverOrbHidden
                var popoverOrbArmed by remember { mutableStateOf(false) }

                LaunchedEffect(popoverOrbLanded, orbSlotActive) {
                    if (!orbSlotActive) {
                        popoverOrbArmed = false
                        orbLandScale.snapTo(1f)
                    } else if (popoverOrbLanded) {
                        if (popoverOrbArmed) {
                            orbLandScale.snapTo(0.86f)
                            orbLandScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = 0.36f, stiffness = 460f),
                                initialVelocity = 1.6f,
                            )
                        }
                        popoverOrbArmed = false
                    } else {
                        popoverOrbArmed = true
                    }
                }

                LaunchedEffect(actionEnabled) {
                    if (!actionEnabled) {
                        orbDragX.snapTo(0f)
                        orbDragY.snapTo(0f)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(
                        modifier = Modifier.layout { measurable, _ ->
                            val clamped = orbRevealState.value.coerceIn(0f, 1f)
                            val gapPx = (actionGapDpState.value.toPx() * clamped).roundToInt().coerceAtLeast(0)
                            val placeable = measurable.measure(Constraints.fixed(gapPx, 0))
                            layout(gapPx, 0) {
                                placeable.placeRelative(0, 0)
                            }
                        },
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .layout { measurable, _ ->
                                val clamped = orbRevealState.value.coerceIn(0f, 1f)
                                val orbPx = (ORB_SIZE_DP.dp.toPx() * clamped).roundToInt().coerceAtLeast(0)
                                val placeable = measurable.measure(Constraints.fixed(orbPx, orbPx))
                                layout(orbPx, orbPx) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .onGloballyPositioned { coords ->
                                if (!currentBatchMode && orbVisible && orbRevealState.value > 0.85f) {
                                    val rect = coords.boundsInRoot()
                                    if (rect != lastReportedOrbBounds) {
                                        lastReportedOrbBounds = rect
                                        activeAction?.onAnchorBoundsChanged?.invoke(rect)
                                    }
                                }
                            }
                            .offset {
                                IntOffset(orbDragX.value.roundToInt(), orbDragY.value.roundToInt())
                            }
                            .pointerInput(actionEnabled, actionInteractionSource) {
                                if (!actionEnabled) return@pointerInput
                                val maxPullPx = 14.dp.toPx()
                                val commitRadiusPx = 42.dp.toPx()
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pointerId = down.id
                                    val startScreenX = down.position.x + orbDragX.value
                                    val startScreenY = down.position.y + orbDragY.value
                                    val press = PressInteraction.Press(down.position)
                                    actionInteractionSource.tryEmit(press)
                                    var totalDx = 0f
                                    var totalDy = 0f

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val active = event.changes.firstOrNull { it.id == pointerId }
                                            ?: event.changes.firstOrNull()
                                        if (active == null || !active.pressed) {
                                            active?.consume()
                                            val lifted = active != null
                                            actionInteractionSource.tryEmit(
                                                if (lifted) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                                            )
                                            orbCoroutineScope.launch {
                                                orbDragX.animateTo(0f, spring(dampingRatio = 0.56f, stiffness = 460f))
                                            }
                                            orbCoroutineScope.launch {
                                                orbDragY.animateTo(0f, spring(dampingRatio = 0.56f, stiffness = 460f))
                                            }
                                            if (lifted && hypot(totalDx, totalDy) <= commitRadiusPx) {
                                                performConfirmHaptic()
                                                if (currentBatchMode) {
                                                    currentBatchExit()
                                                } else {
                                                    latestActionOnClick?.invoke()
                                                }
                                            }
                                            break
                                        }
                                        active.consume()
                                        val curScreenX = active.position.x + orbDragX.value
                                        val curScreenY = active.position.y + orbDragY.value
                                        totalDx = curScreenX - startScreenX
                                        totalDy = curScreenY - startScreenY
                                        val pullX = maxPullPx * tanh(totalDx * 0.34f / maxPullPx.coerceAtLeast(1f))
                                        val pullY = maxPullPx * tanh(totalDy * 0.34f / maxPullPx.coerceAtLeast(1f))
                                        orbCoroutineScope.launch {
                                            orbDragX.snapTo(pullX)
                                            orbDragY.snapTo(pullY)
                                        }
                                    }
                                }
                            },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                // 通过 Layout 尺寸居中驱动玻璃圆球与内层光学描边同步回弹，严禁对 .glassEffect 节点施加 graphicsLayer 缩放，
                                // 否则 glassEffectContainer 下 Backdrop 坐标偏移但 measured 未缩，导致外圈玻璃边框位移而内部 `+` 仅原地缩放。
                                .layout { measurable, constraints ->
                                    val basePx = minOf(constraints.maxWidth, constraints.maxHeight).coerceAtLeast(0)
                                    val landScale = orbLandScale.value.coerceIn(0.6f, 1.35f)
                                    val scaledPx = (basePx * landScale).roundToInt().coerceAtLeast(if (basePx > 0) 1 else 0)
                                    val placeable = measurable.measure(Constraints.fixed(scaledPx, scaledPx))
                                    layout(basePx, basePx) {
                                        placeable.placeRelative(
                                            ((basePx - scaledPx) * 0.5f).roundToInt(),
                                            ((basePx - scaledPx) * 0.5f).roundToInt(),
                                        )
                                    }
                                }
                                .graphicsLayer {
                                    // Keep alpha = 1f when live Vitro orb is hidden so GlassNode.draw() executes
                                    // arrange() -> clear(pane) via Glass.Identity instead of skipping draw() and
                                    // leaving a frozen orb/plus pixel ghost in the backdrop.
                                    alpha = if (orbGlassHidden && vitroBackdrop != null) 1f else effectiveOrbPresenceState.value
                                }
                                .drawBehind {
                                    if (vitroBackdrop != null &&
                                        isDockGlassSuspended &&
                                        actionGlassVisible &&
                                        !orbGlassHidden &&
                                        effectiveOrbPresenceState.value > 0.02f &&
                                        size.width > 1f &&
                                        size.height > 1f
                                    ) {
                                        val radius = size.minDimension * 0.5f
                                        drawCircle(
                                            color = fallbackDockBg,
                                            radius = radius,
                                        )
                                        drawCircle(
                                            color = fallbackBorderColor,
                                            radius = radius,
                                            style = Stroke(width = 0.5.dp.toPx()),
                                        )
                                    }
                                    val shadowSlideVis = (1f - (slideProgressState.value / 0.25f)).coerceIn(0f, 1f)
                                    if (isLight &&
                                        !powerSave &&
                                        !isDockGlassSuspended &&
                                        shadowSlideVis > 0.02f &&
                                        actionGlassVisible &&
                                        !orbGlassHidden &&
                                        effectiveOrbPresenceState.value > 0.02f &&
                                        size.width > 1f &&
                                        size.height > 1f &&
                                        drawContext.canvas.nativeCanvas.isHardwareAccelerated
                                    ) {
                                        val radius = size.minDimension * 0.5f
                                        val cx = size.width * 0.5f
                                        val cy = size.height * 0.5f
                                        cachedOrbShadowClipPath.reset()
                                        cachedOrbShadowClipPath.addRoundRect(
                                            RoundRect(
                                                cx - radius,
                                                cy - radius,
                                                cx + radius,
                                                cy + radius,
                                                radius,
                                                radius,
                                            ),
                                        )
                                        clipPath(cachedOrbShadowClipPath, clipOp = ClipOp.Difference) {
                                            drawIntoCanvas { canvas ->
                                                val pressBoost = if (actionPressed) 1f else 0f
                                                val ambientAlpha = (0.065f + 0.025f * pressBoost) * shadowSlideVis
                                                cachedDockAmbientShadowPaint.color = android.graphics.Color.argb(
                                                    (ambientAlpha * 255).toInt().coerceIn(0, 255),
                                                    0,
                                                    0,
                                                    0,
                                                )
                                                val ambientDy = 4.dp.toPx() + 1.5.dp.toPx() * pressBoost
                                                canvas.nativeCanvas.drawCircle(
                                                    cx,
                                                    cy + ambientDy,
                                                    radius + 0.5.dp.toPx(),
                                                    cachedDockAmbientShadowPaint,
                                                )

                                                val spotAlpha = (0.045f + 0.02f * pressBoost) * shadowSlideVis
                                                cachedDockSpotShadowPaint.color = android.graphics.Color.argb(
                                                    (spotAlpha * 255).toInt().coerceIn(0, 255),
                                                    0,
                                                    0,
                                                    0,
                                                )
                                                val spotDy = 1.5.dp.toPx() + 0.5.dp.toPx() * pressBoost
                                                canvas.nativeCanvas.drawCircle(
                                                    cx,
                                                    cy + spotDy,
                                                    radius,
                                                    cachedDockSpotShadowPaint,
                                                )
                                            }
                                        }
                                    }
                                }
                                .then(
                                    if (actionGlassAttached) {
                                        if (vitroBackdrop != null) {
                                            Modifier.glassEffect(
                                                backdrop = vitroBackdrop,
                                                glass = when {
                                                    orbGlassHidden || isDockGlassSuspended -> Glass.Identity
                                                    isOrbLandBouncing -> staticActionGlass
                                                    else -> actionGlass
                                                },
                                                shape = CircleShape,
                                                state = actionGlassState,
                                                interactionSource = actionInteractionSource,
                                            )
                                        } else {
                                            Modifier
                                                .clip(CircleShape)
                                                .background(fallbackDockBg)
                                                .border(width = 0.5.dp, color = fallbackBorderColor, shape = CircleShape)
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .vitroFingerSpotlight(
                                    shape = CircleShape,
                                    accentColor = palette.primary,
                                    isDarkSurface = !isLight,
                                    enabled = !orbGlassHidden && actionEnabled,
                                    maxRadiusDp = 56.dp,
                                    intensity = 1.15f,
                                ),
                        ) {
                            if ((actionGlassAttached && !orbGlassHidden && vitroBackdrop != null) ||
                                (!batchMode && orbRingAlphaAnim.value > 0.005f)
                            ) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    if (actionGlassAttached &&
                                        !orbGlassHidden &&
                                        vitroBackdrop != null &&
                                        effectiveOrbPresenceState.value > 0.02f &&
                                        size.minDimension > 2f
                                    ) {
                                        val inset = rimStrokeWidthPx * 0.5f
                                        val diameter = (size.minDimension - rimStrokeWidthPx).coerceAtLeast(1f)
                                        drawArc(
                                            brush = opticalRimBrush,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = Offset(
                                                (size.width - size.minDimension) * 0.5f + inset,
                                                (size.height - size.minDimension) * 0.5f + inset,
                                            ),
                                            size = Size(diameter, diameter),
                                            style = cachedRimStroke,
                                        )
                                    }
                                    val ringAlpha = orbRingAlphaAnim.value.coerceIn(0f, 1f)
                                    if (!batchMode && ringAlpha > 0.005f && size.minDimension > 8f) {
                                        val ringInset = 2.4.dp.toPx() + progressRingStrokePx * 0.5f
                                        val ringDiameter = (size.minDimension - ringInset * 2f).coerceAtLeast(1f)
                                        val arcTopLeft = Offset(
                                            (size.width - ringDiameter) * 0.5f,
                                            (size.height - ringDiameter) * 0.5f,
                                        )
                                        val arcSize = Size(ringDiameter, ringDiameter)
                                        val sweepAngle = orbRingProgressAnim.value.coerceIn(0f, 1f) * 360f
                                        drawArc(
                                            color = palette.primary.copy(alpha = (if (isLight) 0.16f else 0.22f) * ringAlpha),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = cachedProgressArcStroke,
                                        )
                                        if (sweepAngle > 1f && !powerSave) {
                                            drawArc(
                                                color = palette.primary.copy(alpha = 0.24f * ringAlpha),
                                                startAngle = -90f,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = cachedProgressGlowStroke,
                                            )
                                        }
                                        if (sweepAngle > 0.5f) {
                                            drawArc(
                                                color = palette.primary.copy(alpha = 0.94f * ringAlpha),
                                                startAngle = -90f,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = cachedProgressArcStroke,
                                            )
                                        }
                                    }
                                }
                            }
                            if (showOrbIcon) {
                                val orbIconMode = when {
                                    batchMode -> DockOrbIconMode.BATCH_CLOSE
                                    showImportCompletedCheck -> DockOrbIconMode.IMPORT_COMPLETE
                                    importInProgress || importJustCompleted -> DockOrbIconMode.IMPORT_PROGRESS
                                    else -> DockOrbIconMode.ACTION
                                }
                                AnimatedContent(
                                    targetState = orbIconMode,
                                    // Hide icon pixels when orbGlassHidden == true (while outer alpha stays 1f so Vitro
                                    // executes clear(pane)); otherwise scale `+` concentrically with orbLandScale.
                                    modifier = Modifier.graphicsLayer {
                                        alpha = if (orbGlassHidden) 0f else 1f
                                        val landScale = orbLandScale.value.coerceIn(0.6f, 1.35f)
                                        scaleX = landScale
                                        scaleY = landScale
                                    },
                                    transitionSpec = {
                                        if (targetState == DockOrbIconMode.IMPORT_COMPLETE) {
                                            (fadeIn(tween(durationMillis = 180)) + scaleIn(
                                                initialScale = 0.65f,
                                                animationSpec = spring(dampingRatio = 0.60f, stiffness = 480f),
                                            )) togetherWith (fadeOut(tween(durationMillis = 110)) + scaleOut(
                                                targetScale = 0.72f,
                                                animationSpec = tween(durationMillis = 110),
                                            ))
                                        } else {
                                            (fadeIn(tween(durationMillis = 180)) + scaleIn(
                                                initialScale = 0.72f,
                                                animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMedium),
                                            )) togetherWith (fadeOut(tween(durationMillis = 140)) + scaleOut(
                                                targetScale = 0.72f,
                                                animationSpec = tween(durationMillis = 140),
                                            ))
                                        }
                                    },
                                    label = "dockOrbIconMorph",
                                ) { mode ->
                                    when (mode) {
                                        DockOrbIconMode.BATCH_CLOSE -> {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "退出批量选择",
                                                tint = palette.text,
                                                modifier = Modifier.size(23.dp),
                                            )
                                        }
                                        DockOrbIconMode.IMPORT_PROGRESS -> {
                                            Box(modifier = Modifier.size(23.dp))
                                        }
                                        DockOrbIconMode.IMPORT_COMPLETE -> {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "导入完成",
                                                tint = palette.primary,
                                                modifier = Modifier.size(23.dp),
                                            )
                                        }
                                        DockOrbIconMode.ACTION -> {
                                            val displayAction = activeAction
                                            if (displayAction != null) {
                                                Icon(
                                                    imageVector = displayAction.icon,
                                                    contentDescription = displayAction.contentDescription,
                                                    tint = palette.text,
                                                    modifier = Modifier.size(23.dp),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "退出批量选择",
                                                    tint = palette.text,
                                                    modifier = Modifier.size(23.dp),
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
        }
    }
}

@Composable
private fun DockBatchActionSlot(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    highlightTint: Color = tint,
    mutedAlpha: Float = 0.34f,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
        label = "batchSlotScale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else mutedAlpha,
        animationSpec = tween(durationMillis = 160),
        label = "batchSlotAlpha",
    )
    val resolvedTint = if (isHighlighted && enabled) highlightTint else tint
    val slotFillAlpha = when {
        pressed && enabled -> 0.10f
        isHighlighted && enabled -> 0.12f
        else -> 0f
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (slotFillAlpha > 0f) {
                    (if (isHighlighted && enabled) highlightTint else tint).copy(alpha = slotFillAlpha)
                } else {
                    Color.Transparent
                },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = contentAlpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = resolvedTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = resolvedTint,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (isHighlighted && enabled) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private class DockSpringLoopState(
    var running: Boolean = false,
    var lastFrameNanos: Long = 0L,
)

private enum class DockOrbIconMode {
    BATCH_CLOSE,
    IMPORT_PROGRESS,
    IMPORT_COMPLETE,
    ACTION,
}

/**
 * Tunes Dock glass brightness and saturation uniformly for both main capsule and `+` orb so dark-mode
 * subtle corner glow is never lifted/oversaturated inside the Dock while preserving card scroll blur.
 */
private fun Glass.withCleanDockFrostedLook(isLight: Boolean): Glass = copy(
    brightness = if (isLight) brightness else brightness.coerceAtLeast(0.44f),
    saturation = if (isLight) 1.04f else 1.10f,
)
