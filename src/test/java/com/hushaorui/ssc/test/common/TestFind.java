package com.hushaorui.ssc.test.common;

import com.hushaorui.ssc.common.anno.FieldDesc;

import java.util.List;

public class TestFind {
    private Long id;
    private List<Long> longs;
    @FieldDesc(uniqueName = "name_num")
    private String name;
    @FieldDesc(uniqueName = "name_num")
    private Integer num;
    @FieldDesc(isGroup = true)
    private Long time;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Long> getLongs() {
        return longs;
    }

    public void setLongs(List<Long> longs) {
        this.longs = longs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
