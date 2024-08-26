package com.gust.cafe.windycrypto.service;

import cn.hutool.core.lang.Assert;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.CryptoSubmitReqVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CryptoPreparationService {
    public List<String> prepare(CryptoSubmitReqVo reqVo) {
        // 一些判断不需要放在加解密阶段
        WindyException.run((Void) -> Assert.notNull(reqVo.getAskEncrypt(), WindyLang.msg("i18n_1827983611962462208")));
        WindyException.run((Void) -> Assert.notBlank(reqVo.getUserPassword(), WindyLang.msg("i18n_1827983611962462209", "i18n_1827983611962462210")));
        return null;
    }
}
