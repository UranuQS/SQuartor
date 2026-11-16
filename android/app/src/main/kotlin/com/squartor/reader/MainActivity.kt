package com.squartor.reader

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Bitmap as AndroidBitmap
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient as AndroidRadialGradient
import android.graphics.Rect as AndroidRect
import android.graphics.RectF as AndroidRectF
import android.graphics.Shader as AndroidShader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import com.squartor.reader.data.EpubStreamEngine
import com.squartor.reader.data.TxtStreamEngine
import com.squartor.reader.model.AmbientGlowSource
import com.squartor.reader.model.AppBrightnessMode
import com.squartor.reader.model.AppPalette
import com.squartor.reader.model.AppThemeId
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.BookFormat
import com.squartor.reader.model.CloudSyncSettings
import com.squartor.reader.model.ImportedFont
import com.squartor.reader.model.ReaderBackgroundId
import com.squartor.reader.model.ReadingStyle
import com.squartor.reader.model.StorageStats
import com.squartor.reader.model.readerPalettes
import com.squartor.reader.ui.glass.CoverAssetCache
import com.squartor.reader.ui.glass.DockContextualAction
import com.squartor.reader.ui.glass.FlClashNavigationDock
import com.squartor.reader.ui.glass.GeminiCornerAmbientCanvas
import com.squartor.reader.ui.glass.GeminiCornerGlowBrushCache
import com.squartor.reader.ui.glass.GeminiCornerToneState
import com.squartor.reader.ui.glass.Ios27GlassVariant
import com.squartor.reader.ui.glass.IsolatedAmbientGlassContainer
import com.squartor.reader.ui.glass.LocalAmbientFlowSeconds
import com.squartor.reader.ui.glass.LocalAmbientGlowColors
import com.squartor.reader.ui.glass.LocalAmbientGlowColorsState
import com.squartor.reader.ui.glass.LocalBookLaunchTracker
import com.squartor.reader.ui.glass.LocalDockOffscreenHiddenState
import com.squartor.reader.ui.glass.LocalHeroBookSlotAlpha
import com.squartor.reader.ui.glass.LocalPagerTransitioning
import com.squartor.reader.ui.glass.LocalPagerTransitioningState
import com.squartor.reader.ui.glass.LocalPowerSaveMode
import com.squartor.reader.ui.glass.LocalReadingStyle
import com.squartor.reader.ui.glass.LocalRootHandlesBottomAmbient
import com.squartor.reader.ui.glass.LocalVitroBackdrop
import com.squartor.reader.ui.glass.LocalVitroPopoverHost
import com.squartor.reader.ui.glass.VitroGlassPopoverItem
import com.squartor.reader.ui.glass.VitroGlassPopoverMenu
import com.squartor.reader.ui.glass.VitroPopoverOverlayHost
import com.squartor.reader.ui.glass.drawGeminiCornerAmbientGlow
import com.squartor.reader.ui.glass.drawGeminiCornerAmbientGlowTextures
import com.squartor.reader.ui.glass.ios27GlassSurface
import com.squartor.reader.ui.glass.rememberAmbientFlowSeconds
import com.squartor.reader.ui.glass.rememberVitroPopoverHostState
import com.squartor.reader.ui.screens.ShelfBatchActions
import com.squartor.reader.ui.reader.NativeStaticLayoutPager
import com.squartor.reader.ui.reader.ReaderBlockItem
import com.squartor.reader.ui.reader.ReaderPreloadCache
import com.squartor.reader.ui.reader.ReaderScreen
import com.squartor.reader.ui.reader.computeReaderPaginationMetrics
import com.squartor.reader.ui.screens.ImportBooksScreen
import com.squartor.reader.ui.screens.ReadingNowScreen
import com.squartor.reader.ui.screens.SettingsScreen
import com.squartor.reader.ui.screens.ShelfScreen
import com.squartor.reader.ui.screens.StatsDetailScreen
import com.squartor.reader.ui.screens.StatsScreen
import com.squartor.reader.ui.screens.trimShelfCoverCache
import com.squartor.reader.ui.state.AppViewModel
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private val MAIN_POWER_SAVE_FILTER = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                requestHighestRefreshRate()
            }
        }
    }
    private var powerReceiverRegistered = false
    private var isPowerSaveState by mutableStateOf(false)

    private val pickBooksLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
        val data = result.data!!
        Thread {
            val uris = mutableListOf<Uri>()
            data.data?.let { uris.add(it) }
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    uris.add(clip.getItemAt(i).uri)
                }
            }
            val picked = resolvePickedUrisToFiles(uris)
            if (picked.isNotEmpty()) {
                runOnUiThread { viewModel.importFiles(picked) }
            }
        }.start()
    }

    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) return@registerForActivityResult
        val treeUri = result.data!!.data!!
        try {
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Throwable) {
        }
        Thread {
            val picked = mutableListOf<String>()
            val directDir = resolveRealDirectoryFromTreeUri(treeUri)
            if (directDir != null && directDir.isDirectory && directDir.canRead()) {
                directDir.walkTopDown().maxDepth(6).forEach { file ->
                    if (file.isFile && isImportableBookName(file.name) && file.length() > 0L) {
                        picked.add(file.absolutePath)
                    }
                }
            }
            if (picked.isEmpty()) {
                val targetRoot = File(cacheDir, "picked_books/${System.currentTimeMillis()}").apply { mkdirs() }
                DocumentFile.fromTreeUri(this, treeUri)?.let { rootDoc ->
                    scanDocumentFileTree(rootDoc, targetRoot, picked, 0)
                }
            }
            if (picked.isNotEmpty()) {
                runOnUiThread { viewModel.importFiles(picked) }
            }
        }.start()
    }

    private val pickFontLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) return@registerForActivityResult
        val uri = result.data!!.data!!
        Thread {
            try {
                val name = displayNameForUri(uri) ?: "custom_font.ttf"
                val targetDir = File(cacheDir, "picked_fonts").apply { mkdirs() }
                val target = File(targetDir, name)
                contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                }
                if (target.exists() && target.length() > 0L) {
                    runOnUiThread { viewModel.importFontFile(target) }
                }
            } catch (_: Throwable) {
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestHighestRefreshRate()
        handleIncomingBookIntent(intent)

        setContent {
            SQuartorRootApp(
                viewModel = viewModel,
                isPowerSave = isPowerSaveState,
                onPickBookFiles = ::launchBookFilePicker,
                onPickBookFolder = ::launchBookFolderPicker,
                onPickFontFile = ::launchFontFilePicker,
                onSaveImageToGallery = ::saveImageBytesToGallery,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingBookIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (!powerReceiverRegistered) {
            runCatching {
                registerReceiver(powerSaveReceiver, MAIN_POWER_SAVE_FILTER)
                powerReceiverRegistered = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighestRefreshRate()
    }

    override fun onStop() {
        if (powerReceiverRegistered) {
            runCatching { unregisterReceiver(powerSaveReceiver) }
            powerReceiverRegistered = false
        }
        super.onStop()
    }

    override fun onPause() {
        viewModel.flushPendingPersistence()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            requestHighestRefreshRate()
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        CoverAssetCache.trimMemory(level)
        trimShelfCoverCache(level)
        ReaderPreloadCache.trimMemory(level)
    }

    private fun launchBookFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("application/epub+zip", "text/plain", "application/octet-stream", "*/*"),
            )
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            pickBooksLauncher.launch(intent)
        } catch (_: Throwable) {
        }
    }

    private fun launchBookFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        try {
            pickFolderLauncher.launch(intent)
        } catch (_: Throwable) {
        }
    }

    private fun launchFontFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            pickFontLauncher.launch(intent)
        } catch (_: Throwable) {
        }
    }

    private fun handleIncomingBookIntent(intent: Intent?) {
        if (intent == null) return
        val importDirPath = intent.getStringExtra("import_dir")
        if (!importDirPath.isNullOrBlank()) {
            Thread {
                val dir = File(importDirPath)
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.walkTopDown().maxDepth(4)
                        .filter { it.isFile && isImportableBookName(it.name) && it.length() > 0L }
                        .map { it.absolutePath }
                        .toList()
                    if (files.isNotEmpty()) {
                        runOnUiThread { viewModel.importFiles(files) }
                    }
                }
            }.start()
            return
        }
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            else -> null
        } ?: return

        Thread {
            val picked = resolvePickedUrisToFiles(listOf(uri))
            if (picked.isNotEmpty()) {
                runOnUiThread { viewModel.importFiles(picked) }
            }
        }.start()
    }

    private fun resolvePickedUrisToFiles(uris: List<Uri>): List<String> {
        val picked = mutableListOf<String>()
        val targetRoot by lazy {
            File(cacheDir, "picked_books/${System.currentTimeMillis()}").apply { mkdirs() }
        }
        for (uri in uris) {
            try {
                val realPath = resolveRealPathFromUri(uri)
                if (realPath != null && File(realPath).exists() && File(realPath).length() > 0L) {
                    picked.add(realPath)
                } else {
                    val displayName = displayNameForUri(uri) ?: "book"
                    if (isImportableBookName(displayName) || isImportableBookUri(uri)) {
                        val target = uniqueTargetFile(targetRoot, displayNameForImport(uri, displayName))
                        contentResolver.openInputStream(uri)?.use { input ->
                            target.outputStream().use { out -> input.copyTo(out) }
                        }
                        if (target.exists() && target.length() > 0L) {
                            picked.add(target.absolutePath)
                        } else {
                            target.delete()
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return picked
    }

    private fun saveImageBytesToGallery(bytes: ByteArray, ext: String) {
        if (bytes.isEmpty()) return
        Thread {
            try {
                val cleanExt = ext.lowercase().ifBlank { "jpg" }
                val mimeType = when (cleanExt) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    else -> "image/jpeg"
                }
                val fileName = "squartor_${System.currentTimeMillis()}.$cleanExt"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SQuartor")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@Thread
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }
                runOnUiThread {
                    Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Throwable) {
                runOnUiThread {
                    Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun scanDocumentFileTree(
        dir: DocumentFile,
        targetDir: File,
        picked: MutableList<String>,
        depth: Int,
    ) {
        if (depth > 6) return
        try {
            for (file in dir.listFiles()) {
                if (file.isDirectory) {
                    scanDocumentFileTree(file, targetDir, picked, depth + 1)
                } else if (file.isFile) {
                    val name = file.name ?: ""
                    val mime = file.type?.lowercase() ?: ""
                    val isBook = isImportableBookName(name) || mime.contains("epub") || mime.startsWith("text/")
                    if (isBook && file.length() > 0L) {
                        val targetName = displayNameForImport(file.uri, name)
                        val output = uniqueTargetFile(targetDir, targetName)
                        contentResolver.openInputStream(file.uri)?.use { input ->
                            output.outputStream().use { out -> input.copyTo(out) }
                        }
                        if (output.exists() && output.length() > 0L) {
                            picked.add(output.absolutePath)
                        } else {
                            output.delete()
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun resolveRealPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null && File(path).exists() && File(path).canRead()) return path
        }
        if (uri.scheme == "content") {
            val authority = uri.authority
            val docId = try {
                if (DocumentsContract.isDocumentUri(this, uri)) {
                    DocumentsContract.getDocumentId(uri)
                } else {
                    uri.path?.substringAfter("/document/", "")?.ifEmpty { uri.path?.substringAfter("/tree/", "") }
                }
            } catch (_: Throwable) {
                null
            }
            if (docId != null) {
                if (docId.startsWith("raw:")) {
                    val file = File(docId.removePrefix("raw:"))
                    if (file.exists() && file.canRead()) return file.absolutePath
                }
                if (authority == "com.android.externalstorage.documents") {
                    val split = docId.split(":")
                    val type = split[0]
                    val relPath = if (split.size > 1) split[1] else ""
                    val fullPath = if ("primary".equals(type, ignoreCase = true)) {
                        "${Environment.getExternalStorageDirectory()}/$relPath"
                    } else {
                        "/storage/$type/$relPath"
                    }
                    val file = File(fullPath)
                    if (file.exists() && file.canRead()) return file.absolutePath
                }
            }
        }
        return null
    }

    private fun resolveRealDirectoryFromTreeUri(treeUri: Uri): File? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val split = docId.split(":")
            val type = split[0]
            val relPath = if (split.size > 1) split[1] else ""
            val candidate = if ("primary".equals(type, ignoreCase = true)) {
                File(Environment.getExternalStorageDirectory(), relPath)
            } else {
                File("/storage/$type/$relPath")
            }
            if (candidate.exists() && candidate.isDirectory && candidate.canRead()) candidate else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun displayNameForImport(uri: Uri, fallback: String): String {
        val name = fallback.ifBlank { uri.lastPathSegment ?: "book" }
        if (isImportableBookName(name)) return name
        val lowerMime = contentResolver.getType(uri)?.lowercase() ?: ""
        return when {
            lowerMime.contains("epub") -> "$name.epub"
            lowerMime.startsWith("text/") -> "$name.txt"
            else -> name
        }
    }

    private fun displayNameForUri(uri: Uri): String? {
        if (uri.scheme == "file") return File(uri.path ?: return null).name
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else null
        } catch (_: Throwable) {
            null
        } finally {
            cursor?.close()
        } ?: uri.lastPathSegment
    }

    private fun isImportableBookUri(uri: Uri): Boolean {
        val value = uri.toString().lowercase()
        val type = contentResolver.getType(uri)?.lowercase() ?: ""
        return value.contains(".epub") || value.contains(".txt") || type.contains("epub") || type.startsWith("text/")
    }

    private fun isImportableBookName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".epub") || lower.endsWith(".txt")
    }

    private fun uniqueTargetFile(targetDir: File, displayName: String): File {
        val cleanName = displayName.replace(Regex("""[\\/:*?"<>|]"""), "_")
        var candidate = File(targetDir, cleanName)
        if (!candidate.exists()) return candidate
        val dot = cleanName.lastIndexOf('.')
        val base = if (dot > 0) cleanName.substring(0, dot) else cleanName
        val ext = if (dot > 0) cleanName.substring(dot) else ""
        var idx = 2
        while (candidate.exists()) {
            candidate = File(targetDir, "$base ($idx)$ext")
            idx++
        }
        return candidate
    }

    private fun requestHighestRefreshRate() {
        val isPowerSave = getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        isPowerSaveState = isPowerSave
        if (isPowerSave) {
            // 低电量模式下不强行锁定高刷 Mode/DecorView 投票，配合系统 60Hz 省电稳帧避免刷新率来回跳变
            window.attributes = window.attributes.apply {
                if (preferredDisplayModeId != 0 || preferredRefreshRate != 0f) {
                    preferredDisplayModeId = 0
                    preferredRefreshRate = 0f
                }
            }
            return
        }
        val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return
        val currentMode = currentDisplay.mode
        val bestMode = currentDisplay.supportedModes
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = bestMode.modeId
            preferredRefreshRate = bestMode.refreshRate
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun SQuartorRootApp(
    viewModel: AppViewModel,
    isPowerSave: Boolean,
    onPickBookFiles: () -> Unit,
    onPickBookFolder: () -> Unit,
    onPickFontFile: () -> Unit,
    onSaveImageToGallery: (ByteArray, String) -> Unit,
) {
    val books by viewModel.books.collectAsState()
    val shelves by viewModel.shelves.collectAsState()
    val style by viewModel.style.collectAsState()
    val fonts by viewModel.fonts.collectAsState()
    val readingStats by viewModel.readingStats.collectAsState()
    val dailyTotals by viewModel.dailyReadingTotals.collectAsState()
    val cloudSync by viewModel.cloudSyncSettings.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val importActivity by viewModel.importActivity.collectAsState()
    val importInProgress by viewModel.importInProgress.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val importJustCompleted by viewModel.importJustCompleted.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncFeedbackMessage by viewModel.syncFeedbackMessage.collectAsState()

    val isSystemDark = isSystemInDarkTheme()
    val isLight = style.isEffectiveLight(isSystemDark)

    val context = LocalContext.current
    // 启动即预热深浅两套 Monet 动态取色种子，切换深浅主题帧零阻塞读取，避免再跑一遍 dynamic*ColorScheme
    var cachedDynamicLightSeed by remember(context) {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                style.appTheme == AppThemeId.wallpaper &&
                isLight
            ) {
                runCatching { dynamicLightColorScheme(context).primary }.getOrNull()
            } else null,
        )
    }
    var cachedDynamicDarkSeed by remember(context) {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                style.appTheme == AppThemeId.wallpaper &&
                !isLight
            ) {
                runCatching { dynamicDarkColorScheme(context).primary }.getOrNull()
            } else null,
        )
    }
    LaunchedEffect(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (cachedDynamicLightSeed == null) {
                cachedDynamicLightSeed = withContext(Dispatchers.Default) {
                    runCatching { dynamicLightColorScheme(context).primary }.getOrNull()
                }
            }
            if (cachedDynamicDarkSeed == null) {
                cachedDynamicDarkSeed = withContext(Dispatchers.Default) {
                    runCatching { dynamicDarkColorScheme(context).primary }.getOrNull()
                }
            }
        }
    }
    // 异步预热阅读器正文/标题可变字体，防止点击开书时在主线程同步解析 NotoSansSC-VF.ttf 引起掉帧
    LaunchedEffect(context, style.fontPath, style.fontWeightValue) {
        withContext(Dispatchers.IO) {
            runCatching {
                NativeStaticLayoutPager.resolveTypeface(context, style.fontPath, style.fontWeightValue)
                NativeStaticLayoutPager.resolveTypeface(
                    context,
                    style.fontPath,
                    (style.fontWeightValue + 200).coerceIn(600, 900),
                )
            }
        }
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showStatsDetail by rememberSaveable { mutableStateOf(false) }
    var showImportPage by rememberSaveable { mutableStateOf(false) }
    var shelfSelectionMode by remember { mutableStateOf(false) }
    var shelfBatchAllSelected by remember { mutableStateOf(false) }
    var shelfBatchHasSelection by remember { mutableStateOf(false) }
    var shelfBatchActions by remember { mutableStateOf<ShelfBatchActions?>(null) }
    var shelfEnterBatchToken by remember { mutableIntStateOf(0) }
    var shelfSeriesSheetOpen by remember { mutableStateOf(false) }
    var shelfSeriesSheetRendered by remember { mutableStateOf(false) }
    val shelfDismissSeriesRef = remember { arrayOfNulls<(() -> Unit)?>(1) }
    var settingsSubPageOpen by remember { mutableStateOf(false) }
    var activeReaderBookId by rememberSaveable { mutableStateOf<String?>(null) }
    var retainedReaderBookId by rememberSaveable { mutableStateOf<String?>(null) }
    // 阅读器开启与退场（及系列弹窗收起）全周期保持后台 Tab 与 Hero 快照稳定，
    // 直至 Hero 封面与系列弹窗双双落地并卸载后再恢复刷新，彻底消除“按最近阅读”重排引发的错位与掉帧。
    val shouldFreezeBackgroundTabs by remember {
        derivedStateOf {
            retainedReaderBookId != null || (selectedTab == 1 && shelfSeriesSheetRendered)
        }
    }
    val isAnyOverlayActive by remember {
        derivedStateOf {
            activeReaderBookId != null ||
                retainedReaderBookId != null ||
                (selectedTab == 1 && (shelfSeriesSheetOpen || shelfSeriesSheetRendered))
        }
    }
    var postOverlayBlurSuspend by remember { mutableStateOf(isAnyOverlayActive) }
    LaunchedEffect(isAnyOverlayActive) {
        if (isAnyOverlayActive) {
            postOverlayBlurSuspend = true
        } else if (postOverlayBlurSuspend) {
            // Keep Chrome + Dock Vitro blurs and ambient palette extraction suspended (~520ms) across Shelf animateItem re-sort and Dock slide-in settle.
            delay(520L)
            postOverlayBlurSuspend = false
        }
    }
    val frozenTabHolder = remember {
        object {
            var booksSnapshot = books
            var statsSnapshot = readingStats
            var dailySnapshot = dailyTotals
        }
    }
    val snapBooks = frozenTabHolder.booksSnapshot
    val structuralOrMetaChanged = books.size != snapBooks.size ||
        books.indices.any { idx ->
            val a = books[idx]
            val b = snapBooks[idx]
            a.id != b.id ||
                a.title != b.title ||
                a.author != b.author ||
                a.seriesOverride != b.seriesOverride ||
                a.seriesOrder != b.seriesOrder ||
                a.shelfName != b.shelfName
        }
    if (!shouldFreezeBackgroundTabs || structuralOrMetaChanged) {
        frozenTabHolder.booksSnapshot = books
        frozenTabHolder.statsSnapshot = readingStats
        frozenTabHolder.dailySnapshot = dailyTotals
    }
    val displayTabBooks = frozenTabHolder.booksSnapshot
    val displayTabStats = frozenTabHolder.statsSnapshot
    val displayTabDailyTotals = frozenTabHolder.dailySnapshot

    val dynamicSeed = if (isLight) cachedDynamicLightSeed else cachedDynamicDarkSeed
    val seedHeroBook = remember(displayTabBooks) { AppViewModel.selectHeroBook(displayTabBooks) }
    val isSeedTransitionBusy = activeReaderBookId != null ||
        retainedReaderBookId != null ||
        shelfSeriesSheetOpen ||
        shelfSeriesSheetRendered ||
        postOverlayBlurSuspend
    LaunchedEffect(
        seedHeroBook?.id,
        seedHeroBook?.coverPath,
        isSeedTransitionBusy,
        style.appTheme,
        style.ambientGlowSource,
    ) {
        if (
            isSeedTransitionBusy ||
            style.appTheme != AppThemeId.wallpaper ||
            style.ambientGlowSource != AmbientGlowSource.bookCover
        ) {
            return@LaunchedEffect
        }
        val target = seedHeroBook ?: return@LaunchedEffect
        delay(450L)
        CoverAssetCache.ensureLoaded(target)
    }
    val liveHeroCoverColor = if (
        style.appTheme == AppThemeId.wallpaper &&
        style.ambientGlowSource == AmbientGlowSource.bookCover
    ) {
        CoverAssetCache.getExtractedCoverColor(seedHeroBook)
    } else {
        null
    }
    val rawEffectiveSeed = liveHeroCoverColor ?: dynamicSeed
    val usesWallpaperDynamicSeed =
        style.customThemeColorValue == null && style.appTheme == AppThemeId.wallpaper
    val targetDynamicSeed = if (usesWallpaperDynamicSeed) {
        rawEffectiveSeed ?: AppThemeId.wallpaper.seedColor
    } else {
        null
    }
    // Keep root palette stable across Reader / SeriesModal open-close animations and commit once after settle
    // instead of running a per-frame animateColorAsState that recomposes the entire 4-tab tree.
    var settledDynamicSeed by remember(style.appTheme, isLight) {
        mutableStateOf(targetDynamicSeed)
    }
    LaunchedEffect(usesWallpaperDynamicSeed, targetDynamicSeed, isSeedTransitionBusy) {
        if (!usesWallpaperDynamicSeed || isSeedTransitionBusy) return@LaunchedEffect
        if (settledDynamicSeed != targetDynamicSeed) {
            delay(220L)
            settledDynamicSeed = targetDynamicSeed
        }
    }
    val effectiveSeed = if (usesWallpaperDynamicSeed) settledDynamicSeed else null

    val palette = remember(style, isLight, effectiveSeed) {
        style.resolvePalette(isLight = isLight, dynamicSeedColor = effectiveSeed)
    }

    val view = LocalView.current
    // edge-to-edge 已在 MainActivity.onCreate 初始化，深浅切换时只更新状态栏/导航栏图标对比度，避免窗口重排
    DisposableEffect(view, isLight) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
        }
        onDispose {}
    }

    var showAddBookMenu by remember { mutableStateOf(false) }
    var addPopoverPresenting by remember { mutableStateOf(false) }
    var addOrbVisibility by remember { mutableFloatStateOf(1f) }
    var showCreateShelfFromDock by remember { mutableStateOf(false) }
    val createShelfDockVisibleState = remember { MutableTransitionState(false) }
    createShelfDockVisibleState.targetState = showCreateShelfFromDock
    val createShelfDockRendered = createShelfDockVisibleState.currentState || createShelfDockVisibleState.targetState
    var addDockOrbBounds by remember { mutableStateOf<Rect?>(null) }
    var launchSourceRect by remember { mutableStateOf<Rect?>(null) }
    var shelfBookBoundsResolver by remember { mutableStateOf<((String) -> Rect?)?>(null) }
    var shelfClosingAlphaResolver by remember { mutableStateOf<((String) -> Float)?>(null) }
    var shelfClipBoundsResolver by remember { mutableStateOf<((String) -> Rect?)?>(null) }
    var launchedFromSeriesSheet by remember { mutableStateOf(false) }
    var lastTrackedBookId by remember { mutableStateOf<String?>(null) }
    var lastTrackedTimeMs by remember { mutableLongStateOf(0L) }
    val readerExpand = remember { Animatable(if (activeReaderBookId != null) 1f else 0f) }
    val readerContentReveal = remember { Animatable(if (activeReaderBookId != null) 1f else 0f) }
    var readerExpandSettled by remember { mutableStateOf(activeReaderBookId != null) }

    LaunchedEffect(activeReaderBookId) {
        if (activeReaderBookId != null) {
            retainedReaderBookId = activeReaderBookId
            readerExpandSettled = false
            val validLaunchRect = launchSourceRect?.takeIf { it.width >= 8f && it.height >= 8f }
            val hasHeroCover = validLaunchRect != null && CoverAssetCache.getThumb(activeReaderBookId!!) != null
            if (hasHeroCover) {
                readerContentReveal.snapTo(0f)
                readerExpand.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.88f,
                        stiffness = 500f,
                        visibilityThreshold = 0.004f,
                    ),
                )
                readerExpandSettled = true
                delay(48L)
                readerContentReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 480,
                        easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
                    ),
                )
            } else {
                readerContentReveal.snapTo(0f)
                launch {
                    delay(45L)
                    readerExpandSettled = true
                    readerContentReveal.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 320,
                            easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
                        ),
                    )
                }
                readerExpand.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.90f,
                        stiffness = 470f,
                        visibilityThreshold = 0.002f,
                    ),
                )
                readerExpandSettled = true
                if (readerContentReveal.value < 1f) {
                    readerContentReveal.snapTo(1f)
                }
            }
        } else {
            readerExpandSettled = false
            val closingId = retainedReaderBookId
            val liveClosingRect = if (
                closingId != null &&
                selectedTab == 1 &&
                !showStatsDetail &&
                !showImportPage
            ) {
                shelfBookBoundsResolver?.invoke(closingId)?.takeIf { it.width >= 8f && it.height >= 8f }
            } else {
                null
            }
            if (liveClosingRect != null) {
                launchSourceRect = liveClosingRect
            }
            val validClosingRect = (liveClosingRect ?: launchSourceRect)?.takeIf { it.width >= 8f && it.height >= 8f }
            val hasHeroCover = validClosingRect != null && closingId != null && CoverAssetCache.getThumb(closingId) != null
            if (readerExpand.value > 0.002f) {
                val acceleratedSeriesClose = launchedFromSeriesSheet && selectedTab == 1 && !shelfSeriesSheetOpen
                if (hasHeroCover) {
                    // 退场：正文在前 85ms 干脆收起进入封面，随后同步非线性平滑落回原位；
                    // 若连按返回同步关闭系列弹窗，保留当前速度无跳变切换为高速弹簧收拢进叠牌
                    val fastModeFlow = snapshotFlow {
                        launchedFromSeriesSheet && selectedTab == 1 && !shelfSeriesSheetOpen
                    }.distinctUntilChanged()
                    suspend fun runTrackedCloseSpring(anim: Animatable<Float, *>) {
                        val done = CompletableDeferred<Unit>()
                        coroutineScope {
                            val watcher = launch {
                                fastModeFlow.collectLatest { fast ->
                                    if (anim.value > 0.0005f) {
                                        val carriedVelocity = anim.velocity.coerceAtMost(0f)
                                        anim.animateTo(
                                            targetValue = 0f,
                                            initialVelocity = carriedVelocity,
                                            animationSpec = spring(
                                                dampingRatio = 0.90f,
                                                stiffness = if (fast) 780f else 480f,
                                                visibilityThreshold = 0.0015f,
                                            ),
                                        )
                                    }
                                    done.complete(Unit)
                                }
                            }
                            done.await()
                            watcher.cancel()
                        }
                    }
                    coroutineScope {
                        launch {
                            if (readerContentReveal.value > 0.01f) {
                                readerContentReveal.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = if (acceleratedSeriesClose) 85 else 120,
                                        easing = FastOutSlowInEasing,
                                    ),
                                )
                            } else {
                                readerContentReveal.snapTo(0f)
                            }
                        }
                        launch { runTrackedCloseSpring(readerExpand) }
                    }
                } else {
                    readerContentReveal.snapTo(0f)
                    readerExpand.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.90f,
                            stiffness = if (acceleratedSeriesClose) 780f else 480f,
                            visibilityThreshold = 0.0015f,
                        ),
                    )
                }
            } else {
                readerContentReveal.snapTo(0f)
            }
            retainedReaderBookId = null
            launchSourceRect = null
            launchedFromSeriesSheet = false
        }
    }

    fun openBookFromTracker(book: BookEntry, sourceRect: Rect?) {
        retainedReaderBookId = book.id
        activeReaderBookId = book.id
        launchSourceRect = sourceRect
        launchedFromSeriesSheet = selectedTab == 1 && shelfSeriesSheetOpen
        lastTrackedBookId = book.id
        lastTrackedTimeMs = SystemClock.uptimeMillis()
        showStatsDetail = false
        showImportPage = false
        showAddBookMenu = false
        shelfSelectionMode = false
        settingsSubPageOpen = false
        viewModel.openBook(book)
    }

    fun openBook(book: BookEntry) {
        val now = SystemClock.uptimeMillis()
        val recentlyTracked = lastTrackedBookId == book.id && (now - lastTrackedTimeMs) in 0L..150L
        if (!recentlyTracked) {
            launchSourceRect = null
        }
        retainedReaderBookId = book.id
        activeReaderBookId = book.id
        launchedFromSeriesSheet = selectedTab == 1 && shelfSeriesSheetOpen
        showStatsDetail = false
        showImportPage = false
        showAddBookMenu = false
        shelfSelectionMode = false
        settingsSubPageOpen = false
        viewModel.openBook(book)
    }

    val activeBook = remember(books, activeReaderBookId) {
        activeReaderBookId?.let { id -> books.firstOrNull { it.id == id } }
    }
    val displayedReaderBook = remember(books, retainedReaderBookId) {
        retainedReaderBookId?.let { id -> books.firstOrNull { it.id == id } }
    }

    // 预热首页与书架全量高频书籍封面至全局 CoverAssetCache，仅在封面列表变更时并发预解码一次
    val displayBooksCoverKey = remember(displayTabBooks) {
        displayTabBooks
            .asSequence()
            .filter { !it.coverPath.isNullOrBlank() }
            .sortedBy { it.id }
            .take(160)
            .map { "${it.id}:${it.coverPath}" }
            .toList()
    }
    LaunchedEffect(displayBooksCoverKey) {
        val prewarmList = displayTabBooks
            .asSequence()
            .filter { !it.coverPath.isNullOrBlank() }
            .take(160)
            .toList()
        val extendedTargets = prewarmList.take(24)
        val thumbOnlyTargets = prewarmList.drop(24)
        if (extendedTargets.isNotEmpty()) {
            CoverAssetCache.preloadBooks(extendedTargets, includeExtendedAssets = true)
        }
        if (thumbOnlyTargets.isNotEmpty()) {
            CoverAssetCache.preloadBooks(thumbOnlyTargets, includeExtendedAssets = false)
        }
    }

    // 阅读器开启时自动每秒记录阅读时长
    LaunchedEffect(activeBook?.id) {
        val bookId = activeBook?.id ?: return@LaunchedEffect
        var accSec = 0
        while (true) {
            delay(1000L)
            accSec++
            if (accSec >= 10) {
                viewModel.recordReadingDuration(bookId, accSec)
                accSec = 0
            }
        }
    }

    val today = remember { LocalDate.now() }
    val todaySeconds = remember(displayTabDailyTotals, today) {
        displayTabDailyTotals[AppViewModel.formatDateKey(today)] ?: 0
    }
    val weekDailySeconds = remember(displayTabDailyTotals, today) {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        (0 until 7).map { i ->
            displayTabDailyTotals[AppViewModel.formatDateKey(monday.plusDays(i.toLong()))] ?: 0
        }
    }
    val weekSeconds = remember(weekDailySeconds) { weekDailySeconds.sum() }
    val monthSeconds = remember(displayTabDailyTotals, today) {
        val firstOfMonth = today.withDayOfMonth(1)
        var sum = 0
        var d = firstOfMonth
        while (!d.isAfter(today)) {
            sum += displayTabDailyTotals[AppViewModel.formatDateKey(d)] ?: 0
            d = d.plusDays(1)
        }
        sum
    }

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = palette.primary,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.text,
            onSurface = palette.text,
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.text,
            onSurface = palette.text,
        )
    }

    val baseDensity = LocalDensity.current
    val scope = rememberCoroutineScope()
    val oldSnapshotLayer = rememberGraphicsLayer()
    val revealSrcRect = remember { AndroidRect() }
    val revealDstRectF = remember { AndroidRectF() }
    val revealShaderMatrix = remember { AndroidMatrix() }
    // 零细线、宽羽化 PorterDuff.Mode.DST_OUT 径向蒙版（无 Stroke、无高对比环带）
    val revealEraserShader = remember {
        AndroidRadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(
                0xFF000000.toInt(),
                0xFF000000.toInt(),
                0x80000000.toInt(),
                0x00000000,
            ),
            floatArrayOf(0f, 0.65f, 0.84f, 1.0f),
            AndroidShader.TileMode.CLAMP,
        )
    }
    val revealEraserPaint = remember(revealEraserShader) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            shader = revealEraserShader
        }
    }
    var pendingCaptureSignal by remember { mutableStateOf<CompletableDeferred<Unit>?>(null) }
    var frozenOldSnapshotBitmap by remember { mutableStateOf<AndroidBitmap?>(null) }
    var isThemeRevealing by remember { mutableStateOf(false) }
    var revealOrigin by remember { mutableStateOf(Offset.Zero) }
    var revealFraction by remember { mutableFloatStateOf(1f) }
    var revealJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val revealTokenRef = remember { intArrayOf(0) }

    fun triggerCircularThemeToggle(originInRoot: Offset) {
        val currentStyleSnapshot = viewModel.style.value
        val nextMode = currentStyleSnapshot.nextCycleBrightnessMode(isSystemDark)
        val willFlipBrightness = currentStyleSnapshot.copy(brightnessMode = nextMode)
            .isEffectiveLight(isSystemDark) != currentStyleSnapshot.isEffectiveLight(isSystemDark)
        val myToken = ++revealTokenRef[0]
        revealJob?.cancel()
        pendingCaptureSignal?.let { if (!it.isCompleted) it.cancel() }
        pendingCaptureSignal = null
        isThemeRevealing = false
        frozenOldSnapshotBitmap = null
        revealFraction = 1f

        // Transitions that keep effective light/dark unchanged (e.g. light -> system on light device)
        // must not freeze/capture a full-screen snapshot or flash a circular reveal.
        if (!willFlipBrightness) {
            viewModel.toggleDayNight(isSystemDark)
            return
        }

        val w = view.width
        val h = view.height
        val effectiveOrigin = if (originInRoot.x > 0f && originInRoot.y > 0f) {
            originInRoot
        } else {
            Offset(w * 0.90f, h * 0.08f)
        }
        revealOrigin = effectiveOrigin
        revealFraction = 0f

        revealJob = scope.launch {
            val captureSignal = CompletableDeferred<Unit>()
            pendingCaptureSignal = captureSignal
            // 仅触发 1 次 DrawScope 录制旧主题 DisplayList，不使用 PixelCopy.request(window)
            withTimeoutOrNull(80L) {
                captureSignal.await()
            }
            if (revealTokenRef[0] != myToken) return@launch
            pendingCaptureSignal = null
            // 在切换主题前冻结为独立 GPU 位图，避免 Vitro RenderNode 原地修改导致旧画面串色
            val capturedBitmap = runCatching { oldSnapshotLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            if (revealTokenRef[0] != myToken) return@launch

            frozenOldSnapshotBitmap = capturedBitmap
            isThemeRevealing = capturedBitmap != null
            viewModel.toggleDayNight(isSystemDark)

            if (capturedBitmap == null) {
                revealFraction = 1f
                isThemeRevealing = false
                return@launch
            }
            try {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                ) { value, _ -> revealFraction = value }
            } finally {
                if (revealTokenRef[0] == myToken) {
                    isThemeRevealing = false
                    frozenOldSnapshotBitmap = null
                    revealFraction = 1f
                }
            }
        }
    }

    val buttonFadeProgress = 1f
    val rootBackdrop = rememberGlassBackdrop()
    val homeTabBackdrop = rememberGlassBackdrop()
    val shelfTabBackdrop = rememberGlassBackdrop()
    val statsTabBackdrop = rememberGlassBackdrop()
    val settingsTabBackdrop = rememberGlassBackdrop()
    val popoverHostState = rememberVitroPopoverHostState()

    val ambientHeroBook = remember(displayTabBooks) {
        AppViewModel.selectHeroBook(displayTabBooks)
    }
    val ambientSecondaryBook = remember(displayTabBooks, ambientHeroBook) {
        if (!ambientHeroBook?.coverPath.isNullOrBlank()) {
            ambientHeroBook
        } else {
            AppViewModel.sortRecentBooks(displayTabBooks)
                .firstOrNull { !it.coverPath.isNullOrBlank() }
                ?: ambientHeroBook
        }
    }
    val readerActive = activeReaderBookId != null || retainedReaderBookId != null
    val overlayCoveringRoot = readerActive ||
        showStatsDetail ||
        showImportPage ||
        (selectedTab == 1 && (shelfSeriesSheetOpen || shelfSeriesSheetRendered))
    val hideAmbientColorTransition = overlayCoveringRoot || (selectedTab == 3 && settingsSubPageOpen)
    LaunchedEffect(
        ambientHeroBook?.id,
        ambientHeroBook?.coverPath,
        ambientSecondaryBook?.id,
        ambientSecondaryBook?.coverPath,
        isSeedTransitionBusy,
        overlayCoveringRoot,
    ) {
        if (isSeedTransitionBusy || overlayCoveringRoot) return@LaunchedEffect
        delay(if (selectedTab == 0) 80L else 300L)
        ambientHeroBook?.let { CoverAssetCache.ensureLoaded(it) }
        if (ambientSecondaryBook != null && ambientSecondaryBook.id != ambientHeroBook?.id) {
            CoverAssetCache.ensureLoaded(ambientSecondaryBook)
        }
    }
    val currentAmbientHero by rememberUpdatedState(ambientHeroBook)
    val currentAmbientSecondary by rememberUpdatedState(ambientSecondaryBook)
    val currentAmbientPalette by rememberUpdatedState(palette)
    val currentAmbientGlowSource by rememberUpdatedState(style.ambientGlowSource)
    // Read CoverAssetCache.versionTick inside derivedStateOf + snapshotFlow so background cover preloads
    // never trigger full-root SQuartorRootApp recompositions.
    val targetGlowColorsState = remember {
        derivedStateOf {
            CoverAssetCache.versionTick
            val activePalette = currentAmbientPalette
            if (currentAmbientGlowSource == AmbientGlowSource.appTheme) {
                activePalette.primary to activePalette.primarySoft
            } else {
                val primaryGlow = currentAmbientHero?.let {
                    CoverAssetCache.getAmbientColor(it, 0, activePalette)
                } ?: activePalette.primary
                val secondaryGlow = currentAmbientSecondary?.let {
                    CoverAssetCache.getSecondaryAmbientColor(it, 1, activePalette)
                } ?: activePalette.primarySoft
                primaryGlow to secondaryGlow
            }
        }
    }
    var committedGlowTargets by remember { mutableStateOf(palette.primary to palette.primarySoft) }
    LaunchedEffect(hideAmbientColorTransition, selectedTab) {
        if (hideAmbientColorTransition) return@LaunchedEffect
        snapshotFlow { targetGlowColorsState.value }.collectLatest { nextTargets ->
            if (nextTargets == committedGlowTargets) return@collectLatest
            if (selectedTab != 0) {
                delay(260L)
            }
            committedGlowTargets = nextTargets
        }
    }
    val glowTweenDurationMs = if (selectedTab == 0) 480 else 220
    val animatedPrimaryGlowState = animateColorAsState(
        targetValue = committedGlowTargets.first,
        animationSpec = tween(durationMillis = glowTweenDurationMs, easing = FastOutSlowInEasing),
        label = "ambientPrimaryGlow",
    )
    val animatedSecondaryGlowState = animateColorAsState(
        targetValue = committedGlowTargets.second,
        animationSpec = tween(durationMillis = glowTweenDurationMs, easing = FastOutSlowInEasing),
        label = "ambientSecondaryGlow",
    )
    val ambientGlowColorsState = remember {
        derivedStateOf { animatedPrimaryGlowState.value to animatedSecondaryGlowState.value }
    }
    val sharedFlowSecondsState = rememberAmbientFlowSeconds(
        isActive = !overlayCoveringRoot &&
            !isPowerSave,
    )

    val heroSlotAlphaProvider = remember {
        { bookId: String ->
            val currentRetainedId = retainedReaderBookId
            val hasLaunchRect = launchSourceRect?.let { it.width >= 8f && it.height >= 8f } == true
            val hasThumb = currentRetainedId != null && CoverAssetCache.getThumb(currentRetainedId) != null
            if (currentRetainedId == null || bookId != currentRetainedId || !hasLaunchRect || !hasThumb) {
                1f
            } else {
                0f
            }
        }
    }

    val currentOpenBookFromTracker by rememberUpdatedState(::openBookFromTracker)
    val stableOpenBookFromTracker = remember {
        { book: BookEntry, sourceRect: Rect? -> currentOpenBookFromTracker(book, sourceRect) }
    }
    val currentOpenBook by rememberUpdatedState(::openBook)
    val stableOpenBook = remember {
        { book: BookEntry -> currentOpenBook(book) }
    }
    val currentToggleBrightness by rememberUpdatedState(::triggerCircularThemeToggle)
    val stableToggleBrightness = remember {
        { origin: Offset -> currentToggleBrightness(origin) }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(density = baseDensity.density, fontScale = 1.0f),
        LocalVitroBackdrop provides rootBackdrop,
        LocalVitroPopoverHost provides popoverHostState,
        LocalReadingStyle provides style,
        LocalAmbientGlowColorsState provides ambientGlowColorsState,
        LocalAmbientFlowSeconds provides sharedFlowSecondsState,
        LocalGlassDarkTheme provides !isLight,
        LocalBookLaunchTracker provides stableOpenBookFromTracker,
        LocalHeroBookSlotAlpha provides heroSlotAlphaProvider,
        LocalPowerSaveMode provides isPowerSave,
    ) {
    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val captureSignal = pendingCaptureSignal
                    if (captureSignal != null) {
                        oldSnapshotLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(oldSnapshotLayer)
                        if (!captureSignal.isCompleted) {
                            captureSignal.complete(Unit)
                        }
                    } else {
                        drawContent()
                        val snapshot = frozenOldSnapshotBitmap
                        val p = revealFraction.coerceIn(0f, 1f)
                        if (isThemeRevealing && snapshot != null && p < 0.999f) {
                            drawIntoCanvas { composeCanvas ->
                                val nc = composeCanvas.nativeCanvas
                                if (snapshot.config != AndroidBitmap.Config.HARDWARE || nc.isHardwareAccelerated) {
                                    val w = size.width
                                    val h = size.height
                                    val saveCount = nc.saveLayer(0f, 0f, w, h, null)
                                    try {
                                        revealSrcRect.set(0, 0, snapshot.width, snapshot.height)
                                        revealDstRectF.set(0f, 0f, w, h)
                                        nc.drawBitmap(snapshot, revealSrcRect, revealDstRectF, null)

                                        val startRadius = 12.dp.toPx()
                                        val maxDx = max(revealOrigin.x, w - revealOrigin.x)
                                        val maxDy = max(revealOrigin.y, h - revealOrigin.y)
                                        val endRadius = (hypot(maxDx, maxDy) / 0.65f).coerceAtLeast(startRadius + 1f)
                                        val currentRadius = startRadius + (endRadius - startRadius) * p

                                        revealShaderMatrix.setScale(currentRadius, currentRadius)
                                        revealShaderMatrix.postTranslate(revealOrigin.x, revealOrigin.y)
                                        revealEraserShader.setLocalMatrix(revealShaderMatrix)
                                        nc.drawRect(0f, 0f, w, h, revealEraserPaint)
                                    } finally {
                                        nc.restoreToCount(saveCount)
                                    }
                                }
                            }
                        }
                    }
                },
        ) {
            val rootDensity = LocalDensity.current
            val windowInfo = LocalWindowInfo.current
            val rootConfiguration = LocalConfiguration.current
            val measuredRootSizeState = remember { mutableStateOf(IntSize.Zero) }
            fun resolveRootSizePx(): Pair<Float, Float> {
                val measured = measuredRootSizeState.value
                val winSize = windowInfo.containerSize
                val effective = when {
                    measured.width > 0 && measured.height > 0 -> measured
                    winSize.width > 0 && winSize.height > 0 -> winSize
                    else -> with(rootDensity) {
                        IntSize(
                            width = rootConfiguration.screenWidthDp.dp.roundToPx().coerceAtLeast(1),
                            height = rootConfiguration.screenHeightDp.dp.roundToPx().coerceAtLeast(1),
                        )
                    }
                }
                return effective.width.toFloat().coerceAtLeast(1f) to effective.height.toFloat().coerceAtLeast(1f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { newSize ->
                        if (newSize.width > 0 && newSize.height > 0 && newSize != measuredRootSizeState.value) {
                            measuredRootSizeState.value = newSize
                        }
                    },
            ) {
                val preloadStatusTop = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
                val preloadCutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
                val preloadSafeTopDp = maxOf(preloadStatusTop, preloadCutoutTop)
                val preloadNavBottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
                val preloadSafeBottomDp = maxOf(preloadNavBottom, 12.dp)
                LaunchedEffect(activeReaderBookId) {
                    val targetId = activeReaderBookId ?: return@LaunchedEffect
                    val targetBook = books.firstOrNull { it.id == targetId } ?: return@LaunchedEffect
                    val (screenW, screenH) = resolveRootSizePx()
                    val chCount = targetBook.chapters.size.coerceAtLeast(1)
                    val chIdx = targetBook.currentChapterIndex.coerceIn(0, chCount - 1)
                    val metrics = computeReaderPaginationMetrics(
                        bookId = targetBook.id,
                        style = style,
                        palette = palette,
                        screenWidthPx = screenW.roundToInt(),
                        screenHeightPx = screenH.roundToInt(),
                        density = rootDensity.density,
                        safeTopDp = preloadSafeTopDp,
                        safeBottomDp = preloadSafeBottomDp,
                    )
                    if (ReaderPreloadCache.getPages(targetBook.id, chIdx, "${metrics.signature}#0") != null) {
                        return@LaunchedEffect
                    }
                    // Yield the first couple of vsync frames so the Hero zoom spring commits smoothly without GC/IO contention.
                    delay(24L)
                    val blocks = ReaderPreloadCache.getBlocks(targetBook.id, chIdx)
                        ?: withContext(Dispatchers.IO) {
                            readBookChapterBlocksSync(targetBook, chIdx)
                        }.also {
                            ReaderPreloadCache.putBlocks(targetBook.id, chIdx, it)
                        }
                    val chTitle = targetBook.chapters.getOrNull(chIdx)?.title ?: targetBook.title
                    val pages = withContext(Dispatchers.Default) {
                        NativeStaticLayoutPager.paginateChapter(
                            context = context,
                            chapterIndex = chIdx,
                            chapterTitle = chTitle,
                            blocks = blocks,
                            style = style,
                            contentWidthPx = metrics.contentWidthPx,
                            contentHeightPx = metrics.contentHeightPx,
                            density = rootDensity.density,
                            textColorArgb = metrics.textArgb,
                        )
                    }
                    ReaderPreloadCache.putPages(targetBook.id, chIdx, "${metrics.signature}#0", pages)
                }
                val pagerState = rememberPagerState(initialPage = selectedTab) { 4 }
                LaunchedEffect(selectedTab) {
                    val cur = pagerState.currentPage
                    if (cur != selectedTab || pagerState.targetPage != selectedTab) {
                        if (kotlin.math.abs(selectedTab - cur) > 1) {
                            val bridgePage = if (selectedTab > cur) selectedTab - 1 else selectedTab + 1
                            pagerState.scrollToPage(bridgePage)
                        }
                        pagerState.animateScrollToPage(
                            page = selectedTab,
                            animationSpec = spring(
                                dampingRatio = 0.94f,
                                stiffness = 520f,
                            ),
                        )
                    }
                }
                val readerFullyOpaqueState = remember {
                    derivedStateOf {
                        val hasActiveHeroCover = launchSourceRect?.let { it.width >= 8f && it.height >= 8f } == true &&
                            retainedReaderBookId?.let {
                                CoverAssetCache.versionTick
                                CoverAssetCache.getThumb(it) != null
                            } == true
                        val coverOpaque = if (hasActiveHeroCover) {
                            readerExpand.value >= 0.66f
                        } else {
                            readerExpand.value >= 0.999f && readerContentReveal.value >= 0.999f
                        }
                        activeReaderBookId != null &&
                            retainedReaderBookId != null &&
                            coverOpaque &&
                            !isThemeRevealing
                    }
                }
                val readerFullyOpaque by readerFullyOpaqueState
                val isHomeWaterfallActive by remember {
                    derivedStateOf {
                        selectedTab == 0 &&
                            !showStatsDetail &&
                            !showImportPage &&
                            !readerFullyOpaque &&
                            retainedReaderBookId == null
                    }
                }
                val hideDock = showStatsDetail ||
                    showImportPage ||
                    activeReaderBookId != null ||
                    retainedReaderBookId != null ||
                    (selectedTab == 1 && (shelfSeriesSheetOpen || shelfSeriesSheetRendered)) ||
                    (selectedTab == 3 && settingsSubPageOpen)
                var dockOffscreenSettled by remember { mutableStateOf(hideDock) }
                val wantImmersiveStatusBars by remember {
                    derivedStateOf {
                        activeReaderBookId != null &&
                            readerExpandSettled &&
                            readerContentReveal.value >= 0.99f
                    }
                }
                val isUiSettledForStatusBarRestore = activeReaderBookId == null &&
                    retainedReaderBookId == null &&
                    !(selectedTab == 1 && (shelfSeriesSheetOpen || shelfSeriesSheetRendered)) &&
                    !hideDock
                var didHideStatusBars by remember(view) { mutableStateOf(false) }
                LaunchedEffect(view, wantImmersiveStatusBars, isUiSettledForStatusBarRestore) {
                    val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
                    val controller = WindowCompat.getInsetsController(window, view)
                    if (wantImmersiveStatusBars && !didHideStatusBars) {
                        delay(550L)
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                        didHideStatusBars = true
                    } else if (!wantImmersiveStatusBars && didHideStatusBars && isUiSettledForStatusBarRestore) {
                        delay(340L)
                        controller.show(WindowInsetsCompat.Type.statusBars())
                        didHideStatusBars = false
                    }
                }
                DisposableEffect(view) {
                    onDispose {
                        if (didHideStatusBars) {
                            val window = (view.context as? Activity)?.window
                            if (window != null) {
                                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.statusBars())
                            }
                            didHideStatusBars = false
                        }
                    }
                }
                val isTabPagerScrolling by remember(pagerState) {
                    derivedStateOf {
                        pagerState.isScrollInProgress ||
                            pagerState.currentPage != pagerState.targetPage ||
                            kotlin.math.abs(pagerState.currentPageOffsetFraction) > 0.001f
                    }
                }
                // Tab 切换或 HorizontalPager 滑动期间必须切换到全屏 rootBackdrop：
                // 单页 Tab Backdrop 仅覆盖自身滑动区域，滑出区域外在图层中为 (0,0,0,0)，
                // Vitro AGSL 仅采样 .rgb，会导致 Dock 玻璃在切页瞬间/滑动过程中扫过黑色阴影。
                val isTabPagerTransitioning = pagerState.currentPage != selectedTab ||
                    pagerState.targetPage != selectedTab ||
                    isTabPagerScrolling
                var prewarmOffscreenTabs by remember { mutableStateOf(false) }
                val prewarmOffscreenTabsState = rememberUpdatedState(prewarmOffscreenTabs)
                val hasLoadedBooks = displayTabBooks.isNotEmpty()
                var didPrewarmWithBooks by remember { mutableStateOf(false) }
                LaunchedEffect(hasLoadedBooks) {
                    if (didPrewarmWithBooks) return@LaunchedEffect
                    // Wait for Tab 0 first frames to commit cleanly, then silently pre-warm offscreen
                    // tabs (Shelf/Stats/Settings) once books are ready so the very first tab switch
                    // after cold start hits warm book grids, stats charts, RenderNodes, and JIT code.
                    withFrameNanos { }
                    withFrameNanos { }
                    delay(if (hasLoadedBooks) 160L else 420L)
                    if (activeReaderBookId == null && !showStatsDetail && !showImportPage) {
                        prewarmOffscreenTabs = true
                        withFrameNanos { }
                        withFrameNanos { }
                        prewarmOffscreenTabs = false
                    }
                    if (hasLoadedBooks) {
                        didPrewarmWithBooks = true
                    }
                }
                val dockOffscreenHidden = hideDock && dockOffscreenSettled
                val dockOffscreenHiddenState = rememberUpdatedState(dockOffscreenHidden)
                // Keep ONE persistent rootBackdrop & Dock backdrop across all 4 tabs and transitions
                // so Dock glass never detaches/rebuilds drawBackdrops or triggers root layer churn.
                val needRootBackdrop = !isPowerSave && !readerFullyOpaque
                val requireTabDockBackdrop = false
                val activeDockBackdrop = if (isPowerSave || readerFullyOpaque) null else rootBackdrop
                // Suspend Shelf top Chrome glass blur ONLY while opaque Reader completely covers the screen;
                // Dock never suspends glass blur so it stays permanent translucent Liquid Glass at all times.
                val tab1SuspendChromeBlurState = readerFullyOpaqueState
                CompositionLocalProvider(
                    LocalDockOffscreenHiddenState provides dockOffscreenHiddenState,
                    LocalRootHandlesBottomAmbient provides true,
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            if (!readerFullyOpaque) {
                                drawContent()
                            }
                        },
                ) {
                    IsolatedAmbientGlassContainer(
                        backdrop = if (needRootBackdrop) rootBackdrop else null,
                        paletteBackgroundColor = palette.background,
                        skipCornerGlowAndIsolation = false,
                        transparentBottomOnPagerTransition = false,
                        renderAmbientBackground = !readerFullyOpaque,
                        ambientBackground = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(palette.background),
                            ) {
                                GeminiCornerAmbientCanvas(
                                    palette = palette,
                                    primaryColorProvider = { ambientGlowColorsState.value.first },
                                    flowTimeProvider = { sharedFlowSecondsState.floatValue },
                                    topLimitFraction = 0.58f,
                                )
                            }
                        },
                    ) {

                    // 4-Tab 常驻预热：各 Tab 空闲态均已按需解绑 .glassBackdrop 并冻结离屏页数据快照，防止后台页随最近阅读变化重组/动画
                val visibleTabMin by remember {
                    derivedStateOf {
                        kotlin.math.floor((pagerState.currentPage + pagerState.currentPageOffsetFraction).toDouble()).toInt()
                    }
                }
                val visibleTabMax by remember {
                    derivedStateOf {
                        kotlin.math.ceil((pagerState.currentPage + pagerState.currentPageOffsetFraction).toDouble()).toInt()
                    }
                }
                val latestBooks by rememberUpdatedState(displayTabBooks)
                val latestShelves by rememberUpdatedState(shelves)
                val latestTodaySeconds by rememberUpdatedState(todaySeconds)
                val latestWeekSeconds by rememberUpdatedState(weekSeconds)
                val latestMonthSeconds by rememberUpdatedState(monthSeconds)
                val latestWeekDailySeconds by rememberUpdatedState(weekDailySeconds)
                val latestDailyTotals by rememberUpdatedState(displayTabDailyTotals)
                val latestStats by rememberUpdatedState(displayTabStats)
                val latestPalette by rememberUpdatedState(palette)
                val latestStyle by rememberUpdatedState(style)
                val latestFonts by rememberUpdatedState(fonts)
                val latestCloudSync by rememberUpdatedState(cloudSync)
                val latestStorageStats by rememberUpdatedState(storageStats)
                val latestIsSyncing by rememberUpdatedState(isSyncing)
                val latestSyncFeedbackMessage by rememberUpdatedState(syncFeedbackMessage)
                val latestReaderFullyOpaque by rememberUpdatedState(readerFullyOpaque)
                val latestShowStatsDetail by rememberUpdatedState(showStatsDetail)
                val latestShowImportPage by rememberUpdatedState(showImportPage)
                val latestIsHomeWaterfallActive by rememberUpdatedState(isHomeWaterfallActive)
                val latestRequireTabDockBackdrop by rememberUpdatedState(requireTabDockBackdrop)
                val latestSelectedTab by rememberUpdatedState(selectedTab)
                val latestButtonFadeProgress by rememberUpdatedState(buttonFadeProgress)
                val latestLaunchedFromSeriesSheet by rememberUpdatedState(launchedFromSeriesSheet)
                val latestRetainedReaderBookId by rememberUpdatedState(retainedReaderBookId)
                val latestActiveReaderBookId by rememberUpdatedState(activeReaderBookId)
                val latestShelfEnterBatchToken by rememberUpdatedState(shelfEnterBatchToken)
                val latestPickBookFiles by rememberUpdatedState(onPickBookFiles)
                val latestPickBookFolder by rememberUpdatedState(onPickBookFolder)
                val latestPickFontFile by rememberUpdatedState(onPickFontFile)

                val tab0LastData = remember { arrayOfNulls<HomeTabSnapshot>(1) }
                val tab0RenderState = remember {
                    derivedStateOf {
                        val active = 0 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 0 ||
                            pagerState.targetPage == 0
                        val prev = tab0LastData[0]
                        if (!active && prev != null && !prev.isVisible && !prev.isActive) {
                            prev
                        } else {
                            HomeTabSnapshot(
                                books = if (!active && prev != null) prev.books else latestBooks,
                                todaySeconds = if (!active && prev != null) prev.todaySeconds else latestTodaySeconds,
                                weekSeconds = if (!active && prev != null) prev.weekSeconds else latestWeekSeconds,
                                monthSeconds = if (!active && prev != null) prev.monthSeconds else latestMonthSeconds,
                                weekDailySeconds = if (!active && prev != null) prev.weekDailySeconds else latestWeekDailySeconds,
                                palette = if (!active && prev != null) prev.palette else latestPalette,
                                themeRevealButtonProgress = if (!active && prev != null) prev.themeRevealButtonProgress else latestButtonFadeProgress,
                                isActive = active && latestIsHomeWaterfallActive,
                                isVisible = active &&
                                    !latestReaderFullyOpaque &&
                                    !latestShowStatsDetail &&
                                    !latestShowImportPage,
                            ).also { tab0LastData[0] = it }
                        }
                    }
                }
                val tab2LastData = remember { arrayOfNulls<StatsTabSnapshot>(1) }
                val tab2RenderState = remember {
                    derivedStateOf {
                        val active = 2 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 2 ||
                            pagerState.targetPage == 2
                        val prev = tab2LastData[0]
                        if (!active && prev != null && !prev.isVisible && !prev.requireDockBackdrop) {
                            prev
                        } else {
                            StatsTabSnapshot(
                                books = if (!active && prev != null) prev.books else latestBooks,
                                dailyTotals = if (!active && prev != null) prev.dailyTotals else latestDailyTotals,
                                bookStats = if (!active && prev != null) prev.bookStats else latestStats,
                                palette = if (!active && prev != null) prev.palette else latestPalette,
                                requireDockBackdrop = false,
                                isVisible = active &&
                                    !latestReaderFullyOpaque &&
                                    !latestShowStatsDetail &&
                                    !latestShowImportPage,
                            ).also { tab2LastData[0] = it }
                        }
                    }
                }
                val tab3LastData = remember { arrayOfNulls<SettingsTabSnapshot>(1) }
                val tab3RenderState = remember {
                    derivedStateOf {
                        val active = 3 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 3 ||
                            pagerState.targetPage == 3
                        val prev = tab3LastData[0]
                        if (!active && prev != null && !prev.isVisible && !prev.requireDockBackdrop) {
                            prev
                        } else {
                            SettingsTabSnapshot(
                                style = if (!active && prev != null) prev.style else latestStyle,
                                fonts = if (!active && prev != null) prev.fonts else latestFonts,
                                cloudSync = if (!active && prev != null) prev.cloudSync else latestCloudSync,
                                storageStats = if (!active && prev != null) prev.storageStats else latestStorageStats,
                                palette = if (!active && prev != null) prev.palette else latestPalette,
                                isSyncing = if (!active && prev != null) prev.isSyncing else latestIsSyncing,
                                syncFeedbackMessage = if (!active && prev != null) prev.syncFeedbackMessage else latestSyncFeedbackMessage,
                                themeRevealButtonProgress = if (!active && prev != null) prev.themeRevealButtonProgress else latestButtonFadeProgress,
                                requireDockBackdrop = false,
                                isVisible = active &&
                                    !latestReaderFullyOpaque &&
                                    !latestShowStatsDetail &&
                                    !latestShowImportPage,
                            ).also { tab3LastData[0] = it }
                        }
                    }
                }
                val tab1LastData = remember { arrayOfNulls<ShelfTabSnapshot>(1) }
                val tab1RenderState = remember {
                    derivedStateOf {
                        val active = 1 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 1 ||
                            pagerState.targetPage == 1
                        val prev = tab1LastData[0]
                        if (!active && prev != null && !prev.isActive) {
                            prev
                        } else {
                            ShelfTabSnapshot(
                                books = if (!active && prev != null) prev.books else latestBooks,
                                shelves = if (!active && prev != null) prev.shelves else latestShelves,
                                palette = if (!active && prev != null) prev.palette else latestPalette,
                                externalEnterBatchToken = if (!active && prev != null) prev.externalEnterBatchToken else latestShelfEnterBatchToken,
                                isActive = active &&
                                    latestSelectedTab == 1 &&
                                    !latestShowStatsDetail &&
                                    !latestShowImportPage,
                            ).also { tab1LastData[0] = it }
                        }
                    }
                }
                val tab1AccelerateSeriesCloseState = remember {
                    derivedStateOf {
                        val active = 1 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 1 ||
                            pagerState.targetPage == 1
                        active &&
                            launchedFromSeriesSheet &&
                            retainedReaderBookId != null &&
                            activeReaderBookId == null
                    }
                }
                val tab1IsReaderOverlayActiveState = remember {
                    derivedStateOf {
                        val active = 1 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 1 ||
                            pagerState.targetPage == 1
                        active && retainedReaderBookId != null
                    }
                }
                val tab1IsVisibleState = remember {
                    derivedStateOf {
                        val active = 1 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 1 ||
                            pagerState.targetPage == 1
                        active &&
                            !latestShowStatsDetail &&
                            !latestShowImportPage
                    }
                }
                val tab1RequireDockBackdropState = remember {
                    derivedStateOf {
                        val active = 1 in visibleTabMin..visibleTabMax ||
                            latestSelectedTab == 1 ||
                            pagerState.targetPage == 1
                        val transitioningNow = pagerState.currentPage != selectedTab ||
                            pagerState.targetPage != selectedTab ||
                            isTabPagerScrolling
                        val needOverlayRootNow = createShelfDockRendered ||
                            transitioningNow
                        val needRootNow = !isPowerSave &&
                            needOverlayRootNow
                        val requireTabDockNow = !isPowerSave &&
                            !needRootNow
                        active && requireTabDockNow && selectedTab == 1
                    }
                }

                var isRecentRowInteracting by remember { mutableStateOf(false) }
                val isRecentRowInteractingState = rememberUpdatedState(isRecentRowInteracting)
                val isTabPagerTransitioningState = rememberUpdatedState(isTabPagerTransitioning)
                val homeTabCallbacks = remember(stableOpenBook, stableToggleBrightness) {
                    HomeTabCallbacks(
                        onOpenBook = stableOpenBook,
                        onOpenShelf = { selectedTab = 1 },
                        onOpenStats = {},
                        onToggleBrightness = stableToggleBrightness,
                        onScrolledOverDockChanged = {},
                        onRecentRowDragLockChanged = { isRecentRowInteracting = it },
                    )
                }
                val statsTabCallbacks = remember(stableOpenBook) {
                    StatsTabCallbacks(
                        onOpenBook = stableOpenBook,
                        onOpenDetail = { showStatsDetail = true },
                    )
                }
                val settingsTabCallbacks = remember(viewModel, stableToggleBrightness) {
                    SettingsTabCallbacks(
                        onDismissSyncFeedback = viewModel::clearSyncFeedback,
                        onUpdateStyle = viewModel::updateStyle,
                        onToggleBrightness = stableToggleBrightness,
                        onImportFontClick = { latestPickFontFile() },
                        onDeleteFont = viewModel::deleteFont,
                        onUpdateCloudSync = viewModel::updateCloudSyncSettings,
                        onSyncUpload = viewModel::syncToCloud,
                        onSyncDownload = viewModel::syncFromCloud,
                        onClearCache = viewModel::clearCache,
                        onSubPageActiveChanged = { settingsSubPageOpen = it },
                    )
                }
                val shelfTabCallbacks = remember(viewModel, stableOpenBook) {
                    ShelfTabCallbacks(
                        onOpenBook = stableOpenBook,
                        onImportFilesClick = { latestPickBookFiles() },
                        onImportFolderClick = { latestPickBookFolder() },
                        onDeleteBooks = viewModel::deleteBooks,
                        onMoveBooksToShelf = viewModel::moveBooksToShelf,
                        onGroupSeries = viewModel::groupBooksAsSeries,
                        onRemoveFromSeries = viewModel::removeBookFromSeries,
                        onUpdateBookMeta = { id, title, author ->
                            viewModel.updateBookMeta(id, title, author, null)
                        },
                        onAddShelf = viewModel::addShelf,
                        onDeleteShelf = viewModel::deleteShelf,
                        onSelectionModeChanged = { shelfSelectionMode = it },
                        onBatchStateChanged = { active, allSelected, hasSelection ->
                            shelfSelectionMode = active
                            shelfBatchAllSelected = allSelected
                            shelfBatchHasSelection = hasSelection
                        },
                        onRegisterBatchActions = { actions -> shelfBatchActions = actions },
                        onSeriesSheetOpenChanged = { shelfSeriesSheetOpen = it },
                        onSeriesSheetRenderedChanged = { shelfSeriesSheetRendered = it },
                        onRegisterSeriesDismiss = { dismiss -> shelfDismissSeriesRef[0] = dismiss },
                        onRegisterBoundsResolver = { resolver -> shelfBookBoundsResolver = resolver },
                        onRegisterClosingAlphaResolver = { resolver -> shelfClosingAlphaResolver = resolver },
                        onRegisterClipBoundsResolver = { resolver -> shelfClipBoundsResolver = resolver },
                    )
                }
                val shouldDrawTabProvider = remember {
                    { tab: Int ->
                        !readerFullyOpaqueState.value && (
                            prewarmOffscreenTabsState.value ||
                                tab == selectedTab ||
                                tab == pagerState.currentPage ||
                                tab == pagerState.targetPage ||
                                tab in visibleTabMin..visibleTabMax
                            )
                    }
                }
                MainTabsPagerHost(
                    pagerState = pagerState,
                    isRecentRowInteractingState = isRecentRowInteractingState,
                    isTabPagerTransitioningState = isTabPagerTransitioningState,
                    shouldDrawTabProvider = shouldDrawTabProvider,
                    tab0RenderState = tab0RenderState,
                    tab1RenderState = tab1RenderState,
                    tab1AccelerateSeriesCloseState = tab1AccelerateSeriesCloseState,
                    tab1IsReaderOverlayActiveState = tab1IsReaderOverlayActiveState,
                    tab1IsVisibleState = tab1IsVisibleState,
                    tab1RequireDockBackdropState = tab1RequireDockBackdropState,
                    tab1SuspendChromeBlurState = tab1SuspendChromeBlurState,
                    tab2RenderState = tab2RenderState,
                    tab3RenderState = tab3RenderState,
                    homeTabCallbacks = homeTabCallbacks,
                    shelfTabCallbacks = shelfTabCallbacks,
                    statsTabCallbacks = statsTabCallbacks,
                    settingsTabCallbacks = settingsTabCallbacks,
                    homeTabBackdrop = homeTabBackdrop,
                    shelfTabBackdrop = shelfTabBackdrop,
                    statsTabBackdrop = statsTabBackdrop,
                    settingsTabBackdrop = settingsTabBackdrop,
                )
                }
                }
                } // 闭合 rootBackdrop 容器，确保二级详情页在其之上且不共享 isInside 玻璃层序

                // 统计详情二级页（StatsDetailScreen 内部自行作用域化 detailBackdrop，避免顶部悬浮药丸自引用）
                AnimatedVisibility(
                    visible = showStatsDetail,
                    enter = slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = 380f),
                        initialOffsetX = { it },
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f),
                        targetOffsetX = { it },
                    ),
                ) {
                    BackHandler { showStatsDetail = false }
                    val detailBackdrop = rememberGlassBackdrop()
                    CompositionLocalProvider(
                        LocalVitroBackdrop provides if (isPowerSave) null else detailBackdrop,
                    ) {
                        StatsDetailScreen(
                            books = displayTabBooks,
                            dailyTotals = displayTabDailyTotals,
                            bookStats = displayTabStats,
                            palette = palette,
                            onBack = { showStatsDetail = false },
                            onOpenBook = stableOpenBook,
                        )
                    }
                }

                // 添加书籍二级页（全屏 iOS 27 液态玻璃导入工作台：多书选择 / 文件夹递归导入 / 快速建架）
                AnimatedVisibility(
                    visible = showImportPage,
                    enter = slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 400f),
                        initialOffsetX = { it },
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 440f),
                        targetOffsetX = { it },
                    ),
                ) {
                    BackHandler { showImportPage = false }
                    ImportBooksScreen(
                        palette = palette,
                        totalBooks = displayTabBooks.size,
                        shelfCount = shelves.size,
                        importActivity = importActivity,
                        onPickFiles = onPickBookFiles,
                        onPickFolder = onPickBookFolder,
                        onAddShelf = viewModel::addShelf,
                        onGoToShelf = {
                            showImportPage = false
                            selectedTab = 1
                        },
                        onBack = { showImportPage = false },
                    )
                }

            // 底部 Vitro 0.1.0 液态玻璃 Dock + 全页面统一的「添加书籍」悬浮弹出菜单
            val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
            val dockBottom = (navBarBottom.value * 0.65f + 16f).coerceIn(24f, 32f).dp
            val isShelfBatchDockActive = selectedTab == 1 &&
                shelfSelectionMode &&
                !showStatsDetail &&
                !showImportPage &&
                activeReaderBookId == null
            LaunchedEffect(hideDock, isShelfBatchDockActive) {
                if (hideDock || isShelfBatchDockActive) {
                    showAddBookMenu = false
                }
            }
            val dockContextualAction = remember(addPopoverPresenting) {
                DockContextualAction(
                    icon = Icons.Rounded.Add,
                    contentDescription = "添加书籍",
                    onClick = { showAddBookMenu = !showAddBookMenu },
                    onAnchorBoundsChanged = { rect -> addDockOrbBounds = rect },
                    hideOrb = addPopoverPresenting,
                    orbVisibilityProvider = { addOrbVisibility },
                )
            }
            FlClashNavigationDock(
                selectedIndex = selectedTab,
                palette = palette,
                hidden = hideDock,
                onSelected = { next ->
                    showStatsDetail = false
                    showImportPage = false
                    showAddBookMenu = false
                    shelfSelectionMode = false
                    shelfSeriesSheetOpen = false
                    shelfSeriesSheetRendered = false
                    settingsSubPageOpen = false
                    selectedTab = next
                    if (next == 3) {
                        viewModel.refreshStorageStats()
                    }
                },
                contextualAction = dockContextualAction,
                onOffscreenSettledChanged = { dockOffscreenSettled = it },
                backdrop = activeDockBackdrop,
                batchMode = isShelfBatchDockActive,
                batchAllSelected = shelfBatchAllSelected,
                batchHasSelection = shelfBatchHasSelection,
                onBatchSelectAll = { shelfBatchActions?.onSelectAll?.invoke() },
                onBatchMoveToShelf = { shelfBatchActions?.onMoveToShelf?.invoke() },
                onBatchMergeSeries = { shelfBatchActions?.onMergeSeries?.invoke() },
                onBatchDelete = { shelfBatchActions?.onDelete?.invoke() },
                onBatchExit = { shelfBatchActions?.onExit?.invoke() },
                importInProgress = importInProgress,
                importProgress = importProgress,
                importJustCompleted = importJustCompleted,
                unmountWhenOffscreen = false,
                suspendGlassBlur = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 6.dp, end = 6.dp, bottom = dockBottom),
            )

            // 根级 Vitro Popover 叠加宿主：位于 .glassBackdrop(rootBackdrop) 容器之外，杜绝自引用模糊与文字被 pane 遮盖
            VitroPopoverOverlayHost(
                hostState = popoverHostState,
                fallbackBackdrop = activeDockBackdrop,
            )

            // 全页面统一的「添加书籍」Vitro 弹性玻璃二级菜单（与书架右上角菜单同源，从 Dock `+` 球丝滑弹出）
            VitroGlassPopoverMenu(
                visible = showAddBookMenu && !hideDock && !isShelfBatchDockActive,
                onDismiss = { showAddBookMenu = false },
                anchorBoundsInRoot = addDockOrbBounds,
                palette = palette,
                anchorIcon = Icons.Rounded.Add,
                anchorIconSize = 23.dp,
                anchorCornerDp = 29.dp,
                width = 218.dp,
                backdrop = activeDockBackdrop,
                onPresentingChanged = { addPopoverPresenting = it },
                onOrbVisibilityChanged = { addOrbVisibility = it },
                items = listOf(
                    VitroGlassPopoverItem(
                        label = "批量选择",
                        icon = Icons.Rounded.SelectAll,
                        onClick = {
                            showStatsDetail = false
                            showImportPage = false
                            settingsSubPageOpen = false
                            shelfSeriesSheetOpen = false
                            shelfSeriesSheetRendered = false
                            selectedTab = 1
                            shelfEnterBatchToken++
                        },
                    ),
                    VitroGlassPopoverItem(
                        label = "扫描导入文件夹",
                        icon = Icons.Rounded.CreateNewFolder,
                        onClick = onPickBookFolder,
                    ),
                    VitroGlassPopoverItem(
                        label = "新建书架分组",
                        icon = Icons.Rounded.BookmarkAdd,
                        onClick = { showCreateShelfFromDock = true },
                    ),
                ),
            )

            if (createShelfDockRendered) {
                var dockShelfNameInput by remember { mutableStateOf("") }
                LaunchedEffect(showCreateShelfFromDock) {
                    if (showCreateShelfFromDock) {
                        dockShelfNameInput = ""
                    }
                }
                BackHandler(enabled = showCreateShelfFromDock) {
                    showCreateShelfFromDock = false
                }
                val createShelfDockTransition = updateTransition(
                    transitionState = createShelfDockVisibleState,
                    label = "dockCreateShelfTransition",
                )
                val modalScaleProgress by createShelfDockTransition.animateFloat(
                    transitionSpec = {
                        if (targetState) {
                            spring(dampingRatio = 0.78f, stiffness = 440f)
                        } else {
                            spring(dampingRatio = 0.96f, stiffness = 560f)
                        }
                    },
                    label = "dockCreateShelfScale",
                ) { shown -> if (shown) 1f else 0f }
                val modalAlpha by createShelfDockTransition.animateFloat(
                    transitionSpec = {
                        tween(durationMillis = if (targetState) 180 else 140)
                    },
                    label = "dockCreateShelfAlpha",
                ) { shown -> if (shown) 1f else 0f }
                val accentOnGlass = if (isLight) {
                    lerpColor(palette.accentText, Color.Black, 0.20f)
                } else {
                    lerpColor(palette.accentText, Color.White, 0.12f)
                }
                val scrimInteraction = remember { MutableInteractionSource() }
                val cardInteraction = remember { MutableInteractionSource() }
                val fieldBgColor = if (isLight) {
                    Color.Black.copy(alpha = 0.06f)
                } else {
                    Color.White.copy(alpha = 0.09f)
                }
                val canConfirmCreate = dockShelfNameInput.trim().isNotEmpty()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(40f)
                        .background(
                            Color.Black.copy(alpha = (if (isLight) 0.24f else 0.44f) * modalAlpha.coerceIn(0f, 1f)),
                        )
                        .clickable(
                            interactionSource = scrimInteraction,
                            indication = null,
                        ) {
                            showCreateShelfFromDock = false
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
                                style = style,
                                variant = Ios27GlassVariant.ReaderSheet,
                                backdrop = if (needRootBackdrop) rootBackdrop else null,
                                isInteractive = false,
                                showContrastBorder = false,
                            )
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                    ) {
                        CompositionLocalProvider(
                            LocalVitroBackdrop provides null,
                            LocalGlassDarkTheme provides !isLight,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "新建书架分组",
                                    color = palette.text,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(fieldBgColor)
                                        .padding(horizontal = 14.dp, vertical = 13.dp),
                                ) {
                                    if (dockShelfNameInput.isEmpty()) {
                                        Text(
                                            text = "例如：轻小说、科幻、已完结",
                                            color = palette.muted.copy(alpha = 0.72f),
                                            fontSize = 14.5.sp,
                                        )
                                    }
                                    BasicTextField(
                                        value = dockShelfNameInput,
                                        onValueChange = { dockShelfNameInput = it },
                                        singleLine = true,
                                        cursorBrush = SolidColor(accentOnGlass),
                                        textStyle = TextStyle(
                                            color = palette.text,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(fieldBgColor)
                                            .clickable { showCreateShelfFromDock = false }
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "取消",
                                            color = palette.text.copy(alpha = 0.82f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (canConfirmCreate) {
                                                    palette.primary.copy(alpha = if (isLight) 0.92f else 0.88f)
                                                } else {
                                                    palette.primary.copy(alpha = 0.28f)
                                                },
                                            )
                                            .clickable(enabled = canConfirmCreate) {
                                                val trimmed = dockShelfNameInput.trim()
                                                if (trimmed.isNotEmpty()) {
                                                    viewModel.addShelf(trimmed)
                                                    selectedTab = 1
                                                }
                                                showCreateShelfFromDock = false
                                            }
                                            .padding(horizontal = 20.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "创建",
                                            color = Color.White.copy(alpha = if (canConfirmCreate) 1f else 0.6f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 错误提示横幅
            errorMessage?.let { msg ->
                val topPad = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 20.dp, end = 20.dp, top = topPad + 10.dp)
                        .fillMaxWidth()
                        .ios27GlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            palette = palette,
                            style = style,
                            variant = Ios27GlassVariant.Chrome,
                            backdrop = if (needRootBackdrop) rootBackdrop else null,
                            tintOverride = Color(0xFFFF3B30).copy(alpha = 0.14f),
                            showContrastBorder = false,
                        )
                        .clickable { viewModel.clearError() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = palette.accentText,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = msg,
                        color = palette.text,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = palette.muted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // 原生 120Hz 阅读器全屏层（零重组 Apple Books 式封面共享边界弹性放大/收回过渡）
            val readerBook = remember(
                displayedReaderBook?.id,
                displayedReaderBook?.bookmarks,
                displayedReaderBook?.title,
                displayedReaderBook?.chapters?.size,
                displayedReaderBook?.sourcePath,
            ) {
                displayedReaderBook
            }
            if (readerBook != null) {
                val (screenW, screenH) = resolveRootSizePx()
                BackHandler(enabled = retainedReaderBookId != null) {
                    val currentActiveId = activeReaderBookId
                    if (currentActiveId != null) {
                        viewModel.closeActiveBook(currentActiveId)
                        activeReaderBookId = null
                    } else if (launchedFromSeriesSheet && selectedTab == 1 && shelfSeriesSheetOpen) {
                        shelfDismissSeriesRef[0]?.invoke()
                    }
                }
                val srcRect = launchSourceRect?.takeIf { it.width >= 8f && it.height >= 8f }?.let { r ->
                    Rect(
                        left = r.left.coerceIn(-screenW * 0.2f, screenW * 0.95f),
                        top = r.top.coerceIn(-screenH * 0.2f, screenH * 0.95f),
                        right = r.right.coerceIn(r.left + 8f, screenW * 1.2f),
                        bottom = r.bottom.coerceIn(r.top + 8f, screenH * 1.2f),
                    )
                }
                val startRect = srcRect ?: Rect(
                    left = screenW * 0.13f,
                    top = screenH * 0.13f,
                    right = screenW * 0.87f,
                    bottom = screenH * 0.87f,
                )
                val heroFromShelfTile = selectedTab == 1 && !showStatsDetail && !showImportPage
                val startCornerPx = with(rootDensity) {
                    if (srcRect != null) {
                        if (heroFromShelfTile) {
                            // 书架主网格 / SeriesModal 卷册卡片统一 12.dp 圆角：Hero 落地圆角逐像素对齐
                            12.dp.toPx()
                        } else {
                            val wDp = srcRect.width.toDp().value
                            (wDp * 0.112f).coerceIn(7f, 13f).dp.toPx()
                        }
                    } else {
                        28.dp.toPx()
                    }
                }
                val baseScale = (startRect.width / screenW).coerceIn(0.28f, 0.94f)
                val dynamicBoundsShape = remember(startRect, screenW, screenH, startCornerPx) {
                    DynamicReaderBoundsShape(
                        startRect = startRect,
                        screenW = screenW,
                        screenH = screenH,
                        startCornerPx = startCornerPx,
                        progressProvider = { readerExpand.value },
                    )
                }

                val coverThumb = remember(readerBook.id, CoverAssetCache.versionTick) {
                    CoverAssetCache.getThumb(readerBook.id)
                }
                val statusBarTopPx = with(rootDensity) {
                    WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding().toPx()
                }
                val navBarBottomPx = with(rootDensity) {
                    WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding().toPx()
                }
                val coverAmbient = remember(readerBook.id, palette) {
                    CoverAssetCache.getAmbientColor(readerBook, 0, palette)
                }
                val readerPaperBg = remember(style.readerBackground, style.autoNightPaper, palette) {
                    if (style.readerBackground == ReaderBackgroundId.theme) {
                        palette.background
                    } else {
                        val base = style.readerPalette
                        if (style.autoNightPaper && !palette.isLight &&
                            (base.id == ReaderBackgroundId.paper || base.id == ReaderBackgroundId.green)
                        ) {
                            readerPalettes[ReaderBackgroundId.warm]?.background ?: base.background
                        } else {
                            base.background
                        }
                    }
                }

                val hasHeroCoverOverlay = srcRect != null && coverThumb != null
                if (hasHeroCoverOverlay) {
                    // Apple Books 风格（图2）：居中完整无裁切封面展示台 + 封面取色氛围光背景 + 一镜到底正文柔和溶解过渡
                    val bmpW = coverThumb.width.toFloat().coerceAtLeast(1f)
                    val bmpH = coverThumb.height.toFloat().coerceAtLeast(1f)
                    val coverAspect = (bmpW / bmpH).coerceIn(0.42f, 1.6f)
                    val hPadPx = with(rootDensity) { 22.dp.toPx() }
                    val topPadPx = statusBarTopPx + with(rootDensity) { 38.dp.toPx() }
                    val bottomPadPx = navBarBottomPx + with(rootDensity) { 38.dp.toPx() }
                    val availW = (screenW - hPadPx * 2f).coerceAtLeast(120f)
                    val availH = (screenH - topPadPx - bottomPadPx).coerceAtLeast(180f)
                    val rawFitW = minOf(availW, availH * coverAspect)
                    val rawFitH = rawFitW / coverAspect
                    val fitWPx = rawFitW.roundToInt().coerceAtLeast(1)
                    val fitHPx = rawFitH.roundToInt().coerceAtLeast(1)
                    val fitW = fitWPx.toFloat()
                    val fitH = fitHPx.toFloat()
                    val fitLeft = (screenW - fitW) * 0.5f
                    val fitTop = topPadPx + (availH - fitH) * 0.5f
                    val showcaseEndCornerPx = with(rootDensity) { 6.dp.toPx() }
                    val showcaseBaseColor = if (isLight) {
                        lerpColor(Color(0xFFF2F3F6), coverAmbient, 0.08f)
                    } else {
                        Color(0xFF0E1015)
                    }
                    val glowAlpha = if (isLight) 0.16f else 0.24f
                    val showcaseGlowBrush = remember(screenW, fitTop, fitW, fitH, coverAmbient, glowAlpha) {
                        Brush.radialGradient(
                            0.0f to coverAmbient.copy(alpha = glowAlpha),
                            0.58f to coverAmbient.copy(alpha = glowAlpha * 0.32f),
                            1.0f to Color.Transparent,
                            center = Offset(screenW * 0.5f, fitTop + fitH * 0.5f),
                            radius = maxOf(fitW, fitH) * 0.95f,
                        )
                    }
                    val resolveLiveStartRect: () -> Rect = {
                        val closingId = retainedReaderBookId
                        if (
                            activeReaderBookId == null &&
                            closingId != null &&
                            selectedTab == 1 &&
                            !showStatsDetail &&
                            !showImportPage
                        ) {
                            shelfBookBoundsResolver?.invoke(closingId)
                                ?.takeIf { it.width >= 8f && it.height >= 8f }
                                ?: startRect
                        } else {
                            startRect
                        }
                    }
                    val showcaseCornerShape = remember(startRect, fitW, fitH, startCornerPx, showcaseEndCornerPx) {
                        DynamicShowcaseCropShape(
                            startWidth = startRect.width,
                            startHeight = startRect.height,
                            fitWidth = fitW,
                            fitHeight = fitH,
                            startCornerPx = startCornerPx,
                            endCornerPx = showcaseEndCornerPx,
                            progressProvider = { readerExpand.value },
                            startRectProvider = resolveLiveStartRect,
                        )
                    }
                    val fitWidthDp = with(rootDensity) { fitWPx.toDp() }
                    val fitHeightDp = with(rootDensity) { fitHPx.toDp() }
                    val closeShowcaseCornerShape = remember(startRect, fitW, fitH, screenW, screenH, startCornerPx) {
                        DynamicShowcaseCropShape(
                            startWidth = startRect.width,
                            startHeight = startRect.height,
                            fitWidth = fitW,
                            fitHeight = fitH,
                            startCornerPx = startCornerPx,
                            endCornerPx = 0f,
                            progressProvider = { readerExpand.value },
                            targetWidth = screenW,
                            targetHeight = screenH,
                            startRectProvider = resolveLiveStartRect,
                        )
                    }
                    val closeReaderCornerShape = remember(startRect, screenW, screenH, startCornerPx) {
                        DynamicShowcaseCornerShape(
                            startWidth = startRect.width,
                            startHeight = startRect.height,
                            fitWidth = screenW,
                            fitHeight = screenH,
                            startCornerPx = startCornerPx,
                            endCornerPx = 0f,
                            progressProvider = { readerExpand.value },
                            startRectProvider = resolveLiveStartRect,
                        )
                    }

                    // 标记本轮阅读会话是否已经真正挂载过正文阅读器：
                    // - 若已挂载，则退场全程保持挂载（仅在 revealT <= 0.002f 跳过绘制），避免 85ms 节点销毁打断缩放帧率；
                    // - 同时退场时 Hero 封面与 ReaderScreen 共用全屏 (0,0,screenW,screenH) -> startRect 同一边界盒，彻底消除阶跃突变。
                    var mountedReaderForSession by remember(readerBook.id) {
                        mutableStateOf(
                            activeReaderBookId != null &&
                                (readerExpandSettled || readerContentReveal.value > 0.001f),
                        )
                    }
                    if (activeReaderBookId != null &&
                        (readerExpandSettled || readerContentReveal.value > 0.001f)
                    ) {
                        mountedReaderForSession = true
                    }

                    // [0] Apple Books 展示台背景层：仅在开书期（或开书尚未进入正文即撤销）承接展示；直接 drawWithContent 零 saveLayer 1080x2400
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f)
                            .drawWithContent {
                                val t = readerExpand.value.coerceIn(0f, 1f)
                                val drawAlpha = if (activeReaderBookId != null || !mountedReaderForSession) {
                                    smoothstep01(t / 0.65f)
                                } else {
                                    0f
                                }
                                if (drawAlpha > 0.002f) {
                                    drawRect(color = showcaseBaseColor, alpha = drawAlpha)
                                    drawRect(brush = showcaseGlowBrush, alpha = drawAlpha)
                                }
                            },
                    )

                    // [0.5] 阅读纸张过渡层：开书承接正文淡入不露底，关书归零让随框缩小的正文直接溶解入封面（零 saveLayer）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0.5f)
                            .drawWithContent {
                                val drawAlpha = if (activeReaderBookId != null) {
                                    smoothstep01(readerContentReveal.value.coerceIn(0f, 1f) / 0.48f)
                                } else {
                                    0f
                                }
                                if (drawAlpha > 0.002f) {
                                    drawRect(color = readerPaperBg, alpha = drawAlpha)
                                }
                            },
                    )

                    // [1] 阅读器全屏层：开书时待 Hero 封面基本到位后挂载；关书时置于封面之上、同框缩回并前 85ms 溶解，结束后仅跳过绘制不中途销毁
                    val readerImmersiveReady by remember(activeReaderBookId) {
                        derivedStateOf {
                            activeReaderBookId != null &&
                                readerExpand.value >= 0.99f &&
                                readerContentReveal.value >= 0.99f
                        }
                    }
                    if (mountedReaderForSession) {
                        val readerLoadChapterBlocks = remember {
                            suspend { targetBook: BookEntry, chIdx: Int ->
                                withContext(Dispatchers.IO) {
                                    readBookChapterBlocksSync(targetBook, chIdx)
                                }
                            }
                        }
                        val readerLoadImageBytes = remember {
                            suspend { src: String ->
                                withContext(Dispatchers.IO) {
                                    EpubStreamEngine.readEpubImageBytes(src)
                                }
                            }
                        }
                        val readerOnProgressChanged = remember(readerBook.id, viewModel) {
                            { chIdx: Int, page: Int, pageCount: Int, prog: Double ->
                                viewModel.updateBookProgress(
                                    bookId = readerBook.id,
                                    chapterIndex = chIdx,
                                    page = page,
                                    pageCount = pageCount,
                                    progress = prog,
                                )
                            }
                        }
                        val latestReaderBook by rememberUpdatedState(readerBook)
                        val readerOnToggleBookmark = remember(readerBook.id, viewModel) {
                            { bm: com.squartor.reader.model.BookBookmark ->
                                val cur = latestReaderBook
                                val exists = cur.bookmarks.any {
                                    it.id == bm.id || (it.chapterIndex == bm.chapterIndex && it.page == bm.page)
                                }
                                if (exists) {
                                    viewModel.removeBookmark(cur.id, bm.id)
                                } else {
                                    viewModel.addBookmark(cur.id, bm)
                                }
                            }
                        }
                        val readerOnRemoveBookmark = remember(readerBook.id, viewModel) {
                            { bmId: String ->
                                viewModel.removeBookmark(readerBook.id, bmId)
                            }
                        }
                        val readerOnBack = remember(readerBook.id, viewModel) {
                            {
                                viewModel.closeActiveBook(readerBook.id)
                                activeReaderBookId = null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(if (activeReaderBookId != null || hasHeroCoverOverlay) 1f else 2f)
                                .drawWithContent {
                                    if (activeReaderBookId != null ||
                                        readerContentReveal.value > if (hasHeroCoverOverlay) 0.01f else 0.002f
                                    ) {
                                        drawContent()
                                    }
                                }
                                .graphicsLayer {
                                    val t = readerExpand.value.coerceIn(0f, 1f)
                                    val revealT = readerContentReveal.value.coerceIn(0f, 1f)
                                    if (activeReaderBookId != null) {
                                        val textEase = smoothstep01((revealT - 0.20f) / 0.80f)
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                        translationX = 0f
                                        translationY = 0f
                                        scaleX = 1f
                                        scaleY = 1f
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                        clip = false
                                        alpha = textEase
                                    } else {
                                        val activeCloseRect = resolveLiveStartRect()
                                        val curLeft = lerpFloat(activeCloseRect.left, 0f, t)
                                        val curTop = lerpFloat(activeCloseRect.top, 0f, t)
                                        val curW = lerpFloat(activeCloseRect.width, screenW, t).coerceAtLeast(1f)
                                        val curH = lerpFloat(activeCloseRect.height, screenH, t).coerceAtLeast(1f)
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        translationX = curLeft
                                        translationY = curTop
                                        scaleX = (curW / screenW.coerceAtLeast(1f)).coerceAtLeast(0.05f)
                                        scaleY = (curH / screenH.coerceAtLeast(1f)).coerceAtLeast(0.05f)
                                        val isClosingOverlay = activeReaderBookId == null
                                        val hasVisibleReaderContent =
                                            !isClosingOverlay ||
                                                readerContentReveal.value > if (hasHeroCoverOverlay) 0.01f else 0.002f
                                        compositingStrategy = if (hasHeroCoverOverlay) {
                                            CompositingStrategy.Auto
                                        } else {
                                            CompositingStrategy.ModulateAlpha
                                        }
                                        shape = closeReaderCornerShape
                                        clip = t < 0.998f && hasVisibleReaderContent
                                        alpha = if (hasHeroCoverOverlay) 1f else smoothstep01(revealT)
                                    }
                                },
                        ) {
                            ReaderScreen(
                            book = readerBook,
                            style = style,
                            palette = palette,
                            loadChapterBlocks = readerLoadChapterBlocks,
                            loadImageBytes = readerLoadImageBytes,
                            onProgressChanged = readerOnProgressChanged,
                            onUpdateStyle = viewModel::updateStyle,
                            onToggleBookmark = readerOnToggleBookmark,
                            onRemoveBookmark = readerOnRemoveBookmark,
                            onSaveImageToGallery = onSaveImageToGallery,
                            onBack = readerOnBack,
                            immersiveReady = readerImmersiveReady,
                            manageSystemBars = false,
                        )
                        }
                    }

                    val resolveHeroCoverAlpha: () -> Float = {
                        val revealT = readerContentReveal.value.coerceIn(0f, 1f)
                        val closingId = retainedReaderBookId
                        if (activeReaderBookId != null) {
                            1f - smoothstep01(revealT / 0.62f)
                        } else {
                            val revealFadeIn = if (mountedReaderForSession) {
                                1f - smoothstep01(revealT)
                            } else {
                                1f
                            }
                            val stackFadeOut = if (launchedFromSeriesSheet && closingId != null && selectedTab == 1 && !shelfSeriesSheetOpen) {
                                shelfClosingAlphaResolver?.invoke(closingId)?.coerceIn(0f, 1f) ?: 1f
                            } else {
                                1f
                            }
                            revealFadeIn * stackFadeOut
                        }
                    }

                    val seriesClipCornerPx = with(rootDensity) { 28.dp.toPx() }
                    val seriesClipPath = remember { Path() }
                    val resolveHeroSeriesClipRect: () -> Rect? = {
                        val bookIdForClip = activeReaderBookId ?: retainedReaderBookId
                        if (
                            launchedFromSeriesSheet &&
                            shelfSeriesSheetOpen &&
                            bookIdForClip != null &&
                            selectedTab == 1 &&
                            !showStatsDetail &&
                            !showImportPage &&
                            readerExpand.value < 0.35f
                        ) {
                            shelfClipBoundsResolver?.invoke(bookIdForClip)
                                ?.takeIf { it.width >= 20f && it.height >= 20f }
                        } else {
                            null
                        }
                    }

                    // [2] Hero 封面展示层：居中等比 ContentScale.Crop 几何裁剪 + RenderThread 零偏移缩放，消除退场末帧拉伸回弹
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (activeReaderBookId != null || mountedReaderForSession) 2f else 1f)
                            .drawWithContent {
                                val modalClipRect = resolveHeroSeriesClipRect()
                                if (modalClipRect != null) {
                                    val clipT = smoothstep01(readerExpand.value.coerceIn(0f, 1f) / 0.35f)
                                    val cLeft = lerpFloat(modalClipRect.left, 0f, clipT)
                                    val cTop = lerpFloat(modalClipRect.top, 0f, clipT)
                                    val cRight = lerpFloat(modalClipRect.right, screenW, clipT)
                                    val cBottom = lerpFloat(modalClipRect.bottom, screenH, clipT)
                                    val cRadius = lerpFloat(seriesClipCornerPx, 0f, clipT)
                                    seriesClipPath.rewind()
                                    seriesClipPath.addRoundRect(
                                        RoundRect(
                                            left = cLeft,
                                            top = cTop,
                                            right = cRight,
                                            bottom = cBottom,
                                            cornerRadius = CornerRadius(cRadius, cRadius),
                                        ),
                                    )
                                    clipPath(seriesClipPath) {
                                        this@drawWithContent.drawContent()
                                    }
                                } else {
                                    drawContent()
                                }
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = fitWidthDp, height = fitHeightDp)
                                .drawWithContent {
                                    if (resolveHeroCoverAlpha() > 0.003f) {
                                        drawContent()
                                    }
                                }
                                .graphicsLayer {
                                    val t = readerExpand.value.coerceIn(0f, 1f)
                                    val revealT = readerContentReveal.value.coerceIn(0f, 1f)
                                    val useFullscreenClose = activeReaderBookId == null && mountedReaderForSession
                                    val activeStartRect = resolveLiveStartRect()
                                    val targetLeft = if (useFullscreenClose) 0f else fitLeft
                                    val targetTop = if (useFullscreenClose) 0f else fitTop
                                    val targetW = if (useFullscreenClose) screenW else fitW
                                    val targetH = if (useFullscreenClose) screenH else fitH
                                    val curLeft = lerpFloat(activeStartRect.left, targetLeft, t)
                                    val curTop = lerpFloat(activeStartRect.top, targetTop, t)
                                    val curW = lerpFloat(activeStartRect.width, targetW, t)
                                        .coerceAtLeast(activeStartRect.width * 0.90f)
                                        .coerceAtLeast(1f)
                                    val curH = lerpFloat(activeStartRect.height, targetH, t)
                                        .coerceAtLeast(activeStartRect.height * 0.90f)
                                        .coerceAtLeast(1f)
                                    val cropScale = maxOf(
                                        curW / fitW.coerceAtLeast(1f),
                                        curH / fitH.coerceAtLeast(1f),
                                    ).coerceAtLeast(0.05f)
                                    val dissolveLift = if (activeReaderBookId != null) 1f + 0.036f * revealT else 1f
                                    transformOrigin = TransformOrigin.Center
                                    scaleX = cropScale * dissolveLift
                                    scaleY = cropScale * dissolveLift
                                    translationX = (curLeft + curW * 0.5f) - fitW * 0.5f
                                    translationY = (curTop + curH * 0.5f) - fitH * 0.5f
                                    shape = if (useFullscreenClose) closeShowcaseCornerShape else showcaseCornerShape
                                    clip = true
                                    shadowElevation = 0f
                                    alpha = 1f
                                },
                        ) {
                            Image(
                                bitmap = coverThumb,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val heroAlpha = resolveHeroCoverAlpha()
                                        compositingStrategy = if (heroAlpha < 0.999f) {
                                            CompositingStrategy.ModulateAlpha
                                        } else {
                                            CompositingStrategy.Auto
                                        }
                                        alpha = heroAlpha
                                    },
                            )
                        }
                    }
                } else {
                    // 无封面书籍回退过渡
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val maxScrim = if (isLight) 0.24f else 0.44f
                                val scrimT = readerExpand.value.coerceIn(0f, 1f)
                                // 关书时暗色遮罩在 readerExpand 降至 0.18f 前完全退场，不在收回尾段残留一圈暗晕
                                val scrimFactor = if (activeReaderBookId != null) {
                                    scrimT
                                } else {
                                    ((scrimT - 0.65f) / 0.35f).coerceIn(0f, 1f)
                                }
                                alpha = scrimFactor * maxScrim
                            }
                            .background(Color.Black),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val t = readerExpand.value.coerceIn(0f, 1f)
                                alpha = smoothstep01(t / 0.14f)
                                if (t < 0.998f) {
                                    shape = dynamicBoundsShape
                                    clip = true
                                } else {
                                    clip = false
                                }
                                shadowElevation = 0f
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(readerPaperBg),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val rawT = readerExpand.value.coerceIn(0f, 1.05f)
                                    val t = rawT.coerceIn(0f, 1f)
                                    val revealT = readerContentReveal.value.coerceIn(0f, 1f)
                                    if (t >= 0.998f && rawT <= 1.002f && revealT >= 0.998f) {
                                        scaleX = 1f
                                        scaleY = 1f
                                        translationX = 0f
                                        translationY = 0f
                                        alpha = 1f
                                    } else {
                                        val curLeft = lerpFloat(startRect.left, 0f, t)
                                        val curTop = lerpFloat(startRect.top, 0f, t)
                                        val curRight = lerpFloat(startRect.right, screenW, t)
                                        val curBottom = lerpFloat(startRect.bottom, screenH, t)
                                        val readerScale = lerpFloat(baseScale, 1f, rawT)
                                        scaleX = readerScale
                                        scaleY = readerScale
                                        translationX = (curLeft + curRight) * 0.5f - screenW * 0.5f
                                        translationY = (curTop + curBottom) * 0.5f - screenH * 0.5f
                                        alpha = smoothstep01(revealT.coerceAtLeast((t - 0.04f) / 0.30f))
                                    }
                                },
                        ) {
                            ReaderScreen(
                                book = readerBook,
                                style = style,
                                palette = palette,
                                loadChapterBlocks = { targetBook, chIdx ->
                                    withContext(Dispatchers.IO) {
                                        val ch = targetBook.chapters.getOrNull(chIdx)
                                        if (targetBook.format == BookFormat.epub) {
                                            val blocks = if (ch != null) {
                                                EpubStreamEngine.readChapterBlocks(ch, targetBook.sourcePath)
                                            } else emptyList()
                                            blocks.map {
                                                ReaderBlockItem(
                                                    kind = it.kind,
                                                    text = it.text,
                                                    imageSrc = it.imageSrc,
                                                    inlineSpans = it.inlineSpans,
                                                    align = it.align,
                                                )
                                            }
                                        } else {
                                            val txtContent = if (ch != null) {
                                                TxtStreamEngine.readChapterText(ch, targetBook.sourcePath)
                                            } else ""
                                            txtContent.lines()
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }
                                                .map { ReaderBlockItem(kind = "paragraph", text = it) }
                                        }
                                    }
                                },
                                loadImageBytes = { src ->
                                    withContext(Dispatchers.IO) {
                                        EpubStreamEngine.readEpubImageBytes(src)
                                    }
                                },
                                onProgressChanged = { chIdx, page, pageCount, prog ->
                                    viewModel.updateBookProgress(
                                        bookId = readerBook.id,
                                        chapterIndex = chIdx,
                                        page = page,
                                        pageCount = pageCount,
                                        progress = prog,
                                    )
                                },
                                onUpdateStyle = viewModel::updateStyle,
                                onToggleBookmark = { bm ->
                                    val exists = readerBook.bookmarks.any {
                                        it.id == bm.id || (it.chapterIndex == bm.chapterIndex && it.page == bm.page)
                                    }
                                    if (exists) {
                                        viewModel.removeBookmark(readerBook.id, bm.id)
                                    } else {
                                        viewModel.addBookmark(readerBook.id, bm)
                                    }
                                },
                                onRemoveBookmark = { bmId ->
                                    viewModel.removeBookmark(readerBook.id, bmId)
                                },
                                onSaveImageToGallery = onSaveImageToGallery,
                                onBack = {
                                    viewModel.closeActiveBook(readerBook.id)
                                    activeReaderBookId = null
                                },
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

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun smoothstep01(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private class DynamicShowcaseCornerShape(
    private val startWidth: Float,
    private val startHeight: Float,
    private val fitWidth: Float,
    private val fitHeight: Float,
    private val startCornerPx: Float,
    private val endCornerPx: Float,
    private val progressProvider: () -> Float,
    private val wProgressProvider: () -> Float = progressProvider,
    private val hProgressProvider: () -> Float = progressProvider,
    private val startRectProvider: (() -> Rect)? = null,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val wt = wProgressProvider()
        val ht = hProgressProvider()
        val t = progressProvider().coerceIn(0f, 1f)
        val liveRect = startRectProvider?.invoke()
        val liveStartW = liveRect?.width ?: startWidth
        val liveStartH = liveRect?.height ?: startHeight
        val curW = lerpFloat(liveStartW, fitWidth, wt).coerceAtLeast(1f)
        val curH = lerpFloat(liveStartH, fitHeight, ht).coerceAtLeast(1f)
        val sx = (curW / fitWidth.coerceAtLeast(1f)).coerceAtLeast(0.05f)
        val sy = (curH / fitHeight.coerceAtLeast(1f)).coerceAtLeast(0.05f)
        val cornerScreenPx = lerpFloat(startCornerPx, endCornerPx, t).coerceAtLeast(0f)
        val uniformScale = kotlin.math.sqrt(sx * sy).coerceAtLeast(0.05f)
        val uniformRadius = (cornerScreenPx / uniformScale).coerceAtMost(minOf(size.width, size.height) * 0.5f)
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                cornerRadius = CornerRadius(uniformRadius, uniformRadius),
            ),
        )
    }
}

