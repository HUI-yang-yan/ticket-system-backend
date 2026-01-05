package com.ticket.system.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String idCard;
    private String realName;
    private String phone;
    private String email;
    private Integer userType;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}