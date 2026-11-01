package com.drdisagree.xmorphclock.xposed.data

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.FaceTile
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

@SuppressLint("MissingPermission")
internal class TileController(private val context: Context) {

    private val wifi = context.getSystemService(WifiManager::class.java)
    private val bluetooth = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val camera = context.getSystemService(CameraManager::class.java)
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val torchId: String? by lazy {
        runCatching {
            camera?.cameraIdList?.firstOrNull { id ->
                camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    @Volatile
    var torchOn = false
        private set

    fun registerTorch(executor: Executor, onChange: () -> Unit) {
        val id = torchId ?: return
        camera?.registerTorchCallback(executor, object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId != id) return
                torchOn = enabled
                onChange()
            }
        })
    }

    fun state(tile: FaceTile): Int = runCatching {
        when (tile) {
            FaceTile.WIFI -> on(wifi?.isWifiEnabled == true)
            FaceTile.BLUETOOTH -> on(bluetooth?.isEnabled == true)
            FaceTile.FLASHLIGHT -> on(torchOn)
            FaceTile.HOTSPOT -> on(hotspotEnabled())
            FaceTile.MOBILE_DATA -> on(telephony?.isDataEnabled == true)
            FaceTile.RINGER -> when (ringerMode()) {
                AudioManager.RINGER_MODE_NORMAL -> FaceData.TILE_ON
                AudioManager.RINGER_MODE_VIBRATE -> FaceData.TILE_ALT
                else -> FaceData.TILE_OFF
            }

            FaceTile.DND -> on(notifications?.currentInterruptionFilter.let { it != null && it != NotificationManager.INTERRUPTION_FILTER_ALL })
        }
    }.getOrDefault(FaceData.TILE_OFF)

    fun toggle(tile: FaceTile) {
        try {
            val enable = state(tile) == FaceData.TILE_OFF
            when (tile) {
                FaceTile.WIFI -> setWifi(enable)
                FaceTile.BLUETOOTH -> setBluetooth(enable)
                FaceTile.FLASHLIGHT -> torchId?.let { camera?.setTorchMode(it, !torchOn) }
                FaceTile.HOTSPOT -> setHotspot(enable)
                FaceTile.MOBILE_DATA -> setMobileData(enable)
                FaceTile.RINGER -> setRinger(nextRinger(ringerMode()))
                FaceTile.DND -> setZen(enable)
            }
        } catch (throwable: Throwable) {
            HookEnv.log("Tile toggle failed: $tile", throwable)
        }
    }

    private fun on(value: Boolean): Int = if (value) FaceData.TILE_ON else FaceData.TILE_OFF

    @Suppress("DEPRECATION")
    private fun setWifi(enable: Boolean) {
        wifi?.setWifiEnabled(enable)
    }

    @Suppress("DEPRECATION")
    private fun setBluetooth(enable: Boolean) {
        val adapter = bluetooth ?: return
        if (enable) adapter.enable() else adapter.disable()
    }

    @Suppress("DEPRECATION")
    private fun setMobileData(enable: Boolean) {
        telephony?.setDataEnabled(enable)
    }

    private fun hotspotEnabled(): Boolean =
        wifi?.javaClass?.getMethod(IS_AP_ENABLED)?.invoke(wifi) as? Boolean ?: false

    private fun setHotspot(enable: Boolean) {
        val tethering = context.getSystemService(TETHERING_SERVICE) ?: return
        if (!enable) {
            tethering.javaClass.getMethod(STOP_TETHERING, Int::class.javaPrimitiveType).invoke(tethering, TETHERING_WIFI)
            return
        }
        val builderClass = Class.forName(REQUEST_BUILDER)
        val request = builderClass.getConstructor(Int::class.javaPrimitiveType).newInstance(TETHERING_WIFI)
            .let { builder -> builderClass.getMethod(BUILD).invoke(builder) }
        val callbackClass = Class.forName(START_CALLBACK)
        val callback = Proxy.newProxyInstance(callbackClass.classLoader, arrayOf(callbackClass)) { _, _, _ -> null }
        tethering.javaClass.getMethod(START_TETHERING, request.javaClass, Executor::class.java, callbackClass)
            .invoke(tethering, request, context.mainExecutor, callback)
    }

    private fun ringerMode(): Int =
        runCatching { audio?.javaClass?.getMethod(GET_RINGER_INTERNAL)?.invoke(audio) as Int }.getOrNull()
            ?: audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

    private fun nextRinger(mode: Int): Int = when (mode) {
        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
        AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
        else -> AudioManager.RINGER_MODE_NORMAL
    }

    private fun setRinger(mode: Int) {
        val manager = audio ?: return
        runCatching { manager.javaClass.getMethod(SET_RINGER_INTERNAL, Int::class.javaPrimitiveType).invoke(manager, mode) }
            .onFailure { manager.ringerMode = mode }
    }

    private fun setZen(enable: Boolean) {
        val manager = notifications ?: return
        val mode = if (enable) ZEN_IMPORTANT_INTERRUPTIONS else ZEN_OFF
        runCatching {
            manager.javaClass.getMethod(SET_ZEN, Int::class.javaPrimitiveType, Uri::class.java, String::class.java)
                .invoke(manager, mode, null, ZEN_REASON)
        }.onFailure {
            manager.setInterruptionFilter(
                if (enable) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
    }

    private companion object {
        const val TETHERING_SERVICE = "tethering"
        const val TETHERING_WIFI = 0
        const val IS_AP_ENABLED = "isWifiApEnabled"
        const val STOP_TETHERING = "stopTethering"
        const val START_TETHERING = "startTethering"
        const val BUILD = "build"
        const val REQUEST_BUILDER = "android.net.TetheringManager${'$'}TetheringRequest${'$'}Builder"
        const val START_CALLBACK = "android.net.TetheringManager${'$'}StartTetheringCallback"
        const val GET_RINGER_INTERNAL = "getRingerModeInternal"
        const val SET_RINGER_INTERNAL = "setRingerModeInternal"
        const val SET_ZEN = "setZenMode"
        const val ZEN_OFF = 0
        const val ZEN_IMPORTANT_INTERRUPTIONS = 1
        const val ZEN_REASON = "XMorphClock"
    }
}
