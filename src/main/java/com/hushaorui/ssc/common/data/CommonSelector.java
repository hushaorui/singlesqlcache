package com.hushaorui.ssc.common.data;

import com.alibaba.fastjson.annotation.JSONField;
import com.hushaorui.ssc.common.anno.FieldDesc;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommonSelector<MODEL> {
    @FieldDesc(noneSave = true)
    private Integer firstResult;
    @FieldDesc(noneSave = true)
    private Integer maxResult;
    @FieldDesc(noneSave = true)
    private String orderBy;
    @FieldDesc(noneSave = true)
    protected MODEL model;

    @JSONField(serialize = false)
    public void setPagingResult(Integer firstResult, Integer maxResult) {
        this.firstResult = firstResult;
        this.maxResult = maxResult;
    }
}
