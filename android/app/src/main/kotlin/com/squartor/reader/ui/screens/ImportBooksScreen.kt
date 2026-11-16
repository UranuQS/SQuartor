package com.squartor.reader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squartor.reader.model.AppPalette
import com.squartor.reader.ui.glass.Ios27GlassVariant
import com.squartor.reader.AmbientSubpageBackground
import com.squartor.reader.ui.glass.LiquidGlassDialog
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.ios27GlassSurface
import com.squartor.reader.ui.glass.rememberSafeGlassEffectState
import com.squartor.reader.ui.glass.vitroInteractiveCard
import com.squartor.reader.ui.state.ImportActivity
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.ScrollEdgeEffectStyle
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffectContainer
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import io.github.itshedimisawi.vitro.scrollEdgeEffect

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportBooksScreen(
    palette: AppPalette,
    totalBooks: Int,
    shelfCount: Int,
    importActivity: ImportActivity?,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onAddShelf: (String) -> Unit,
    onGoToShelf: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = palette.isLight
    val haptics = LocalHapticFeedback.current
    val readingStyle = LocalReadingStyle.current
    val powerSave = LocalPowerSaveMode.current
    val importBackdrop = rememberGlassBackdrop()
    val effectiveBackdrop = if (powerSave) null else importBackdrop
    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 20 } }

    val statusTop = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusTop, cutoutTop)
    val navBottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val topBarClearance = topInset + 10.dp + 44.dp + 12.dp

    val accentOnGlass = if (isLight) {
        lerpColor(palette.accentText, Color.Black, 0.20f)
    } else {
        lerpColor(palette.accentText, Color.White, 0.14f)
    }
    val secondaryText = palette.text.copy(alpha = if (isLight) 0.64f else 0.70f)

    var showCreateShelfDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalVitroBackdrop provides effectiveBackdrop,
        LocalGlassDarkTheme provides !isLight,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(palette.background),
        ) {
            // 1. 专属 Backdrop 光场层：给上方 iOS 27 液态玻璃控件提供折射来源
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

            // 2. 主滚动内容区
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEdgeEffect(
                        top = topBarClearance,
                        bottom = navBottom + 24.dp,
                        style = ScrollEdgeEffectStyle.Soft,
                        color = palette.background,
                        isTopHidden = !scrollState.canScrollBackward,
                        isBottomHidden = !scrollState.canScrollForward,
                    )
                    .verticalScroll(scrollState)
                    .padding(
                        start = 20.dp,
                        top = topBarClearance,
                        end = 20.dp,
                        bottom = navBottom + 32.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "添加书籍",
                            color = palette.text,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentOnGlass.copy(alpha = if (isLight) 0.11f else 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "共 $totalBooks 本 · $shelfCount 个书架",
                                color = accentOnGlass,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    fontFeatureSettings = "tnum",
                                ),
                            )
                        }
                    }
                    Text(
                        text = "自由选取多本电子书或扫描整目录，系统会自动识别封面、章节目录与编码。",
                        color = secondaryText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }

                // 实时导入进度卡片
                AnimatedVisibility(
                    visible = importActivity != null,
                    enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(150)),
                ) {
                    importActivity?.let { activity ->
                        ImportStatusGlassCard(
                            activity = activity,
                            palette = palette,
                            backdrop = effectiveBackdrop,
                            accentOnGlass = accentOnGlass,
                            onGoToShelf = onGoToShelf,
                        )
                    }
                }

                // 主操作卡 1：多文件自由选择
                ImportOptionHeroCard(
                    title = "选择多本书籍",
                    subtitle = "从本地或云盘文档器同时多选 TXT、EPUB 文件，逐一高速解析入库。",
                    badges = listOf("EPUB 电子书", "TXT 纯文本", "支持多选"),
                    icon = Icons.Rounded.UploadFile,
                    palette = palette,
                    backdrop = effectiveBackdrop,
                    accentOnGlass = accentOnGlass,
                    highlighted = true,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPickFiles()
                    },
                )

                // 主操作卡 2：扫描并导入文件夹
                ImportOptionHeroCard(
                    title = "扫描并导入文件夹",
                    subtitle = "选定书籍文件夹后递归扫描全部子目录，自动跳过重复条目。",
                    badges = listOf("递归目录", "自动去重"),
                    icon = Icons.Rounded.FolderOpen,
                    palette = palette,
                    backdrop = effectiveBackdrop,
                    accentOnGlass = accentOnGlass,
                    highlighted = false,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPickFolder()
                    },
                )

                // 次级辅助行：快速新建书架
                val createShelfInteraction = remember { MutableInteractionSource() }
                val createShelfGlassState = rememberSafeGlassEffectState(
                    interactionSource = createShelfInteraction,
                    active = true,
                )
                val shelfRowShape = RoundedCornerShape(20.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 62.dp)
                        .ios27GlassSurface(
                            shape = shelfRowShape,
                            palette = palette,
                            style = readingStyle,
                            variant = Ios27GlassVariant.Card,
                            backdrop = effectiveBackdrop,
                            isInteractive = true,
                            state = createShelfGlassState,
                            interactionSource = createShelfInteraction,
                            showContrastBorder = false,
                        )
                        .clip(shelfRowShape)
                        .clickable(
                            interactionSource = createShelfInteraction,
                            indication = null,
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showCreateShelfDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentOnGlass.copy(alpha = if (isLight) 0.11f else 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = null,
                            tint = accentOnGlass,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "新建书架分组",
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "导入后可随时一键归档到自定义书架",
                            color = secondaryText,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.text.copy(alpha = if (isLight) 0.45f else 0.52f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // 底层支持说明 Bento 双格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ImportCapabilityTile(
                        icon = Icons.Rounded.AutoStories,
                        title = "EPUB 图文引擎",
                        subtitle = "提取封面、插画与原生目录层级",
                        palette = palette,
                        accentOnGlass = accentOnGlass,
                        modifier = Modifier.weight(1f),
                    )
                    ImportCapabilityTile(
                        icon = Icons.Rounded.Description,
                        title = "TXT 智能分章",
                        subtitle = "UTF-8 / GB18030 自适应识别",
                        palette = palette,
                        accentOnGlass = accentOnGlass,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 底部快捷跳转书架按钮
                val goShelfInteraction = remember { MutableInteractionSource() }
                val goShelfGlassState = rememberSafeGlassEffectState(
                    interactionSource = goShelfInteraction,
                    active = true,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .ios27GlassSurface(
                            shape = CircleShape,
                            palette = palette,
                            style = readingStyle,
                            variant = Ios27GlassVariant.SubtlePill,
                            backdrop = effectiveBackdrop,
                            isInteractive = true,
                            state = goShelfGlassState,
                            interactionSource = goShelfInteraction,
                            showContrastBorder = false,
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = goShelfInteraction,
                            indication = null,
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onGoToShelf()
                        }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = "前往书架浏览藏书",
                        color = palette.text,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // 3. 悬浮在 Backdrop 之上的 iOS 27 顶部导航栏
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = topInset + 10.dp, end = 18.dp)
                    .glassEffectContainer(spacing = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val backGlassState = rememberSafeGlassEffectState(
                    interactionSource = backInteraction,
                    active = true,
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .ios27GlassSurface(
                            shape = CircleShape,
                            palette = palette,
                            style = readingStyle,
                            variant = Ios27GlassVariant.Chrome,
                            backdrop = effectiveBackdrop,
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

                AnimatedVisibility(
                    visible = isScrolled,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(140)),
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .ios27GlassSurface(
                                shape = CircleShape,
                                palette = palette,
                                style = readingStyle,
                                variant = Ios27GlassVariant.Chrome,
                                backdrop = effectiveBackdrop,
                                isInteractive = false,
                                showContrastBorder = false,
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "添加书籍",
                            color = palette.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (showCreateShelfDialog) {
                CreateShelfQuickDialog(
                    palette = palette,
                    accentOnGlass = accentOnGlass,
                    onDismiss = { showCreateShelfDialog = false },
                    onConfirm = { name ->
                        onAddShelf(name)
                        showCreateShelfDialog = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportOptionHeroCard(
    title: String,
    subtitle: String,
    badges: List<String>,
    icon: ImageVector,
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    accentOnGlass: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    val secondaryText = palette.text.copy(alpha = if (isLight) 0.64f else 0.70f)
    val iconContainerBg = if (highlighted) {
        accentOnGlass.copy(alpha = if (isLight) 0.14f else 0.24f)
    } else {
        if (isLight) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.10f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .vitroInteractiveCard(
                shape = RoundedCornerShape(24.dp),
                variant = Ios27GlassVariant.Card,
                palette = palette,
                style = readingStyle,
                backdrop = backdrop,
                verticalOnly = false,
                maxPullDp = 12.dp,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconContainerBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) accentOnGlass else palette.text,
                modifier = Modifier.size(25.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = palette.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
            )
            Text(
                text = subtitle,
                color = secondaryText,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                badges.forEach { badge ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (highlighted) {
                                    accentOnGlass.copy(alpha = if (isLight) 0.10f else 0.18f)
                                } else {
                                    if (isLight) Color.White.copy(alpha = 0.56f)
                                    else Color.White.copy(alpha = 0.09f)
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 3.5.dp),
                    ) {
                        Text(
                            text = badge,
                            color = if (highlighted) accentOnGlass else secondaryText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isLight) Color.White.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.10f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.text.copy(alpha = if (isLight) 0.58f else 0.68f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ImportStatusGlassCard(
    activity: ImportActivity,
    palette: AppPalette,
    backdrop: GlassBackdrop?,
    accentOnGlass: Color,
    onGoToShelf: () -> Unit,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    val statusTint = when {
        activity.failed -> if (isLight) Color(0xFFB91C1C) else Color(0xFFFF7B7B)
        else -> accentOnGlass
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .ios27GlassSurface(
                shape = RoundedCornerShape(20.dp),
                palette = palette,
                style = readingStyle,
                variant = Ios27GlassVariant.Card,
                backdrop = backdrop,
                isInteractive = false,
                showContrastBorder = false,
            )
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                activity.active -> CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = accentOnGlass,
                    modifier = Modifier.size(20.dp),
                )
                activity.failed -> Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = statusTint,
                    modifier = Modifier.size(22.dp),
                )
                else -> Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = statusTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    color = palette.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (activity.detail.isNotEmpty()) {
                    Text(
                        text = activity.detail,
                        color = palette.text.copy(alpha = if (isLight) 0.62f else 0.68f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!activity.active && !activity.failed) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentOnGlass.copy(alpha = if (isLight) 0.14f else 0.22f))
                        .clickable(onClick = onGoToShelf)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "去书架",
                        color = accentOnGlass,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (activity.active && activity.total > 0) {
            LinearProgressIndicator(
                progress = { activity.progress },
                color = accentOnGlass,
                trackColor = palette.text.copy(alpha = if (isLight) 0.08f else 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun ImportCapabilityTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    palette: AppPalette,
    accentOnGlass: Color,
    modifier: Modifier = Modifier,
) {
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current
    val backdrop = LocalVitroBackdrop.current
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .ios27GlassSurface(
                shape = RoundedCornerShape(18.dp),
                palette = palette,
                style = readingStyle,
                variant = Ios27GlassVariant.Card,
                backdrop = backdrop,
                isInteractive = false,
                showContrastBorder = false,
            )
            .padding(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentOnGlass,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = title,
            color = palette.text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            color = palette.text.copy(alpha = if (isLight) 0.62f else 0.68f),
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
internal fun CreateShelfQuickDialog(
    palette: AppPalette,
    accentOnGlass: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var shelfName by remember { mutableStateOf("") }
    val isLight = palette.isLight
    val readingStyle = LocalReadingStyle.current

    LiquidGlassDialog(
        onDismissRequest = onDismiss,
        palette = palette,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "新建书架",
                color = palette.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLight) Color.White.copy(alpha = 0.64f) else Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (shelfName.isEmpty()) {
                    Text(
                        text = "输入书架名称…",
                        color = palette.text.copy(alpha = if (isLight) 0.42f else 0.48f),
                        fontSize = 14.5.sp,
                    )
                }
                BasicTextField(
                    value = shelfName,
                    onValueChange = { shelfName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = palette.text,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(accentOnGlass),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                val cancelInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .ios27GlassSurface(
                            shape = CircleShape,
                            palette = palette,
                            style = readingStyle,
                            variant = Ios27GlassVariant.SubtlePill,
                            backdrop = null,
                            isInteractive = false,
                            state = null,
                            interactionSource = cancelInteraction,
                            showContrastBorder = true,
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = cancelInteraction,
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "取消",
                        color = palette.text.copy(alpha = 0.78f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                val trimmed = shelfName.trim()
                val okInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .ios27GlassSurface(
                            shape = CircleShape,
                            palette = palette,
                            style = readingStyle,
                            variant = Ios27GlassVariant.AccentPill,
                            backdrop = null,
                            isInteractive = false,
                            state = null,
                            interactionSource = okInteraction,
                            showContrastBorder = true,
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = okInteraction,
                            indication = null,
                            enabled = trimmed.isNotEmpty(),
                        ) {
                            onConfirm(trimmed)
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "创建",
                        color = accentOnGlass.copy(alpha = if (trimmed.isNotEmpty()) 1f else 0.45f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
