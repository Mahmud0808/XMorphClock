package com.drdisagree.xmorphclock.data.repository

import com.drdisagree.xmorphclock.data.source.FontStore
import com.drdisagree.xmorphclock.domain.model.UserFont
import com.drdisagree.xmorphclock.domain.repository.FontRepository
import kotlinx.coroutines.flow.Flow

class FontRepositoryImpl(private val store: FontStore) : FontRepository {

    override val fonts: Flow<List<UserFont>> = store.fonts

    override suspend fun import(uri: String): UserFont? = store.import(uri)
}
