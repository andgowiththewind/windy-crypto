package com.gust.cafe.windycrypto.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import com.esotericsoftware.minlog.Log;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秘钥工具类
 *
 * @author Dororo
 * @version 2.0 解决单例模式下的线程安全问题&效率问题
 * @date 2024-08-27 20:43:20
 * @see DigestUtil#sha256Hex(String)
 * <p>摘要算法请使用DigestUtil</p>
 */
@Slf4j
public class AesUtils {
    // 明文盐值:由于当前系统业务场景下,不要求用户同时输入盐值(只需要用户输入密码,避免记忆繁琐),所以这里在系统内部使用固定盐值
    private final static String explicitSaltString = "aaa1234567890bbb";
    // ConcurrentHashMap解决多线程下的线程安全问题
    private static final ConcurrentHashMap<String, AesWrapper> aesCache = new ConcurrentHashMap<>();

    public static AesWrapper getAes(String userPassword) {
        TimeInterval timer = DateUtil.timer();
        Assert.notBlank(userPassword, "userPassword must not be blank");
        // 不明文存`userPassword`,转摘要算法
        String userPasswordSha256Hex = DigestUtil.sha256Hex(userPassword);
        // 从缓存中获取,如果不存在则创建并放入缓存
        AesWrapper aesWrapper = aesCache.computeIfAbsent(userPasswordSha256Hex, k -> createAes(userPassword));
        if (timer.intervalMs() > 1000) log.debug(StrUtil.format("耗时[{}]ms为[{}](摘要值)创建AES", timer.intervalMs(), userPasswordSha256Hex));
        return aesWrapper;
    }

    private static AesWrapper createAes(String userPassword) {
        try {
            // 1.0 秘钥派生函数,将用户输入的任意长度的密码转换为固定长度的秘钥
            byte[] salt = explicitSaltString.getBytes();
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(userPassword.toCharArray(), salt, 65536, 256);// 65536是迭代次数,256是期望生成的密钥长度
            SecretKey secretKey = factory.generateSecret(spec);
            // 2.0 生成AES加密算法
            byte[] secretKeyBytes = secretKey.getEncoded();
            // 偏移向量同样使用显示盐值
            AES aes = new AES("CBC", "PKCS7Padding", secretKeyBytes, salt);
            AesWrapper aesWrapper = new AesWrapper(aes);
            return aesWrapper;
        } catch (Exception e) {
            throw new RuntimeException("Error generating AES key", e);
        }
    }

    /**
     * AES包装类
     * <p>不暴露AES,避免`aes.setXXXX`等方法被调用导致单例内部内容被修改;</p>
     */
    public static class AesWrapper {
        private AES aes;

        // 设置为私有构造方法,外部无法直接创建
        private AesWrapper(AES aes) {
            this.aes = aes;
        }

        public String encryptHex(String data) {
            return aes.encryptHex(data);
        }

        public String decryptStr(String data) {
            return aes.decryptStr(data);
        }

        // TODO 更多方法包装参考AES原始方法
    }
}
