package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.esotericsoftware.minlog.Log;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.CryptoSubmitReqVo;
import io.reactivex.rxjava3.core.Completable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

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
        // 一些判断不需要放在加解密阶段
        WindyException.run((Void) -> Assert.notNull(reqVo.getAskEncrypt(), WindyLang.msg("i18n_1827983611962462208")));
        WindyException.run((Void) -> Assert.notBlank(reqVo.getUserPassword(), WindyLang.msg("i18n_1827983611962462209", "i18n_1827983611962462210")));
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
            windyList = allResults.get();
            log.debug("获取或新建Windy对象,耗时[{}ms]", timer.intervalMs());
        }
        return null;
    }
}
