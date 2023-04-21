package com.hushaorui.ssc.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.hushaorui.ssc.common.TwinsValue;
import com.hushaorui.ssc.common.em.SscDataSourceType;
import com.hushaorui.ssc.config.JSONSerializer;
import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.main.Operator;
import com.hushaorui.ssc.main.SpecialOperator;
import com.hushaorui.ssc.main.TableOperatorFactory;
import com.hushaorui.ssc.param.ValueFirstResult;
import com.hushaorui.ssc.param.ValueMaxResult;
import com.hushaorui.ssc.test.common.TestLongList;
import com.hushaorui.ssc.test.common.TestNovelSectionModel;
import com.hushaorui.ssc.test.common.TestUseDb;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

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
            config.setDataSourceType(SscDataSourceType.Mysql.name());
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

    @Test
    public void test_tableCount_8_limit_insert() {
        try {
            SpecialOperator<TestNovelSectionModel> specialOperator = tableOperatorFactory.getSpecialOperator(TestNovelSectionModel.class);
            for (int i = 0; i < 200; i++) {
                TestNovelSectionModel sectionModel = new TestNovelSectionModel();
                sectionModel.setAccountId(2L);
                sectionModel.setNovelId((long) (i % 2));
                sectionModel.setSectionText("text" + i);
                specialOperator.insert(sectionModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void test_tableCount_8_limit_selectByCondition() {
        try {
            SpecialOperator<TestNovelSectionModel> specialOperator = tableOperatorFactory.getSpecialOperator(TestNovelSectionModel.class);
            List<TwinsValue<String, Object>> conditions = new ArrayList<>();
            conditions.add(new TwinsValue<>("", new ValueFirstResult(0)));
            conditions.add(new TwinsValue<>("", new ValueMaxResult(10)));
            List<TestNovelSectionModel> list = specialOperator.selectByCondition(conditions);
            for (TestNovelSectionModel model : list) {
                System.out.println(jsonSerializer.toJsonString(model));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_tableCount_8_limit_countByCondition() {
        try {
            SpecialOperator<TestNovelSectionModel> specialOperator = tableOperatorFactory.getSpecialOperator(TestNovelSectionModel.class);
            List<TwinsValue<String, Object>> conditions = new ArrayList<>();
            conditions.add(new TwinsValue<>("accountId", 2L));
            int count = specialOperator.countByCondition(conditions);
            System.out.println(count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_long_list() {
        try {
            Operator<TestLongList> operator = tableOperatorFactory.getOperator(TestLongList.class);
            /*TestLongList testLongList = new TestLongList();
            List<Long> longValue = new ArrayList<>();
            for (long i = 0; i < 9; i++) {
                longValue.add(i);
            }
            testLongList.setLongs(longValue);
            operator.insert(testLongList);*/
            TestLongList testLongList = operator.selectById(1L);
            System.out.println(jsonSerializer.toJsonString(testLongList.getLongs()));
            for (Long num : testLongList.getLongs()) {
                System.out.println(num);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
