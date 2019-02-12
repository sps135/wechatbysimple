package com.simple.wechatsimple.util

import android.text.TextUtils
import com.simple.wechatsimple.model.action.RefreshUnreadCountModel
import com.simple.wechatsimple.model.databse.UnreadCountModel
import com.simple.wechatsimple.model.databse.UnreadMessageRecordModel
import io.realm.Realm
import org.greenrobot.eventbus.EventBus

class UnreadCountManager private constructor(private val unreadCountCacheMap: CacheMap<Int, Int>) {

    fun recordUnreadMessage(targetId: Int, messageId: String) {
        if (targetId == 0 || TextUtils.isEmpty(messageId)) {
            return
        }
        Realm.getDefaultInstance().executeTransaction {
            val unreadRecord = it.createObject(UnreadMessageRecordModel::class.java, messageId)
            unreadRecord.targetId = targetId
        }

        val target = unreadCountCacheMap[targetId]
        if (target == null) {
            val unreadCount = Realm
                    .getDefaultInstance()
                    .where(UnreadMessageRecordModel::class.java)
                    .equalTo("targetId", targetId)
                    .count()
            unreadCountCacheMap.put(targetId, unreadCount.toInt())
        } else {
            unreadCountCacheMap.put(targetId, target.inc())
        }
    }

    fun getTargetUnreadCount(targetId: Int): Int? {
        if (targetId == 0) {
            return 0
        }

        val target = unreadCountCacheMap[targetId]
        if (target == null) {
            val all = Realm
                    .getDefaultInstance()
                    .where(UnreadMessageRecordModel::class.java)
                    .findAll()
            val unreadCount = Realm
                    .getDefaultInstance()
                    .where(UnreadMessageRecordModel::class.java)
                    .equalTo("targetId", targetId)
                    .count()
            unreadCountCacheMap.put(targetId, unreadCount.toInt())
        }

        return unreadCountCacheMap[targetId]
    }

    fun removeMessageUnreadRecord(messageId: String) {
        Realm.getDefaultInstance().executeTransaction {
            val result = Realm
                    .getDefaultInstance()
                    .where(UnreadMessageRecordModel::class.java)
                    .equalTo("id", messageId)
                    .findFirst()
            result?.let { result ->
                unreadCountCacheMap.put(result.targetId,
                        unreadCountCacheMap[result.targetId].dec())
            }
            result?.deleteFromRealm()
        }
    }

    fun getTotalCount(): Int {
        var total = 0
        unreadCountCacheMap.snapshot().keys.forEach {
            if (unreadCountCacheMap[it] != -1) {
                total += unreadCountCacheMap[it]
            }
        }

        return total
    }

    companion object {
        private var INSTANCE: UnreadCountManager? = null

        @JvmStatic
        fun getInstance(): UnreadCountManager {
            if (INSTANCE == null) {
                synchronized(UnreadCountManager::class.java) {
                    INSTANCE = UnreadCountManager(CacheMap(256))
                }
            }
            return INSTANCE!!
        }
    }

}