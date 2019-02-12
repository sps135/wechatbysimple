package com.simple.wechatsimple

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.support.multidex.MultiDexApplication
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.lqr.emoji.LQREmotionKit
import com.simple.imlib.IMClient
import com.squareup.leakcanary.LeakCanary
import io.realm.Realm
import io.realm.RealmConfiguration

class MyApp : MultiDexApplication() {

    private val activities = mutableListOf<Activity>()

    var mMainThreadId: Long = 0//主线程id
    var mHandler: Handler? = null//主线程Handler

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        IMClient.getInstance().init(this)

        LQREmotionKit.init(this) { context: Context?, path: String?, imageView: ImageView? ->
            Glide
                    .with(context as Context)
                    .load(path)
                    .apply(RequestOptions.circleCropTransform())
                    .into(imageView as ImageView)
        }

        Realm.init(this)


        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        mMainThreadId = android.os.Process.myTid().toLong()
        mHandler = Handler()
        LeakCanary.install(this)
    }

    fun addActivity(activity: Activity) {
        activities.add(activity)
    }

    fun removeActivity(activity: Activity) {
        activities.remove(activity)
    }

    fun exit() {
        activities.forEach {
            it.finish()
        }
    }

    companion object {
        private lateinit var INSTANCE: MyApp

        @JvmStatic
        fun getInstance(): MyApp {
            return INSTANCE
        }

        @JvmStatic
        fun getLoginStateConfig(): RealmConfiguration {
            return RealmConfiguration.Builder().name("loginState.realm").build()
        }
    }
}