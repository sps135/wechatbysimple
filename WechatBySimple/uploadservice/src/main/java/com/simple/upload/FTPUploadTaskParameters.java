package com.simple.upload;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * FTP upload parameters.
 * @author Aleksandar Gotev
 */
public class FTPUploadTaskParameters implements Parcelable {

    protected static final String PARAM_FTP_TASK_PARAMETERS = "ftpTaskParameters";

    /**
     * The default FTP connection timeout in milliseconds.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    /**
     * The default FTP socket timeout in milliseconds.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = 30000;

    /**
     * The default protocol to use when FTP over SSL is enabled (FTPS).
     */
    public static final String DEFAULT_SECURE_SOCKET_PROTOCOL = "TLS";

    public int port;
    public String username;
    public String password;
    public int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    public int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    public boolean compressedFileTransfer;
    public String createdDirectoriesPermissions;
    public boolean useSSL;
    public boolean implicitSecurity;
    public String secureSocketProtocol = DEFAULT_SECURE_SOCKET_PROTOCOL;
    public String id;
    public String serverUrl;
    private int maxRetries = 0;
    public boolean autoDeleteSuccessfullyUploadedFiles = false;
    public ArrayList<UploadFile> files = new ArrayList<>();

    public FTPUploadTaskParameters() {

    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<FTPUploadTaskParameters> CREATOR =
            new Creator<FTPUploadTaskParameters>() {
                @Override
                public FTPUploadTaskParameters createFromParcel(final Parcel in) {
                    return new FTPUploadTaskParameters(in);
                }

                @Override
                public FTPUploadTaskParameters[] newArray(final int size) {
                    return new FTPUploadTaskParameters[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(port);
        parcel.writeString(username);
        parcel.writeString(password);
        parcel.writeInt(connectTimeout);
        parcel.writeInt(socketTimeout);
        parcel.writeByte((byte) (compressedFileTransfer ? 1 : 0));
        parcel.writeString(createdDirectoriesPermissions);
        parcel.writeByte((byte) (useSSL ? 1 : 0));
        parcel.writeByte((byte) (implicitSecurity ? 1 : 0));
        parcel.writeString(secureSocketProtocol);
        parcel.writeString(id);
        parcel.writeString(serverUrl);
        parcel.writeInt(maxRetries);
        parcel.writeByte((byte) (autoDeleteSuccessfullyUploadedFiles ? 1 : 0));
        parcel.writeList(files);
    }

    private FTPUploadTaskParameters(Parcel in) {
        port = in.readInt();
        username = in.readString();
        password = in.readString();
        connectTimeout = in.readInt();
        socketTimeout = in.readInt();
        compressedFileTransfer = in.readByte() == 1;
        createdDirectoriesPermissions = in.readString();
        useSSL = in.readByte() == 1;
        implicitSecurity = in.readByte() == 1;
        secureSocketProtocol = in.readString();
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

    public FTPUploadTaskParameters setMaxRetries(int maxRetries) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = maxRetries;

        return this;
    }
}
