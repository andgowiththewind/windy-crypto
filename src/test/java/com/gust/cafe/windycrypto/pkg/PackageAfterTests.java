package com.gust.cafe.windycrypto.pkg;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.SneakyThrows;
import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;


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
        sqlite();
        redis();
        exe4j();
    }

    @SneakyThrows
    private static void sqlite() {
        Optional<File> max = Arrays.stream(FileUtil.ls(FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "attachments/db"))))
                .filter(File::isFile)
                .filter(f -> StrUtil.startWithIgnoreCase(FileUtil.getName(f), "windy_crypto_") && StrUtil.endWithIgnoreCase(FileUtil.getName(f), ".zip"))
                .max((f1, f2) -> {
                    String time1 = StrUtil.subBetween(FileUtil.getName(f1), "windy_crypto_", ".zip");
                    String time2 = StrUtil.subBetween(FileUtil.getName(f2), "windy_crypto_", ".zip");
                    return time1.compareTo(time2);
                });
        Assert.isTrue(max.isPresent(), "sqlite文件不存在");
        File sqliteZip = max.get();
        File toDir = FileUtil.file(getCurrentDir(), "target/attachments/db");
        new ZipFile(sqliteZip).extractAll(FileUtil.getAbsolutePath(toDir));
        //
        File bat = FileUtil.file(getCurrentDir(), "attachments/db/100---实时打包数据库文件.bat");
        if (FileUtil.exist(bat) && FileUtil.isFile(bat)) {
            File batCopy = FileUtil.file(getCurrentDir(), "attachments/db", FileUtil.getName(bat));
            FileUtil.copy(bat, batCopy, true);
        }
    }

    private static void redis() {
    }

    private static void exe4j() {
    }


    private static String getCurrentDir() {
        return SystemUtil.getUserInfo().getCurrentDir();
    }
}
