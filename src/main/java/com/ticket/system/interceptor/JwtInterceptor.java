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

    // Token自动续期阈值（分钟），当剩余有效期小于此值时自动续期
    private static final int TOKEN_RENEW_THRESHOLD_MINUTES = 30;

    // Token续期后新的过期时间（小时）
    private static final int TOKEN_RENEW_EXPIRE_HOURS = 24;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(jwtUtil.getHeader());

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

            // 检查是否需要自动续期
            long expireTime = jwtUtil.getExpireTimeFromToken(token);
            long currentTime = System.currentTimeMillis();
            long remainingTimeMinutes = (expireTime - currentTime) / (1000 * 60);

            if (remainingTimeMinutes < TOKEN_RENEW_THRESHOLD_MINUTES) {
                // Token快过期了，自动续期
                log.debug("Token即将过期，自动续期: userId={}, 剩余{}分钟", userId, remainingTimeMinutes);

                // 生成新Token
                String newToken = jwtUtil.generateToken(userId, username);

                // 更新Redis缓存
                String newRedisKey = "user:token:" + newToken;
                redisUtil.set(newRedisKey, userInfo, TOKEN_RENEW_EXPIRE_HOURS * 60 * 60L, java.util.concurrent.TimeUnit.SECONDS);

                // 删除旧Token的缓存
                redisUtil.delete(redisKey);

                // 通过Response Header返回新Token
                response.setHeader("Token-Renew", "true");
                response.setHeader("Authorization", newToken);

                log.info("Token自动续期成功: userId={}", userId);
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