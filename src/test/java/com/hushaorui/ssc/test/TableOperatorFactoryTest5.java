package com.hushaorui.ssc.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.hushaorui.ssc.common.em.SscDataSourceType;
import com.hushaorui.ssc.config.JSONSerializer;
import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.main.Operator;
import com.hushaorui.ssc.main.TableOperatorFactory;
import com.hushaorui.ssc.test.common.TestFirstId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class TableOperatorFactoryTest5 {
    private TableOperatorFactory factory;
    private JSONSerializer jsonSerializer;
    @Before
    public void beforeTest() {
        try {
            jsonSerializer = (JSONSerializer) Class.forName("com.hushaorui.ssc.config.DefaultJSONSerializer").newInstance();
            DruidDataSource druidDataSource = TableOperatorFactoryTest.newDataSource("jdbc:mysql://192.168.1.239:3306/test_ssc?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC",
                    "root", "123456", 10);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(druidDataSource);
            SscGlobalConfig config = new SscGlobalConfig();
            config.setDataSourceType(SscDataSourceType.Mysql.name());
            // 关闭缓存
            //config.setMaxInactiveTime(0);
            // 设置启动策略
            //config.setLaunchPolicy(SscLaunchPolicy.DROP_TABLE_AND_CRETE);
            config.getLogFactory().setLogLevel("DEBUG");
            factory = new TableOperatorFactory(jdbcTemplate, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_FirstId() {
        try {
            Operator<TestFirstId> operator = factory.getOperator(TestFirstId.class);
            TestFirstId testFirstId = new TestFirstId();
            testFirstId.setName("tom3");
            operator.insert(testFirstId);
            testFirstId = operator.selectByUniqueName("name", "tom3");
            System.out.println(jsonSerializer.toJsonString(testFirstId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
