package com.hushaorui.ssc.main;

import com.alibaba.fastjson.JSONArray;
import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;
import com.hushaorui.ssc.common.data.DataClassDesc;
import com.hushaorui.ssc.common.data.SscTableInfo;
import com.hushaorui.ssc.common.em.CacheStatus;
import com.hushaorui.ssc.config.IdGeneratePolicy;
import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.config.TableSplitPolicy;
import com.hushaorui.ssc.exception.SscRuntimeException;
import com.hushaorui.ssc.util.SscStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
public class TableOperatorFactory {
    /**
     * 核心配置
     */
    protected SingleSqlCacheConfig globalConfig;
    /**
     * 数据库操作类
     */
    protected JdbcTemplate jdbcTemplate;
    /**
     * 定时器
     */
    private Timer timer;
    /**
     * 定时任务
     */
    private TimerTask timerTask;
    /**
     * 工厂名称
     */
    private String name;
    /**
     * 数据库的数据缓存 key1:pojoClass, key2:idValue, value:cacheObject
     */
    private Map<Class<?>, Map<Serializable, ObjectInstanceCache>> idCacheMap = new ConcurrentHashMap<>();
    /**
     * 存入缓存的开关，全局性质，控制所有
     */
    private boolean saveSwitch;
    /**
     * 取出缓存的开关，全局性质，控制所有
     */
    private boolean getSwitch;
    /**
     * 所有的数据类描述
     */
    protected static final Map<Class<?>, SscTableInfo> tableInfoMapping = new HashMap<>();
    /** 给用户操作的缓存类 */
    protected static final Map<Class<?>, Operator<?>> userOperatorMap = new HashMap<>();
    /** 无缓存的操作类 */
    protected static final Map<Class<?>, Operator<?>> noCachedOperatorMap = new HashMap<>();
    protected static final Map<String, SscTableInfo> tableNameMapping = new HashMap<>();

    public TableOperatorFactory(String name, JdbcTemplate jdbcTemplate, SingleSqlCacheConfig globalConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.globalConfig = globalConfig;
        if (globalConfig.getMaxInactiveTime() > 0) {
            // 开启缓存
            this.name = TableOperatorFactory.class.getSimpleName() + "-" + name + "-Timer";
            timer = new Timer(this.name);
            // 默认 60000 (一分钟)
            timer.schedule(timerTask = getTimerTask(), globalConfig.getPersistenceIntervalTime(), globalConfig.getPersistenceIntervalTime());
            saveSwitch = true;
            getSwitch = true;
            log.info(String.format("缓存已开启，持久化间隔：%s 毫秒，缓存最大闲置时间: %s 毫秒", globalConfig.getPersistenceIntervalTime(), globalConfig.getMaxInactiveTime()));
        } else {
            saveSwitch = false;
            getSwitch = false;
        }
    }

    public TableOperatorFactory(JdbcTemplate jdbcTemplate, SingleSqlCacheConfig globalConfig) {
        this("default", jdbcTemplate, globalConfig);
    }

    public TableOperatorFactory(String name, JdbcTemplate jdbcTemplate) {
        this(name, jdbcTemplate, new SingleSqlCacheConfig());
    }

    public TableOperatorFactory(JdbcTemplate jdbcTemplate) {
        this("default", jdbcTemplate, new SingleSqlCacheConfig());
    }

