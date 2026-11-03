package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.UserFont
import com.drdisagree.xmorphclock.domain.repository.FontRepository

class ImportFontUseCase(private val repository: FontRepository) {
    suspend operator fun invoke(uri: String): UserFont? = repository.import(uri)
}
