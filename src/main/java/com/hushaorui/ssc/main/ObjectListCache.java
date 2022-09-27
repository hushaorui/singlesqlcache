package com.hushaorui.ssc.main;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
class ObjectListCache extends CommonCache {
    /** id列表缓存对象 */
    private List<Serializable> idList;
}
