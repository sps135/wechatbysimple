package com.simple.imlib.task;

import com.google.protobuf.InvalidProtocolBufferException;
import com.proto.imlib.Muduo;
import com.simple.imlib.listener.ITaskWrapperListener;
import com.tencent.mars.stn.StnLogic;
import com.tencent.mars.wrapper.TaskProperty;
import com.tencent.mars.wrapper.remote.NanoMarsTaskWrapper;

@TaskProperty(
        host = "localhost",
        path = "",
        cmdID = 10001,
        longChannelSupport = true,
        shortChannelSupport = false
)
public class MessageTaskWrapper extends NanoMarsTaskWrapper<Muduo.IMMessage.Builder, Muduo.Response> {

    private String id;

    private int fromUserId;

    private int targetId;

    Muduo.IMMessage.MessageType messageType;

    Muduo.IMMessage.ConversationType conversationType;

    private long createAt;

    private String extras;

    private ITaskWrapperListener listener;

    public MessageTaskWrapper(String id,
                              int fromUserId,
                              int targetId,
                              long createAt,
                              String extras,
                              Muduo.IMMessage.MessageType messageType,
                              Muduo.IMMessage.ConversationType conversationType,
                              ITaskWrapperListener listener) {
        super();
        this.id = id;
        this.fromUserId = fromUserId;
        this.targetId = targetId;
        this.messageType = messageType;
        this.conversationType = conversationType;
        this.createAt = createAt;
        this.extras = extras;
        this.listener = listener;
    }

    @Override
    public Muduo.IMMessage.Builder onPreEncode() {
        Muduo.IMMessage.Builder builder = Muduo.IMMessage.newBuilder()
                .setId(id)
                .setFromUserId(fromUserId)
                .setTargetId(targetId)
                .setMessageType(messageType)
                .setConversationType(conversationType)
                .setCreateAt(createAt)
                .setExtras(extras);

        return builder;
    }

    @Override
    public Muduo.Response onBufDecode(byte[] buf) throws InvalidProtocolBufferException {
        return Muduo.Response.parseFrom(buf);
    }

    @Override
    public void onPostDecode(Muduo.Response response) {

    }

    @Override
    public void onTaskEnd(int errType, int errCode) {
        if (errCode == StnLogic.RESP_FAIL_HANDLE_NORMAL) {
            if (response.getErrorCode() == Muduo.Response.ErrorCode.NO_ERROR.getNumber()) {
                listener.onComplete();
            } else {
                listener.onError(response.getErrorMsg());
            }
        }
    }

    @Override
    public String typeName() {
        return "muduo.IMMessage";
    }
}
