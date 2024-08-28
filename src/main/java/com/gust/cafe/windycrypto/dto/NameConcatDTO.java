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

    /**
     * 通过mainName和扩展名构造,考虑`NULL-NULL`各种组合
     *
     * @param mainName
     * @param extName
     */
    public NameConcatDTO(String mainName, String extName) {
        if (StrUtil.isBlank(mainName) && StrUtil.isBlank(extName)) {
            throw new IllegalArgumentException("mainName and extName cannot be empty at the same time");
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
