package com.simple.wechatsimple.model;

public class ContactItemModel {

    public static final int ITEM = 0;
    public static final int SECTION = 1;

    public ContactItemModel(String text) {
        this.type = SECTION;
        this.text = text;
        this.isTitle = true;
    }

    public ContactItemModel() {
        this.type = ITEM;
        this.isTitle = false;
    }

    private int uid;

    private String nickname;

    private String portrait = "";


    private int type;
    private String text;
    private int sectionPosition;
    private int listPosition;
    private boolean isTitle;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSectionPosition() {
        return sectionPosition;
    }

    public void setSectionPosition(int sectionPosition) {
        this.sectionPosition = sectionPosition;
    }

    public int getListPosition() {
        return listPosition;
    }

    public void setListPosition(int listPosition) {
        this.listPosition = listPosition;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isTitle() {
        return isTitle;
    }

    public void setTitle(boolean title) {
        isTitle = title;
    }
}
