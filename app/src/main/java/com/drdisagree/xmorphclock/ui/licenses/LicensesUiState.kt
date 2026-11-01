package com.drdisagree.xmorphclock.ui.licenses

import com.drdisagree.xmorphclock.domain.model.License

data class LicensesUiState(
    val licenses: List<License> = emptyList()
)
