package com.simple.imlib.listener

interface IUploadImageListener {

    fun onStart(messageId: String)

    fun onProgress(messageId: String, percent: Int)

    fun onCancel(messageId: String)

    fun onComplete(messageId: String)

    fun onError(message: String)
}