    private TimerTask getTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                log.info(String.format("%s 定时任务执行开始", name));
                final long now = System.currentTimeMillis();
                try {
                    idCacheMap.forEach((objectClass, value) -> value.entrySet().removeIf(cacheEntry -> {
                        ObjectInstanceCache cache = cacheEntry.getValue();
                        if (cache.getCacheStatus().isNeedSynToDB()) {
                            Operator<?> noCachedOperator = getNoCachedOperator(objectClass);
                            if (noCachedOperator == null) {
                                return false;
                            }
                            String option = "";
                            Object optionObject = null;
                            try {
                                synchronized (cache) {
                                    optionObject = cache.getObjectInstance();
                                    CacheStatus newStatus;
                                    switch (cache.getCacheStatus()) {
                                        case DELETE: {
                                            option = "deleteById";
                                            if (optionObject != null) {
                                                noCachedOperator.delete(cache.getId());
                                            }
                                            newStatus = CacheStatus.AFTER_DELETE;
                                        }
                                        break;
                                        case INSERT: {
                                            option = "insert";
                                            try {
                                                doInsert(noCachedOperator, optionObject);
                                                newStatus = CacheStatus.AFTER_INSERT;
                                            } catch (Exception e) {
                                                log.error(String.format("缓存插入数据失败，cache: %s", JSONArray.toJSONString(cache)), e);
                                                // 插入失败的数据不删除
                                                return false;
                                            }
                                        }
                                        break;
                                        case UPDATE: {
                                            option = "update";
                                            try {
                                                doUpdate(noCachedOperator, optionObject);
                                                newStatus = CacheStatus.AFTER_UPDATE;
                                            } catch (Exception e) {
                                                log.error(String.format("缓存更新数据失败，cache: %s", JSONArray.toJSONString(cache)), e);
                                                // 插入失败的数据不删除
                                                return false;
                                            }
                                        }
                                        break;
                                        default: {
                                            newStatus = CacheStatus.AFTER_UPDATE;
                                        }
                                    }
                                    // 最后设置状态
                                    cache.setCacheStatus(newStatus);
                                }
                            } catch (Exception e) {
                                log.error(String.format("缓存同步进数据库失败,class:%s, object:%s, option:%s",
                                        objectClass.getName(), JSONArray.toJSONString(optionObject), option), e);
                            }
                        }
                        // 清理超过最大闲置时间的缓存数据
                        boolean remove = cache.getLastUseTime() + globalConfig.getMaxInactiveTime() < now;
                        if (remove) {
                            log.info(String.format("因长时间未使用，删除缓存, class:%s, id:%s", objectClass.getName(), cacheEntry.getKey()));
                        }
                        return remove;
                    }));
                    /*uniqueFieldCacheMap.entrySet().removeIf(entry1 -> {
                        Map<String, Map<Object, ObjectInstanceCache>> outerMap = entry1.getValue();
                        outerMap.entrySet().removeIf(entry2 -> {
                            Map<Object, ObjectInstanceCache> innerMap = entry2.getValue();
                            innerMap.entrySet().removeIf(entry -> {
                                long lastUseTime = entry.getValue().getLastUseTime();
                                boolean delete = lastUseTime == 0 || lastUseTime + maxInactiveTime < now;
                                if (delete) {
                                    log.info(String.format("因长时间未使用，删除(unique)缓存, class:%s,uniqueName:%s,uniqueValue:%s", entry1.getKey().getName(), entry2.getKey(), entry.getKey()));
                                }
                                return delete;
                            });
                            return innerMap.isEmpty();
                        });
                        return outerMap.isEmpty();
                    });*/
                    /*findAllFieldCacheMap.entrySet().removeIf(entry1 -> {
                        Map<String, Map<Object, DataIdCache>> map1 = entry1.getValue();
                        map1.entrySet().removeIf(entry2 -> {
                            Map<Object, DataIdCache> map2 = entry2.getValue();
                            map2.entrySet().removeIf(entry -> {
                                long lastUseTime = entry.getValue().getLastUseTime();
                                boolean delete = lastUseTime == 0 || lastUseTime + maxInactiveTime < now;
                                if (delete) {
                                    log.info(String.format("因长时间未使用，删除(findAll)缓存, class:%s,fieldName:%s,fieldValue:%s", entry1.getKey().getName(), entry2.getKey(), entry.getKey()));
                                }
                                return delete;
                            });
                            return map2.isEmpty();
                        });
                        return map1.isEmpty();
                    });*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.info(String.format("%s 定时任务执行结束", name));
            }
        };
    }
    /**
     * 关闭缓存
     */
    public void close() {
        log.warn("开始关闭缓存");
        // 先关闭存储缓存
        saveSwitch = false;
        // 取消任务
        timerTask.cancel();
        // 最后执行一遍任务
        timerTask.run();
        // 关闭定时器
        timer.cancel();
        // 清理所有缓存数据
        idCacheMap.clear();
        // 清理所有唯一字段值缓存
        //uniqueFieldCacheMap.clear();
        // 清理数据集缓存
        //findAllFieldCacheMap.clear();
        // 最后关闭取缓存开关，保证缓存中的数据都同步到了数据库后才开始从数据库中直接拿数据
        getSwitch = false;
        log.warn("缓存已关闭");
    }

    private SscTableInfo getTableInfoNotNull(Class<?> dataClass) {
        return tableInfoMapping.computeIfAbsent(dataClass, clazz -> {
            DataClassDesc dataClassDesc = new DataClassDesc();
            dataClassDesc.setDataClass(clazz);
            // 表的数量
            int tableCount;
            DataClass annotation = clazz.getDeclaredAnnotation(DataClass.class);
            // 表名
            String tableName;
            if (annotation == null) {
                // 默认不分表
                tableCount = 1;
                tableName = generateTableName(clazz);

            } else {
                tableCount = annotation.tableCount();
                String value = annotation.value();
                if (value.length() == 0) {
                    tableName = generateTableName(clazz);
                } else {
                    tableName = value;
                }
            }
            if (tableNameMapping.containsKey(tableName)) {
                // 表名重复
                throw new SscRuntimeException("Duplicate table name: " + tableName);
            }
            // 所有的类字段名和表字段名的映射
            Map<String, String> propColumnMapping = new LinkedHashMap<>();
            // 所有的表字段名和类字段名的映射
            Map<String, String> columnPropMapping = new LinkedHashMap<>();
            // 所有的类字段名和数据库表字段类型的映射
            Map<String, String> propColumnTypeMapping = new HashMap<>();
            // 所有类字段名和它的get方法
            Map<String, Method> propGetMethods = new HashMap<>();
            // 所有的类字段名和它的set方法
            Map<String, Method> propSetMethods = new HashMap<>();
            // 所有不为空的字段集合
            Set<String> notNullProps = new HashSet<>();
            // 有默认值的字段集合
            Map<String, String> propDefaultValues = new HashMap<>();
            // 所有唯一键的字段集合
            Map<String, Set<String>> uniqueProps = new HashMap<>();
            // 所有不会更新的字段集合
            Set<String> notUpdateProps = new HashSet<>();
            // 所有不需要缓存的字段集合
            Set<String> notCachedProps = new HashSet<>();

            // id字段的名称
            String idPropName = null;
            // 使用使用id生成策略
            Boolean idIsAuto = null;
            Class<?> tempClass = clazz;
            while (!Object.class.equals(tempClass)) {
                Field[] declaredFields = tempClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    field.setAccessible(true);
                    if (Modifier.isStatic(field.getModifiers())) {
                        // 静态字段，不能当作属性字段
                        continue;
                    }
                    // 类字段名
                    String propName = field.getName();
                    // 表字段名
                    String columnName;
                    FieldDesc fieldDesc = field.getDeclaredAnnotation(FieldDesc.class);
                    if (fieldDesc == null) {
                        columnName = generateColumnName(propName);
                    } else {
                        if (fieldDesc.noneSave()) {
                            // 该字段不参与数据库操作
                            continue;
                        }
                        if (fieldDesc.isId()) {
                            if (idPropName != null) {
                                throw new SscRuntimeException(String.format("ID field can only have one in class: %s", clazz.getName()));
                            }
                            idPropName = propName;
                            idIsAuto = fieldDesc.isAuto();
                        }
                        String value = fieldDesc.value();
                        if (value.length() == 0) {
                            columnName = generateColumnName(propName);
                        } else {
                            columnName = value;
                        }
                    }
                    if (columnPropMapping.containsKey(columnName)) {
                        throw new SscRuntimeException(String.format("Duplicate column name: %s in class: %s", columnName, clazz.getName()));
                    }
                    // 将类字段名和表字段名添加映射
                    propColumnMapping.put(propName, columnName);
                    columnPropMapping.put(columnName, propName);
                    Class<?> fieldType = field.getType();
                    String getMethodName = SscStringUtils.getGetMethodNameByFieldName(field);
                    Method getMethod;
                    try {
                        getMethod = clazz.getMethod(getMethodName);
                        propGetMethods.put(propName, getMethod);
                    } catch (NoSuchMethodException e) {
                        throw new SscRuntimeException(String.format("Field has no getter method, name: %s in class: %s", propName, clazz.getName()));
                    }
                    String setMethodName = SscStringUtils.getSetMethodNameByFieldName(field);
                    Method setMethod;
                    try {
                        setMethod = clazz.getMethod(setMethodName, fieldType);
                        propSetMethods.put(propName, setMethod);
                    } catch (NoSuchMethodException e) {
                        throw new SscRuntimeException(String.format("Field has no setter method, name: %s in class: %s", propName, clazz.getName()));
                    }
                    String columnType;
                    if (fieldDesc != null) {
                        if (fieldDesc.isNotNull()) {
                            notNullProps.add(propName);
                        } else {
                            String defaultValue = fieldDesc.defaultValue();
                            if (defaultValue.length() > 0) {
                                propDefaultValues.put(propName, defaultValue);
                            }
                        }

                        String uniqueName = fieldDesc.uniqueName();
                        if (uniqueName.length() > 0) {
                            uniqueProps.computeIfAbsent(uniqueName, key -> new HashSet<>()).add(propName);
                        }
                        if (fieldDesc.isNotUpdate()) {
                            notUpdateProps.add(propName);
                        }
                        if (!fieldDesc.cached()) {
                            notCachedProps.add(propName);
                        }
                        if (fieldDesc.columnType().length() == 0) {
                            columnType = globalConfig.getJavaTypeToTableType().getOrDefault(fieldType, globalConfig.getDefaultTableType());
                        } else {
                            columnType = fieldDesc.columnType();
                        }
                    } else {
                        // 没有 FieldDesc 注解，默认也当作表字段解析
                        columnType = globalConfig.getJavaTypeToTableType().getOrDefault(fieldType, globalConfig.getDefaultTableType());
                    }
                    // 这里目前只考虑到了mysql等和mysql差不多的数据库
                    if ("VARCHAR".equals(columnType)) {
                        columnType = String.format("%s(%s)", columnType, globalConfig.getDefaultLength());
                    }
                    propColumnTypeMapping.put(propName, columnType);
                }
                tempClass = tempClass.getSuperclass();
            }
            if (propColumnMapping.isEmpty()) {
                throw new SscRuntimeException("No data in the table: " + tableName);
            }
            if (idPropName == null) {
                // 没有标注id，则第一个字段为id
                idPropName = propColumnMapping.keySet().iterator().next();
                // 默认使用id生成策略
                idIsAuto = true;
            }
            dataClassDesc.setTableCount(tableCount);
            dataClassDesc.setTableName(tableName);
            dataClassDesc.setColumnPropMapping(columnPropMapping);
            dataClassDesc.setPropColumnMapping(propColumnMapping);
            dataClassDesc.setPropColumnTypeMapping(propColumnTypeMapping);
            dataClassDesc.setPropGetMethods(propGetMethods);
            dataClassDesc.setPropSetMethods(propSetMethods);
            dataClassDesc.setIdPropName(idPropName);
            dataClassDesc.setUseIdGeneratePolicy(idIsAuto);
            dataClassDesc.setNotNullProps(notNullProps);
            dataClassDesc.setPropDefaultValues(propDefaultValues);
            dataClassDesc.setUniqueProps(uniqueProps);
            // id是不进行更新的
            notUpdateProps.add(idPropName);
            dataClassDesc.setNotUpdateProps(notUpdateProps);
            dataClassDesc.setNotCachedProps(notCachedProps);

            SscTableInfo sscTableInfo = new SscTableInfo(dataClassDesc, globalConfig);
            tableNameMapping.put(tableName, sscTableInfo);
            switch (globalConfig.getLaunchPolicy()) {
                case DROP_TABLE: {
                    // 删除表
                    dropTable(sscTableInfo.getDropTableSql());
                    break;
                }
                case DROP_TABLE_AND_CRETE: {
                    // 删除表
                    dropTable(sscTableInfo.getDropTableSql());
                    // 然后创建表
                    createTable(sscTableInfo.getCreateTableSql());
                    break;
                }
                case CREATE_TABLE_IF_NOT_EXIST: {
                    createTable(sscTableInfo.getCreateTableSql());
                    break;
                }
                /*case MODIFY_COLUMN_IF_UPDATE: {
                    createTable(sscTableInfo.getCreateTableSql());
                    // TODO 待开发
                    break;
                }*/
            }
            //System.out.println(JSONArray.toJSONString(sscTableInfo, SerializerFeature.PrettyFormat));
            IdGeneratePolicy idGeneratePolicy = globalConfig.getIdGeneratePolicyMap().get(sscTableInfo.getIdJavaType());
            if (idGeneratePolicy instanceof DefaultIntegerIdGeneratePolicy) {
                DefaultIntegerIdGeneratePolicy policy = (DefaultIntegerIdGeneratePolicy) idGeneratePolicy;
                int maxId = 0;
                for (String sql : sscTableInfo.getSelectMaxIdSql()) {
                    Integer id = jdbcTemplate.queryForObject(sql, Integer.class);
                    if (id != null && id > maxId) {
                        maxId = id;
                    }
                }
                maxId++;
                policy.map.put(clazz, new AtomicInteger(maxId));
                log.info(String.format("默认id生成器:%s, 初始化起始id:%s", idGeneratePolicy.getClass().getName(), maxId));
            } else {
                Map<Class<?>, AtomicLong> map;
                if (idGeneratePolicy instanceof DefaultLongIdGeneratePolicy) {
                    DefaultLongIdGeneratePolicy policy = (DefaultLongIdGeneratePolicy) idGeneratePolicy;
                    map = policy.map;
                } else if (idGeneratePolicy instanceof DefaultStringIdGeneratePolicy) {
                    DefaultStringIdGeneratePolicy policy = (DefaultStringIdGeneratePolicy) idGeneratePolicy;
                    map = policy.map;
                } else {
                    map = null;
                }
                if (map != null) {
                    long maxId = 0;
                    for (String sql : sscTableInfo.getSelectMaxIdSql()) {
                        Long id = jdbcTemplate.queryForObject(sql, Long.class);
                        if (id != null && id > maxId) {
                            maxId = id;
                        }
                    }
                    maxId++;
                    map.put(clazz, new AtomicLong(maxId));
                    log.info(String.format("默认id生成器:%s, 初始化起始id:%s", idGeneratePolicy.getClass().getName(), maxId));
                }
            }
            log.info(String.format("class: %s register success", clazz.getName()));
            return sscTableInfo;
        });
    }

