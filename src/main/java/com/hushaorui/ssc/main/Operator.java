package com.hushaorui.ssc.main;

import javafx.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 数据操作器
 * @param <DATA> 数据的类型
 */
public interface Operator<DATA> {

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
     * 根据id删除数据
     */
    void delete(Serializable id);

    /**
     * 根据id查询
     */
    DATA selectById(Serializable id);

    /**
     * 根据id查询，如果没有查询到，则将 insertFunction 的 返回结果数据插入并返回
     * @param id 数据id
     * @param insertFunction 数据缺省时调用并将结果插入并返回
     * @return 数据对象
     */
    DATA selectById(Serializable id, Function<Serializable, DATA> insertFunction);

    /**
     * 根据唯一键查询数据
     * @see com.hushaorui.ssc.common.anno.FieldDesc
     * @param uniqueName 唯一键名称(自定义)
     * @param data 封装了条件的数据对象
     * @return 数据对象
     */
    DATA selectByUniqueName(String uniqueName, DATA data);

    /**
     * 根据条件查询数据
     * @param conditions 条件集合 key: 类属性名或表字段名 ,value: 值
     * @return 数据集合
     */
    List<DATA> selectByCondition(Pair<String, Object>... conditions);

    /**
     * 获取数据库操作类
     * @return 数据库操作类
     */
    JdbcTemplate getJdbcTemplate();
}