package com.gust.cafe.windycrypto.mapper;

import com.gust.cafe.windycrypto.dao.TkCombinationMapper;
import com.gust.cafe.windycrypto.domain.StatDailyTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 请输入类描述
 *
 * @author Dororo
 * @date 2024-08-31 23:00
 */
public interface StatDailyTaskMapper extends TkCombinationMapper<StatDailyTask> {
    List<StatDailyTask> selectHeatMapList(@Param("minIoDay") String minIoDay, @Param("maxIoDay") String maxIoDay);
}