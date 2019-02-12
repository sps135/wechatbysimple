package com.simple.wechatsimple.data.source

import com.simple.wechatsimple.data.source.local.LocalDataSource
import com.simple.wechatsimple.data.source.remote.RemoteDataSource
import com.simple.wechatsimple.model.databse.UserModel
import io.reactivex.Observable

class DataSourceRepository(val remoteDataSource: RemoteDataSource,
                           val localDataSource: LocalDataSource) : DataSource {

    override fun login(name: String, password: String): Observable<UserModel>? {
        return remoteDataSource.login(name, password)
    }

    override fun getUserList(): Observable<List<UserModel>>? {
        return remoteDataSource.getUserList()
    }

    override fun getUserInfo(userId: Int): Observable<UserModel>? {
        return remoteDataSource.getUserInfo(userId)
    }

    companion object {
        private var INSTANCE: DataSourceRepository? = null

        @JvmStatic
        fun getInstance(remoteSource: RemoteDataSource,
                        localSource: LocalDataSource) =
                INSTANCE ?: synchronized(DataSourceRepository::class.java) {
                    INSTANCE ?: DataSourceRepository(remoteSource, localSource).also {
                        INSTANCE = it
                    }
                }
    }
}