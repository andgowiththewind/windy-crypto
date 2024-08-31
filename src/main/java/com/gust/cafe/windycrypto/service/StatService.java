package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.components.RedisSlaveCache;
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

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


@Slf4j
@Service
public class StatService {
    @Autowired
    private RedisMasterCache redisMasterCache;
    @Autowired
    private RedisSlaveCache redisSlaveCache;// 高频数据请求必须走从库
    @Autowired
    @Qualifier(ThreadPoolConstants.STAT)
    private ThreadPoolTaskExecutor statTaskExecutor;

    /**
     * 返回两个表格的数据 & IO信息
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
        // (3) 最近一分钟的IO信息
        Collection<String> ioKeys = redisSlaveCache.keys(StrUtil.format("{}_*", CacheConstants.LAST_MINUTE_IO));
        List<JSONObject> ioList = new ArrayList<>();
        List<JSONObject> resultList = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(ioKeys)) {
            List<CompletableFuture<JSONObject>> futureList = ioKeys.stream().map(key -> CompletableFuture.supplyAsync(() -> {
                String dtStr = StrUtil.subAfter(key, StrUtil.format("{}_", CacheConstants.LAST_MINUTE_IO), false);
                DateTime dt = DateUtil.parse(dtStr, "yyyyMMddHHmmss");
                //
                List<String> longList = redisSlaveCache.listGetAll(key);
                BigDecimal total = BigDecimal.ZERO;
                for (String longStr : longList) {
                    long longVal = Long.parseLong(longStr);
                    BigDecimal bigDecimalVal = BigDecimal.valueOf(longVal);
                    total = NumberUtil.add(total, bigDecimalVal);
                }
                return JSONUtil.createObj()
                        .putOpt("key", DateUtil.format(dt, "yyyy-MM-dd HH:mm:ss"))
                        .putOpt("value", total.longValue())
                        .putOpt("datetimeStr", DateUtil.format(dt, "yyyy-MM-dd HH:mm:ss"))
                        .putOpt("label", FileUtil.readableFileSize(total.longValue()));
            }, statTaskExecutor)).collect(Collectors.toList());
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
            CompletableFuture<List<JSONObject>> allResults = allFutures.thenApply(v -> futureList.stream().map(CompletableFuture::join).collect(Collectors.toList()));
            resultList = allResults.join();
            //
        }
        // 从当前时刻往前推2分钟
        DateTime justNow = DateUtil.date();
        for (int i = 0; i < 60; i++) {
            DateTime offsetSecond = DateUtil.offsetSecond(justNow, -(i + 1));
            String dtStr = DateUtil.format(offsetSecond, "yyyy-MM-dd HH:mm:ss");
            // 如果`resultList`有对应的记录,则收集,否则补0
            Optional<JSONObject> first = resultList.stream().filter(jo -> jo.getStr("datetimeStr").equals(dtStr)).findFirst();
            if (first.isPresent()) {
                ioList.add(first.get());
            } else {
                ioList.add(JSONUtil.createObj()
                        .putOpt("key", dtStr)
                        .putOpt("value", 0)
                        .putOpt("datetimeStr", dtStr)
                        .putOpt("label", "0B")
                );
            }
        }
        // 倒叙
        Collections.reverse(ioList);

        //
        // (X) 合并
        JSONObject data = JSONUtil.createObj().putOpt("insightTableData", insightTableData).putOpt("processTableData", processTableData).putOpt("ioList", ioList);
        JSONObject resVo = JSONUtil.createObj().putOpt("code", WsMessageService.CodeEnum.CODE_555.getCode()).putOpt("data", data);
        String message = JSONUtil.toJsonStr(resVo);
        //
        // (X) 发生websocket消息
        WindyCryptoWebsocket.sendMessage(sessionId, message);
    }

    /**
     * 记录秒级别的本次处理的字节数,用于统计
     *
     * @param subBytes
     * @see {@link com.gust.cafe.windycrypto.job.IoBytesBySecondJob#delLastMinuteIo} 删除的操作写在JOB中
     */
    public void addSecondLevelBytes(String subBytes) {
        CompletableFuture.runAsync(() -> {
            DateTime atThisMoment = DateUtil.date();
            String dt = DateUtil.format(atThisMoment, "yyyyMMddHHmmss");
            String mapKey = StrUtil.format("{}_{}", CacheConstants.LAST_MINUTE_IO, dt);
            redisMasterCache.listRightPushValue(mapKey, ListUtil.toList(subBytes));
        }, statTaskExecutor).whenComplete(getBiConsumer());
    }

    private static BiConsumer<Void, Throwable> getBiConsumer() {
        return (v, e) -> {
            if (e != null) {
                log.error("addSecondLevelBytes error", e);
            }
        };
    }
}
