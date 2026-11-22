package com.drdisagree.xmorphclock.data.source

import android.content.Context
import androidx.core.net.toUri
import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.domain.model.ReportLog
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportFileSource(private val context: Context) {

    suspend fun write(target: String, files: Map<String, ByteArray>): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(target.toUri(), WRITE_TRUNCATE)?.use { stream ->
                ZipOutputStream(stream).use { zip ->
                    files.forEach { (name, bytes) ->
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                }
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun systemLogs(): Map<ReportLog, String> = buildMap {
        read(SYSTEMUI_COMMAND)?.let { put(ReportLog.SYSTEMUI, it) }
        read(CRASH_COMMAND)?.let { put(ReportLog.CRASH, it) }
        read(FRAMEWORK_COMMAND)?.let { put(ReportLog.FRAMEWORK, it) }
    }

    suspend fun depthFiles(): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        buildMap {
            APP_DEPTH_FILES.forEach { (source, name) ->
                File(context.filesDir, source).takeIf { it.isFile }?.let { put(DEPTH_DIR + name, it.readBytes()) }
            }
            RootShell.read(FRAMEWORK_SUBJECT_COMMAND) { it.readBytes() }?.takeIf { it.isNotEmpty() }?.let { put(DEPTH_DIR + FRAMEWORK_SUBJECT, it) }
            read(WALLPAPER_INFO_COMMAND)?.let { put(DEPTH_DIR + WALLPAPER_INFO, it.toByteArray()) }
            read(SURFACE_COMMAND)?.let { put(DEPTH_DIR + SURFACE_LAYERS, it.toByteArray()) }
        }
    }

    private suspend fun read(command: String): String? = RootShell.read(command) { it.readBytes().decodeToString() }
        ?.trim()
        ?.ifEmpty { null }
}

private const val WRITE_TRUNCATE = "wt"
private const val DEPTH_DIR = "depth/"
private const val FRAMEWORK_SUBJECT = "depth_subject_framework.png"
private const val WALLPAPER_INFO = "wallpaper_info.xml"
private const val SURFACE_LAYERS = "surfaceflinger.txt"
private val APP_DEPTH_FILES = listOf(
    "lockscreen_frame.png" to "lockscreen_frame.png",
    "lock_wallpaper_transform" to "lock_wallpaper_transform.txt",
    "depth_preview.png" to "depth_preview.png",
    "shared/depth_subject.png" to "depth_subject_app.png"
)
private val FRAMEWORK_SUBJECT_COMMAND = "cat /data/adb/lspd/modules/0/${BuildConfig.APPLICATION_ID}/files/depth_subject.png 2>/dev/null"
private const val WALLPAPER_INFO_COMMAND =
    "abx2xml /data/system/users/0/wallpaper_info.xml - 2>/dev/null || cat /data/system/users/0/wallpaper_info.xml"
private const val SURFACE_COMMAND = "dumpsys SurfaceFlinger 2>/dev/null | grep -A3 'Layer \\[.*WallpaperService-'"
private const val SYSTEMUI_COMMAND =
    "pid=$(pidof com.android.systemui); " +
        "if [ -n \"\$pid\" ]; then logcat -d -v threadtime -b main,system --pid=\$pid 2>/dev/null | tail -n 600; " +
        "else logcat -d -v threadtime -b main,system 2>/dev/null | " +
        "grep -iE 'XMorphClock|LSPosed|Vector|libxposed|AndroidRuntime|FATAL|com\\.android\\.systemui' | tail -n 600; fi"
private const val CRASH_COMMAND = "logcat -d -v threadtime -b crash 2>/dev/null | tail -n 300"
private const val FRAMEWORK_COMMAND =
    "dir=/data/adb/lspd/log; " +
        "m=$(ls -t \$dir/modules_*.log 2>/dev/null | head -n 1); " +
        "v=$(ls -t \$dir/verbose_*.log 2>/dev/null | head -n 1); " +
        "if [ -n \"\$m\" ]; then echo \"# \$m\"; tail -n 300 \"\$m\"; fi; " +
        "if [ -n \"\$v\" ]; then echo \"# \$v\"; grep -iE 'xmorph|systemui|error|exception|fatal' \"\$v\" | tail -n 200; fi"
