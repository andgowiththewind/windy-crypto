package com.gust.cafe.windycrypto.enums;

import cn.hutool.core.lang.Assert;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 加解密生命周期状态枚举
 *
 * @author Dororo
 * @date 2024-08-26 09:58:43
 */
@Getter
@AllArgsConstructor
public enum WindyStatusEnum {
    NOT_EXIST(-1, "不存在", "文件不存在或已删除"),
    FREE(0, "空闲", "文件存在且未被分配任务"),
    ;
    private Integer code;
    private String label;
    private String remark;

    public static List<Integer> codeList() {
        return Arrays.stream(WindyStatusEnum.values()).map(WindyStatusEnum::getCode).collect(Collectors.toList());
    }

    public static WindyStatusEnum getByCode(Integer code) {
        if (code == null) return null;
        for (WindyStatusEnum anEnum : WindyStatusEnum.values()) {
            if (anEnum.getCode().intValue() == code.intValue()) {
                return anEnum;
            }
        }
        return null;
    }
}
