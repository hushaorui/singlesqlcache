package com.hushaorui.ssc.common.em;

import java.util.HashMap;
import java.util.Map;

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
    // <当前状态, <新的状态, 最终状态>>
    private static final Map<CacheStatus, Map<CacheStatus, CacheStatus>> tansMap;
    static {
        Map<CacheStatus, Map<CacheStatus, CacheStatus>> tempMap = new HashMap<>();
        {
            Map<CacheStatus, CacheStatus> map = new HashMap<>(6, 1.5f);
            map.put(INSERT, INSERT);
            // 需要插入的数据，需要更新时，仍然是需要插入的
            map.put(UPDATE, INSERT);
            // 需要插入的数据，需要删除时，表现为已删除
            map.put(DELETE, AFTER_DELETE);
            // 需要插入的数据，需要转变为已插入，表现为需要插入
            map.put(AFTER_INSERT, INSERT);
            // 需要插入的数据，需要转变为已更新，表现为需要插入
            map.put(AFTER_UPDATE, INSERT);
            // 需要插入的数据，需要转变为已删除，表现为已删除
            map.put(AFTER_DELETE, AFTER_DELETE);
            tempMap.put(INSERT, map);
        }
        {
            Map<CacheStatus, CacheStatus> map = new HashMap<>(6, 1.5f);
            map.put(UPDATE, UPDATE);
            // 需要更新的数据，需要插入时，表现为已更新
            map.put(INSERT, UPDATE);
            // 需要更新的数据，需要删除时，表现为需要删除
            map.put(DELETE, DELETE);
            // 需要更新的数据，需要转变为已插入，仍然表现为需要更新
            map.put(AFTER_INSERT, UPDATE);
            // 需要更新的数据，需要转变为已更新，表现为已更新
            map.put(AFTER_UPDATE, AFTER_UPDATE);
            // 需要更新的数据，需要转变为已删除，表现为已删除
            map.put(AFTER_DELETE, AFTER_DELETE);
            tempMap.put(UPDATE, map);
        }
        {
            Map<CacheStatus, CacheStatus> map = new HashMap<>(6, 1.5f);
            map.put(DELETE, DELETE);
            // 需要删除的数据，需要插入时，表现为需要更新
            map.put(INSERT, UPDATE);
            // 需要删除的数据，需要更新时，表现为需要删除
            map.put(UPDATE, DELETE);
            // 需要删除的数据，需要转变为已插入，表现为需要删除
            map.put(AFTER_INSERT, DELETE);
            // 需要删除的数据，需要转变为已更新，表现为需要删除
            map.put(AFTER_UPDATE, DELETE);
            // 需要删除的数据，需要转变为已删除，表现为已删除
            map.put(AFTER_DELETE, AFTER_DELETE);
            tempMap.put(DELETE, map);
        }
        {
            Map<CacheStatus, CacheStatus> map = new HashMap<>(6, 1.5f);
            map.put(AFTER_INSERT, AFTER_INSERT);
            // 已经插入的数据，需要插入时，表现为需要更新
            map.put(INSERT, UPDATE);
            // 已经插入的数据，需要更新时，表现为需要更新
            map.put(UPDATE, UPDATE);
            // 已经插入的数据，需要删除时，表现为需要删除
            map.put(DELETE, DELETE);
            // 已经插入的数据，需要转变为已更新时，表现为已更新
            map.put(AFTER_UPDATE, AFTER_UPDATE);
            // 已经插入的数据，需要转变为已删除时，表现为已删除
            map.put(AFTER_DELETE, AFTER_DELETE);
            tempMap.put(AFTER_INSERT, map);
        }
        {
            Map<CacheStatus, CacheStatus> map = new HashMap<>(6, 1.5f);
            map.put(AFTER_UPDATE, AFTER_UPDATE);
            // 已经更新的数据，需要插入时，表现为需要更新
            map.put(INSERT, UPDATE);
            // 已经更新的数据，需要更新时，表现为需要更新
            map.put(UPDATE, UPDATE);
            // 已经更新的数据，需要删除时，表现为需要删除
            map.put(DELETE, DELETE);
            // 已经更新的数据，需要转变为已插入时，表现为已更新
            map.put(AFTER_INSERT, AFTER_UPDATE);
            // 已经更新的数据，需要转变为已删除时，表现为已删除
            map.put(AFTER_DELETE, AFTER_DELETE);
            tempMap.put(AFTER_UPDATE, map);
        }
        {
            Map<CacheStatus, CacheStatus> map = new HashMap<>(6, 1.5f);
            map.put(AFTER_DELETE, AFTER_DELETE);
            // 已经删除的数据，需要插入时，表现为需要插入
            map.put(INSERT, INSERT);
            // 已经删除的数据，需要更新时，表现为已删除
            map.put(UPDATE, AFTER_DELETE);
            // 已经删除的数据，需要删除时，表现为已删除
            map.put(DELETE, AFTER_DELETE);
            // 已经删除的数据，需要转变为已插入时，表现为已插入
            map.put(AFTER_INSERT, AFTER_INSERT);
            // 已经删除的数据，需要转变为已更新时，表现为已删除
            map.put(AFTER_UPDATE, AFTER_DELETE);
            tempMap.put(AFTER_DELETE, map);
        }
        tansMap = tempMap;
    }
    CacheStatus(boolean needSynToDB, boolean delete) {
        this.needSynToDB = needSynToDB;
        this.delete = delete;
    }

    public boolean isNeedSynToDB() {
        return needSynToDB;
    }

    public boolean isDelete() {
        return delete;
    }

    /**
     * 根据旧的状态和新的状态，获得真正的状态
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @return 真正的状态
     */
    public static CacheStatus getResultStatus(CacheStatus oldStatus, CacheStatus newStatus) {
        if (oldStatus == null) {
            return newStatus;
        }
        return tansMap.get(oldStatus).get(newStatus);
    }
}
