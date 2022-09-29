package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.DataClass;

@DataClass(value = "test_player_account", tableCount = 2)
public class TestAccount {
    private Long accountId;
    private Long userId;
    private String accountName;
    private String password;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
