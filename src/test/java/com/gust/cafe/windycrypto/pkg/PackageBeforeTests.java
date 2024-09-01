package com.gust.cafe.windycrypto.pkg;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;

import java.io.File;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.gust.cafe.windycrypto.util.ProcessBuilderUtils;

public class PackageBeforeTests {


    public static void main(String[] args) {
        String repeat = StrUtil.repeat("=", 100);
        String simpleName = PackageBeforeTests.class.getSimpleName();
        //
        Console.error(StrUtil.format("{} {} {} {}", repeat, simpleName, "start", repeat));
        extracted();
        Console.error(StrUtil.format("{} {} {} {}", repeat, simpleName, "end  ", repeat));
    }

    private static void extracted() {
        // 注意win环境使用`npm.cmd`代替`npm`
        String[] installArgs = ArrayUtil.toArray(ListUtil.toList("npm.cmd", "install", "--registry=https://registry.npmmirror.com"), String.class);
        File workingDirectory = FileUtil.file(SystemUtil.getUserInfo().getCurrentDir(), "web-ui");
        ProcessBuilderUtils.execute(installArgs, workingDirectory);
        Console.log("安装成功");
        //
        String[] buildArgs = ArrayUtil.toArray(ListUtil.toList("npm.cmd", "run", "build:prod"), String.class);
        ProcessBuilderUtils.execute(buildArgs, workingDirectory);
        Console.log("构建成功");
        //
        // 拷贝到`resources/static`目录
        File from = FileUtil.file(SystemUtil.getUserInfo().getCurrentDir(), "web-ui/dist");
        File to = FileUtil.file(SystemUtil.getUserInfo().getCurrentDir(), "src/main/resources/static");
        FileUtil.copyContent(from, to, true);
        Console.log("拷贝成功");
    }
}
