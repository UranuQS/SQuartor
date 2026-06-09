package com.squartor.reader

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File

class MainActivity : FlutterActivity() {
    private var pendingDirectoryResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "squartor/native_picker")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "pickBookDirectory" -> pickBookDirectory(result)
                    "saveImageToGallery" -> saveImageToGallery(
                        call.argument<ByteArray>("bytes"),
                        call.argument<String>("fileName"),
                        call.argument<String>("mimeType"),
                        result
                    )
                    else -> result.notImplemented()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
    }

    override fun onResume() {
        super.onResume()
        requestHighestRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            requestHighestRefreshRate()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_BOOK_DIRECTORY) return
        val result = pendingDirectoryResult ?: return
        pendingDirectoryResult = null
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            result.success(emptyList<String>())
            return
        }
        val treeUri = data.data!!
        val flags = data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try {
            contentResolver.takePersistableUriPermission(
                treeUri,
                flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Throwable) {
            // Some providers grant temporary access only; reading below can still work.
        }
        Thread {
            try {
                val picked = mutableListOf<String>()
                val targetRoot = File(cacheDir, "picked_books/${System.currentTimeMillis()}")
                targetRoot.mkdirs()
                val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
                copyBookDocuments(treeUri, rootDocumentId, targetRoot, picked)
                runOnUiThread { result.success(picked) }
            } catch (error: Throwable) {
                runOnUiThread {
                    result.error("PICK_BOOK_DIRECTORY_FAILED", error.message, null)
                }
            }
        }.start()
    }

    private fun pickBookDirectory(result: MethodChannel.Result) {
        if (pendingDirectoryResult != null) {
            result.error("PICKER_BUSY", "A directory picker is already open.", null)
            return
        }
        pendingDirectoryResult = result
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQUEST_BOOK_DIRECTORY)
        } catch (error: Throwable) {
            pendingDirectoryResult = null
            result.error("PICK_BOOK_DIRECTORY_FAILED", error.message, null)
        }
    }

    private fun saveImageToGallery(
        bytes: ByteArray?,
        requestedName: String?,
        requestedMimeType: String?,
        result: MethodChannel.Result
    ) {
        if (bytes == null || bytes.isEmpty()) {
            result.error("EMPTY_IMAGE", "Image data is empty.", null)
            return
        }
        Thread {
            try {
                val fileName = requestedName ?: "squartor_${System.currentTimeMillis()}.jpg"
                val mimeType = requestedMimeType ?: "image/jpeg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/SQuartor"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: error("Unable to create gallery item.")
                try {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(bytes)
                    } ?: error("Unable to open gallery output stream.")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }
                    runOnUiThread { result.success(uri.toString()) }
                } catch (error: Throwable) {
                    contentResolver.delete(uri, null, null)
                    throw error
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    result.error("SAVE_IMAGE_FAILED", error.message, null)
                }
            }
        }.start()
    }

    private fun copyBookDocuments(
        treeUri: Uri,
        documentId: String,
        targetDir: File,
        picked: MutableList<String>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            documentId
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            )
            val nameIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
            val mimeIndex = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            while (cursor.moveToNext()) {
                val childId = cursor.getString(idIndex) ?: continue
                val name = cursor.getString(nameIndex) ?: "book"
                val mime = cursor.getString(mimeIndex) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    copyBookDocuments(treeUri, childId, targetDir, picked)
                    continue
                }
                if (!isImportableBookName(name)) continue
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    childId
                )
                val output = uniqueTargetFile(targetDir, name)
                contentResolver.openInputStream(documentUri)?.use { input ->
                    output.outputStream().use { outputStream ->
                        input.copyTo(outputStream)
                    }
                }
                if (output.exists() && output.length() > 0L) {
                    picked.add(output.absolutePath)
                } else {
                    output.delete()
                }
            }
        }
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
        val extension = if (dot > 0) cleanName.substring(dot) else ""
        var index = 2
        while (candidate.exists()) {
            candidate = File(targetDir, "$base ($index)$extension")
            index += 1
        }
        return candidate
    }

    private fun requestHighestRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val currentDisplay = display ?: return
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                window.decorView.javaClass
                    .getMethod(
                        "setFrameRate",
                        java.lang.Float.TYPE,
                        java.lang.Integer.TYPE
                    )
                    .invoke(window.decorView, bestMode.refreshRate, 0)
            } catch (_: Throwable) {
                // Older compile/runtime combinations can ignore this hint.
            }
        }
    }

    companion object {
        private const val REQUEST_BOOK_DIRECTORY = 2309
    }
}
