package com.ticket.system.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class StationInfoDTO {

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

    // 完整地址
    public String getFullAddress() {
        return province + city + stationName;
    }

    // 获取城市名称
    public String getCityName() {
        return city + "市";
    }
}