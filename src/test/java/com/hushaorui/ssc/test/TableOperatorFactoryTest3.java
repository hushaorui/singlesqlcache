package com.hushaorui.ssc.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.hushaorui.ssc.config.JSONSerializer;
import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.main.Operator;
import com.hushaorui.ssc.main.TableOperatorFactory;
import com.hushaorui.ssc.test.common.TestUseDb;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class TableOperatorFactoryTest3 {
    private TableOperatorFactory tableOperatorFactory;
    private JSONSerializer jsonSerializer;
    @Before
    public void beforeTest() {
        try {
            jsonSerializer = (JSONSerializer) Class.forName("com.hushaorui.ssc.config.DefaultJSONSerializer").newInstance();
            DruidDataSource druidDataSource = TableOperatorFactoryTest.newDataSource("jdbc:mysql://192.168.1.239:3306/test_ssc?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC",
                    "root", "123456", 10);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(druidDataSource);
            SscGlobalConfig config = new SscGlobalConfig();
            // 关闭缓存
            config.setMaxInactiveTime(0);
            // 设置启动策略
            //config.setLaunchPolicy(SscLaunchPolicy.DROP_TABLE_AND_CRETE);
            config.getLogFactory().setLogLevel("DEBUG");
            tableOperatorFactory = new TableOperatorFactory(jdbcTemplate, config);
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
            testUseDb.setLargeNum(null);
            operator.insert(testUseDb);
            TestUseDb select2 = operator.selectById(testUseDb.getDbId());
            System.out.println(select2.getType());
            System.out.println(select2.getLargeNum());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
