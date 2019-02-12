package com.simple.wechatsimple.util

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.simple.wechatsimple.contacts.ContactsViewModel
import com.simple.wechatsimple.data.source.DataSourceRepository
import com.simple.wechatsimple.data.source.inject.Injection
import com.simple.wechatsimple.login.LoginViewModel
import com.simple.wechatsimple.recent.RecentViewModel
import com.simple.wechatsimple.session.SessionViewModel

class ViewModelFactory private constructor(
        private val application: Application,
        private val dataSourceRepository: DataSourceRepository) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>) =
            with(modelClass) {
                when {
                    isAssignableFrom(LoginViewModel::class.java) ->
                        LoginViewModel(application, dataSourceRepository)
                    isAssignableFrom(ContactsViewModel::class.java) ->
                        ContactsViewModel(application, dataSourceRepository)
                    isAssignableFrom(SessionViewModel::class.java) ->
                        SessionViewModel(application, dataSourceRepository)
                    isAssignableFrom(RecentViewModel::class.java) ->
                        RecentViewModel(application, dataSourceRepository)
                    else ->
                        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                } as T
            }

    companion object {
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        fun getInstance(application: Application) = INSTANCE
                ?: synchronized(ViewModelFactory::class.java) {
                    INSTANCE
                            ?: ViewModelFactory(application,
                                    Injection.provideDataRepository())
                                    .also {
                                        INSTANCE = it
                                    }
                }
    }
}