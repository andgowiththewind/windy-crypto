package com.gust.cafe.windycrypto;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.gust.cafe.windycrypto.util.AesUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class JustTests {

    @Test
    public void test01() {
        String name = FileUtil.getName("C:\\Users\\Administrator\\Desktop\\1.2.txt");
        System.out.println(name);
    }
}
