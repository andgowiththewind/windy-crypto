package com.gust.cafe.windycrypto.controller;

import com.gust.cafe.windycrypto.dto.core.R;
import com.gust.cafe.windycrypto.service.InsightTableService;
import com.gust.cafe.windycrypto.vo.req.InsightTableReqVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件表格控制器
 *
 * @author Dororo
 * @date 2024-08-23 14:16
 */
@RestController
@RequestMapping("/insightTable")
public class InsightTableController {
    private final InsightTableService insightTableService;

    public InsightTableController(InsightTableService insightTableService) {
        this.insightTableService = insightTableService;
    }

    @PostMapping("/getData")
    public R getInsightTableData(@RequestBody @Validated InsightTableReqVo reqVo) {
        return R.data(insightTableService.getInsightTableData(reqVo));
    }
}