    /**
     * 提前注册所有的数据类
     */
    public void registerDataClass(Class<?> dataClass) {
        getTableInfoNotNull(dataClass);
    }

    private void dropTable(String[] dropTableSql) {
        for (String sql : dropTableSql) {
            jdbcTemplate.execute(sql);
            log.info("执行删除表sql: " + sql);
        }
    }

    private void createTable(String[] createTableSql) {
        for (String sql : createTableSql) {
            try {
                jdbcTemplate.execute(sql);
                log.info("执行创建表sql: " + sql);
            } catch (Exception e) {
                if (!e.getMessage().contains("already exists")) {
                    throw new SscRuntimeException(e);
                }
            }
        }
    }


    private String generateColumnName(String propName) {
        switch (globalConfig.getColumnNameStyle()) {
            case UPPERCASE_UNDERLINE:
                return SscStringUtils.humpToUnderline(propName).toUpperCase();
            case LOWERCASE:
                return propName.toLowerCase();
            case UPPERCASE:
                return propName.toUpperCase();
            default:
                return SscStringUtils.humpToUnderline(propName).toLowerCase();
        }
    }

    private String generateTableName(Class<?> clazz) {
        switch (globalConfig.getTableNameStyle()) {
            case UPPERCASE_UNDERLINE:
                return SscStringUtils.humpToUnderline(clazz.getSimpleName()).toUpperCase();
            case SIMPLE_CLASS_NAME:
                return clazz.getSimpleName();
            case LOWERCASE_SIMPLE_CLASS_NAME:
                return clazz.getSimpleName().toLowerCase();
            case UPPERCASE_SIMPLE_CLASS_NAME:
                return clazz.getSimpleName().toUpperCase();
            case LOWERCASE_FULL_CLASS_NAME_UNDERLINE:
                return clazz.getName().replace(".", "_").toLowerCase();
            case UPPERCASE_FULL_CLASS_NAME_UNDERLINE:
                return clazz.getName().replace(".", "_").toUpperCase();
            default:
                return SscStringUtils.humpToUnderline(clazz.getSimpleName());
        }
    }

