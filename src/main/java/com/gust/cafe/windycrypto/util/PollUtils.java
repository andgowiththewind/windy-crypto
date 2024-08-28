package com.gust.cafe.windycrypto.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 轮询工具类
 *
 * @author Dororo
 * @date 2024-08-28 15:15
 */
public class PollUtils {
    /**
     * 轮询
     *
     * @param intervalMs 间隔毫秒
     * @param maxMs      最大毫秒
     * @param actionCs   执行动作,如果业务上认为执行失败需要主动抛出异常
     * @param successCs  成功回调
     * @param errorCs    失败回调
     */
    public static void poll(long intervalMs, long maxMs, Consumer<Void> actionCs, Consumer<Void> successCs, Consumer<Void> errorCs) {
        TimeInterval timer = DateUtil.timer();
        boolean finalSuccess = false;
        while (NumberUtil.compare(timer.intervalMs(), maxMs) <= 0) {
            try {
                actionCs.accept(null);
                finalSuccess = true;
                break;
            } catch (Exception e) {
                // Ignore
            }
            ThreadUtil.sleep(intervalMs);
        }
        if (finalSuccess) {
            Optional.ofNullable(successCs).filter(Objects::nonNull).ifPresent(consumer -> consumer.accept(null));
        } else {
            Optional.ofNullable(errorCs).filter(Objects::nonNull).ifPresent(consumer -> consumer.accept(null));
        }
    }
}
