package com.simple.wechatsimple.contacts

import android.app.Application
import android.databinding.ObservableArrayList
import android.databinding.ObservableList
import com.simple.wechatsimple.base.BaseModelView
import com.simple.wechatsimple.base.SingleLiveEvent
import com.simple.wechatsimple.data.source.DataSourceRepository
import com.simple.wechatsimple.model.ContactItemModel
import com.simple.wechatsimple.model.databse.UserModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Consumer
import java.util.*
import kotlin.collections.ArrayList

class ContactsViewModel(context: Application,
                        private val dataRepository: DataSourceRepository) : BaseModelView(context) {

    val data: ObservableList<ContactItemModel> = ObservableArrayList()

    val onItemEvent = SingleLiveEvent<ContactItemModel>()

    fun getUserList() {
        dataRepository
                .getUserList()!!
                .compose(transformType())
                .subscribe(Consumer {
                    data.clear()
                    data.addAll(it)
                }, DefaultErrorCallback()).also {
                    mDisposable.addAll(it)
                }
    }

    private inline fun transformType():
            ObservableTransformer<List<UserModel>, List<ContactItemModel>> = ObservableTransformer {
        it.flatMap {
            val dataMap = TreeMap<String, ArrayList<UserModel>>()
            for (user in it) {
                val key = user.pinyin.substring(0, 1);
                if (dataMap[key] != null) {
                    dataMap[key]!!.add(user)
                } else {
                    val array = ArrayList<UserModel>()
                    array.add(user)
                    dataMap[key] = array
                }
            }

            val datas = mutableListOf<ContactItemModel>()

            val keys = dataMap.keys.toList()

            var sectionPosition = 0
            var listPosition = 0
            for (key in keys) {
                ContactItemModel(key).apply {
                    sectionPosition = sectionPosition
                    listPosition = listPosition++
                    datas.add(this)
                }

                val items = dataMap[key]
                for (item in items!!) {
                    ContactItemModel().apply {
                        nickname = item.nickname
                        portrait = item.portrait
                        uid = item.uid
                        sectionPosition = sectionPosition
                        listPosition = listPosition++
                        datas.add(this)
                    }

                }

                sectionPosition++
            }

            Observable.just(datas)
        }
    }

}