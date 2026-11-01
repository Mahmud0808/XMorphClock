package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.License
import com.drdisagree.xmorphclock.domain.repository.LicenseRepository

class GetLicensesUseCase(private val repository: LicenseRepository) {
    suspend operator fun invoke(): List<License> = repository.licenses()
}
