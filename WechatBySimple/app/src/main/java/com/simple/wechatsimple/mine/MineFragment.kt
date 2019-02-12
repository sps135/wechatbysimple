package com.simple.wechatsimple.mine

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.lqr.optionitemview.OptionItemView
import com.simple.imlib.IMClient
import com.simple.wechatsimple.MyApp
import com.simple.wechatsimple.R
import com.simple.wechatsimple.login.LoginActivity
import com.simple.wechatsimple.model.action.LogOutAction
import com.simple.wechatsimple.model.databse.LoginStateModel
import com.simple.wechatsimple.util.GlideRoundTransform
import com.simple.wechatsimple.util.UserInfoManager
import io.realm.Realm
import org.greenrobot.eventbus.EventBus

class MineFragment : Fragment() {

    private lateinit var ivPortrait: ImageView

    private lateinit var tvNickName: TextView

    private lateinit var tvLogOut: OptionItemView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_mine, container, false).apply {
            ivPortrait = findViewById(R.id.ivHeader)
            tvNickName = findViewById(R.id.tvName)
            tvLogOut = findViewById(R.id.tv_log_out)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(activity!!)
                .load(UserInfoManager.getInstance().getSelfUserInfo()!!.portrait)
                .apply(RequestOptions.placeholderOf(R.drawable.portrait_placeholder))
                .apply(RequestOptions.errorOf(R.drawable.portrait_placeholder))
                .apply(RequestOptions.bitmapTransform(GlideRoundTransform(activity!!, 5)))
                .into(ivPortrait)

        tvNickName.text = UserInfoManager.getInstance().getSelfUserInfo()!!.nickname
        tvLogOut.setOnClickListener {
            IMClient.getInstance().disconnect()
            Realm.getInstance(MyApp.getLoginStateConfig()).executeTransaction { realm: Realm ->
                realm.delete(LoginStateModel::class.java)
            }
            MyApp.getInstance().exit()

            val intent = Intent().apply {
                setClass(activity, LoginActivity::class.java)
            }
            activity!!.startActivity(intent)
        }
    }

}