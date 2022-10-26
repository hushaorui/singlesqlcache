package com.hushaorui.ssc.main;

import com.hushaorui.ssc.config.TableSplitPolicy;
import com.hushaorui.ssc.util.SscStringUtils;
import javafx.util.Pair;

public class DefaultStringTableSplitPolicy implements TableSplitPolicy<String> {
    private static TableSplitPolicy<String> instance;
    public static TableSplitPolicy<String> getInstance() {
        if (instance == null) {
            synchronized (DefaultStringTableSplitPolicy.class) {
                if (instance == null) {
                    instance = new DefaultStringTableSplitPolicy();
                }
            }
        }
        return instance;
    }
    private DefaultStringTableSplitPolicy() {}
    @Override
    public int getTableIndex(String tableSplitField, int maxTableCount) {
        if (tableSplitField == null) {
            return 0;
        }
        String md5Encode = SscStringUtils.md5Encode(tableSplitField);
        return md5Encode.charAt(0) % maxTableCount;
    }

    @Override
    public Pair<String, String> getRange() {
        return new Pair<>("", "");
    }
}
