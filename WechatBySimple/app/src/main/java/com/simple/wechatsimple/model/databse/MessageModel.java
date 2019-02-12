package com.simple.wechatsimple.model.databse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.simple.imlib.constant.Constant;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class MessageModel extends RealmObject {

    @PrimaryKey
    private String id;

    private int fromUserId;

    private int targetId;

    private int messageType;

    private int conversationType;

    private long createAt;

    private boolean isReceived;

    private String extras;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(int fromUserId) {
        this.fromUserId = fromUserId;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }

    public boolean isReceived() {
        return isReceived;
    }

    public void setReceived(boolean received) {
        isReceived = received;
    }

    public int getConversationType() {
        return conversationType;
    }

    public void setConversationType(int conversationType) {
        this.conversationType = conversationType;
    }

    public String getExtras() {
        return extras;
    }

    public void setExtras(String extras) {
        this.extras = extras;
    }

    public boolean isPrivateMessage() {
        return (conversationType == Constant.PRIVATE_MESSAGE);
    }

    public void setImageUri(String imageUri) {
        JsonObject extrasValue = new JsonObject();
        extrasValue.addProperty("imageUri", imageUri);

        extras = extrasValue.toString();
    }

    public void setAudioParams(String audioUri, int duration) {
        JsonObject extrasValue = new JsonObject();
        extrasValue.addProperty("audioUri", audioUri);
        extrasValue.addProperty("audioDuration", duration);

        extras = extrasValue.toString();
    }

    public void setTextMessage(String message) {
        JsonObject extrasValue = new JsonObject();
        extrasValue.addProperty("message", message);

        extras = extrasValue.toString();
    }

    public String getImageUri() {
        Gson gson = new Gson();
        JsonObject object = gson.fromJson(extras, JsonObject.class);

        if (object.has("imageUri")) {
            return object.get("imageUri").getAsString();
        }
        return "";
    }

    public String getAudioUri() {
        Gson gson = new Gson();
        JsonObject object = gson.fromJson(extras, JsonObject.class);

        if (object.has("audioUri")) {
            return object.get("audioUri").getAsString();
        }
        return "";
    }

    public int getAudioDuration() {
        Gson gson = new Gson();
        JsonObject object = gson.fromJson(extras, JsonObject.class);

        if (object.has("audioDuration")) {
            return object.get("audioDuration").getAsInt();
        }
        return 0;
    }

    public String getTextMessage() {
        Gson gson = new Gson();
        JsonObject object = gson.fromJson(extras, JsonObject.class);

        if (object.has("message")) {
            return object.get("message").getAsString();
        }
        return "";
    }
}
