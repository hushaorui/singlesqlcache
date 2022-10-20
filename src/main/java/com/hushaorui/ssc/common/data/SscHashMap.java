package com.hushaorui.ssc.common.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class SscHashMap<K, V> extends HashMap<K, V> {

    public static <K, V> SscHashMap<K, V> of(Object... kOrV) {
        SscHashMap<K, V> kvSscHashMap = new SscHashMap<>();
        kvSscHashMap.put(kOrV);
        return kvSscHashMap;
    }

    public SscHashMap<K, V> put(Object... kOrV) {
        if (kOrV == null) {
            return this;
        }
        Object key = null;
        for (Object o : kOrV) {
            if (key == null) {
                key = o;
            } else {
                put((K) key, (V) o);
                key = null;
            }
        }
        return this;
    }

    public SscHashMap<K, V> Put(K k, V v) {
        put(k, v);
        return this;
    }
    public SscHashMap<K, V> Remove(K k) {
        return this;
    }

    public SscHashMap<K, V> PutAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
        return this;
    }

    public SscHashMap<K, V> Clear() {
        super.clear();
        return this;
    }

    public SscHashMap<K, V> ReplaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        super.replaceAll(function);
        return this;
    }
}
