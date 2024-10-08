package com.gust.cafe.windycrypto.runner;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * 测试环境创建目录
 *
 * @author Dororo
 * @date 2024-08-23 10:40
 */
@Slf4j
@Component
public class DevMkdirRunner implements ApplicationRunner {
    private final Environment environment;

    public DevMkdirRunner(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String springProfilesActive = environment.getProperty("spring.profiles.active");
        log.debug("=====================================================================");
        log.info("当前环境:{}", springProfilesActive);
        log.debug("=====================================================================");

        if (StrUtil.equalsIgnoreCase(springProfilesActive, "dev")) {
            String currentDir = SystemUtil.getUserInfo().getCurrentDir();
            File file = FileUtil.file(currentDir, "target", "测试被加解密顶级目录");
            FileUtil.del(file);
            type01(file);
            // type02(file);
        }

    }

    private static void type01(File file) throws IOException {
        for (int i = 0; i < 1; i++) {
            int numI = i + 1;
            String nameI = StrUtil.fillAfter(Convert.toStr(numI), '0', 4);
            File levelOne = FileUtil.file(file, nameI);
            FileUtil.mkdir(levelOne);
            for (int j = 0; j < 10; j++) {
                int numJ = j + 1;
                String nameJ = StrUtil.fillAfter(Convert.toStr(numJ), '0', 4);
                String levelTwoName = StrUtil.format("{}-{}", nameI, nameJ);
                File levelTwo = FileUtil.file(levelOne, levelTwoName);
                FileUtil.mkdir(levelTwo);
                // 写入一个文件
                File txt = FileUtil.file(levelTwo, StrUtil.format("{}.txt", levelTwoName));
                txt.createNewFile();
                FileUtil.writeUtf8String(levelTwoName, txt);
            }
        }
    }

    private void type02(File file) {
        for (int i = 0; i < 10; i++) {
            int numI = i + 1;
            String mark = StrUtil.fillAfter(Convert.toStr(numI), '0', 3);
            File tmp = FileUtil.file(file, mark + ".txt");
            FileUtil.writeUtf8String(mark, tmp);
        }
    }
}
