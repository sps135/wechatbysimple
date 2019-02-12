package com.simple.wechatsimple.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.NotificationCompat
import android.util.Base64
import com.simple.imlib.constant.Constant
import com.simple.wechatsimple.R
import com.simple.wechatsimple.model.databse.MessageModel
import com.simple.wechatsimple.session.SessionActivity


class NotificationInterface {

    private val mMessageCacheList = HashMap<String, ArrayList<MessageModel>>()

    private val notification_id = "simple_id"

    private val notification_channel_name = "simple_channel_name"

    private val flags = Notification.FLAG_AUTO_CANCEL or Notification.FLAG_ONLY_ALERT_ONCE

    private var isForeground = false

    fun sendNotification(context: Context, message: MessageModel) {
        if (isForeground) {
            notifyByVibrator(context)
        } else {
            notifyByNotification(context, message)
        }
    }

    fun setIsForeground(isForeground: Boolean) {
        this.isForeground = isForeground
    }

    private fun notifyByNotification(context: Context, message: MessageModel) {
        val nm = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                    notification_id,
                    notification_channel_name,
                    NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(notificationChannel)
        }
        val pendingIntent = getPendingIntent(context, message)
        val title = UserInfoManager.getInstance().getNickName(message.targetId)

        var content = ""
        when (message.messageType) {
            Constant.VOICE_TYPE -> {
                content = "[语言消息]"
            }
            Constant.IMAGE_TYPE -> {
                content = "[图文消息]"
            }
            Constant.TEXT_TYPE -> {
                content = String(Base64.decode(message.textMessage, Base64.DEFAULT))
            }
        }

        val notification = getNotification(context, title, content, pendingIntent)
        nm.notify(message.targetId, notification)
    }

    private fun notifyByVibrator(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(1000)
    }

    private fun getPendingIntent(context: Context, message: MessageModel): PendingIntent {

        val intent = Intent().apply {
            val bundle = Bundle()
            bundle.putInt("targetId", message.fromUserId)
            bundle.putInt("conversationType", Constant.PRIVATE_MESSAGE)
            putExtras(bundle)
            setClass(context, SessionActivity::class.java)
        }
        return PendingIntent.getActivity(context, message.id.hashCode(), intent, flags)
    }

    private fun getNotification(context: Context, title: String, content: String, intent: PendingIntent): Notification {
        val builder = NotificationCompat.Builder(context)
        builder.setContentTitle(title)
                .setContentText(content)
                .setContentIntent(intent) //设置通知栏点击意图
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setAutoCancel(true)
                .setOngoing(false)
                .setChannelId(notification_id)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.wechat_notification)

        return builder.build()
    }

    companion object {
        private var INSTANCE: NotificationInterface? = null

        @JvmStatic
        fun getInstance(): NotificationInterface {
            if (INSTANCE == null) {
                synchronized(NotificationInterface::class.java) {
                    INSTANCE = NotificationInterface()
                }
            }
            return INSTANCE!!
        }
    }
}