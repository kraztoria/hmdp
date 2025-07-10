package com.hmdp.service.impl;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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

        // 3. 保存验证码到 session 中
        session.setAttribute("code", code);

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
        String cachedCode = (String) session.getAttribute("code");
        if (null == cachedCode || !cachedCode.equals(code)) {
            log.warn("[login] {} 用户输入的验证码错误 输入验证码: {} 期望验证码: {}", phone, code, cachedCode);
            return Result.fail("验证码错误");
        }

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

        // 将用户信息保存到 session 中 并删除验证码信息
        session.removeAttribute("code");
        session.setAttribute("user", user);
        log.info("[login] {} 用户登录成功", phone);
        return Result.ok();
    }
}
