package com.simple.wechatsimple.login

import android.animation.ObjectAnimator
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateInterpolator
import com.jaeger.library.StatusBarUtil
import com.simple.imlib.constant.Constant
import com.simple.wechatsimple.base.BaseFragment
import com.simple.wechatsimple.databinding.FragLoginBinding
import com.simple.wechatsimple.main.MainActivity
import com.simple.wechatsimple.session.SessionActivity
import com.simple.wechatsimple.util.KeyboardWatcher

class LoginFragment : BaseFragment(), KeyboardWatcher.SoftKeyboardStateListener {
    private lateinit var viewDataBinding: FragLoginBinding

    private lateinit var mKeyboardWatcher: KeyboardWatcher

    private var mScreenHeight: Int = 0

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewDataBinding = FragLoginBinding.inflate(inflater, container, false)
                .apply {
                    login = (activity as LoginActivity).obtainViewModel()
                    mViewModelView = login

                    login!!.navigateToMain.observe(activity!!, Observer {
                        val intent = Intent().apply {
                            setClass(activity, MainActivity::class.java)
                        }
                        activity!!.startActivity(intent)
                        activity!!.finish()
                    })
                }

        mKeyboardWatcher = KeyboardWatcher(activity!!.findViewById(Window.ID_ANDROID_CONTENT))
                .apply {
                    addSoftKeyboardStateListener(this@LoginFragment)
                }

        mScreenHeight = resources.displayMetrics.heightPixels;

        StatusBarUtil.setColor(activity, resources.getColor(android.R.color.white))
        return viewDataBinding.root
    }

    override fun onSoftKeyboardOpened(keyboardHeightInPx: Int) {
        val location = intArrayOf(0, 0)
        viewDataBinding.body.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val bottom = mScreenHeight - (y + viewDataBinding.body.height)
        if (keyboardHeightInPx > bottom) {
            val animatorTranslateY = ObjectAnimator.ofFloat(viewDataBinding.body,
                    "translationY",
                    0.0f,
                    -(keyboardHeightInPx - bottom).toFloat()).apply {
                duration = 300
                interpolator = AccelerateInterpolator()
            }
            animatorTranslateY.start()
        }
    }

    override fun onSoftKeyboardClosed() {
        val animatorTranslateY = ObjectAnimator.ofFloat(viewDataBinding.body,
                "translationY",
                viewDataBinding.body.translationY,
                0f).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
        }

        animatorTranslateY.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mKeyboardWatcher.removeSoftKeyboardStateListener(this)
    }

}