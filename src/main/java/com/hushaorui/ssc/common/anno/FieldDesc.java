package com.hushaorui.ssc.common.anno;

import java.lang.annotation.*;

/**
 * 标记不能为空的字段或
 * 标记整个类的字段都不能为空(DataId字段在一些情况下会忽略)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@Documented
public @interface FieldDesc {
    /** 字段名 */
    String value() default "";
    /** 数据库字段的完整名称，如 VARCHAR(64) */
    String columnType() default "";
    /** 是否是id */
    boolean isId() default false;
    /** 字段不保存进数据库 */
    boolean noneSave() default false;
    /** 字段不允许为null */
    boolean isNotNull() default true;
    /** 值在数据库中是否唯一(同一个类中 uniqueName相同的字段联合唯一) */
    String uniqueName() default "";
    /** 字段不更新 (只在插入数据时设值) */
    boolean isNotUpdate() default false;
    /** 根据这些字段查询所有 */
    String findAllGroupName() default "";
    /** 是否需要缓存，默认是需要的 */
    boolean cached() default true;
}
