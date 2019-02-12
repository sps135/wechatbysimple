package com.simple.imlib

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Environment
import android.os.Handler
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.proto.imlib.Muduo
import com.simple.imlib.constant.Constant
import com.simple.imlib.listener.*
import com.simple.imlib.task.*
import com.simple.upload.ServerResponse
import com.simple.upload.UploadInfo
import com.simple.upload.UploadRequest
import com.simple.upload.UploadStatusDelegate
import com.tencent.mars.app.AppLogic
import com.tencent.mars.wrapper.remote.MarsServiceProxy
import com.tencent.mars.wrapper.service.DebugMarsServiceProfile
import com.tencent.mars.wrapper.service.MarsServiceNative
import com.tencent.mars.xlog.Log
import com.tencent.mars.xlog.Xlog
import java.util.*

class IMClient {

    private var mContext: Context? = null

    private var mainHandler: Handler? = null

    private val accountInfo = AppLogic.AccountInfo(
            Random(System.currentTimeMillis() / 1000).nextInt().toLong(), "anonymous")

    private var mStatus: Int = 0

    private class SampleMarsServiceProfile : DebugMarsServiceProfile() {

        override fun longLinkHost(): String {
            return "marsopen.cn"
        }
    }

    fun sendImageMessage(uploadId: String?,
                         fromUserId: Int,
                         targetId: Int,
                         createAt: Long,
                         path: String,
                         conversationType: Int,
                         listener: IUploadImageListener) {
        upload(path, uploadId, object : IUploadServiceListener {
            override fun onStart(uploadId: String) {
                mainHandler!!.post {
                    try {
                        listener.onStart(uploadId)
                    } catch (e: Exception) {
                        listener.onError(uploadId)
                    }
                }
            }

            override fun onProgress(uploadId: String, percent: Int) {
                mainHandler!!.post {
                    try {
                        if (percent == 100) {
                            listener.onProgress(uploadId, 99)
                        } else {
                            listener.onProgress(uploadId, percent)
                        }

                    } catch (e: Exception) {
                        listener.onError(uploadId)
                    }
                }
            }

            override fun onCancel(uploadId: String) {
                mainHandler!!.post {
                    try {
                        listener.onCancel(uploadId)
                    } catch (e: Exception) {
                        listener.onError(uploadId)
                    }
                }
            }

            override fun onComplete(uploadId: String, url: String) {
                val type: Muduo.IMMessage.ConversationType
                if (conversationType == Constant.PRIVATE_MESSAGE) {
                    type = Muduo.IMMessage.ConversationType.PRIVATE
                } else {
                    type = Muduo.IMMessage.ConversationType.GROUP
                }
                val jsnObj = JsonObject()
                jsnObj.addProperty("imageUri", url)
                MarsServiceProxy.send(MessageTaskWrapper(
                        uploadId,
                        fromUserId,
                        targetId,
                        createAt,
                        jsnObj.toString(),
                        Muduo.IMMessage.MessageType.IMAGE_MESSGAGE,
                        type,
                        object : ITaskWrapperListener {
                            override fun onComplete() {
                                mainHandler!!.post {
                                    listener.onComplete(uploadId)
                                }
                            }

                            override fun onError(message: String) {
                                mainHandler!!.post {
                                    listener.onError(uploadId)
                                }
                            }
                        }))
            }


            override fun onError(uploadId: String, message: String) {
                mainHandler!!.post {
                    try {
                        listener.onError(uploadId)
                    } catch (e: Exception) {
                        listener.onError(uploadId)
                    }
                }
            }
        })
    }

    fun sendVoiceMessage(uploadId: String?,
                         fromUserId: Int,
                         targetId: Int,
                         createAt: Long,
                         conversationType: Int,
                         duration: Int,
                         path: String,
                         listener: IUploadVoiceListener) {
        upload(path, uploadId, object : IUploadServiceListener {
            override fun onStart(uploadId: String) {
                mainHandler!!.post {
                    try {
                        listener.onStart(uploadId)
                    } catch (e: Exception) {
                        listener.onError(uploadId, e.message!!)
                    }
                }
            }

            override fun onProgress(uploadId: String, percent: Int) {
            }

            override fun onCancel(uploadId: String) {
            }

            override fun onComplete(uploadId: String, url: String) {
                val type: Muduo.IMMessage.ConversationType
                if (conversationType == Constant.PRIVATE_MESSAGE) {
                    type = Muduo.IMMessage.ConversationType.PRIVATE
                } else {
                    type = Muduo.IMMessage.ConversationType.GROUP
                }
                val jsnObj = JsonObject()
                jsnObj.addProperty("audioUri", url)
                jsnObj.addProperty("audioDuration", duration)

                MarsServiceProxy.send(MessageTaskWrapper(
                        uploadId,
                        fromUserId,
                        targetId,
                        createAt,
                        jsnObj.toString(),
                        Muduo.IMMessage.MessageType.VOICE_MESSGAGE,
                        type,
                        object : ITaskWrapperListener {
                            override fun onComplete() {
                                mainHandler!!.post {
                                    listener.onComplete(uploadId)
                                }
                            }

                            override fun onError(message: String) {
                                mainHandler!!.post {
                                    listener.onError(uploadId, message)
                                }
                            }
                        }))
            }

            override fun onError(uploadId: String, message: String) {
                mainHandler!!.post {
                    listener.onError(uploadId, message)
                }
            }
        })
    }

    fun setConnectStatus(status: Int) {
        mStatus = status
    }

    fun getConnectStatus(): Int {
        return mStatus
    }

