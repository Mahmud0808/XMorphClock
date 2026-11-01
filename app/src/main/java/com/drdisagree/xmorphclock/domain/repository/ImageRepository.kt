package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.FaceImage

interface ImageRepository {
    suspend fun import(uri: String): FaceImage?
    suspend fun delete(ids: List<String>)
}
