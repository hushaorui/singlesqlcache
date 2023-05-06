package com.hushaorui.ssc.main;

import java.util.List;
import java.util.function.Function;

/**
 * 数据操作器(当存在分表字段时使用，而不是使用 Operator)
 * @param <DATA> 数据的类型
 */
public interface SpecialOperator<DATA> extends BaseOperator<DATA> {

    /**
     * 根据id查询
     */
    DATA selectById(Comparable id, Comparable tableSplitFieldValue);


    /**
     * 根据id查询部分字段
     */
    DATA findById(String selectStr, Comparable id, Comparable tableSplitFieldValue);

    /**
     * 根据id查询，如果没有查询到，则将 insertFunction 的 返回结果数据插入并返回
     * @param id 数据id
     * @param insertFunction 数据缺省时调用并将结果插入并返回
     * @return 数据对象
     */
    DATA selectByIdNotNull(Comparable id, Comparable tableSplitFieldValue, Function<Comparable, DATA> insertFunction);

    /**
     * 根据唯一键查询数据
     * @see com.hushaorui.ssc.common.anno.FieldDesc
     * @param uniqueName 唯一键名称(自定义)
     * @param data 封装了条件的数据对象
     * @return 数据对象
     */
    DATA selectByUniqueName(String uniqueName, Comparable tableSplitFieldValue, DATA data);

    /**
     * 根据唯一键查询数据
     * @see com.hushaorui.ssc.common.anno.FieldDesc
     * @param uniqueName 唯一键名称(自定义)
     * @param data 封装了条件的数据对象
     * @return 数据对象
     */
    DATA findByUniqueName(String selectStr, String uniqueName, Comparable tableSplitFieldValue, DATA data);


    /**
     * 根据分表字段查询数据
     * @param tableSplitFieldValue 分表字段值
     * @return 数据集合
     */
    List<DATA> selectByTableSplitField(Comparable tableSplitFieldValue);

    /**
     * 根据条件查询id
     * @param tableSplitFieldValue 分表字段值
     * @return id集合
     */
    <T> List<T> selectAllId(Comparable tableSplitFieldValue);

    /**
     * 根据分组字段查询
     * @param tableSplitFieldValue 分表字段值
     * @param fieldName 分组字段名
     * @param fieldValue 字段值
     * @return 数据集合
     */
    List<DATA> selectByGroupField(Comparable tableSplitFieldValue, String fieldName, Comparable fieldValue);

    /**
     * 根据分组字段查询
     * @param tableSplitFieldValue 分表字段值
     * @param fieldName 分组字段名
     * @param fieldValue 字段值
     * @return 数据集合
     */
    List<DATA> findByGroupField(String selectStr, Comparable tableSplitFieldValue, String fieldName, Comparable fieldValue);
}