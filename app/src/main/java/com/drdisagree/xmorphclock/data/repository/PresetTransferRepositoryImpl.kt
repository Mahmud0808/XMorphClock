package com.drdisagree.xmorphclock.data.repository

import com.drdisagree.xmorphclock.BuildConfig
import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.data.mapper.CompositionJsonMapper
import com.drdisagree.xmorphclock.data.mapper.PresetArchiveMapper
import com.drdisagree.xmorphclock.data.mapper.PresetBundleMapper
import com.drdisagree.xmorphclock.data.source.ImageStore
import com.drdisagree.xmorphclock.data.source.MyFacesSource
import com.drdisagree.xmorphclock.data.source.presets.PresetArchive
import com.drdisagree.xmorphclock.data.source.presets.PresetBundle
import com.drdisagree.xmorphclock.data.source.presets.PresetContainer
import com.drdisagree.xmorphclock.data.source.presets.PresetDocumentSource
import com.drdisagree.xmorphclock.data.source.presets.PresetException
import com.drdisagree.xmorphclock.data.source.presets.PresetPaths
import com.drdisagree.xmorphclock.data.source.presets.PresetPreviewImages
import com.drdisagree.xmorphclock.domain.editor.FaceImageRefs
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.PresetFileError
import com.drdisagree.xmorphclock.domain.model.PresetFilePreview
import com.drdisagree.xmorphclock.domain.model.PresetImportResult
import com.drdisagree.xmorphclock.domain.repository.PresetTransferRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PresetTransferRepositoryImpl(
    private val documents: PresetDocumentSource,
    private val mine: MyFacesSource,
    private val images: ImageStore,
    private val previews: PresetPreviewImages
) : PresetTransferRepository {

    override suspend fun exportPresets(target: String, styles: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val files = LinkedHashMap<String, ByteArray>()
            val pending = ArrayDeque(styles.distinct())
            val visited = HashSet<Int>()
            while (pending.isNotEmpty()) {
                val style = pending.removeFirst()
                if (!visited.add(style) || FaceKind.of(style)?.isMine(style) != true) continue
                val face = mine.face(style) ?: continue
                files[PresetPaths.of(PresetPaths.PRESETS, "$style${PresetPaths.JSON}")] = ClockFaceJsonMapper.toJson(face).toByteArray(Charsets.UTF_8)
                mine.layout(style)?.let { layout ->
                    files[PresetPaths.of(PresetPaths.PRESETS, "$style${PresetPaths.LAYOUT}")] = CompositionJsonMapper.toJson(layout).toByteArray(Charsets.UTF_8)
                    pending += layout.widgetStyles.filter { FaceKind.of(it)?.isMine(it) == true }
                }
                FaceImageRefs.ids(face).forEach { id ->
                    images.bytes(id)?.let { files[PresetPaths.of(PresetPaths.IMAGES, "$id${PresetPaths.WEBP}")] = it }
                }
            }
            if (files.isEmpty()) return@withContext false
            val archive = PresetArchive(System.currentTimeMillis(), BuildConfig.VERSION_NAME, files)
            documents.write(target, PresetContainer.seal(PresetArchiveMapper.toZip(archive)))
        } catch (_: IOException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    override suspend fun readPresets(source: String): PresetFilePreview = withContext(Dispatchers.IO) {
        try {
            val bundle = load(source)
            val ids = previews.show(bundle.images)
            PresetFilePreview(bundle.entries.map { it.copy(face = FaceImageRefs.remap(it.face, ids)) })
        } catch (e: PresetException) {
            PresetFilePreview(error = e.error)
        } catch (_: IOException) {
            PresetFilePreview(error = PresetFileError.READ_FAILED)
        }
    }

    override suspend fun importPresets(source: String, styles: Set<Int>): PresetImportResult = withContext(Dispatchers.IO) {
        try {
            val bundle = load(source)
            val chosen = bundle.closure(styles)
            if (chosen.isEmpty()) return@withContext PresetImportResult()
            val ids = bundle.imageIds(chosen).associateWith { id -> images.adopt(id, bundle.images.getValue(id)) }
            val created = chosen.associateWith { style -> create(style, bundle.faces.getValue(style), ids) }
            chosen.forEach { style -> bundle.layouts[style]?.let { mine.saveLayout(created.getValue(style), remap(it, created)) } }
            PresetImportResult(imported = chosen.count { FaceKind.of(it) == FaceKind.CLOCK })
        } catch (e: PresetException) {
            PresetImportResult(error = e.error)
        } catch (_: IOException) {
            PresetImportResult(error = PresetFileError.READ_FAILED)
        }
    }

    override fun closePreview() = previews.clear()

    private fun load(source: String): PresetBundle {
        val data = documents.read(source) ?: throw PresetException(PresetFileError.READ_FAILED)
        val bundle = PresetBundleMapper.fromFile(data)
        if (!bundle.images.all { (id, bytes) -> images.accepts("$id${PresetPaths.WEBP}", bytes) }) {
            throw PresetException(PresetFileError.DAMAGED)
        }
        return bundle
    }

    private fun create(style: Int, face: ClockFace, ids: Map<String, String>): Int =
        mine.create(requireNotNull(FaceKind.of(style)), FaceImageRefs.remap(face, ids))

    private fun remap(layout: Composition, styles: Map<Int, Int>): Composition =
        layout.copy(items = layout.items.map { item -> styles[item.style]?.takeUnless { item.clock }?.let { item.copy(style = it) } ?: item })
}
