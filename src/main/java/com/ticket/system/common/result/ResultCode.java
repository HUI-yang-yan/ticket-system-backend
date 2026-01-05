package com.ticket.system.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILURE(500, "操作失败"),

    // 参数错误
    PARAM_ERROR(400, "参数错误"),

    // 权限错误
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),

    // 业务错误
    BUSINESS_ERROR(1000, "业务错误"),

    // 系统错误
    SYSTEM_ERROR(5000, "系统错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}