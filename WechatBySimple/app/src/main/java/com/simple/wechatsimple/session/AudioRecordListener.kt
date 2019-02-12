package com.simple.wechatsimple.session

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.simple.wechatsimple.util.IAudioRecordListener
import com.simple.wechatsimple.R
import java.io.File

class AudioRecordListener(val context: Context,
                          val rootView: View,
                          val finishListener: OnRecordFinishListener) : IAudioRecordListener {

    private lateinit var timerTv: TextView

    private lateinit var stateTv: TextView

    private lateinit var stateIv: ImageView

    private lateinit var recordWindow: PopupWindow

    override fun initTipView() {
        val view = View.inflate(context, R.layout.popup_audio_wi_vo, null)
        timerTv = view.findViewById(R.id.rc_audio_timer)
        stateTv = view.findViewById(R.id.rc_audio_state_text)
        stateIv = view.findViewById(R.id.rc_audio_state_image)
        recordWindow = PopupWindow(view, -1, -1).apply {
            showAtLocation(rootView, 17, 0, 0)
            isFocusable = true
            isOutsideTouchable = false
            isTouchable = false
        }
    }

    override fun setTimeoutTipView(counter: Int) {
        stateIv.visibility = View.GONE
        stateTv.visibility = View.VISIBLE
        stateTv.text = "手指上滑，取消发送"
        stateTv.setBackgroundResource(R.drawable.bg_voice_popup)
        timerTv.text = "${counter}"
        timerTv.visibility = View.VISIBLE
    }

    override fun setRecordingTipView() {
        stateIv.visibility = View.VISIBLE
        stateIv.setImageResource(R.mipmap.ic_volume_1)
        stateTv.visibility = View.VISIBLE
        stateTv.text = "手指上滑，取消发送"
        stateTv.setBackgroundResource(R.drawable.bg_voice_popup)
        timerTv.visibility = View.GONE
    }

    override fun setAudioShortTipView() {
        stateIv.setImageResource(R.mipmap.ic_volume_wraning)
        stateTv.text = "录音时间太短"
    }

    override fun setCancelTipView() {
        timerTv.visibility = View.GONE
        stateIv.visibility = View.VISIBLE
        stateIv.setImageResource(R.mipmap.ic_volume_cancel)
        stateTv.visibility = View.VISIBLE
        stateTv.setBackgroundResource(R.drawable.corner_voice_style)
    }

    override fun destroyTipView() {
        recordWindow.dismiss()
    }

    override fun onStartRecord() {
    }

    override fun onFinish(audioPath: Uri?, duration: Int) {
        if (File(audioPath!!.path).exists()) {
            finishListener.onFinish(audioPath!!.path, duration)
        }
    }

    override fun onAudioDBChanged(db: Int) {
        when (db % 5) {
            0 -> {
                stateIv.setImageResource(R.mipmap.ic_volume_1)
            }
            1 -> {
                stateIv.setImageResource(R.mipmap.ic_volume_2)
            }
            2 -> {
                stateIv.setImageResource(R.mipmap.ic_volume_3)
            }
            3 -> {
                stateIv.setImageResource(R.mipmap.ic_volume_4)
            }
            4 -> {
                stateIv.setImageResource(R.mipmap.ic_volume_5)
            }
            5 -> {
                stateIv.setImageResource(R.mipmap.ic_volume_6)
            }
            6 -> {
                stateIv.setImageResource(R.mipmap.ic_volume_7)
            }
            else -> {
                stateIv.setImageResource(R.mipmap.ic_volume_8)
            }
        }
    }

    interface OnRecordFinishListener {
        fun onFinish(audioPath: String, duration: Int)
    }
}