package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.TableSplitPolicy;
import javafx.util.Pair;

public class DefaultLongTableSplitPolicy implements TableSplitPolicy<Long> {
    private static TableSplitPolicy<Long> instance;
    public static TableSplitPolicy<Long> getInstance() {
        if (instance == null) {
            synchronized (DefaultLongTableSplitPolicy.class) {
                if (instance == null) {
                    instance = new DefaultLongTableSplitPolicy();
                }
            }
        }
        return instance;
    }
    private DefaultLongTableSplitPolicy() {}
    @Override
    public int getTableIndex(Long id, int maxTableCount) {
        return (int) (id % maxTableCount);
    }

    @Override
    public Pair<Long, Long> getRange() {
        return new Pair<>(0L, Long.MAX_VALUE);
    }
}
