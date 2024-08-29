package com.gust.cafe.windycrypto.vo.req;

import com.gust.cafe.windycrypto.components.WindyLang;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoSubmitReqVo {
    private List<String> windyPathList;
    private List<String> dirPathList;
    private Boolean askEncrypt;
    private String userPassword;
    private Boolean isRequireCoverName;
}
