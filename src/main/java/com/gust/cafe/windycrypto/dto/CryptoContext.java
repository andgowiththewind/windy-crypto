package com.gust.cafe.windycrypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.List;

/**
 * 加解密上下文
 *
 * @author Dororo
 * @date 2024-08-26 18:40
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoContext {
    private Boolean askEncrypt; // 本次操作是否为加密
    private String userPassword;// 用户密码
    private String userPasswordSha256Hex;// 用户密码摘要算法密文,计算一次即可
    //
    private String beforePath;// 原文件路径
    private String beforeCacheId;// 原文件缓存ID
    //
    private String tmpPath;// 临时文件路径
    // 临时文件不记录缓存
    //
    private String afterPath;// 目标文件路
    private String afterCacheId;// 目标文件缓存ID
    //
    private BufferedInputStream bis;// 输入流,被加解密的文件读取流
    private BufferedOutputStream bos;// 输出流,临时文件写入流
    private List<Integer> intSaltList;// 整数盐值列表
    private String intSaltStr;// 字符串盐值
    private String intSaltStrEncryptHex;// 字符串盐值加密后的十六进制字符串
    private List<Integer> bitSwitchList;// 比特位列表,已设计:[index=0代表是否加密了原文件名,index=1预留未分配,index=2预留未分配,index=3预留未分配]
    //
    private Boolean ignoreMissingHiddenFilename;// 当加密文件源文件名为加密形式且缺失配置文件时,是否忽略
    //
    private Long ioConsumingMs;// IO耗时毫秒数
}
