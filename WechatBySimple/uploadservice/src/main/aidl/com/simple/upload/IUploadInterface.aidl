// IUploadInterface.aidl
package com.simple.upload;

// Declare any non-default types here with import statements
import com.simple.upload.FTPUploadTaskParameters;
import com.simple.upload.UploadStatusDelegate;

interface IUploadInterface {

    void upload(in FTPUploadTaskParameters params);

    void registerDelegate(UploadStatusDelegate delegate, String id);
}
