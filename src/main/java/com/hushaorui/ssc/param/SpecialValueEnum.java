package com.hushaorui.ssc.param;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum SpecialValueEnum {
    ValueBetween(ValueBetween.class, " between", " between ? and ?"),
    ValueGreatOrEqual(ValueGreatOrEqual.class, " >=", " >= ?"),
    ValueGreatThan(ValueGreatThan.class, " >", " > ?"),
    ValueIn(ValueIn.class, " in", ""),
    ValueIsNotNull(ValueIsNotNull.class, " is not null", " is not null"),
    ValueIsNull(ValueIsNull.class, " is null", " is null"),
    ValueLessOrEqual(ValueLessOrEqual.class, " <=", " <= ?"),
    ValueLessThan(ValueLessThan.class, " <", " < ?"),
    ValueLike(ValueLike.class, " like", " like ?"),
    None(Object.class, "", " = ?"),
    ;

    SpecialValueEnum(Class<?> clazz, String keyString, String sqlString) {
        this.clazz = clazz;
        this.keyString = keyString;
        this.sqlString = sqlString;
    }

    private Class<?> clazz;
    private String keyString;
    private String sqlString;
    private static final Map<Class<?>, SpecialValueEnum> mapping;
    static {
        SpecialValueEnum[] values = SpecialValueEnum.values();
        Map<Class<?>, SpecialValueEnum> map = new HashMap<>(values.length, 1.5f);
        for (SpecialValueEnum valueEnum : values) {
            map.put(valueEnum.clazz, valueEnum);
        }
        mapping = Collections.unmodifiableMap(map);
    }

    public static String getKeyString(Object object) {
        if (object == null) {
            return None.keyString;
        }
        return mapping.getOrDefault(object.getClass(), None).keyString;
    }

    public static String getSqlString(Object object) {
        if (object == null) {
            return None.sqlString;
        }
        return mapping.getOrDefault(object.getClass(), None).sqlString;
    }
}
