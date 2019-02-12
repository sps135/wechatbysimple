package com.simple.wechatsimple.data.source.remote.network

data class BaseResponseBodyEntity(var success: Boolean,
                                  var errorCode: Int,
                                  var message: String,
                                  var data: String) {

}