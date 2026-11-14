package com.tailorbook.core.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import timber.log.Timber
import java.io.File

/**
 * Utility for recording and playing audio voice notes for garment stitching instructions (Phase B).
 */
object VoiceNoteHelper {

    private var activeRecorder: MediaRecorder? = null
    private var activePlayer: MediaPlayer? = null
    private var currentRecordingPath: String? = null

    /**
     * Extracts an [AUDIO:path] tag from stitching instructions text.
     */
    fun extractVoiceNote(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = Regex("""\[AUDIO:([^\]]+)\]""").find(text)
        return match?.groupValues?.get(1)
    }

    /**
     * Extracts a [MASTER:name] or [KARIGAR:name] tag from stitching instructions text.
     */
    fun extractMaster(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val masterMatch = Regex("""\[MASTER:([^\]]+)\]""").find(text)
        if (masterMatch != null) return masterMatch.groupValues[1]
        val karigarMatch = Regex("""\[KARIGAR:([^\]]+)\]""").find(text)
        return karigarMatch?.groupValues?.get(1)
    }

    /**
     * Backward-compatible alias for extractMaster.
     */
    fun extractKarigar(text: String?): String? = extractMaster(text)

    /**
     * Removes system metadata tags like [AUDIO:...], [MASTER:...], and [KARIGAR:...] for clean user display.
     */
    fun cleanInstructions(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text
            .replace(Regex("""\[AUDIO:[^\]]+\]"""), "")
            .replace(Regex("""\[MASTER:[^\]]+\]"""), "")
            .replace(Regex("""\[KARIGAR:[^\]]+\]"""), "")
            .trim()
    }

    /**
     * Embeds audio path and cutting master name into instructions string.
     */
    fun packInstructions(baseText: String, audioPath: String? = null, masterName: String? = null, karigarName: String? = null): String {
        val name = masterName ?: karigarName
        val clean = cleanInstructions(baseText)
        val audioTag = if (!audioPath.isNullOrBlank()) " [AUDIO:$audioPath]" else ""
        val masterTag = if (!name.isNullOrBlank()) " [MASTER:$name]" else ""
        return "$clean$audioTag$masterTag".trim()
    }

    /**
     * Starts recording audio to private filesDir. Returns the destination file path.
     */
    fun startRecording(context: Context): String? {
        stopPlayback()
        stopRecording()
        HapticHelper.click(context)

        return try {
            val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outputFile = File(dir, "vn_${System.currentTimeMillis()}.m4a")
            val path = outputFile.absolutePath

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(path)
                prepare()
                start()
            }

            activeRecorder = recorder
            currentRecordingPath = path
            path
        } catch (e: Exception) {
            Timber.e(e, "VoiceNoteHelper: Failed to start recording")
            activeRecorder?.release()
            activeRecorder = null
            null
        }
    }

    /**
     * Stops the active recording and returns the final file path if valid.
     */
    fun stopRecording(): String? {
        val path = currentRecordingPath
        try {
            activeRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Timber.e(e, "VoiceNoteHelper: Failed to stop recorder cleanly")
        } finally {
            activeRecorder = null
            currentRecordingPath = null
        }
        return if (path != null && File(path).exists() && File(path).length() > 0) path else null
    }

    /**
     * Plays back an audio voice note file.
     */
    fun playVoiceNote(filePath: String, onFinished: () -> Unit) {
        stopPlayback()
        val file = File(filePath)
        if (!file.exists()) {
            onFinished()
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    release()
                    activePlayer = null
                    onFinished()
                }
                prepare()
                start()
            }
            activePlayer = player
        } catch (e: Exception) {
            Timber.e(e, "VoiceNoteHelper: Playback failed for $filePath")
            activePlayer?.release()
            activePlayer = null
            onFinished()
        }
    }

    /**
     * Stops any ongoing audio playback.
     */
    fun stopPlayback() {
        try {
            activePlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Timber.e(e, "VoiceNoteHelper: Error stopping playback")
        } finally {
            activePlayer = null
        }
    }

    fun isPlaying(): Boolean {
        return try {
            activePlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }
}
