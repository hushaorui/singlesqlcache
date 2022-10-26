package com.hushaorui.ssc.config;

import javafx.util.Pair;

/**
 * 分表策略
 * @param <TABLE_SPLIT_FIELD_TYPE> id属性的类型
 */
public interface TableSplitPolicy<TABLE_SPLIT_FIELD_TYPE extends Comparable<TABLE_SPLIT_FIELD_TYPE>> {
    /**
     * 根据id获取对应
     * @param tableSplitField id或其他分表字段
     * @param maxTableCount 最大分表数量
     * @return id对应的表索引
     */
    int getTableIndex(TABLE_SPLIT_FIELD_TYPE tableSplitField, int maxTableCount);

    /**
     * 作用范围
     * @return key: 最小值，value：最大值
     */
    Pair<TABLE_SPLIT_FIELD_TYPE, TABLE_SPLIT_FIELD_TYPE> getRange();
}
