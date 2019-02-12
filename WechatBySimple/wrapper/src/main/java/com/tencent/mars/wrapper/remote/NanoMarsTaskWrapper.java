/*
 * Tencent is pleased to support the open source community by making Mars available.
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.mars.wrapper.remote;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.tencent.mars.stn.StnLogic;
import com.tencent.mars.xlog.Log;

/**
 * MarsTaskWrapper using nano protocol buffer encoding
 * <p></p>
 * Created by zhaoyuan on 16/2/29.
 */
public abstract class NanoMarsTaskWrapper<T extends GeneratedMessageV3.Builder, R extends GeneratedMessageV3> extends AbstractTaskWrapper {

    private static final String TAG = "Mars.Sample.NanoMarsTaskWrapper";

    protected R response;

    public NanoMarsTaskWrapper() {
        super();
    }

    @Override
    public byte[] req2buf() {
        return PackageUtils.packageData(onPreEncode().build().toByteArray(), typeName());
    }

    @Override
    public int buf2resp(byte[] buf) {
        try {
            byte[] responseData = PackageUtils.unpackData(buf);
            response = onBufDecode(responseData);
            onPostDecode(response);
            return StnLogic.RESP_FAIL_HANDLE_NORMAL;

        } catch (Exception e) {
            Log.e(TAG, "%s", e);
        }

        return StnLogic.RESP_FAIL_HANDLE_TASK_END;
    }

    public abstract T onPreEncode();

    public abstract void onPostDecode(R response);

    public abstract R onBufDecode(byte[] buf) throws InvalidProtocolBufferException;

    public abstract String typeName();
}
