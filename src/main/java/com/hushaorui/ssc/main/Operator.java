package com.hushaorui.ssc.main;

import java.io.Serializable;

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
}