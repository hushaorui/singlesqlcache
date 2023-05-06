package com.hushaorui.ssc.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.hushaorui.ssc.common.em.SscDataSourceType;
import com.hushaorui.ssc.config.JSONSerializer;
import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.main.Operator;
import com.hushaorui.ssc.main.SpecialOperator;
import com.hushaorui.ssc.main.TableOperatorFactory;
import com.hushaorui.ssc.test.common.TestFind;
import com.hushaorui.ssc.test.common.TestSpecialFind;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TableOperatorFactoryTest4 {
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
            config.setMaxInactiveTime(0);
            // 设置启动策略
            //config.setLaunchPolicy(SscLaunchPolicy.DROP_TABLE_AND_CRETE);
            config.getLogFactory().setLogLevel("DEBUG");
            factory = new TableOperatorFactory(jdbcTemplate, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_insert() {
        try {
            Operator<TestFind> operator = factory.getOperator(TestFind.class);
            TestFind testFind = new TestFind();
            List<Long> longs = new ArrayList<>();
            longs.add(1L);
            longs.add(2L);
            longs.add(5L);
            testFind.setLongs(longs);
            testFind.setName("jack");
            testFind.setTime(System.currentTimeMillis());
            operator.insert(testFind);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_findById() {
        try {
            Operator<TestFind> operator = factory.getOperator(TestFind.class);
            TestFind testFind = operator.findById("id,name, longs", 1L);
            System.out.println(jsonSerializer.toJsonString(testFind));
            testFind = operator.findById("id,name", 1L);
            System.out.println(jsonSerializer.toJsonString(testFind));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_findByUniqueName() {
        try {
            Operator<TestFind> operator = factory.getOperator(TestFind.class);
            TestFind testFind = operator.findByUniqueName("id,name, longs", "name", "jack");
            System.out.println(jsonSerializer.toJsonString(testFind));
            testFind = operator.findByUniqueName("id,name", "name", "jack");
            System.out.println(jsonSerializer.toJsonString(testFind));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_findByGroupField() {
        try {
            Operator<TestFind> operator = factory.getOperator(TestFind.class);
            List<TestFind> list = operator.findByGroupField("id, name, longs", "time", 1683341381786L);
            System.out.println(jsonSerializer.toJsonString(list));
            list = operator.findByGroupField("id, name", "time", 1683341381786L);
            System.out.println(jsonSerializer.toJsonString(list));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_special_insert() {
        try {
            TestSpecialFind testSpecialFind = new TestSpecialFind();
            testSpecialFind.setLongs(Collections.singletonList(10L));
            testSpecialFind.setName("tom");
            testSpecialFind.setTime(System.currentTimeMillis());
            testSpecialFind.setOtherId(9);
            factory.getSpecialOperator(TestSpecialFind.class).insert(testSpecialFind);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_special_findById() {
        try {
            SpecialOperator<TestSpecialFind> specialOperator = factory.getSpecialOperator(TestSpecialFind.class);
            TestSpecialFind specialFind = specialOperator.findById("name, otherId", 1L, 9);
            System.out.println(jsonSerializer.toJsonString(specialFind));
            specialFind = specialOperator.findById("name, otherId, time", 1L, 9);
            System.out.println(jsonSerializer.toJsonString(specialFind));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_special_findByUniqueName() {
        try {
            SpecialOperator<TestSpecialFind> specialOperator = factory.getSpecialOperator(TestSpecialFind.class);
            TestSpecialFind query = new TestSpecialFind();
            query.setName("tom");
            TestSpecialFind specialFind = specialOperator.findByUniqueName("name,otherId", "name", 9, query);
            System.out.println(jsonSerializer.toJsonString(specialFind));
            specialFind = specialOperator.findByUniqueName("time,name,otherId", "name", 9, query);
            System.out.println(jsonSerializer.toJsonString(specialFind));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_special_findByGroupField() {
        try {
            SpecialOperator<TestSpecialFind> specialOperator = factory.getSpecialOperator(TestSpecialFind.class);
            List<TestSpecialFind> list = specialOperator.findByGroupField("name,otherId", 9, "time", 1683342753019L);
            System.out.println(jsonSerializer.toJsonString(list));
            list = specialOperator.findByGroupField("time,name,otherId", 9, "time", 1683342753019L);
            System.out.println(jsonSerializer.toJsonString(list));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
