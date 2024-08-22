package com.gust.cafe.windycrypto.constant;

/**
 * 线程池名称常量
 *
 * @author Dororo
 * @date 2023-12-04 12:10
 */
public class ThreadPoolConstants {
    // 任务分发使用的线程池
    public static final String DISPATCH = "dispatch";
    // 处理加密解密实际操作线程池
    public static final String CRYPTO = "crypto";
    // 统计线程池、发送WS消息
    public static final String STAT = "stat";
}
