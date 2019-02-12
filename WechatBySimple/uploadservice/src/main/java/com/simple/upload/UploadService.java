package com.simple.upload;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 文件上传服务
 *
 * @author simple
 */
public final class UploadService extends Service {

    private static final String TAG = UploadService.class.getSimpleName();

    public static int UPLOAD_POOL_SIZE = 2;

    public static int KEEP_ALIVE_TIME_IN_SECONDS = 5;

    public static int IDLE_TIMEOUT = 10 * 1000;

    public static boolean EXECUTE_IN_FOREGROUND = true;

    public static String NAMESPACE = "net.gotev";

    public static int BUFFER_SIZE = 4096;

    public static int INITIAL_RETRY_WAIT_TIME = 1000;

    public static int BACKOFF_MULTIPLIER = 2;

    public static int MAX_RETRY_WAIT_TIME = 10 * 10 * 1000;

    protected static final int UPLOAD_NOTIFICATION_BASE_ID = 1234; // Something unique

    protected static final long PROGRESS_REPORT_INTERVAL = 166;

    // constants used in the intent which starts this service
    private static final String ACTION_UPLOAD_SUFFIX = ".uploadservice.action.upload";
    protected static final String PARAM_TASK_PARAMETERS = "taskParameters";
    protected static final String PARAM_TASK_CLASS = "taskClass";

    // constants used in broadcast intents
    private static final String BROADCAST_ACTION_SUFFIX = ".uploadservice.broadcast.status";
    protected static final String PARAM_BROADCAST_DATA = "broadcastData";

    // internal variables
    private PowerManager.WakeLock wakeLock;
    private int notificationIncrementalId = 0;
    private static final Map<String, UploadTask> uploadTasksMap = new ConcurrentHashMap<>();
    private static final Map<String, WeakReference<UploadStatusDelegate>> uploadDelegates = new ConcurrentHashMap<>();
    private final BlockingQueue<Runnable> uploadTasksQueue = new LinkedBlockingQueue<>();
    private static volatile String foregroundUploadId = null;
    private ThreadPoolExecutor uploadThreadPool;

    protected static String getActionUpload() {
        return NAMESPACE + ACTION_UPLOAD_SUFFIX;
    }

    protected static String getActionBroadcast() {
        return NAMESPACE + BROADCAST_ACTION_SUFFIX;
    }

    /**
     * Stops the upload task with the given uploadId.
     *
     * @param uploadId The unique upload id
     */
    public static synchronized void stopUpload(final String uploadId) {
        UploadTask removedTask = uploadTasksMap.get(uploadId);
        if (removedTask != null) {
            removedTask.cancel();
        }
    }

    /**
     * Stop all the active uploads.
     */
    public static synchronized void stopAllUploads() {
        if (uploadTasksMap.isEmpty()) {
            return;
        }

        // using iterator instead for each loop, because it's faster on Android
        Iterator<String> iterator = uploadTasksMap.keySet().iterator();

        while (iterator.hasNext()) {
            UploadTask taskToCancel = uploadTasksMap.get(iterator.next());
            taskToCancel.cancel();
        }
    }

    /**
     * Stops the service if no upload tasks are currently running
     *
     * @param context application context
     * @return true if the service is getting stopped, false otherwise
     */
    public static synchronized boolean stop(final Context context) {
        return stop(context, false);
    }

    /**
     * Stops the service.
     *
     * @param context   application context
     * @param forceStop stops the service no matter if some tasks are running
     * @return true if the service is getting stopped, false otherwise
     */
    public static synchronized boolean stop(final Context context, boolean forceStop) {
        if (forceStop) {
            return context.stopService(new Intent(context, UploadService.class));
        }
        return uploadTasksMap.isEmpty() && context.stopService(new Intent(context, UploadService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.setReferenceCounted(false);

        if (!wakeLock.isHeld())
            wakeLock.acquire();

        if (UPLOAD_POOL_SIZE <= 0) {
            UPLOAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
        }

        // Creates a thread pool manager
        uploadThreadPool = new ThreadPoolExecutor(
                UPLOAD_POOL_SIZE,       // Initial pool size
                UPLOAD_POOL_SIZE,       // Max pool size
                KEEP_ALIVE_TIME_IN_SECONDS,
                TimeUnit.SECONDS,
                uploadTasksQueue);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopAllUploads();
        uploadThreadPool.shutdown();

        if (wakeLock.isHeld())
            wakeLock.release();

        uploadTasksMap.clear();
        uploadDelegates.clear();

        Logger.debug(TAG, "UploadService destroyed");
    }


    UploadTask getTask(FTPUploadTaskParameters parameters) {

        UploadTask uploadTask;

        uploadTask = new UploadTask();
        try {
            uploadTask.init(this, parameters);
        } catch (Exception e) {
            return null;
        }


        return uploadTask;
    }

    /**
     * Called by each task when it is completed (either successfully, with an error or due to
     * user cancellation).
     *
     * @param uploadId the uploadID of the finished task
     */
    protected synchronized void taskCompleted(String uploadId) {
        uploadTasksMap.remove(uploadId);
        uploadDelegates.remove(uploadId);
    }

    /**
     * Sets the delegate which will receive the events for the given upload request.
     * Those events will not be sent in broadcast, but only to the delegate.
     *
     * @param uploadId uploadID of the upload request
     * @param delegate the delegate instance
     */
    protected static void setUploadStatusDelegate(String uploadId, UploadStatusDelegate delegate) {
        if (delegate == null)
            return;

        uploadDelegates.put(uploadId, new WeakReference<>(delegate));
    }

    /**
     * Gets the delegate for an upload request.
     *
     * @param uploadId uploadID of the upload request
     * @return {@link UploadStatusDelegate} or null if no delegate has been set for the given
     * uploadId
     */
    protected static UploadStatusDelegate getUploadStatusDelegate(String uploadId) {
        WeakReference<UploadStatusDelegate> reference = uploadDelegates.get(uploadId);

        if (reference == null)
            return null;

        UploadStatusDelegate delegate = reference.get();

        if (delegate == null) {
            uploadDelegates.remove(uploadId);
            Logger.info(TAG, "\n\n\nUpload delegate for upload with Id " + uploadId + " is gone!\n" +
                    "Probably you have set it in an activity and the user navigated away from it\n" +
                    "before the upload was completed. From now on, the events will be dispatched\n" +
                    "with broadcast intents. If you see this message, consider switching to the\n" +
                    "UploadServiceBroadcastReceiver registered globally in your manifest.\n" +
                    "Read this:\n" +
                    "https://github.com/gotev/android-upload-service/wiki/Monitoring-upload-status\n");
        }

        return delegate;
    }

    private IBinder mBinder = new IUploadInterface.Stub() {
        @Override
        public void upload(FTPUploadTaskParameters params) throws RemoteException {
            UploadTask currentTask = getTask(params);

            notificationIncrementalId += 2;

            uploadTasksMap.put(currentTask.params.id, currentTask);
            uploadThreadPool.execute(currentTask);
        }

        @Override
        public void registerDelegate(UploadStatusDelegate delegate, String id) throws RemoteException {
            setUploadStatusDelegate(id, delegate);
            delegate.onStart(id);
        }
    };
}
