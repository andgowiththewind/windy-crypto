package com.gust.cafe.windycrypto.pkg;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;

import java.io.File;
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
        File from = FileUtil.file(SystemUtil.getUserInfo().getCurrentDir(), "attachments");
        File to = FileUtil.file(SystemUtil.getUserInfo().getCurrentDir(), "target/attachments");
        FileUtil.copyContent(from, to, true);
        Console.log("拷贝成功：{} ===> {}", FileUtil.getAbsolutePath(from), FileUtil.getAbsolutePath(to));
        //

    }
}
