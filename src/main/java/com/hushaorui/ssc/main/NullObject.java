package com.hushaorui.ssc.main;

/**
 * 空对象
 * 因 ConcurrentHashMap 等集合中无法放入null值
 */
class NullObject implements Comparable<NullObject> {
    static final NullObject INSTANCE = new NullObject();
    @Override
    public int compareTo(NullObject o) {
        return 0;
    }
}
