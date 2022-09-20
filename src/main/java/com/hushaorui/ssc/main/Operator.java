package com.hushaorui.ssc.main;

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
}