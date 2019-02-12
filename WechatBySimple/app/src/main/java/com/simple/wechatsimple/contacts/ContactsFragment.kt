package com.simple.wechatsimple.contacts

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.imlib.constant.Constant
import com.simple.wechatsimple.R
import com.simple.wechatsimple.base.BaseFragment
import com.simple.wechatsimple.databinding.FragContactsBinding
import com.simple.wechatsimple.main.MainActivity
import com.simple.wechatsimple.session.SessionActivity
import com.simple.wechatsimple.util.obtainViewModel

class ContactsFragment : BaseFragment() {
    private lateinit var viewDataBinding: FragContactsBinding

    private lateinit var adapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewDataBinding = FragContactsBinding.inflate(inflater, container, false).apply {
            contact = (activity as MainActivity).obtainViewModel(ContactsViewModel::class.java)
            contact!!.onItemEvent.observe(activity!!, Observer {
                val intent = Intent().apply {

                    val bundle = Bundle()
                    bundle.putInt("targetId", it!!.uid)
                    bundle.putInt("conversationType", Constant.PRIVATE_MESSAGE)
                    putExtras(bundle)

                    setClass(activity, SessionActivity::class.java)
                }
                activity!!.startActivity(intent)
            })

            mViewModelView = contact
        }

        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAdapter()
        viewDataBinding.contact!!.getUserList()
    }

    private fun setUpAdapter() {
        adapter = ContactAdapter(viewDataBinding.contact!!)
        viewDataBinding.lvContactList.adapter = adapter

        val headerView = LayoutInflater.from(activity).inflate(R.layout.item_contact_header, null)
        viewDataBinding.lvContactList.addHeaderView(headerView)
    }
}