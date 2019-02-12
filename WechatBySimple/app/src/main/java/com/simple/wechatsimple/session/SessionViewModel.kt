package com.simple.wechatsimple.session

import android.app.Application
import android.databinding.ObservableArrayList
import android.databinding.ObservableField
import android.databinding.ObservableList
import android.text.style.ImageSpan
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.proto.imlib.Muduo
import com.simple.imlib.IMClient
import com.simple.imlib.constant.Constant
import com.simple.imlib.listener.ISendTextMessageListener
import com.simple.imlib.listener.IUploadImageListener
import com.simple.imlib.listener.IUploadVoiceListener
import com.simple.wechatsimple.base.BaseModelView
import com.simple.wechatsimple.base.IBaseBindingAdapterItem
import com.simple.wechatsimple.base.SingleLiveEvent
import com.simple.wechatsimple.data.source.DataSourceRepository
import com.simple.wechatsimple.model.BaseMessageItemModel
import com.simple.wechatsimple.model.ImageMessageItemModel
import com.simple.wechatsimple.model.TextMessageItemModel
import com.simple.wechatsimple.model.VoiceMessageItemModel
import com.simple.wechatsimple.model.databse.MessageModel
import com.simple.wechatsimple.model.databse.UserModel
import com.simple.wechatsimple.util.MoonUtils
import com.simple.wechatsimple.util.UnreadCountManager
import com.simple.wechatsimple.util.UserInfoManager
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SessionViewModel(private val mContext: Application,
                       private val dataRepository: DataSourceRepository) : BaseModelView(mContext) {

    init {
        EventBus.getDefault().register(this)
    }

    val messageList: ObservableList<BaseMessageItemModel> = ObservableArrayList()

    val userInfoManager = UserInfoManager.getInstance()

    val refreshAdapterSingleLiveEvent = SingleLiveEvent<Void>()

    val movePositionSingleLiveEvent = SingleLiveEvent<Void>()

    val stopRefreshSingleLiveEvent = SingleLiveEvent<Void>()

    val messageEditText = ObservableField<String>()

    val gson = Gson()

    var mConversationType: Int? = null

    var mTargetId: Int? = null

    fun initConversationInfo(conversationType: Int, targetId: Int) {
        mConversationType = conversationType
        mTargetId = targetId
    }

    fun retry(item: IBaseBindingAdapterItem) {
        when (item) {
            is VoiceMessageItemModel -> {
                sendVoice(item)
            }
        }
    }

    fun loadMore() {
        if (messageList.size == 0) {
            return
        }
        getHistoryMessage(messageList[0].createAt)
    }

    fun getHistoryMessage(createTime: Long) {
        val user = UserInfoManager.getInstance().getSelfUserInfo()
        var resultList = mutableListOf<BaseMessageItemModel>()
        var decNum = 0
        val result: RealmQuery<MessageModel>

        if (createTime > 0) {
            result = getHistory(user!!.uid, createTime)
        } else {
            result = getHistory(user!!.uid)
        }

        result
                .findAll()
                .toList()
                .reversed()
                .map {
                    when (it.messageType) {
                        Constant.IMAGE_TYPE -> {
                            ImageMessageItemModel()
                                    .apply {
                                        id = it.id
                                        fromUserId = it.fromUserId
                                        targetId = it.targetId
                                        messageType = it.conversationType
                                        createAt = it.createAt
                                        portrait = userInfoManager.getPortrait(fromUserId)
                                        nickname = userInfoManager.getNickName(fromUserId)
                                        isReceived = it.isReceived
                                        if (isReceived) {
                                            decNum.inc()
                                        }
                                        isShowLoading = false
                                        isShowError = false
                                        imageUri = it.imageUri
                                    }
                        }
                        Constant.TEXT_TYPE -> {
                            TextMessageItemModel()
                                    .apply {
                                        id = it.id
                                        fromUserId = it.fromUserId
                                        targetId = it.targetId
                                        messageType = it.conversationType
                                        createAt = it.createAt
                                        portrait = userInfoManager.getPortrait(fromUserId)
                                        nickname = userInfoManager.getNickName(fromUserId)
                                        isReceived = it.isReceived
                                        if (isReceived) {
                                            decNum.inc()
                                        }
                                        isShowLoading = false
                                        isShowError = false
                                        message = MoonUtils.identifyFaceExpression(
                                                mContext,
                                                String(Base64.decode(it.textMessage, Base64.DEFAULT)),
                                                ImageSpan.ALIGN_BOTTOM,
                                                MoonUtils.DEF_SCALE)
                                    }
                        }
                        Constant.VOICE_TYPE -> {
                            VoiceMessageItemModel()
                                    .apply {
                                        id = it.id
                                        fromUserId = it.fromUserId
                                        targetId = it.targetId
                                        messageType = it.conversationType
                                        createAt = it.createAt
                                        portrait = userInfoManager.getPortrait(fromUserId)
                                        nickname = userInfoManager.getNickName(fromUserId)
                                        isReceived = it.isReceived
                                        if (isReceived) {
                                            decNum.inc()
                                        }
                                        isShowLoading = false
                                        isShowError = false
                                        audioUri = it.audioUri
                                        duration = it.audioDuration
                                    }
                        }
                        else -> {
                            BaseMessageItemModel()
                        }
                    }
                }.apply {
                    resultList.addAll(this)
                }
        val resultArrayList = arrayListOf<BaseMessageItemModel>()
        resultArrayList.addAll(resultList)
        resultArrayList.addAll(messageList.toList())

        resultArrayList.forEachIndexed { index, baseMessageItemModel ->
            if (index == 0) {
                baseMessageItemModel.isNeedShowDateLabel = true
            } else {
                val prevItem = resultArrayList[index - 1]
                with(prevItem) {
                    if (isReceived != baseMessageItemModel.isReceived) {
                        baseMessageItemModel.isNeedShowDateLabel = true
                    } else {
                        baseMessageItemModel.isNeedShowDateLabel =
                                (baseMessageItemModel.createAt - createAt) > 60 * 5 * 1000
                    }
                }
            }
            UnreadCountManager.getInstance().removeMessageUnreadRecord(baseMessageItemModel.id)
        }

        messageList.clear()
        messageList.addAll(resultArrayList)

        if (createTime.toInt() == 0) {
            movePositionSingleLiveEvent.call()
        }
        stopRefreshSingleLiveEvent.call()
    }

    fun getHistory(selfId: Int): RealmQuery<MessageModel> {
        return Realm.getDefaultInstance().where(MessageModel::class.java)
                .equalTo("targetId", mTargetId)
                .or()
                .equalTo("targetId", selfId)
                .and()
                .equalTo("fromUserId", mTargetId)
                .sort("createAt", Sort.DESCENDING)
    }

    fun getHistory(selfId: Int, createTime: Long): RealmQuery<MessageModel> {
        return Realm.getDefaultInstance().where(MessageModel::class.java)
                .equalTo("targetId", mTargetId)
                .and()
                .lessThan("createAt", createTime)
                .or()
                .equalTo("targetId", selfId)
                .and()
                .equalTo("fromUserId", mTargetId)
                .and()
                .lessThan("createAt", createTime)
                .sort("createAt", Sort.DESCENDING)
                .limit(10)
    }


    fun sendImage(uploadId: String?, path: String) {
        val user = UserInfoManager.getInstance().getSelfUserInfo()
        val currentTime = System.currentTimeMillis()
        IMClient.getInstance().sendImageMessage(
                uploadId,
                user!!.uid,
                mTargetId!!,
                currentTime,
                path,
                mConversationType!!,
                object : IUploadImageListener {
                    override fun onStart(messageId: String) {
                        if (uploadId != null) {
                            changeSendingState(messageId, true, false)
                        } else {
                            val viewModel = MessageModel().apply {
                                id = messageId
                                fromUserId = user.uid
                                targetId = mTargetId!!
                                conversationType = mConversationType!!
                                messageType = Constant.IMAGE_TYPE
                                createAt = currentTime
                                isReceived = false
                                imageUri = path
                            }

                            Realm.getDefaultInstance().executeTransaction {
                                it.copyToRealm(viewModel)
                            }

                            val itemModel = ImageMessageItemModel().apply {
                                id = messageId
                                fromUserId = user.uid
                                targetId = mTargetId!!
                                messageType = mConversationType!!
                                createAt = currentTime
                                portrait = userInfoManager.getPortrait(fromUserId)
                                nickname = userInfoManager.getNickName(fromUserId)
                                isReceived = false
                                isNeedShowDateLabel = isNeedShowTimeLabel(false, createAt)
                                isShowLoading = true
                                isShowError = false
                                imageUri = path
                            }

                            EventBus.getDefault().post(itemModel)

                            messageList.add(itemModel)
                            movePositionSingleLiveEvent.call()
                        }
                    }

                    override fun onProgress(messageId: String, percent: Int) {
                        changeProgress(messageId, percent)
                    }

                    override fun onCancel(messageId: String) {
                    }

                    override fun onComplete(messageId: String) {
                        changeSendingState(messageId, false, false)
                    }

                    override fun onError(messageId: String) {
                        changeSendingState(messageId, false, true)
                    }

                })
    }

    fun sendVoice(item: VoiceMessageItemModel) {
        sendVoice(item.id, item.audioUri, item.duration)
    }

    fun sendVoice(uploadId: String?, path: String, audioDuration: Int) {
        val user = UserInfoManager.getInstance().getSelfUserInfo()
        val currentTime = System.currentTimeMillis()
        IMClient.getInstance().sendVoiceMessage(
                uploadId,
                user!!.uid,
                mTargetId!!,
                currentTime,
                mConversationType!!,
                audioDuration,
                path,
                object : IUploadVoiceListener {
                    override fun onStart(messageId: String) {
                        if (uploadId != null) {
                            changeSendingState(messageId, true, false)
                        } else {
                            val viewModel = MessageModel().apply {
                                id = messageId
                                fromUserId = user.uid
                                targetId = mTargetId!!
                                conversationType = mConversationType!!
                                messageType = Constant.VOICE_TYPE
                                createAt = currentTime
                                isReceived = false

                                val extrasValue = JsonObject()
                                extrasValue.addProperty("audioUri", path)
                                extrasValue.addProperty("audioDuration", audioDuration)
                                extras = extrasValue.toString()
                            }

                            Realm.getDefaultInstance().executeTransaction {
                                it.copyToRealm(viewModel)
                            }

                            val itemModel = VoiceMessageItemModel().apply {
                                id = messageId
                                fromUserId = user.uid
                                targetId = mTargetId!!
                                messageType = mConversationType!!
                                createAt = currentTime
                                portrait = userInfoManager.getPortrait(fromUserId)
                                nickname = userInfoManager.getNickName(fromUserId)
                                isReceived = false
                                isNeedShowDateLabel = isNeedShowTimeLabel(false, createAt)
                                isShowLoading = true
                                isShowError = false
                                duration = audioDuration
                                audioUri = path
                            }

                            EventBus.getDefault().post(itemModel)

                            messageList.add(itemModel)
                            movePositionSingleLiveEvent.call()
                        }
                    }

                    override fun onComplete(messageId: String) {
                        changeSendingState(messageId, false, false)
                    }

                    override fun onError(messageId: String, errorMessage: String) {
                        changeSendingState(messageId, false, true)
                    }

                })
    }

    fun sendTextMessage() {
        sendTextMessage(null)
    }

    fun sendTextMessage(uploadId: String?) {
        val user = UserInfoManager.getInstance().getSelfUserInfo()
        val currentTime = System.currentTimeMillis()
        IMClient.getInstance().sendTextMessage(
                uploadId,
                user!!.uid,
                mTargetId!!,
                mConversationType!!,
                currentTime,
                messageEditText.get()!!,
                object : ISendTextMessageListener {
                    override fun onStart(messageId: String) {
                        if (uploadId != null) {
                            changeSendingState(messageId, true, false)
                        } else {
                            val viewModel = MessageModel().apply {
                                id = messageId
                                fromUserId = user.uid
                                targetId = mTargetId!!
                                conversationType = mConversationType!!
                                messageType = Constant.TEXT_TYPE
                                createAt = currentTime
                                isReceived = false
                                textMessage = String(Base64.encode(messageEditText.get()!!.toByteArray(), Base64.DEFAULT))
                            }

                            Realm.getDefaultInstance().executeTransaction {
                                it.copyToRealm(viewModel)
                            }

                            val itemModel = TextMessageItemModel().apply {
                                id = messageId
                                fromUserId = user.uid
                                targetId = mTargetId!!
                                messageType = mConversationType!!
                                createAt = currentTime
                                portrait = userInfoManager.getPortrait(fromUserId)
                                nickname = userInfoManager.getNickName(fromUserId)
                                isReceived = false
                                isNeedShowDateLabel = isNeedShowTimeLabel(false, createAt)
                                isShowLoading = true
                                isShowError = false
                                message = MoonUtils.identifyFaceExpression(mContext, messageEditText.get(), ImageSpan.ALIGN_BOTTOM, MoonUtils.DEF_SCALE)
                                messageText = messageEditText.get()
                            }

                            EventBus.getDefault().post(itemModel)

                            messageList.add(itemModel)
                            movePositionSingleLiveEvent.call()
                        }
                    }

                    override fun onComplete(messageId: String) {
                        changeSendingState(messageId, false, false)
                    }

                    override fun onError(messageId: String, errorMessage: String) {
                        changeSendingState(messageId, false, true)
                    }
                })
    }

    private fun changeSendingState(messageId: String, showLoading: Boolean, showError: Boolean) {
        val messageModel = messageList.find {
            it.id == messageId
        }
        if (messageModel != null) {
            with(messageModel) {
                isShowLoading = showLoading
                isShowError = showError
            }
            val index = messageList.indexOf(messageModel)
            messageList[index] = messageModel
        }
    }

    private fun changeProgress(messageId: String, percent: Int) {
        val messageModel = messageList.find {
            it.id == messageId
        }
        if (messageModel != null) {
            with(messageModel) {
                progress = percent
            }
            val index = messageList.indexOf(messageModel)
            messageList[index] = messageModel
        }
    }

    private fun isNeedShowTimeLabel(isReceiveNext: Boolean, createAtNext: Long): Boolean {
        if (messageList.size > 0) {
            val prevItem = messageList[messageList.size - 1]
            with(prevItem) {
                if (isReceived != isReceiveNext) {
                    return true
                }
                return createAtNext - createAt > 60 * 5 * 1000
            }
        } else {
            return true
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUserInfo(userInfo: UserModel) {
        messageList.forEach {
            if (userInfo.uid == it.fromUserId) {
                it.portrait = userInfo.portrait
                it.nickname = userInfo.nickname
            }
        }
        refreshAdapterSingleLiveEvent.call()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(message: Muduo.IMMessage) {
        val user = Realm.getDefaultInstance().where(UserModel::class.java).findFirst()
        if ((message.targetId.toInt() != 0 && message.targetId.toInt() == user!!.uid)
                || (message.targetId.toInt() != 0 && message.targetId.toInt() == mTargetId)) {
            when (message.messageType) {
                Muduo.IMMessage.MessageType.VOICE_MESSGAGE -> {
                    handleVoiceMessage(message)
                }

                Muduo.IMMessage.MessageType.TEXT_MESSGAGE -> {
                    handleTextMessage(message)
                }

                Muduo.IMMessage.MessageType.IMAGE_MESSGAGE -> {
                    handleImageMessage(message)
                }

                else -> {

                }
            }
        }


    }

    private fun handleVoiceMessage(message: Muduo.IMMessage) {
        with(message) {
            val itemModel = VoiceMessageItemModel()
            itemModel.id = id
            itemModel.fromUserId = fromUserId
            itemModel.targetId = targetId.toInt()
            itemModel.messageType = mConversationType!!
            itemModel.createAt = createAt
            itemModel.portrait = userInfoManager.getPortrait(message.fromUserId)
            itemModel.nickname = userInfoManager.getNickName(message.fromUserId)
            itemModel.isReceived = true
            itemModel.isNeedShowDateLabel = isNeedShowTimeLabel(true, message.createAt)
            itemModel.isShowLoading = false
            itemModel.isShowError = false

            gson.fromJson(message.extras, JsonObject::class.java).apply {
                itemModel.duration = get("duration").asInt
                itemModel.audioUri = get("duration").asString
            }

            messageList.add(itemModel)
            movePositionSingleLiveEvent.call()

            UnreadCountManager.getInstance().removeMessageUnreadRecord(itemModel.id)
        }
    }

    private fun handleTextMessage(message: Muduo.IMMessage) {
        with(message) {
            val itemModel = TextMessageItemModel()
            itemModel.id = id
            itemModel.fromUserId = fromUserId
            itemModel.targetId = targetId.toInt()
            itemModel.messageType = mConversationType!!
            itemModel.createAt = createAt
            itemModel.portrait = userInfoManager.getPortrait(message.fromUserId)
            itemModel.nickname = userInfoManager.getNickName(message.fromUserId)
            itemModel.isReceived = true
            itemModel.isNeedShowDateLabel = isNeedShowTimeLabel(true, message.createAt)
            itemModel.isShowLoading = false
            itemModel.isShowError = false

            gson.fromJson(message.extras, JsonObject::class.java).apply {
                itemModel.messageText = String(Base64.decode(get("message").asString, Base64.DEFAULT))
                itemModel.message = MoonUtils.identifyFaceExpression(mContext, itemModel.messageText, ImageSpan.ALIGN_BOTTOM, MoonUtils.DEF_SCALE)
            }

            messageList.add(itemModel)
            movePositionSingleLiveEvent.call()

            UnreadCountManager.getInstance().removeMessageUnreadRecord(itemModel.id)
        }
    }

    private fun handleImageMessage(message: Muduo.IMMessage) {
        with(message) {
            val itemModel = ImageMessageItemModel()
            itemModel.id = id
            itemModel.fromUserId = fromUserId
            itemModel.targetId = targetId.toInt()
            itemModel.messageType = mConversationType!!
            itemModel.createAt = createAt
            itemModel.portrait = userInfoManager.getPortrait(message.fromUserId)
            itemModel.nickname = userInfoManager.getNickName(message.fromUserId)
            itemModel.isReceived = true
            itemModel.isNeedShowDateLabel = isNeedShowTimeLabel(true, message.createAt)
            itemModel.isShowLoading = false
            itemModel.isShowError = false

            gson.fromJson(message.extras, JsonObject::class.java).apply {
                itemModel.imageUri = get("imageUri").asString
            }

            messageList.add(itemModel)
            movePositionSingleLiveEvent.call()

            UnreadCountManager.getInstance().removeMessageUnreadRecord(itemModel.id)
        }
    }
}