private class DynamicShowcaseCropShape(
    private val startWidth: Float,
    private val startHeight: Float,
    private val fitWidth: Float,
    private val fitHeight: Float,
    private val startCornerPx: Float,
    private val endCornerPx: Float,
    private val progressProvider: () -> Float,
    private val wProgressProvider: () -> Float = progressProvider,
    private val hProgressProvider: () -> Float = progressProvider,
    private val targetWidth: Float = fitWidth,
    private val targetHeight: Float = fitHeight,
    private val startRectProvider: (() -> Rect)? = null,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val wt = wProgressProvider()
        val ht = hProgressProvider()
        val t = progressProvider().coerceIn(0f, 1f)
        val liveRect = startRectProvider?.invoke()
        val liveStartW = liveRect?.width ?: startWidth
        val liveStartH = liveRect?.height ?: startHeight
        val curW = lerpFloat(liveStartW, targetWidth, wt)
            .coerceAtLeast(liveStartW * 0.90f)
            .coerceAtLeast(1f)
        val curH = lerpFloat(liveStartH, targetHeight, ht)
            .coerceAtLeast(liveStartH * 0.90f)
            .coerceAtLeast(1f)
        val safeFitW = fitWidth.coerceAtLeast(1f)
        val safeFitH = fitHeight.coerceAtLeast(1f)
        val cropScale = maxOf(curW / safeFitW, curH / safeFitH).coerceAtLeast(0.05f)
        val localW = (curW / cropScale).coerceIn(1f, size.width.coerceAtLeast(1f))
        val localH = (curH / cropScale).coerceIn(1f, size.height.coerceAtLeast(1f))
        val insetX = (size.width - localW) * 0.5f
        val insetY = (size.height - localH) * 0.5f
        val cornerScreenPx = lerpFloat(startCornerPx, endCornerPx, t).coerceAtLeast(0f)
        val localCorner = (cornerScreenPx / cropScale).coerceAtLeast(0f)
        return Outline.Rounded(
            RoundRect(
                left = insetX,
                top = insetY,
                right = insetX + localW,
                bottom = insetY + localH,
                cornerRadius = CornerRadius(localCorner, localCorner),
            ),
        )
    }
}

