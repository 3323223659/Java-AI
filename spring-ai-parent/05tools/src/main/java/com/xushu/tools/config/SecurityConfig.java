package com.xushu.tools.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类
 * 核心功能：
 * 1. 内存用户存储配置
 * 2. 端点访问控制
 * 3. 方法级安全注解支持
 */
@Configuration
@EnableMethodSecurity // 启用方法级安全控制（如@PreAuthorize）
public class SecurityConfig {

    /**
     * 内存用户配置（生产环境应连接数据库）
     * @return UserDetailsService实例
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // 创建普通用户
        UserDetails user = User.withUsername("user1")
                .password("pass1")
                .roles("USER") // 分配USER角色
                .build();

        // 创建管理员用户
        UserDetails admin = User.withUsername("admin")
                .password("pass2")
                .roles("ADMIN") // 分配ADMIN角色
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    /**
     * 安全过滤链配置
     * @param http HttpSecurity构建器
     * @return 配置好的SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 授权配置
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/tool").permitAll() // 开放工具端点
                        .anyRequest().authenticated() // 其他请求需认证
                )
                // 启用默认表单登录（测试用）
                .with(new FormLoginConfigurer<>(), Customizer.withDefaults());

        return http.build();
    }

    /**
     * 密码编码器（仅测试用）
     * 生产环境必须使用BCryptPasswordEncoder等安全编码器
     * @return NoOpPasswordEncoder实例
     */
    @Bean
    public static org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // 明文密码（不安全！）
    }
}