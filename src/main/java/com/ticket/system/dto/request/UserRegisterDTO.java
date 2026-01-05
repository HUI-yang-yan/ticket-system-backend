package com.ticket.system.dto.request;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度在3-20个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度在6-20个字符")
    private String password;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "\\d{17}[\\d|x|X]|\\d{15}", message = "身份证格式不正确")
    private String idCard;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 10, message = "姓名长度在2-10个字符")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "1[3-9]\\d{9}", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;
}