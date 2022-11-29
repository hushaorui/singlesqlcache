package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.TableSplitPolicy;
import com.hushaorui.ssc.common.TwinsValue;

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
    public int getTableIndex(Long tableSplitField, int maxTableCount) {
        return tableSplitField == null ? 0 : (int) (tableSplitField % maxTableCount);
    }

    @Override
    public TwinsValue<Long, Long> getRange() {
        return new TwinsValue<>(0L, Long.MAX_VALUE);
    }
}
