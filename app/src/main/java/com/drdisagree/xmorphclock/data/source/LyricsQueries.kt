package com.drdisagree.xmorphclock.data.source

internal object LyricsQueries {

    fun of(title: String, artist: String?): List<LyricsQuery> {
        val rawTitle = title.trim()
        val rawArtist = artist?.trim()?.takeIf { it.isNotEmpty() }
        val result = ArrayList<LyricsQuery>()
        result += LyricsQuery(rawTitle, rawArtist, strict = true)
        val cleaned = cleanTitle(rawTitle)
        val cleanedArtist = rawArtist?.let(::cleanArtist)?.takeIf { it.isNotEmpty() }
        val parts = cleaned.split(DASH, limit = 2).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size == 2) {
            result += LyricsQuery(parts[1], parts[0], strict = false)
            result += LyricsQuery(parts[0], parts[1], strict = false)
            result += LyricsQuery(parts[0], null, strict = false)
        } else if (cleaned.isNotEmpty()) {
            result += LyricsQuery(cleaned, cleanedArtist, strict = false)
            result += LyricsQuery(cleaned, null, strict = false)
        }
        return result
            .distinctBy { normalize(it.title) + SEPARATOR + it.artist?.let(::normalize).orEmpty() }
            .filter { normalize(it.title).isNotEmpty() }
            .take(MAX_QUERIES)
    }

    fun sameTitle(query: String, found: String?): Boolean {
        if (found.isNullOrBlank()) return true
        val wanted = normalize(cleanTitle(query))
        val got = normalize(cleanTitle(found))
        if (wanted.isEmpty() || got.isEmpty()) return true
        return wanted.contains(got) || got.contains(wanted)
    }

    fun cleanTitle(title: String): String = title
        .replace(BRACKETS, " ")
        .substringBefore(PIPE)
        .substringBefore(FULL_PIPE)
        .replace(FEATURING, "")
        .replace(SPACES, " ")
        .trim()
        .trim('-', ' ')

    private fun cleanArtist(artist: String): String = artist
        .replace(TOPIC, "")
        .replace(VEVO, "")
        .replace(SPACES, " ")
        .trim()

    private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    private val BRACKETS = Regex("""[(\[【「].*?[)\]】」]""")
    private val FEATURING = Regex("""(?i)\s+(ft\.?|feat\.?|featuring)\s+.*$""")
    private val TOPIC = Regex("""(?i)\s*-\s*topic$""")
    private val VEVO = Regex("""(?i)vevo$""")
    private val SPACES = Regex("""\s+""")
    private val DASH = Regex("""\s+[-–—]\s+""")
    private const val PIPE = "|"
    private const val FULL_PIPE = "｜"
    private const val SEPARATOR = "|"
    private const val MAX_QUERIES = 4
}
