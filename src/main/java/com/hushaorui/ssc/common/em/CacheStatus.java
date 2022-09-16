package com.hushaorui.ssc.common.em;

import lombok.Getter;

@Getter
public enum CacheStatus {
    /** 需要插入数据 */
    INSERT(true, false),
    /** 需要更新数据 */
    UPDATE(true, false),
    /** 需要删除数据 */
    DELETE(true, true),
    /** 刚完成插入数据 */
    AFTER_INSERT(false, false),
    /** 刚完成更新数据 */
    AFTER_UPDATE(false, false),
    /** 刚完成删除数据 */
    AFTER_DELETE(false, true),
    ;
    /** 是否需要与数据库同步 */
    private boolean needSynToDB;
    /** 对象是否已被删除 */
    private boolean delete;
    CacheStatus(boolean needSynToDB, boolean delete) {
        this.needSynToDB = needSynToDB;
        this.delete = delete;
    }
}
