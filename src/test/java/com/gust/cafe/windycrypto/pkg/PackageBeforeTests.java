package com.gust.cafe.windycrypto.pkg;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;

import java.util.Arrays;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PackageBeforeTests {
    public static void main(String[] args) {
        Console.error("======================================PackageBeforeTests");
        // 创建命令行
        // CommandLine cmdLine = new CommandLine("cmd");
        // cmdLine.addArgument("/c");
        // cmdLine.addArgument("npm run build:prod");

        // npm install --registry=https://registry.npmmirror.com

        CommandLine cmdLine = new CommandLine("npm.cmd");// win环境需要使用`npm.cmd`
        cmdLine.addArgument("installw");
        cmdLine.addArgument("--registry=https://registry.npmmirror.com");

        // 设置输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        // 创建执行器
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(FileUtil.file("D:/gust/dev/project/github/windy-crypto/web-ui"));
        executor.setStreamHandler(streamHandler);

        // 异步执行命令
        ExecuteResultHandler resultHandler = new ExecuteResultHandler() {
            @Override
            public void onProcessComplete(int exitValue) {
                System.out.println("Process completed with exit value: " + exitValue);
                System.out.println("Output: " + outputStream.toString());
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                System.err.println("Process failed: " + e.getMessage());
            }
        };

        try {
            executor.execute(cmdLine, resultHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
