package com.gust.cafe.windycrypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

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
    private String beforePath;// 原文件路径
    private String tmpPath;// 临时文件路径
    private String afterPath;// 目标文件路
    private BufferedInputStream bis;// 输入流,被加解密的文件读取流
    private BufferedOutputStream bos;// 输出流,临时文件写入流
}
