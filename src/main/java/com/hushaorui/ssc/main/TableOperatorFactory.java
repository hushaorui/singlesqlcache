package com.hushaorui.ssc.main;

import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;
import com.hushaorui.ssc.common.data.DataClassDesc;
import com.hushaorui.ssc.common.data.SscSqlResult;
import com.hushaorui.ssc.common.data.SscTableInfo;
import com.hushaorui.ssc.common.em.CacheStatus;
import com.hushaorui.ssc.config.*;
import com.hushaorui.ssc.exception.SscRuntimeException;
import com.hushaorui.ssc.log.SscLog;
import com.hushaorui.ssc.util.SscStringUtils;
import javafx.util.Pair;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class TableOperatorFactory {
    private SscLog log;
    /**
     * 核心配置
     */
    private SscGlobalConfig globalConfig;
    /**
     * 数据库操作类
     */
    private JdbcTemplate jdbcTemplate;
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
    private Map<Class<?>, Map<Comparable, ObjectInstanceCache>> idCacheMap = new ConcurrentHashMap<>();
    /**
     * 针对唯一字段的数据缓存 key1:pojoClass, key2:uniqueFieldName, key3:uniqueFieldValue, value:cacheObject
     */
    private Map<Class<?>, Map<String, Map<Object, ObjectInstanceCache>>> uniqueFieldCacheMap = new ConcurrentHashMap<>();
    /**
     * 针对分表字段条件查询的数据缓存 key1:pojoClass, key2:分表字段名称, key3:字段的值, value:cacheObject
     */
    private Map<Class<?>, Map<Comparable, ObjectListCache>> tableSplitFieldCacheMap = new ConcurrentHashMap<>();
    private Map<Class<?>, Map<String, Map<Comparable, ObjectListCache>>> groupFieldCacheMap = new ConcurrentHashMap<>();

    /**
     * 存入缓存的开关
     */
    private boolean saveSwitch;
    /**
     * 取出缓存的开关
     */
    private boolean getSwitch;
    /**
     * 所有的数据类描述
     */
    private final Map<Class<?>, SscTableInfo> tableInfoMapping = new ConcurrentHashMap<>();
    /**
     * 给用户操作的缓存类
     */
    private final Map<Class<?>, Operator<?>> userOperatorMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, SpecialOperator<?>> userSpecialOperatorMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, CompletelyOperator<?>> userCompletelyOperatorMap = new ConcurrentHashMap<>();
    /**
     * 无缓存的操作类
     */
    private final Map<Class<?>, Operator<?>> noCachedOperatorMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, SpecialOperator<?>> noCachedSpecialOperatorMap = new ConcurrentHashMap<>();
    private final Map<Class<?>, CompletelyOperator<?>> noCachedCompletelyOperatorMap = new ConcurrentHashMap<>();


    private final Set<String> allTableNames = Collections.synchronizedSet(new HashSet<>());

    public TableOperatorFactory(String name, JdbcTemplate jdbcTemplate, SscGlobalConfig globalConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.globalConfig = globalConfig;
        this.log = globalConfig.getLogFactory().getLog(TableOperatorFactory.class);
        String classesDescFilePath = globalConfig.getClassesDescFilePath();
        if (classesDescFilePath != null) {
            InputStream inputStream = TableOperatorFactory.class.getClassLoader().getResourceAsStream(classesDescFilePath);
            if (inputStream != null) {
                org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
                    byte[] bytes = new byte[bufferedInputStream.available()];
                    bufferedInputStream.read(bytes);
                    String data = new String(bytes, StandardCharsets.UTF_8);
                    SscDataConfigOfYaml configOfYaml = yaml.loadAs(data, SscDataConfigOfYaml.class);
                    Map<String, SscData> classes = configOfYaml.classes;
                    classes.forEach(this::createTableInfo);
                } catch (Throwable e) {
                    if (e instanceof NoClassDefFoundError) {
                        log.warn("No class: org.yaml.snakeyaml.Yaml");
                    } else {
                        log.error(e, "读取%s文件失败", globalConfig.getClassesDescFilePath());
                    }
                }
            }
        }
        if (globalConfig.getMaxInactiveTime() > 0) {
            // 开启缓存
            this.name = TableOperatorFactory.class.getSimpleName() + "-" + name + "-Timer";
            timer = new Timer(this.name);
            // 默认 60000 (一分钟)
            long persistenceIntervalTime = globalConfig.getPersistenceIntervalTime();
            timer.schedule(timerTask = getTimerTask(), persistenceIntervalTime, persistenceIntervalTime);
            saveSwitch = true;
            getSwitch = true;
            log.debug("缓存已开启，持久化间隔：%s 毫秒，缓存最大闲置时间: %s 毫秒", persistenceIntervalTime, globalConfig.getMaxInactiveTime());
            if (globalConfig.isAddShutdownHook()) {
                // 程序关闭前执行
                Runtime.getRuntime().addShutdownHook(new Thread(this::beforeShutdown));
            }
        } else {
            saveSwitch = false;
            getSwitch = false;
        }
    }

    private void beforeShutdown() {
        log.debug("程序关闭前执行");
        close();
    }

    public TableOperatorFactory(JdbcTemplate jdbcTemplate, SscGlobalConfig globalConfig) {
        this("default", jdbcTemplate, globalConfig);
    }

    public TableOperatorFactory(String name, JdbcTemplate jdbcTemplate) {
        this(name, jdbcTemplate, new SscGlobalConfig());
    }

    public TableOperatorFactory(JdbcTemplate jdbcTemplate) {
        this("default", jdbcTemplate, new SscGlobalConfig());
    }

    private TimerTask getTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                log.debug("%s 定时任务执行开始", name);
                final long now = System.currentTimeMillis();
                try {
                    idCacheMap.forEach((objectClass, value) -> value.entrySet().removeIf(cacheEntry -> {
                        ObjectInstanceCache cache = cacheEntry.getValue();
                        if (cache.getCacheStatus().isNeedSynToDB()) {
                            Comparable tableSplitFieldValue = cache.getTableSplitFieldValue();
                            if (tableSplitFieldValue == null) {
                                // 没有特殊分表字段
                                Operator<?> noCachedOperator = getNoCachedOperator(objectClass);
                                String option = "";
                                Object dataObject = null;
                                try {
                                    synchronized (cache) {
                                        dataObject = cache.getObjectInstance();
                                        CacheStatus newStatus;
                                        switch (cache.getCacheStatus()) {
                                            case DELETE: {
                                                option = "deleteById";
                                                if (dataObject != null) {
                                                    noCachedOperator.delete(cache.getId());
                                                }
                                                newStatus = CacheStatus.AFTER_DELETE;
                                            }
                                            break;
                                            case INSERT: {
                                                option = "insert";
                                                try {
                                                    doInsert(noCachedOperator, dataObject);
                                                    newStatus = CacheStatus.AFTER_INSERT;
                                                } catch (Exception e) {
                                                    log.error(e, "缓存插入数据失败，cache: %s", globalConfig.getJsonSerializer().toJsonString(cache));
                                                    // 插入失败的数据不删除
                                                    return false;
                                                }
                                            }
                                            break;
                                            case UPDATE: {
                                                option = "update";
                                                try {
                                                    doUpdate(noCachedOperator, dataObject);
                                                    newStatus = CacheStatus.AFTER_UPDATE;
                                                } catch (Exception e) {
                                                    log.error(e, "缓存更新数据失败，cache: %s", globalConfig.getJsonSerializer().toJsonString(cache));
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
                                    log.error(e, "缓存同步进数据库失败,class:%s, object:%s, option:%s",
                                            objectClass.getName(), globalConfig.getJsonSerializer().toJsonString(dataObject), option);
                                }
                            } else {
                                // 存在特殊分表字段
                                SpecialOperator<?> specialOperator = getNoCachedSpecialOperator(objectClass);
                                String option = "";
                                Object dataObject = null;
                                try {
                                    synchronized (cache) {
                                        dataObject = cache.getObjectInstance();
                                        CacheStatus newStatus;
                                        switch (cache.getCacheStatus()) {
                                            case DELETE: {
                                                option = "deleteById";
                                                if (dataObject != null) {
                                                    Method deleteMethod = specialOperator.getClass().getMethod("delete", Object.class);
                                                    deleteMethod.invoke(specialOperator, dataObject);
                                                }
                                                newStatus = CacheStatus.AFTER_DELETE;
                                            }
                                            break;
                                            case INSERT: {
                                                option = "insert";
                                                try {
                                                    Method insertMethod = specialOperator.getClass().getMethod("insert", Object.class);
                                                    insertMethod.invoke(specialOperator, dataObject);
                                                    newStatus = CacheStatus.AFTER_INSERT;
                                                } catch (Exception e) {
                                                    log.error(e, "缓存插入数据失败，cache: %s", globalConfig.getJsonSerializer().toJsonString(cache));
                                                    // 插入失败的数据不删除
                                                    return false;
                                                }
                                            }
                                            break;
                                            case UPDATE: {
                                                option = "update";
                                                try {
                                                    Method updateMethod = specialOperator.getClass().getMethod("update", Object.class);
                                                    updateMethod.invoke(specialOperator, dataObject);
                                                    newStatus = CacheStatus.AFTER_UPDATE;
                                                } catch (Exception e) {
                                                    log.error(e, "缓存更新数据失败，cache: %s", globalConfig.getJsonSerializer().toJsonString(cache));
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
                                    log.error(e, "缓存同步进数据库失败,class:%s, object:%s, option:%s",
                                            objectClass.getName(), globalConfig.getJsonSerializer().toJsonString(dataObject), option);
                                }
                            }
                        }
                        // 清理超过最大闲置时间的缓存数据
                        boolean remove = cache.getLastUseTime() + globalConfig.getMaxInactiveTime() < now;
                        if (remove) {
                            log.debug("因长时间未使用，删除缓存, class:%s, id:%s", objectClass.getName(), cacheEntry.getKey());
                        }
                        return remove;
                    }));
                    uniqueFieldCacheMap.entrySet().removeIf(entry1 -> {
                        Map<String, Map<Object, ObjectInstanceCache>> outerMap = entry1.getValue();
                        outerMap.entrySet().removeIf(entry2 -> {
                            Map<Object, ObjectInstanceCache> innerMap = entry2.getValue();
                            innerMap.entrySet().removeIf(entry -> {
                                long lastUseTime = entry.getValue().getLastUseTime();
                                boolean delete = lastUseTime == 0 || lastUseTime + globalConfig.getMaxInactiveTime() < now;
                                if (delete) {
                                    log.debug("因长时间未使用，删除(unique)缓存, class:%s,uniqueName:%s,uniqueValue:%s", entry1.getKey().getName(), entry2.getKey(), entry.getKey());
                                }
                                return delete;
                            });
                            return innerMap.isEmpty();
                        });
                        return outerMap.isEmpty();
                    });
                    tableSplitFieldCacheMap.entrySet().removeIf(entry1 -> {
                        Map<Comparable, ObjectListCache> map = entry1.getValue();
                        map.entrySet().removeIf(entry2 -> {
                            map.entrySet().removeIf(entry -> {
                                ObjectListCache objectListCache = entry.getValue();
                                long lastUseTime = objectListCache.getLastUseTime();
                                boolean delete = lastUseTime == 0 || lastUseTime + globalConfig.getMaxInactiveTime() < now;
                                if (delete) {
                                    log.debug("因长时间未使用，删除(tableSplitField)缓存, class:%s,fieldName:%s,fieldValue:%s", entry1.getKey().getName(), entry2.getKey(), entry.getKey());
                                }
                                return delete;
                            });
                            return map.isEmpty();
                        });
                        return map.isEmpty();
                    });
                    groupFieldCacheMap.entrySet().removeIf(entry1 -> {
                        Map<String, Map<Comparable, ObjectListCache>> outerMap = entry1.getValue();
                        outerMap.entrySet().removeIf(entry2 -> {
                            Map<Comparable, ObjectListCache> innerMap = entry2.getValue();
                            innerMap.entrySet().removeIf(entry -> {
                                long lastUseTime = entry.getValue().getLastUseTime();
                                boolean delete = lastUseTime == 0 || lastUseTime + globalConfig.getMaxInactiveTime() < now;
                                if (delete) {
                                    log.debug("因长时间未使用，删除(group)缓存, class:%s,fieldName:%s,fieldValue:%s",
                                            entry1.getKey().getName(), entry2.getKey(), entry.getKey());
                                }
                                return delete;
                            });
                            return innerMap.isEmpty();
                        });
                        return outerMap.isEmpty();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.debug("%s 定时任务执行结束", name);
            }
        };
    }

    /**
     * 关闭缓存
     */
    public void close() {
        log.warn(name + "开始关闭缓存");
        // 先关闭存储缓存
        saveSwitch = false;
        if (timerTask != null) {
            // 取消任务
            timerTask.cancel();
            // 最后执行一遍任务
            timerTask.run();
        }
        if (timer != null) {
            // 关闭定时器
            timer.cancel();
        }
        // 清理所有缓存数据
        idCacheMap.clear();
        // 清理所有唯一字段值缓存
        uniqueFieldCacheMap.clear();
        // 清理分表字段值缓存
        tableSplitFieldCacheMap.clear();
        // 清理分组字段缓存
        groupFieldCacheMap.clear();
        // 最后关闭取缓存开关，保证缓存中的数据都同步到了数据库后才开始从数据库中直接拿数据
        getSwitch = false;
        log.warn(name + "缓存已关闭");
    }

    private DataClassDesc getDataClassDesc(Class<?> clazz, SscData sscData) {
        DataClassDesc dataClassDesc = DataClassDesc.transFrom(sscData);
        dataClassDesc.setDataClass(clazz);
        if (dataClassDesc.getTableName() == null || dataClassDesc.getTableName().isEmpty()) {
            // 生成表名
            dataClassDesc.setTableName(generateTableName(clazz));
        }
        if (allTableNames.contains(dataClassDesc.getTableName())) {
            // 表名重复
            throw new SscRuntimeException("Duplicate table name: " + dataClassDesc.getTableName());
        }
        // 所有的类字段名和表字段名的映射
        Map<String, String> propColumnMapping;
        if (dataClassDesc.getPropColumnMapping() == null) {
            propColumnMapping = new LinkedHashMap<>();
        } else {
            propColumnMapping = dataClassDesc.getPropColumnMapping();
        }
        // 所有的表字段名和类字段名的映射
        Map<String, String> columnPropMapping;
        if (dataClassDesc.getColumnPropMapping() == null) {
            columnPropMapping = new LinkedHashMap<>();
        } else {
            columnPropMapping = dataClassDesc.getColumnPropMapping();
        }
        // 所有的类字段名和数据库表字段类型的映射
        Map<String, String> propColumnTypeMapping;
        if (dataClassDesc.getPropColumnTypeMapping() == null) {
            propColumnTypeMapping = new HashMap<>();
        } else {
            propColumnTypeMapping = dataClassDesc.getPropColumnTypeMapping();
        }
        // 所有类字段名和它的get方法
        Map<String, Method> propGetMethods = new HashMap<>();
        // 所有的类字段名和它的set方法
        Map<String, Method> propSetMethods = new HashMap<>();
        // 所有不为空的字段集合
        Set<String> notNullProps;
        if (dataClassDesc.getNotNullProps() == null) {
            notNullProps = new HashSet<>();
        } else {
            notNullProps = dataClassDesc.getNotNullProps();
        }
        // 有默认值的字段集合
        Map<String, String> propDefaultValues;
        if (dataClassDesc.getPropDefaultValues() == null) {
            propDefaultValues = new HashMap<>();
        } else {
            propDefaultValues = dataClassDesc.getPropDefaultValues();
        }
        // 所有唯一键的字段集合
        Map<String, Set<String>> uniqueProps;
        if (dataClassDesc.getUniqueProps() == null) {
            uniqueProps = new HashMap<>();
        } else {
            uniqueProps = dataClassDesc.getUniqueProps();
        }
        // 所有不会更新的字段集合
        Set<String> notUpdateProps;
        if (dataClassDesc.getNotUpdateProps() == null) {
            notUpdateProps = new HashSet<>();
        } else {
            notUpdateProps = dataClassDesc.getNotUpdateProps();
        }
        // 所有分组字段集合
        Set<String> groupProps;
        if (dataClassDesc.getGroupProps() == null) {
            groupProps = new HashSet<>();
        } else {
            groupProps = dataClassDesc.getGroupProps();
        }
        // 所有被忽略的字段集合
        Set<String> ignoreProps;
        if (dataClassDesc.getIgnoreProps() == null) {
            ignoreProps = new HashSet<>();
        } else {
            ignoreProps = dataClassDesc.getIgnoreProps();
        }

        // id字段的名称
        String idPropName = dataClassDesc.getIdPropName();
        // 使用使用id生成策略
        /*Boolean idIsAuto;
        if (dataClassDesc.isUseIdGeneratePolicy()) {
            idIsAuto = true;
        }*/
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
                if (ignoreProps.contains(propName)) {
                    continue;
                }
                // 表字段名
                String columnName;
                if (!propColumnMapping.containsKey(propName)) {
                    // 配置中没有字段名，生成一个
                    columnName = generateColumnName(propName);
                } else {
                    columnName = propColumnMapping.get(propName);
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
                if (!propColumnTypeMapping.containsKey(propName)) {
                    columnType = getColumnTypeByJavaType(fieldType);
                } else {
                    columnType = propColumnTypeMapping.get(propName);
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
            throw new SscRuntimeException("No data in the table: " + dataClassDesc.getTableName());
        }
        if (idPropName == null) {
            // 没有标注id，则第一个字段为id
            idPropName = propColumnMapping.keySet().iterator().next();
            // 默认使用id生成策略
            //idIsAuto = true;
        }
        dataClassDesc.setColumnPropMapping(columnPropMapping);
        dataClassDesc.setPropColumnMapping(propColumnMapping);
        dataClassDesc.setPropColumnTypeMapping(propColumnTypeMapping);
        dataClassDesc.setPropGetMethods(propGetMethods);
        dataClassDesc.setPropSetMethods(propSetMethods);
        dataClassDesc.setIdPropName(idPropName);
        //dataClassDesc.setUseIdGeneratePolicy(idIsAuto);
        dataClassDesc.setNotNullProps(notNullProps);
        dataClassDesc.setPropDefaultValues(propDefaultValues);
        dataClassDesc.setUniqueProps(uniqueProps);
        // id是不进行更新的
        notUpdateProps.add(idPropName);
        dataClassDesc.setNotUpdateProps(notUpdateProps);
        dataClassDesc.setIgnoreProps(ignoreProps);
        dataClassDesc.setGroupProps(groupProps);
        return dataClassDesc;
    }

    private DataClassDesc getDataClassDesc(Class<?> clazz, Boolean haveTableSplitField) {
        DataClassDesc dataClassDesc = new DataClassDesc();
        dataClassDesc.setDataClass(clazz);
        // 表的数量
        int tableCount;
        // 分表字段
        String tableSplitField;
        // 是否启用缓存
        boolean cached;
        DataClass annotation = clazz.getDeclaredAnnotation(DataClass.class);
        // 表名
        String tableName;
        // 使用使用id生成策略
        boolean idIsAuto;
        if (annotation == null) {
            // 默认不分表
            tableCount = 1;
            tableName = generateTableName(clazz);
            // 默认启用缓存
            cached = true;
            // 默认id使用自动生成策略
            idIsAuto = true;
            // 默认使用id作为分表字段，这里不需指定
            tableSplitField = null;
        } else {
            tableCount = annotation.tableCount();
            cached = annotation.cached();
            idIsAuto = annotation.isAuto();
            String value = annotation.value();
            if (value.length() == 0) {
                tableName = generateTableName(clazz);
            } else {
                tableName = value;
            }
            tableSplitField = annotation.tableSplitField().length() > 0 ? annotation.tableSplitField() : null;
        }
        if (haveTableSplitField != null) {
            if (haveTableSplitField && tableSplitField == null) {
                throw new SscRuntimeException(String.format("class:%s 未指定分表字段，请使用getOperator方法获取操作对象", clazz.getName()));
            } else if (! haveTableSplitField && tableSplitField != null) {
                throw new SscRuntimeException(String.format("class:%s 已指定分表字段: %s，请使用getSpecialOperator方法获取操作对象", clazz.getName(), tableSplitField));
            }
        }
        if (allTableNames.contains(tableName)) {
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
        // 所有被忽略的字段集合
        Set<String> ignoreProps = new HashSet<>();
        // 所有分组字段
        Set<String> groupProps = new HashSet<>();

        // id字段的名称
        String idPropName = null;
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
                    if (fieldDesc.ignore()) {
                        // 该字段不参与数据库操作
                        ignoreProps.add(propName);
                        continue;
                    }
                    if (fieldDesc.isId()) {
                        if (idPropName != null) {
                            throw new SscRuntimeException(String.format("ID field can only have one in class: %s", clazz.getName()));
                        }
                        idPropName = propName;
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
                    if (fieldDesc.isGroup()) {
                        groupProps.add(propName);
                    }
                    if (fieldDesc.columnType().length() == 0) {
                        columnType = getColumnTypeByJavaType(fieldType);
                    } else {
                        columnType = fieldDesc.columnType();
                    }
                } else {
                    // 没有 FieldDesc 注解，默认也当作表字段解析
                    columnType = getColumnTypeByJavaType(fieldType);
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
        }
        dataClassDesc.setTableCount(tableCount);
        if (tableSplitField != null) {
            // 如果注解中填的是表字段名，也不会报错，这里还原为类字段名
            String fieldName = columnPropMapping.getOrDefault(tableSplitField, tableSplitField);
            if (! propColumnMapping.containsKey(fieldName)) {
                throw new SscRuntimeException("未知的数据库分表字段名：" + fieldName);
            }
            // 分表字段是不能更新的
            notUpdateProps.add(fieldName);
            // 分表字段的值也不能为null
            notNullProps.add(fieldName);
            dataClassDesc.setTableSplitField(fieldName);
        }
        dataClassDesc.setTableName(tableName);
        dataClassDesc.setCached(cached);
        dataClassDesc.setColumnPropMapping(columnPropMapping.isEmpty() ? Collections.emptyMap() : columnPropMapping);
        dataClassDesc.setPropColumnMapping(propColumnMapping.isEmpty() ? Collections.emptyMap() : propColumnMapping);
        dataClassDesc.setPropColumnTypeMapping(propColumnTypeMapping.isEmpty() ? Collections.emptyMap() : propColumnTypeMapping);
        dataClassDesc.setPropGetMethods(propGetMethods.isEmpty() ? Collections.emptyMap() : propGetMethods);
        dataClassDesc.setPropSetMethods(propSetMethods.isEmpty() ? Collections.emptyMap() : propSetMethods);
        dataClassDesc.setIdPropName(idPropName);
        dataClassDesc.setUseIdGeneratePolicy(idIsAuto);
        dataClassDesc.setNotNullProps(notNullProps.isEmpty() ? Collections.emptySet() : notNullProps);
        dataClassDesc.setPropDefaultValues(propDefaultValues.isEmpty() ? Collections.emptyMap() : propDefaultValues);
        dataClassDesc.setUniqueProps(uniqueProps.isEmpty() ? Collections.emptyMap() : uniqueProps);
        // id是不进行更新的
        notUpdateProps.add(idPropName);
        dataClassDesc.setNotUpdateProps(notUpdateProps.isEmpty() ? Collections.emptySet() : notUpdateProps);
        dataClassDesc.setIgnoreProps(ignoreProps.isEmpty() ? Collections.emptySet() : ignoreProps);
        dataClassDesc.setGroupProps(groupProps.isEmpty() ? Collections.emptySet() : groupProps);
        return dataClassDesc;
    }

    private SscTableInfo getTableInfoNotNull(Class<?> dataClass, Boolean haveTableSplitField) {
        return tableInfoMapping.computeIfAbsent(dataClass, clazz -> {
            DataClassDesc dataClassDesc = getDataClassDesc(clazz, haveTableSplitField);
            return newTableInfo(clazz, dataClassDesc, globalConfig);
        });
    }

    private String getColumnTypeByJavaType(Class<?> javaType) {
        // 枚举默认是字符串类型的
        return globalConfig.getJavaTypeToTableType().getOrDefault(javaType, javaType.isEnum() ? "VARCHAR" : globalConfig.getDefaultTableType());
    }

    private void createTableInfo(String className, SscData sscData) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error(e, "创建类描述对象失败：%s", className);
            return;
        }
        DataClassDesc dataClassDesc = getDataClassDesc(clazz, sscData);
        tableInfoMapping.put(clazz, newTableInfo(clazz, dataClassDesc, globalConfig));
    }

    private SscTableInfo newTableInfo(Class<?> clazz, DataClassDesc dataClassDesc, SscGlobalConfig globalConfig) {
        Class<? extends SscTableInfo> tableInfoClass = globalConfig.getTableInfoClass();
        try {
            Constructor<? extends SscTableInfo> constructor = tableInfoClass.getConstructor(DataClassDesc.class, SscGlobalConfig.class);
            SscTableInfo sscTableInfo = constructor.newInstance(dataClassDesc, globalConfig);
            allTableNames.add(dataClassDesc.getTableName());
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
            }
            //System.out.println(globalConfig.getJsonSerializer().toJsonString(sscTableInfo, SerializerFeature.PrettyFormat));
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
                log.debug("默认id生成器:%s, 初始化起始id:%s", idGeneratePolicy.getClass().getName(), maxId);
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
                    log.debug("默认id生成器:%s, 初始化起始id:%s", idGeneratePolicy.getClass().getName(), maxId);
                }
            }
            log.debug("class: %s 数据类注册成功", clazz.getName());
            return sscTableInfo;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e, "创建TableInfo失败");
            return null;
        }
    }

    /**
     * 提前注册所有的数据类
     */
    public void registerDataClass(Class<?> dataClass) {
        getTableInfoNotNull(dataClass, null);
    }

    private void dropTable(String[] dropTableSql) {
        for (String sql : dropTableSql) {
            try {
                log.debug("删除表, sql: " + sql);
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                if (! e.getMessage().contains("does not exist")) {
                    log.error(e, "删除表失败");
                }
            }
        }
    }

    private void createTable(String[] createTableSql) {
        for (String sql : createTableSql) {
            try {
                log.debug("尝试创建表, sql: " + sql);
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                if (! e.getMessage().contains("already exist")) {
                    log.error(e, "尝试创建表失败");
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
        SscTableInfo sscTableInfo = getTableInfoNotNull(dataClass, false);
        if (sscTableInfo == null) {
            throw new SscRuntimeException("The class is unregistered: " + dataClass.getName());
        }
        return getNoCachedOperator(dataClass, sscTableInfo);
    }

    private <T> SpecialOperator<T> getNoCachedSpecialOperator(Class<T> dataClass) {
        SscTableInfo sscTableInfo = getTableInfoNotNull(dataClass, true);
        if (sscTableInfo == null) {
            throw new SscRuntimeException("The class is unregistered: " + dataClass.getName());
        }
        return getNoCachedSpecialOperator(dataClass, sscTableInfo);
    }

    private <T> SpecialOperator<T> getNoCachedSpecialOperator(Class<T> dataClass, SscTableInfo sscTableInfo) {
        return (SpecialOperator<T>) noCachedSpecialOperatorMap.computeIfAbsent(dataClass, clazz -> new SpecialOperator<Object>() {
            private CompletelyOperator<Object> completelyOperator = getNoCachedCompletelyOperator((Class<Object>) dataClass, sscTableInfo);
            @Override
            public Object selectById(Comparable id, Comparable tableSplitFieldValue) {
                return completelyOperator.selectById(id, tableSplitFieldValue);
            }

            @Override
            public Object selectByIdNotNull(Comparable id, Comparable tableSplitFieldValue, Function<Comparable, Object> insertFunction) {
                return completelyOperator.selectByIdNotNull(id, tableSplitFieldValue, insertFunction);
            }

            @Override
            public Object selectByUniqueName(String uniqueName, Comparable tableSplitFieldValue, Object o) {
                return completelyOperator.selectByUniqueName(uniqueName, tableSplitFieldValue, o);
            }

            @Override
            public List<Object> selectByTableSplitField(Comparable tableSplitFieldValue) {
                return completelyOperator.selectByTableSplitField(tableSplitFieldValue);
            }

            @Override
            public <T> List<T> selectAllId(Comparable tableSplitFieldValue) {
                return completelyOperator.selectAllId(tableSplitFieldValue);
            }

            @Override
            public List<Object> selectByGroupField(Comparable tableSplitFieldValue, String fieldName, Comparable fieldValue) {
                return completelyOperator.selectByGroupField(tableSplitFieldValue, fieldName, fieldValue);
            }

            @Override
            public void insert(Object o) {
                completelyOperator.insert(o);
            }

            @Override
            public void update(Object o) {
                completelyOperator.update(o);
            }

            @Override
            public void delete(Object o) {
                completelyOperator.delete(o);
            }

            @Override
            public List<Object> selectByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.selectByCondition(conditions);
            }

            @Override
            public <T> List<T> selectIdByCondition(List<Pair<String, Object>> conditions) {
                return (List<T>) completelyOperator.selectIdByCondition(conditions);
            }

            @Override
            public int countByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.countByCondition(conditions);
            }

            @Override
            public JdbcTemplate getJdbcTemplate() {
                return completelyOperator.getJdbcTemplate();
            }
        });
    }
    private <T> Operator<T> getNoCachedOperator(Class<T> dataClass, SscTableInfo sscTableInfo) {
        return (Operator<T>) noCachedOperatorMap.computeIfAbsent(dataClass, clazz -> new Operator<Object>() {
            private CompletelyOperator<Object> completelyOperator = getNoCachedCompletelyOperator((Class<Object>) dataClass, sscTableInfo);
            @Override
            public void delete(Comparable id) {
                completelyOperator.delete(id);
            }

            @Override
            public Object selectById(Comparable id) {
                return completelyOperator.selectById(id);
            }

            @Override
            public Object selectByIdNotNull(Comparable id, Function<Comparable, Object> insertFunction) {
                return completelyOperator.selectByIdNotNull(id, insertFunction);
            }

            @Override
            public Object selectByUniqueName(String uniqueName, Object o) {
                return completelyOperator.selectByUniqueName(uniqueName, o);
            }

            @Override
            public List<Object> selectByGroupField(String fieldName, Comparable fieldValue) {
                return completelyOperator.selectByGroupField(fieldName, fieldValue);
            }

            @Override
            public void insert(Object o) {
                completelyOperator.insert(o);
            }

            @Override
            public void update(Object o) {
                completelyOperator.update(o);
            }

            @Override
            public void delete(Object o) {
                completelyOperator.delete(o);
            }

            @Override
            public List<Object> selectByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.selectByCondition(conditions);
            }

            @Override
            public <T> List<T> selectIdByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.selectIdByCondition(conditions);
            }

            @Override
            public int countByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.countByCondition(conditions);
            }

            @Override
            public JdbcTemplate getJdbcTemplate() {
                return completelyOperator.getJdbcTemplate();
            }
        });
    }

    private Object[] getConditionParams(DataClassDesc classDesc, Set<String> propNames, Object data) {
        int size = propNames.size();
        int tableCount = classDesc.getTableCount();
        Object[] params = new Object[size * tableCount];
        int index = 0;
        for (String propName : propNames) {
            params[index++] = getFieldValueFromObject(classDesc, propName, data);
        }
        int leftParamSize = (tableCount - 1) * size;
        for (int i = 0; i < leftParamSize; i++) {
            params[index] = params[index % size];
            index++;
        }
        return params;
    }

    /**
     * 将id设置到对象中去
     */
    private void setObjectId(Object object, Object id, DataClassDesc classDesc) {
        Method idSetMethod = classDesc.getPropSetMethods().get(classDesc.getIdPropName());
        try {
            // 将新的id设置到对象中
            idSetMethod.invoke(object, id);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SscRuntimeException(e);
        }
    }
    private <T> CompletelyOperator<T> getNoCachedCompletelyOperator(Class<T> dataClass, SscTableInfo sscTableInfo) {
        return (CompletelyOperator<T>) noCachedCompletelyOperatorMap.computeIfAbsent(dataClass, clazz -> newNoCachedCompletelyOperator(clazz, sscTableInfo));
    }

    private <T> CompletelyOperator<T> newNoCachedCompletelyOperator(Class<T> clazz, SscTableInfo sscTableInfo) {
        return (CompletelyOperator<T>) new CompletelyOperator<Object>() {
            private DataClassDesc classDesc = sscTableInfo.getClassDesc();
            private RowMapper<Object> rowMapper;

            @Override
            public void insert(Object object) {
                Object id = getIdValueFromObject(classDesc, object);
                if (id == null) {
                    if (classDesc.isUseIdGeneratePolicy()) {
                        IdGeneratePolicy<?> idGeneratePolicy = globalConfig.getIdGeneratePolicyMap().get(sscTableInfo.getIdJavaType());
                        // 根据id生成策略获取新的id
                        id = idGeneratePolicy.getId(clazz);
                        setObjectId(object, id, classDesc);
                    }
                }
                assert id != null : new SscRuntimeException("插入数据失败，id为null且不使用id生成策略, class: " + clazz.getName());
                int tableIndex = getTableIndex(classDesc, object, id);
                String insertSql = sscTableInfo.getInsertSql()[tableIndex];
                Object[] params = getInsertParamArrayFromObject(classDesc, object);
                log.debug("执行插入语句：%s, id:%s", insertSql, id);
                jdbcTemplate.update(insertSql, params);
            }

            @Override
            public Object selectByIdNotNull(Comparable id, Function<Comparable, Object> insertFunction) {
                Object object = selectById(id);
                if (object != null) {
                    return object;
                }
                return insertAndReturn(id, insertFunction);
            }

            @Override
            public Object selectByIdNotNull(Comparable id, Comparable tableSplitFieldValue, Function<Comparable, Object> insertFunction) {
                Object object = selectById(id, tableSplitFieldValue);
                if (object != null) {
                    return object;
                }
                return insertAndReturn(id, insertFunction);
            }

            private Object insertAndReturn(Comparable id, Function<Comparable, Object> insertFunction) {
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
                Object id = getIdValueFromObject(classDesc, object);
                int tableIndex = getTableIndex(classDesc, object, id);
                String updateSql = sscTableInfo.getUpdateAllNotCachedByIdSql()[tableIndex];
                Object[] params = getUpdateParamArrayFromObject(classDesc, object);
                log.debug("执行更新语句：%s, id:%s", updateSql, id);
                jdbcTemplate.update(updateSql, params);
            }

            @Override
            public void delete(Object object) {
                Comparable id = getIdValueFromObject(classDesc, object);
                assert id != null : new SscRuntimeException("删除数据时，id不可为null, class:" + clazz.getName());
                int tableIndex = getTableIndex(classDesc, object, id);
                delete(id, tableIndex);
            }

            @Override
            public void delete(Comparable id) {
                int tableIndex;
                if (clazz.equals(id.getClass())) {
                    Comparable realId = getIdValueFromObject(classDesc, id);
                    tableIndex = getTableIndex(classDesc, id, realId);
                } else {
                    tableIndex = getTableIndex(classDesc, null, id);
                }
                delete(id, tableIndex);
            }

            private void delete(Comparable id, int tableIndex) {
                String deleteSql = sscTableInfo.getDeleteByIdSql()[tableIndex];
                log.debug("执行删除语句：%s, id:%s", deleteSql, id);
                jdbcTemplate.update(deleteSql, id);
            }

            private RowMapper<Object> getRowMapper() {
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
                                        if (byte[].class.equals(propType)) {
                                            methodName = "getBytes";
                                        } else if (Integer.class.equals(propType) || int.class.equals(propType)) {
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
                                    return data;
                                } catch (Exception e) {
                                    throw new SscRuntimeException(e);
                                }
                            };
                        }
                    }
                }
                return rowMapper;
            }

            @Override
            public Object selectById(Comparable id) {
                int tableIndex = getTableIndex(classDesc, null, id);
                String sql = sscTableInfo.getSelectByIdSql()[tableIndex];
                try {
                    log.debug("执行查询语句(selectById): %s, id:%s", sql, id);
                    return jdbcTemplate.queryForObject(sql, getRowMapper(), id);
                } catch (EmptyResultDataAccessException ignore) {
                    // queryForObject 在查不到数据时会抛出此异常
                    return null;
                }
            }

            @Override
            public Object selectById(Comparable id, Comparable tableSplitFieldValue) {
                int tableIndex = getTableIndex(tableSplitFieldValue, classDesc.getTableCount());
                String sql = sscTableInfo.getSelectByIdSql()[tableIndex];
                try {
                    log.debug("执行查询语句(selectById): %s, id:%s, tableSplitFieldValue:%s", sql, id, tableSplitFieldValue);
                    return jdbcTemplate.queryForObject(sql, getRowMapper(), id);
                } catch (EmptyResultDataAccessException ignore) {
                    // queryForObject 在查不到数据时会抛出此异常
                    return null;
                }
            }

            @Override
            public Object selectByUniqueName(String uniqueName, Object data) {
                Set<String> propNames = classDesc.getUniqueProps().get(uniqueName);
                if (propNames == null) {
                    throw new SscRuntimeException("There is no uniqueName in class: " + clazz.getName());
                }
                String sql = sscTableInfo.getUniqueSelectSqlMap().get(uniqueName);
                Object[] params = getConditionParams(classDesc, propNames, data);
                try {
                    log.debug("执行查询语句(selectByUniqueName): %s, uniqueName:%s", sql, uniqueName);
                    return jdbcTemplate.queryForObject(sql, getRowMapper(), params);
                } catch (EmptyResultDataAccessException ignore) {
                    // queryForObject 在查到数据数量是0的时候会抛出此异常
                    return null;
                }
            }

            @Override
            public Object selectByUniqueName(String uniqueName, Comparable tableSplitFieldValue, Object data) {
                Set<String> propNames = classDesc.getUniqueProps().get(uniqueName);
                if (propNames == null) {
                    throw new SscRuntimeException("There is no uniqueName in class: " + clazz.getName());
                }
                int tableIndex = getTableIndex(tableSplitFieldValue, classDesc.getTableCount());
                String sql = sscTableInfo.getUniqueAndFieldSelectSqlMap().get(uniqueName)[tableIndex];
                Object[] params = getConditionParams(classDesc, propNames, data);
                try {
                    log.debug("执行查询语句(selectByUniqueName): %s, uniqueName:%s,tableSplitFieldValue:%s", sql, uniqueName, tableSplitFieldValue);
                    return jdbcTemplate.queryForObject(sql, getRowMapper(), params);
                } catch (EmptyResultDataAccessException ignore) {
                    // queryForObject 在查到数据数量是0的时候会抛出此异常
                    return null;
                }
            }

            @Override
            public List<Object> selectByCondition(List<Pair<String, Object>> conditions) {
                String key = sscTableInfo.getKeyByConditionList(conditions);
                SscSqlResult sscSqlResult = sscTableInfo.getNoCachedSelectConditionSql(key, conditions);
                Object[] params = sscSqlResult.getParams().toArray();
                //System.out.println(JSONArray.toJSONString(sscSqlResult, SerializerFeature.DisableCircularReferenceDetect));
                log.debug("执行查询语句(selectByCondition): %s", sscSqlResult.getSql());
                return jdbcTemplate.query(sscSqlResult.getSql(), getRowMapper(), params);
            }

            @Override
            public int countByCondition(List<Pair<String, Object>> conditions) {
                String key = sscTableInfo.getKeyByConditionList(conditions);
                SscSqlResult sscSqlResult = sscTableInfo.getNoCachedCountConditionSql(key, conditions);
                Object[] params = sscSqlResult.getParams().toArray();
                //System.out.println(JSONArray.toJSONString(sscSqlResult, SerializerFeature.DisableCircularReferenceDetect));
                log.debug("执行查询语句(countByCondition): %s", sscSqlResult.getSql());
                List<Integer> integers = jdbcTemplate.queryForList(sscSqlResult.getSql(), int.class, params);
                if (integers == null) {
                    return 0;
                }
                int sum = 0;
                for (Integer num : integers) {
                    sum += num;
                }
                return sum;
            }

            @Override
            public <T> List<T> selectIdByCondition(List<Pair<String, Object>> conditions) {
                String key = sscTableInfo.getKeyByConditionList(conditions);
                SscSqlResult sscSqlResult = sscTableInfo.getNoCachedSelectIdConditionSql(key, conditions);
                Object[] params = sscSqlResult.getParams().toArray();
                //System.out.println(JSONArray.toJSONString(sscSqlResult, SerializerFeature.DisableCircularReferenceDetect));
                log.debug("执行查询语句(selectIdByCondition)：%s", sscSqlResult.getSql());
                return (List<T>) jdbcTemplate.queryForList(sscSqlResult.getSql(), (Class<Object>) sscTableInfo.getIdJavaType(), params);
            }

            @Override
            public List<Object> selectByTableSplitField(Comparable tableSplitFieldValue) {
                // 根据分表字段获取对应的表的索引
                int tableIndex = getTableIndex(tableSplitFieldValue, classDesc.getTableCount());
                // 获取对应的sql
                String sql = sscTableInfo.getSelectByTableSplitFieldSql()[tableIndex];
                assert sql != null : new SscRuntimeException("未能找到对应表索引的sql, index: " + tableIndex);
                // 执行sql
                log.debug("执行查询语句(selectByTableSplitField)：%s, tableSplitFieldValue:%s", sql, tableSplitFieldValue);
                return jdbcTemplate.query(sql, getRowMapper(), tableSplitFieldValue);
            }

            @Override
            public List<Object> selectByGroupField(String fieldName, Comparable fieldValue) {
                String propName = classDesc.getColumnPropMapping().getOrDefault(fieldName, fieldName);
                String sql = sscTableInfo.getUnionSelectByGroupFieldSql().get(propName);
                assert sql != null : new SscRuntimeException(String.format("class:%s 未知的分组字段名称：%s", classDesc.getDataClass().getName(), fieldName));
                // 执行sql
                log.debug("执行查询语句(selectByGroupField)：%s, fieldName:%s, fieldValue:%s", sql, fieldName, fieldValue);
                return jdbcTemplate.query(sql, getRowMapper(), fieldValue);
            }

            @Override
            public List<Object> selectByGroupField(Comparable tableSplitFieldValue, String fieldName, Comparable fieldValue) {
                String propName = classDesc.getColumnPropMapping().getOrDefault(fieldName, fieldName);
                if (propName.equals(classDesc.getTableSplitField())) {
                    return selectByTableSplitField(tableSplitFieldValue);
                }
                String[] sqlArray = sscTableInfo.getSelectByGroupFieldSql().get(propName);
                assert sqlArray != null : new SscRuntimeException("未能找到该字段对应的sql，fieldName: " + fieldName);
                // 根据分表字段获取对应的表的索引
                int tableIndex = getTableIndex(tableSplitFieldValue, classDesc.getTableCount());
                String sql = sqlArray[tableIndex];
                // 执行sql
                log.debug("执行查询语句(selectByGroupField)：%s, tableSplitFieldValue:%s, fieldName:%s, fieldValue:%s",
                        sql, tableSplitFieldValue, fieldName, fieldValue);
                return jdbcTemplate.query(sql, getRowMapper(), fieldValue);
            }

            @Override
            public JdbcTemplate getJdbcTemplate() {
                return jdbcTemplate;
            }

            @Override
            public <T> List<T> selectAllId(Comparable tableSplitFieldValue) {
                // 根据分表字段获取对应的表的索引
                int tableIndex = getTableIndex(tableSplitFieldValue, classDesc.getTableCount());
                String sql = sscTableInfo.getSelectIdByTableSplitFieldSql()[tableIndex];
                assert sql != null : new SscRuntimeException("未能找到对应表索引的sql, index: " + tableIndex);
                // 执行sql
                log.debug("执行查询语句(selectAllId)：%s, tableSplitFieldValue:%s", sql, tableSplitFieldValue);
                return (List<T>) jdbcTemplate.queryForList(sql, (Class<Object>) sscTableInfo.getIdJavaType(), tableSplitFieldValue);
            }
        };
    }

    private <T> CompletelyOperator<T> getCompletelyOperator(Class<T> dataClass, SscTableInfo sscTableInfo) {
        return (CompletelyOperator<T>) userCompletelyOperatorMap.computeIfAbsent(dataClass, clazz -> newCompletelyOperator(clazz, sscTableInfo));
    }
    private <T> CompletelyOperator<T> newCompletelyOperator(Class<T> dataClass, SscTableInfo sscTableInfo) {
        return new CompletelyOperator<T>() {
            private final DataClassDesc classDesc = sscTableInfo.getClassDesc();

            @Override
            public void insert(T object) {
                if (!saveSwitch) {
                    // 存储缓存已关闭，直接插入数据库
                    getNoCachedCompletelyOperator(dataClass, sscTableInfo).insert(object);
                    return;
                }
                insertOrUpdate(getIdValueFromObject(classDesc, object), object);
            }

            private void insertOrUpdate(Comparable id, T object) {
                if (object == null) {
                    log.warn("尝试添加为null的缓存，添加失败");
                    return;
                }
                Class<?> objectClass = object.getClass();
                if (id == null) {
                    if (classDesc.isUseIdGeneratePolicy()) {
                        IdGeneratePolicy<?> idGeneratePolicy = globalConfig.getIdGeneratePolicyMap().get(sscTableInfo.getIdJavaType());
                        // 根据id生成策略获取新的id
                        id = (Comparable) idGeneratePolicy.getId(dataClass);
                        setObjectId(object, id, classDesc);
                    } else {
                        id = getIdValueFromObject(classDesc, object);
                    }
                }
                insertOrUpDateOrDelete(objectClass, id, object, CacheStatus.INSERT);
            }

            private void insertOrUpDateOrDelete(Class<?> objectClass, Comparable id, T object, CacheStatus newStatus) {
                insertOrUpDateOrDelete(objectClass, id, getFieldValueFromObject(classDesc, classDesc.getTableSplitField(), object), object, newStatus);
            }
            private void insertOrUpDateOrDelete(Class<?> objectClass, Comparable id, Comparable tableSplitFieldValue, T object, CacheStatus newStatus) {
                /*if (CacheStatus.DELETE.equals(newStatus)) {
                    // 需要删除该数据
                    Map<Comparable, ObjectInstanceCache> classMap = idCacheMap.get(objectClass);
                    if (classMap != null) {
                        ObjectInstanceCache cache = classMap.get(id);
                        if (cache != null) {
                            synchronized (cache) {
                                cache.setCacheStatus(newStatus);
                                log.debug("缓存预备删除数据, class:%s, id: %s, object: %s", objectClass.getName(), id, object);
                            }
                            // 尝试添加分表字段缓存
                            addTableSplitFieldCache(classDesc, id, tableSplitFieldValue, true);
                        }
                    }
                    return;
                }*/
                Map<Comparable, ObjectInstanceCache> classMap = idCacheMap.computeIfAbsent(objectClass, key -> new ConcurrentHashMap<>());
                ObjectInstanceCache cache = classMap.computeIfAbsent(id, key -> {
                    ObjectInstanceCache c = new ObjectInstanceCache();
                    c.setId(id);
                    c.setObjectClass(objectClass);
                    c.setTableSplitFieldValue(tableSplitFieldValue);
                    if (object != null) {
                        // 注意，只有在创建缓存对象的时候或者缓存中的数据对象为null(查询不存在的数据)时才会设置
                        c.setObjectInstance(object);
                        if (c.getLastModifyTime() == 0) {
                            // == 0的时候为该cache对象第一次创建的时候，可能需要判断唯一键
                            // 唯一键的缓存
                            // 尝试获取数据对象中的唯一字段(或联合字段)的值
                            addUniqueValueCache(classDesc, object, c);
                            // 尝试添加分表字段缓存
                            addTableSplitFieldCache(classDesc, id, tableSplitFieldValue, newStatus.isDelete());
                            // 尝试添加分组字段缓存
                            addGroupFieldCache(classDesc, id, object, c, newStatus.isDelete());
                        }
                    }
                    // 当前状态
                    return c;
                });
                synchronized (cache) {
                    if (cache.getObjectInstance() == null) {
                        cache.setObjectInstance(object);
                    }
                    cache.setCacheStatus(CacheStatus.getResultStatus(cache.getCacheStatus(), newStatus));
                }
                long now = System.currentTimeMillis();
                cache.setLastModifyTime(now);
                cache.setLastUseTime(now);
                log.debug("%s缓存, class:%s, id: %s", cache.getCacheStatus(), objectClass.getName(), id);
            }

            @Override
            public void update(T object) {
                if (object == null) {
                    log.warn("尝试更新为null的缓存，更新失败");
                    return;
                }
                Comparable id = getIdValueFromObject(classDesc, object);
                insertOrUpDateOrDelete(dataClass, id, object, CacheStatus.UPDATE);
            }

            @Override
            public void delete(T object) {
                Comparable id = getIdValueFromObject(classDesc, object);
                insertOrUpDateOrDelete(dataClass, id, object, CacheStatus.DELETE);
            }

            @Override
            public void delete(Comparable id) {
                insertOrUpDateOrDelete(dataClass, id, null, CacheStatus.DELETE);
            }

            @Override
            public T selectByIdNotNull(Comparable id, Function<Comparable, T> insertFunction) {
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.debug("缓存已关闭，直接查询数据库, class:%s, id:%s", dataClass.getName(), id);
                    Operator<?> noCachedOperator = getNoCachedCompletelyOperator(dataClass, sscTableInfo);
                    Object value = noCachedOperator.selectById(id);
                    if (value == null) {
                        synchronized (SscStringUtils.getLock(id, dataClass)) {
                            value = noCachedOperator.selectById(id);
                            if (value == null && insertFunction != null) {
                                value = insertFunction.apply(id);
                                if (value != null) {
                                    doInsert(noCachedOperator, value);
                                }
                            }
                        }
                    }
                    return (T) value;
                }
                Map<Comparable, ObjectInstanceCache> classMap = idCacheMap.computeIfAbsent(dataClass, key -> new ConcurrentHashMap<>());
                ObjectInstanceCache cache = classMap.get(String.valueOf(id));
                if (cache == null) {
                    log.debug("未找到缓存(selectById)，直接查询数据库, class:%s, id:%s", dataClass.getName(), id);
                    // 缓存击穿，查询数据库
                    Operator<?> noCachedOperator = getNoCachedCompletelyOperator(dataClass, sscTableInfo);
                    T value = (T) noCachedOperator.selectById(id);
                    if (value == null) {
                        synchronized (SscStringUtils.getLock(id, dataClass)) {
                            value = (T) noCachedOperator.selectById(id);
                            if (value == null && insertFunction != null) {
                                value = insertFunction.apply(id);
                                if (value == null) {
                                    insertOrUpDateOrDelete(dataClass, id, null, CacheStatus.AFTER_DELETE);
                                } else {
                                    insertOrUpDateOrDelete(dataClass, id, value, CacheStatus.INSERT);
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
                                T newInstance = insertFunction.apply(id);
                                cache.setObjectInstance(newInstance);
                                // 预备删除，转为更新
                                cache.setCacheStatus(CacheStatus.UPDATE);
                                return newInstance;
                            } else if (CacheStatus.AFTER_DELETE.equals(cache.getCacheStatus())) {
                                // 已经删除了，这里需要插入
                                T newInstance = insertFunction.apply(id);
                                cache.setObjectInstance(newInstance);
                                cache.setCacheStatus(CacheStatus.INSERT);
                                return newInstance;
                            }
                        }
                    }
                    T objectInstance = (T) cache.getObjectInstance();
                    if (objectInstance == null && insertFunction != null) {
                        // 这里可能永远不会运行
                        synchronized (cache) {
                            objectInstance = (T) cache.getObjectInstance();
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
            public T selectByIdNotNull(Comparable id, Comparable tableSplitFieldValue, Function<Comparable, T> insertFunction) {
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.debug("缓存已关闭，直接查询数据库, class:%s, id:%s", dataClass.getName(), id);
                    CompletelyOperator<?> noCachedOperator = getNoCachedCompletelyOperator(dataClass, sscTableInfo);
                    Object value = noCachedOperator.selectById(id, tableSplitFieldValue);
                    if (value == null) {
                        synchronized (SscStringUtils.getLock(id, dataClass)) {
                            value = noCachedOperator.selectById(id, tableSplitFieldValue);
                            if (value == null && insertFunction != null) {
                                value = insertFunction.apply(id);
                                if (value != null) {
                                    doInsert(noCachedOperator, value);
                                }
                            }
                        }
                    }
                    return (T) value;
                }
                Map<Comparable, ObjectInstanceCache> classMap = idCacheMap.computeIfAbsent(dataClass, key -> new ConcurrentHashMap<>());
                ObjectInstanceCache cache = classMap.get(id);
                if (cache == null) {
                    log.debug("未找到缓存(selectById)，直接查询数据库, class:%s, id:%s", dataClass.getName(), id);
                    // 缓存击穿，查询数据库
                    CompletelyOperator<T> noCachedOperator = getNoCachedCompletelyOperator(dataClass, sscTableInfo);
                    T value = noCachedOperator.selectById(id, tableSplitFieldValue);
                    if (value == null) {
                        synchronized (SscStringUtils.getLock(id, dataClass)) {
                            value = noCachedOperator.selectById(id, tableSplitFieldValue);
                            if (value == null && insertFunction != null) {
                                value = insertFunction.apply(id);
                                if (value == null) {
                                    insertOrUpDateOrDelete(dataClass, id, tableSplitFieldValue, null, CacheStatus.AFTER_DELETE);
                                } else {
                                    insertOrUpDateOrDelete(dataClass, id, tableSplitFieldValue, value, CacheStatus.INSERT);
                                }
                            }
                        }
                    }
                    return value;
                } else {
                    // 更新最后一次使用时间
                    cache.setLastUseTime(System.currentTimeMillis());
                    if (cache.getCacheStatus().isDelete()) {
                        if (insertFunction == null) {
                            return null;
                        }
                        synchronized (cache) {
                            // 该数据原本需要删除
                            if (CacheStatus.DELETE.equals(cache.getCacheStatus())) {
                                T newInstance = insertFunction.apply(id);
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
                    return (T) objectInstance;
                }
            }

            @Override
            public T selectById(Comparable id) {
                return selectByIdNotNull(id, null);
            }

            @Override
            public T selectById(Comparable id, Comparable tableSplitFieldValue) {
                return selectByIdNotNull(id, tableSplitFieldValue, null);
            }

            @Override
            public T selectByUniqueName(String uniqueName, Object data) {
                Set<String> propNames = classDesc.getUniqueProps().get(uniqueName);
                if (propNames == null) {
                    throw new SscRuntimeException("There is no uniqueName in class: " + dataClass.getName());
                }
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.debug("缓存已关闭，直接查询数据库, class:%s, uniqueName:%s, value:%s", dataClass.getName(), uniqueName, data);
                    return (T) doSelectByUniqueName(getNoCachedCompletelyOperator(dataClass, sscTableInfo), uniqueName, data);
                }

                Object uniqueValue = getUniqueValueByUniqueName(classDesc, propNames, data);
                if (uniqueValue == null) {
                    log.error("未能获取到唯一键的值, class:%s, uniqueName:%s, value:%s", dataClass.getName(), uniqueName, data);
                    return null;
                }
                Map<Object, ObjectInstanceCache> cacheMap = uniqueFieldCacheMap.computeIfAbsent(dataClass, key -> new ConcurrentHashMap<>()).computeIfAbsent(uniqueName, key -> new ConcurrentHashMap<>());
                ObjectInstanceCache cache = cacheMap.get(uniqueValue);
                if (cache == null) {
                    log.debug("未找到缓存(selectByUniqueName)，直接查询数据库, class:%s, uniqueName:%s, key:%s", dataClass.getName(), uniqueName, data);
                    // 缓存击穿，查询数据库
                    T value = (T) doSelectByUniqueName(getNoCachedCompletelyOperator(dataClass, sscTableInfo), uniqueName, data);
                    // 如果没有id，无法放入缓存
                    if (value == null) {
                        // 防止击穿
                        addUniqueFieldCacheObject(dataClass, uniqueName, uniqueValue, CacheStatus.AFTER_DELETE, null);
                    } else {
                        Comparable id = getIdValueFromObject(classDesc, value);
                        if (id != null) {
                            // 添加id缓存(同时也会添加唯一键缓存)
                            insertOrUpDateOrDelete(dataClass, id, value, CacheStatus.AFTER_INSERT);
                        } else {
                            // 没有id(一般不可能存在这种情况)
                            addUniqueFieldCacheObject(dataClass, uniqueName, uniqueValue, CacheStatus.AFTER_INSERT, value);
                        }
                    }
                    return value;
                } else {
                    // 更新最后一次使用时间
                    cache.setLastUseTime(System.currentTimeMillis());
                    if (cache.getCacheStatus().isDelete()) {
                        // 该对象需要删除或已被删除
                        return null;
                    }
                    Object objectInstance = cache.getObjectInstance();
                    if (objectInstance == null) {
                        return null;
                    } else {
                        // 再次计算唯一值，如果不相匹配，则该唯一键的值经过了修改，相当于被删除了
                        Object uniqueValueCheck = getUniqueValueByUniqueName(classDesc, propNames, objectInstance);
                        if (! uniqueValue.equals(uniqueValueCheck)) {
                            return null;
                        }
                    }
                    return (T) objectInstance;
                }
            }

            @Override
            public T selectByUniqueName(String uniqueName, Comparable tableSplitFieldValue, Object data) {
                Set<String> propNames = classDesc.getUniqueProps().get(uniqueName);
                if (propNames == null) {
                    throw new SscRuntimeException("There is no uniqueName in class: " + dataClass.getName());
                }
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.debug("缓存已关闭，直接查询数据库, class:%s, uniqueName:%s, value:%s", dataClass.getName(), uniqueName, data);
                    return (T) doSelectByUniqueName(getNoCachedCompletelyOperator(dataClass, sscTableInfo), uniqueName, tableSplitFieldValue, data);
                }

                Object uniqueValue = getUniqueValueByUniqueName(classDesc, propNames, data);
                if (uniqueValue == null) {
                    log.error("未能获取到唯一键的值, class:%s, uniqueName:%s, value:%s", dataClass.getName(), uniqueName, data);
                    return null;
                }
                Map<Object, ObjectInstanceCache> cacheMap = uniqueFieldCacheMap.computeIfAbsent(dataClass, key -> new ConcurrentHashMap<>()).computeIfAbsent(uniqueName, key -> new ConcurrentHashMap<>());
                ObjectInstanceCache cache = cacheMap.get(uniqueValue);
                if (cache == null) {
                    log.debug("未找到缓存(selectByUniqueName)，直接查询数据库, class:%s, uniqueName:%s, key:%s", dataClass.getName(), uniqueName, data);
                    // 缓存击穿，查询数据库
                    T value = (T) doSelectByUniqueName(getNoCachedCompletelyOperator(dataClass, sscTableInfo), uniqueName, data);
                    // 如果没有id，无法放入缓存
                    if (value == null) {
                        // 防止击穿
                        addUniqueFieldCacheObject(dataClass, uniqueName, uniqueValue, CacheStatus.AFTER_DELETE, null);
                    } else {
                        Comparable id = getIdValueFromObject(classDesc, value);
                        if (id != null) {
                            // 添加id缓存(同时也会添加唯一键缓存)
                            insertOrUpDateOrDelete(dataClass, id, tableSplitFieldValue, value, CacheStatus.AFTER_INSERT);
                        } else {
                            // 没有id(一般不可能存在这种情况)
                            addUniqueFieldCacheObject(dataClass, uniqueName, uniqueValue, CacheStatus.AFTER_INSERT, value);
                        }
                    }
                    return value;
                } else {
                    // 更新最后一次使用时间
                    cache.setLastUseTime(System.currentTimeMillis());
                    if (cache.getCacheStatus().isDelete()) {
                        // 该对象需要删除或已被删除
                        return null;
                    }
                    Object objectInstance = cache.getObjectInstance();
                    if (objectInstance == null) {
                        return null;
                    } else {
                        // 再次计算唯一值，如果不相匹配，则该唯一键的值经过了修改，相当于被删除了
                        Object uniqueValueCheck = getUniqueValueByUniqueName(classDesc, propNames, objectInstance);
                        if (! uniqueValue.equals(uniqueValueCheck)) {
                            return null;
                        }
                    }
                    return (T) objectInstance;
                }
            }

            @Override
            public List<T> selectByCondition(List<Pair<String, Object>> conditions) {
                return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByCondition(conditions);
            }

            @Override
            public int countByCondition(List<Pair<String, Object>> conditions) {
                return getNoCachedCompletelyOperator(dataClass, sscTableInfo).countByCondition(conditions);
            }

            @Override
            public <A> List<A> selectIdByCondition(List<Pair<String, Object>> conditions) {
                return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectIdByCondition(conditions);
            }

            private List<Comparable> getIdListFromDbByGroupField(Comparable tableSplitFieldValue, String fieldName, Comparable fieldValue) {
                ArrayList<Comparable> idList = new ArrayList<>();
                List<T> dataList;
                if (tableSplitFieldValue == null) {
                    dataList = getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByGroupField(fieldName, fieldValue);
                } else {
                    dataList = getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByGroupField(tableSplitFieldValue, fieldName, fieldValue);
                }
                if (dataList != null) {
                    for (T data : dataList) {
                        Comparable idValue = getIdValueFromObject(classDesc, data);
                        if (idValue == null) {
                            continue;
                        }
                        idList.add(idValue);
                        // 添加id缓存
                        insertOrUpDateOrDelete(dataClass, idValue, tableSplitFieldValue, data, CacheStatus.AFTER_INSERT);
                    }
                }
                return idList;
            }

            private List<Comparable> getIdListFromDb(Comparable tableSplitFieldValue) {
                ArrayList<Comparable> idList = new ArrayList<>();
                List<T> dataList = getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByTableSplitField(tableSplitFieldValue);
                if (dataList != null) {
                    for (T data : dataList) {
                        Comparable idValue = getIdValueFromObject(classDesc, data);
                        if (idValue == null) {
                            continue;
                        }
                        idList.add(idValue);
                        // 添加id缓存
                        insertOrUpDateOrDelete(dataClass, idValue, tableSplitFieldValue, data, CacheStatus.AFTER_INSERT);
                    }
                }
                return idList;
            }

            @Override
            public <T1> List<T1> selectAllId(Comparable tableSplitFieldValue) {
                if (! getSwitch) {
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectAllId(tableSplitFieldValue);
                }
                Map<Comparable, ObjectListCache> cacheMap = tableSplitFieldCacheMap.computeIfAbsent(dataClass, k1 -> new ConcurrentHashMap<>());
                ObjectListCache cache = getCacheFromMap(cacheMap, tableSplitFieldValue);
                if (cache == null) {
                    log.debug("未找到缓存(selectAllId)，直接查询数据库, class:%s, tableSplitFieldValue:%s", dataClass.getName(), tableSplitFieldValue);
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectAllId(tableSplitFieldValue);
                }
                cache.setLastUseTime(System.currentTimeMillis());
                return (List<T1>) cache.getIdList();
            }

            private ObjectListCache getCacheFromMapByGroupField(Map<Comparable, ObjectListCache> cacheMap, Comparable tableSplitFieldValue,
                                                                String fieldName, Comparable fieldValue) {
                ObjectListCache cache = cacheMap.get(fieldValue);
                List<Comparable> idListFromDb;
                if (cache == null) {
                    // 这里调用一下这个方法，该方法会创建新的 ObjectListCache
                    idListFromDb = getIdListFromDbByGroupField(tableSplitFieldValue, fieldName, fieldValue);
                    cache = cacheMap.get(fieldValue);
                    if (cache == null) {
                        return null;
                    }
                } else {
                    idListFromDb = null;
                }
                if (CacheStatus.AFTER_INSERT.equals(cache.getCacheStatus())) {
                    // 缓存对象是新创建的，需要把从数据库中查到的id加进来
                    cache.setCacheStatus(CacheStatus.AFTER_UPDATE);
                    List<Comparable> idList = cache.getIdList();
                    if (idListFromDb == null) {
                        idListFromDb = getIdListFromDbByGroupField(tableSplitFieldValue, fieldName, fieldValue);
                    }
                    idListFromDb.forEach(idInList -> {
                        if (! idList.contains(idInList)) {
                            idList.add(idInList);
                        }
                    });
                }
                return cache;
            }

            private ObjectListCache getCacheFromMap(Map<Comparable, ObjectListCache> cacheMap, Comparable tableSplitFieldValue) {
                ObjectListCache cache = cacheMap.get(tableSplitFieldValue);
                List<Comparable> idListFromDb;
                if (cache == null) {
                    // 这里调用一下这个方法，该方法会创建新的 ObjectListCache
                    idListFromDb = getIdListFromDb(tableSplitFieldValue);
                    cache = cacheMap.get(tableSplitFieldValue);
                    if (cache == null) {
                        return null;
                    }
                } else {
                    idListFromDb = null;
                }
                if (CacheStatus.AFTER_INSERT.equals(cache.getCacheStatus())) {
                    // 缓存对象是新创建的，需要把从数据库中查到的id加进来
                    cache.setCacheStatus(CacheStatus.AFTER_UPDATE);
                    List<Comparable> idList = cache.getIdList();
                    if (idListFromDb == null) {
                        idListFromDb = getIdListFromDb(tableSplitFieldValue);
                    }
                    idListFromDb.forEach(idInList -> {
                        if (! idList.contains(idInList)) {
                            idList.add(idInList);
                        }
                    });
                }
                return cache;
            }

            @Override
            public List<T> selectByTableSplitField(Comparable tableSplitFieldValue) {
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.debug("缓存已关闭，直接查询数据库, class:%s, tableSplitFieldValue:%s", dataClass.getName(), tableSplitFieldValue);
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByTableSplitField(tableSplitFieldValue);
                }
                Map<Comparable, ObjectListCache> cacheMap = tableSplitFieldCacheMap.computeIfAbsent(dataClass, k1 -> new ConcurrentHashMap<>());
                ObjectListCache cache = getCacheFromMap(cacheMap, tableSplitFieldValue);
                if (cache == null) {
                    log.debug("未找到缓存(selectByTableSplitField)，直接查询数据库, class:%s, tableSplitFieldValue:%s", dataClass.getName(), tableSplitFieldValue);
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByTableSplitField(tableSplitFieldValue);
                }
                cache.setLastUseTime(System.currentTimeMillis());
                List<Comparable> idList = cache.getIdList();
                List<T> dataList = new ArrayList<>(idList.size());
                for (Comparable idValue : idList) {
                    // 根据id去查询对应的数据
                    T data = selectById(idValue, tableSplitFieldValue);
                    if (data == null) {
                        continue;
                    }
                    dataList.add(data);
                }
                return dataList;
            }


            @Override
            public List<T> selectByGroupField(String fieldName, Comparable fieldValue) {
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.debug("缓存已关闭，直接查询数据库, class:%s, fieldName:%s, fieldValue:%s",
                            dataClass.getName(), fieldName, fieldValue);
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByGroupField(fieldName, fieldValue);
                }
                String propName = classDesc.getColumnPropMapping().getOrDefault(fieldName, fieldName);
                Map<Comparable, ObjectListCache> cacheMap = groupFieldCacheMap.computeIfAbsent(dataClass, k1 -> new ConcurrentHashMap<>())
                        .computeIfAbsent(propName, k2 -> new ConcurrentHashMap<>());
                ObjectListCache cache = getCacheFromMapByGroupField(cacheMap, null, fieldName, fieldValue);
                if (cache == null) {
                    log.debug("未找到缓存(selectByGroupField)，直接查询数据库, class:%s, tableSplitFieldValue:%s, fieldName:%s, fieldValue:%s",
                            dataClass.getName(), fieldName, fieldValue);
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByGroupField(fieldName, fieldValue);
                }
                cache.setLastUseTime(System.currentTimeMillis());
                List<Comparable> idList = cache.getIdList();
                List<T> dataList = new ArrayList<>(idList.size());
                for (Comparable idValue : idList) {
                    // 根据id去查询对应的数据
                    T data = selectById(idValue);
                    if (data == null) {
                        continue;
                    }
                    dataList.add(data);
                }
                return dataList;
            }

            @Override
            public List<T> selectByGroupField(Comparable tableSplitFieldValue, String fieldName, Comparable fieldValue) {
                if (!getSwitch) {
                    // 缓存已关闭，直接调用数据库查询
                    log.debug("缓存已关闭，直接查询数据库, class:%s, tableSplitFieldValue:%s, fieldName:%s, fieldValue:%s",
                            dataClass.getName(), tableSplitFieldValue, fieldName, fieldValue);
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByGroupField(tableSplitFieldValue, fieldName, fieldValue);
                }
                String propName = classDesc.getColumnPropMapping().getOrDefault(fieldName, fieldName);
                Map<Comparable, ObjectListCache> cacheMap = groupFieldCacheMap.computeIfAbsent(dataClass, k1 -> new ConcurrentHashMap<>())
                        .computeIfAbsent(propName, k2 -> new ConcurrentHashMap<>());
                ObjectListCache cache = getCacheFromMapByGroupField(cacheMap, tableSplitFieldValue, fieldName, fieldValue);
                if (cache == null) {
                    log.debug("未找到缓存(selectByGroupField)，直接查询数据库, class:%s, tableSplitFieldValue:%s, fieldName:%s, fieldValue:%s",
                            dataClass.getName(), tableSplitFieldValue, fieldName, fieldValue);
                    return getNoCachedCompletelyOperator(dataClass, sscTableInfo).selectByGroupField(tableSplitFieldValue, fieldName, fieldValue);
                }
                cache.setLastUseTime(System.currentTimeMillis());
                List<Comparable> idList = cache.getIdList();
                List<T> dataList = new ArrayList<>(idList.size());
                for (Comparable idValue : idList) {
                    // 根据id去查询对应的数据
                    T data = selectById(idValue, tableSplitFieldValue);
                    if (data == null) {
                        continue;
                    }
                    dataList.add(data);
                }
                return dataList;
            }

            @Override
            public JdbcTemplate getJdbcTemplate() {
                return jdbcTemplate;
            }
        };

    }

    public <T> Operator<T> getOperator(Class<T> dataClass) {
        if (globalConfig.getMaxInactiveTime() <= 0) {
            return getNoCachedOperator(dataClass);
        }
        SscTableInfo sscTableInfo = getTableInfoNotNull(dataClass, false);
        if (sscTableInfo == null) {
            throw new SscRuntimeException("The class is unregistered: " + dataClass.getName());
        }
        return sscTableInfo.getClassDesc().isCached() ? (Operator<T>) userOperatorMap.computeIfAbsent(dataClass, clazz -> new Operator<Object>() {
            private CompletelyOperator<Object> completelyOperator = getCompletelyOperator((Class<Object>) dataClass, sscTableInfo);
            @Override
            public void delete(Comparable id) {
                completelyOperator.delete(id);
            }

            @Override
            public Object selectById(Comparable id) {
                return completelyOperator.selectById(id);
            }

            @Override
            public Object selectByIdNotNull(Comparable id, Function<Comparable, Object> insertFunction) {
                return completelyOperator.selectByIdNotNull(id, insertFunction);
            }

            @Override
            public Object selectByUniqueName(String uniqueName, Object o) {
                return completelyOperator.selectByUniqueName(uniqueName, o);
            }

            @Override
            public List<Object> selectByGroupField(String fieldName, Comparable fieldValue) {
                return completelyOperator.selectByGroupField(fieldName, fieldValue);
            }

            @Override
            public void insert(Object o) {
                completelyOperator.insert(o);
            }

            @Override
            public void update(Object o) {
                completelyOperator.update(o);
            }

            @Override
            public void delete(Object o) {
                completelyOperator.delete(o);
            }

            @Override
            public List<Object> selectByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.selectByCondition(conditions);
            }

            @Override
            public <T> List<T> selectIdByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.selectIdByCondition(conditions);
            }

            @Override
            public int countByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.countByCondition(conditions);
            }

            @Override
            public JdbcTemplate getJdbcTemplate() {
                return completelyOperator.getJdbcTemplate();
            }
        }) : getNoCachedOperator(dataClass, sscTableInfo);
    }

    /**
     * 当分表字段自定义时，使用此方法
     * @param dataClass 数据类
     * @return 操作类
     */
    public <T> SpecialOperator<T> getSpecialOperator(Class<T> dataClass) {
        if (globalConfig.getMaxInactiveTime() <= 0) {
            return getNoCachedSpecialOperator(dataClass);
        }
        SscTableInfo sscTableInfo = getTableInfoNotNull(dataClass, true);
        if (sscTableInfo == null) {
            throw new SscRuntimeException("The class is unregistered: " + dataClass.getName());
        }
        return sscTableInfo.getClassDesc().isCached() ? (SpecialOperator<T>) userSpecialOperatorMap.computeIfAbsent(dataClass, clazz -> new SpecialOperator<Object>() {
            private CompletelyOperator<Object> completelyOperator = getCompletelyOperator((Class<Object>) dataClass, sscTableInfo);

            @Override
            public Object selectById(Comparable id, Comparable tableSplitFieldValue) {
                return completelyOperator.selectById(id, tableSplitFieldValue);
            }

            @Override
            public Object selectByIdNotNull(Comparable id, Comparable tableSplitFieldValue, Function<Comparable, Object> insertFunction) {
                return completelyOperator.selectByIdNotNull(id, tableSplitFieldValue, insertFunction);
            }

            @Override
            public Object selectByUniqueName(String uniqueName, Comparable tableSplitFieldValue, Object o) {
                return completelyOperator.selectByUniqueName(uniqueName, tableSplitFieldValue, o);
            }

            @Override
            public List<Object> selectByTableSplitField(Comparable tableSplitFieldValue) {
                return completelyOperator.selectByTableSplitField(tableSplitFieldValue);
            }

            @Override
            public void insert(Object o) {
                completelyOperator.insert(o);
            }

            @Override
            public void update(Object o) {
                completelyOperator.update(o);
            }

            @Override
            public void delete(Object o) {
                completelyOperator.delete(o);
            }

            @Override
            public List<Object> selectByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.selectByCondition(conditions);
            }

            @Override
            public <T> List<T> selectIdByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.selectIdByCondition(conditions);
            }

            @Override
            public int countByCondition(List<Pair<String, Object>> conditions) {
                return completelyOperator.countByCondition(conditions);
            }

            @Override
            public <T> List<T> selectAllId(Comparable tableSplitFieldValue) {
                return completelyOperator.selectAllId(tableSplitFieldValue);
            }

            @Override
            public List<Object> selectByGroupField(Comparable tableSplitFieldValue, String fieldName, Comparable fieldValue) {
                return completelyOperator.selectByGroupField(tableSplitFieldValue, fieldName, fieldValue);
            }

            @Override
            public JdbcTemplate getJdbcTemplate() {
                return completelyOperator.getJdbcTemplate();
            }
        }) : getNoCachedSpecialOperator(dataClass, sscTableInfo);
    }

    private void addGroupFieldCache(DataClassDesc classDesc, Comparable id, Object object, ObjectInstanceCache cache, boolean delete) {
        Class<?> dataClass = classDesc.getDataClass();
        Map<String, String> columnPropMapping = classDesc.getColumnPropMapping();
        for (String groupPropName : classDesc.getGroupProps()) {
            String propName = columnPropMapping.getOrDefault(groupPropName, groupPropName);
            Map<Comparable, ObjectListCache> cacheMap = groupFieldCacheMap.computeIfAbsent(dataClass, k1 -> new ConcurrentHashMap<>())
                    .computeIfAbsent(propName, k2 -> new ConcurrentHashMap<>());
            // 从对象中新获取的分组字段的值
            Comparable newFieldValue = getFieldValueFromObjectOrDefault(classDesc, propName, object, NullObject.INSTANCE);
            if (delete) {
                // 本次为删除操作
                ObjectListCache objectListCache = cacheMap.get(newFieldValue);
                // 如果缓存不存在，就不操作
                if (objectListCache != null) {
                    objectListCache.getIdList().remove(id);
                }
            } else {
                cacheMap.computeIfAbsent(newFieldValue, k2 -> {
                    ObjectListCache objectListCache = new ObjectListCache();
                    objectListCache.setIdList(Collections.synchronizedList(new ArrayList<>()));
                    // 这里仅仅作为标记，并不代表缓存的状态
                    objectListCache.setCacheStatus(CacheStatus.AFTER_INSERT);
                    return objectListCache;
                }).getIdList().add(id);
            }
            // 该缓存中旧有分组字段的值
            Comparable oldFieldValue = cache.getGroupFieldValue(propName);
            // 新旧两个对象是否相等
            boolean equals = SscStringUtils.objectEquals(oldFieldValue, newFieldValue);
            // 分组字段发生了改变，旧的缓存中也将该id删除
            if (! equals) {
                // 重新设置分组字段的值
                cache.setGroupFieldValue(propName, newFieldValue);
                ObjectListCache objectListCache = cacheMap.get(oldFieldValue);
                // 如果缓存不存在，就不操作
                if (objectListCache != null) {
                    objectListCache.getIdList().remove(id);
                }
            }
        }
    }

    private void addTableSplitFieldCache(DataClassDesc classDesc, Comparable id, Comparable tableSplitFieldValue, boolean delete) {
        String tableSplitField = classDesc.getTableSplitField();
        if (tableSplitField == null) {
            return;
        }
        if (tableSplitFieldValue == null) {
            throw new SscRuntimeException(String.format("添加分表字段缓存时失败，分表字段 %s 为null", tableSplitField));
        }
        Class<?> dataClass = classDesc.getDataClass();
        Map<Comparable, ObjectListCache> cacheMap = tableSplitFieldCacheMap.computeIfAbsent(dataClass, k1 -> new ConcurrentHashMap<>());
        if (delete) {
            // 本次为删除操作
            ObjectListCache objectListCache = cacheMap.get(tableSplitFieldValue);
            // 如果缓存不存在，就不操作
            if (objectListCache != null) {
                objectListCache.getIdList().remove(id);
            }
        } else {
            cacheMap.computeIfAbsent(tableSplitFieldValue, k2 -> {
                ObjectListCache objectListCache = new ObjectListCache();
                objectListCache.setIdList(Collections.synchronizedList(new ArrayList<>()));
                // 这里仅仅作为标记，并不代表缓存的状态
                objectListCache.setCacheStatus(CacheStatus.AFTER_INSERT);
                return objectListCache;
            }).getIdList().add(id);
        }
    }

    /**
     * 获取数据对象的所有唯一对象的值
     *
     * @param classDesc 数据描述对象
     * @param object    数据对象
     */
    private void addUniqueValueCache(DataClassDesc classDesc, Object object, ObjectInstanceCache cache) {
        if (object == null) {
            return;
        }
        Map<String, Set<String>> uniqueProps = classDesc.getUniqueProps();
        if (uniqueProps.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Set<String>> entry : uniqueProps.entrySet()) {
            try {
                Object uniqueValue = getUniqueValueByUniqueName(classDesc, entry.getValue(), object);
                Map<Object, ObjectInstanceCache> uniqueMap = uniqueFieldCacheMap.computeIfAbsent(classDesc.getDataClass(), key2 -> new ConcurrentHashMap<>())
                        .computeIfAbsent(entry.getKey(), key -> new ConcurrentHashMap<>());
                // 放入唯一字段，这里覆盖
                uniqueMap.put(uniqueValue, cache);
            } catch (Exception e) {
                log.error("获取数据的唯一字段值失败(getUniqueValue), classDesc:%s", globalConfig.getJsonSerializer().toJsonString(classDesc));
            }
        }
    }

    /**
     * 在根据唯一键查找时从缓存和数据库中均未找到，则将创建一个缓存对象，放入集合，防止击穿
     *
     * @param objectClass 数据类的class
     * @param uniqueName  唯一键的名称
     * @param uniqueValue 唯一键计算后的值
     */
    private ObjectInstanceCache addUniqueFieldCacheObject(Class<?> objectClass, String uniqueName, Object uniqueValue, CacheStatus cacheStatus, Object data) {
        Map<Object, ObjectInstanceCache> uniqueMap = uniqueFieldCacheMap.computeIfAbsent(objectClass, key2 -> new ConcurrentHashMap<>())
                .computeIfAbsent(uniqueName, key -> new ConcurrentHashMap<>());
        ObjectInstanceCache cache = new ObjectInstanceCache();
        cache.setCacheStatus(cacheStatus);
        cache.setObjectInstance(data);
        long now = System.currentTimeMillis();
        cache.setLastUseTime(now);
        return uniqueMap.putIfAbsent(uniqueValue, cache);
    }

    /*private Object getUniqueValueByMap(List<Pair<String, Object>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        JSONSerializer jsonSerializer = globalConfig.getJsonSerializer();
        int size = conditions.size();
        if (size == 1) {
            return SscStringUtils.md5Encode(jsonSerializer.toJsonString(conditions.get(0).getValue()));
        } else {
            // 联合字段
            StringBuilder builder1 = new StringBuilder();
            StringBuilder builder2 = new StringBuilder();
            int halfSize = size / 2;
            int i = 0;
            for (Pair<String, Object> pair : conditions) {
                if (i <= halfSize) {
                    builder1.append(jsonSerializer.toJsonString(pair.getValue()));
                } else {
                    builder2.append(jsonSerializer.toJsonString(pair.getValue()));
                }
                i++;
            }
            return getStringFromBuilder(builder1, builder2);
        }
    }*/

    /*private String getNoCachedConditionKey(DataClassDesc classDesc, SscCondition... conditions) {
        if (conditions == null || conditions.length == 0) {
            return "";
        }
        StringBuilder key = new StringBuilder();
        boolean notFirst = false;
        for (SscCondition condition : conditions) {
            HashMap<String, Object> fieldMap = condition.getFieldMap();
            if (fieldMap == null || fieldMap.isEmpty()) {
                // 没有设置条件，该对象无意义
                continue;
            }
            if (notFirst) {
                // 拼接条件
                key.append(" ").append(condition.getType().getSign());
            }
            for (String propOrColumn : fieldMap.keySet()) {
                if (notFirst) {
                    key.append(" ");
                } else {
                    notFirst = true;
                }
                key.append(classDesc.getColumnByProp(propOrColumn));
            }
            List<SscCondition> nextList = condition.getNextList();
            if (nextList != null) {
                for (SscCondition next : nextList) {
                    HashMap<String, Object> map = next.getFieldMap();
                    Class<?> joinClass = next.getJoinClass();
                    DataClassDesc desc;
                    if (joinClass == null) {
                        desc = classDesc;
                    } else {
                        SscTableInfo sscTableInfo = getTableInfoNotNull(joinClass);
                        desc = sscTableInfo.getClassDesc();
                        // 最好是拼接这种不能当作字段的字符串，以免和字段拼接时和字段混淆
                        key.append(" class ").append(desc.getTableName());
                    }
                    if (map != null) {
                        for (String propOrColumn : map.keySet()) {
                            key.append(" ").append(desc.getColumnByProp(propOrColumn));
                        }
                    }
                }
            }
        }
        return key.toString().intern();
    }*/

    private Object getUniqueValueByUniqueName(DataClassDesc classDesc, Set<String> propNames, Object data) {
        int size = propNames.size();
        JSONSerializer jsonSerializer = globalConfig.getJsonSerializer();
        if (size == 1) {
            return SscStringUtils.md5Encode(jsonSerializer.toJsonString(getFieldValueFromObject(classDesc, propNames.iterator().next(), data)));
        } else {
            // 联合字段
            StringBuilder builder1 = new StringBuilder();
            StringBuilder builder2 = new StringBuilder();
            int halfSize = size / 2;
            int i = 0;
            for (String propName : propNames) {
                Object fieldValue = getFieldValueFromObject(classDesc, propName, data);
                if (i <= halfSize) {
                    builder1.append(jsonSerializer.toJsonString(fieldValue));
                } else {
                    builder2.append(jsonSerializer.toJsonString(fieldValue));
                }
                i++;
            }
            return getStringFromBuilder(builder1, builder2);
        }
    }

    private String getStringFromBuilder(StringBuilder builder1, StringBuilder builder2) {
        // 将长度控制在2个md5, 一个太容易碰撞
        return String.format("%s;%s", SscStringUtils.md5Encode(builder1.toString()), SscStringUtils.md5Encode(builder2.toString()));
    }

    /*private int doCountByCondition(Operator<?> operator, List<Pair<String, Object>> conditions) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Method insertMethod = operatorClass.getMethod("countByCondition", List.class);
            return (int) insertMethod.invoke(operator, conditions);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("countByCondition error! operator class: %s, value: %s", operator.getClass().getName(), globalConfig.getJsonSerializer().toJsonString(conditions)), e);
        }
    }*/

    /*private List<Object> doSelectCondition(Operator<?> operator, List<Pair<String, Object>> conditions) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Method insertMethod = operatorClass.getMethod("selectByCondition", List.class);
            return (List<Object>) insertMethod.invoke(operator, conditions);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("selectByCondition error! operator class: %s, value: %s", operator.getClass().getName(), globalConfig.getJsonSerializer().toJsonString(conditions)), e);
        }
    }*/

    /*private <T> List<T> doSelectByIdCondition(Operator<?> operator, List<Pair<String, Object>> conditions) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Method insertMethod = operatorClass.getMethod("selectIdByCondition", List.class);
            return (List<T>) insertMethod.invoke(operator, conditions);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("selectIdByCondition error! operator class: %s, value: %s", operator.getClass().getName(), globalConfig.getJsonSerializer().toJsonString(conditions)), e);
        }
    }*/

    private Object doSelectByUniqueName(Operator<?> operator, String uniqueName, Comparable tableSplitFieldValue, Object data) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Method insertMethod = operatorClass.getMethod("selectByUniqueName", String.class, Comparable.class, Object.class);
            return insertMethod.invoke(operator, uniqueName, tableSplitFieldValue, data);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("SelectByUniqueName error! operator class: %s, value: %s", operator.getClass().getName(), data), e);
        }
    }

    private Object doSelectByUniqueName(Operator<?> operator, String uniqueName, Object data) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Method insertMethod = operatorClass.getMethod("selectByUniqueName", String.class, Object.class);
            return insertMethod.invoke(operator, uniqueName, data);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("SelectByUniqueName error! operator class: %s, value: %s", operator.getClass().getName(), data), e);
        }
    }

    private void doUpdate(Operator<?> operator, Object value) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Method insertMethod = operatorClass.getMethod("update", Object.class);
            insertMethod.invoke(operator, value);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("Update error! operator class: %s, value: %s", operator.getClass().getName(), value), e);
        }
    }

    private void doInsert(Operator<?> operator, Object value) {
        try {
            Class<? extends Operator> operatorClass = operator.getClass();
            Method insertMethod = operatorClass.getMethod("insert", Object.class);
            insertMethod.invoke(operator, value);
        } catch (Exception e) {
            throw new SscRuntimeException(String.format("Insert error! operator class: %s, value: %s", operator.getClass().getName(), value), e);
        }
    }

    /**
     * 从数据对象中获取指定字段名称的字段的值
     *
     * @param classDesc 类的描述封装对象
     * @param propName  类属性名称
     * @param object    数据对象
     * @return 属性的值
     */
    private <T> T getFieldValueFromObjectOrDefault(DataClassDesc classDesc, String propName, Object object, T defaultValue) {
        Object value = getFieldValueFromObject(classDesc, propName, object);
        return value == null ? defaultValue : (T) value;
    }

    /**
     * 从数据对象中获取指定字段名称的字段的值
     *
     * @param classDesc 类的描述封装对象
     * @param propName  类属性名称
     * @param object    数据对象
     * @return 属性的值
     */
    private <T> T getFieldValueFromObject(DataClassDesc classDesc, String propName, Object object) {
        if (object == null || propName == null) {
            return null;
        }
        Method getMethod = classDesc.getPropGetMethods().get(propName);
        try {
            return (T) getMethod.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SscRuntimeException(String.format("Get %s value error in class: %s", propName, classDesc.getDataClass().getName()), e);
        }
    }

    private Comparable getIdValueFromObject(DataClassDesc classDesc, Object object) {
        Method idGetMethod = classDesc.getPropGetMethods().get(classDesc.getIdPropName());
        try {
            return (Comparable) idGetMethod.invoke(object);
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
    private int getTableIndex(DataClassDesc classDesc, Object object, Object id) {
        int tableCount = classDesc.getTableCount();
        if (tableCount == 1) {
            // 不分表
            return 0;
        } else {
            Object tableSplitFieldValue;
            {
                String tableSplitField = classDesc.getTableSplitField();
                if (tableSplitField == null) {
                    tableSplitFieldValue = id;
                } else {
                    tableSplitFieldValue = getFieldValueFromObject(classDesc, tableSplitField, object);
                    assert  tableSplitFieldValue != null : new SscRuntimeException(String.format("插入数据失败，分表字段：%s 值为null, class: %s", tableSplitField, object.getClass().getName()));
                }
            }
            return getTableIndex((Comparable) tableSplitFieldValue, tableCount);
        }
    }

    private int getTableIndex(Comparable comparableId, int tableCount) {
        if (tableCount == 1) {
            return 0;
        }
        // 根据id获取表的索引
        TableSplitPolicy<? extends Comparable> policy = globalConfig.getTableSplitPolicyMap().get(comparableId.getClass()).floorEntry(comparableId).getValue();
        assert policy != null : new SscRuntimeException(String.format("字段类型:%s, 字段值:%s，未能找到对应的分表策略", comparableId.getClass(), comparableId));
        try {
            Method method = policy.getClass().getMethod("getTableIndex", comparableId.getClass(), int.class);
            return (int) method.invoke(policy, comparableId, tableCount);
        } catch (Exception e) {
            throw new SscRuntimeException(e);
        }
    }

    public void outputClassDescYamlData(String filePath) throws IOException {
        SscDataConfigOfYaml configOfYaml = new SscDataConfigOfYaml();
        Map<String, SscData> classes = new HashMap<>();
        configOfYaml.classes = classes;
        for (SscTableInfo tableInfo : tableInfoMapping.values()) {
            DataClassDesc classDesc = tableInfo.getClassDesc();
            classes.put(classDesc.getDataClass().getName(), SscData.transFrom(classDesc));
        }
        if (filePath != null) {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            String dump = new org.yaml.snakeyaml.Yaml().dumpAsMap(configOfYaml);
            //String dump = new org.yaml.snakeyaml.Yaml().dumpAll(classes.entrySet().iterator());
            //String dump = new org.yaml.snakeyaml.Yaml().dumpAs(configOfYaml, Tag.MAP, null);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(dump.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }
    }
}

