package com.gust.cafe.windycrypto.vo.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderTreeReqVo {
    @NotBlank(message = "路径不能为空")
    private String path;
}