    private <T> Operator<T> getNoCachedOperator(Class<T> dataClass) {
        SscTableInfo sscTableInfo = getTableInfoNotNull(dataClass);
        if (sscTableInfo == null) {
            throw new SscRuntimeException("The class is unregistered: " + dataClass.getName());
        }
        return (Operator<T>) noCachedOperatorMap.computeIfAbsent(dataClass, clazz -> new Operator<Object>() {
            private RowMapper<T> rowMapper;
            @Override
            public void insert(Object object) {
                DataClassDesc classDesc = sscTableInfo.getClassDesc();
                Object id = getIdValueFromObject(classDesc, object);
                if (id == null) {
                    if (classDesc.isUseIdGeneratePolicy()) {
                        IdGeneratePolicy<?> idGeneratePolicy = globalConfig.getIdGeneratePolicyMap().get(sscTableInfo.getIdJavaType());
                        // 根据id生成策略获取新的id
                        id = idGeneratePolicy.getId(clazz);
                        setObjectId(object, id, classDesc);
                    } else {
                        throw new SscRuntimeException("插入数据时，id为null且不使用id生成策略, class: " + clazz.getName());
                    }
                }
                int tableIndex = getTableIndex(id, classDesc.getTableCount());
                String insertSql = sscTableInfo.getInsertSql()[tableIndex];
                Object[] params = getInsertParamArrayFromObject(classDesc, object);
                log.info(String.format("执行插入语句：%s, id:%s", insertSql, id));
                jdbcTemplate.update(insertSql, params);
            }

            @Override
            public Object selectById(Serializable id, Function<Serializable, Object> insertFunction) {
                Object object = selectById(id);
                if (object != null) {
                    return object;
                }
                if (insertFunction == null) {
                    return null;
                }
                Object apply = insertFunction.apply(id);
                if (apply == null) {
                    return null;
                }
                insert(apply);
                return apply;
            }

            @Override
            public void update(Object object) {
                DataClassDesc classDesc = sscTableInfo.getClassDesc();
                Object id = getIdValueFromObject(classDesc, object);
                int tableIndex = getTableIndex(id, classDesc.getTableCount());
                String updateSql = sscTableInfo.getUpdateAllNotCachedByIdSql()[tableIndex];
                Object[] params = getUpdateParamArrayFromObject(classDesc, object);
                log.info(String.format("执行更新语句：%s, id:%s", updateSql, id));
                jdbcTemplate.update(updateSql, params);
            }

            @Override
            public void delete(Object object) {
                DataClassDesc classDesc = sscTableInfo.getClassDesc();
                Object id = getIdValueFromObject(classDesc, object);
                int tableIndex = getTableIndex(id, classDesc.getTableCount());
                String deleteSql = sscTableInfo.getDeleteByIdSql()[tableIndex];
                log.info(String.format("执行删除语句：%s, id:%s", deleteSql, id));
                jdbcTemplate.update(deleteSql, id);
            }

            @Override
            public void delete(Serializable id) {
                int tableIndex = getTableIndex(id, sscTableInfo.getClassDesc().getTableCount());
                String deleteSql = sscTableInfo.getDeleteByIdSql()[tableIndex];
                jdbcTemplate.update(deleteSql, id);
            }

            @Override
            public Object selectById(Serializable id) {
                DataClassDesc classDesc = sscTableInfo.getClassDesc();
                int tableIndex = getTableIndex(id, classDesc.getTableCount());
                String sql = sscTableInfo.getSelectByIdSql()[tableIndex];
                if (rowMapper == null) {
                    synchronized (this) {
                        if (rowMapper == null) {
                            rowMapper = (resultSet, rowNum) -> {
                                try {
                                    Object data = clazz.newInstance();
                                    for (Map.Entry<String, String> entry : classDesc.getPropColumnMapping().entrySet()) {
                                        String propName = entry.getKey();
                                        String columnName = entry.getValue();
                                        Class<?> propType = classDesc.getPropGetMethods().get(propName).getReturnType();
                                        String methodName;
                                        // 如果有其他特殊的方法，这里还需要添加
                                        if (Integer.class.equals(propType) || int.class.equals(propType)) {
                                            methodName = "getInt";
                                        } else {
                                            if (propType.isPrimitive()) {
                                                // 基本数据类型，首字母要大写
                                                methodName = "get" + propType.getSimpleName().substring(0, 1).toUpperCase() + propType.getSimpleName().substring(1);
                                            } else {
                                                methodName = "get" + propType.getSimpleName();
                                            }
                                        }
                                        Method method = ResultSet.class.getMethod(methodName, String.class);
                                        Method setMethod = classDesc.getPropSetMethods().get(propName);
                                        setMethod.invoke(data, method.invoke(resultSet, columnName));
                                    }
                                    return (T) data;
                                } catch (Exception e) {
                                    throw new SscRuntimeException(String.format("select error! sql: [%s], id: %s", sql, id), e);
                                }
                            };
                        }
                    }
                }
                try {
                    return jdbcTemplate.queryForObject(sql, rowMapper, id);
                } catch (EmptyResultDataAccessException ignore) {
                    // queryForObject 在查不到数据时会抛出此异常
                    return null;
                }
            }
        });
    }

