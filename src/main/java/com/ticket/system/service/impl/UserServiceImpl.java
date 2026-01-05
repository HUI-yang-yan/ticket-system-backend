package com.ticket.system.service.impl;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.JwtUtil;
import com.ticket.system.common.util.RedisUtil;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.UserLoginDTO;
import com.ticket.system.dto.request.UserRegisterDTO;
import com.ticket.system.dto.response.UserInfoDTO;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    @Transactional
    public UserInfoDTO register(UserRegisterDTO userRegisterDTO) {
        // 校验用户名是否存在
        if (checkUsernameExist(userRegisterDTO.getUsername())) {
            throw new BusinessException(ErrorCode.USER_EXIST.getCode(), "用户名已存在");
        }

        // 校验手机号是否存在
        if (checkPhoneExist(userRegisterDTO.getPhone())) {
            throw new BusinessException(ErrorCode.USER_EXIST.getCode(), "手机号已注册");
        }

        // 校验身份证是否存在
        if (checkIdCardExist(userRegisterDTO.getIdCard())) {
            throw new BusinessException(ErrorCode.USER_EXIST.getCode(), "身份证已注册");
        }

        // 创建用户实体
        User user = new User();
        BeanUtils.copyProperties(userRegisterDTO, user);

        // 密码加密
        String encryptedPassword = (userRegisterDTO.getPassword());
        user.setPassword(encryptedPassword);

        // 设置默认值
        user.setUserType(0); // 普通用户
        user.setStatus(1);   // 正常状态
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        // 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "注册失败");
        }

        // 返回用户信息
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        BeanUtils.copyProperties(user, userInfoDTO);

        return userInfoDTO;
    }

    @Override
    public String login(UserLoginDTO userLoginDTO) {
        // 查询用户
        User user = userMapper.selectByUsername(userLoginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST.getCode(), "用户不存在");
        }

        // 校验密码
        String encryptedPassword =(userLoginDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR.getCode(), "密码错误");
        }

        // 校验用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED.getCode(), "用户已被禁用");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 缓存用户信息
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        BeanUtils.copyProperties(user, userInfoDTO);
        redisUtil.set("user:token:" + token, userInfoDTO, 24 * 60 * 60L, java.util.concurrent.TimeUnit.SECONDS);
        return token;
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST.getCode(), "用户不存在");
        }

        UserInfoDTO userInfoDTO = new UserInfoDTO();
        BeanUtils.copyProperties(user, userInfoDTO);

        return userInfoDTO;
    }

    @Override
    public void updateUser(UserInfoDTO userInfoDTO) {
        User user = userMapper.selectById(userInfoDTO.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST.getCode(), "用户不存在");
        }

        // 更新用户信息
        user.setRealName(userInfoDTO.getRealName());
        user.setPhone(userInfoDTO.getPhone());
        user.setEmail(userInfoDTO.getEmail());
        user.setUpdateTime(new Date());

        int result = userMapper.update(user);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "更新用户信息失败");
        }
    }

    @Override
    public void logout() {
        UserInfoDTO user = ThreadLocalUtil.getUser();
        if (user != null) {
            redisUtil.delete("user:token:" + user.getId());
        }
    }

    @Override
    public boolean checkUsernameExist(String username) {
        User user = userMapper.selectByUsername(username);
        return user != null;
    }

    @Override
    public boolean checkPhoneExist(String phone) {
        User user = userMapper.selectByPhone(phone);
        return user != null;
    }

    @Override
    public boolean checkIdCardExist(String idCard) {
        User user = userMapper.selectByIdCard(idCard);
        return user != null;
    }
}