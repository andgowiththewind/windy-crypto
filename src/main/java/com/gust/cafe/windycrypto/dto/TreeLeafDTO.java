package com.gust.cafe.windycrypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 目录树叶子
 *
 * @author Dororo
 * @date 2024-08-23 11:04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeLeafDTO {
    // 节点名称
    private String label;
    // 子节点
    private List<TreeLeafDTO> children;
    // 扩展属性
    private Integer id;
    private Integer parentId;// 禁止使用`pId`
    private String absPath;
}
