package com.hushaorui.ssc.config;

import com.hushaorui.ssc.common.em.ColumnNameStyle;
import com.hushaorui.ssc.common.em.SscLaunchPolicy;
import com.hushaorui.ssc.common.em.TableNameStyle;
import com.hushaorui.ssc.main.DefaultIntegerIdGeneratePolicy;
import com.hushaorui.ssc.main.DefaultLongIdGeneratePolicy;
import com.hushaorui.ssc.main.DefaultStringIdGeneratePolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 中心配置
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SingleSqlCacheConfig {
    /**缓存默认保留时间(不使用后开始计时) 单位：毫秒，默认5分钟 */
    private long maxInactiveTime = 300000L;
    /** 表名风格 (没有指定表名时使用) */
    private TableNameStyle tableNameStyle = TableNameStyle.LOWERCASE_UNDERLINE;
    /** 字段名风格 (没有指定字段名时使用) */
    private ColumnNameStyle columnNameStyle = ColumnNameStyle.LOWERCASE_UNDERLINE;
    /** json数据转换器 */
    private JSONSerializer jsonSerializer = new DefaultJSONSerializer();
    /** java类型和表类型的映射 */
    private Map<Class<?>, String> javaTypeToTableType = new HashMap<>();
    {
        javaTypeToTableType.put(String.class, "VARCHAR");
        javaTypeToTableType.put(byte.class, "TINYINT");
        javaTypeToTableType.put(Byte.class, "TINYINT");
        javaTypeToTableType.put(short.class, "SMALLINT");
        javaTypeToTableType.put(Short.class, "SMALLINT");
        javaTypeToTableType.put(int.class, "INT");
        javaTypeToTableType.put(Integer.class, "INT");
        javaTypeToTableType.put(long.class, "BIGINT");
        javaTypeToTableType.put(Long.class, "BIGINT");
        javaTypeToTableType.put(float.class, "FLOAT");
        javaTypeToTableType.put(Float.class, "FLOAT");
        javaTypeToTableType.put(double.class, "DOUBLE");
        javaTypeToTableType.put(Double.class, "DOUBLE");
        javaTypeToTableType.put(boolean.class, "BOOL");
        javaTypeToTableType.put(Boolean.class, "BOOL");
        javaTypeToTableType.put(char.class, "VARCHAR");
        javaTypeToTableType.put(Character.class, "VARCHAR");
        javaTypeToTableType.put(Timestamp.class, "TIMESTAMP");
        javaTypeToTableType.put(Date.class, "DATETIME");
    }
    /** 默认情况下表格字段的类型 */
    private String defaultTableType = "JSON";
    /** 默认的 VARCHAR 类型长度 */
    private int defaultLength = 64;

    /** 添加新的java类型与数据库表类型的映射，可直接覆盖 */
    public void addJavaTypeAndTableType(Class<?> javaType, String columnType) {
        javaTypeToTableType.put(javaType, columnType);
    }

    /** 启动策略 */
    private SscLaunchPolicy launchPolicy = SscLaunchPolicy.CREATE_TABLE_IF_NOT_EXIST;

    /** id生成策略 */
    private Map<Class<?>, IdGeneratePolicy<?>> idGeneratePolicyMap = new HashMap<>();
    {
        idGeneratePolicyMap.put(Integer.class, DefaultIntegerIdGeneratePolicy.getInstance());
        idGeneratePolicyMap.put(Long.class, DefaultLongIdGeneratePolicy.getInstance());
        idGeneratePolicyMap.put(String.class, DefaultStringIdGeneratePolicy.getInstance());
    }
}
