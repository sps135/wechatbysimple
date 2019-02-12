package com.simple.wechatsimple.data.source.local

import com.simple.wechatsimple.data.source.DataSource
import com.simple.wechatsimple.data.source.excutor.AppExecutors
import com.simple.wechatsimple.model.databse.UserModel
import io.reactivex.Observable

class LocalDataSource private constructor(val appExecutors: AppExecutors) : DataSource {

    override fun getUserInfo(userId: Int): Observable<UserModel>? {
        return super.getUserInfo(userId)
    }

    companion object {
        private var INSTANCE: LocalDataSource? = null

        @JvmStatic
        fun getInstance(appExecutors: AppExecutors): LocalDataSource {
            if (INSTANCE == null) {
                synchronized(LocalDataSource::class.java) {
                    INSTANCE = LocalDataSource(appExecutors)
                }
            }
            return INSTANCE!!
        }
    }
}