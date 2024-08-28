package com.gust.cafe.windycrypto.dto.core;

import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件抽象类
 * <p>一个Windy代表一个文件抽象</p>
 * <p>one Windy represents one file abstract, which is the basic unit of the file system</p>
 *
 * @author Dororo
 * @date 2024-08-23 14:23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Windy implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;// ID
    private String absPath;// 绝对路径
    private String mainName;// 文件名
    private String extName;// 扩展名
    private String name;// 文件名
    /**
     * @see {@link  WindyStatusEnum}
     */
    private Integer code;// 状态码
    private String label;// 状态码标签
    private String desc;//  状态码描述
    //
    private String latestMsg;// 最近消息
    private Integer percentage;// 文件加解密进度百分百
    private String percentageLabel;// 文件加解密进度百分百标签
    private Long size;// 文件大小
    private String sizeLabel;// 可读性文件大小
    private Boolean hadEncrypted;// 是否已加密
    private String createTime;// 创建时间
    private String updateTime;// 更新时间
}
