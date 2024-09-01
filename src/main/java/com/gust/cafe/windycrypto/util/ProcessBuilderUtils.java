package com.gust.cafe.windycrypto.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * 命令行工具类
 *
 * @author Dororo
 * @date 2024-09-01 14:48
 * @see {@link org.apache.commons.exec}
 */
@Slf4j
public class ProcessBuilderUtils {


    /**
     * @param commandWithArgs
     * @param workingDirectory
     */
    @SneakyThrows
    public static int execute(String[] commandWithArgs, File workingDirectory) {
        TimeInterval timer = DateUtil.timer();
        // 创建 ProcessBuilder 实例
        ProcessBuilder processBuilder = new ProcessBuilder(commandWithArgs);

        // 设置工作目录
        Assert.isTrue(FileUtil.isDirectory(workingDirectory), "working directory must be a directory");
        if (workingDirectory != null) processBuilder.directory(workingDirectory);

        // 合并标准错误流和标准输出流，以便输出都显示在控制台
        processBuilder.redirectErrorStream(true);

        // 启动进程
        Process process = processBuilder.start();

        // 获取输入流并逐行读取输出
        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // 等待进程完成并获取退出值
        int exitValue = process.waitFor();

        // 打印执行时间
        log.debug("process executed in {} ms with exit code {}", timer.intervalMs(), exitValue);

        // 如果进程返回值不为0，抛出异常
        if (exitValue != 0) {
            throw new RuntimeException("process failed with exit code " + exitValue);
        }

        return exitValue;
    }

}
