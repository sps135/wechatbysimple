package com.simple.wechatsimple.contacts

import android.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.simple.wechatsimple.databinding.ItemContactBinding
import com.simple.wechatsimple.model.ContactItemModel
import de.halfbit.pinnedsection.PinnedSectionListView

open class ContactAdapter(val viewModel: ContactsViewModel) : BaseAdapter(), PinnedSectionListView.PinnedSectionListAdapter {

    private var datas: MutableList<ContactItemModel>

    init {
        datas = mutableListOf()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ItemContactBinding

        if (convertView == null) {
            val inflater = LayoutInflater.from(parent!!.context)

            binding = ItemContactBinding.inflate(inflater, parent, false)
        } else {
            binding = DataBindingUtil.getBinding(convertView)!!
        }

        val itemListener = object : OnItemClickListener {
            override fun onItemClick() {
                viewModel.onItemEvent.value = datas[position]
            }
        }

        with(binding) {
            contact = datas[position]
            listener = itemListener
            executePendingBindings()
        }

        return binding.root
    }

    override fun getItem(position: Int) = datas[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = datas.size

    override fun getViewTypeCount() = 2

    override fun getItemViewType(position: Int) = datas[position].type

    override fun isItemViewTypePinned(viewType: Int) = viewType == ContactItemModel.SECTION

    fun setData(dataMap: List<ContactItemModel>) {
        datas.clear()
        datas.addAll(dataMap)

        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick()
    }

}