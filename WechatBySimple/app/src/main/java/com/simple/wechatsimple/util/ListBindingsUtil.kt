package com.simple.wechatsimple.util

import android.databinding.BindingAdapter
import android.support.v7.widget.RecyclerView
import android.widget.HeaderViewListAdapter
import android.widget.ListView
import com.lqr.recyclerview.LQRRecyclerView
import com.simple.wechatsimple.contacts.ContactAdapter
import com.simple.wechatsimple.model.BaseMessageItemModel
import com.simple.wechatsimple.model.ContactItemModel
import com.simple.wechatsimple.model.ConversationItemModel
import com.simple.wechatsimple.recent.RecentMessageAdapter
import com.simple.wechatsimple.session.SessionAdapter

object ListBindingsUtil {

    @BindingAdapter("app:items")
    @JvmStatic
    fun setItems(listView: ListView, data: List<ContactItemModel>) {
        val hAdapter = listView.adapter as HeaderViewListAdapter
        with(hAdapter.wrappedAdapter as ContactAdapter) {
            setData(data)
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun setItems(recyclerView: RecyclerView, data: List<BaseMessageItemModel>) {
        with(recyclerView.adapter as SessionAdapter) {
            setData(data)
        }

    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun setItems(recyclerView: LQRRecyclerView, data: List<ConversationItemModel>) {
        with(recyclerView.adapter as RecentMessageAdapter) {
            setData(data)
        }
    }


}