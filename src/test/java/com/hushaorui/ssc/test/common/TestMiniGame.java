package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.DataClass;

@DataClass(tableSplitField = "userId")
public class TestMiniGame {
    private Long gameId;
    private Long userId;
    private String data;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
