package com.simple.upload;

import android.os.RemoteException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class UploadTask implements Runnable, CopyStreamListener {

    private static final String LOG_TAG = UploadTask.class.getSimpleName();

    protected static final int TASK_COMPLETED_SUCCESSFULLY = 200;

    protected static final byte[] EMPTY_RESPONSE = "".getBytes(Charset.forName("UTF-8"));

    protected static final String PARAM_REMOTE_PATH = "ftpRemotePath";
    protected static final String PARAM_PERMISSIONS = "ftpPermissions";

    protected UploadService service;

    protected FTPUploadTaskParameters params = null;

    private final List<String> successfullyUploadedFiles = new ArrayList<>();

    protected boolean shouldContinue = true;

    private long lastProgressNotificationTime;

    protected long totalBytes;

    protected long uploadedBytes;

    private final long startTime;

    private int attempts;

    private FTPClient ftpClient = null;

    /**
     * Implementation of the upload logic.
     *
     * @throws Exception if an error occurs
     */
    protected void upload() throws Exception {
        try {
            if (params.useSSL) {
                String secureProtocol = params.secureSocketProtocol;

                if (secureProtocol == null || secureProtocol.isEmpty())
                    secureProtocol = FTPUploadTaskParameters.DEFAULT_SECURE_SOCKET_PROTOCOL;

                ftpClient = new FTPSClient(secureProtocol, params.implicitSecurity);

                Logger.debug(LOG_TAG, "Created FTP over SSL (FTPS) client with "
                        + secureProtocol + " protocol and "
                        + (params.implicitSecurity ? "implicit security" : "explicit security"));

            } else {
                ftpClient = new FTPClient();
            }

            ftpClient.setBufferSize(UploadService.BUFFER_SIZE);
            ftpClient.setCopyStreamListener(this);
            ftpClient.setDefaultTimeout(params.connectTimeout);
            ftpClient.setConnectTimeout(params.connectTimeout);
            ftpClient.setAutodetectUTF8(true);

            Logger.debug(LOG_TAG, "Connect timeout set to " + params.connectTimeout + "ms");

            Logger.debug(LOG_TAG, "Connecting to " + params.serverUrl
                    + ":" + params.port + " as " + params.username);
            try {
                ftpClient.connect(params.serverUrl, params.port);
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Connect error!");
            }

            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new Exception("Can't connect to " + params.serverUrl
                        + ":" + params.port
                        + ". The server response is: " + ftpClient.getReplyString());
            }

            if (!ftpClient.login(params.username, params.password)) {
                throw new Exception("Error while performing login on " + params.serverUrl
                        + ":" + params.port
                        + " with username: " + params.username
                        + ". Check your credentials and try again.");
            }

            // to prevent the socket timeout on the control socket during file transfer,
            // set the control keep alive timeout to a half of the socket timeout
            int controlKeepAliveTimeout = params.socketTimeout / 2 / 1000;

            ftpClient.setSoTimeout(params.socketTimeout);
            ftpClient.setControlKeepAliveTimeout(controlKeepAliveTimeout);
            ftpClient.setControlKeepAliveReplyTimeout(controlKeepAliveTimeout * 1000);

            Logger.debug(LOG_TAG, "Socket timeout set to " + params.socketTimeout
                    + "ms. Enabled control keep alive every " + controlKeepAliveTimeout + "s");

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(params.compressedFileTransfer ?
                    FTP.COMPRESSED_TRANSFER_MODE : FTP.STREAM_TRANSFER_MODE);

            // this is needed to calculate the total bytes and the uploaded bytes, because if the
            // request fails, the upload method will be called again
            // (until max retries is reached) to retry the upload, so it's necessary to
            // know at which status we left, to be able to properly notify firther progress.
            calculateUploadedAndTotalBytes();

            String baseWorkingDir = ftpClient.printWorkingDirectory();
            Logger.debug(LOG_TAG, "FTP default working directory is: " + baseWorkingDir);

            Iterator<UploadFile> iterator = new ArrayList<>(params.files).iterator();
            while (iterator.hasNext()) {
                UploadFile file = iterator.next();

                if (!shouldContinue)
                    break;

                uploadFile(baseWorkingDir, file);
                addSuccessfullyUploadedFile(file);
                iterator.remove();
            }

            // Broadcast completion only if the user has not cancelled the operation.
            if (shouldContinue) {
                broadcastCompleted(new ServerResponse(UploadTask.TASK_COMPLETED_SUCCESSFULLY,
                        UploadTask.EMPTY_RESPONSE, null));
            }

        } finally {
            if (ftpClient.isConnected()) {
                try {
                    Logger.debug(LOG_TAG, "Logout and disconnect from FTP server: "
                            + params.serverUrl + ":" + params.port);
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (Exception exc) {
                    Logger.error(LOG_TAG, "Error while closing FTP connection to: "
                            + params.serverUrl + ":" + params.port, exc);
                }
            }
            ftpClient = null;
        }
    }

    private void calculateUploadedAndTotalBytes() {
        uploadedBytes = 0;

        for (String filePath : getSuccessfullyUploadedFiles()) {
            uploadedBytes += new File(filePath).length();
        }

        totalBytes = uploadedBytes;

        for (UploadFile file : params.files) {
            totalBytes += file.length(service);
        }
    }

    private void uploadFile(String baseWorkingDir, UploadFile file) throws IOException {
        Logger.debug(LOG_TAG, "Starting FTP upload of: " + file.getName(service)
                + " to: " + file.getProperty(PARAM_REMOTE_PATH));

        String remoteDestination = file.getProperty(PARAM_REMOTE_PATH);

        if (remoteDestination.startsWith(baseWorkingDir)) {
            remoteDestination = remoteDestination.substring(baseWorkingDir.length());
        }

        makeDirectories(remoteDestination, params.createdDirectoriesPermissions);

        InputStream localStream = file.getStream(service);
        try {
            String remoteFileName = getRemoteFileName(file);
            if (!ftpClient.storeFile(remoteFileName, localStream)) {
                throw new IOException("Error while uploading: " + file.getName(service)
                        + " to: " + file.getProperty(PARAM_REMOTE_PATH));
            }

            setPermission(remoteFileName, file.getProperty(PARAM_PERMISSIONS));

        } finally {
            localStream.close();
        }

        // get back to base working directory
        if (!ftpClient.changeWorkingDirectory(baseWorkingDir)) {
            Logger.info(LOG_TAG, "Can't change working directory to: " + baseWorkingDir);
        }
    }

    private String getRemoteFileName(UploadFile file) {

        // if the remote path ends with /
        // it means that the remote path specifies only the directory structure, so
        // get the remote file name from the local file
        if (file.getProperty(PARAM_REMOTE_PATH).endsWith("/")) {
            return file.getName(service);
        }

        // if the remote path contains /, but it's not the last character
        // it means that I have something like: /path/to/myfilename
        // so the remote file name is the last path element (myfilename in this example)
        if (file.getProperty(PARAM_REMOTE_PATH).contains("/")) {
            String[] tmp = file.getProperty(PARAM_REMOTE_PATH).split("/");
            return tmp[tmp.length - 1];
        }

        // if the remote path does not contain /, it means that it specifies only
        // the remote file name
        return file.getProperty(PARAM_REMOTE_PATH);
    }

    private void makeDirectories(String dirPath, String permissions) throws IOException {
        if (!dirPath.contains("/")) return;

        String[] pathElements = dirPath.split("/");

        if (pathElements.length == 1) return;

        // if the string ends with / it means that the dir path contains only directories,
        // otherwise if it does not contain /, the last element of the path is the file name,
        // so it must be ignored when creating the directory structure
        int lastElement = dirPath.endsWith("/") ? pathElements.length : pathElements.length - 1;

        for (int i = 0; i < lastElement; i++) {
            String singleDir = pathElements[i];
            if (singleDir.isEmpty()) continue;

            if (!ftpClient.changeWorkingDirectory(singleDir)) {
                if (ftpClient.makeDirectory(singleDir)) {
                    Logger.debug(LOG_TAG, "Created remote directory: " + singleDir);
                    if (permissions != null) {
                        setPermission(singleDir, permissions);
                    }
                    ftpClient.changeWorkingDirectory(singleDir);
                } else {
                    throw new IOException("Unable to create remote directory: " + singleDir);
                }
            }
        }
    }

    private void setPermission(String remoteFileName, String permissions) {
        if (permissions == null || "".equals(permissions))
            return;

        // http://stackoverflow.com/questions/12741938/how-can-i-change-permissions-of-a-file-on-a-ftp-server-using-apache-commons-net
        try {
            if (ftpClient.sendSiteCommand("chmod " + permissions + " " + remoteFileName)) {
                Logger.error(LOG_TAG, "Error while setting permissions for: "
                        + remoteFileName + " to: " + permissions
                        + ". Check if your FTP user can set file permissions!");
            } else {
                Logger.debug(LOG_TAG, "Permissions for: " + remoteFileName + " set to: " + permissions);
            }
        } catch (IOException exc) {
            Logger.error(LOG_TAG, "Error while setting permissions for: "
                    + remoteFileName + " to: " + permissions
                    + ". Check if your FTP user can set file permissions!", exc);
        }
    }

    /**
     * Implement in subclasses to be able to do something when the upload is successful.
     */
    protected void onSuccessfulUpload() {
    }

    public UploadTask() {
        startTime = new Date().getTime();
    }

    protected void init(UploadService service, FTPUploadTaskParameters ftpUploadTaskParameters) throws IOException {
        this.params = ftpUploadTaskParameters;
        this.service = service;
    }

    @Override
    public final void run() {

        attempts = 0;

        int errorDelay = UploadService.INITIAL_RETRY_WAIT_TIME;

        while (attempts <= params.getMaxRetries() && shouldContinue) {
            attempts++;

            try {
                upload();
                break;

            } catch (Exception exc) {
                if (!shouldContinue) {
                    break;
                } else if (attempts > params.getMaxRetries()) {
                    broadcastError(exc);
                } else {
                    Logger.error(LOG_TAG, "Error in uploadId " + params.id
                            + " on attempt " + attempts
                            + ". Waiting " + errorDelay / 1000 + "s before next attempt. ", exc);

                    long beforeSleepTs = System.currentTimeMillis();

                    while (shouldContinue && System.currentTimeMillis() < (beforeSleepTs + errorDelay)) {
                        try {
                            Thread.sleep(2000);
                        } catch (Throwable ignored) {
                        }
                    }

                    errorDelay *= UploadService.BACKOFF_MULTIPLIER;
                    if (errorDelay > UploadService.MAX_RETRY_WAIT_TIME) {
                        errorDelay = UploadService.MAX_RETRY_WAIT_TIME;
                    }
                }
            }
        }

        if (!shouldContinue) {
            broadcastCancelled();
        }
    }

    /**
     * Sets the last time the notification was updated.
     * This is handled automatically and you should never call this method.
     *
     * @param lastProgressNotificationTime time in milliseconds
     * @return {@link UploadTask}
     */
    protected final UploadTask setLastProgressNotificationTime(long lastProgressNotificationTime) {
        this.lastProgressNotificationTime = lastProgressNotificationTime;
        return this;
    }

    /**
     * Broadcasts a progress update.
     *
     * @param uploadedBytes number of bytes which has been uploaded to the server
     * @param totalBytes    total bytes of the request
     */
    protected final void broadcastProgress(final long uploadedBytes, final long totalBytes) {

        long currentTime = System.currentTimeMillis();
        if (uploadedBytes < totalBytes && currentTime < lastProgressNotificationTime + UploadService.PROGRESS_REPORT_INTERVAL) {
            return;
        }

        setLastProgressNotificationTime(currentTime);

        Logger.debug(LOG_TAG, "Broadcasting upload progress for " + params.id
                + ": " + uploadedBytes + " bytes of " + totalBytes);

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                successfullyUploadedFiles,
                pathStringListFrom(params.files));

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.IN_PROGRESS)
                .setUploadInfo(uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
        if (delegate != null) {
            try {
                delegate.onProgress(uploadInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            service.sendBroadcast(data.getIntent());
        }

    }

    /**
     * Broadcasts a completion status update and informs the {@link UploadService} that the task
     * executes successfully.
     * Call this when the task has completed the upload request and has received the response
     * from the server.
     *
     * @param response response got from the server
     */
    protected final void broadcastCompleted(final ServerResponse response) {

        final boolean successfulUpload = response.getHttpCode() >= 200 && response.getHttpCode() < 400;

        if (successfulUpload) {
            onSuccessfulUpload();

            if (params.autoDeleteSuccessfullyUploadedFiles && !successfullyUploadedFiles.isEmpty()) {
                for (String filePath : successfullyUploadedFiles) {
                    deleteFile(new File(filePath));
                }
            }
        }

        Logger.debug(LOG_TAG, "Broadcasting upload " + (successfulUpload ? "completed" : "error")
                + " for " + params.id);

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                successfullyUploadedFiles,
                pathStringListFrom(params.files));

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
        if (delegate != null) {
            try {
                if (successfulUpload) {
                    delegate.onCompleted(uploadInfo, response);
                } else {
                    delegate.onError(uploadInfo, response, null);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            BroadcastData data = new BroadcastData()
                    .setStatus(successfulUpload ? BroadcastData.Status.COMPLETED : BroadcastData.Status.ERROR)
                    .setUploadInfo(uploadInfo)
                    .setServerResponse(response);

            service.sendBroadcast(data.getIntent());
        }

        service.taskCompleted(params.id);
    }

    /**
     * Broadcast a cancelled status.
     * This called automatically by {@link UploadTask} when the user cancels the request,
     * and the specific implementation of {@link UploadTask#upload()} either
     * returns or throws an exception. You should never call this method explicitly in your
     * implementation.
     */
    protected final void broadcastCancelled() {

        Logger.debug(LOG_TAG, "Broadcasting cancellation for upload with ID: " + params.id);

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                successfullyUploadedFiles,
                pathStringListFrom(params.files));

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.CANCELLED)
                .setUploadInfo(uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
        if (delegate != null) {
            try {
                delegate.onCancelled(uploadInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        } else {
            service.sendBroadcast(data.getIntent());
        }

        service.taskCompleted(params.id);
    }

    /**
     * Add a file to the list of the successfully uploaded files and remove it from the file list
     *
     * @param file file on the device
     */
    protected final void addSuccessfullyUploadedFile(UploadFile file) {
        if (!successfullyUploadedFiles.contains(file.path)) {
            successfullyUploadedFiles.add(file.path);
            params.files.remove(file);
        }
    }

    /**
     * Adds all the files to the list of successfully uploaded files.
     * This will automatically remove them from the params.getFiles() list.
     */
    protected final void addAllFilesToSuccessfullyUploadedFiles() {
        for (Iterator<UploadFile> iterator = params.files.iterator(); iterator.hasNext(); ) {
            UploadFile file = iterator.next();

            if (!successfullyUploadedFiles.contains(file.path)) {
                successfullyUploadedFiles.add(file.path);
            }
            iterator.remove();
        }
    }

    /**
     * Gets the list of all the successfully uploaded files.
     * You must not modify this list in your subclasses! You can only read its contents.
     * If you want to add an element into it,
     * use {@link UploadTask#addSuccessfullyUploadedFile(UploadFile)}
     *
     * @return list of strings
     */
    protected final List<String> getSuccessfullyUploadedFiles() {
        return successfullyUploadedFiles;
    }

    /**
     * Broadcasts an error.
     * This called automatically by {@link UploadTask} when the specific implementation of
     * {@link UploadTask#upload()} throws an exception and there aren't any left retries.
     * You should never call this method explicitly in your implementation.
     *
     * @param exception exception to broadcast. It's the one thrown by the specific implementation
     *                  of {@link UploadTask#upload()}
     */
    private void broadcastError(final Exception exception) {

        Logger.info(LOG_TAG, "Broadcasting error for upload with ID: "
                + params.id + ". " + exception.getMessage());

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                successfullyUploadedFiles,
                pathStringListFrom(params.files));

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.ERROR)
                .setUploadInfo(uploadInfo)
                .setException(exception);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
        if (delegate != null) {
            try {
                delegate.onError(uploadInfo, null, exception.getMessage());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            service.sendBroadcast(data.getIntent());
        }

        service.taskCompleted(params.id);
    }

    /**
     * Tries to delete a file from the device.
     * If it fails, the error will be printed in the LogCat.
     *
     * @param fileToDelete file to delete
     * @return true if the file has been deleted, otherwise false.
     */
    private boolean deleteFile(File fileToDelete) {
        boolean deleted = false;

        try {
            deleted = fileToDelete.delete();

            if (!deleted) {
                Logger.error(LOG_TAG, "Unable to delete: "
                        + fileToDelete.getAbsolutePath());
            } else {
                Logger.info(LOG_TAG, "Successfully deleted: "
                        + fileToDelete.getAbsolutePath());
            }

        } catch (Exception exc) {
            Logger.error(LOG_TAG,
                    "Error while deleting: " + fileToDelete.getAbsolutePath() +
                            " Check if you granted: android.permission.WRITE_EXTERNAL_STORAGE", exc);
        }

        return deleted;
    }

    private static List<String> pathStringListFrom(List<UploadFile> files) {
        final List<String> filesLeft = new ArrayList<>(files.size());
        for (UploadFile f : files) {
            filesLeft.add(f.getPath());
        }
        return filesLeft;
    }

    public final void cancel() {
        this.shouldContinue = false;
    }

    @Override
    public void bytesTransferred(CopyStreamEvent event) {

    }

    @Override
    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
        uploadedBytes += bytesTransferred;
        broadcastProgress(uploadedBytes, totalBytes);

        if (!shouldContinue) {
            try {
                ftpClient.disconnect();
            } catch (Exception exc) {
                Logger.error(LOG_TAG, "Failed to abort current file transfer", exc);
            }
        }
    }
}
