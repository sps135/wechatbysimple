package com.simple.wechatsimple.data.source.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.simple.wechatsimple.data.source.DataSource
import com.simple.wechatsimple.data.source.excutor.AppExecutors
import com.simple.wechatsimple.data.source.local.LocalDataSource
import com.simple.wechatsimple.data.source.remote.network.BaseRequestBodyEntity
import com.simple.wechatsimple.data.source.remote.network.BaseResponseBodyEntity
import com.simple.wechatsimple.data.source.remote.network.Network
import com.simple.wechatsimple.util.MD5
import com.simple.wechatsimple.model.databse.UserModel
import com.simple.wechatsimple.util.GsonUtil
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody

class RemoteDataSource private constructor(val appExecutors: AppExecutors) : DataSource {

    private val JSON = MediaType.parse("application/json; charset=utf-8")

    private val LOGIN_CODE = 1001

    private val CONTACTS_CODE = 1002

    private val USER_INFO_CODE = 1003

    override fun login(name: String, password: String): Observable<UserModel>? {

        val loginRequestData = JsonObject().apply {
            addProperty("username", name)
            addProperty("password", MD5.md5(password))
        }

        return Network
                .getNetworkApi()
                .login(packageData(BaseRequestBodyEntity(loginRequestData.toString(), LOGIN_CODE)))
                .compose(resultTransform<UserModel>())
                .compose(threadTransform())
    }

    override fun getUserInfo(userId: Int): Observable<UserModel>? {
        val userInfoRequestData = JsonObject().apply {
            addProperty("userId", userId)
        }

        return Network
                .getNetworkApi()
                .userInfo(packageData(BaseRequestBodyEntity(userInfoRequestData.toString(), USER_INFO_CODE)))
                .compose(resultTransform<UserModel>())
                .compose(threadTransform())
    }

    override fun getUserList(): Observable<List<UserModel>>? {
        val requestData = JsonObject()

        return Network
                .getNetworkApi()
                .userList(packageData(BaseRequestBodyEntity(requestData.toString(), CONTACTS_CODE)))
                .compose(resultListTransform<UserModel>())
                .compose(threadTransform())
    }

    private fun <T> threadTransform(): ObservableTransformer<T, T> =
            ObservableTransformer {
                it.subscribeOn(Schedulers.from(appExecutors.networkIO))
                        .observeOn(AndroidSchedulers.mainThread())
            }

    private inline fun <reified T : Any> resultTransform(): ObservableTransformer<BaseResponseBodyEntity, T> = ObservableTransformer {
        it.flatMap {
            if (!it.success) {
                throw IllegalArgumentException(it.message)
            }
            val data = gson!!.fromJson<T>(it.data)
            Observable.just(data)
        }
    }

    private inline fun <reified T : Any> resultListTransform(): ObservableTransformer<BaseResponseBodyEntity, List<T>> = ObservableTransformer {
        it.flatMap {
            if (!it.success) {
                throw IllegalArgumentException(it.message)
            }
            Observable.just(GsonUtil.parseString2List<T>(it.data, T::class.java))
        }
    }

    private inline fun <reified T : Any> Gson.fromJson(json: String): T {
        return Gson().fromJson(json, T::class.java)
    }

    private fun packageData(data: BaseRequestBodyEntity) =
            RequestBody.create(JSON, gson!!.toJson(data))

    companion object {
        private var INSTANCE: RemoteDataSource? = null

        private var gson: Gson? = null

        @JvmStatic
        fun getInstance(appExecutors: AppExecutors): RemoteDataSource {
            if (INSTANCE == null) {
                synchronized(LocalDataSource::class.java) {
                    INSTANCE = RemoteDataSource(appExecutors)
                    gson = Gson()
                }
            }
            return INSTANCE!!
        }
    }

}