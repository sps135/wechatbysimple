package com.simple.wechatsimple.model;

import com.simple.wechatsimple.R;

public class VoiceMessageItemModel extends BaseMessageItemModel {

    private int duration;

    private String audioUri;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getAudioUri() {
        return audioUri;
    }

    public void setAudioUri(String audioUri) {
        this.audioUri = audioUri;
    }

    @Override
    public int getItemViewType() {
        if (isReceived()) {
            return R.layout.item_audio_receive;
        } else {
            return R.layout.item_audio_send;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VoiceMessageItemModel) {
            return ((VoiceMessageItemModel) obj).getId().equals(getId());
        }

        return false;
    }
}