private class DynamicReaderBoundsShape(
    private val startRect: Rect,
    private val screenW: Float,
    private val screenH: Float,
    private val startCornerPx: Float,
    private val progressProvider: () -> Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val t = progressProvider().coerceIn(0f, 1f)
        val safeL = lerpFloat(startRect.left, 0f, t).coerceIn(0f, size.width)
        val safeT = lerpFloat(startRect.top, 0f, t).coerceIn(0f, size.height)
        val safeR = lerpFloat(startRect.right, screenW, t).coerceIn(safeL + 1f, size.width.coerceAtLeast(safeL + 1f))
        val safeB = lerpFloat(startRect.bottom, screenH, t).coerceIn(safeT + 1f, size.height.coerceAtLeast(safeT + 1f))
        val cr = lerpFloat(startCornerPx, 0f, (t * 1.15f).coerceIn(0f, 1f)).coerceAtLeast(0f)
        return Outline.Rounded(
            RoundRect(
                left = safeL,
                top = safeT,
                right = safeR,
                bottom = safeB,
                cornerRadius = CornerRadius(cr, cr),
            ),
        )
    }
}

private fun readBookChapterBlocksSync(targetBook: BookEntry, chIdx: Int): List<ReaderBlockItem> {
    val ch = targetBook.chapters.getOrNull(chIdx)
    return if (targetBook.format == BookFormat.epub) {
        val blocks = if (ch != null) {
            EpubStreamEngine.readChapterBlocks(ch, targetBook.sourcePath)
        } else {
            emptyList()
        }
        blocks.map {
            ReaderBlockItem(
                kind = it.kind,
                text = it.text,
                imageSrc = it.imageSrc,
                inlineSpans = it.inlineSpans,
                align = it.align,
            )
        }
    } else {
        val txtContent = if (ch != null) {
            TxtStreamEngine.readChapterText(ch, targetBook.sourcePath)
        } else {
            ""
        }
        txtContent.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { ReaderBlockItem(kind = "paragraph", text = it) }
    }
}

