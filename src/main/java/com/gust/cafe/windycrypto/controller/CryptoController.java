package com.gust.cafe.windycrypto.controller;

import com.gust.cafe.windycrypto.components.WindyLang;
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
import java.util.concurrent.CompletableFuture;

/**
 * 加解密控制器
 *
 * @author Dororo
 * @date 2024-08-26 15:12
 */
@RestController
@RequestMapping("/crypto")
public class CryptoController {
    private final CryptoPreparationService cryptoPreparationService;
    private final CryptoService cryptoService;

    public CryptoController(CryptoPreparationService cryptoPreparationService, CryptoService cryptoService) {
        this.cryptoPreparationService = cryptoPreparationService;
        this.cryptoService = cryptoService;
    }

    @PostMapping("/submit")
    public R submit(@RequestBody @Validated CryptoSubmitReqVo reqVo) {
        List<String> prepare = cryptoPreparationService.prepare(reqVo);
        return R.ok(WindyLang.msg("i18n_1827976801159352320"));
    }
}
