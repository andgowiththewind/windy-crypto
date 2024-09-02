package com.gust.cafe.windycrypto.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
                return ResultDTO.builder().success(true).message("all adjustments are ready").build();
            } else {
                String msg = CollectionUtil.toList(sqliteResult.getMessage(), redisResult.getMessage()).stream().collect(Collectors.joining(";"));
                return ResultDTO.builder().success(false).message(msg).build();
            }
        }).exceptionally(e -> {
            log.error("unknown anomaly => ", e);
            throw new RuntimeException(e);
        });
        // 阻塞主线程
        ResultDTO resultDTO = complexFuture.get();
        if (!resultDTO.getSuccess()) {
            log.info("the running environment is checked normally, which takes [{0}] ms.", mainThreadTimer.intervalMs());
            throw new RuntimeException(resultDTO.getMessage());
        }
        //
        // 启动`attachments/.redis/打开两个REDIS.bat`
        String currentDir = SystemUtil.getUserInfo().getCurrentDir();
        File file = FileUtil.file(currentDir, "attachments", "redis", "打开两个REDIS.bat");
        executeCommand(file.getAbsolutePath());
    }

    // 主要通过判断`redis-server.exe`文件判断REDIS是否解压缩成功,如果没有则解压缩
    // 主要通过判断`up_redis_windows.conf`文件判断是否渲染成功,如果没有则渲染
    @SneakyThrows
    private static ResultDTO checkRedis() {
        try {
            TimeInterval timer = DateUtil.timer();
            String currentDir = SystemUtil.getUserInfo().getCurrentDir();
            File zip = FileUtil.file(currentDir, "attachments", "redis", "REDIS-X64-3.2.100.zip");
            ArrayList<File> arrayList = ListUtil.toList(
                    FileUtil.file(currentDir, "attachments", "redis", "redis01"),
                    FileUtil.file(currentDir, "attachments", "redis", "redis02")
            );
            //
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("masterPort", "6391");
            dataModel.put("slavePort", "6392");
            dataModel.put("masterPassword", "gust.cafe");
            //
            for (File redisDir : arrayList) {
                File redisServerExe = FileUtil.file(redisDir, "redis-server.exe");
                boolean rseOk = FileUtil.exist(redisServerExe) && FileUtil.isFile(redisServerExe);
                // 如果不存在则解压缩
                Assert.isTrue(FileUtil.exist(zip) && FileUtil.isFile(zip), "file does not exist or is not a file :{}", zip);
                if (!rseOk) new ZipFile(zip).extractAll(FileUtil.getAbsolutePath(redisDir));
                File upRedisWindowsConf = FileUtil.file(redisDir, "up_redis_windows.conf");
                // 如果不存在则渲染
                boolean urwcOk = FileUtil.exist(upRedisWindowsConf) && FileUtil.isFile(upRedisWindowsConf);
                if (!urwcOk) {
                    FreeMarkerUtils.renderFile(FreeMarkerUtils.FmConfig.builder()
                            .directoryForTemplateLoading(redisDir)
                            .dataModel(dataModel)
                            .templateName("up_redis_windows.conf.ftl")
                            .outputFile(upRedisWindowsConf)
                            .build());
                    log.info("rendering file success [{}]", upRedisWindowsConf.getAbsolutePath());
                }
            }
            log.info("redis environmental inspection is completed, which takes [{}] ms.", timer.intervalMs());
            return ResultDTO.builder().success(true).message(StrUtil.format("redis environmental inspection is completed, which takes [{}] ms.", timer.intervalMs())).build();
        } catch (ZipException e) {
            log.error("redis check failed. => ", e);
            return ResultDTO.builder().success(false).message("failed to decompress file.").build();
        }
    }

    //
    private static ResultDTO checkSqlite() {
        try {
            TimeInterval timer = DateUtil.timer();
            String currentDir = SystemUtil.getUserInfo().getCurrentDir();
            File sqliteFile = FileUtil.file(currentDir, "attachments", "db", "windy-crypto.sqlite");
            if (FileUtil.exist(sqliteFile) && FileUtil.isFile(sqliteFile)) {
                return ResultDTO.builder().success(true).message(StrUtil.format("The sqlite file exists, which took [{}] ms.", timer.intervalMs())).build();
            }
            // 否则在目录下找最新时间的zip进行解压缩
            File parent = FileUtil.getParent(sqliteFile, 1);
            // windy_crypto_20240822_161418.zip
            // windy_crypto_20240823_171418.zip
            Arrays.stream(FileUtil.ls(FileUtil.getAbsolutePath(parent)))
                    // 过滤出zip文件
                    .filter(f -> StrUtil.startWithIgnoreCase(f.getName(), "windy_crypto_") && StrUtil.endWithIgnoreCase(f.getName(), ".zip"))
                    // 比较文件名中的时间部分,返回最新的文件
                    .max((f1, f2) -> {
                        String time1 = StrUtil.subBetween(f1.getName(), "windy_crypto_", ".zip");
                        String time2 = StrUtil.subBetween(f2.getName(), "windy_crypto_", ".zip");
                        return time1.compareTo(time2);
                    })
                    // 解压缩
                    .ifPresent(f -> {
                        try {
                            new ZipFile(f).extractAll(FileUtil.getAbsolutePath(parent));
                        } catch (ZipException e) {
                            log.error("failed to decompress file [{}]", f.getAbsolutePath());
                            throw new RuntimeException(e);
                        }
                    });
            // 再次检查sqlite文件
            if (FileUtil.exist(sqliteFile) && FileUtil.isFile(sqliteFile)) {
                return ResultDTO.builder().success(true).message(StrUtil.format("the sqlite file exists, which took [{}] ms.", timer.intervalMs())).build();
            } else {
                return ResultDTO.builder().success(false).message(StrUtil.format("the sqlite file does not exist, which takes [{}] ms.", timer.intervalMs())).build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private static void executeCommand(String batPath) {

        Assert.isTrue(FileUtil.exist(batPath) && FileUtil.isFile(batPath), "file does not exist or is not a file :{}", batPath);
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
