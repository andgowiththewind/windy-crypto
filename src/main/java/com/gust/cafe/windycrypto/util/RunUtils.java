package com.gust.cafe.windycrypto.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 准备启动环境
 *
 * @author Dororo
 * @date 2024-08-22 16:50
 */
@Slf4j
public class RunUtils {

    @SneakyThrows
    public static void check() {
        TimeInterval mainThreadTimer = DateUtil.timer();
        //
        CompletableFuture<ResultDTO> checkSqliteFuture = CompletableFuture.supplyAsync(() -> checkSqlite());
        CompletableFuture<ResultDTO> checkRedisFuture = CompletableFuture.supplyAsync(() -> checkRedis());
        // 合并两个异步任务
        CompletableFuture<ResultDTO> complexFuture = checkSqliteFuture.thenCombine(checkRedisFuture, (sqliteResult, redisResult) -> {
            if (sqliteResult.getSuccess() && redisResult.getSuccess()) {
                return ResultDTO.builder().success(true).message("全部调节准备完毕").build();
            } else {
                String msg = CollectionUtil.toList(sqliteResult.getMessage(), redisResult.getMessage()).stream().collect(Collectors.joining(";"));
                return ResultDTO.builder().success(false).message(msg).build();
            }
        });
        // 阻塞主线程
        ResultDTO resultDTO = complexFuture.get();
        if (!resultDTO.getSuccess()) {
            log.info("运行环境检查正常,耗时[{}]ms", mainThreadTimer.intervalMs());
            throw new RuntimeException(resultDTO.getMessage());
        }
        //
        // 启动`attachments/.redis/打开两个REDIS.bat`
        String currentDir = SystemUtil.getUserInfo().getCurrentDir();
        File file = FileUtil.file(currentDir, "attachments", "redis", "打开两个REDIS.bat");
        executeCommand(file.getAbsolutePath());
    }

    private static ResultDTO checkRedis() {
        return null;
    }

    private static ResultDTO checkSqlite() {
        return null;
    }

    @SneakyThrows
    private static void executeCommand(String batPath) {

        Assert.isTrue(FileUtil.exist(batPath) && FileUtil.isFile(batPath), "文件不存在或不是文件:{}", batPath);
        String parent = FileUtil.getParent(batPath, 1);
        String name = FileUtil.getName(batPath);
        String cmdPart = StrUtil.format("cd {} && call {}", parent, name);
        // 创建进程构造器对象并设置要运行的命令及参数
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", cmdPart);
        // 将标准输入流、错误输出流合并到同一个流中
        processBuilder.redirectErrorStream(true);
        // 开始执行命令
        Process process = processBuilder.start();
        // 等待命令执行完成
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Batch file executed successfully.");
        } else {
            System.err.println("Failed to execute batch file with error code: " + exitCode);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultDTO {
        private Boolean success;
        private String message;
    }
}
