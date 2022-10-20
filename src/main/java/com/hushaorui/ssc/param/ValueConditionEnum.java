package com.hushaorui.ssc.param;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ValueConditionEnum {
    ValueBetween(ValueBetween.class, " between", " between ? and ?"),
    ValueGreatOrEqual(ValueGreatOrEqual.class, " >=", " >= ?"),
    ValueGreatThan(ValueGreatThan.class, " >", " > ?"),
    ValueIn(ValueIn.class, " in", ""),
    ValueIsNotNull(ValueIsNotNull.class, " is not null", " is not null"),
    ValueIsNull(ValueIsNull.class, " is null", " is null"),
    ValueLessOrEqual(ValueLessOrEqual.class, " <=", " <= ?"),
    ValueLessThan(ValueLessThan.class, " <", " < ?"),
    ValueLike(ValueLike.class, " like", " like ?"),
    Equal(Object.class, "", " = ?"),
    ;

    ValueConditionEnum(Class<?> clazz, String keyString, String sqlString) {
        this.clazz = clazz;
        this.keyString = keyString;
        this.sqlString = sqlString;
    }

    private Class<?> clazz;
    private String keyString;
    private String sqlString;
    private static final Map<Class<?>, ValueConditionEnum> mapping;
    static {
        ValueConditionEnum[] values = ValueConditionEnum.values();
        Map<Class<?>, ValueConditionEnum> map = new HashMap<>(values.length, 1.5f);
        for (ValueConditionEnum valueEnum : values) {
            map.put(valueEnum.clazz, valueEnum);
        }
        mapping = Collections.unmodifiableMap(map);
    }

    public static String getKeyString(Object object) {
        if (object == null) {
            return Equal.keyString;
        }
        return mapping.getOrDefault(object.getClass(), Equal).keyString;
    }

    public static String getSqlString(Object object) {
        if (object == null) {
            return Equal.sqlString;
        }
        return mapping.getOrDefault(object.getClass(), Equal).sqlString;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getKeyString() {
        return keyString;
    }

    public String getSqlString() {
        return sqlString;
    }
}
