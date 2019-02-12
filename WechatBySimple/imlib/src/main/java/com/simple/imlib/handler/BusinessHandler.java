package com.simple.imlib.handler;

import com.tencent.mars.wrapper.remote.PushMessage;

public abstract class BusinessHandler {

    public abstract boolean handlerReceivedMessage(PushMessage pushMessage);

}
