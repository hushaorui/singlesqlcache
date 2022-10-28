package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

/*
drop table test_player;
create table test_player (
    user_id bigint primary key comment '玩家id',
    username varchar(128) not null unique comment '玩家名称',
    third_type varchar(64) comment '第三方类型',
    third_id bigint comment '第三方id',
    create_time bigint not null comment '创建时间',
    last_login_time bigint comment '最后一次登录时间',
    last_login_ip varchar(128) comment '最后一次登录的ip',
    unique key (third_type, third_id)
) ENGINE = innoDB AUTO_INCREMENT = 100000000001;
 */
@DataClass(tableCount = 4)
public class TestPlayer {
    @FieldDesc(isId = true)
    private Long userId;
    @FieldDesc(columnType = "VARCHAR(128)", uniqueName = "username", isNotNull = true)
    private String username;
    @FieldDesc(uniqueName = "third_type_third_id")
    private String thirdType;
    @FieldDesc(uniqueName = "third_type_third_id")
    private Long thirdId;
    @FieldDesc(isNotUpdate = true, isNotNull = true)
    private Long createTime;
    private Timestamp birthdayTime;
    private Date primarySchoolStartDay;
    private Long lastLoginTime;
    private String lastLoginIp;
    private byte[] byteData;
    //private TestAddress testAddress;

    // 该段不会保存
    @FieldDesc(ignore = true)
    private String extraString;

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

    public String getThirdType() {
        return thirdType;
    }

    public void setThirdType(String thirdType) {
        this.thirdType = thirdType;
    }

    public Long getThirdId() {
        return thirdId;
    }

    public void setThirdId(Long thirdId) {
        this.thirdId = thirdId;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Timestamp getBirthdayTime() {
        return birthdayTime;
    }

    public void setBirthdayTime(Timestamp birthdayTime) {
        this.birthdayTime = birthdayTime;
    }

    public Date getPrimarySchoolStartDay() {
        return primarySchoolStartDay;
    }

    public void setPrimarySchoolStartDay(Date primarySchoolStartDay) {
        this.primarySchoolStartDay = primarySchoolStartDay;
    }

    public Long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public byte[] getByteData() {
        return byteData;
    }

    public void setByteData(byte[] byteData) {
        this.byteData = byteData;
    }

    public String getExtraString() {
        return extraString;
    }

    public void setExtraString(String extraString) {
        this.extraString = extraString;
    }

    @Override
    public String toString() {
        return "TestPlayer{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", thirdType='" + thirdType + '\'' +
                ", thirdId=" + thirdId +
                ", createTime=" + createTime +
                ", birthdayTime=" + birthdayTime +
                ", primarySchoolStartDay=" + primarySchoolStartDay +
                ", lastLoginTime=" + lastLoginTime +
                ", lastLoginIp='" + lastLoginIp + '\'' +
                ", byteData=" + Arrays.toString(byteData) +
                ", extraString='" + extraString + '\'' +
                '}';
    }
}
