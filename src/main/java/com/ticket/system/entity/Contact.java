package com.ticket.system.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Contact {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String idCard;
    private Integer type;       // 1-成人 2-儿童 3-学生
    private Integer isDefault;  // 0-否 1-是
    private Integer status;     // 0-删除 1-正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
