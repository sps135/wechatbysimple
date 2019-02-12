package com.simple.wechatsimple.login

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jaeger.library.StatusBarUtil
import com.simple.wechatsimple.R
import com.simple.wechatsimple.base.BaseActivity
import com.simple.wechatsimple.util.obtainViewModel
import com.simple.wechatsimple.util.replaceFragmentInActivity

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_login)
        replaceFragmentInActivity(findOrCreateViewFragment(), R.id.contentFrame)
    }

    private fun findOrCreateViewFragment() =
            supportFragmentManager.findFragmentById(R.id.contentFrame) ?: LoginFragment()

    fun obtainViewModel(): LoginViewModel = obtainViewModel(LoginViewModel::class.java)

}