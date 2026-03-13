package com.Bobr.mill.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.Bobr.mill.R
import com.Bobr.mill.data.DataManager

class SoundManager(context: Context, private val dataManager: DataManager) {
    private var soundPool: SoundPool
    private var placeSoundId: Int = 0
    private var moveSoundId: Int = 0
    private var removeSoundId: Int = 0

    // NEU: MediaPlayer für die durchgehende Hintergrundmusik
    private var mediaPlayer: MediaPlayer? = null

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        try {
            placeSoundId = soundPool.load(context, R.raw.sound_place, 1)
            moveSoundId = soundPool.load(context, R.raw.sound_move, 1)
            removeSoundId = soundPool.load(context, R.raw.sound_remove, 1)

            // NEU: Musik laden und auf Dauerschleife (Loop) stellen
            mediaPlayer = MediaPlayer.create(context, R.raw.music_bg)
            mediaPlayer?.isLooping = true
            updateMusicVolume() // Setzt die Lautstärke sofort auf den gespeicherten Wert
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- SOUND EFFEKTE (SFX) ---
    fun playPlaceSound() {
        val vol = dataManager.sfxVolume
        if (placeSoundId != 0) soundPool.play(placeSoundId, vol, vol, 0, 0, 1f)
    }

    fun playMoveSound() {
        val vol = dataManager.sfxVolume
        if (moveSoundId != 0) soundPool.play(moveSoundId, vol, vol, 0, 0, 1f)
    }

    fun playRemoveSound() {
        val vol = dataManager.sfxVolume
        if (removeSoundId != 0) soundPool.play(removeSoundId, vol, vol, 0, 0, 1f)
    }

    // --- MUSIK ---
    fun startMusic() {
        mediaPlayer?.let {
            if (!it.isPlaying) it.start()
        }
    }

    fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
    }

    fun updateMusicVolume() {
        val vol = dataManager.musicVolume
        mediaPlayer?.setVolume(vol, vol)
    }

    fun release() {
        soundPool.release()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}