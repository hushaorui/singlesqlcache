package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 不分表的操作类
 */
public class UniqueTableOperator extends AbstractTableOperator {

    public UniqueTableOperator(JdbcTemplate jdbcTemplate, SingleSqlCacheConfig singleSqlCacheConfig) {
        super(jdbcTemplate, singleSqlCacheConfig);
    }

    public UniqueTableOperator(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }
}
