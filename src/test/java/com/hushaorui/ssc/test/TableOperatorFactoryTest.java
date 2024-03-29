package com.hushaorui.ssc.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.hushaorui.ssc.common.TwinsValue;
import com.hushaorui.ssc.config.JSONSerializer;
import com.hushaorui.ssc.config.SscGlobalConfig;
import com.hushaorui.ssc.main.Operator;
import com.hushaorui.ssc.main.TableOperatorFactory;
import com.hushaorui.ssc.test.common.TestSpecial;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableOperatorFactoryTest {
    private JdbcTemplate jdbcTemplate;
    private TableOperatorFactory tableOperatorFactory;
    private JSONSerializer jsonSerializer;
    @Before
    public void beforeTest() {
        try {
            jsonSerializer = (JSONSerializer) Class.forName("com.hushaorui.ssc.config.DefaultJSONSerializer").newInstance();
            DruidDataSource druidDataSource = newDataSource("jdbc:mysql://192.168.1.239:3306/test_ssc?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=UTC",
                    "root", "123456", 10);
            jdbcTemplate = new JdbcTemplate(druidDataSource);
            SscGlobalConfig config = new SscGlobalConfig();
            // 关闭缓存
            //config.setMaxInactiveTime(0);
            // 设置启动策略
            //config.setLaunchPolicy(SscLaunchPolicy.DROP_TABLE_AND_CRETE);
            config.getLogFactory().setLogLevel("DEBUG");
            tableOperatorFactory = new TableOperatorFactory(jdbcTemplate, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static DruidDataSource newDataSource(String urlP, String usernameP, String passwordP, int maxActiveP) throws SQLException {
        return newDataSource(urlP, usernameP, passwordP, maxActiveP, null);
    }

    public static DruidDataSource newDataSource(String urlP, String usernameP, String passwordP, int maxActiveP, String driverClass) throws SQLException {
        DruidDataSource dataSource = new DruidDataSource();
        if (driverClass == null) {
            driverClass = "com.mysql.jdbc.Driver";
        }

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

    @Test
    public void test_TestSpecial_insert() {
        try {
            Operator<TestSpecial> operator = tableOperatorFactory.getOperator(TestSpecial.class);
            TestSpecial testSpecial = new TestSpecial();
            String[] stringArray = new String[] {"2", "3"};
            Boolean[] booleans = new Boolean[] {true, false, null};
            String message = "message111";
            List<Integer> integers = Arrays.asList(1, 2, 3, 4);
            Map<Integer, String> integerStringMap = new HashMap<>();
            integerStringMap.put(10, "10");
            integerStringMap.put(11, "10");
            integerStringMap.put(12, "10");
            testSpecial.setStringArray(stringArray);
            testSpecial.setBooleans(booleans);
            testSpecial.setMessage(message);
            testSpecial.setIntegers(integers);
            testSpecial.setIntegerStringMap(integerStringMap);
            operator.insert(testSpecial);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_TestSpecial_select() {
        try {
            Operator<TestSpecial> operator = tableOperatorFactory.getOperator(TestSpecial.class);
            TestSpecial testSpecial = operator.selectById(1L);
            System.out.println(jsonSerializer.toJsonString(testSpecial));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_TestSpecial_selectByCondition() {
        try {
            Operator<TestSpecial> operator = tableOperatorFactory.getOperator(TestSpecial.class);
            TestSpecial query = new TestSpecial();
            Map<Integer, String> integerStringMap = new HashMap<>();
            integerStringMap.put(10, "10");
            integerStringMap.put(11, "10");
            integerStringMap.put(12, "10");
            query.setIntegerStringMap(integerStringMap);
            List<TestSpecial> testSpecial = operator.selectByCondition(new TwinsValue<>("integerStringMap", integerStringMap));
            System.out.println(jsonSerializer.toJsonString(testSpecial));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

/*
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
            System.out.println("更新前: " + jsonSerializer.toJsonString(testPlayer));
            testPlayer.setThirdId(8000000L);
            testPlayer.setLastLoginTime(System.currentTimeMillis());
            operator.update(testPlayer);
            TestPlayer player = operator.selectById(testPlayer.getUserId());
            System.out.println("更新后: " + jsonSerializer.toJsonString(player));
            operator.delete(player);
            player = operator.selectById(testPlayer.getUserId());
            System.out.println("删除后: " + jsonSerializer.toJsonString(player));
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
            System.out.println(jsonSerializer.toJsonString(player));
            tableOperatorFactory.close();
            TestPlayer player2 = operator.selectById(id);
            System.out.println(jsonSerializer.toJsonString(player2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByUniqueName() {
        try {
            // 开启缓存来测试
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TestPlayer select = new TestPlayer();
            select.setThirdType("morlia1");
            select.setThirdId(1000001L);
            TestPlayer player = operator.selectByUniqueName("third_type_third_id", select);
            System.out.println(player == null ? null : player.hashCode());
            // 第二次能够使用缓存
            player = operator.selectByUniqueName("third_type_third_id", select);
            System.out.println(player == null ? null : player.hashCode());
            System.out.println(jsonSerializer.toJsonString(player));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByUniqueName_2() {
        try {
            // 开启缓存来测试
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TestPlayer select = new TestPlayer();
            select.setUsername("张三1");
            TestPlayer player = operator.selectByUniqueName("username", select);
            System.out.println(player == null ? null : player.hashCode());
            // 第二次能够使用缓存
            player = operator.selectByUniqueName("username", select);
            System.out.println(player == null ? null : player.hashCode());
            System.out.println(jsonSerializer.toJsonString(player));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByCondition_1() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TwinsValue<String, Object> condition1 = new TwinsValue<>("username", "张三1");
            TwinsValue<String, Object> condition2 = new TwinsValue<>("third_type", "morlia1");
            List<TestPlayer> testPlayers = operator.selectByCondition(condition1, condition2);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
            testPlayers = operator.selectByCondition(condition1, condition2);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByCondition_2() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            ArrayList<Long> ids = new ArrayList<>();
            ids.add(4L);
            ids.add(5L);
            ids.add(6L);
            ids.add(10L);
            TwinsValue<String, Object> condition1 = new TwinsValue<>("userId", new ValueIn<>(ids));
            TwinsValue<String, Object> condition2 = new TwinsValue<>("firstResult", new ValueFirstResult(0));
            TwinsValue<String, Object> condition3 = new TwinsValue<>("maxResult", new ValueMaxResult(3));
            List<TestPlayer> testPlayers = operator.selectByCondition(condition1, condition2, condition3);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
            testPlayers = operator.selectByCondition(condition1, condition2, condition3);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByCondition_ValueIsNotNull() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TwinsValue<String, Object> condition1 = new TwinsValue<>("lastLoginIp", ValueIsNotNull.IS_NOT_NULL);
            List<TestPlayer> testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
            testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByCondition_ValueIsNull() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TwinsValue<String, Object> condition1 = new TwinsValue<>("lastLoginIp", ValueIsNull.IS_NULL);
            List<TestPlayer> testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
            testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByCondition_ValueGreatThan() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TwinsValue<String, Object> condition1 = new TwinsValue<>("user_id", new ValueGreatThan<>(5L));
            List<TestPlayer> testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
            testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_selectByCondition_ValueBetween() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TwinsValue<String, Object> condition1 = new TwinsValue<>("user_id", new ValueBetween<>(5L, 6L));
            List<TestPlayer> testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
            testPlayers = operator.selectByCondition(condition1);
            System.out.println(jsonSerializer.toJsonString(testPlayers));
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

    @Test
    public void test_getAllClassDescYamlData() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            tableOperatorFactory.outputClassDescYamlData("C:\\Users\\Win10\\Desktop\\ssc.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_limit() {
        Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
        List<TwinsValue<String, Object>> conditions = new ArrayList<>();
        conditions.add(new TwinsValue<>("", new ValueFirstResult(0)));
        conditions.add(new TwinsValue<>("", new ValueMaxResult(4)));
        List<TestPlayer> testPlayers = operator.selectByCondition(conditions);
        testPlayers.forEach(System.out::println);
    }

    @Test
    public void test_selectIdByCondition() {
        Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
        List<TwinsValue<String, Object>> conditions = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(8L);
        conditions.add(new TwinsValue<>("userId", new ValueIn<>(ids)));
        conditions.add(new TwinsValue<>("", new ValueFirstResult(0)));
        conditions.add(new TwinsValue<>("", new ValueMaxResult(4)));
        List<Long> idList = operator.selectIdByCondition(conditions);
        idList.forEach(System.out::println);
    }

    @Test
    public void test_countByCondition() {
        Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
        List<TwinsValue<String, Object>> conditions = new ArrayList<>();
        *//*List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(8L);
        conditions.add(new TwinsValue<>("userId", new ValueIn<>(ids)));
        conditions.add(new TwinsValue<>("", new ValueFirstResult(0)));
        conditions.add(new TwinsValue<>("", new ValueMaxResult(4)));*//*
        int count = operator.countByCondition(conditions);
        System.out.println(count);
    }

    @Test
    public void test_cast() {
        try {
            String className = TestPlayer.class.getName();
            Operator<Object> operator = tableOperatorFactory.getOperator((Class<Object>) Class.forName(className));
            TestPlayer testPlayer = new TestPlayer();
            testPlayer.setUserId(100L);
            Object object = testPlayer;
            operator.delete(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_updateUniqueName() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TestPlayer testPlayer = new TestPlayer();
            testPlayer.setUsername("张三1");
            TestPlayer result = operator.selectByUniqueName("username", testPlayer);
            System.out.println(JSONArray.toJSONString(result));
            result.setUsername("张三111");
            operator.update(result);
            result = operator.selectByUniqueName("username", testPlayer);
            System.out.println("第二次查找：" + JSONArray.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_blob() {
        try {
            Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
            TestPlayer testPlayer = new TestPlayer();
            testPlayer.setUsername("张三1");
            TestPlayer result = operator.selectByUniqueName("username", testPlayer);
            byte[] byteData = result.getByteData();
            if (byteData != null) {
                System.out.println("byteData: " + new String(byteData, StandardCharsets.UTF_8));
            }
            result.setByteData("haha".getBytes(StandardCharsets.UTF_8.name()));
            operator.update(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_tableCount_1_insert() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            {
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setUserId(100L);
                testMiniGame.setData("data04");
                operator.insert(testMiniGame);
            }
            {
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setUserId(101L);
                testMiniGame.setData("data25");
                operator.insert(testMiniGame);
            }
            {
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setUserId(102L);
                testMiniGame.setData("data26");
                operator.insert(testMiniGame);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_tableCount_1_limit() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            List<TestMiniGame> testMiniGames = operator.selectByCondition(new TwinsValue<>("", new ValueFirstResult(0)), new TwinsValue<>("", new ValueMaxResult(1)));
            System.out.println(JSONArray.toJSONString(testMiniGames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_MiniGame_select() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            List<TestMiniGame> testMiniGames = operator.selectByTableSplitField(100L);
            System.out.println(JSONArray.toJSONString(testMiniGames));
            *//*{
                // 中间加一个同userId的数据
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setUserId(100L);
                testMiniGame.setData("data06");
                operator.insert(testMiniGame);
            }*//*
            *//*{
                // 删除一个数据
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setGameId(6L);
                testMiniGame.setUserId(100L);
                operator.delete(testMiniGame);
            }*//*
            testMiniGames = operator.selectByTableSplitField(100L);
            System.out.println(JSONArray.toJSONString(testMiniGames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_MiniGame_insert_and_select() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            {
                // 先插入，再查询
                // 中间加一个同userId的数据
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setUserId(100L);
                testMiniGame.setData("data06");
                operator.insert(testMiniGame);
            }
            List<TestMiniGame> testMiniGames = operator.selectByTableSplitField(100L);
            System.out.println("user100: " + JSONArray.toJSONString(testMiniGames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_MiniGame_delete_and_select() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            {
                // 先删除，再查询
                // 删除一个数据
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setGameId(6L);
                testMiniGame.setUserId(100L);
                operator.delete(testMiniGame);
            }
            List<TestMiniGame> testMiniGames = operator.selectByTableSplitField(100L);
            System.out.println("user100: " + JSONArray.toJSONString(testMiniGames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_MiniGame_selectByGroup() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            List<TestMiniGame> testMiniGames = operator.selectByGroupField(100L, "data", "data0");
            System.out.println(JSONArray.toJSONString(testMiniGames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_MiniGame_insert_and_selectByGroup() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            {
                // 先插入，再查询
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setUserId(100L);
                testMiniGame.setData("data0");
                operator.insert(testMiniGame);
            }
            List<TestMiniGame> testMiniGames = operator.selectByGroupField(100L, "data", "data0");
            System.out.println(JSONArray.toJSONString(testMiniGames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void test_MiniGame_delete_and_selectByGroup() {
        try {
            SpecialOperator<TestMiniGame> operator = tableOperatorFactory.getSpecialOperator(TestMiniGame.class);
            {
                // 先插入，再查询
                TestMiniGame testMiniGame = new TestMiniGame();
                testMiniGame.setGameId(7L);
                testMiniGame.setUserId(100L);
                testMiniGame.setData("data0");
                operator.delete(testMiniGame);
            }
            List<TestMiniGame> testMiniGames = operator.selectByGroupField(100L, "data", "data0");
            System.out.println(JSONArray.toJSONString(testMiniGames));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_common_resource() {
        try {
            Operator<CommonResource> operator = tableOperatorFactory.getOperator(CommonResource.class);
            List<TwinsValue<String, Object>> conditions = new ArrayList<>();
            conditions.add(new TwinsValue<>("uploadUserId", 1L));
            conditions.add(new TwinsValue<>("resourceType", 1));
            List<Integer> privileges = new ArrayList<>(4);
            privileges.add(-1);
            privileges.add(-2);
            privileges.add(0);
            conditions.add(new TwinsValue<>("privilegeLevel", new ValueIn<>(privileges)));
            List<CommonResource> commonResources = operator.selectByCondition(conditions);
            System.out.println(JSONArray.toJSONString(commonResources));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_ValueIsNot() {
        Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
        TwinsValue<String, Object> pair = new TwinsValue<>("username", new ValueIsNot<>("张三1"));
        List<TestPlayer> testPlayers = operator.selectByCondition(pair);
        if (testPlayers != null) {
            for (TestPlayer testPlayer : testPlayers) {
                System.out.println(JSONArray.toJSONString(testPlayer));
            }
        }
    }

    @Test
    public void test_deleteByCondition() {
        Operator<TestPlayer> operator = tableOperatorFactory.getOperator(TestPlayer.class);
        TwinsValue<String, Object> pair = new TwinsValue<>("username", "99");
        operator.deleteByCondition(pair);
    }*/
}
