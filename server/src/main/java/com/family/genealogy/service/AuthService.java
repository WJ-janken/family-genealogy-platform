package com.family.genealogy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.family.genealogy.common.BusinessException;
import com.family.genealogy.entity.SysUser;
import com.family.genealogy.mapper.SysUserMapper;
import com.family.genealogy.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    /**
     * 后台管理员登录
     */
    public Map<String, Object> login(String username, String password) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", buildUserInfo(user));
        return result;
    }

    /**
     * 微信小程序登录
     */
    public Map<String, Object> wxLogin(String code) {
        // 调用微信接口获取 openid
        String openid = getWxOpenid(code);
        if (openid == null) {
            throw new BusinessException("微信登录失败");
        }

        // 查找或创建用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenid, openid)
        );
        if (user == null) {
            user = new SysUser();
            user.setOpenid(openid);
            user.setRole("MEMBER");
            user.setStatus(1);
            user.setNickname("新用户");
            userMapper.insert(user);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", buildUserInfo(user));
        return result;
    }

    /**
     * 获取当前用户信息
     */
    public Map<String, Object> getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserInfo(user);
    }

    private String getWxOpenid(String code) {
        try {
            String url = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    appid, secret, code
            );
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("openid")) {
                return (String) response.get("openid");
            }
            log.error("微信登录失败: {}", response);
            return null;
        } catch (Exception e) {
            log.error("调用微信接口异常: ", e);
            return null;
        }
    }

    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("nickname", user.getNickname());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("role", user.getRole());
        info.put("roleName", getRoleName(user.getRole()));
        info.put("memberId", user.getMemberId());
        return info;
    }

    private String getRoleName(String role) {
        return switch (role) {
            case "SUPER_ADMIN" -> "超级管理员";
            case "ADMIN" -> "管理员";
            case "MEMBER" -> "家族成员";
            default -> "访客";
        };
    }
}
