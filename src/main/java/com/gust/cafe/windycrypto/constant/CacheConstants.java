package com.gust.cafe.windycrypto.constant;

public class CacheConstants {
    // 一级缓存caffeine:接口防抖
    public static final String CAFFEINE_ANTI_SHAKE_LOCK = "caffeineAntiShakeLock";
    // 存放<绝对路径,ID>的map
    public static final String PATH_ID_MAP = "path_id_map";
    // 存放<ID,文件对象>的map
    public static final String WINDY_MAP = "windy_map";
    // 加锁,当通过绝对路径获取Windy对象时
    public static final String WINDY_GET_OR_DEFAULT_LOCK = "windyGetOrDefaultLock";
    // 配置更新锁
    public static final String CFG_CRUD_LOCK = "cfgCrudLock";
    // 记录某一秒的IO字节数
    public static final String IO_BYTES_BY_SECOND = "ioBytesBySecond";
}