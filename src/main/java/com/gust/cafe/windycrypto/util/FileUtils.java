package com.gust.cafe.windycrypto.util;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;

/**
 * 文件工具类
 *
 * @author Dororo
 * @date 2024-01-06 22:38 基于Hutool二开,结合ChatGPT
 */
public class FileUtils {
    public static boolean openDirectory(String directoryPath) {
        if (StrUtil.isBlank(directoryPath)) {
            return false;
        }
        try {
            OS osType = getOsType();
            if (osType == OS.WINDOWS) {
                Runtime.getRuntime().exec("explorer.exe " + directoryPath);
                return true;
            } else if (osType == OS.UNIX) {
                Runtime.getRuntime().exec("xdg-open " + directoryPath);
                return true;
            } else {
                Console.error("不支持的操作系统");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean openFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return false;
        }
        try {
            OS osType = getOsType();
            if (osType == OS.WINDOWS) {
                // WINDOWS系统下如果路径包含空格,则需要用双引号包裹
                if (filePath.contains(" ")) {
                    filePath = StrUtil.format("\"{}\"", filePath);
                }
                Runtime.getRuntime().exec("cmd /c start " + filePath);
                return true;
            } else if (osType == OS.UNIX) {
                Runtime.getRuntime().exec("xdg-open " + filePath);
                return true;
            } else {
                Console.error("不支持的操作系统");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;
    }

    public static boolean openUrl(String url) {
        return openFile(url);
    }

    private static OS getOsType() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OS.WINDOWS;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            return OS.UNIX;
        } else {
            return OS.OTHER;
        }
    }

    public enum OS {
        WINDOWS, UNIX, OTHER
    }
}
