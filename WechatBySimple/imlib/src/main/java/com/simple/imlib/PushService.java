package com.simple.imlib;

import android.content.Context;
import android.util.Log;

import com.simple.imlib.handler.BusinessHandler;
import com.simple.imlib.handler.LogOutMessageHandler;
import com.simple.imlib.handler.MessageHandler;
import com.tencent.mars.wrapper.remote.PushMessage;
import com.tencent.mars.wrapper.remote.PushMessageHandler;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by caoshaokun on 16/12/20.
 */
public class PushService implements PushMessageHandler {

    private Thread recvThread;

    private LinkedBlockingQueue<PushMessage> pushMessages = new LinkedBlockingQueue<>();

    private Context context;

    private BusinessHandler[] handlers;

    public PushService(Context context) {
        this.context = context;
        handlers = new BusinessHandler[]{
                new LogOutMessageHandler(context),
                new MessageHandler(context),
        };
        this.start();
    }

    public void start() {
        if (recvThread == null) {
            recvThread = new Thread(pushReceiver, "PUSH-RECEIVER");

            recvThread.start();
        }
    }

    private final Runnable pushReceiver = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    PushMessage pushMessage = pushMessages.take();
                    if (pushMessage != null) {
                        for (BusinessHandler handler : handlers) {
                            if (handler.handlerReceivedMessage(pushMessage)) {
                                Log.i("handlerReceivedMessage", "handle");
                                break;
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        //
                    }
                }
            }
        }
    };

    @Override
    public void process(PushMessage message) {
        pushMessages.offer(message);
    }
}
