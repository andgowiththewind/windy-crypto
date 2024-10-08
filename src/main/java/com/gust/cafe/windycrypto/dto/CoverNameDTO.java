package com.gust.cafe.windycrypto.dto;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.system.SystemUtil;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.util.AesUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private String sourceName;// 原文件名
    private String sourceExtName;// 原扩展名
    private String sourceMainName;// 原文件名(不带扩展名)
    //
    private String coverName;// 已加密文件的完整文件名,记录
    private String userPassword;// 用户密码
    private Boolean passwordCorrect;// 密码是否正确
    private List<Integer> intSaltList;// 解密后整数盐值列表
    private String intSaltListStr;// 已加密整数盐值列表字符串
    //
    private List<Integer> bitSwitchList;// 比特位列表

    /**
     * 解析加密文件名
     * <p>
     * 一个demo(单引号无关):`$safeLockedV2$bb7f5fe493c0fe6a1c54bafc181ccb820351c1a051ac6cff78c8a22a0fd9c708$0a7450f53a28da82d8c7497278d953cd$0000$1829102994264821760$password.txt`
     * 已加密文件名(分隔符`$`):`固定识别前缀`+`密码摘要算法密文`+`整数盐对称加密密文`+`四位比特位`+`确保不重名的随机码`+`原文件名(如果存在)`+`原扩展名(如果存在)`
     * 说明：`固定识别前缀`标记这个文件是否曾经由当前系统进行加密,如果有这个前缀说明是; @see {@link CommonConstants#ENCRYPTED_PREFIX}
     * 说明：`密码摘要算法密文`是用户密码的摘要算,用于校验用户密码是否正确
     * 说明：`整数盐对称加密密文`是IO流加解密时使用的整数盐值数组转字符串后的密文
     * 说明：`四位比特位`是预留的四位比特位,用于标记文件的特殊属性,比如`1000`,此时一号位的1表示此文件的文件名是否在同目录下的同ID文档中记录
     * 说明：`确保不重名的随机码`是为了确保不重名,目前使用雪花ID生成
     * 说明：`原文件名`是原文件的文件名,如果存在的话;不存在原文件名的场景:比如`.gitkeep`文件
     * 说明：`原扩展名`是原文件的扩展名,如果存在的话;不存在原扩展名的场景:比如`config`文件
     *
     * </p>
     *
     * @param coverName         已加密文件名(FileUtil.getName)
     * @param inputUserPassword 用户输入的密码
     * @return 解析结果
     */
    public static CoverNameDTO analyse(String coverName, String inputUserPassword) {
        Assert.notBlank(coverName, "coverName must not be blank");
        Assert.notBlank(inputUserPassword, "inputUserPassword must not be blank");
        //
        CoverNameDTO coverNameDTO = new CoverNameDTO();
        coverNameDTO.setCoverName(coverName);
        coverNameDTO.setPasswordCorrect(false);// 默认密码错误
        // 拆分
        // [, safeLockedV2, bb7f5fe493c0fe6a1c54bafc181ccb820351c1a051ac6cff78c8a22a0fd9c708, 0a7450f53a28da82d8c7497278d953cd, 0000, 1829102994264821760, password.txt]
        // 第[0]个元素: []
        // 第[1]个元素: [safeLockedV2]
        // 第[2]个元素: [bb7f5fe493c0fe6a1c54bafc181ccb820351c1a051ac6cff78c8a22a0fd9c708]
        // 第[3]个元素: [0a7450f53a28da82d8c7497278d953cd]
        // 第[4]个元素: [0000]
        // 第[5]个元素: [1829102994264821760]
        // 第[6]个元素: [password.txt]
        List<String> parts = StrUtil.split(coverName, CommonConstants.ENCRYPTED_SEPARATOR);
        WindyException.run((Void) -> Assert.isTrue(parts.size() >= 7, "文件名格式不正确,无法解密 (require:parts>=7)"));
        // 摘要算法部分
        String passwordDigest = parts.get(2);
        WindyException.run((Void) -> Assert.notBlank(passwordDigest, "文件名格式不正确,无法解密 (密码摘要算法密文不能为空)"));

        // 将用户本次输入的密码进行同样的摘要算法,与文件名中的摘要算法密文进行比对
        String inputPasswordDigest = DigestUtil.sha256Hex(inputUserPassword);
        boolean passwordCorrect = StrUtil.equals(inputPasswordDigest, passwordDigest);
        WindyException.run((Void) -> Assert.isTrue(passwordCorrect, "用户输入的密码摘要算法值与文件名记录的不一致,不能使用当前密码解密"));
        // TODO 密码正确
        coverNameDTO.setPasswordCorrect(true);
        coverNameDTO.setUserPassword(inputUserPassword);
        //
        // 密码正确则将整数盐数组解密
        String encryptedSaltStr = parts.get(3);
        // Assert.notBlank(encryptedSaltStr, "encryptedSaltStr must not be blank");
        WindyException.run((Void) -> Assert.notBlank(encryptedSaltStr, "文件名格式不正确,无法解密 (整数盐对称加密密文为空)"));
        // 解密
        AesUtils.AesWrapper aesWrapper = AesUtils.getAes(inputUserPassword);
        String saltStr = null;// 举例:1,2,3
        try {
            saltStr = aesWrapper.decryptStr(encryptedSaltStr);
        } catch (Exception e) {
            // ignore
        }
        String finalSaltStr = saltStr;
        WindyException.run((Void) -> Assert.notBlank(finalSaltStr, "用户输入的密码无法解密出盐值(文件名可能被篡改)"));
        //
        List<String> intStrList = StrUtil.split(saltStr, StrUtil.COMMA);
        // 每个都是整数
        for (String intStr : intStrList) {
            WindyException.run((Void) -> Assert.isTrue(NumberUtil.isInteger(intStr), "解密后整数盐值列表字符串中存在非整数"));
        }
        List<Integer> intSaltList = intStrList.stream().map(Integer::valueOf).collect(Collectors.toList());
        coverNameDTO.setIntSaltList(intSaltList);
        coverNameDTO.setIntSaltListStr(saltStr);
        //
        // 考虑到原文件名也可能存在分隔符$,因此不能单纯地取index=6的内容,应该取第六个$符号之后的内容
        String sourceName = StrUtil.subSuf(coverName, StrUtil.ordinalIndexOf(coverName, CommonConstants.ENCRYPTED_SEPARATOR, 6) + 1);
        coverNameDTO.setSourceName(sourceName);

        // 取巧:通过文件系统获取扩展名和主文件名
        File ghostFile = FileUtil.file(SystemUtil.getUserInfo().getCurrentDir(), sourceName);
        coverNameDTO.setSourceMainName(FileUtil.mainName(ghostFile));
        coverNameDTO.setSourceExtName(FileUtil.extName(ghostFile));
        //
        // 收集bitSwitchList
        String bitSwitchStr = parts.get(4);
        WindyException.run((Void) -> Assert.notBlank(bitSwitchStr, "文件名格式不正确,无法解密 (四位比特位为空)"));
        // 四个位子
        bitSwitchStr.chars().forEach(bit -> {
            WindyException.run((Void) -> Assert.isTrue(Validator.isNumber(String.valueOf((char) bit)), "四位比特位中存在非数字"));
        });
        // 字符串转char数组
        List<Integer> bitSwitchList = new ArrayList<>();
        char[] bitSwitchChars = bitSwitchStr.toCharArray();
        for (char bitSwitchChar : bitSwitchChars) {
            int bitSwitch = Integer.parseInt(String.valueOf(bitSwitchChar));
            bitSwitchList.add(bitSwitch);
        }
        coverNameDTO.setBitSwitchList(bitSwitchList);
        //
        return coverNameDTO;
    }

}
