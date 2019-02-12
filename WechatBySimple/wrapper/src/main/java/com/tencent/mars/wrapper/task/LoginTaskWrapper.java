package com.tencent.mars.wrapper.task;

import com.google.protobuf.InvalidProtocolBufferException;
import com.proto.imlib.Muduo;
import com.tencent.mars.stn.StnLogic;
import com.tencent.mars.wrapper.TaskProperty;
import com.tencent.mars.wrapper.remote.NanoMarsTaskWrapper;

@TaskProperty(
        host = "localhost",
        path = "/mars/sendmessage",
        cmdID = 10001,
        longChannelSupport = true,
        shortChannelSupport = false
)
public class LoginTaskWrapper extends NanoMarsTaskWrapper<Muduo.LoginRequest.Builder, Muduo.Response> {

    private int mUserId;

    public LoginTaskWrapper(int userId) {
        super();
        mUserId = userId;
    }

    @Override
    public Muduo.LoginRequest.Builder onPreEncode() {
        return Muduo.LoginRequest.newBuilder().setUserId(mUserId);
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

        }
    }

    @Override
    public String typeName() {
        return "muduo.LoginRequest";
    }
}