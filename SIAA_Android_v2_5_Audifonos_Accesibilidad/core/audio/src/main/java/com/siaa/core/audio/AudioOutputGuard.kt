package com.siaa.core.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.security.MessageDigest

/** Connected-device guard, NOT proof that an A2DP sink is physically a headset.
 * A user's output confirmation and an OEM device test are still required. */
class AudioOutputGuard(private val context: Context, private val onNoisy: () -> Unit) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private var registered = false
    private var previousSignature = ""
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onNoisy()
        }
    }
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = checkChange()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = checkChange()
    }
    private fun checkChange() {
        val now = routeSignature(context)
        if (previousSignature != now) { previousSignature = now; onNoisy() }
    }
    fun hasSafePrivateOutput(): Boolean = privateOutputs(context).isNotEmpty()
    fun register() {
        if (registered) return
        previousSignature = routeSignature(context)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        manager?.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        registered = true
    }
    fun unregister() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        manager?.unregisterAudioDeviceCallback(callback)
        registered = false
    }
    companion object {
        private val SAFE_OUTPUT_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_HEARING_AID, AudioDeviceInfo.TYPE_BLE_HEADSET
        )
        fun privateOutputs(context: Context): List<AudioDeviceInfo> = context.getSystemService(AudioManager::class.java)
            ?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.filter { it.type in SAFE_OUTPUT_TYPES }.orEmpty()
        /** No MAC addresses, location, identifiers or Bluetooth permissions. Same-named devices can collide. */
        fun routeSignature(context: Context): String {
            val labels = privateOutputs(context).map { "${it.type}:${it.productName}" }.sorted()
            if (labels.isEmpty()) return ""
            return MessageDigest.getInstance("SHA-256").digest(labels.joinToString("|").toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }
}
