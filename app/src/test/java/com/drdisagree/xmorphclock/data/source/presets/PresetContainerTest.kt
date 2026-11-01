package com.drdisagree.xmorphclock.data.source.presets

import com.drdisagree.xmorphclock.data.mapper.PresetArchiveMapper
import com.drdisagree.xmorphclock.domain.model.PresetFileError
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class PresetContainerTest {

    private val archive = PresetArchive(
        createdAt = 1_700_000_000_000L,
        appVersion = "1.0",
        files = mapOf(
            "presets/6000.json" to "{\"name\":\"Mine\"}".toByteArray(),
            "presets/6000.layout.json" to "{\"items\":[]}".toByteArray(),
            "images/abc123.webp" to byteArrayOf(1, 2, 3, 4)
        )
    )

    @Test
    fun roundTrip() {
        val sealed = PresetContainer.seal(PresetArchiveMapper.toZip(archive))
        val restored = PresetArchiveMapper.fromZip(PresetContainer.open(sealed))
        assertEquals(archive.createdAt, restored.createdAt)
        assertEquals(archive.appVersion, restored.appVersion)
        assertEquals(archive.files.keys, restored.files.keys)
        archive.files.forEach { (path, bytes) -> assertArrayEquals(bytes, restored.files.getValue(path)) }
        assertEquals(setOf("6000.json", "6000.layout.json"), restored.filesIn("presets").keys)
    }

    @Test
    fun randomIvPerFile() {
        val payload = PresetArchiveMapper.toZip(archive)
        assertFalse(PresetContainer.seal(payload).contentEquals(PresetContainer.seal(payload)))
    }

    @Test
    fun tamperedPayloadIsRejected() {
        val sealed = PresetContainer.seal(PresetArchiveMapper.toZip(archive))
        sealed[sealed.size - 20] = (sealed[sealed.size - 20].toInt() xor 1).toByte()
        expect(PresetFileError.DAMAGED) { PresetContainer.open(sealed) }
    }

    @Test
    fun tamperedHeaderIsRejected() {
        val sealed = PresetContainer.seal(PresetArchiveMapper.toZip(archive))
        sealed[HEADER_IV] = (sealed[HEADER_IV].toInt() xor 1).toByte()
        expect(PresetFileError.DAMAGED) { PresetContainer.open(sealed) }
    }

    @Test
    fun wrongMagicIsRejected() {
        val sealed = PresetContainer.seal(PresetArchiveMapper.toZip(archive))
        sealed[0] = 0
        expect(PresetFileError.NOT_PRESET_FILE) { PresetContainer.open(sealed) }
        expect(PresetFileError.NOT_PRESET_FILE) { PresetContainer.open(byteArrayOf(1, 2)) }
    }

    @Test
    fun unsupportedVersionIsRejected() {
        val sealed = PresetContainer.seal(PresetArchiveMapper.toZip(archive))
        sealed[HEADER_VERSION] = (PresetContainer.VERSION + 1).toByte()
        expect(PresetFileError.UNSUPPORTED_VERSION) { PresetContainer.open(sealed) }
    }

    @Test
    fun truncatedFileIsRejected() {
        val sealed = PresetContainer.seal(PresetArchiveMapper.toZip(archive))
        expect(PresetFileError.DAMAGED) { PresetContainer.open(sealed.copyOf(HEADER_IV + 4)) }
    }

    @Test
    fun unsafePathIsRejected() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("{\"format\":1}".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("files/../escape.json"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }
        expect(PresetFileError.DAMAGED) { PresetArchiveMapper.fromZip(output.toByteArray()) }
    }

    @Test
    fun newerFormatIsRejected() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("{\"format\":${PresetArchiveMapper.FORMAT + 1}}".toByteArray())
            zip.closeEntry()
        }
        expect(PresetFileError.UNSUPPORTED_VERSION) { PresetArchiveMapper.fromZip(output.toByteArray()) }
    }

    private fun expect(error: PresetFileError, block: () -> Unit) {
        try {
            block()
            fail("Expected $error")
        } catch (e: PresetException) {
            assertEquals(error, e.error)
        }
    }

    private companion object {
        const val HEADER_VERSION = 4
        const val HEADER_IV = 5
    }
}
