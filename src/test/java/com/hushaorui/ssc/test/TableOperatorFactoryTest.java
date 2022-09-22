package com.hushaorui.ssc.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONArray;
import com.hushaorui.ssc.common.data.ColumnMetaData;
import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.main.Operator;
import com.hushaorui.ssc.main.TableOperatorFactory;
import com.hushaorui.ssc.test.common.TestPlayer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class TableOperatorFactoryTest {
    private JdbcTemplate jdbcTemplate;
    private TableOperatorFactory tableOperatorFactory;
    @Before
    public void beforeTest() {
        try {
            DruidDataSource druidDataSource = newDataSource("jdbc:mysql://192.168.1.239:3306/test_ssc?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC",
                    "root", "123456", 10);
            jdbcTemplate = new JdbcTemplate(druidDataSource);
            SingleSqlCacheConfig config = new SingleSqlCacheConfig();
            // 关闭缓存
            //config.setMaxInactiveTime(0);
            // 设置启动策略
            //config.setLaunchPolicy(SscLaunchPolicy.DROP_TABLE_AND_CRETE);
            tableOperatorFactory = new TableOperatorFactory(jdbcTemplate, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_createTable() {
        try {
            jdbcTemplate.execute("create table test2 (\n" +
                    "    id bigint primary key auto_increment comment 'id字段',\n" +
                    "    username varchar(64) unique," +
                    "    age int not null" +
                    ") ENGINE = innoDB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_table_info() {
        try {
            SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet("SELECT * FROM test2 LIMIT 0"); //注意limit 0更合适
            SqlRowSetMetaData sqlRowSetMetaData = sqlRowSet.getMetaData();
            for (int i = 1; i <= sqlRowSetMetaData.getColumnCount(); i++) {
                //获取字段的名称、类型和描述
                ColumnMetaData columnMetaData = new ColumnMetaData(sqlRowSetMetaData, i);
                System.out.println(columnMetaData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test_dropTable() {
        try {
            jdbcTemplate.execute("drop table test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_registerDataClass() {
        try {
            tableOperatorFactory.registerDataClass(TestPlayer.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_insert() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            operator.insert(getTestPlayer());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_update_select_delete() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TestPlayer testPlayer = getTestPlayer();
            operator.insert(testPlayer);
            System.out.println("更新前: " + JSONArray.toJSONString(testPlayer));
            testPlayer.setThirdId(8000000L);
            testPlayer.setLastLoginTime(System.currentTimeMillis());
            operator.update(testPlayer);
            TestPlayer player = operator.selectById(testPlayer.getUserId());
            System.out.println("更新后: " + JSONArray.toJSONString(player));
            operator.delete(player);
            player = operator.selectById(testPlayer.getUserId());
            System.out.println("删除后: " + JSONArray.toJSONString(player));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_insert_batch() {
        try {
            // 开启缓存来测试
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            Long id = null;
            for (int i = 0; i < 10; i++) {
                long now = System.currentTimeMillis();
                TestPlayer testPlayer = getTestPlayer("张三" + i, "extra" + i, "morlia" + i, 1000000L + i, new Timestamp(now), new Date());
                operator.insert(testPlayer);
                if (id == null) {
                    id = testPlayer.getUserId();
                }
            }
            TestPlayer player = operator.selectById(id);
            System.out.println(JSONArray.toJSONString(player));
            tableOperatorFactory.close();
            TestPlayer player2 = operator.selectById(id);
            System.out.println(JSONArray.toJSONString(player2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TestPlayer getTestPlayer() {
        TestPlayer player = new TestPlayer();
        player.setUsername("张三");
        long now = System.currentTimeMillis();
        player.setCreateTime(now);
        player.setExtraString("extra111");
        player.setThirdType("morlia");
        player.setThirdId(1000000L);
        player.setBirthdayTime(new Timestamp(now));
        player.setPrimarySchoolStartDay(new Date());
        return getTestPlayer("张三", "extra111", "morlia", 1000000L, new Timestamp(now), new Date());
    }

    private TestPlayer getTestPlayer(String username, String extraString, String thirdType, Long thirdId, Timestamp birthDayTime, Date primarySchoolStartDay) {
        TestPlayer player = new TestPlayer();
        player.setUsername(username);
        long now = System.currentTimeMillis();
        player.setCreateTime(now);
        player.setExtraString(extraString);
        player.setThirdType(thirdType);
        player.setThirdId(thirdId);
        player.setBirthdayTime(birthDayTime);
        player.setPrimarySchoolStartDay(primarySchoolStartDay);
        return player;
    }

    public static DruidDataSource newDataSource(String urlP, String usernameP, String passwordP, int maxActiveP) throws SQLException {
        DruidDataSource dataSource = new DruidDataSource();
        String driverClass = "com.mysql.jdbc.Driver";

        long maxWaitMillis = 60000;
        String validationQuery = "SELECT 1";
        long timeBetweenEvictionRunsMillis = 300000;
        dataSource.setDriverClassName(driverClass);
        dataSource.setUrl(urlP);
        dataSource.setUsername(usernameP);
        dataSource.setPassword(passwordP);
        dataSource.setMaxActive(maxActiveP);
        dataSource.setMaxWait(maxWaitMillis);
        dataSource.setValidationQuery(validationQuery);

        dataSource.setTestOnBorrow(true);
        dataSource.setTestOnReturn(true);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);

        dataSource.init();
        return dataSource;
    }
}
