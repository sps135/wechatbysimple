package com.simple.imlib.constant

class Constant {
    companion object {
        const val PACKAGE_NAME = "com.simple.wechatsimple"
        const val CLS = "com.simple.wechatsimple.util.PushReceiver"

        const val PRIVATE_MESSAGE = 1
        const val GROUP_MESSAGE = 2

        const val FTP_SERVER_URL = "XXX.XXX.XXX.XXX"
        const val FTP_SERVER_BASE_URL = "http://XXX.XXX.XXX.XXX:XX"
        const val DEFAULT_PORTRAIT_URL = "http://XXX.XXX.XXX.XXX:XX/DefaultPortrait.jpg"
        const val FTP_SERVER_PORT = 21
        const val FTP_USERNAME = "XXX"
        const val FTP_PASSWORD = "XXX"

        const val LOG_OUT_CMDID = 20000
        const val MESSAGE_CMDID = 20001

        const val LOG_OUT_ACTION = "LOG_OUT"
        const val MESSAGE_ACTION = "MESSAGE"

        const val IMAGE_TYPE = 1
        const val TEXT_TYPE = 2
        const val AUDIO_TYPE = 3
        const val VOICE_TYPE = 4
    }
}