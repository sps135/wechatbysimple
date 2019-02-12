package com.simple.wechatsimple.model.databse;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ConversationModel extends RealmObject {

    private int conversation;

    @PrimaryKey
    private int targetId;

    public int getConversation() {
        return conversation;
    }

    public void setConversation(int conversation) {
        this.conversation = conversation;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
}
