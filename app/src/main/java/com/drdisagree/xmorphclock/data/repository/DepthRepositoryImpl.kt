package com.drdisagree.xmorphclock.data.repository

import android.graphics.Bitmap
import com.drdisagree.xmorphclock.data.source.WallpaperSource
import com.drdisagree.xmorphclock.data.source.depth.DepthStore
import com.drdisagree.xmorphclock.data.source.depth.SubjectSegmenter
import com.drdisagree.xmorphclock.domain.model.DepthModelState
import com.drdisagree.xmorphclock.domain.model.DepthSettings
import com.drdisagree.xmorphclock.domain.model.DepthStatus
import com.drdisagree.xmorphclock.domain.repository.DepthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class DepthRepositoryImpl(
    private val store: DepthStore,
    private val processor: DepthProcessor,
    private val segmenter: SubjectSegmenter,
    private val source: WallpaperSource
) : DepthRepository {

    override val settings: Flow<DepthSettings> = store.settings
    override val subject: Flow<Bitmap?> = store.subject
    override val modelState: Flow<DepthModelState> = flow {
        segmenter.check()
        emitAll(segmenter.state)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            store.loadPreview()
            if (store.settings.value.enabled && source.lastFrame() == null) source.requestFrame()
        }
    }

    override val status: Flow<DepthStatus> = combine(store.status, segmenter.state) { status, model ->
        if (status == DepthStatus.EXTRACTING && model == DepthModelState.DOWNLOADING) DepthStatus.PREPARING else status
    }

    override suspend fun retry() {
        val frame = source.lastFrame() ?: return source.requestFrame()
        processor.process(frame, source.frameScale, force = true)
    }

    override suspend fun setEnabled(enabled: Boolean) {
        store.setEnabled(enabled)
        if (!enabled) return
        val frame = source.lastFrame() ?: return source.requestFrame()
        processor.process(frame, source.frameScale, force = store.subject.value == null)
    }

    override suspend fun setOpacity(opacity: Float) = store.setOpacity(opacity)}
