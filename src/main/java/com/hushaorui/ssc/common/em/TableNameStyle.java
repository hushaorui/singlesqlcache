package com.hushaorui.ssc.common.em;

/**
 * 表名风格
 */
public enum TableNameStyle {
    /** 默认的，小写且下划线 */
    LOWERCASE_UNDERLINE,
    /** 大写和下划线 */
    UPPERCASE_UNDERLINE,
    /** 和简单类名一致 */
    SIMPLE_CLASS_NAME,
    /** 简单类名全小写 */
    LOWERCASE_SIMPLE_CLASS_NAME,
    /** 简单类名全大写 */
    UPPERCASE_SIMPLE_CLASS_NAME,
    /** 全类名 '.' 转下划线且小写 */
    LOWERCASE_FULL_CLASS_NAME_UNDERLINE,
    /** 全类名 '.' 转下划线且大写 */
    UPPERCASE_FULL_CLASS_NAME_UNDERLINE,
}
