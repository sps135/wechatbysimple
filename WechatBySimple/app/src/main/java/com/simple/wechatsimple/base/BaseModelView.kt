package com.simple.wechatsimple.base

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import java.net.SocketTimeoutException

open class BaseModelView(context: Application) : AndroidViewModel(context) {

    val showLoadingEvent = SingleLiveEvent<Void>()

    val hideLoadingEvent = SingleLiveEvent<Void>()

    val showMessageEvent = SingleLiveEvent<String>()

    protected val mDisposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        mDisposable.dispose()
    }

    inner class DefaultErrorCallback : Consumer<Throwable> {
        override fun accept(t: Throwable?) {
            hideLoadingEvent.call()
            when (t) {
                is SocketTimeoutException -> showMessageEvent.value = "链接失败，请检查你的网络设置"
                else -> showMessageEvent.value = t!!.message
            }
        }
    }

}