    fun sendTextMessage(uploadId: String?,
                        fromUserId: Int,
                        targetId: Int,
                        conversationType: Int,
                        createAt: Long,
                        message: String,
                        listener: ISendTextMessageListener) {
        var id = ""
        if (uploadId == null) {
            id = UUID.randomUUID().toString()
        } else {
            id = uploadId
        }
        listener.onStart(id)

        val type: Muduo.IMMessage.ConversationType
        if (conversationType == Constant.PRIVATE_MESSAGE) {
            type = Muduo.IMMessage.ConversationType.PRIVATE
        } else {
            type = Muduo.IMMessage.ConversationType.GROUP
        }
        val jsnObj = JsonObject()
        jsnObj.addProperty("message", String(Base64.encode(message.toByteArray(), Base64.DEFAULT)))

        MarsServiceProxy.send(MessageTaskWrapper(
                id,
                fromUserId,
                targetId,
                createAt,
                jsnObj.toString(),
                Muduo.IMMessage.MessageType.TEXT_MESSGAGE,
                type,
                object : ITaskWrapperListener {
                    override fun onComplete() {
                        mainHandler!!.post {
                            listener.onComplete(id)
                        }
                    }

                    override fun onError(message: String) {
                        mainHandler!!.post {
                            listener.onError(id, message)
                        }
                    }
                }))
    }

    fun setForeground(foreground: Boolean) {
        MarsServiceProxy.inst.setForeground(foreground)
    }

    fun connect(userId: Int) {
        MarsServiceProxy.inst.setAccountInfo(userId)
        MarsServiceProxy.inst.connect()
    }

    fun getHistoryMessage(userId: Int) {
        MarsServiceProxy.send(HistoryTaskWrapper(userId))
    }

    fun disconnect() {
        MarsServiceProxy.inst.disconnect()
    }

    fun init(context: Application) {
        mContext = context.applicationContext
        mainHandler = Handler(mContext!!.mainLooper)

        System.loadLibrary("stlport_shared")
        System.loadLibrary("marsxlog")

        openXlog()

        MarsServiceNative.setProfileFactory { SampleMarsServiceProfile() }

        MarsServiceProxy.init(mContext, null)
        MarsServiceProxy.inst.accountInfo = accountInfo

        val pushService = PushService(mContext)
        MarsServiceProxy.setOnPushMessageListener(Constant.LOG_OUT_CMDID, pushService)
        MarsServiceProxy.setOnPushMessageListener(Constant.MESSAGE_CMDID, pushService)
    }

    fun openXlog() {
        val pid = android.os.Process.myPid()
        var processName: String? = null
        val am = mContext!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (appProcess in am.runningAppProcesses) {
            if (appProcess.pid == pid) {
                processName = appProcess.processName
                break
            }
        }

        if (processName == null) {
            return
        }

        val SDCARD = Environment.getExternalStorageDirectory().absolutePath
        val logPath = "$SDCARD/marssample/log"

        val logFileName = if (processName.indexOf(":") == -1) "MarsSample" else "MarsSample_" + processName.substring(processName.indexOf(":") + 1)

        if (BuildConfig.DEBUG) {
            Xlog.appenderOpen(Xlog.LEVEL_VERBOSE, Xlog.AppednerModeAsync, "", logPath, logFileName, "")
            Xlog.setConsoleLogOpen(true)
        } else {
            Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, "", logPath, logFileName, "")
            Xlog.setConsoleLogOpen(false)
        }
        Log.setLogImp(Xlog())
    }

    private fun upload(path: String, uploadId: String?, listener: IUploadServiceListener) {
        val fileName = getFileName(path)
        val extName = getExtensionName(path);
        UploadRequest(mContext!!.applicationContext, uploadId, Constant.FTP_SERVER_URL, Constant.FTP_SERVER_PORT)
                .setUsernameAndPassword(Constant.FTP_USERNAME, Constant.FTP_PASSWORD)
                .setMaxRetries(0)
                .setDelegate(object : UploadStatusDelegate.Stub() {
                    override fun onStart(id: String?) {
                        listener.onStart(id!!)
                    }

                    override fun onProgress(uploadInfo: UploadInfo?) {
                        listener.onProgress(uploadInfo!!.uploadId, uploadInfo!!.progressPercent)
                    }

                    override fun onError(uploadInfo: UploadInfo?, serverResponse: ServerResponse?, exception: String?) {
                        listener.onError(uploadInfo!!.uploadId, exception!!)
                    }

                    override fun onCompleted(uploadInfo: UploadInfo?, serverResponse: ServerResponse?) {
                        listener.onComplete(uploadInfo!!.uploadId, "${Constant.FTP_SERVER_BASE_URL}/${fileName}.${extName}")
                    }

                    override fun onCancelled(uploadInfo: UploadInfo?) {
                        listener.onCancel(uploadInfo!!.uploadId)
                    }
                })
                .addFileToUpload(path, "/pub/${fileName}.${extName}")
                .startUpload()
    }

    private fun getExtensionName(filename: String?): String? {
        if (filename != null && filename.length > 0) {
            val dot = filename.lastIndexOf('.')
            if (dot > -1 && dot < filename.length - 1) {
                return filename.substring(dot + 1)
            }
        }
        return filename
    }

    private fun getFileName(path: String): String? {
        val start = path.lastIndexOf("/")
        val end = path.lastIndexOf(".")
        return if (start != -1 && end != -1) {
            path.substring(start + 1, end)
        } else {
            null
        }
    }

    companion object {
        private var INSTANCE: IMClient? = null

        @JvmStatic
        fun getInstance(): IMClient {
            if (INSTANCE == null) {
                synchronized(IMClient::class.java) {
                    INSTANCE = IMClient()
                }
            }
            return INSTANCE!!
        }
    }
}