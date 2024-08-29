package com.gust.cafe.windycrypto.components;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

@Slf4j
// @Component
public class AppLock {

    private FileLock fileLock;
    private FileChannel fileChannel;

    @PostConstruct
    public void lockFile() throws Exception {
        File file = FileUtil.file(SystemUtil.getUserInfo().getHomeDir(), "windy-crypto-app.lock");
        if (!FileUtil.exist(file)) {
            FileUtil.touch(file);
            FileUtil.writeUtf8String("make sure only one windy-crypto app allows it", file);
        }
        //
        fileChannel = new FileOutputStream(file).getChannel();
        fileLock = fileChannel.tryLock();

        if (fileLock == null) {
            Console.error("====================================================================================================");
            Console.error("Unable to acquire lock on app.lock file. Another instance might be running.");
            Console.error("无法在app.lock文件上获取锁。 另一个实例可能正在运行。请不必重复允许当前应用程序。");
            Console.error("====================================================================================================");
            throw new IllegalStateException("Unable to acquire lock on app.lock file. Another instance might be running.");
        }

        System.out.println("Lock acquired on app.lock file.");
    }

    @PreDestroy
    public void releaseFileLock() throws Exception {
        if (fileLock != null) {
            fileLock.release();
            System.out.println("Lock released on app.lock file.");
        }
        if (fileChannel != null) {
            fileChannel.close();
        }
    }
}