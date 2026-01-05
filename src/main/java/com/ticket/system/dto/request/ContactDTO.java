package com.ticket.system.dto.request;

import lombok.Data;

@Data
public class ContactDTO {
    String realName;
    String phone;
    String idCard;
    Integer type;
}
