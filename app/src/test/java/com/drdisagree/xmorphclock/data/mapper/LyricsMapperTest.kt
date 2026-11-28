package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.LyricLine
import com.drdisagree.xmorphclock.domain.model.LyricWord
import com.drdisagree.xmorphclock.engine.render.LyricText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsMapperTest {

    @Test
    fun parsesLrcLines() {
        val lyrics = LrcMapper.fromLrc("k", "[ar:Someone]\n[00:01.50]First line\n[00:04.00][00:10.00]Repeated\n[00:06.25]\n")
        assertNotNull(lyrics)
        val lines = lyrics!!.lines
        assertEquals(listOf("First line", "Repeated", "Repeated"), lines.map { it.text })
        assertEquals(1500L, lines[0].start)
        assertEquals(4000L, lines[0].end)
        assertEquals(6250L, lines[1].end)
        assertEquals(1, lyrics.indexAt(5000L))
        assertEquals(-1, lyrics.indexAt(1000L))
    }

    @Test
    fun parsesEnhancedLrcWords() {
        val lyrics = LrcMapper.fromLrc("k", "[00:01.00]<00:01.00>Hello <00:01.60>world<00:02.40>\n[00:03.00]Next")!!
        val first = lyrics.lines.first()
        assertEquals("Hello world", first.text)
        assertEquals(listOf("Hello ", "world"), first.words.map { it.text })
        assertEquals(1600L, first.words[0].end)
        assertEquals(2400L, first.words[1].end)
    }

    @Test
    fun parsesLrcMuxJson() {
        val json = """{"lines":[{"text":"One more time","start":30362,"end":31837,"words":[{"text":"One ","start":30362,"end":30662},{"text":"more ","start":30812,"end":30887},{"text":"time","start":31312,"end":31837}]}]}"""
        val lyrics = LrcMuxJsonMapper.fromJson("k", json)!!
        assertEquals("One more time", lyrics.lines.single().text)
        assertEquals(3, lyrics.lines.single().words.size)
        assertNull(LrcMuxJsonMapper.fromJson("k", """{"lines":[]}"""))
    }

    @Test
    fun sungFollowsWordsAndLines() {
        val words = listOf(LyricWord("One ", 0, 300), LyricWord("more ", 400, 500), LyricWord("time", 600, 1000))
        val line = LyricLine("One more time", 0, 1000, words)
        assertEquals(0f, LyricText.sung(line, 0), 0.001f)
        assertEquals(1.5f, LyricText.sung(line, 150), 0.001f)
        assertEquals(3f, LyricText.sung(line, 350), 0.001f)
        assertEquals(11f, LyricText.sung(line, 800), 0.001f)
        assertEquals(13f, LyricText.sung(line, 2000), 0.001f)
        val plain = LyricLine("abcd", 1000, 2000)
        assertEquals(2f, LyricText.sung(plain, 1500), 0.001f)
    }
}
