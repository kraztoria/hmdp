package com.hmdp.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final UserMapper userMapper;

    private final StringRedisTemplate redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {

        // 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            log.warn("[sendCode] 手机号格式错误 {}", phone);
            return Result.fail("手机号格式错误");
        }

        // 2. 生成6位验证码
        String code = RandomUtil.randomNumbers(6);
        if (RegexUtils.isCodeInvalid(code)) {
            log.error("[sendCode] 验证码格式错误 {}", code);
            throw new RuntimeException("验证码格式错误");
        }

        // 3. 保存验证码到 redis 中并设定有效时间
        redisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 4. 发送短信验证码
        log.info("[sendCode] 给 {} 发送验证码成功 {}", phone, code);

        return Result.ok(code);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        // 验证手机号和验证码的合法性
        if (RegexUtils.isPhoneInvalid(phone) || RegexUtils.isCodeInvalid(code)) {
            log.warn("[login] 手机号或验证码格式错误 {} {}", phone, code);
            return Result.fail("手机号或验证码格式错误");
        }

        // 校验验证码
        String cachedCode = redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (null == cachedCode || !cachedCode.equals(code)) {
            log.warn("[login] {} 用户输入的验证码错误 输入验证码: {} 期望验证码: {}", phone, code, cachedCode);
            return Result.fail("验证码错误");
        }
        // 删除验证码
        redisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + phone);

        // 根据手机号查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        // 不存在，创建新用户
        if (null == user) {
            log.info("[login] {} 用户为新用户，创建新用户", phone);

            user = new User();
            user.setPhone(phone);
            user.setNickName((String) PhoneUtil.hideBetween(phone));

            // 将用户插入数据库中
            userMapper.insert(user);
        }

        // 生成 token
        String token = IdUtil.fastSimpleUUID();

        // 将用户信息保存到 redis 中
        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("nickName", user.getNickName());
        userMap.put("icon", user.getIcon());
        redisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, userMap);
        redisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        log.info("[login] {} 用户登录成功", phone);
        return Result.ok(token);
    }
}
