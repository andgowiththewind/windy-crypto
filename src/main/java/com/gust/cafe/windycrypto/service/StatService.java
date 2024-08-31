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
import com.gust.cafe.windycrypto.domain.StatDailyTask;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import com.gust.cafe.windycrypto.mapper.StatDailyTaskMapper;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
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
    //
    @Autowired
    private StatDailyTaskMapper statDailyTaskMapper;

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
        List<Windy> insightTableData = getInsightTableData(ids);
        // (2) 正在加解密的全部记录
        List<Windy> processTableData = getProcessTableData();
        // (3) 最近一分钟的IO信息
        List<JSONObject> ioList = getIoList();
        // (4) 近一年的热力图
        List<JSONObject> gridList = getHeatMapList();
        //
        // (X) 合并
        JSONObject data = JSONUtil.createObj()
                .putOpt("insightTableData", insightTableData)
                .putOpt("processTableData", processTableData)
                .putOpt("ioList", ioList)
                .putOpt("gridList", gridList);
        JSONObject resVo = JSONUtil.createObj().putOpt("code", WsMessageService.CodeEnum.CODE_555.getCode()).putOpt("data", data);
        String message = JSONUtil.toJsonStr(resVo);
        //
        // (X) 发生websocket消息
        WindyCryptoWebsocket.sendMessage(sessionId, message);
    }

    private List<JSONObject> getHeatMapList() {
        // 根据时间范围查库,365日前至昨日的数据都查缓存,今天的数据实时查
        List<StatDailyTask> tasks = new ArrayList<>();
        List<StatDailyTask> asOfYesterdayStatDailyTaskData = redisSlaveCache.getCacheObject(CacheConstants.AS_OF_YESTERDAY_STAT_DAILY_TASK_DATA);
        if (asOfYesterdayStatDailyTaskData == null) {
            DateTime min = DateUtil.offsetDay(DateUtil.date(), -365);
            String minIoDay = DateUtil.format(min, "yyyy-MM-dd");
            String maxIoDay = DateUtil.format(DateUtil.yesterday(), "yyyy-MM-dd");
            tasks = Optional.ofNullable(statDailyTaskMapper.selectHeatMapList(minIoDay, maxIoDay)).filter(CollectionUtil::isNotEmpty).orElse(new ArrayList<>());
            // 缓存
            redisMasterCache.setCacheObject(CacheConstants.AS_OF_YESTERDAY_STAT_DAILY_TASK_DATA, tasks);
        } else {
            tasks = asOfYesterdayStatDailyTaskData;
        }
        // 查今天的数据
        String today = DateUtil.format(DateUtil.date(), "yyyy-MM-dd");
        List<StatDailyTask> todayTasks = Optional.ofNullable(statDailyTaskMapper.selectHeatMapList(today, today)).filter(CollectionUtil::isNotEmpty).orElse(new ArrayList<>());
        // 合并
        tasks.addAll(todayTasks);
        //
        List<JSONObject> resultList = new ArrayList<>();
        DateTime justNow = DateUtil.date();
        // 原子Integer
        AtomicInteger atomicInteger = new AtomicInteger(0);
        for (int i = 365; i >= 0; i--) {
            DateTime offsetDay = DateUtil.offsetDay(justNow, -i);
            String dtStr = DateUtil.format(offsetDay, "yyyy-MM-dd");
            List<StatDailyTask> groupByIoDay = tasks.stream().filter(task -> task.getIoDay().equals(dtStr)).collect(Collectors.toList());
            Map<Boolean, List<StatDailyTask>> ioSuccessMap = groupByIoDay.stream().collect(Collectors.partitioningBy(task -> task.getIoSuccess().equals("1")));
            List<StatDailyTask> successPart = ioSuccessMap.get(Boolean.TRUE);
            Double ioRatePerSecondSum = successPart.stream().collect(Collectors.summingDouble(row -> Double.parseDouble(row.getIoRatePerSecond())));
            BigDecimal averageSpeed = successPart.size() == 0 ? BigDecimal.ZERO : NumberUtil.div(BigDecimal.valueOf(ioRatePerSecondSum), BigDecimal.valueOf(successPart.size()), 0);
            //
            String title = StrUtil.format("{}：[success={}], [error={}], [average speed={}]", dtStr, successPart.size(), ioSuccessMap.get(Boolean.FALSE).size(), FileUtil.readableFileSize(averageSpeed.longValue()));
            //
            JSONObject item = JSONUtil.createObj()
                    .putOpt("id", atomicInteger.incrementAndGet())
                    .putOpt("date", dtStr)
                    .putOpt("success", successPart.size())
                    .putOpt("error", ioSuccessMap.get(Boolean.FALSE).size())
                    .putOpt("title", title);
            resultList.add(item);
        }
        // 收集完再筛选当日最多成功数的元素
        Integer maxSuccess = resultList.stream().max(Comparator.comparingInt(o -> o.getInt("success"))).map(s -> s.getInt("success")).orElse(0);
        // 根据当日成功数与最大成功数的比例,计算颜色,['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b']
        resultList.forEach(jo -> {
            Integer success = jo.getInt("success");
            if (maxSuccess == 0) {
                jo.putOpt("color", "#ebedf0");
            } else {
                BigDecimal div = NumberUtil.div(BigDecimal.valueOf(success), BigDecimal.valueOf(maxSuccess), 2);
                // 比较div是否介于0~0.25
                if (NumberUtil.isGreater(div, BigDecimal.ZERO) && NumberUtil.isLessOrEqual(div, BigDecimal.valueOf(0.25))) {
                    jo.putOpt("color", "#c6e48b");
                } else if (NumberUtil.isGreaterOrEqual(div, BigDecimal.valueOf(0.25)) && NumberUtil.isLessOrEqual(div, BigDecimal.valueOf(0.5))) {
                    jo.putOpt("color", "#c6e48b");
                } else if (NumberUtil.isGreaterOrEqual(div, BigDecimal.valueOf(0.5)) && NumberUtil.isLessOrEqual(div, BigDecimal.valueOf(0.75))) {
                    jo.putOpt("color", "#7bc96f");
                } else if (NumberUtil.isGreaterOrEqual(div, BigDecimal.valueOf(0.75)) && NumberUtil.isLessOrEqual(div, BigDecimal.ONE)) {
                    jo.putOpt("color", "#239a3b");
                } else {
                    jo.putOpt("color", "#ebedf0");
                }
            }
        });
        return resultList;
    }

    private List<JSONObject> getIoList() {
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
                        .putOpt("key", DateUtil.format(dt, "HH:mm:ss"))
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
                        .putOpt("key", DateUtil.format(offsetSecond, "HH:mm:ss"))
                        .putOpt("value", 0)
                        .putOpt("datetimeStr", dtStr)
                        .putOpt("label", "0B")
                );
            }
        }
        // 倒叙
        Collections.reverse(ioList);
        return ioList;
    }

    private List<Windy> getProcessTableData() throws InterruptedException, ExecutionException {
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
        return processTableData;
    }

    private List<Windy> getInsightTableData(List<String> ids) throws InterruptedException, ExecutionException {
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
        return insightTableData;
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
