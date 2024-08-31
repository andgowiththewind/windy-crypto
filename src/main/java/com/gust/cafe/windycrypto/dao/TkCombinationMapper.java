package com.gust.cafe.windycrypto.dao;

/**
 * 合并tk.mapper多个接口
 *
 * @author Dororo
 * @date 2024-08-08 15:48
 */
public interface TkCombinationMapper<T> extends
        tk.mybatis.mapper.common.BaseMapper<T>,
        tk.mybatis.mapper.common.Mapper<T>,
        tk.mybatis.mapper.common.special.InsertListMapper<T>,
        tk.mybatis.mapper.common.ConditionMapper<T> {
}
