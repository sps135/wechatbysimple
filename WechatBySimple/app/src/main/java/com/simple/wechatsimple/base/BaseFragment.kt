package com.simple.wechatsimple.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.android.tu.loadingdialog.LoadingDailog
import com.simple.wechatsimple.component.AlertDialog


open class BaseFragment : Fragment() {
    protected var mViewModelView: BaseModelView? = null

    private var mLoadingDialog: LoadingDailog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadBuilder = LoadingDailog.Builder(activity)
                .setMessage("加载中...")
                .setCancelable(true)
                .setCancelOutside(true)
        mLoadingDialog = loadBuilder.create()

        mViewModelView!!.showLoadingEvent.observe(activity as AppCompatActivity, Observer {
            showLoadingDialog()
        })

        mViewModelView!!.hideLoadingEvent.observe(activity as AppCompatActivity, Observer {
            hideLoadingDialog()
        })

        mViewModelView!!.showMessageEvent.observe(activity as AppCompatActivity, Observer {
            showMessage(it!!)
        })
    }

    private fun showLoadingDialog() {
        mLoadingDialog!!.show()
    }

    private fun hideLoadingDialog() {
        if (mLoadingDialog!!.isShowing) {
            mLoadingDialog!!.dismiss()
        }
    }

    protected fun showMessage(message: String) {
        AlertDialog(activity)
                .builder()
                .setMsg(message)
                .setPositiveButton("确定") {
                }
                .show()
    }
}