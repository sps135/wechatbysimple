package com.simple.imlib.task;

import com.google.protobuf.InvalidProtocolBufferException;
import com.proto.imlib.Muduo;
import com.tencent.mars.stn.StnLogic;
import com.tencent.mars.wrapper.TaskProperty;
import com.tencent.mars.wrapper.remote.NanoMarsTaskWrapper;

@TaskProperty(
        host = "localhost",
        path = "",
        cmdID = 10009,
        longChannelSupport = true,
        shortChannelSupport = false
)
public class HistoryTaskWrapper extends NanoMarsTaskWrapper<Muduo.HistoryMessage.Builder, Muduo.Response> {

    private int id;

    public HistoryTaskWrapper(int id) {
        super();
        this.id = id;
    }

    @Override
    public Muduo.HistoryMessage.Builder onPreEncode() {
        Muduo.HistoryMessage.Builder builder = Muduo.HistoryMessage.newBuilder()
                .setId(id);

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

        }
    }

    @Override
    public String typeName() {
        return "muduo.HistoryMessage";
    }
}
