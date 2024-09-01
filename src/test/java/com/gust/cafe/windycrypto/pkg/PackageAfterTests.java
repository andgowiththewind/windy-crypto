package com.gust.cafe.windycrypto.pkg;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

public class PackageAfterTests {
    public static void main(String[] args) {
        String repeat = StrUtil.repeat("=", 100);
        String simpleName = PackageAfterTests.class.getSimpleName();
        //
        Console.error(StrUtil.format("{} {} {} {}", repeat, simpleName, "start", repeat));
        extracted();
        Console.error(StrUtil.format("{} {} {} {}", repeat, simpleName, "end  ", repeat));
    }

    private static void extracted() {

    }
}
