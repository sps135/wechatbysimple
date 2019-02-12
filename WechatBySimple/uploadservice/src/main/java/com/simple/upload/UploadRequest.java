package com.simple.upload;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.FileNotFoundException;
import java.util.UUID;

public class UploadRequest {
    private static final String LOG_TAG = UploadRequest.class.getSimpleName();

    protected final Context context;
    protected final FTPUploadTaskParameters params = new FTPUploadTaskParameters();
    protected UploadStatusDelegate delegate;
    protected static final String PARAM_REMOTE_PATH = "ftpRemotePath";
    protected static final String PARAM_PERMISSIONS = "ftpPermissions";


    public UploadRequest(Context context, String uploadId, String serverUrl, int port) {
        this(context, uploadId, serverUrl);

        if (port <= 0) {
            throw new IllegalArgumentException("Specify valid FTP port!");
        }

        params.port = port;
    }

    /**
     * Creates a new FTP upload request and automatically generates an upload id that will
     * be returned when you call {@link UploadRequest#startUpload()}.
     *
     * @param context application context
     * @param serverUrl server IP address or hostname
     * @param port FTP port
     */
    public UploadRequest(final Context context, final String serverUrl, int port) {
        this(context, null, serverUrl, port);
    }

    /**
     * Creates a new upload request.
     *
     * @param context   application context
     * @param uploadId  unique ID to assign to this upload request. If is null or empty, a random
     *                  UUID will be automatically generated. It's used in the broadcast receiver
     *                  when receiving updates.
     * @param serverUrl URL of the server side script that handles the request
     * @throws IllegalArgumentException if one or more arguments are not valid
     */
    public UploadRequest(final Context context, final String uploadId, final String serverUrl)
            throws IllegalArgumentException {

        if (context == null)
            throw new IllegalArgumentException("Context MUST not be null!");

        if (serverUrl == null || "".equals(serverUrl)) {
            throw new IllegalArgumentException("Server URL cannot be null or empty");
        }

        this.context = context;

        if (uploadId == null || uploadId.isEmpty()) {
            Logger.debug(LOG_TAG, "null or empty upload ID. Generating it");
            params.id = UUID.randomUUID().toString();
        } else {
            Logger.debug(LOG_TAG, "setting provided upload ID");
            params.id = uploadId;
        }

        params.serverUrl = serverUrl;
        Logger.debug(LOG_TAG, "Created new upload request to "
                + params.serverUrl + " with ID: " + params.id);
    }

    /**
     * Start the background file upload service.
     *
     * @return the uploadId string. If you have passed your own uploadId in the constructor, this
     * method will return that same uploadId, otherwise it will return the automatically
     * generated uploadId
     */
    public String startUpload() {
        Intent intent = new Intent(context, UploadService.class);
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
        return params.id;
    }

