package com.gust.cafe.windycrypto;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import org.apache.ibatis.logging.LogException;
import org.junit.jupiter.api.Test;

import java.io.File;

public class JustTests {

    @Test
    public void test01() {
    }

    @Test
    public void mkdirTest() {
        String currentDir = SystemUtil.getUserInfo().getCurrentDir();
        File file = FileUtil.file(currentDir, "target", "测试被加解密顶级目录");
        try {
            FileUtil.del(file);
        } catch (IORuntimeException e) {
            Console.error(e.getMessage());
        }
        FileUtil.mkdir(file);
        for (int i = 1; i < 2; i++) {
            String mark = StrUtil.fillAfter(Convert.toStr(i), '0', 4);
            File sonFile = FileUtil.file(file, mark + ".txt");
            FileUtil.writeUtf8String(mark, sonFile);
        }
    }
}