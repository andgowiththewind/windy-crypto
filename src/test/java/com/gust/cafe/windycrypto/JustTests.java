package com.gust.cafe.windycrypto;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.gust.cafe.windycrypto.util.ProcessBuilderUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JustTests {

    @Test
    public void test01() {
        // {"npm.cmd", "installAAA", "--registry=https://registry.npmmirror.com"}
        String[] commandWithArgs = ArrayUtil.toArray(ListUtil.toList("npm.cmd", "install----test---AAA", "--registry=https://registry.npmmirror.com"), String.class);
        // String[] commandWithArgs = ArrayUtil.toArray(ListUtil.toList("npm.cmd", "install", "--registry=https://registry.npmmirror.com"), String.class);
        File workingDirectory = FileUtil.file("D:/gust/dev/project/github/windy-crypto/web-ui");
        ProcessBuilderUtils.execute(commandWithArgs, workingDirectory);
        System.out.println("=====================");
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

    @Test
    public void i18nTest() {
        // 为前后端同时增加翻译
        String zh = "安全起见,小于8个字节的文件不允许加解密";
        // 纯小写
        String en = "For security reasons, files less than 8 bytes are not allowed";
        String id = IdUtil.getSnowflakeNextIdStr();
        String currentDir = SystemUtil.getUserInfo().getCurrentDir();
        String format = StrUtil.format("i18n_{}={}", id, zh);
        ArrayList<File> list = ListUtil.toList(FileUtil.file(currentDir, "src/main/resources/messages.properties"), FileUtil.file(currentDir, "src/main/resources/messages_zh.properties"));
        list.forEach(file -> {
            FileUtil.appendUtf8Lines(ListUtil.of(format), file);
        });
        //
        File file = FileUtil.file(currentDir, "src/main/resources/messages_en.properties");
        FileUtil.appendUtf8Lines(ListUtil.of(StrUtil.format("i18n_{}={}", id, en)), file);
        //
        String format1 = StrUtil.format("    i18n_{}: '{}',", id, zh);
        File file1 = FileUtil.file(currentDir, "web-ui/src/lang/zh.js");
        List<String> lines = FileUtil.readUtf8Lines(file1);
        List<String> newLines = new ArrayList<>();
        newLines.add(lines.get(0));
        for (int i = 0; i < lines.size(); i++) {
            if (!(i == 0 || i == lines.size() - 1)) {
                newLines.add(lines.get(i));
            }
        }
        newLines.add(format1);
        newLines.add(lines.get(lines.size() - 1));
        FileUtil.writeUtf8Lines(newLines, file1);
        //
        String format2 = StrUtil.format("    i18n_{}: '{}',", id, en);
        File file2 = FileUtil.file(currentDir, "web-ui/src/lang/en.js");
        List<String> lines2 = FileUtil.readUtf8Lines(file2);
        List<String> newLines2 = new ArrayList<>();
        newLines2.add(lines2.get(0));
        for (int i = 0; i < lines2.size(); i++) {
            if (!(i == 0 || i == lines2.size() - 1)) {
                newLines2.add(lines2.get(i));
            }
        }
        newLines2.add(format2);
        newLines2.add(lines2.get(lines2.size() - 1));
        FileUtil.writeUtf8Lines(newLines2, file2);
    }


}