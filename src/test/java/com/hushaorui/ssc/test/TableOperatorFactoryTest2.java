package com.hushaorui.ssc.test;

import com.hushaorui.ssc.config.JSONSerializer;
import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.main.Operator;
import com.hushaorui.ssc.main.TableOperatorFactory;
import com.hushaorui.ssc.test.common.TestUseDb;
import org.junit.Before;
import org.junit.Test;

public class TableOperatorFactoryTest2 {
    private TableOperatorFactory tableOperatorFactory;
    private JSONSerializer jsonSerializer;
    @Before
    public void beforeTest() {
        try {
            jsonSerializer = (JSONSerializer) Class.forName("com.hushaorui.ssc.config.DefaultJSONSerializer").newInstance();
            SscGlobalConfig config = new SscGlobalConfig();
            // 关闭缓存
            //config.setMaxInactiveTime(0);
            // 设置启动策略
            //config.setLaunchPolicy(SscLaunchPolicy.DROP_TABLE_AND_CRETE);
            config.getLogFactory().setLogLevel("DEBUG");
            config.setOnlyInMemory(true);
            tableOperatorFactory = new TableOperatorFactory(null, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_insert_select() {
        try {
            Operator<TestUseDb> operator = tableOperatorFactory.getOperator(TestUseDb.class);
            TestUseDb testUseDb = new TestUseDb();
            testUseDb.setDbName("dbName1");
            testUseDb.setType(10);
            operator.insert(testUseDb);
            TestUseDb select = operator.selectById(testUseDb.getDbId());
            System.out.println(jsonSerializer.toJsonString(select));
            operator.delete(testUseDb.getDbId());
            TestUseDb select2 = operator.selectById(testUseDb.getDbId());
            System.out.println(jsonSerializer.toJsonString(select2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
