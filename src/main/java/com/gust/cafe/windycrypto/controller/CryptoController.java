package com.gust.cafe.windycrypto.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.controller.core.BaseController;
import com.gust.cafe.windycrypto.dto.core.R;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.service.CryptoPreparationService;
import com.gust.cafe.windycrypto.service.CryptoService;
import com.gust.cafe.windycrypto.vo.req.CryptoSubmitReqVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 加解密控制器
 *
 * @author Dororo
 * @date 2024-08-26 15:12
 */
@RestController
@RequestMapping("/crypto")
public class CryptoController extends BaseController {
    private final CryptoPreparationService cryptoPreparationService;
    private final CryptoService cryptoService;

    public CryptoController(CryptoPreparationService cryptoPreparationService, CryptoService cryptoService) {
        this.cryptoPreparationService = cryptoPreparationService;
        this.cryptoService = cryptoService;
    }

    @PostMapping("/submit")
    public R submit(@RequestBody @Validated CryptoSubmitReqVo reqVo) {
        // 计算任务量
        List<String> absPathList = cryptoPreparationService.prepare(reqVo);
        if (CollectionUtil.isEmpty(absPathList)) throw new WindyException(WindyLang.msg("i18n_1828011035181846528"));// 无符合条件任务
        //
        // 异步处理长耗时任务
        super.runAsyncExceptionally((v) -> cryptoService.actionAsync(absPathList, reqVo));
        //
        // 反馈
        return R.ok(WindyLang.msg("i18n_1827976801159352320") + "\tcount=" + absPathList.size());// 任务已提交
    }
}
