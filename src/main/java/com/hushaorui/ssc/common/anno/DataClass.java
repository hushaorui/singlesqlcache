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
    /** 是否启用缓存，默认启用 */
    boolean cached() default true;
    /** id是否使用自动生成策略，默认使用 */
    boolean isAuto() default true;
}
