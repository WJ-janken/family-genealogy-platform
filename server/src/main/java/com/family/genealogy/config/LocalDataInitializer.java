package com.family.genealogy.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.family.genealogy.entity.SysUser;
import com.family.genealogy.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 本地开发环境数据初始化
 * 确保 admin 用户密码正确
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 确保 admin 用户密码为 admin123
        SysUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
        );
        if (admin != null) {
            String encodedPassword = passwordEncoder.encode("admin123");
            admin.setPassword(encodedPassword);
            userMapper.updateById(admin);
            log.info(">>> 本地开发环境: admin 用户密码已重置为 admin123");
        } else {
            log.warn(">>> 本地开发环境: 未找到 admin 用户");
        }
    }
}
