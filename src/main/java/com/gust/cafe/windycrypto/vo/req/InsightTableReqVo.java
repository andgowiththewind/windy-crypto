package com.gust.cafe.windycrypto.vo.req;

import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.dto.core.Windy;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightTableReqVo {
    @Valid
    private Windy model;
    @Valid
    private PageDTO page;
    @Valid
    private ParamsDTO params;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageDTO {
        private Integer pageNum;
        private Integer pageSize;
        private Integer total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParamsDTO {
        private String scope;
        @NotBlank(message = "路径不能为空")
        private String path;
    }

    @Getter
    @AllArgsConstructor
    public enum ScopeEnum {
        NOT_ENCRYPTED("notEncrypted", "未加密"),
        ENCRYPTED("encrypted", "已加密"),
        ALL("all", "全部");
        private final String label;
        private final String desc;

        public static List<String> labelList() {
            return Arrays.stream(ScopeEnum.values()).map(ScopeEnum::getLabel).collect(Collectors.toList());
        }

        // 忽略大小写地根据label获取枚举
        public static ScopeEnum getByLabelEqualsIgnoreCase(String label) {
            for (ScopeEnum anEnum : ScopeEnum.values()) {
                if (StrUtil.equalsIgnoreCase(anEnum.getLabel(), label)) {
                    return anEnum;
                }
            }
            return null;
        }
    }
}