    /**
     * Sets the automatic file deletion after successful upload.
     *
     * @param autoDeleteFiles true to auto delete files included in the
     *                        request when the upload is completed successfully.
     *                        By default this setting is set to false, and nothing gets deleted.
     * @return self instance
     */
    public UploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFiles) {
        params.autoDeleteSuccessfullyUploadedFiles = autoDeleteFiles;
        return this;
    }

    /**
     * Sets the maximum number of retries that the library will try if an error occurs,
     * before returning an error.
     *
     * @param maxRetries number of maximum retries on error
     * @return self instance
     */
    public UploadRequest setMaxRetries(int maxRetries) {
        params.setMaxRetries(maxRetries);
        return this;
    }

    public UploadRequest setDelegate(UploadStatusDelegate delegate) {
        this.delegate = delegate;
        return this;
    }

    /**
     * Add a file to be uploaded.
     *
     * @param filePath   path to the local file on the device
     * @param remotePath absolute path (or relative path to the default remote working directory)
     *                   of the file on the FTP server. Valid paths are for example:
     *                   {@code /path/to/myfile.txt}, {@code relative/path/} or {@code myfile.zip}.
     *                   If any of the directories of the specified remote path does not exist,
     *                   they will be automatically created. You can also set with which permissions
     *                   to create them by using
     *                   method.
     *                   <br><br>
     *                   Remember that if the remote path ends with {@code /}, the remote file name
     *                   will be the same as the local file, so for example if I'm uploading
     *                   {@code /home/alex/photos.zip} into {@code images/} remote path, I will have
     *                   {@code photos.zip} into the remote {@code images/} directory.
     *                   <br><br>
     *                   If the remote path does not end with {@code /}, the last path segment
     *                   will be used as the remote file name, so for example if I'm uploading
     *                   {@code /home/alex/photos.zip} into {@code images/vacations.zip}, I will
     *                   have {@code vacations.zip} into the remote {@code images/} directory.
     * @return {@link UploadRequest}
     * @throws FileNotFoundException if the local file does not exist
     */
    public UploadRequest addFileToUpload(String filePath, String remotePath) throws FileNotFoundException {
        return addFileToUpload(filePath, remotePath, null);
    }

    /**
     * Add a file to be uploaded.
     *
     * @param filePath    path to the local file on the device
     * @param remotePath  absolute path (or relative path to the default remote working directory)
     *                    of the file on the FTP server. Valid paths are for example:
     *                    {@code /path/to/myfile.txt}, {@code relative/path/} or {@code myfile.zip}.
     *                    If any of the directories of the specified remote path does not exist,
     *                    they will be automatically created. You can also set with which permissions
     *                    to create them by using
     *                    method.
     *                    <br><br>
     *                    Remember that if the remote path ends with {@code /}, the remote file name
     *                    will be the same as the local file, so for example if I'm uploading
     *                    {@code /home/alex/photos.zip} into {@code images/} remote path, I will have
     *                    {@code photos.zip} into the remote {@code images/} directory.
     *                    <br><br>
     *                    If the remote path does not end with {@code /}, the last path segment
     *                    will be used as the remote file name, so for example if I'm uploading
     *                    {@code /home/alex/photos.zip} into {@code images/vacations.zip}, I will
     *                    have {@code vacations.zip} into the remote {@code images/} directory.
     * @param permissions UNIX permissions for the uploaded file
     * @return {@link UploadRequest}
     * @throws FileNotFoundException if the local file does not exist
     */
    public UploadRequest addFileToUpload(String filePath, String remotePath, UnixPermissions permissions)
            throws FileNotFoundException {
        UploadFile file = new UploadFile(filePath);

        if (remotePath == null || remotePath.isEmpty()) {
            throw new IllegalArgumentException("You have to specify a remote path");
        }

        file.setProperty(PARAM_REMOTE_PATH, remotePath);

        if (permissions != null) {
            file.setProperty(PARAM_PERMISSIONS, permissions.toString());
        }

        params.files.add(file);
        return this;
    }


    /**
     * Set the credentials used to login on the FTP Server.
     *
     * @param username account username
     * @param password account password
     * @return {@link UploadRequest}
     */
    public UploadRequest setUsernameAndPassword(String username, String password) {
        if (username == null || "".equals(username)) {
            throw new IllegalArgumentException("Specify FTP account username!");
        }

        if (password == null || "".equals(password)) {
            throw new IllegalArgumentException("Specify FTP account password!");
        }

        params.username = username;
        params.password = password;
        return this;
    }

    /**
     * Enables or disables FTP over SSL processing (FTPS). By default SSL is disabled.
     *
     * @param useSSL true to enable SSL, false to disable it
     * @return {@link UploadRequest}
     */
    public UploadRequest useSSL(boolean useSSL) {
        params.useSSL = useSSL;
        return this;
    }

    /**
     * Sets FTPS security mode. By default the security mode is explicit. This flag is used
     * only if {@link UploadRequest#useSSL(boolean)} is set to true.
     *
     * @param isImplicit true sets security mode to implicit, false sets it to explicit.
     * @return {@link UploadRequest}
     * @see <a href="https://en.wikipedia.org/wiki/FTPS#Methods_of_invoking_security">FTPS Security modes</a>
     */
    public UploadRequest setSecurityModeImplicit(boolean isImplicit) {
        params.implicitSecurity = isImplicit;
        return this;
    }

    /**
     * Sets the secure socket protocol to use when {@link UploadRequest#useSSL(boolean)}
     * is set to true. The default protocol is TLS. Supported protocols are SSL and TLS.
     *
     * @param protocol TLS or SSL (TLS is the default)
     * @return {@link UploadRequest}
     */
    public UploadRequest setSecureSocketProtocol(String protocol) {
        params.secureSocketProtocol = protocol;
        return this;
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IUploadInterface sub = IUploadInterface.Stub.asInterface(service);
            try {
                sub.registerDelegate(delegate, params.id);
                sub.upload(params);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

}
