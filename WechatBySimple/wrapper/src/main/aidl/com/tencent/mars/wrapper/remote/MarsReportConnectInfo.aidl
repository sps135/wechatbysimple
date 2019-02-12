// MarsRecvCallBack.aidl
package com.tencent.mars.wrapper.remote;

// Declare any non-default types here with import statements

interface MarsReportConnectInfo {

    // returns processed ?
    void reportConnectInfo(int status, int longlinkstatus);

}
