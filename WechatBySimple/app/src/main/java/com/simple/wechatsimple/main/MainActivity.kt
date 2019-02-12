package com.simple.wechatsimple.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.flyco.tablayout.CommonTabLayout
import com.flyco.tablayout.listener.CustomTabEntity
import com.flyco.tablayout.listener.OnTabSelectListener
import com.jaeger.library.StatusBarUtil
import com.simple.imlib.IMClient
import com.simple.wechatsimple.MyApp
import com.simple.wechatsimple.R
import com.simple.wechatsimple.base.BaseActivity
import com.simple.wechatsimple.component.AlertDialog
import com.simple.wechatsimple.contacts.ContactsFragment
import com.simple.wechatsimple.data.source.inject.Injection
import com.simple.wechatsimple.discovery.DiscoveryFragment
import com.simple.wechatsimple.login.LoginActivity
import com.simple.wechatsimple.mine.MineFragment
import com.simple.wechatsimple.model.action.LogOutAction
import com.simple.wechatsimple.model.action.LongLinkStatusActionModel
import com.simple.wechatsimple.model.action.RefreshUnreadCountModel
import com.simple.wechatsimple.model.databse.LoginStateModel
import com.simple.wechatsimple.recent.RecentFragment
import com.simple.wechatsimple.recent.RecentViewModel
import com.simple.wechatsimple.util.NotificationInterface
import com.simple.wechatsimple.util.UnreadCountManager
import com.simple.wechatsimple.util.UserInfoManager
import com.simple.wechatsimple.util.obtainViewModel
import com.tencent.mars.stn.StnLogic
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var mTlMainTab: CommonTabLayout

    private lateinit var mVpMain: ViewPager

    private lateinit var mIvRight: ImageView

    private lateinit var mIvLeft: ImageView

    private lateinit var mTvTitle: TextView

    private val mTabFragments = arrayListOf<Fragment>()

    val mTitles = arrayOf("微信", "通讯录", "发现", "我")

    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)
        EventBus.getDefault().register(this)
        initView()
    }

    private fun initView() {
        mIvLeft = findViewById(R.id.ivToolbarNavigation)
        mIvRight = findViewById(R.id.ibAddMenu)
        mTvTitle = findViewById(R.id.tvToolbarTitle)
        mTlMainTab = findViewById<CommonTabLayout>(R.id.tl_main).apply {
            setOnTabSelectListener(object : OnTabSelectListener {
                override fun onTabSelect(position: Int) {
                    mVpMain.currentItem = position
                    handleTitle(position)
                }

                override fun onTabReselect(position: Int) {

                }
            })
        }

        mVpMain = findViewById<ViewPager>(R.id.vp_main).apply {
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(p0: Int) {

                }

                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {

                }

                override fun onPageSelected(p0: Int) {
                    mTlMainTab.currentTab = p0
                }
            })
        }

        val mTabIconsNormal = arrayOf(R.mipmap.message_normal, R.mipmap.contacts_normal, R.mipmap.discovery_normal, R.mipmap.me_normal)
        val mTabIconsSelected = arrayOf(R.mipmap.message_press, R.mipmap.contacts_press, R.mipmap.discovery_press, R.mipmap.me_press)

        val mTabEntities = ArrayList<CustomTabEntity>()
        for (i in 0..3) {
            mTabEntities.add(TabEntity(mTitles[i], mTabIconsSelected[i], mTabIconsNormal[i]))
            mTlMainTab.setTabData(mTabEntities)
        }

        mTabFragments.add(RecentFragment())
        mTabFragments.add(ContactsFragment())
        mTabFragments.add(DiscoveryFragment())
        mTabFragments.add(MineFragment())

        mVpMain.adapter = HomepageAdapter(supportFragmentManager)
        mVpMain.offscreenPageLimit = 4

        connectToIM()

        UserInfoManager
                .getInstance()
                .setUserInfoProvider(
                        object : UserInfoManager.UserInfoProvider {
                            override fun provide(userId: Int) {
                                Injection
                                        .provideDataRepository()
                                        .remoteDataSource
                                        .getUserInfo(userId)!!
                                        .subscribe({
                                            UserInfoManager.getInstance().refreshUserInfo(it)
                                        }, {
                                            Log.i("UserInfoManager", it.message)
                                        }).apply {
                                            disposable.add(this)
                                        }
                            }
                        })

        if (Build.VERSION.SDK_INT >= 21) {
            window.navigationBarColor = resources.getColor(R.color.color_f7f7f7)
        }

        handleTitle(0)
        StatusBarUtil.setColorNoTranslucent(this, resources.getColor(R.color.color_ededed))
        StatusBarUtil.setLightMode(this)
    }

    private fun handleTitle(index: Int) {
        when (index) {
            0 -> {
                mIvLeft.visibility = View.GONE
                mTvTitle.text = "微信"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        IMClient.getInstance().setForeground(true)
        NotificationInterface.getInstance().setIsForeground(true)
        updateUnreadCountInternal()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        IMClient.getInstance().setForeground(false)
        NotificationInterface.getInstance().setIsForeground(false)
        EventBus.getDefault().unregister(this)
        UserInfoManager.getInstance().clearUserInfoProvider()
    }

    private fun connectToIM() {
        val user = UserInfoManager.getInstance().getSelfUserInfo()
        IMClient.getInstance().connect(user!!.uid)
    }

    inner class HomepageAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getCount(): Int {
            return mTabFragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mTitles[position]
        }

        override fun getItem(position: Int): Fragment {
            return mTabFragments.get(position)
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUserInfo(logOutAction: LogOutAction) {
        AlertDialog(this)
                .builder()
                .setMsg("已在另外一台设备登录")
                .setPositiveButton("确定") {
                    IMClient.getInstance().disconnect()
                    Realm.getInstance(MyApp.getLoginStateConfig()).executeTransaction { realm: Realm ->
                        realm.delete(LoginStateModel::class.java)
                    }
                    MyApp.getInstance().exit()

                    val intent = Intent().apply {
                        setClass(this@MainActivity, LoginActivity::class.java)
                    }
                    this@MainActivity.startActivity(intent)
                }
                .show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateMessageUnreadCount(refreshUnreadCountModel: RefreshUnreadCountModel) {
        updateUnreadCountInternal()
    }

    private fun updateUnreadCountInternal() {
        val count = UnreadCountManager.getInstance().getTotalCount()
        if (count != 0) {
            mTlMainTab.showMsg(0, count)
        } else {
            mTlMainTab.hideMsg(0)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLongLinkStatus(longLinkStatusActionModel: LongLinkStatusActionModel) {
        if (longLinkStatusActionModel.status == StnLogic.CONNECTED &&
                IMClient.getInstance().getConnectStatus() != StnLogic.CONNECTED) {
            IMClient
                    .getInstance()
                    .getHistoryMessage(UserInfoManager.getInstance().getSelfUserInfo()!!.uid)
        }
        IMClient.getInstance().setConnectStatus(longLinkStatusActionModel.status)

        when(longLinkStatusActionModel.status) {
            StnLogic.CONNECTTING -> {
                mTvTitle.text = "连接中..."
            }
            StnLogic.CONNECTED -> {
                mTvTitle.text = "微信"
            }
            else -> {
                mTvTitle.text = "微信(未连接)"
            }
        }
    }

    fun obtainRecentViewModel(): RecentViewModel = obtainViewModel(RecentViewModel::class.java)
}