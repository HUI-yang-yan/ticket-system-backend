package com.ticket.system.interceptor;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.JwtUtil;
import com.ticket.system.common.util.RedisUtil;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.response.UserInfoDTO;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(jwtUtil.getHeader());

//        if (StringUtils.hasText(token)) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "未登录或 Token 缺失"
            );
        }
        try {

            // 验证Token是否过期
            if (jwtUtil.isTokenExpired(token)) {
                throw new BusinessException(ErrorCode.TOKEN_EXPIRED.getCode(), "Token已过期");
            }

            // 解析Token
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = (String) claims.get("username");

            // 从Redis中获取用户信息
            String redisKey = "user:token:" + token;
            UserInfoDTO userInfo = (UserInfoDTO) redisUtil.get(redisKey);

            if (userInfo == null) {
                // 重新查询数据库获取用户信息
                userInfo = new UserInfoDTO();
                userInfo.setId(userId);
                userInfo.setUsername(username);

                // 缓存到Redis
                redisUtil.set(redisKey, userInfo, 24 * 60 * 60L, java.util.concurrent.TimeUnit.SECONDS);
            }

            // 设置到ThreadLocal
            ThreadLocalUtil.setUser(userInfo);
            log.debug("用户认证成功: userId={}, username={}", userId, username);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "Token无效");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清除ThreadLocal
        ThreadLocalUtil.removeUser();
    }
}