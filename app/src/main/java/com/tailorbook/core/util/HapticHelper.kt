package com.tailorbook.core.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object HapticHelper {

    /** Light tactile click for buttons, tabs, steppers */
    fun tick(context: Context? = null, haptic: HapticFeedback? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        try {
            context?.let { ctx ->
                val vibrator = getVibrator(ctx)
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(15)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /** Satisfying crisp confirmation click for primary actions (Save, Mark Ready, Deliver) */
    fun click(context: Context? = null, haptic: HapticFeedback? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            context?.let { ctx ->
                val vibrator = getVibrator(ctx)
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(35)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /** Success pattern (double pulse) for completing an order, cloud sync, or receiving payment */
    fun success(context: Context? = null, haptic: HapticFeedback? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            context?.let { ctx ->
                val vibrator = getVibrator(ctx)
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = longArrayOf(0, 30, 60, 40)
                        val amplitudes = intArrayOf(0, 180, 0, 255)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 30, 60, 40), -1)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
