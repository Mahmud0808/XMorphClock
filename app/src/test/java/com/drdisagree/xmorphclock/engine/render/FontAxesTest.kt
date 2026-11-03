package com.drdisagree.xmorphclock.engine.render

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FontAxesTest {

    @Test
    fun matchesKnownWeightAxes() {
        val found = File("src/main/res/font").listFiles { file -> file.extension == "ttf" }.orEmpty()
            .filter { file -> file.inputStream().use { FontAxes.hasWeight(it) } }
            .map { it.nameWithoutExtension }
            .toSortedSet()
        assertEquals(WEIGHTED, found)
    }

    @Test
    fun rejectsTruncatedData() {
        assertFalse(FontAxes.hasWeight(ByteArrayInputStream(ByteArray(8))))
    }

    private companion object {
        val WEIGHTED = sortedSetOf(
            "font_archivo", "font_big_shoulders", "font_bodoni", "font_caveat", "font_doodle", "font_doto", "font_dynapuff",
            "font_figtree", "font_fraunces", "font_fredoka", "font_google_sans_flex", "font_inter", "font_jetbrains_mono",
            "font_lexend", "font_manrope", "font_montserrat", "font_nunito", "font_orbitron", "font_oswald", "font_outfit",
            "font_playfair", "font_plus_jakarta", "font_quicksand", "font_rubik", "font_sora", "font_space_grotesk",
            "font_symbols", "font_unbounded", "font_urbanist", "morph_roboto_flex"
        )
    }
}
