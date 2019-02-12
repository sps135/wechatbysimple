package com.simple.wechatsimple.data.source.excutor

import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DiskIOThreadExecutor : Executor {
    private val diskIO = Executors.newSingleThreadExecutor()

    override fun execute(command: Runnable?) {
    }
}