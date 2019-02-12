package com.simple.wechatsimple.login

import android.app.Application
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import com.simple.wechatsimple.MyApp
import com.simple.wechatsimple.base.BaseModelView
import com.simple.wechatsimple.base.SingleLiveEvent
import com.simple.wechatsimple.data.source.DataSourceRepository
import com.simple.wechatsimple.model.databse.LoginStateModel
import com.simple.wechatsimple.model.databse.UserModel
import com.simple.wechatsimple.util.UserInfoManager
import io.reactivex.functions.Consumer
import io.realm.Realm
import io.realm.RealmConfiguration

class LoginViewModel(context: Application,
                     private val dataRepository: DataSourceRepository)
    : BaseModelView(context) {

    val nameEditText = ObservableField<String>()
    val passwordEditText = ObservableField<String>()
    val loginEnable = ObservableBoolean(false)
    val textWatcher = UsernamePasswordTextWatcher()

    val navigateToMain = SingleLiveEvent<Void>()

    fun loginAction() {
        dataRepository
                .login(nameEditText.get()!!, passwordEditText.get()!!)!!
                .doOnSubscribe {
                    showLoadingEvent.call()
                }
                .subscribe(Consumer { user: UserModel ->
                    hideLoadingEvent.call()

                    Realm.getInstance(MyApp.getLoginStateConfig()).executeTransaction {
                        it.delete(LoginStateModel::class.java)
                        val state = it.createObject(LoginStateModel::class.java)
                        state.uid = user.uid
                    }

                    val config = RealmConfiguration.Builder().name("${user.uid}.realm").build()
                    Realm.setDefaultConfiguration(config)

                    user.isSelf = true
                    UserInfoManager.getInstance().refreshUserInfo(user)

                    navigateToMain.call()
                }, DefaultErrorCallback()).also {
                    mDisposable.addAll(it)
                }
    }

    inner class UsernamePasswordTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (nameEditText.get() != null
                    && !TextUtils.isEmpty(nameEditText.get()!!)
                    && passwordEditText.get() != null
                    && !TextUtils.isEmpty(passwordEditText.get()!!)) {
                loginEnable.set(true)
            } else {
                loginEnable.set(false)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }


}