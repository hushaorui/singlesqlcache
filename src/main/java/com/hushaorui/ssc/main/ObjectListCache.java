package com.hushaorui.ssc.main;

import java.io.Serializable;
import java.util.List;

class ObjectListCache extends CommonCache {
    /** id列表缓存对象 */
    private List<Serializable> idList;

    public List<Serializable> getIdList() {
        return idList;
    }

    public void setIdList(List<Serializable> idList) {
        this.idList = idList;
    }
}
