package com.simple.wechatsimple.util;

public class AudioStateMessage {
    public int what;
    public Object obj;

    public AudioStateMessage() {
    }

    public static AudioStateMessage obtain() {
        return new AudioStateMessage();
    }
}