private class TabTopFadeBrushCache(val bg: Color) {
    private var lastHeight = -1f
    var brush: Brush? = null
        private set

    fun ensureForHeight(h: Float): Brush {
        if (h == lastHeight && brush != null) return brush!!
        lastHeight = h
        val fadeEndY = (h * 0.56f).coerceAtLeast(1f)
        return Brush.verticalGradient(
            0.00f to bg,
            0.84f to bg,
            1.00f to Color.Transparent,
            startY = 0f,
            endY = fadeEndY,
        ).also { brush = it }
    }
}

/**
 * 全页面通用的底角单主色 Gemini 动态氛围光（置于页面最底层 Z=0）：
 * - 平稳期（!LocalPagerTransitioning）：绘制完整底色与底角氛围光供本页玻璃控件及 Dock 折射；
 * - 切页滑动期（topOnlyWhenPagerTransitioning && LocalPagerTransitioning）：下半屏透明，透出全局固定底角光，杜绝切页光影割裂。
 */
@Composable
internal fun AmbientSubpageBackground(
    palette: AppPalette,
    topOnlyWhenPagerTransitioning: Boolean = false,
    isAnimating: Boolean = true,
    topLimitFraction: Float = 0.58f,
) {
    val isLight = palette.isLight
    val ambientSource = LocalReadingStyle.current.ambientGlowSource
    val glowState = LocalAmbientGlowColorsState.current
    val staticFallbackGlow = LocalAmbientGlowColors.current?.first
    val toneState = remember(isLight) { GeminiCornerToneState(isLight = isLight) }
    val topFadeCache = remember(palette.background) { TabTopFadeBrushCache(palette.background) }
    val pagerTransitioningState = LocalPagerTransitioningState.current
    val staticPagerTransitioning = LocalPagerTransitioning.current
    val flowSecondsState = rememberAmbientFlowSeconds(isActive = isAnimating)

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas
        val isPagerTransitioning = topOnlyWhenPagerTransitioning &&
            (pagerTransitioningState?.value ?: staticPagerTransitioning)
        if (isPagerTransitioning) {
            // Keep top half opaque behind floating headers/chips while letting stationary root glow show seamlessly below
            drawRect(
                brush = topFadeCache.ensureForHeight(h),
                size = Size(w, h * 0.56f),
            )
            return@Canvas
        }
        drawRect(color = palette.background)
        val primaryGlow = if (ambientSource == AmbientGlowSource.appTheme) {
            palette.primary
        } else {
            glowState?.value?.first ?: staticFallbackGlow ?: palette.primary
        }
        val (cornerFilter, bridgeFilter) = toneState.ensureFilters(primaryGlow)
        drawGeminiCornerAmbientGlowTextures(
            isLight = isLight,
            cornerColorFilter = cornerFilter,
            bottomBridgeColorFilter = bridgeFilter,
            flowSeconds = flowSecondsState.floatValue,
            topLimitFraction = topLimitFraction,
        )
    }
}

