package com.gust.cafe.windycrypto.controller;

import com.gust.cafe.windycrypto.domain.StatDailyTask;
import com.gust.cafe.windycrypto.dto.core.R;
import com.gust.cafe.windycrypto.mapper.StatDailyTaskMapper;
import com.gust.cafe.windycrypto.service.StatDailyTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Dororo
 * @date 2024-08-31 23:02
 */
@RestController
@RequestMapping("/statDailyTask")
public class StatDailyTaskController {
    private final StatDailyTaskMapper statDailyTaskMapper;

    public StatDailyTaskController(StatDailyTaskMapper statDailyTaskMapper) {
        this.statDailyTaskMapper = statDailyTaskMapper;
    }


    @GetMapping("/test")
    public R test() {
        StatDailyTask insertVo = StatDailyTask.builder()
                .ioDay("2024-08-31")
                .ioSize("100")
                .ioRatePerSecond("100")
                .ioSuccess("1")
                .build();

        statDailyTaskMapper.insert(insertVo);
        return R.ok();
    }
}
