package com.gust.cafe.windycrypto.pkg;

import cn.hutool.core.lang.Console;

import java.util.Arrays;

public class PackageBeforeTests {
    public static void main(String[] args) {
        Console.error("======================================PackageBeforeTests");
        Arrays.stream(args).forEach(System.out::println);
    }
}
