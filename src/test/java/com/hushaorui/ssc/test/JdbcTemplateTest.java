package com.hushaorui.ssc.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.hushaorui.ssc.common.data.ColumnMetaData;
import com.hushaorui.ssc.common.em.SscLaunchPolicy;
import com.hushaorui.ssc.config.SingleSqlCacheConfig;
import com.hushaorui.ssc.main.UniqueTableOperator;
import com.hushaorui.ssc.test.common.TestPlayer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.sql.SQLException;

public class JdbcTemplateTest {
    private JdbcTemplate jdbcTemplate;
    @Before
    public void beforeTest() {
        try {
            DruidDataSource druidDataSource = newDataSource("jdbc:mysql://192.168.1.239:3306/test_ssc?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC",
                    "root", "123456", 10);
            jdbcTemplate = new JdbcTemplate(druidDataSource);
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
            SingleSqlCacheConfig config = new SingleSqlCacheConfig();
            config.setLaunchPolicy(SscLaunchPolicy.DROP_TABLE_AND_CRETE);
            UniqueTableOperator uniqueTableOperator = new UniqueTableOperator(jdbcTemplate, config);
            uniqueTableOperator.registerDataClass(TestPlayer.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
