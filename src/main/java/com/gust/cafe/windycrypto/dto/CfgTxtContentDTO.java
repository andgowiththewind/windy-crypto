package com.gust.cafe.windycrypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * `XXX.windycfg`文件内容
 *
 * @author Dororo
 * @date 2024-08-28 09:20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CfgTxtContentDTO {
    private String createTime;// 创建时间
    private String updateTime;// 更新时间

}
