package com.gust.cafe.windycrypto.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.InsightTableReqVo;
import com.gust.cafe.windycrypto.vo.res.InsightTableResVo;
import org.springframework.stereotype.Service;

@Service
public class InsightTableService {
    public InsightTableResVo getInsightTableData(InsightTableReqVo reqVo) {
        // 路径
        String path = reqVo.getParams().getPath();
        // 范围
        InsightTableReqVo.ScopeEnum scopeEnum = InsightTableReqVo.ScopeEnum.getByLabelEqualsIgnoreCase(reqVo.getParams().getSearchScope());
        // 路径校验
        WindyException.run((Void) -> Assert.isTrue(FileUtil.exist(path) && FileUtil.isDirectory(path), WindyLang.msg("i18n_1826891933163851776")));
        // 范围校验
        WindyException.run((Void) -> Assert.notNull(scopeEnum, WindyLang.msg("i18n_1826891933163851777")));


        return null;
    }
}
