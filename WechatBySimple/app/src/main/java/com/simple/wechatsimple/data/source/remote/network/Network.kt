package com.simple.wechatsimple.data.source.remote.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

const val URL = "http://XXX.XXX.XXX.XXX:XXXX"

class Network {

    companion object {
        private var networkApi: NetworkApi? = null

        @JvmStatic
        fun getNetworkApi(): NetworkApi {
            synchronized(Network::class.java) {
                if (networkApi == null) {
                    val logInterceptor = HttpLoggingInterceptor()
                    logInterceptor.level = HttpLoggingInterceptor.Level.BODY
                    val okHttpClient = OkHttpClient()
                            .newBuilder()
                            .retryOnConnectionFailure(true)
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .addInterceptor {
                                val request = it
                                        .request()
                                        .newBuilder()
                                        .addHeader("Connection", "close")
                                        .build()
                                it.proceed(request)
                            }
                            .addInterceptor(logInterceptor)
                            .build()
                    val retrofit = Retrofit
                            .Builder()
                            .client(okHttpClient)
                            .baseUrl(URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            .build()
                    networkApi = retrofit.create(NetworkApi::class.java)
                }
            }
            return networkApi!!
        }
    }
}