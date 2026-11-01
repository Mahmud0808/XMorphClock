package com.drdisagree.xmorphclock.data.repository

import android.content.Context
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.License
import com.drdisagree.xmorphclock.domain.repository.LicenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LicenseRepositoryImpl(private val context: Context) : LicenseRepository {

    override suspend fun licenses(): List<License> = withContext(Dispatchers.IO) {
        LICENSES.mapNotNull { (name, file) ->
            try {
                License(context.getString(name), context.assets.open("$LICENSE_DIR/$file").bufferedReader().use { it.readText() })
            } catch (_: Exception) {
                null
            }
        }
    }
}

private const val LICENSE_DIR = "licenses"
private val LICENSES = listOf(
    R.string.license_roboto_flex to "roboto_flex_ofl.txt",
    R.string.license_liquid_glass to "android_liquid_glass_apache.txt",
    R.string.font_name_inter to "font_inter_ofl.txt",
    R.string.font_name_google_sans_flex to "font_google_sans_flex_ofl.txt",
    R.string.font_name_manrope to "font_manrope_ofl.txt",
    R.string.font_name_jetbrains_mono to "font_jetbrains_mono_ofl.txt",
    R.string.font_name_fraunces to "font_fraunces_ofl.txt",
    R.string.font_name_space_grotesk to "font_space_grotesk_ofl.txt",
    R.string.font_name_oswald to "font_oswald_ofl.txt",
    R.string.font_name_unbounded to "font_unbounded_ofl.txt",
    R.string.font_name_doto to "font_doto_ofl.txt",
    R.string.font_name_playfair to "font_playfair_ofl.txt",
    R.string.font_name_rubik to "font_rubik_ofl.txt",
    R.string.font_name_archivo to "font_archivo_ofl.txt",
    R.string.font_name_big_shoulders to "font_big_shoulders_ofl.txt",
    R.string.font_name_orbitron to "font_orbitron_ofl.txt",
    R.string.font_name_sora to "font_sora_ofl.txt",
    R.string.font_name_outfit to "font_outfit_ofl.txt",
    R.string.font_name_lexend to "font_lexend_ofl.txt",
    R.string.font_name_quicksand to "font_quicksand_ofl.txt",
    R.string.font_name_nunito to "font_nunito_ofl.txt",
    R.string.font_name_montserrat to "font_montserrat_ofl.txt",
    R.string.font_name_plus_jakarta to "font_plus_jakarta_ofl.txt",
    R.string.font_name_urbanist to "font_urbanist_ofl.txt",
    R.string.font_name_figtree to "font_figtree_ofl.txt",
    R.string.font_name_bebas to "font_bebas_ofl.txt",
    R.string.font_name_anton to "font_anton_ofl.txt",
    R.string.font_name_bodoni to "font_bodoni_ofl.txt",
    R.string.font_name_instrument_serif to "font_instrument_serif_ofl.txt",
    R.string.font_name_dm_serif to "font_dm_serif_ofl.txt",
    R.string.font_name_caveat to "font_caveat_ofl.txt",
    R.string.font_name_share_tech_mono to "font_share_tech_mono_ofl.txt",
    R.string.font_name_monoton to "font_monoton_ofl.txt",
    R.string.font_name_michroma to "font_michroma_ofl.txt",
    R.string.font_name_major_mono to "font_major_mono_ofl.txt",
    R.string.font_name_dynapuff to "font_dynapuff_ofl.txt",
    R.string.font_name_bagel_fat to "font_bagel_fat_ofl.txt",
    R.string.font_name_fredoka to "font_fredoka_ofl.txt",
    R.string.font_name_abril to "font_abril_ofl.txt",
    R.string.font_name_shrikhand to "font_shrikhand_ofl.txt",
    R.string.font_name_gloock to "font_gloock_ofl.txt",
    R.string.font_name_caveat_brush to "font_caveat_brush_ofl.txt",
    R.string.font_name_londrina to "font_londrina_ofl.txt",
    R.string.font_name_gochi to "font_gochi_ofl.txt",
    R.string.font_name_mansalva to "font_mansalva_ofl.txt",
    R.string.font_name_sedgwick to "font_sedgwick_ofl.txt",
    R.string.font_name_doodle to "font_doodle_ofl.txt",
    R.string.font_name_symbols to "font_symbols_apache.txt"
)
