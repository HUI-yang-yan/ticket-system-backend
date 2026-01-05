package com.ticket.system.service;

import com.ticket.system.dto.request.UserLoginDTO;
import com.ticket.system.dto.request.UserRegisterDTO;
import com.ticket.system.dto.response.UserInfoDTO;

public interface UserService {
    UserInfoDTO register(UserRegisterDTO userRegisterDTO);
    String login(UserLoginDTO userLoginDTO);
    UserInfoDTO getUserInfo(Long userId);
    void updateUser(UserInfoDTO userInfoDTO);
    void logout();
    boolean checkUsernameExist(String username);
    boolean checkPhoneExist(String phone);
    boolean checkIdCardExist(String idCard);
}