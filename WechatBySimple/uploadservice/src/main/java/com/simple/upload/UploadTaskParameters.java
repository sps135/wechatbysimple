package com.simple.upload;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 */
public final class UploadTaskParameters implements Parcelable {

    public String id;
    public String serverUrl;
    private int maxRetries = 0;
    public boolean autoDeleteSuccessfullyUploadedFiles = false;
    public ArrayList<UploadFile> files = new ArrayList<>();

    public UploadTaskParameters() {

    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<UploadTaskParameters> CREATOR =
            new Creator<UploadTaskParameters>() {
                @Override
                public UploadTaskParameters createFromParcel(final Parcel in) {
                    return new UploadTaskParameters(in);
                }

                @Override
                public UploadTaskParameters[] newArray(final int size) {
                    return new UploadTaskParameters[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(id);
        parcel.writeString(serverUrl);
        parcel.writeInt(maxRetries);
        parcel.writeByte((byte) (autoDeleteSuccessfullyUploadedFiles ? 1 : 0));
        parcel.writeList(files);
    }

    private UploadTaskParameters(Parcel in) {
        id = in.readString();
        serverUrl = in.readString();
        maxRetries = in.readInt();
        autoDeleteSuccessfullyUploadedFiles = in.readByte() == 1;
        in.readList(files, UploadFile.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public UploadTaskParameters setMaxRetries(int maxRetries) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = maxRetries;

        return this;
    }

}
