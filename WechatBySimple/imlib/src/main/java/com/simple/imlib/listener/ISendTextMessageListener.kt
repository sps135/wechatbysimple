package com.simple.imlib.listener

interface ISendTextMessageListener {

    fun onStart(messageId: String)

    fun onComplete(messageId: String)

    fun onError(messageId: String, errorMessage: String)
}