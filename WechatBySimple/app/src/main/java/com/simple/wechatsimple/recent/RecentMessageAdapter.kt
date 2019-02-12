package com.simple.wechatsimple.recent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.simple.imlib.constant.Constant
import com.simple.wechatsimple.R
import com.simple.wechatsimple.base.BaseBindingAdapter
import com.simple.wechatsimple.databinding.ItemRecentMessageBinding
import com.simple.wechatsimple.model.ConversationItemModel
import com.simple.wechatsimple.session.SessionActivity

class RecentMessageAdapter(context: Context) :
        BaseBindingAdapter<ConversationItemModel, ItemRecentMessageBinding>(context) {

    override fun getLayoutResId(viewType: Int) = R.layout.item_recent_message

    override fun onBindItem(binding: ItemRecentMessageBinding, item: ConversationItemModel) {
        binding.item = item
        binding.itemListener = onItemClickListener
        binding.executePendingBindings()
    }

    val onItemClickListener = object : OnItemClickListener {
        override fun onItem(item: ConversationItemModel) {
            val intent = Intent().apply {

                val bundle = Bundle()
                bundle.putInt("targetId", item.targetId)
                if (item.conversationType == Constant.PRIVATE_MESSAGE) {
                    bundle.putInt("conversationType", Constant.PRIVATE_MESSAGE)
                }
                putExtras(bundle)

                setClass(context, SessionActivity::class.java)
            }
            context!!.startActivity(intent)
        }
    }

    interface OnItemClickListener {
        fun onItem(item: ConversationItemModel)
    }
}