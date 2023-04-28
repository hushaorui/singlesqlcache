package com.hushaorui.ssc.main;

import java.util.List;
import java.util.function.Function;

/**
 * 数据操作器
 * @param <DATA> 数据的类型
 */
public interface Operator<DATA> extends BaseOperator<DATA> {

    /**
     * 根据id删除数据
     */
    void delete(Comparable id);

    /**
     * 根据id查询
     */
    DATA selectById(Comparable id);

    /**
     * 根据id查询部分字段
     */
    DATA findById(String selectStr, Comparable id);

    /**
     * 根据id查询，如果没有查询到，则将 insertFunction 的 返回结果数据插入并返回
     * @param id 数据id
     * @param insertFunction 数据缺省时调用并将结果插入并返回
     * @return 数据对象
     */
    DATA selectByIdNotNull(Comparable id, Function<Comparable, DATA> insertFunction);

    /**
     * 根据唯一键查询数据
     * @see com.hushaorui.ssc.common.anno.FieldDesc
     * @param uniqueName 唯一键名称(自定义)
     * @param data 封装了条件的数据对象
     * @return 数据对象
     */
    DATA selectByUniqueName(String uniqueName, DATA data);

    /**
     * 根据唯一键查询数据（只适合非联合唯一的字段，也就是单个字段唯一）
     * @param uniqueName 唯一键名称(自定义)
     * @param fieldValue 唯一键字段值
     * @return 数据对象
     */
    DATA selectByUniqueName(String uniqueName, Comparable fieldValue);

    /**
     * 根据分组字段查询
     * @param fieldName 分组字段名
     * @param fieldValue 字段值
     * @return
     */
    List<DATA> selectByGroupField(String fieldName, Comparable fieldValue);
}