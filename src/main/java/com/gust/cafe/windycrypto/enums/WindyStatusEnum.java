package com.gust.cafe.windycrypto.enums;

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
    WAITING(10, "排队中", "已分配任务,处于排队线程中但未进入工作线程处理"),
    OUTPUTTING(20, "输出中", "作为源文件,正在输出到其他文件"),
    INPUTTING(30, "输入中", "作为目标文件,正在接收其他文件的输出"),
    ALMOST(40, "即将完成", "字节流处理完毕但仍需处理改名等收尾操作"),
    // 40完成后会回归至0,即空闲状态
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
