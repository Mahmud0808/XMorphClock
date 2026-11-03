package com.drdisagree.xmorphclock.data.repository

import android.content.Context
import com.drdisagree.xmorphclock.data.source.ClockFaces
import com.drdisagree.xmorphclock.data.source.FaceLibrary
import com.drdisagree.xmorphclock.data.source.FaceOverrideSource
import com.drdisagree.xmorphclock.data.source.MyFacesSource
import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FaceRepositoryImpl(
    private val context: Context,
    private val overrides: FaceOverrideSource,
    private val mine: MyFacesSource
) : FaceRepository {

    override fun faces(kind: FaceKind): Flow<List<FacePreset>> = combine(
        presetFlow(kind),
        overrides.changes.map { overrides.load() },
        mine.changes.map { mine.load(kind) }
    ) { presets, edited, own ->
        own.map { (style, face) -> FacePreset(style, face.copy(category = mineCategory(face)), layout = mine.layout(style)) } + presets.map { preset ->
            edited[preset.style]?.let { preset.copy(face = it.copy(category = preset.face.category), isModified = true) } ?: preset
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun face(style: Int): FacePreset? = withContext(Dispatchers.IO) {
        val kind = FaceKind.of(style) ?: return@withContext null
        if (kind.isMine(style)) return@withContext mine.face(style)?.let { FacePreset(style, it.copy(category = mineCategory(it)), layout = mine.layout(style)) }
        val default = FaceLibrary.faceOf(context, style) ?: return@withContext null
        val edited = overrides.load()[style]
        FacePreset(style, edited?.copy(category = default.category) ?: default, isModified = edited != null)
    }

    override suspend fun defaultFace(style: Int): ClockFace? = withContext(Dispatchers.IO) {
        FaceLibrary.faceOf(context, style)
    }

    override suspend fun saveFace(style: Int, face: ClockFace) = withContext(Dispatchers.IO) {
        if (FaceKind.of(style)?.isMine(style) == true) mine.save(style, face) else overrides.save(style, face)
    }

    override suspend fun resetFace(style: Int) = withContext(Dispatchers.IO) {
        overrides.delete(style)
    }

    override suspend fun createFace(kind: FaceKind, face: ClockFace): Int = withContext(Dispatchers.IO) {
        mine.create(kind, face.copy(category = mineCategory(face)))
    }

    override suspend fun deleteFace(style: Int) = withContext(Dispatchers.IO) {
        if (FaceKind.of(style)?.isMine(style) == true) mine.delete(style)
    }

    override suspend fun saveLayout(style: Int, layout: Composition?) = withContext(Dispatchers.IO) {
        if (FaceKind.of(style)?.isMine(style) == true) mine.saveLayout(style, layout)
    }

    private fun presetFlow(kind: FaceKind): Flow<List<FacePreset>> = callbackFlow {
        val listener: () -> Unit = { trySend(presets(kind)) }
        trySend(presets(kind))
        if (kind == FaceKind.CLOCK) ClockFaces.addChangeListener(listener)
        awaitClose { if (kind == FaceKind.CLOCK) ClockFaces.removeChangeListener(listener) }
    }

    private fun presets(kind: FaceKind): List<FacePreset> = FaceLibrary.presets(context, kind).mapIndexed { index, face ->
        FacePreset(FaceLibrary.styleOf(kind, face, index), face)
    }

    private fun mineCategory(face: ClockFace): String =
        if (CustomFaces.isPrivate(face)) face.category else CustomFaces.MINE_CATEGORY
}
