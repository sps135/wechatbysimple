package com.simple.wechatsimple.recent

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.wechatsimple.base.BaseFragment
import com.simple.wechatsimple.databinding.FragmentRecentMessageBinding
import com.simple.wechatsimple.main.MainActivity

class RecentFragment : BaseFragment() {

    private lateinit var viewDataBinding: FragmentRecentMessageBinding

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewDataBinding = FragmentRecentMessageBinding.inflate(inflater, container, false)
                .apply {
                    recent = (activity as MainActivity).obtainRecentViewModel()
                    mViewModelView = recent

                    recent!!.refreshAdapterSingleLiveEvent.observe(activity!!, Observer {
                        viewDataBinding.rvRecentMessage.adapter!!.notifyDataSetChanged()
                    })
                }

        viewDataBinding.rvRecentMessage.layoutManager = LinearLayoutManager(activity)
        viewDataBinding.rvRecentMessage.adapter = RecentMessageAdapter(activity!!)

        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (mViewModelView as RecentViewModel).getHistory()
    }
}