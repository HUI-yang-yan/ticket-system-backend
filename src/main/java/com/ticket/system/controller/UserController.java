package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.UserLoginDTO;
import com.ticket.system.dto.request.UserRegisterDTO;
import com.ticket.system.dto.response.UserInfoDTO;
import com.ticket.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "用户管理", description = "用户注册、登录、信息管理")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户提供用户名、密码、手机号、身份证进行注册")
    public Result<UserInfoDTO> register(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
        log.info("用户注册: username={}", userRegisterDTO.getUsername());
        UserInfoDTO userInfo = userService.register(userRegisterDTO);
        return Result.success("注册成功", userInfo);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT Token")
    public Result<String> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        log.info("用户登录: username={}", userLoginDTO.getUsername());
        String token = userService.login(userLoginDTO);
        return Result.success("登录成功", token);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "获取登录用户详细信息")
    public Result<UserInfoDTO> getUserInfo() {
        UserInfoDTO userInfo = userService.getUserInfo(getCurrentUserId());
        return Result.success(userInfo);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新用户个人资料信息")
    public Result<String> updateUser(@RequestBody @Valid UserInfoDTO userInfoDTO) {
        userService.updateUser(userInfoDTO);
        return Result.success("更新成功");
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出当前登录状态")
    public Result<String> logout() {
        userService.logout();
        return Result.success("登出成功");
    }

    @GetMapping("/check/username")
    @Operation(summary = "检查用户名是否存在", description = "验证用户名是否已被注册")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsernameExist(username);
        return Result.success(exists);
    }

    @GetMapping("/check/phone")
    @Operation(summary = "检查手机号是否存在", description = "验证手机号是否已被注册")
    public Result<Boolean> checkPhone(@RequestParam String phone) {
        boolean exists = userService.checkPhoneExist(phone);
        return Result.success(exists);
    }

    @GetMapping("/check/idCard")
    @Operation(summary = "检查身份证是否存在", description = "验证身份证号是否已被注册")
    public Result<Boolean> checkIdCard(@RequestParam String idCard) {
        boolean exists = userService.checkIdCardExist(idCard);
        return Result.success(exists);
    }

    private Long getCurrentUserId() {
        return ThreadLocalUtil.getUserId();
    }
}