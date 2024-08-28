package com.gust.cafe.windycrypto.constant;

import cn.hutool.core.util.StrUtil;
import freemarker.template.Configuration;
import freemarker.template.Version;

public class CommonConstants {
    public static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_32;
    public static final String TMP_EXT_NAME = "windytmp";// 临时文件后缀
    public static final String CFG_EXT_NAME = "windycfg";// 配置文件后缀
    public static final String ENCRYPTED_MARK = "safeLockedV2";// 已加密文件名标记,代码上忽略大小写
    public static final String ENCRYPTED_SEPARATOR = "$";// 已加密文件名分隔符
    public static final String ENCRYPTED_PREFIX = StrUtil.format("{}{}{}", ENCRYPTED_SEPARATOR, ENCRYPTED_MARK, ENCRYPTED_SEPARATOR);// eg: $safeLockedV2$
}
