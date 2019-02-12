package com.simple.imlib.listener

interface IUploadServiceListener {
    fun onStart(uploadId: String)

    fun onProgress(uploadId: String, percent: Int)

    fun onCancel(uploadId: String)

    fun onComplete(uploadId: String, url: String)

    fun onError(uploadId: String, message: String)
}