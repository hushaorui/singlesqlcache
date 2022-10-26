package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.TableSplitPolicy;
import javafx.util.Pair;

public class DefaultIntegerTableSplitPolicy implements TableSplitPolicy<Integer> {
    private static TableSplitPolicy<Integer> instance;
    public static TableSplitPolicy<Integer> getInstance() {
        if (instance == null) {
            synchronized (DefaultIntegerTableSplitPolicy.class) {
                if (instance == null) {
                    instance = new DefaultIntegerTableSplitPolicy();
                }
            }
        }
        return instance;
    }
    private DefaultIntegerTableSplitPolicy() {}
    @Override
    public int getTableIndex(Integer tableSplitField, int maxTableCount) {
        return tableSplitField == null ? 0 : tableSplitField % maxTableCount;
    }

    @Override
    public Pair<Integer, Integer> getRange() {
        return new Pair<>(0, Integer.MAX_VALUE);
    }
}
