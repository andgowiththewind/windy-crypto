package com.gust.cafe.windycrypto.service;

import com.gust.cafe.windycrypto.domain.StatDailyTask;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import com.gust.cafe.windycrypto.mapper.StatDailyTaskMapper;

/**
 * 请输入类描述
 *
 * @author Dororo
 * @date 2024-08-31 23:00
 */
@Service
public class StatDailyTaskService {

    @Autowired
    private StatDailyTaskMapper statDailyTaskMapper;


    public void addTask(StatDailyTask insertVo) {
        statDailyTaskMapper.insert(insertVo);
    }
}
