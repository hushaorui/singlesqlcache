package com.hushaorui.ssc.common.anno;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataClass {
    /** 表名，默认会根据配置和类名进行取名 */
    String value() default "";
    /** 表的数量，大于1时将进行分表，需要指定分表字段 */
    int tableCount() default 1;
    /** 用于分表的字段名，未指定时默认使用id来分表，该字段默认不可更新，不可为null */
    String tableSplitField() default "";
    /** 是否启用缓存，默认启用 */
    boolean cached() default true;
    /** id是否使用自动生成策略，默认使用 */
    boolean isAuto() default true;
    /** 在使用自动生成策略且使用默认策略后生效，作为Long类型或String类型的id的起始值 */
    long idFirstLongValue() default 1L;
    /** 在使用自动生成策略且使用默认策略后生效，作为Integer类型的id的起始值 */
    int idFirstIntegerValue() default 1;
    /** 为true时默认使用全局配置中同名配置值 */
    boolean appendNumberAtFirstTable() default true;
}
