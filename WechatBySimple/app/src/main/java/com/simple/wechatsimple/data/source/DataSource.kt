package com.simple.wechatsimple.data.source

import com.simple.wechatsimple.model.databse.UserModel
import io.reactivex.Observable


interface DataSource {

    fun login(name: String, password: String): Observable<UserModel>? {
        return null
    }

    fun getUserList(): Observable<List<UserModel>>? {
        return null
    }

    fun getUserInfo(userId: Int): Observable<UserModel>? {
        return null
    }
}