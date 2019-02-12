package com.simple.wechatsimple.recent

import android.app.Application
import android.databinding.ObservableArrayList
import android.databinding.ObservableList
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.proto.imlib.Muduo
import com.simple.imlib.constant.Constant
import com.simple.wechatsimple.base.BaseModelView
import com.simple.wechatsimple.base.SingleLiveEvent
import com.simple.wechatsimple.data.source.DataSourceRepository
import com.simple.wechatsimple.model.ConversationItemModel
import com.simple.wechatsimple.model.ImageMessageItemModel
import com.simple.wechatsimple.model.TextMessageItemModel
import com.simple.wechatsimple.model.VoiceMessageItemModel
import com.simple.wechatsimple.model.action.RefreshUnreadCountModel
import com.simple.wechatsimple.model.databse.ConversationModel
import com.simple.wechatsimple.model.databse.MessageModel
import com.simple.wechatsimple.model.databse.UserModel
import com.simple.wechatsimple.util.TimeUtils
import com.simple.wechatsimple.util.UnreadCountManager
import com.simple.wechatsimple.util.UserInfoManager
import io.realm.Realm
import io.realm.Sort
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class RecentViewModel(private val mContext: Application,
                      private val dataRepository: DataSourceRepository) : BaseModelView(mContext) {

    init {
        EventBus.getDefault().register(this)
    }

    val messageList: ObservableList<ConversationItemModel> = ObservableArrayList()

    val userInfoManager = UserInfoManager.getInstance()

    val refreshAdapterSingleLiveEvent = SingleLiveEvent<Void>()

    val gson = Gson()

    fun getHistory() {
        val user = UserInfoManager.getInstance().getSelfUserInfo()
        Realm.getDefaultInstance().executeTransaction { realm: Realm ->
            realm.where(ConversationModel::class.java)
                    .findAll()
                    .toList()
                    .forEach {
                        if (it.conversation == Constant.PRIVATE_MESSAGE) {
                            val message = realm.where(MessageModel::class.java)
                                    .equalTo("targetId", it.targetId)
                                    .or()
                                    .equalTo("targetId", user!!.uid)
                                    .and()
                                    .equalTo("fromUserId", it.targetId)
                                    .sort("createAt", Sort.DESCENDING)
                                    .findFirst()
                            if (message != null) {
                                val item = ConversationItemModel().apply {
                                    portraitUrl = userInfoManager.getPortrait(it.targetId)
                                    title = userInfoManager.getNickName(it.targetId)
                                    conversationType = Constant.PRIVATE_MESSAGE
                                    date = TimeUtils.getMsgFormatTime(message.createAt)
                                    if (message.targetId == user!!.uid) {
                                        targetId = message.fromUserId
                                    } else {
                                        targetId = message.targetId
                                    }

                                    when (message!!.messageType) {
                                        Constant.TEXT_TYPE -> {
                                            content = String(Base64.decode(message.textMessage, Base64.DEFAULT))
                                        }

                                        Constant.IMAGE_TYPE -> {
                                            content = "[图文消息]"
                                        }

                                        Constant.VOICE_TYPE -> {
                                            content = "[语音消息]"
                                        }
                                    }
                                }
                                messageList.add(item)
                            }
                        }

                    }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUserInfo(userInfo: UserModel) {
        messageList.forEach {
            if (it.conversationType == Constant.PRIVATE_MESSAGE
                    && it.targetId == userInfo.uid) {
                it.portraitUrl = userInfo.portrait
                it.title = userInfo.nickname
                return@forEach
            }
        }

        refreshAdapterSingleLiveEvent.call()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceivedMessage(message: Muduo.IMMessage) {
        if (message.conversationType.number == Constant.PRIVATE_MESSAGE) {
            when (message.messageType) {
                Muduo.IMMessage.MessageType.IMAGE_MESSGAGE -> {
                    handlePrivateImageMessage(message.fromUserId, message.createAt)
                }

                Muduo.IMMessage.MessageType.VOICE_MESSGAGE -> {
                    handlePrivateVoiceMessage(message.fromUserId, message.createAt)
                }

                Muduo.IMMessage.MessageType.TEXT_MESSGAGE -> {
                    val jsonObject = gson.fromJson(message.extras, JsonObject::class.java)
                    handlePrivateTextMessage(
                            message.fromUserId,
                            message.createAt,
                            jsonObject["message"].asString)
                }
            }
            saveConversationModel(message.fromUserId, Constant.PRIVATE_MESSAGE)

        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSendVoiceMessage(voiceMessage: VoiceMessageItemModel) {
        if (voiceMessage.messageType == Constant.PRIVATE_MESSAGE) {
            handlePrivateVoiceMessage(
                    voiceMessage.targetId,
                    voiceMessage.createAt)

            saveConversationModel(voiceMessage.targetId, Constant.PRIVATE_MESSAGE)
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSendTextMessage(textMessage: TextMessageItemModel) {
        if (textMessage.messageType == Constant.PRIVATE_MESSAGE) {
            handlePrivateTextMessage(
                    textMessage.targetId,
                    textMessage.createAt,
                    textMessage.messageText,
                    false)

            saveConversationModel(textMessage.targetId, Constant.PRIVATE_MESSAGE)
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSendImageMessage(imageMessage: ImageMessageItemModel) {
        if (imageMessage.messageType == Constant.PRIVATE_MESSAGE) {
            handlePrivateImageMessage(
                    imageMessage.targetId,
                    imageMessage.createAt)

            saveConversationModel(imageMessage.targetId, Constant.PRIVATE_MESSAGE)
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateMessageUnreadCount(refreshUnreadCountModel: RefreshUnreadCountModel) {
        refreshAdapterSingleLiveEvent.call()
    }

    private fun handlePrivateVoiceMessage(targetId: Int, createAt: Long) {
        val index = messageList.indexOfFirst {
            it.targetId == targetId
        }
        if (index == -1) {
            val item = ConversationItemModel().apply {
                portraitUrl = userInfoManager.getPortrait(targetId)
                title = userInfoManager.getNickName(targetId)
                content = "[语音消息]"
                date = TimeUtils.getMsgFormatTime(createAt)
                conversationType = Constant.PRIVATE_MESSAGE
                this.targetId = targetId
            }
            messageList.add(item)
        } else {
            val item = messageList[index]
            item.content = "[语音消息]"

            messageList[index] = item
        }
    }

    private fun handlePrivateTextMessage(targetId: Int, createAt: Long, message: String, needDecode: Boolean = true) {
        val index = messageList.indexOfFirst {
            it.targetId == targetId
        }
        if (index == -1) {
            val item = ConversationItemModel().apply {
                portraitUrl = userInfoManager.getPortrait(targetId)
                title = userInfoManager.getNickName(targetId)
                if (needDecode) {
                    content = String(Base64.decode(message, Base64.DEFAULT))
                } else {
                    content = message
                }
                date = TimeUtils.getMsgFormatTime(createAt)
                conversationType = Constant.PRIVATE_MESSAGE
                this.targetId = targetId
            }
            messageList.add(item)
        } else {
            val item = messageList[index]
            if (needDecode) {
                item.content = String(Base64.decode(message, Base64.DEFAULT))
            } else {
                item.content = message
            }

            messageList[index] = item
        }
    }

    private fun handlePrivateImageMessage(targetId: Int, createAt: Long) {
        val index = messageList.indexOfFirst {
            it.targetId == targetId
        }
        if (index == -1) {
            val item = ConversationItemModel().apply {
                portraitUrl = userInfoManager.getPortrait(targetId)
                title = userInfoManager.getNickName(targetId)
                content = "[图片]"
                date = TimeUtils.getMsgFormatTime(createAt)
                conversationType = Constant.PRIVATE_MESSAGE
                this.targetId = targetId
            }
            messageList.add(item)
        } else {
            val item = messageList[index]
            item.content = "[图片]"

            messageList[index] = item
        }
    }

    private fun saveConversationModel(toUserId: Int, conversationType: Int) {
        Realm.getDefaultInstance().executeTransaction {
            val conversationModel = ConversationModel().apply {
                conversation = conversationType
                targetId = toUserId
            }

            it.copyToRealmOrUpdate(conversationModel)
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }
}