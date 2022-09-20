package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;
import lombok.Getter;
import lombok.Setter;

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
@Getter
@Setter
@DataClass(tableCount = 4)
public class TestPlayer {
    @FieldDesc(isId = true)
    private Long userId;
    @FieldDesc(columnType = "VARCHAR(128)", uniqueName = "username", isNotNull = true)
    private String username;
    @FieldDesc(uniqueName = "thirdType_thirdId")
    private String thirdType;
    @FieldDesc(uniqueName = "thirdType_thirdId")
    private Long thirdId;
    @FieldDesc(isNotUpdate = true, isNotNull = true)
    private Long createTime;
    private Long lastLoginTime;
    private Long lastLoginIp;

    // 该段不会保存
    @FieldDesc(noneSave = true)
    private String extraString;
}
