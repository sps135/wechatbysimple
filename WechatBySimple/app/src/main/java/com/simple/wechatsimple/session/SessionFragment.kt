package com.simple.wechatsimple.session

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.bingoogolapple.refreshlayout.BGANormalRefreshViewHolder
import cn.bingoogolapple.refreshlayout.BGARefreshLayout
import com.lqr.emoji.EmotionKeyboard
import com.lqr.emoji.IEmotionSelectedListener
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.tools.PictureFileUtils.getPath
import com.simple.wechatsimple.R
import com.simple.wechatsimple.base.BaseFragment
import com.simple.wechatsimple.databinding.FragSessionBinding
import com.simple.wechatsimple.model.action.RefreshUnreadCountModel
import com.simple.wechatsimple.util.*
import kotlinx.android.synthetic.main.frag_session.*
import kotlinx.android.synthetic.main.include_func_layout.*
import org.greenrobot.eventbus.EventBus
import java.io.File

class SessionFragment : BaseFragment(), BGARefreshLayout.BGARefreshLayoutDelegate, IEmotionSelectedListener {

    private lateinit var viewDataBinding: FragSessionBinding

    private lateinit var emotionKeyboard: EmotionKeyboard

    private lateinit var tvTitle: TextView

    private var targetId = 0

    private var conversationType: Int? = null

    val REQUEST_IMAGE_PICKER = 1000

    val REQUEST_TAKE_PHOTO = 1001

