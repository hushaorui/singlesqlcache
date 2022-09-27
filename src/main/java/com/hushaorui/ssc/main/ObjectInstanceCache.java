package com.hushaorui.ssc.main;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class ObjectInstanceCache extends CommonCache {
    /** 缓存对象 */
    private Object objectInstance;
}
