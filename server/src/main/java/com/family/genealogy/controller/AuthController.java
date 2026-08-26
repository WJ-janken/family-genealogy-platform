package com.family.genealogy.controller;

import com.family.genealogy.common.Result;
import com.family.genealogy.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 后台管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> data = authService.login(request.getUsername(), request.getPassword());
        return Result.success(data);
    }

    /**
     * 微信小程序登录
     */
    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody WxLoginRequest request) {
        Map<String, Object> data = authService.wxLogin(request.getCode());
        return Result.success(data);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user-info")
    public Result<Map<String, Object>> getUserInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> data = authService.getUserInfo(userId);
        return Result.success(data);
    }

    @Data
    static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    static class WxLoginRequest {
        @NotBlank(message = "code不能为空")
        private String code;
    }
}
