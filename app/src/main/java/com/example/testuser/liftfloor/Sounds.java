package com.example.testuser.liftfloor;

import android.media.SoundPool;



import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;

/**
 * Created by denny on 5/14/15.
 */
public class Sounds {
    SoundPool soundPool;
    HashMap<String, Integer> soundPoolMap;
    AudioManager audioManager;
    TextToSpeech tts;

    public Sounds(Context context) {
        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<String, Integer>();
        soundPoolMap.put("sndclick1", soundPool.load(context, R.raw.sndclick1, 1));
    }

    void sound(){
                float curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                float maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float leftVolume = curVolume / maxVolume;
                float rightVolume = curVolume / maxVolume;
                int priority = 1;
                int no_loop = 0;
                float normal_playback_rate = 1f;
                soundPool.play(soundPoolMap.get("sndclick1"), leftVolume, rightVolume, priority, no_loop, normal_playback_rate);
    }

}
