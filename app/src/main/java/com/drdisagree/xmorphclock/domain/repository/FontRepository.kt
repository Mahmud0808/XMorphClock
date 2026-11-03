package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.UserFont
import kotlinx.coroutines.flow.Flow

interface FontRepository {
    val fonts: Flow<List<UserFont>>
    suspend fun import(uri: String): UserFont?
}