@Immutable
private data class HomeTabSnapshot(
    val books: List<BookEntry>,
    val todaySeconds: Int,
    val weekSeconds: Int,
    val monthSeconds: Int,
    val weekDailySeconds: List<Int>,
    val palette: AppPalette,
    val themeRevealButtonProgress: Float,
    val isActive: Boolean,
    val isVisible: Boolean,
)

@Immutable
private data class ShelfTabSnapshot(
    val books: List<BookEntry>,
    val shelves: List<String>,
    val palette: AppPalette,
    val externalEnterBatchToken: Int,
    val isActive: Boolean,
)

@Immutable
private data class StatsTabSnapshot(
    val books: List<BookEntry>,
    val dailyTotals: Map<String, Int>,
    val bookStats: Map<String, Map<String, Int>>,
    val palette: AppPalette,
    val requireDockBackdrop: Boolean,
    val isVisible: Boolean,
)

@Immutable
private data class SettingsTabSnapshot(
    val style: ReadingStyle,
    val fonts: List<ImportedFont>,
    val cloudSync: CloudSyncSettings,
    val storageStats: StorageStats,
    val palette: AppPalette,
    val isSyncing: Boolean,
    val syncFeedbackMessage: String?,
    val themeRevealButtonProgress: Float,
    val requireDockBackdrop: Boolean,
    val isVisible: Boolean,
)

