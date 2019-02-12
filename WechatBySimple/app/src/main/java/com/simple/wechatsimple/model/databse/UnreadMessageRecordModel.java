package com.simple.wechatsimple.model.databse;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class UnreadMessageRecordModel extends RealmObject {

    @PrimaryKey
    private String id;

    private int targetId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
}
