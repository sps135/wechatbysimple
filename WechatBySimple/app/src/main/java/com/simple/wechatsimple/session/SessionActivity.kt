package com.simple.wechatsimple.session

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.jaeger.library.StatusBarUtil
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.simple.wechatsimple.R
import com.simple.wechatsimple.base.BaseActivity
import com.simple.wechatsimple.util.replaceFragmentInActivity

class SessionActivity : BaseActivity() {

    private var targetId: Int? = null

    private var conversationType: Int? = null

    val REQUEST_IMAGE_PICKER = 1000

    val REQUEST_TAKE_PHOTO = 1001

    private lateinit var fragment: SessionFragment

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_session)

        conversationType = intent.extras!!.getInt("conversationType")
        targetId = intent.extras!!.getInt("targetId", 0)

        fragment = findOrCreateViewFragment() as SessionFragment
        replaceFragmentInActivity(fragment, R.id.contentFrame)

        if (Build.VERSION.SDK_INT >= 21) {
            window.navigationBarColor = resources.getColor(R.color.color_f7f7f7)
        }

        StatusBarUtil.setColorNoTranslucent(this, resources.getColor(R.color.color_ededed))
        StatusBarUtil.setLightMode(this)
    }

    private fun findOrCreateViewFragment() =
            supportFragmentManager.findFragmentById(R.id.contentFrame)
                    ?: SessionFragment.forSessionFragment(targetId!!, conversationType!!)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PictureConfig.CHOOSE_REQUEST -> {
                // 图片、视频、音频选择结果回调
                val selectList = PictureSelector.obtainMultipleResult(data)
                selectList.forEach {
                    fragment.sendImage(it.compressPath)
                }
            }
        }
    }

    override fun onBackPressed() {
        fragment.back()
    }
}