package com.simple.imlib.handler;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.proto.imlib.Muduo;
import com.simple.imlib.constant.Constant;
import com.tencent.mars.wrapper.remote.PackageUtils;
import com.tencent.mars.wrapper.remote.PushMessage;

public class MessageHandler extends BusinessHandler {
    private Context context;

    public MessageHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean handlerReceivedMessage(PushMessage pushMessage) {
        Log.i("MessageHandler", "handlerReceivedMessage");
        if (pushMessage.cmdId == Constant.MESSAGE_CMDID) {
            try {
                Log.i("MessageHandler", "unpackData");
                byte[] responseData = PackageUtils.unpackData(pushMessage.buffer);
                Log.i("MessageHandler", "parseData");
                Muduo.IMMessage message = Muduo.IMMessage.parseFrom(responseData);

                Intent intent = new Intent();
                intent.setAction(Constant.MESSAGE_ACTION);

                Bundle bundle = new Bundle();
                bundle.putSerializable("message", message);
                intent.putExtras(bundle);

                intent.setComponent(new ComponentName(Constant.PACKAGE_NAME, Constant.CLS));
                context.sendBroadcast(intent);
            } catch (Exception e) {
                Log.i("VoiceMessageHandler", e.getMessage());
                return false;
            }
            return true;
        }
        return false;
    }
}
