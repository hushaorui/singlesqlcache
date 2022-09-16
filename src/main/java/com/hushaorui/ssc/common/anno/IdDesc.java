package com.hushaorui.ssc.common.anno;

import java.lang.annotation.*;

/**
 * id字段标记，如果该类没有字段被标记，则会使用名称为id的字段作为id，如果也没有，则报错
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface IdDesc {
    /** 表字段名称，默认会根据配置和类字段名称命名 */
    String value() default "";
}
