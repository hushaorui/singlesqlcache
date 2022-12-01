package com.hushaorui.ssc.main;

import com.hushaorui.ssc.common.TwinsValue;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
     * @return 数据集合
     */
    default List<DATA> selectByCondition() {
        return selectByCondition(Collections.emptyList());
    }
    /**
     * 根据条件查询数据
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    default List<DATA> selectByCondition(TwinsValue<String, Object>... conditions) {
        List<DATA> result;
        if (conditions == null || conditions.length == 0) {
            result = selectByCondition(Collections.emptyList());
        } else {
            result = selectByCondition(Arrays.asList(conditions));
        }
        return result;
    }

    /**
     * 根据条件查询数据
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    List<DATA> selectByCondition(List<TwinsValue<String, Object>> conditions);

    /**
     * 根据条件查询id
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return id集合
     */
    <T> List<T> selectIdByCondition(List<TwinsValue<String, Object>> conditions);

    /**
     * 根据条件删除数据
     * @param conditions 筛选条件
     */
    void deleteByCondition(List<TwinsValue<String, Object>> conditions);

    /**
     * 根据条件删除数据
     * @param conditions 筛选条件
     */
    default void deleteByCondition(TwinsValue<String, Object>... conditions) {
        deleteByCondition(Arrays.asList(conditions));
    }
    /**
     * 根据条件查询id
     * @return id集合
     */
    default <T> List<T> selectIdByCondition() {
        return selectIdByCondition(Collections.emptyList());
    }
    /**
     * 根据条件查询id
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return id集合
     */
    default <T> List<T> selectIdByCondition(TwinsValue<String, Object>... conditions) {
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
    int countByCondition(List<TwinsValue<String, Object>> conditions);

    /**
     * 根据条件查询数据的总数
     * @return 数据集合
     */
    default int countByCondition() {
        return countByCondition(Collections.emptyList());
    }
    /**
     * 根据条件查询数据的总数
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    default int countByCondition(TwinsValue<String, Object>... conditions) {
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