    /** 将id设置到对象中去 */
    private void setObjectId(Object object, Object id, DataClassDesc classDesc) {
        Method idSetMethod = classDesc.getPropSetMethods().get(classDesc.getIdPropName());
        try {
            // 将新的id设置到对象中
            idSetMethod.invoke(object, id);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SscRuntimeException(e);
        }
    }

    public <T> Operator<T> getOperator(Class<T> dataClass) {
        if (globalConfig.getMaxInactiveTime() <= 0) {
            return getNoCachedOperator(dataClass);
        }
        SscTableInfo sscTableInfo = getTableInfoNotNull(dataClass);
        if (sscTableInfo == null) {
            throw new SscRuntimeException("The class is unregistered: " + dataClass.getName());
        }
        return (Operator<T>) userOperatorMap.computeIfAbsent(dataClass, clazz -> new Operator<Object>() {
            private final DataClassDesc classDesc = sscTableInfo.getClassDesc();

            @Override
            public void insert(Object object) {
                if (! saveSwitch) {
                    // 存储缓存已关闭，直接插入数据库
                    getNoCachedOperator(dataClass).insert((T) object);
                    return;
                }
                insertOrUpdate(getIdValueFromObject(classDesc, object), object);
            }

            private void insertOrUpdate(Serializable id, Object object) {
                if (object == null) {
                    log.warn("尝试添加为null的缓存，添加失败");
                    return;
                }
                Class<?> objectClass = object.getClass();
                if (id == null) {
                    if (classDesc.isUseIdGeneratePolicy()) {
                        IdGeneratePolicy<?> idGeneratePolicy = globalConfig.getIdGeneratePolicyMap().get(sscTableInfo.getIdJavaType());
                        // 根据id生成策略获取新的id
                        id = (Serializable) idGeneratePolicy.getId(clazz);
                        setObjectId(object, id, classDesc);
                    } else {
                        id = getIdValueFromObject(classDesc, object);
                    }
                }
                insertOrUpDateOrDelete(objectClass, id, object, CacheStatus.INSERT);
            }

            private void insertOrUpDateOrDelete(Class<?> objectClass, Serializable id, Object object, CacheStatus newStatus) {
                if (CacheStatus.DELETE.equals(newStatus)) {
                    // 需要删除该数据
                    Map<Serializable, ObjectInstanceCache> classMap = idCacheMap.get(objectClass);
                    if (classMap != null) {
                        ObjectInstanceCache cache = classMap.get(id);
                        if (cache != null) {
                            synchronized (cache) {
                                cache.setCacheStatus(newStatus);
                                log.info(String.format("缓存预备删除数据, class:%s, id: %s, object: %s", objectClass.getName(), id, object));
                            }
                        }
                    }
                    return;
                }
                Map<Serializable, ObjectInstanceCache> classMap = idCacheMap.computeIfAbsent(objectClass, key -> new ConcurrentHashMap<>());
                ObjectInstanceCache cache = classMap.computeIfAbsent(id, key -> {
                    ObjectInstanceCache c = new ObjectInstanceCache();
                    c.setId(id);
                    return c;
                });
                synchronized (cache) {
                    if (object != null) {
                        cache.setObjectInstance(object);
                        if (cache.getLastModifyTime() == 0) {
                            // == 0的时候为该cache对象第一次创建的时候，可能需要判断唯一键
                            // TODO 唯一键的缓存
                            // TODO 条件查询的缓存
                        }
                    }
                    // 当前状态
                    CacheStatus currentStatus = cache.getCacheStatus();
                    if (currentStatus != null && newStatus.isNeedSynToDB()) {
                        // 删除的逻辑在上面已经处理过了， 这里不需要再考虑删除的情况
                        switch (currentStatus) {
                            // 已经删除的，现在要插入
                            case AFTER_DELETE:
                                cache.setCacheStatus(CacheStatus.INSERT);
                                break;
                            // 原本要删除的，转为更新
                            case DELETE:
                                // 已经插入的，转为更新
                            case AFTER_INSERT:
                                // 已经更新的，转为更新
                            case AFTER_UPDATE:
                                cache.setCacheStatus(CacheStatus.UPDATE);
                                break;
                            // 无需处理
                            case INSERT:
                                // 无需处理
                            case UPDATE:
                                break;
                        }
                    } else {
                        // 只是单纯添加缓存，不需要同步数据库
                        cache.setCacheStatus(newStatus);
                    }
                    cache.setObjectClass(objectClass);
                    long now = System.currentTimeMillis();
                    cache.setLastModifyTime(now);
                    cache.setLastUseTime(now);
                }
                log.info(String.format("%s缓存, class:%s, id: %s", cache.getCacheStatus(), objectClass.getName(), id));
            }

            @Override
            public void update(Object object) {
                if (object == null) {
                    log.warn("尝试更新为null的缓存，更新失败");
                    return;
                }
                Serializable id = getIdValueFromObject(classDesc, object);
                insertOrUpDateOrDelete(clazz, id, object, CacheStatus.UPDATE);
            }

            @Override
            public void delete(Object object) {
                Serializable id = getIdValueFromObject(classDesc, object);
                insertOrUpDateOrDelete(clazz, id, null, CacheStatus.AFTER_DELETE);
            }

            @Override
            public void delete(Serializable id) {
                insertOrUpDateOrDelete(clazz, id, null, CacheStatus.AFTER_DELETE);
            }

            @Override
            public Object selectById(Serializable id, Function<Serializable, Object> insertFunction) {
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.info(String.format("缓存已关闭，直接查询数据库, class:%s, id:%s", clazz.getName(), id));
                    Operator<?> noCachedOperator = getNoCachedOperator(clazz);
                    Object value = noCachedOperator.selectById(id);
                    if (value == null) {
                        synchronized (SscStringUtils.getLock(id, clazz)) {
                            value = noCachedOperator.selectById(id);
                            if (value == null && insertFunction != null) {
                                value = insertFunction.apply(id);
                                if (value != null) {
                                    doInsert(noCachedOperator, value);
                                }
                            }
                        }
                    }
                    return value;
                }
                Map<Serializable, ObjectInstanceCache> classMap = idCacheMap.computeIfAbsent(clazz, key -> new ConcurrentHashMap<>());
                ObjectInstanceCache cache = classMap.get(String.valueOf(id));
                if (cache == null) {
                    log.info(String.format("未找到缓存，直接查询数据库, class:%s, id:%s", clazz.getName(), id));
                    // 缓存击穿，查询数据库
                    Operator<?> noCachedOperator = getNoCachedOperator(clazz);
                    Object value = noCachedOperator.selectById(id);
                    if (value == null) {
                        synchronized (SscStringUtils.getLock(id, clazz)) {
                            value = noCachedOperator.selectById(id);
                            if (value == null && insertFunction != null) {
                                value = insertFunction.apply(id);
                                if (value == null) {
                                    insertOrUpDateOrDelete(clazz, id, null, CacheStatus.AFTER_DELETE);
                                } else {
                                    insertOrUpDateOrDelete(clazz, id, value, CacheStatus.INSERT);
                                }
                            }
                        }
                    }
                    return value;
                } else {
                    // 更新最后一次使用时间
                    cache.setLastUseTime(System.currentTimeMillis());
                    if (cache.getCacheStatus().isDelete() && insertFunction != null) {
                        synchronized (cache) {
                            // 该数据原本需要删除
                            if (CacheStatus.DELETE.equals(cache.getCacheStatus())) {
                                T newInstance = (T) insertFunction.apply(id);
                                cache.setObjectInstance(newInstance);
                                // 预备删除，转为更新
                                cache.setCacheStatus(CacheStatus.UPDATE);
                                return newInstance;
                            } else if (CacheStatus.AFTER_DELETE.equals(cache.getCacheStatus())) {
                                // 已经删除了，这里需要插入
                                T newInstance = (T) insertFunction.apply(id);
                                cache.setObjectInstance(newInstance);
                                cache.setCacheStatus(CacheStatus.INSERT);
                                return newInstance;
                            }
                        }
                    }
                    Object objectInstance = cache.getObjectInstance();
                    if (objectInstance == null && insertFunction != null) {
                        // 这里可能永远不会运行
                        synchronized (cache) {
                            objectInstance = cache.getObjectInstance();
                            if (objectInstance == null) {
                                objectInstance = insertFunction.apply(id);
                                cache.setObjectInstance(objectInstance);
                                cache.setCacheStatus(CacheStatus.INSERT);
                            }
                        }
                    }
                    return objectInstance;
                }
            }

            @Override
            public Object selectById(Serializable id) {
                return selectById(id, null);
            }
        });
    }

    private void doUpdate(Operator<?> operator, Object value) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Class<? extends Operator> genericType = SscStringUtils.getGenericType(operatorClass, 0);
            Method insertMethod = operatorClass.getMethod("update", genericType);
            insertMethod.invoke(operator, value);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("Update error! operator class: %s, value: %s", operator.getClass().getName(), JSONArray.toJSONString(value)), e);
        }
    }

    private void doInsert(Operator<?> operator, Object value) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Class<? extends Operator> genericType = SscStringUtils.getGenericType(operatorClass, 0);
            Method insertMethod = operatorClass.getMethod("insert", genericType);
            insertMethod.invoke(operator, value);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("Insert error! operator class: %s, value: %s", operator.getClass().getName(), JSONArray.toJSONString(value)), e);
        }
    }

    private Serializable getIdValueFromObject(DataClassDesc classDesc, Object object) {
        Method idGetMethod = classDesc.getPropGetMethods().get(classDesc.getIdPropName());
        try {
            return (Serializable) idGetMethod.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SscRuntimeException("Get id value error in class: " + classDesc.getDataClass().getName(), e);
        }
    }

    /**
     * 从对象中获取更新的所有字段数据
     */
    private Object[] getUpdateParamArrayFromObject(DataClassDesc classDesc, Object object) {
        Map<String, String> propColumnMapping = classDesc.getPropColumnMapping();
        Set<String> notUpdateProps = classDesc.getNotUpdateProps();
        Object[] paramArray = new Object[propColumnMapping.size() - notUpdateProps.size() + 1];
        int index = 0;
        for (String propName : propColumnMapping.keySet()) {
            if (notUpdateProps.contains(propName)) {
                // 不需要更新的字段，跳过
                continue;
            }
            Method method = classDesc.getPropGetMethods().get(propName);
            try {
                paramArray[index++] = method.invoke(object);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new SscRuntimeException(e);
            }
        }
        try {
            // 最后加上id
            paramArray[paramArray.length - 1] = classDesc.getPropGetMethods().get(classDesc.getIdPropName()).invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SscRuntimeException(e);
        }
        return paramArray;
    }

    /**
     * 从对象中获取插入的所有字段数据
     */
    private Object[] getInsertParamArrayFromObject(DataClassDesc classDesc, Object object) {
        Map<String, String> propColumnMapping = classDesc.getPropColumnMapping();
        Object[] paramArray = new Object[propColumnMapping.size()];
        int index = 0;
        for (String propName : propColumnMapping.keySet()) {
            Method method = classDesc.getPropGetMethods().get(propName);
            try {
                paramArray[index++] = method.invoke(object);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new SscRuntimeException(e);
            }
        }
        return paramArray;
    }

    /**
     * 根据id和分表的数量获取id对应的表的索引
     */
    private int getTableIndex(Object id, int tableCount) {
        if (tableCount == 1) {
            // 不分表
            return 0;
        } else {
            Comparable comparableId = (Comparable) id;
            // 根据id获取表的索引
            TableSplitPolicy<? extends Comparable> policy = globalConfig.getTableSplitPolicyMap().get(id.getClass()).floorEntry(comparableId).getValue();
            try {
                Method method = policy.getClass().getMethod("getTableIndex", comparableId.getClass(), int.class);
                return (int) method.invoke(policy, comparableId, tableCount);
            } catch (Exception e) {
                throw new SscRuntimeException(e);
            }
        }
    }
}

