package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gust.cafe.windycrypto.components.RedisSlaveCache;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import com.gust.cafe.windycrypto.websocket.WindyCryptoWebsocket;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Service
public class StatService {
    @Autowired
    private RedisSlaveCache redisSlaveCache;// 高频数据请求必须走从库
    @Autowired
    @Qualifier(ThreadPoolConstants.STAT)
    private ThreadPoolTaskExecutor statTaskExecutor;

    /**
     * 返回两个表格的数据
     * <p>都使用线程池进行优化</p>
     *
     * @param sessionId ws连接的唯一标识
     * @param ids       第一个表格的id集合
     */
    @SneakyThrows
    public void onMessageUpdateTableCache(String sessionId, List<String> ids) {
        // (1) 条件分页查询的table数据
        List<Windy> insightTableData = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(ids)) {
            List<CompletableFuture<Windy>> futureList = ids.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> {
                        Windy cacheMapValue = redisSlaveCache.getCacheMapValue(CacheConstants.WINDY_MAP, id);
                        return cacheMapValue;
                    }, statTaskExecutor))
                    .collect(Collectors.toList());
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
            CompletableFuture<List<Windy>> allResults = allFutures.thenApply(v -> futureList.stream().map(CompletableFuture::join).collect(Collectors.toList()));
            List<Windy> windyList = allResults.get();
            windyList.forEach(windy -> Optional.ofNullable(windy).filter(w -> w != null).ifPresent(insightTableData::add));
        }
        //
        // (2) 正在加解密的全部记录
        List<Windy> processTableData = new ArrayList<>();
        Map<String, Windy> windyMap = redisSlaveCache.getCacheMap(CacheConstants.WINDY_MAP);
        Set<String> idSet = windyMap.keySet();
        if (CollectionUtil.isNotEmpty(idSet)) {
            List<CompletableFuture<Windy>> futureList = idSet.stream().map(id -> CompletableFuture.supplyAsync(() -> {
                Windy windy = windyMap.get(id);
                // 涉及筛选,如果不符合条件,返回null,后续合并结果时根据是否为null进行判断
                if (windy != null && windy.getCode() != null) {
                    boolean matchCode = windy.getCode().intValue() == WindyStatusEnum.OUTPUTTING.getCode() || windy.getCode().intValue() == WindyStatusEnum.ALMOST.getCode();
                    return matchCode ? windy : null;
                }
                return null;
            }, statTaskExecutor)).collect(Collectors.toList());
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
            CompletableFuture<List<Windy>> allResults = allFutures.thenApply(v -> futureList.stream().map(CompletableFuture::join).collect(Collectors.toList()));
            List<Windy> windyList = allResults.get();
            windyList.forEach(windy -> Optional.ofNullable(windy).filter(w -> w != null).ifPresent(processTableData::add));
        }
        //
        //
        // (3) 合并
        JSONObject data = JSONUtil.createObj().putOpt("insightTableData", insightTableData).putOpt("processTableData", processTableData);
        JSONObject resVo = JSONUtil.createObj().putOpt("code", WsMessageService.CodeEnum.CODE_555.getCode()).putOpt("data", data);
        String message = JSONUtil.toJsonStr(resVo);
        //
        // (4) 发生websocket消息
        WindyCryptoWebsocket.sendMessage(sessionId, message);
    }

    // 记录秒级别的本次处理的字节数,用于统计
    public void addSecondLevelBytes(double subBytes) {
        CompletableFuture.runAsync(() -> {
            DateTime atThisMoment = DateUtil.date();
            String dt = DateUtil.format(atThisMoment, "yyyyMMddHHmmss");
            String mapKey = StrUtil.format("{}_{}", CacheConstants.IO_BYTES_BY_SECOND, dt);



            

        }, statTaskExecutor).whenComplete((v, e) -> {
            if (e != null) {
                log.error("addSecondLevelBytes error", e);
            }
        });
    }
}
