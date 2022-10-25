package com.hushaorui.ssc.common.data;

import com.hushaorui.ssc.common.em.SscConditionEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SscCondition {
    /** 条件类型 */
    private SscConditionEnum type = SscConditionEnum.AND;
    /** 字段的值集合 */
    private HashMap<String, Object> fieldMap;
    /** 与之相邻的条件对象 */
    private List<SscCondition> nextList;
    /** 相关联表对应的数据类 */
    private Class<?> joinClass;
    /** 整数数值(FIRST MAX) */
    private int number;

    public SscConditionEnum getType() {
        return type;
    }

    public HashMap<String, Object> getFieldMap() {
        return fieldMap;
    }

    public List<SscCondition> getNextList() {
        return nextList;
    }

    public Class<?> getJoinClass() {
        return joinClass;
    }

    public int getNumber() {
        return number;
    }

    public SscCondition() {
        fieldMap = new HashMap<>();
    }
    public SscCondition(HashMap<String, Object> fieldMap) {
        this.fieldMap = fieldMap;
    }

    private SscCondition(SscConditionEnum type) {
        this.type = type;
        fieldMap = new HashMap<>();
    }

    private SscCondition(SscConditionEnum type, Class<?> joinClass) {
        this.type = type;
        this.joinClass = joinClass;
    }

    private SscCondition(SscConditionEnum type, int number) {
        this.type = type;
        this.number = number;
    }

    private SscCondition(SscConditionEnum type, HashMap<String, Object> fieldMap) {
        this.type = type;
        this.fieldMap = fieldMap;
    }

    private SscCondition(SscConditionEnum type, HashMap<String, Object> fieldMap, Class<?> joinClass) {
        this.type = type;
        this.fieldMap = fieldMap;
        this.joinClass = joinClass;
    }

    private void addToList(SscCondition next) {
        if (nextList == null) {
            nextList = new ArrayList<>();
        }
        nextList.add(next);
    }

    public SscCondition and(String key, Object value) {
        SscCondition sscCondition = new SscCondition();
        sscCondition.fieldMap.put(key, value);
        addToList(sscCondition);
        return this;
    }
    public SscCondition or(String key, Object value) {
        SscCondition sscCondition = new SscCondition(SscConditionEnum.OR);
        sscCondition.fieldMap.put(key, value);
        addToList(sscCondition);
        return this;
    }
    public SscCondition leftJoin(Class<?> joinClass) {
        SscCondition next = new SscCondition(SscConditionEnum.LEFT_OUTER_JOIN, joinClass);
        addToList(next);
        return this;
    }
    public SscCondition rightJoin(Class<?> joinClass) {
        SscCondition next = new SscCondition(SscConditionEnum.RIGHT_OUTER_JOIN, joinClass);
        addToList(next);
        return this;
    }
    public SscCondition innerJoin(Class<?> joinClass) {
        SscCondition next = new SscCondition(SscConditionEnum.INNER_JOIN, joinClass);
        addToList(next);
        return this;
    }

    public SscCondition on(String key, Object value) {
        SscCondition next = new SscCondition(SscConditionEnum.ON);
        next.fieldMap.put(key, value);
        addToList(next);
        return this;
    }

    public SscCondition on(HashMap<String, Object> fieldMap) {
        SscCondition next = new SscCondition(SscConditionEnum.ON, fieldMap);
        addToList(next);
        return this;
    }

    public SscCondition first(int first) {
        SscCondition next = new SscCondition(SscConditionEnum.FIRST, first);
        addToList(next);
        return this;
    }

    public SscCondition max(int max) {
        SscCondition next = new SscCondition(SscConditionEnum.MAX, max);
        addToList(next);
        return this;
    }

    public SscCondition and(HashMap<String, Object> fieldMap) {
        addToList(new SscCondition(fieldMap));
        return this;
    }
    public SscCondition or(HashMap<String, Object> fieldMap) {
        addToList(new SscCondition(SscConditionEnum.OR, fieldMap));
        return this;
    }
    public SscCondition leftJoin(Class<?> joinClass, HashMap<String, Object> fieldMap) {
        addToList(new SscCondition(SscConditionEnum.LEFT_OUTER_JOIN, fieldMap, joinClass));
        return this;
    }
    public SscCondition rightJoin(Class<?> joinClass, HashMap<String, Object> fieldMap) {
        addToList(new SscCondition(SscConditionEnum.RIGHT_OUTER_JOIN, fieldMap, joinClass));
        return this;
    }
    public SscCondition innerJoin(Class<?> joinClass, HashMap<String, Object> fieldMap) {
        addToList(new SscCondition(SscConditionEnum.INNER_JOIN, fieldMap, joinClass));
        return this;
    }
}
