package com.gust.cafe.windycrypto.constant;

public class CacheConstants {
    // 一级缓存caffeine:接口防抖
    public static final String CAFFEINE_ANTI_SHAKE_LOCK = "caffeineAntiShakeLock";
    // 存放<绝对路径,ID>的map
    public static final String PATH_ID_MAP = "pathIdMap";
    // 存放<ID,文件对象>的map
    public static final String WINDY_MAP = "windyMap";
    // 加锁,当通过绝对路径获取Windy对象时
    public static final String WINDY_GET_OR_DEFAULT_LOCK = "windyGetOrDefaultLock";
    // 加锁,当通过绝对路径更新Windy对象时,一般在关键状态变更时使用,同一状态下频繁更新信息时不建议使用
    public static final String WINDY_UPDATE_LOCK = "windyUpdateLock";
}