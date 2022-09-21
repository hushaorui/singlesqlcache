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
}
