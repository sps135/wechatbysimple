package com.simple.wechatsimple.data.source.inject

import com.simple.wechatsimple.data.source.DataSourceRepository
import com.simple.wechatsimple.data.source.excutor.AppExecutors
import com.simple.wechatsimple.data.source.local.LocalDataSource
import com.simple.wechatsimple.data.source.remote.RemoteDataSource

object Injection {

    fun provideDataRepository(): DataSourceRepository {
        return DataSourceRepository.getInstance(
                RemoteDataSource.getInstance(AppExecutors()),
                LocalDataSource.getInstance(AppExecutors()))
    }
}