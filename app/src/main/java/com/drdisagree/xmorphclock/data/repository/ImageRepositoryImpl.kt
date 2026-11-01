package com.drdisagree.xmorphclock.data.repository

import com.drdisagree.xmorphclock.data.source.ImageStore
import com.drdisagree.xmorphclock.domain.model.FaceImage
import com.drdisagree.xmorphclock.domain.repository.ImageRepository

class ImageRepositoryImpl(private val store: ImageStore) : ImageRepository {
    override suspend fun import(uri: String): FaceImage? = store.import(uri)

    override suspend fun delete(ids: List<String>) = store.delete(ids)
}
