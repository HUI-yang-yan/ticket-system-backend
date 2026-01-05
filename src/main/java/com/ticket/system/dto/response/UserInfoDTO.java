package com.ticket.system.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class UserInfoDTO {

    private Long id;
    private String username;
    private String idCard;
    private String realName;
    private String phone;
    private String email;
    private Integer userType;
    private Integer status;
    private Date createTime;
    private Date updateTime;

    // 脱敏处理
    public String getIdCard() {
        if (idCard != null && idCard.length() >= 15) {
            return idCard.substring(0, 6) + "******" + idCard.substring(idCard.length() - 4);
        }
        return idCard;
    }

    public String getPhone() {
        if (phone != null && phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }
}