@Stable
private class HomeTabCallbacks(
    val onOpenBook: (BookEntry) -> Unit,
    val onOpenShelf: () -> Unit,
    val onOpenStats: () -> Unit,
    val onToggleBrightness: (Offset) -> Unit,
    val onScrolledOverDockChanged: (Boolean) -> Unit,
    val onRecentRowDragLockChanged: (Boolean) -> Unit,
)

@Stable
private class StatsTabCallbacks(
    val onOpenBook: (BookEntry) -> Unit,
    val onOpenDetail: () -> Unit,
)

@Stable
private class SettingsTabCallbacks(
    val onDismissSyncFeedback: () -> Unit,
    val onUpdateStyle: (ReadingStyle) -> Unit,
    val onToggleBrightness: (Offset) -> Unit,
    val onImportFontClick: () -> Unit,
    val onDeleteFont: (ImportedFont) -> Unit,
    val onUpdateCloudSync: (CloudSyncSettings) -> Unit,
    val onSyncUpload: () -> Unit,
    val onSyncDownload: () -> Unit,
    val onClearCache: () -> Unit,
    val onSubPageActiveChanged: (Boolean) -> Unit,
)

@Stable
private class ShelfTabCallbacks(
    val onOpenBook: (BookEntry) -> Unit,
    val onImportFilesClick: () -> Unit,
    val onImportFolderClick: () -> Unit,
    val onDeleteBooks: (Set<String>) -> Unit,
    val onMoveBooksToShelf: (Set<String>, String?) -> Unit,
    val onGroupSeries: (Set<String>, String) -> Unit,
    val onRemoveFromSeries: (String) -> Unit,
    val onUpdateBookMeta: (String, String, String) -> Unit,
    val onAddShelf: (String) -> Unit,
    val onDeleteShelf: (String) -> Unit,
    val onSelectionModeChanged: (Boolean) -> Unit,
    val onBatchStateChanged: (Boolean, Boolean, Boolean) -> Unit,
    val onRegisterBatchActions: (ShelfBatchActions?) -> Unit,
    val onSeriesSheetOpenChanged: (Boolean) -> Unit,
    val onSeriesSheetRenderedChanged: (Boolean) -> Unit,
    val onRegisterSeriesDismiss: ((() -> Unit)?) -> Unit,
    val onRegisterBoundsResolver: (((String) -> Rect?) -> Unit),
    val onRegisterClosingAlphaResolver: (((String) -> Float) -> Unit),
    val onRegisterClipBoundsResolver: (((String) -> Rect?) -> Unit),
)

