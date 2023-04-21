package com.hushaorui.ssc.common.data;

import java.lang.reflect.Method;
import java.util.Map;

public class CommonClassDesc {
    /** 镜像 */
    private Class<?> dataClass;
    /** 所有类字段名和它的get方法 */
    private Map<String, Method> propGetMethods;
    /** 所有的类字段名和它的set方法 */
    private Map<String, Method> propSetMethods;
    /** 字段的第一个泛型 */
    private Map<String, Class<?>> genericTypes;

    public Class<?> getDataClass() {
        return dataClass;
    }

    public void setDataClass(Class<?> dataClass) {
        this.dataClass = dataClass;
    }

    public Map<String, Method> getPropGetMethods() {
        return propGetMethods;
    }

    public void setPropGetMethods(Map<String, Method> propGetMethods) {
        this.propGetMethods = propGetMethods;
    }

    public Map<String, Method> getPropSetMethods() {
        return propSetMethods;
    }

    public void setPropSetMethods(Map<String, Method> propSetMethods) {
        this.propSetMethods = propSetMethods;
    }

    public Map<String, Class<?>> getGenericTypes() {
        return genericTypes;
    }

    public void setGenericTypes(Map<String, Class<?>> genericTypes) {
        this.genericTypes = genericTypes;
    }
}
