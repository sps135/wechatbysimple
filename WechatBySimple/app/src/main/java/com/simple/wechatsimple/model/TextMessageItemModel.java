package com.simple.wechatsimple.model;

import android.text.SpannableString;

import com.simple.wechatsimple.R;

public class  TextMessageItemModel extends BaseMessageItemModel {

    private SpannableString message;

    private String messageText;

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public SpannableString getMessage() {
        return message;
    }

    public void setMessage(SpannableString message) {
        this.message = message;
    }

    @Override
    public int getItemViewType() {
        if (isReceived()) {
            return R.layout.item_text_receive;
        } else {
            return R.layout.item_text_send;
        }
    }
}
