package com.simple.wechatsimple.model.databse;

import io.realm.RealmObject;

public class LoginStateModel extends RealmObject {

    private int uid;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }
}
