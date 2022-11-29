package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.FieldDesc;

public class CommonResource {
    /**
     * 自增资源id
     */
    private Long resourceId;

    /**
     * 资源名称
     */
    @FieldDesc(isNotNull = true)
    private String resourceName;

    /**
     * 资源类型，对应一个枚举值
     */
    @FieldDesc(isNotNull = true)
    private Integer resourceType;

    /**
     * 扩展属性，不同的类型有不同的内容
     */
    @FieldDesc(columnType = "TEXT")
    private String resourceExtra;

    /**
     * 权限等级，数字越大意味着越开放，-1为仅本人可见，-2为仅本人可见且加密
     */
    @FieldDesc(isNotNull = true)
    private Integer privilegeLevel;

    /**
     * 上传者用户id
     */
    @FieldDesc(isNotNull = true)
    private Long uploadUserId;

    /**
     * 最后一次修改时间
     */
    private Long lastModifyTime;

    /**
     * 创建时间
     */
    @FieldDesc(isNotNull = true, isNotUpdate = true)
    private Long createTime;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getResourceType() {
        return resourceType;
    }

    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceExtra() {
        return resourceExtra;
    }

    public void setResourceExtra(String resourceExtra) {
        this.resourceExtra = resourceExtra;
    }

    public Integer getPrivilegeLevel() {
        return privilegeLevel;
    }

    public void setPrivilegeLevel(Integer privilegeLevel) {
        this.privilegeLevel = privilegeLevel;
    }

    public Long getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(Long uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public Long getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
