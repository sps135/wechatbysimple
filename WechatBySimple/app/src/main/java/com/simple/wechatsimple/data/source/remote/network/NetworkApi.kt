package com.simple.wechatsimple.data.source.remote.network

import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path


interface NetworkApi {

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("*/*")
    fun login(@Body data: RequestBody): Observable<BaseResponseBodyEntity>

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("*/*")
    fun userList(@Body data: RequestBody): Observable<BaseResponseBodyEntity>

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("*/*")
    fun userInfo(@Body data: RequestBody): Observable<BaseResponseBodyEntity>
}