    companion object {

        fun forSessionFragment(targetId: Int, conversationType: Int): SessionFragment {
            val fragment = SessionFragment()
            val bundle = Bundle()
            bundle.putInt("targetId", targetId)
            bundle.putInt("conversationType", conversationType)
            fragment.arguments = bundle

            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        targetId = arguments!!.getInt("targetId", 0)
        conversationType = arguments!!.getInt("conversationType")
        viewDataBinding = FragSessionBinding.inflate(inflater, container, false).apply {
            session = (activity as AppCompatActivity).obtainViewModel(SessionViewModel::class.java)
            session!!.initConversationInfo(conversationType!!, targetId)
            session!!.refreshAdapterSingleLiveEvent.observe(activity!!, Observer {
                viewDataBinding.rvMsg.adapter!!.notifyDataSetChanged()
            })
            session!!.movePositionSingleLiveEvent.observe(activity!!, Observer {
                viewDataBinding.etContent.setText("")
                UIUtils.postTaskDelay({
                    val count = viewDataBinding.rvMsg.getAdapter()!!.getItemCount() - 1
                    if (count != -1) {
                        viewDataBinding.rvMsg.smoothScrollToPosition(count)
                    }
                }, 50)
            })
            session!!.stopRefreshSingleLiveEvent.observe(activity!!, Observer {
                viewDataBinding.refreshLayout.endRefreshing()
            })
            mViewModelView = session
        }

        viewDataBinding.rvMsg.layoutManager = LinearLayoutManager(activity)
        viewDataBinding.rvMsg.adapter = SessionAdapter(activity!!, ArrayList(), viewDataBinding.session!!)
        tvTitle = viewDataBinding.root.findViewById(R.id.tvToolbarTitle)

        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initListener()

        viewDataBinding.session!!.getHistoryMessage(0)

        tvTitle.text = UserInfoManager.getInstance().getNickName(targetId)
    }

    override fun onBGARefreshLayoutBeginLoadingMore(refreshLayout: BGARefreshLayout?): Boolean {
        return false
    }

    override fun onBGARefreshLayoutBeginRefreshing(refreshLayout: BGARefreshLayout?) {
        viewDataBinding.session!!.loadMore()
    }

    fun sendImage(path: String) {
        (mViewModelView as SessionViewModel).sendImage(null, path)
    }

    fun back() {
        if (elEmotion.isShown() || llMore.isShown()) {
            emotionKeyboard.interceptBackPress()
            ivEmo.setImageResource(R.mipmap.ic_cheat_emo)
        } else {
            activity!!.finish()
        }
    }

    private fun initView() {
        initAudioRecordManager()

        elEmotion.attachEditText(etContent)

        emotionKeyboard = EmotionKeyboard.with(activity).apply {
            bindToEditText(etContent)
            bindToContent(llContent)
            setEmotionLayout(flEmotionView)
            bindToEmotionButton(ivEmo, ivMore)
            setOnEmotionButtonOnClickListener {
                when (it.id) {
                    R.id.ivEmo -> {
                        clearState()
                        if (!elEmotion.isShown && llMore.isShown) {
                            displayEmotionLayout()
                            return@setOnEmotionButtonOnClickListener true
                        } else if (elEmotion.isShown && !llMore.isShown) {
                            ivEmo.setImageResource(R.mipmap.ic_cheat_emo)
                            return@setOnEmotionButtonOnClickListener false
                        }
                        displayEmotionLayout()
                    }

                    R.id.ivMore -> {
                        clearState()
                        if (!llMore.isShown && elEmotion.isShown) {
                            displayMoreLayout()
                            return@setOnEmotionButtonOnClickListener true
                        }
                        displayMoreLayout()
                    }
                }
                false
            }
        }

        refreshLayout.setDelegate(this@SessionFragment)
        val refreshViewHolder = BGANormalRefreshViewHolder(activity, false)
                .apply {
                    setRefreshingText("")
                    setPullDownRefreshText("")
                    setReleaseRefreshText("")
                }
        refreshLayout.setRefreshViewHolder(refreshViewHolder)

        etContent.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                UIUtils.postTaskDelay({
                    val count = viewDataBinding.rvMsg.getAdapter()!!.getItemCount() - 1
                    if (count != -1) {
                        viewDataBinding.rvMsg.smoothScrollToPosition(count)
                    }
                }, 50)
            }
        }
    }

    override fun onEmojiSelected(key: String?) {
    }

    override fun onStickerSelected(categoryName: String?, stickerName: String?, stickerBitmapPath: String?) {
        // TODO mPresenter.sendFileMsg(new File(stickerBitmapPath));
    }

    private fun initListener() {
        elEmotion.setEmotionSelectedListener(this)
        elEmotion.setEmotionAddVisiable(true)
        elEmotion.setEmotionSettingVisiable(true)

        llContent.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    closeBottomAndKeyboard()
                }
            }
            false
        }

        rvMsg.setOnTouchListener { v, event ->
            closeBottomAndKeyboard()
            false
        }

        ivAudio.setOnClickListener {
            if (btnAudio.isShown) {
                hideAudio()
                etContent.requestFocus()
                emotionKeyboard.showSoftInput()
            } else {
                etContent.clearFocus()
                showAudioButton()
                hideEmotionLayout()
                llMore.visibility = View.GONE
            }
            UIUtils.postTaskDelay({
                val count = viewDataBinding.rvMsg.getAdapter()!!.getItemCount() - 1
                if (count != -1) {
                    viewDataBinding.rvMsg.smoothScrollToPosition(count)
                }
            }, 50)
        }

        etContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (TextUtils.isEmpty(s)) {
                    btnSend.visibility = View.GONE
                    ivMore.visibility = View.VISIBLE
                } else {
                    btnSend.visibility = View.VISIBLE
                    ivMore.visibility = View.GONE
                }
            }
        })

        etContent.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                UIUtils.postTaskDelay({
                    val count = viewDataBinding.rvMsg.getAdapter()!!.getItemCount() - 1
                    if (count != -1) {
                        viewDataBinding.rvMsg.smoothScrollToPosition(count)
                    }

                }, 50)
            }
        }

        etContent.setOnKeyBoardHideListener { keyCode, event -> }

        btnAudio.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    AudioRecordManager.getInstance(activity).startRecord()
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isCancelled(v, event)) {
                        AudioRecordManager.getInstance(activity).willCancelRecord()
                    } else {
                        AudioRecordManager.getInstance(activity).continueRecord()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    AudioRecordManager.getInstance(activity).stopRecord()
                    AudioRecordManager.getInstance(activity).destroyRecord()
                }
            }
            false
        }

        rlAlbum.setOnClickListener {
            PictureSelector.create(activity!!)
                    .openGallery(PictureMimeType.ofImage())
                    .maxSelectNum(9)
                    .minSelectNum(0)
                    .imageSpanCount(4)
                    .selectionMode(PictureConfig.MULTIPLE)
                    .previewImage(true)
                    .isCamera(true)
                    .imageFormat(PictureMimeType.PNG)
                    .isZoomAnim(true)
                    .sizeMultiplier(0.5f)
                    .setOutputCameraPath("/CustomPath")
                    .enableCrop(false)
                    .compress(true)
                    .hideBottomControls(true)
                    .isGif(true)
                    .forResult(PictureConfig.CHOOSE_REQUEST)//结果回调onActivityResult code
        }
    }

    private fun isCancelled(view: View, event: MotionEvent): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)

        return (event.rawX < location[0] || event.rawX > location[0] + view.width
                || event.rawY < location[1] - 40)
    }

    private fun clearState() {
        UIUtils.postTaskDelay({
            val count = viewDataBinding.rvMsg.getAdapter()!!.getItemCount() - 1
            if (count != -1) {
                viewDataBinding.rvMsg.smoothScrollToPosition(count)
            }
        }, 50)
        etContent.clearFocus()
    }

    private fun displayEmotionLayout() {
        elEmotion.visibility = View.VISIBLE
        ivEmo.setImageResource(R.mipmap.ic_cheat_keyboard)
        llMore.visibility = View.GONE
        hideAudio()
    }

    private fun displayMoreLayout() {
        hideEmotionLayout()
        llMore.visibility = View.VISIBLE
        hideAudio()
    }

    private fun hideEmotionLayout() {
        elEmotion.visibility = View.GONE
        ivEmo.setImageResource(R.mipmap.ic_cheat_emo)
    }

    private fun hideAudio() {
        btnAudio.visibility = View.GONE
        etContent.visibility = View.VISIBLE
        ivAudio.setImageResource(R.mipmap.ic_cheat_voice)
    }

    private fun showAudioButton() {
        btnAudio.visibility = View.VISIBLE
        etContent.visibility = View.GONE
        ivAudio.setImageResource(R.mipmap.ic_cheat_keyboard)

        if (flEmotionView.isShown) {
            emotionKeyboard.interceptBackPress()
        } else {
            emotionKeyboard.hideSoftInput()
        }
    }

    private fun closeBottomAndKeyboard() {
        elEmotion.visibility = View.GONE
        llMore.visibility = View.GONE
        emotionKeyboard.interceptBackPress()
        ivEmo.setImageResource(R.mipmap.ic_cheat_emo)
    }

    private fun initAudioRecordManager() {
        AudioRecordManager.getInstance(activity!!.applicationContext).maxVoiceDuration = 120
        val file = File(FileUtil.getDir("audio", activity))
        if (!file.exists()) {
            file.mkdirs()
        }
        AudioRecordManager.getInstance(activity!!.applicationContext).setAudioSavePath(file.absolutePath)
        AudioRecordManager.getInstance(activity!!.applicationContext).audioRecordListener =
                AudioRecordListener(
                        activity!!,
                        llRoot,
                        object : AudioRecordListener.OnRecordFinishListener {
                            override fun onFinish(audioPath: String, duration: Int) {
                                viewDataBinding.session!!
                                        .sendVoice(null, audioPath, duration)
                            }
                        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AudioRecordManager.getInstance(activity!!.applicationContext).audioRecordListener = null

        val notify = RefreshUnreadCountModel().apply {
            this.targetId = this@SessionFragment.targetId
        }
        EventBus.getDefault().post(notify)
    }
}