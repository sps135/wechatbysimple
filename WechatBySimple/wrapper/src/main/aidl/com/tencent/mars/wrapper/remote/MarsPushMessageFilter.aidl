// MarsRecvCallBack.aidl
package com.tencent.mars.wrapper.remote;

// Declare any non-default types here with import statements

interface MarsPushMessageFilter {

    // returns processed ?
    boolean onRecv(int cmdId, inout byte[] buffer);

}
