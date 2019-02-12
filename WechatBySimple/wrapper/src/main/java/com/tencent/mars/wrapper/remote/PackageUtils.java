package com.tencent.mars.wrapper.remote;

import java.util.ArrayList;

public class PackageUtils {
    static {
        System.loadLibrary("native-lib");
    }

    public static byte[] packageData(byte[] data, String typeName) {
        return packageDataNative(data, data.length, typeName);
    }

    public static byte[] unpackData(byte[] data) {
        return unpackDataNative(data, data.length);
    }

    private static native byte[] packageDataNative(byte[] data, int length, String typeName);

    private static native byte[] unpackDataNative(byte[] data, int length);
}
