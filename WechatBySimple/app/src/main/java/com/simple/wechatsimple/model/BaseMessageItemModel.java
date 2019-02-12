package com.simple.wechatsimple.model;

import com.simple.wechatsimple.base.IBaseBindingAdapterItem;

public class BaseMessageItemModel implements IBaseBindingAdapterItem {

    private String id;

    private int fromUserId;

    private int targetId;

    private int messageType;

    private long createAt;

    private String portrait;

    private String nickname;

    private boolean isReceived;

    private boolean isNeedShowDateLabel;

    private boolean isShowLoading;

    private boolean isShowError;

    private int progress;

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

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isReceived() {
        return isReceived;
    }

    public void setReceived(boolean received) {
        isReceived = received;
    }

    public boolean isNeedShowDateLabel() {
        return isNeedShowDateLabel;
    }

    public void setNeedShowDateLabel(boolean needShowDateLabel) {
        isNeedShowDateLabel = needShowDateLabel;
    }

    public boolean isShowLoading() {
        return isShowLoading;
    }

    public void setShowLoading(boolean showLoading) {
        isShowLoading = showLoading;
    }

    public boolean isShowError() {
        return isShowError;
    }

    public void setShowError(boolean showError) {
        isShowError = showError;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public int getItemViewType() {
        return 0;
    }
}
