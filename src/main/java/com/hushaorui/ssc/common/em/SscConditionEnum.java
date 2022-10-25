package com.hushaorui.ssc.common.em;

public enum SscConditionEnum {
    AND("&"),
    OR("|"),
    LEFT_OUTER_JOIN("<"),
    RIGHT_OUTER_JOIN(">"),
    INNER_JOIN("*"),
    ON("_"),
    FIRST("^"),
    MAX("$"),
    ;
    /** 一个指代符号 */
    private String sign;

    public String getSign() {
        return sign;
    }

    SscConditionEnum(String sign) {
        this.sign = sign;
    }
}
