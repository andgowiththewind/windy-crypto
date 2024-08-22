package com.gust.cafe.windycrypto.constant;

import cn.hutool.core.util.StrUtil;
import freemarker.template.Configuration;
import freemarker.template.Version;

public class CommonConstants {
    public static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_32;
    public static final String TMP_EXT_NAME = "windytmp";
    public static final String ENCRYPTED_MARK = "safeLockedV2";
    public static final String ENCRYPTED_PREFIX = StrUtil.format("${}$", ENCRYPTED_MARK);
}
