package com.drdisagree.xmorphclock.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsQueriesTest {

    @Test
    fun videoTitleSplitsIntoSongAndArtist() {
        val queries = LyricsQueries.of("Toota Jo Kabhi Tara - Tiger Shroff, Jacqueline | Sumedha K | Sachin Jigar", "Zee Music Company")
        assertTrue(queries.first().strict)
        assertTrue(queries.any { it.title == "Toota Jo Kabhi Tara" && it.artist == null })
    }

    @Test
    fun bracketsAndFeaturingAreRemoved() {
        assertEquals("Shape of You", LyricsQueries.cleanTitle("Shape of You (Official Music Video) [4K]"))
        assertEquals("Song", LyricsQueries.cleanTitle("Song ft. Someone Else"))
    }

    @Test
    fun cleanMetadataStaysSingleQuery() {
        val queries = LyricsQueries.of("Shape of You", "Ed Sheeran")
        assertEquals("Shape of You", queries.first().title)
        assertEquals("Ed Sheeran", queries.first().artist)
    }

    @Test
    fun titlesCompareLoosely() {
        assertTrue(LyricsQueries.sameTitle("Toota Jo Kabhi Tara - Tiger Shroff", "Toota Jo Kabhi Tara"))
        assertFalse(LyricsQueries.sameTitle("Shape of You", "Perfect"))
    }
}
