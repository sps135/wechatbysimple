package com.simple.wechatsimple.splash

import android.content.Intent
import android.os.Bundle
import android.os.UserManager
import android.support.v7.app.AppCompatActivity
import com.jaeger.library.StatusBarUtil
import com.simple.wechatsimple.MyApp
import com.simple.wechatsimple.R
import com.simple.wechatsimple.login.LoginActivity
import com.simple.wechatsimple.main.MainActivity
import com.simple.wechatsimple.model.databse.LoginStateModel
import com.simple.wechatsimple.model.databse.UserModel
import com.simple.wechatsimple.util.UserInfoManager
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import io.realm.RealmConfiguration
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    private val mDisposable = CompositeDisposable();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_splash)
        StatusBarUtil.setTranslucent(this)

        AndPermission.with(this)
                .runtime()
                .permission(
                        Permission.Group.STORAGE,
                        Permission.Group.CAMERA,
                        Permission.Group.MICROPHONE)
                .onGranted {
                    navigateToNextStep()
                }
                .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposable.dispose()
    }

    private fun navigateToNextStep() {
        Observable
                .timer(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val intent = Intent().apply {
                        setClass(this@SplashActivity, LoginActivity::class.java)
                        val result = Realm
                                .getInstance(MyApp.getLoginStateConfig())
                                .where(LoginStateModel::class.java)
                                .findFirst()
                        if (result == null) {
                            setClass(this@SplashActivity, LoginActivity::class.java)
                        } else {
                            val config = RealmConfiguration.Builder().name("${result.uid}.realm").build()
                            Realm.setDefaultConfiguration(config)

                            setClass(this@SplashActivity, MainActivity::class.java)
                        }
                    }
                    this@SplashActivity.startActivity(intent)
                    this@SplashActivity.finish()
                }.apply {
                    mDisposable.add(this)
                }
    }
}