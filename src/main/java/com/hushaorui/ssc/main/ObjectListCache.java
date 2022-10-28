package com.hushaorui.ssc.main;

import java.util.List;

class ObjectListCache extends CommonCache {
    /** id列表缓存对象 */
    private List<Comparable> idList;

    public List<Comparable> getIdList() {
        return idList;
    }

    public void setIdList(List<Comparable> idList) {
        this.idList = idList;
    }
}
