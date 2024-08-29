package com.gust.cafe.windycrypto.vo.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Boolean ignoreMissingHiddenFilename;
}
