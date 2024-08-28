package com.gust.cafe.windycrypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * `XXX.windycfg`文件内容
 * <p>文件名已经标记了归属,文件正文内容记录关键信息即可,考虑后续扩展,封装为DTO</p>
 *
 * @author Dororo
 * @date 2024-08-28 09:20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CfgTxtContentDTO {
    private String sourceName;// 原文件名(含可能存在的扩展名)
    // 其他信息待扩展
    //
    private String createTime;// 创建时间
    private String updateTime;// 更新时间
}
