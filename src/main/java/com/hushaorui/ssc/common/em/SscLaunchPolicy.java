package com.hushaorui.ssc.common.em;

/**
 * 启动策略
 */
public enum SscLaunchPolicy {
    /** 什么都不做 */
    NONE,
    /** 表不存在时创建表，默认 */
    CREATE_TABLE_IF_NOT_EXIST,
    ///** 表不存在时创建表，表字段发生变化时更新表字段 */
    //MODIFY_COLUMN_IF_UPDATE,
    /** 删除表格重新创建 */
    DROP_TABLE_AND_CRETE,
    /** 删除表 */
    DROP_TABLE,

}
