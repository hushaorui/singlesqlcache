package com.hushaorui.ssc.config;

import javafx.util.Pair;

/**
 * 分表策略
 * @param <ID_TYPE> id属性的类型
 */
public interface TableSplitPolicy<ID_TYPE extends Comparable<ID_TYPE>> {
    /**
     * 根据id获取对应
     * @param id id
     * @param maxTableCount 最大分表数量
     * @return id对应的表索引
     */
    int getTableIndex(ID_TYPE id, int maxTableCount);

    /**
     * 作用范围
     * @return key: 最小值，value：最大值
     */
    Pair<ID_TYPE, ID_TYPE> getRange();
}
