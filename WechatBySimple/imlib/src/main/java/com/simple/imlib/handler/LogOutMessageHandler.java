package com.simple.imlib.handler;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.simple.imlib.constant.Constant;
import com.tencent.mars.wrapper.remote.PushMessage;

public class LogOutMessageHandler extends BusinessHandler {
    private Context context;

    public LogOutMessageHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean handlerReceivedMessage(PushMessage pushMessage) {
        if (pushMessage.cmdId == Constant.LOG_OUT_CMDID) {
            Intent intent = new Intent();
            intent.setAction(Constant.LOG_OUT_ACTION);
            intent.setComponent(new ComponentName(Constant.PACKAGE_NAME, Constant.CLS));
            context.sendBroadcast(intent);
            return true;
        }
        return false;
    }
}
