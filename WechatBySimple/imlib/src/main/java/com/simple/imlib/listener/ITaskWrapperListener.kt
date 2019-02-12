package com.simple.imlib.listener

interface ITaskWrapperListener {
    fun onComplete()

    fun onError(message: String)
}