package com.gust.cafe.windycrypto;

import cn.hutool.system.SystemUtil;
import org.junit.jupiter.api.Test;

public class JustTests {

    @Test
    public void test01() {
        String homeDir = SystemUtil.getUserInfo().getHomeDir();
        System.out.println(homeDir);
    }
}
