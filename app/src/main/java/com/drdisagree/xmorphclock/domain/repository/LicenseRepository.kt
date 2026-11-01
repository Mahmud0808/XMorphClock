package com.drdisagree.xmorphclock.domain.repository

import com.drdisagree.xmorphclock.domain.model.License

interface LicenseRepository {
    suspend fun licenses(): List<License>
}
