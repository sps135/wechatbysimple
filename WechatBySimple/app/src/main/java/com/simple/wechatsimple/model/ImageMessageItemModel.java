package com.simple.wechatsimple.model;

import com.simple.wechatsimple.R;

public class ImageMessageItemModel extends BaseMessageItemModel {

    private String imageUri;

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    @Override
    public int getItemViewType() {
        if (isReceived()) {
            return R.layout.item_image_receive;
        } else {
            return R.layout.item_image_send;
        }
    }
}
