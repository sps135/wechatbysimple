package com.simple.wechatsimple.model.databse;

import io.realm.RealmObject;

public class UnreadCountModel extends RealmObject {

    private int targetId;

    private int unreadCount;

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
