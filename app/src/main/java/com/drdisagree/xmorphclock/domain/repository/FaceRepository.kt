package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import kotlinx.coroutines.flow.Flow

interface FaceRepository {
    fun faces(kind: FaceKind): Flow<List<FacePreset>>
    suspend fun face(style: Int): FacePreset?
    suspend fun defaultFace(style: Int): ClockFace?
    suspend fun saveFace(style: Int, face: ClockFace)
    suspend fun resetFace(style: Int)
    suspend fun createFace(kind: FaceKind, face: ClockFace): Int
    suspend fun deleteFace(style: Int)
    suspend fun saveLayout(style: Int, layout: Composition?)
}