@Composable
private fun MainTabsPagerHost(
    pagerState: PagerState,
    isRecentRowInteractingState: State<Boolean>,
    isTabPagerTransitioningState: State<Boolean>,
    shouldDrawTabProvider: (Int) -> Boolean,
    tab0RenderState: State<HomeTabSnapshot>,
    tab1RenderState: State<ShelfTabSnapshot>,
    tab1AccelerateSeriesCloseState: State<Boolean>,
    tab1IsReaderOverlayActiveState: State<Boolean>,
    tab1IsVisibleState: State<Boolean>,
    tab1RequireDockBackdropState: State<Boolean>,
    tab1SuspendChromeBlurState: State<Boolean>,
    tab2RenderState: State<StatsTabSnapshot>,
    tab3RenderState: State<SettingsTabSnapshot>,
    homeTabCallbacks: HomeTabCallbacks,
    shelfTabCallbacks: ShelfTabCallbacks,
    statsTabCallbacks: StatsTabCallbacks,
    settingsTabCallbacks: SettingsTabCallbacks,
    homeTabBackdrop: GlassBackdrop,
    shelfTabBackdrop: GlassBackdrop,
    statsTabBackdrop: GlassBackdrop,
    settingsTabBackdrop: GlassBackdrop,
) {
    val allowTabPagerSwipe = false
    val defaultOverscrollFactory = LocalOverscrollFactory.current
    val lockedPagerNestedScroll = remember { object : NestedScrollConnection {} }
    val pagerModifier = remember {
        Modifier
            .fillMaxSize()
            .clipToBounds()
    }
    val pageHostModifier = remember {
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer()
    }
    val userScrollEnabled = allowTabPagerSwipe && !isRecentRowInteractingState.value
    CompositionLocalProvider(
        LocalOverscrollFactory provides null,
        LocalPagerTransitioningState provides isTabPagerTransitioningState,
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 3,
            userScrollEnabled = userScrollEnabled,
            pageNestedScrollConnection = lockedPagerNestedScroll,
            key = { it },
            modifier = pagerModifier,
        ) { tab ->
            CompositionLocalProvider(LocalOverscrollFactory provides defaultOverscrollFactory) {
                Box(
                    modifier = pageHostModifier
                        .drawWithContent {
                            if (shouldDrawTabProvider(tab)) {
                                drawContent()
                            }
                        },
                ) {
                    when (tab) {
                        0 -> Tab0HomeHost(
                            snapshotState = tab0RenderState,
                            callbacks = homeTabCallbacks,
                            sharedBackdrop = homeTabBackdrop,
                        )
                        1 -> Tab1ShelfHost(
                            snapshotState = tab1RenderState,
                            accelerateSeriesCloseState = tab1AccelerateSeriesCloseState,
                            isReaderOverlayActiveState = tab1IsReaderOverlayActiveState,
                            isVisibleState = tab1IsVisibleState,
                            requireDockBackdropState = tab1RequireDockBackdropState,
                            suspendChromeBlurState = tab1SuspendChromeBlurState,
                            callbacks = shelfTabCallbacks,
                            sharedBackdrop = shelfTabBackdrop,
                        )
                        2 -> Tab2StatsHost(
                            snapshotState = tab2RenderState,
                            callbacks = statsTabCallbacks,
                            sharedBackdrop = statsTabBackdrop,
                        )
                        3 -> Tab3SettingsHost(
                            snapshotState = tab3RenderState,
                            callbacks = settingsTabCallbacks,
                            sharedBackdrop = settingsTabBackdrop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Tab0HomeHost(
    snapshotState: State<HomeTabSnapshot>,
    callbacks: HomeTabCallbacks,
    sharedBackdrop: GlassBackdrop,
) {
    val snapshot = snapshotState.value
    ReadingNowScreen(
        books = snapshot.books,
        todaySeconds = snapshot.todaySeconds,
        weekSeconds = snapshot.weekSeconds,
        monthSeconds = snapshot.monthSeconds,
        weekDailySeconds = snapshot.weekDailySeconds,
        palette = snapshot.palette,
        onOpenBook = callbacks.onOpenBook,
        onOpenShelf = callbacks.onOpenShelf,
        onOpenStats = callbacks.onOpenStats,
        onToggleBrightness = callbacks.onToggleBrightness,
        themeRevealButtonProgress = snapshot.themeRevealButtonProgress,
        isActive = snapshot.isActive,
        isVisible = snapshot.isVisible,
        sharedBackdrop = sharedBackdrop,
        onScrolledOverDockChanged = callbacks.onScrolledOverDockChanged,
        onRecentRowDragLockChanged = callbacks.onRecentRowDragLockChanged,
    )
}

@Composable
private fun Tab1ShelfHost(
    snapshotState: State<ShelfTabSnapshot>,
    accelerateSeriesCloseState: State<Boolean>,
    isReaderOverlayActiveState: State<Boolean>,
    isVisibleState: State<Boolean>,
    requireDockBackdropState: State<Boolean>,
    suspendChromeBlurState: State<Boolean>,
    callbacks: ShelfTabCallbacks,
    sharedBackdrop: GlassBackdrop,
) {
    val snapshot = snapshotState.value
    ShelfScreen(
        books = snapshot.books,
        shelves = snapshot.shelves,
        palette = snapshot.palette,
        onOpenBook = callbacks.onOpenBook,
        onImportFilesClick = callbacks.onImportFilesClick,
        onImportFolderClick = callbacks.onImportFolderClick,
        onDeleteBooks = callbacks.onDeleteBooks,
        onMoveBooksToShelf = callbacks.onMoveBooksToShelf,
        onGroupSeries = callbacks.onGroupSeries,
        onRemoveFromSeries = callbacks.onRemoveFromSeries,
        onUpdateBookMeta = callbacks.onUpdateBookMeta,
        onAddShelf = callbacks.onAddShelf,
        onDeleteShelf = callbacks.onDeleteShelf,
        onSelectionModeChanged = callbacks.onSelectionModeChanged,
        onBatchStateChanged = callbacks.onBatchStateChanged,
        onRegisterBatchActions = callbacks.onRegisterBatchActions,
        onSeriesSheetOpenChanged = callbacks.onSeriesSheetOpenChanged,
        onSeriesSheetRenderedChanged = callbacks.onSeriesSheetRenderedChanged,
        accelerateSeriesCloseState = accelerateSeriesCloseState,
        isReaderOverlayActiveState = isReaderOverlayActiveState,
        suspendChromeBlurState = suspendChromeBlurState,
        onRegisterSeriesDismiss = callbacks.onRegisterSeriesDismiss,
        externalEnterBatchToken = snapshot.externalEnterBatchToken,
        isActive = snapshot.isActive,
        isVisibleState = isVisibleState,
        sharedBackdrop = sharedBackdrop,
        requireDockBackdropState = requireDockBackdropState,
        onRegisterBoundsResolver = callbacks.onRegisterBoundsResolver,
        onRegisterClosingAlphaResolver = callbacks.onRegisterClosingAlphaResolver,
        onRegisterClipBoundsResolver = callbacks.onRegisterClipBoundsResolver,
    )
}

@Composable
private fun Tab2StatsHost(
    snapshotState: State<StatsTabSnapshot>,
    callbacks: StatsTabCallbacks,
    sharedBackdrop: GlassBackdrop,
) {
    val snapshot = snapshotState.value
    StatsScreen(
        books = snapshot.books,
        dailyTotals = snapshot.dailyTotals,
        bookStats = snapshot.bookStats,
        palette = snapshot.palette,
        onOpenBook = callbacks.onOpenBook,
        onOpenDetail = callbacks.onOpenDetail,
        sharedBackdrop = sharedBackdrop,
        requireDockBackdrop = snapshot.requireDockBackdrop,
        isVisible = snapshot.isVisible,
    )
}

@Composable
private fun Tab3SettingsHost(
    snapshotState: State<SettingsTabSnapshot>,
    callbacks: SettingsTabCallbacks,
    sharedBackdrop: GlassBackdrop,
) {
    val snapshot = snapshotState.value
    SettingsScreen(
        style = snapshot.style,
        fonts = snapshot.fonts,
        cloudSync = snapshot.cloudSync,
        storageStats = snapshot.storageStats,
        palette = snapshot.palette,
        isSyncing = snapshot.isSyncing,
        syncFeedbackMessage = snapshot.syncFeedbackMessage,
        onDismissSyncFeedback = callbacks.onDismissSyncFeedback,
        onUpdateStyle = callbacks.onUpdateStyle,
        onToggleBrightness = callbacks.onToggleBrightness,
        themeRevealButtonProgress = snapshot.themeRevealButtonProgress,
        onImportFontClick = callbacks.onImportFontClick,
        onDeleteFont = callbacks.onDeleteFont,
        onUpdateCloudSync = callbacks.onUpdateCloudSync,
        onSyncUpload = callbacks.onSyncUpload,
        onSyncDownload = callbacks.onSyncDownload,
        onClearCache = callbacks.onClearCache,
        onSubPageActiveChanged = callbacks.onSubPageActiveChanged,
        sharedBackdrop = sharedBackdrop,
        requireDockBackdrop = snapshot.requireDockBackdrop,
        isVisible = snapshot.isVisible,
    )
}
