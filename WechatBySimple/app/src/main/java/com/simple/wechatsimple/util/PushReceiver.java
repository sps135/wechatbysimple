package com.simple.wechatsimple.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.proto.imlib.Muduo;
import com.simple.imlib.constant.Constant;
import com.simple.wechatsimple.model.action.LogOutAction;
import com.simple.wechatsimple.model.action.LongLinkStatusActionModel;
import com.simple.wechatsimple.model.action.RefreshUnreadCountModel;
import com.simple.wechatsimple.model.databse.MessageModel;

import org.greenrobot.eventbus.EventBus;

import io.realm.Realm;

public class PushReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Constant.LOG_OUT_ACTION.equals(intent.getAction())) {
            EventBus.getDefault().post(new LogOutAction());
        } else if (Constant.MESSAGE_ACTION.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            Muduo.IMMessage message = (Muduo.IMMessage) bundle.getSerializable("message");

            try {
                Realm.getDefaultInstance().beginTransaction();
                MessageModel viewModel;
                viewModel = Realm.getDefaultInstance().createObject(MessageModel.class, message.getId());
                viewModel.setFromUserId(message.getFromUserId());
                viewModel.setTargetId((int) message.getTargetId());
                switch (message.getMessageType().getNumber()) {
                    case 1:
                        viewModel.setMessageType(Constant.TEXT_TYPE);
                        break;
                    case 2:
                        viewModel.setMessageType(Constant.IMAGE_TYPE);
                        break;
                    case 3:
                        viewModel.setMessageType(Constant.VOICE_TYPE);
                        break;
                }
                viewModel.setConversationType(message.getConversationType().getNumber());
                viewModel.setCreateAt(message.getCreateAt());
                viewModel.setReceived(true);
                viewModel.setExtras(message.getExtras());
                Realm.getDefaultInstance().commitTransaction();
                if (message.getConversationType().getNumber() == Constant.PRIVATE_MESSAGE) {
                    UnreadCountManager.getInstance().recordUnreadMessage(message.getFromUserId(), message.getId());

                    RefreshUnreadCountModel notify = new RefreshUnreadCountModel();
                    notify.setTargetId(viewModel.getTargetId());
                    EventBus.getDefault().post(notify);
                }

                NotificationInterface.getInstance().sendNotification(context, viewModel);
            } catch (Exception e) {
                e.printStackTrace();
            }

            EventBus.getDefault().post(message);
        } else if ("LONG_LINK_STATUS".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            int longLinkStatus = bundle.getInt("longLinkStatus");

            LongLinkStatusActionModel model = new LongLinkStatusActionModel();
            model.setStatus(longLinkStatus);
            EventBus.getDefault().post(model);
        }
    }
}
