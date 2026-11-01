package com.drdisagree.xmorphclock.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.editor.CustomFaces

@Composable
fun categoryLabel(category: String): String = stringResource(
    when (category) {
        CustomFaces.MINE_CATEGORY -> R.string.category_mine
        "small" -> R.string.category_small
        "medium" -> R.string.category_medium
        "inline" -> R.string.category_inline
        "expressive" -> R.string.category_expressive
        "artistic" -> R.string.category_artistic
        "playful" -> R.string.category_playful
        "analog" -> R.string.category_analog
        "elegant" -> R.string.category_elegant
        "classic" -> R.string.category_classic
        "graphic" -> R.string.category_graphic
        "digital" -> R.string.category_digital
        "minimal" -> R.string.category_minimal
        "glass" -> R.string.category_glass
        "solid" -> R.string.category_solid
        "bold" -> R.string.category_bold
        "status" -> R.string.category_status
        "controls" -> R.string.category_controls
        "activity" -> R.string.category_activity
        else -> R.string.category_other
    }
)
