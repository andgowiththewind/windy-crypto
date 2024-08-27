package com.gust.cafe.windycrypto.dto;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 已加密文件的文件名抽象为一个对象
 *
 * @author Dororo
 * @date 2024-08-27 20:19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverNameDTO {
    private String coverName;// 已加密文件名
    private String unCoverName;// 未加密文件名
    private String userPassword;// 用户密码
    private Boolean passwordCorrect;// 密码是否正确
    private List<Integer> intSaltList;// 解密后整数盐值列表
    private String intSaltListStr;// 已加密整数盐值列表字符串


    /**
     * 解析加密文件名
     * <p>
     * 已加密文件名(分隔符`$`):`固定识别前缀`+`密码摘要算法密文`+`整数盐对称加密密文`+`四位比特位`+`原文件名(如果存在)`+`原扩展名(如果存在)`
     * 说明：`固定识别前缀`标记这个文件是否曾经由当前系统进行加密,如果有这个前缀说明是; @see {@link CommonConstants#ENCRYPTED_PREFIX}
     * 说明：`密码摘要算法密文`是用户密码的摘要算,用于校验用户密码是否正确
     * 说明：`整数盐对称加密密文`是IO流加解密时使用的整数盐值数组转字符串后的密文
     * 说明：`四位比特位`是预留的四位比特位,用于标记文件的特殊属性,比如`1000`,此时一号位的1表示此文件的文件名是否在同目录下的同ID文档中记录
     * 说明：`原文件名`是原文件的文件名,如果存在的话;不存在原文件名的场景:比如`.gitkeep`文件
     * 说明：`原扩展名`是原文件的扩展名,如果存在的话;不存在原扩展名的场景:比如`config`文件
     *
     * </p>
     *
     * @param coverName         已加密文件名
     * @param inputUserPassword 用户输入的密码
     * @return 解析结果
     */
    public static CoverNameDTO analyse(String coverName, String inputUserPassword) {
        Assert.notBlank(coverName, "coverName must not be blank");
        Assert.notBlank(inputUserPassword, "inputUserPassword must not be blank");
        // 拆分
        List<String> parts = StrUtil.split(coverName, CommonConstants.ENCRYPTED_SEPARATOR);


        return null;
    }

}
