package com.drdisagree.xmorphclock.data.source.depth

import android.content.Context
import android.graphics.Bitmap
import com.drdisagree.xmorphclock.domain.model.DepthModelState
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SubjectSegmenter(private val context: Context) {

    private val segmenter by lazy {
        SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
    }
    private val installer by lazy { ModuleInstall.getClient(context) }
    private val stateFlow = MutableStateFlow(DepthModelState.CHECKING)

    val state: StateFlow<DepthModelState> = stateFlow.asStateFlow()

    suspend fun subject(bitmap: Bitmap): Bitmap? {
        if (!ensureModel()) return null
        val client = runCatching { segmenter }.getOrNull() ?: return null
        return await(client.process(InputImage.fromBitmap(bitmap, 0)))?.foregroundBitmap
    }

    suspend fun check() {
        if (stateFlow.value == DepthModelState.DOWNLOADING) return
        stateFlow.value = probe()
    }

    private suspend fun probe(): DepthModelState {
        val services = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (services != ConnectionResult.SUCCESS) return DepthModelState.UNAVAILABLE
        val client = runCatching { segmenter }.getOrNull() ?: return DepthModelState.UNAVAILABLE
        return when (await(installer.areModulesAvailable(client))?.areModulesAvailable()) {
            true -> DepthModelState.READY
            false -> DepthModelState.MISSING
            null -> DepthModelState.UNAVAILABLE
        }
    }

    private suspend fun ensureModel(): Boolean {
        check()
        when (stateFlow.value) {
            DepthModelState.READY -> return true
            DepthModelState.UNAVAILABLE -> return false
            else -> Unit
        }
        stateFlow.value = DepthModelState.DOWNLOADING
        val request = ModuleInstallRequest.newBuilder().addApi(segmenter).build()
        if (await(installer.installModules(request)) == null) {
            stateFlow.value = probe()
            return false
        }
        repeat(INSTALL_POLLS) {
            if (probe() == DepthModelState.READY) {
                stateFlow.value = DepthModelState.READY
                return true
            }
            delay(INSTALL_POLL_MS)
        }
        stateFlow.value = probe()
        return stateFlow.value == DepthModelState.READY
    }

    private suspend fun <T> await(task: Task<T>): T? = if (task.isComplete) {
        if (task.isSuccessful) task.result else null
    } else suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        task.addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
    }

    private companion object {
        const val INSTALL_POLLS = 90
        const val INSTALL_POLL_MS = 2_000L
    }
}
