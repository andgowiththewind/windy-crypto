package com.gust.cafe.windycrypto.domain;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请输入类描述
 *
 * @author Dororo
 * @date 2024-08-31 23:00
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stat_daily_task")
public class StatDailyTask {
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    @Column(name = "io_day")
    private String ioDay;

    @Column(name = "io_size")
    private String ioSize;

    @Column(name = "io_rate_per_second")
    private String ioRatePerSecond;

    @Column(name = "io_success")
    private String ioSuccess;
}