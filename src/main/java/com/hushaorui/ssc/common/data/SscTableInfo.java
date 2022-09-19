package com.hushaorui.ssc.common.data;

import java.util.Map;

/**
 * 表的信息
 */
public class SscTableInfo {
    /** 建表语句 */
    private String[] createTableSql;
    /** 删表语句 */
    private String[] dropTableSql;
    /** 查询所有的语句 */
    private String selectAllSql;
    /** 根据id查询指定数据 */
    private String selectByIdSql;
    /** 根据唯一约束的字段查询数据 */
    private String selectByUniqueKeySql;
    /** 根据特定字段组合查询数据 */
    private Map<String, String> selectByConditionSql;
    /** 所有不需要缓存的数据更新 */
    private String updateAllNotCachedSql;

}
