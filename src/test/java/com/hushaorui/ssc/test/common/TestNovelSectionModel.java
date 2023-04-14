package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.DataClass;
import com.hushaorui.ssc.common.anno.FieldDesc;

/**
 * 小说文本切片
 */
@DataClass(tableCount = 8, tableSplitField = "accountId")
public class TestNovelSectionModel {
    private Long sectionId;

    /**
     * 文本内容
     */
    @FieldDesc(columnType = "VARCHAR(4096)", isNotNull = true)
    private String sectionText;
    /**
     * 小说id
     */
    @FieldDesc(isGroup = true, isNotNull = true, isNotUpdate = true)
    private Long novelId;

    // 创建者账号id
    @FieldDesc(isNotNull = true, isNotUpdate = true)
    private Long accountId;

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionText() {
        return sectionText;
    }

    public void setSectionText(String sectionText) {
        this.sectionText = sectionText;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
