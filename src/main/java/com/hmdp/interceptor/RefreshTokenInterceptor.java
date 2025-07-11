package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 刷新 token 状态的拦截器 </br>
 * 如果 token 存在就从 redis 中读取用户信息并放入 {@link UserHolder} 中，同时也会刷新 token 时间 </br>
 * 对所有路径都进行拦截 但一定会放行
 *
 * @author Kraztoria rotroutine@163.com
 * @since 2025/7/11 19:59
 */
@Service
@AllArgsConstructor
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token)) return true;

        String redisKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        if (CollectionUtils.isEmpty(entries)) {
            return true;
        }

        // 更新凭证有效期
        redisTemplate.expire(redisKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        UserDTO userDTO = BeanUtil.toBean(entries, UserDTO.class);
        UserHolder.saveUser(userDTO);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
