package com.gust.cafe.windycrypto.mvn;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.system.SystemUtil;
import com.gust.cafe.windycrypto.util.FreeMarkerUtils;
import lombok.SneakyThrows;
import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.util.*;


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
        config();
        exe4j();
    }

    private static void config() {
        // JAR包打包进EXE后无法修改配置文件,因此需要将配置文件放在外部`config`目录下,方便内部SpringBoot读取
        File configDir = FileUtil.file(getCurrentDir(), "target/config");
        FileUtil.mkdir(configDir);
        // `config`目录至少有一个文件,否则`innoSetup`将报错
        FileUtil.touch(FileUtil.file(configDir, ".gitkeep"));
        // 拷贝全部yml
        Arrays.stream(FileUtil.ls(FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "src/main/resources"))))
                .filter(f -> StrUtil.endWithIgnoreCase(FileUtil.getName(f), ".yml"))
                .forEach(f -> FileUtil.copy(f, FileUtil.file(configDir, FileUtil.getName(f)), true));
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
        Console.error("已释放sqlite文件");
        //
        File bat = FileUtil.file(getCurrentDir(), "attachments/db/100---实时打包数据库文件.bat");
        if (FileUtil.exist(bat) && FileUtil.isFile(bat)) {
            File batCopy = FileUtil.file(getCurrentDir(), "target", "attachments/db", FileUtil.getName(bat));
            FileUtil.copy(bat, batCopy, true);
            Console.error("已拷贝bat文件: {}", FileUtil.getName(bat));
        }

    }

    @SneakyThrows
    private static void redis() {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("masterPort", "6391");
        dataModel.put("slavePort", "6392");
        dataModel.put("masterPassword", "gust.cafe");
        File redisZip = FileUtil.file(getCurrentDir(), "attachments/redis/REDIS-X64-3.2.100.zip");
        //
        JSONObject redis01 = JSONUtil.createObj()
                .putOpt("from", FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "attachments/redis/redis01")))
                .putOpt("to", FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "target/attachments/redis/redis01")));
        JSONObject redis02 = JSONUtil.createObj()
                .putOpt("from", FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "attachments/redis/redis02")))
                .putOpt("to", FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "target/attachments/redis/redis02")));
        for (JSONObject entries : ListUtil.toList(redis01, redis02)) {
            // 释放redis文件
            new ZipFile(redisZip).extractAll(entries.getStr("to"));
            Console.error("已释放redis文件：{}", entries.getStr("to"));
            // 渲染多个模板文件
            ArrayList<String> templateNameList = ListUtil.toList("up.bat", "up_redis_windows.conf.ftl");
            for (String templateName : templateNameList) {
                FreeMarkerUtils.FmConfig fmConfig = FreeMarkerUtils.FmConfig.builder()
                        .directoryForTemplateLoading(FileUtil.file(entries.getStr("from")))
                        .dataModel(dataModel)
                        .templateName(templateName)
                        .outputFile(FileUtil.file(entries.getStr("to"), templateName))
                        .build();
                FreeMarkerUtils.renderFile(fmConfig);
                Console.error("已渲染模板文件：{}", templateName);
            }
            // 一个脚本
            File bat2 = FileUtil.file(getCurrentDir(), "attachments/redis/打开两个REDIS.bat");
            if (FileUtil.exist(bat2) && FileUtil.isFile(bat2)) {
                File batCopy = FileUtil.file(getCurrentDir(), "target", "attachments/redis", FileUtil.getName(bat2));
                FileUtil.copy(bat2, batCopy, true);
                Console.error("已拷贝bat文件: {}", FileUtil.getName(bat2));
            }
        }
    }

    private static void exe4j() {
        exe4jHandleExe2jar();
        exe4jHandleJar2Setup();

    }

    private static void exe4jHandleExe2jar() {
        // exe4j软件不会自动创建目录
        FileUtil.mkdir(FileUtil.file(getCurrentDir(), "target/exe-jar2exe"));
        // exe4j 配置文件
        File directoryForTemplateLoading = FileUtil.file(getCurrentDir(), "attachments/freemarker/exe");
        String templateName = "jar2exe.exe4j.ftl";
        File outputFile = FileUtil.file(getCurrentDir(), "target/windy-crypto-jar2exe.exe4j");
        Map<String, Object> dataModel = new HashMap<>();
        // 项目名称
        dataModel.put("applicationName", "windy-crypto");
        // 产品输出目录
        dataModel.put("distributionSourceDir", StrUtil.replace(FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "target/exe-jar2exe")), "\\", "/"));
        // jar 包位置
        dataModel.put("classPathArchiveLocation", StrUtil.replace(FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "target/windy-crypto.jar")), "\\", "/"));
        // 图标文件
        dataModel.put("iconFile", StrUtil.replace(FileUtil.getAbsolutePath(FileUtil.file(getCurrentDir(), "attachments/freemarker/exe/0001.ico")), "\\", "/"));
        // 渲染
        FreeMarkerUtils.renderFile(FreeMarkerUtils.FmConfig.builder()
                .directoryForTemplateLoading(directoryForTemplateLoading)
                .dataModel(dataModel)
                .templateName(templateName)
                .outputFile(outputFile)
                .build());
    }

    private static void exe4jHandleJar2Setup() {
        File directoryForTemplateLoading = FileUtil.file(getCurrentDir(), "attachments/freemarker/exe");
        String templateName = "exe2setup.iss.ftl";
        File outputFile = FileUtil.file(getCurrentDir(), "target/windy-crypto-exe2setup.iss");
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("fmMyAppName", "windy-crypto");

    }

    private static String getCurrentDir() {
        return SystemUtil.getUserInfo().getCurrentDir();
    }
}
