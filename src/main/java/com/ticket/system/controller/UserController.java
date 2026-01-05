package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.UserLoginDTO;
import com.ticket.system.dto.request.UserRegisterDTO;
import com.ticket.system.dto.response.UserInfoDTO;
import com.ticket.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<UserInfoDTO> register(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
        log.info("用户注册: username={}", userRegisterDTO.getUsername());
        UserInfoDTO userInfo = userService.register(userRegisterDTO);
        return Result.success("注册成功", userInfo);
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        log.info("用户登录: username={}", userLoginDTO.getUsername());
        String token = userService.login(userLoginDTO);
        return Result.success("登录成功", token);
    }

    @GetMapping("/info")
    public Result<UserInfoDTO> getUserInfo() {
        UserInfoDTO userInfo = userService.getUserInfo(getCurrentUserId());
        return Result.success(userInfo);
    }

    @PutMapping("/update")
    public Result<String> updateUser(@RequestBody @Valid UserInfoDTO userInfoDTO) {
        userService.updateUser(userInfoDTO);
        return Result.success("更新成功");
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        userService.logout();
        return Result.success("登出成功");
    }

    @GetMapping("/check/username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsernameExist(username);
        return Result.success(exists);
    }

    @GetMapping("/check/phone")
    public Result<Boolean> checkPhone(@RequestParam String phone) {
        boolean exists = userService.checkPhoneExist(phone);
        return Result.success(exists);
    }

    @GetMapping("/check/idCard")
    public Result<Boolean> checkIdCard(@RequestParam String idCard) {
        boolean exists = userService.checkIdCardExist(idCard);
        return Result.success(exists);
    }

    private Long getCurrentUserId() {
        return ThreadLocalUtil.getUserId();
    }
}