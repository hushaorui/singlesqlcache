package com.hushaorui.ssc.config;

public class SscField {
    /** 表字段名，默认：根据配置中的风格结合类属性名生成 */
    public String columnName;
    /** 字段的表类型，默认：根据java类型判断 */
    public String columnType;
    /** 是否不为空，默认：false */
    public boolean notNull;
    /** 默认值，默认：null */
    public String defaultValue;
    /** 是否不更新，默认：false */
    public boolean notUpdate;
    /** 是否忽略该字段，默认：false */
    public boolean ignore;
}
