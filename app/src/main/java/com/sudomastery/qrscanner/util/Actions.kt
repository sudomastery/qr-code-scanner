package com.sudomastery.qrscanner.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast

object Actions {

    fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // Reused across beeps: each instance holds a native AudioTrack that is
    // never released otherwise, and per-call instances can be finalized
    // mid-tone.
    private val toneGenerator: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull()
    }

    fun beep() {
        runCatching { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150) }
    }

    fun copy(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        // Android 13+ shows its own clipboard confirmation overlay
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }
    }

    fun share(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share scan"))
    }

    fun openUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }

    fun openRaw(context: Context, raw: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(raw)))
        }.onFailure {
            Toast.makeText(context, "No app can handle this content", Toast.LENGTH_SHORT).show()
        }
    }
}
