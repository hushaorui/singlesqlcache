package com.hushaorui.ssc.main;

import javafx.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 数据操作器
 * @param <DATA> 数据的类型
 */
public interface BaseOperator<DATA> {

    /**
     * 插入数据
     * @param data 数据对象
     */
    void insert(DATA data);

    /**
     * 根据id更新数据
     */
    void update(DATA data);

    /**
     * 根据id删除数据
     */
    void delete(DATA data);

    /**
     * 根据条件查询数据
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    default List<DATA> selectByCondition(Pair<String, Object>... conditions) {
        if (conditions == null || conditions.length == 0) {
            return selectByCondition(Collections.emptyList());
        }
        return selectByCondition(Arrays.asList(conditions));
    }

    /**
     * 根据条件查询数据
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    List<DATA> selectByCondition(List<Pair<String, Object>> conditions);

    /**
     * 根据条件查询id
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return id集合
     */
    <T> List<T> selectIdByCondition(List<Pair<String, Object>> conditions);

    /**
     * 根据条件查询id
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return id集合
     */
    default <T> List<T> selectIdByCondition(Pair<String, Object>... conditions) {
        if (conditions == null) {
            return selectIdByCondition(Collections.emptyList());
        }
        return selectIdByCondition(Arrays.asList(conditions));
    }

    /**
     * 根据条件查询数据的总数
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    int countByCondition(List<Pair<String, Object>> conditions);

    /**
     * 根据条件查询数据的总数
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    default int countByCondition(Pair<String, Object>... conditions) {
        if (conditions == null) {
            return countByCondition(Collections.emptyList());
        }
        return countByCondition(Arrays.asList(conditions));
    }

    /**
     * 获取数据库操作类
     * @return 数据库操作类
     */
    JdbcTemplate getJdbcTemplate();
}