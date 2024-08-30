package com.gust.cafe.windycrypto.controller;

import com.gust.cafe.windycrypto.dto.core.R;
import com.gust.cafe.windycrypto.service.SysService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys/")
public class SysController {
    private final SysService sysService;

    public SysController(SysService sysService) {
        this.sysService = sysService;
    }

    @GetMapping("/getWsUrlPrefix")
    public R getWsUrlPrefix() {
        return R.data(sysService.getWsUrlPrefix());
    }
}
