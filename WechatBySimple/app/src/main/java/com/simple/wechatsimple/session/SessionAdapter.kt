package com.simple.wechatsimple.session

import android.content.Context
import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.simple.wechatsimple.R
import com.simple.wechatsimple.base.BaseBindRecyclerViewAdapter
import com.simple.wechatsimple.base.IBaseBindingAdapterItem
import com.simple.wechatsimple.databinding.*
import com.simple.wechatsimple.model.BaseMessageItemModel
import com.simple.wechatsimple.model.ImageMessageItemModel
import com.simple.wechatsimple.model.TextMessageItemModel
import com.simple.wechatsimple.model.VoiceMessageItemModel

class SessionAdapter(val context: Context, val list: ArrayList<BaseMessageItemModel>, val sessionViewModel: SessionViewModel)
    : BaseBindRecyclerViewAdapter<BaseMessageItemModel>(context, list) {

    override fun getItemViewType(position: Int) = list[position].itemViewType

    override fun onCreateMyViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        when (viewType) {
            R.layout.item_audio_send -> {
                return SendVoiceViewHolder(DataBindingUtil
                        .inflate(inflater, R.layout.item_audio_send, parent, false))
            }

            R.layout.item_audio_receive -> {
                return ReceivedVoiceViewHolder(DataBindingUtil
                        .inflate(inflater, R.layout.item_audio_receive, parent, false))
            }

            R.layout.item_text_send -> {
                return SendTextViewHolder(DataBindingUtil
                        .inflate(inflater, R.layout.item_text_send, parent, false))
            }

            R.layout.item_text_receive -> {
                return ReceivedTextViewHolder(DataBindingUtil
                        .inflate(inflater, R.layout.item_text_receive, parent, false))
            }

            R.layout.item_image_send -> {
                return SendImageViewHolder(DataBindingUtil
                        .inflate(inflater, R.layout.item_image_send, parent, false))
            }

            R.layout.item_image_receive -> {
                return ReceivedImageViewHolder(DataBindingUtil
                        .inflate(inflater, R.layout.item_image_receive, parent, false))
            }

            else -> {
                return null
            }
        }
    }

    override fun onBindMyViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val retryListener = object : OnRetryListener {
            override fun onRetry(item: IBaseBindingAdapterItem) {
                sessionViewModel.retry(item)
            }
        }
        when (holder) {
            is SendVoiceViewHolder -> {
                val model = mList[position] as VoiceMessageItemModel
                holder.binding.item = model
                holder.binding.retryListener = retryListener
                holder.binding.executePendingBindings()
            }

            is ReceivedVoiceViewHolder -> {
                val model = mList[position] as VoiceMessageItemModel
                holder.binding.item = model
                holder.binding.executePendingBindings()
            }

            is SendTextViewHolder -> {
                val model = mList[position] as TextMessageItemModel
                holder.binding.item = model
                holder.binding.retryListener = retryListener
                holder.binding.executePendingBindings()
            }

            is ReceivedTextViewHolder -> {
                val model = mList[position] as TextMessageItemModel
                holder.binding.item = model
                holder.binding.retryListener = retryListener
                holder.binding.executePendingBindings()
            }

            is SendImageViewHolder -> {
                val model = mList[position] as ImageMessageItemModel
                holder.binding.item = model
                holder.binding.retryListener = retryListener
                holder.binding.executePendingBindings()
            }

            is ReceivedImageViewHolder -> {
                val model = mList[position] as ImageMessageItemModel
                holder.binding.item = model
                holder.binding.retryListener = retryListener
                holder.binding.executePendingBindings()
            }
        }
    }

    fun setData(data: List<BaseMessageItemModel>) {
        list.clear()
        list.addAll(data)

        notifyDataSetChanged()
    }

    inner class SendVoiceViewHolder(val binding: ItemAudioSendBinding)
        : RecyclerView.ViewHolder(binding.root)

    inner class SendTextViewHolder(val binding: ItemTextSendBinding)
        : RecyclerView.ViewHolder(binding.root)

    inner class SendImageViewHolder(val binding: ItemImageSendBinding)
        : RecyclerView.ViewHolder(binding.root)

    inner class ReceivedVoiceViewHolder(val binding: ItemAudioReceiveBinding)
        : RecyclerView.ViewHolder(binding.root)

    inner class ReceivedTextViewHolder(val binding: ItemTextReceiveBinding)
        : RecyclerView.ViewHolder(binding.root)

    inner class ReceivedImageViewHolder(val binding: ItemImageReceiveBinding)
        : RecyclerView.ViewHolder(binding.root)

    interface OnRetryListener {
        fun onRetry(item: IBaseBindingAdapterItem)
    }
}