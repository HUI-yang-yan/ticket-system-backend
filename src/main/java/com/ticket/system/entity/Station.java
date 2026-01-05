package com.ticket.system.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Station {
    private Long id;
    private String stationCode;
    private String stationName;
    private String city;
    private String province;
    private String pinyin;
    private String pinyinShort;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}