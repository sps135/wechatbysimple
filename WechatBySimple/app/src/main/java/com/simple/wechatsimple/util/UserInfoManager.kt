package com.simple.wechatsimple.util

import com.simple.imlib.constant.Constant
import com.simple.wechatsimple.model.databse.UserModel
import io.realm.Realm
import org.greenrobot.eventbus.EventBus

class UserInfoManager private constructor(private val userCacheMap: CacheMap<Int, UserModel>) {

    private var userInfoProvider: UserInfoProvider? = null

    fun getUserInfo(userId: Int): UserModel? {
        if (userId == 0) {
            return null
        }

        var userInfo: UserModel?
        userInfo = userCacheMap[userId]
        if (userInfo == null) {
            userInfo = Realm
                    .getDefaultInstance()
                    .where(UserModel::class.java)
                    .equalTo("uid", userId)
                    .findFirst()
        }

        return userInfo
    }

    fun getSelfUserInfo(): UserModel? {
        return Realm
                .getDefaultInstance()
                .where(UserModel::class.java)
                .equalTo("isSelf", true)
                .findFirst()
    }

    fun clearSelfInfo() {
        val user = Realm
                .getDefaultInstance()
                .where(UserModel::class.java)
                .equalTo("isSelf", true)
                .findFirst()
        if (user != null) {
            Realm.getDefaultInstance().executeTransaction {
                user.deleteFromRealm()
            }
        }
    }

    fun getNickName(userId: Int): String {
        if (userId == 0) {
            return ""
        }

        val user = getUserInfo(userId)

        if (user != null) {
            return user.nickname
        }

        userInfoProvider!!.provide(userId)
        return ""
    }

    fun getPortrait(userId: Int): String {
        if (userId == 0) {
            return Constant.DEFAULT_PORTRAIT_URL
        }


        val user = getUserInfo(userId)

        if (user != null) {
            return user.portrait
        }
        userInfoProvider!!.provide(userId)
        return Constant.DEFAULT_PORTRAIT_URL
    }

    fun refreshUserInfo(user: UserModel) {
        userCacheMap.put(user.uid, user)

        Realm.getDefaultInstance().executeTransaction {
            it.copyToRealmOrUpdate(user)
        }

        EventBus.getDefault().post(user)
    }

    fun setUserInfoProvider(userInfoProvider: UserInfoProvider) {
        this.userInfoProvider = userInfoProvider
    }

    fun clearUserInfoProvider() {
        userInfoProvider = null
    }

    companion object {
        private var INSTANCE: UserInfoManager? = null

        @JvmStatic
        fun getInstance(): UserInfoManager {
            if (INSTANCE == null) {
                synchronized(UserInfoManager::class.java) {
                    INSTANCE = UserInfoManager(CacheMap(256))
                }
            }
            return INSTANCE!!
        }
    }

    interface UserInfoProvider {
        fun provide(userId: Int)
    }
}