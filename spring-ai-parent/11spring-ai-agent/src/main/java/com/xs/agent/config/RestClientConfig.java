package com.xs.agent.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.xs.agent.evaluator_optimizer.SimpleEvaluatorOptimizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * REST客户端配置类
 * 配置全局的RestClient实例，用于HTTP请求处理
 * 主要功能：
 * 1. 设置统一的连接和读取超时时间
 * 2. 配置默认的请求工厂
 */
@Configuration
public class RestClientConfig {

    /**
     * 创建可配置的RestClient.Builder实例
     * 使用原型模式（prototype），每次获取都是新的Builder实例
     *
     * @param restClientBuilderConfigurer Spring Boot提供的配置器
     * @return 配置好的RestClient.Builder实例
     *
     * 配置说明：
     * - 读取超时：600秒（适用于长时间运行的AI模型请求）
     * - 连接超时：600秒（确保复杂网络环境下的连接稳定性）
     */
    @Bean
    @Scope("prototype") // 每次依赖注入时创建新实例
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
        // 创建基础Builder并配置请求工厂
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withReadTimeout(Duration.ofSeconds(600))  // 设置读取超时
                                .withConnectTimeout(Duration.ofSeconds(600))  // 设置连接超时
                ));

        // 应用Spring Boot的自动配置
        return restClientBuilderConfigurer.configure(builder);
    }
}