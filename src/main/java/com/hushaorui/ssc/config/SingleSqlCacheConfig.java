package com.hushaorui.ssc.config;

import com.hushaorui.ssc.common.em.ColumnNameStyle;
import com.hushaorui.ssc.common.em.SscLaunchPolicy;
import com.hushaorui.ssc.common.em.TableNameStyle;
import com.hushaorui.ssc.exception.SscRuntimeException;
import com.hushaorui.ssc.log.SscLogFactory;
import com.hushaorui.ssc.log.SscLogFactoryImpl;
import com.hushaorui.ssc.main.*;
import javafx.util.Pair;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 中心配置
 */
public class SingleSqlCacheConfig {
    /**缓存默认最大闲置时间(不使用后开始计算) 单位：毫秒，默认5分钟，如果设置为0或小于0，则表示关闭缓存 */
    private long maxInactiveTime = 300000;
    /** 缓存默认持久化间隔时间，同时清理闲置缓存 单位：毫秒 */
    private long persistenceIntervalTime = 60000;
    /** 表名风格 (没有指定表名时使用) */
    private TableNameStyle tableNameStyle = TableNameStyle.LOWERCASE_UNDERLINE;
    /** 字段名风格 (没有指定字段名时使用) */
    private ColumnNameStyle columnNameStyle = ColumnNameStyle.LOWERCASE_UNDERLINE;
    /** 唯一键辅助表的表名后缀 */
    private String uniqueTableNameSuffix = "_assist_table";
    /** 唯一键辅助表的id名称，如果和你的表字段名称重复，请重设此值 */
    private String uniqueTableIdName = "assist_id";
    /** 日志打印类 */
    private SscLogFactory logFactory = SscLogFactoryImpl.getInstance();

    /** json数据转换器 */
    private JSONSerializer jsonSerializer;
    /** java类型和表类型的映射 */
    private Map<Class<?>, String> javaTypeToTableType = new HashMap<>();
    {
        try {
            // 为了解耦
            jsonSerializer = (JSONSerializer) Class.forName("com.hushaorui.ssc.config.DefaultJSONSerializer").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new SscRuntimeException(e);
        }

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
    /** 分表策略 */
    private Map<Class<?>, TreeMap<Comparable, TableSplitPolicy<? extends Comparable>>> tableSplitPolicyMap = new HashMap<>();
    {
        addTableSplitPolicy(Integer.class, DefaultIntegerTableSplitPolicy.getInstance());
        addTableSplitPolicy(Long.class, DefaultLongTableSplitPolicy.getInstance());
        addTableSplitPolicy(String.class, DefaultStringTableSplitPolicy.getInstance());
    }
    /** 添加分表策略 */
    private void addTableSplitPolicy(Class<?> idClass, TableSplitPolicy<? extends Comparable> tableSplitPolicy) {
        tableSplitPolicyMap.computeIfAbsent(idClass, key -> new TreeMap<>()).put(tableSplitPolicy.getRange().getKey(), tableSplitPolicy);
    }
    /** 添加新的分表策略，添加前请清空默认值 */
    public void addTableSplitPolicy(Class<?> idClass, TreeMap<Comparable, TableSplitPolicy<? extends Comparable>> treeMap) {
        // 作用范围，包括最小值，不包括最大值
        checkTableSplitPolicyTreeMap(treeMap);
        tableSplitPolicyMap.put(idClass, treeMap);
    }
    /** 检查分表策略的作用范围是否重叠 */
    private void checkTableSplitPolicyTreeMap(TreeMap<Comparable, TableSplitPolicy<? extends Comparable>> treeMap) {
        Comparable lastValue = null;
        for (TableSplitPolicy<? extends Comparable> policy : treeMap.values()) {
            Pair<? extends Comparable, ? extends Comparable> policyRange = policy.getRange();
            if (lastValue == null) {
                lastValue = policyRange.getValue();
            } else {
                Comparable key = policyRange.getKey();
                if (key.compareTo(lastValue) < 0) {
                //if (policyRange.getKey() < lastValue) {
                    throw new SscRuntimeException(String.format("The range of TableSplitPolicy is overlapping, value1:%s, value2:%s", key, lastValue));
                }
            }
        }
    }

    public long getMaxInactiveTime() {
        return maxInactiveTime;
    }

    public void setMaxInactiveTime(long maxInactiveTime) {
        this.maxInactiveTime = maxInactiveTime;
    }

    public long getPersistenceIntervalTime() {
        return persistenceIntervalTime;
    }

    public void setPersistenceIntervalTime(long persistenceIntervalTime) {
        this.persistenceIntervalTime = persistenceIntervalTime;
    }

    public TableNameStyle getTableNameStyle() {
        return tableNameStyle;
    }

    public void setTableNameStyle(TableNameStyle tableNameStyle) {
        this.tableNameStyle = tableNameStyle;
    }

    public ColumnNameStyle getColumnNameStyle() {
        return columnNameStyle;
    }

    public void setColumnNameStyle(ColumnNameStyle columnNameStyle) {
        this.columnNameStyle = columnNameStyle;
    }

    public String getUniqueTableNameSuffix() {
        return uniqueTableNameSuffix;
    }

    public void setUniqueTableNameSuffix(String uniqueTableNameSuffix) {
        this.uniqueTableNameSuffix = uniqueTableNameSuffix;
    }

    public String getUniqueTableIdName() {
        return uniqueTableIdName;
    }

    public void setUniqueTableIdName(String uniqueTableIdName) {
        this.uniqueTableIdName = uniqueTableIdName;
    }

    public JSONSerializer getJsonSerializer() {
        return jsonSerializer;
    }

    public void setJsonSerializer(JSONSerializer jsonSerializer) {
        this.jsonSerializer = jsonSerializer;
    }

    public Map<Class<?>, String> getJavaTypeToTableType() {
        return javaTypeToTableType;
    }

    public void setJavaTypeToTableType(Map<Class<?>, String> javaTypeToTableType) {
        this.javaTypeToTableType = javaTypeToTableType;
    }

    public String getDefaultTableType() {
        return defaultTableType;
    }

    public void setDefaultTableType(String defaultTableType) {
        this.defaultTableType = defaultTableType;
    }

    public int getDefaultLength() {
        return defaultLength;
    }

    public void setDefaultLength(int defaultLength) {
        this.defaultLength = defaultLength;
    }

    public SscLaunchPolicy getLaunchPolicy() {
        return launchPolicy;
    }

    public void setLaunchPolicy(SscLaunchPolicy launchPolicy) {
        this.launchPolicy = launchPolicy;
    }

    public Map<Class<?>, IdGeneratePolicy<?>> getIdGeneratePolicyMap() {
        return idGeneratePolicyMap;
    }

    public void setIdGeneratePolicyMap(Map<Class<?>, IdGeneratePolicy<?>> idGeneratePolicyMap) {
        this.idGeneratePolicyMap = idGeneratePolicyMap;
    }

    public Map<Class<?>, TreeMap<Comparable, TableSplitPolicy<? extends Comparable>>> getTableSplitPolicyMap() {
        return tableSplitPolicyMap;
    }

    public void setTableSplitPolicyMap(Map<Class<?>, TreeMap<Comparable, TableSplitPolicy<? extends Comparable>>> tableSplitPolicyMap) {
        this.tableSplitPolicyMap = tableSplitPolicyMap;
    }

    public SscLogFactory getLogFactory() {
        return logFactory;
    }

    public void setLogFactory(SscLogFactory logFactory) {
        this.logFactory = logFactory;
    }
}
