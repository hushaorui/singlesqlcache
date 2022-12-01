package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;

/**
 * <p>
 * 
 * </p>
 *
 * @author Hu Shao Rui
 * @since 2022-11-28
 */
@DataClass
public class User {

    /**
     * 自增用户id
     */
    private Long userId;

    /**
     * 用户名称
     */
    @FieldDesc(uniqueName = "username", isNotNull = true)
    private String username;

    /**
     * md5后的用户密码
     */
    @FieldDesc(isNotNull = true)
    private String password;

    /**
     * 权限等级，0最高
     */
    @FieldDesc(isNotNull = true)
    private Integer privilegeLevel;

    /**
     * 最后一次登录时间
     */
    private Long lastLoginTime;

    /**
     * 创建时间
     */
    @FieldDesc(isNotNull = true, isNotUpdate = true)
    private Long createTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPrivilegeLevel() {
        return privilegeLevel;
    }

    public void setPrivilegeLevel(Integer privilegeLevel) {
        this.privilegeLevel = privilegeLevel;
    }

    public Long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
