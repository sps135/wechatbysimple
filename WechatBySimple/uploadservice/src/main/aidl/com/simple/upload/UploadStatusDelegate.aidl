package com.simple.upload;

import com.simple.upload.UploadInfo;
import com.simple.upload.ServerResponse;

interface UploadStatusDelegate {

    void onStart(String id);

    void onProgress(in UploadInfo uploadInfo);

    void onError(in UploadInfo uploadInfo, in ServerResponse serverResponse, String exception);

    void onCompleted(in UploadInfo uploadInfo, in ServerResponse serverResponse);

    void onCancelled(in UploadInfo uploadInfo);
}
