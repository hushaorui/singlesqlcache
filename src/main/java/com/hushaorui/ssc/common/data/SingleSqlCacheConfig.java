package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.common.em.ColumnNameStyle;
import com.hushaorui.ssc.common.em.TableNameStyle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 中心配置
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SingleSqlCacheConfig {
    /**缓存默认保留时间(不使用后开始计时) 单位：毫秒，默认5分钟 */
    private long maxInactiveTime = 300000L;
    /** 表名风格 (没有指定表名时使用) */
    private TableNameStyle tableNameStyle = TableNameStyle.LOWERCASE_UNDERLINE;
    /** 字段名风格 (没有指定字段名时使用) */
    private ColumnNameStyle columnNameStyle = ColumnNameStyle.LOWERCASE_UNDERLINE;
}
