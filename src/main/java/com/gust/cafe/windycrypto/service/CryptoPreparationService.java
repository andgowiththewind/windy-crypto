package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.CryptoSubmitReqVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CryptoPreparationService {
    @Autowired
    @Qualifier(ThreadPoolConstants.DISPATCH)
    private ThreadPoolTaskExecutor dispatchTaskExecutor;
    @Autowired
    private WindyCacheService windyCacheService;


    @SneakyThrows
    public List<String> prepare(CryptoSubmitReqVo reqVo) {
        // 根据传参将所有的文件转为Windy缓存对象
        List<Windy> windyCache = getWindyCache(reqVo);
        // 一些筛选判断不需要放在加解密阶段
        List<Windy> windyFilter = filterWindyCache(windyCache, reqVo);
        // 传递绝对路径即可
        List<String> absPathList = windyFilter.stream().map(Windy::getAbsPath).collect(Collectors.toList());
        return absPathList;
    }

    private List<Windy> filterWindyCache(List<Windy> windyCache, CryptoSubmitReqVo reqVo) {
        Boolean askEncrypt = reqVo.getAskEncrypt();
        List<Windy> collect = windyCache.stream().filter(cache -> {
            // 仅空闲状态的文件才能被加解密
            WindyStatusEnum anEnum = WindyStatusEnum.getByCode(cache.getCode());
            boolean statusMatch = anEnum != null && anEnum.equals(WindyStatusEnum.FREE);
            if (!statusMatch) return false;
            // 如果是加密任务,排除已经加密过的文件;如果是解密任务,排除未加密过的文件
            Boolean hadEncrypted = cache.getHadEncrypted();
            if (askEncrypt && hadEncrypted) return false;
            if (!askEncrypt && !hadEncrypted) return false;
            return true;
        }).collect(Collectors.toList());
        return collect;
    }

    private List<Windy> getWindyCache(CryptoSubmitReqVo reqVo) throws InterruptedException, ExecutionException {
        // 一些判断不需要放在加解密阶段
        WindyException.run((Void) -> Assert.notNull(reqVo.getAskEncrypt(), WindyLang.msg("i18n_1827983611962462208")));
        WindyException.run((Void -> {
            String userPassword = reqVo.getUserPassword();
            Assert.notBlank(userPassword, WindyLang.msg("i18n_1827983611962462209", "i18n_1827983611962462210"));
            // 8~32位限制
            Assert.isTrue(userPassword.length() >= 8 && userPassword.length() <= 32, WindyLang.msg("i18n_1828312465394503680"));
        }));
        //
        List<Windy> windyList = new ArrayList<>();
        //
        TimeInterval timer = DateUtil.timer();
        if (CollectionUtil.isNotEmpty(reqVo.getWindyPathList())) {
            // 创建多个 CompletableFuture 任务,每个任务都使用独立的线程
            List<CompletableFuture<Windy>> futureList = reqVo.getWindyPathList().stream()
                    .map(path -> CompletableFuture.supplyAsync(() -> windyCacheService.lockGetOrDefault(path)))
                    .collect(Collectors.toList());
            // 使用 CompletableFuture.allOf 等待所有任务完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));// 0是惯用写法
            // 当所有任务完成后，收集每个任务的返回值
            CompletableFuture<List<Windy>> allResults = allFutures.thenApply(v ->
                    futureList.stream()
                            .map(CompletableFuture::join) // 使用 join 获取结果
                            .collect(Collectors.toList())
            );
            // 收集所有任务的结果
            List<Windy> byWindyPath = allResults.get();
            windyList.addAll(byWindyPath);
            log.debug("根据windy路径获取或新建Windy对象,耗时[{}ms]", timer.intervalMs());
        }

        if (CollectionUtil.isNotEmpty(reqVo.getDirPathList())) {
            // 创建多个 CompletableFuture 任务,每个任务都使用独立的线程
            List<File> collect = reqVo.getDirPathList().stream()
                    .map(dirPath -> FileUtil.loopFiles(dirPath))
                    .filter(CollectionUtil::isNotEmpty)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            List<CompletableFuture<Windy>> futureList = collect.stream()
                    .map(FileUtil::getAbsolutePath)
                    .map(path -> CompletableFuture.supplyAsync(() -> windyCacheService.lockGetOrDefault(path), dispatchTaskExecutor))
                    .collect(Collectors.toList());
            // 使用 CompletableFuture.allOf 等待所有任务完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));// 0是惯用写法
            // 当所有任务完成后，收集每个任务的返回值
            CompletableFuture<List<Windy>> allResults = allFutures.thenApply(v ->
                    futureList.stream()
                            .map(CompletableFuture::join) // 使用 join 获取结果
                            .collect(Collectors.toList())
            );
            // 收集所有任务的结果
            List<Windy> byDirPath = allResults.get();
            windyList.addAll(byDirPath);
            log.debug("根据目录路径获取或新建Windy对象,耗时[{}ms]", timer.intervalMs());
        }

        // 根据ID去重
        Map<String, Windy> collect = windyList.stream().collect(Collectors.toMap(Windy::getId, windy -> windy, (oldValue, newValue) -> oldValue));
        List<Windy> afterDistinct = collect.values().stream().collect(Collectors.toList());
        log.debug("去重后的Windy对象数量[{}],耗时[{}ms]", afterDistinct.size(), timer.intervalMs());
        return afterDistinct;
    }
}
