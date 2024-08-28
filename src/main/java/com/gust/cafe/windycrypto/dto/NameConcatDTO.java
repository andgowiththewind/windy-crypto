package com.gust.cafe.windycrypto.dto;

import cn.hutool.core.util.StrUtil;

/**
 * 解决文件名拼接问题
 *
 * @author Dororo
 * @date 2024-08-28 11:08
 */

public class NameConcatDTO {
    private String mainName;
    private String extName;
    private String concatName;// 计算后

    public NameConcatDTO(String mainName, String extName) {
        if (StrUtil.isNotBlank(mainName) && StrUtil.isNotBlank(extName)) {
            throw new IllegalArgumentException("mainName and extName cannot be both not blank");
        }
        this.mainName = mainName;
        this.extName = extName;
        if (StrUtil.isNotBlank(mainName) && StrUtil.isNotBlank(extName)) {
            // 如果都不为空,则拼接,eg:test.txt
            this.concatName = mainName + "." + extName;
        } else if (StrUtil.isNotBlank(mainName) && StrUtil.isBlank(extName)) {
            // 如果扩展名为空,则不拼接,eg:config
            this.concatName = mainName;
        } else if (StrUtil.isBlank(mainName) && StrUtil.isNotBlank(extName)) {
            // 如果文件名为空,eg: .gitkeep
            this.concatName = "." + extName;
        }
    }

    public String getConcatName() {
        return concatName;
    }
}
