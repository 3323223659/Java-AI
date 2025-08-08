package com.xushu.springai.cc;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootTest
public class TestAdvisor {

    /**
     * 测试日志顾问 - SimpleLoggerAdvisor
     * 演示如何添加简单的请求/响应日志记录
     *
     * 日志级别设置：
     * 在application.properties中添加：
     * logging.level.org.springframework.ai.chat.client.advisor=DEBUG
     *
     * @param chatClientBuilder 自动注入的ChatClient构建器
     */
    @Test
    public void testLoggerAdvisor(@Autowired ChatClient.Builder chatClientBuilder) {
        // 1. 创建ChatClient并添加日志顾问
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor()) // 添加日志顾问
                .build();

        // 2. 构建并执行聊天请求
        String content = chatClient.prompt()
                .user("你好") // 用户消息
                .call()     // 同步调用
                .content(); // 获取响应

        System.out.println(content);
    }

    /**
     * 测试安全防护顾问 - SafeGuardAdvisor
     * 演示如何拦截包含敏感词的请求
     *
     * @param chatClientBuilder 自动注入的ChatClient构建器
     * @param chatMemory 自动注入的聊天记忆组件
     */
    @Test
    public void testAdvisor(@Autowired ChatClient.Builder chatClientBuilder,
                            @Autowired ChatMemory chatMemory) {
        // 1. 创建ChatClient并添加多个顾问
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),    // 日志顾问
                        new SafeGuardAdvisor(List.of("小小")) // 安全顾问，设置敏感词"小小"
                )
                .build();

        // 2. 构建并执行包含敏感词的请求
        String content = chatClient.prompt()
                .user("小小是谁") // 包含敏感词的查询
                .call()
                .content();

        System.out.println(content);
    }

    /**
     * 测试重读顾问 - ReReadingAdvisor
     * 演示自定义顾问如何修改用户提问
     *
     * @param chatClientBuilder 自动注入的ChatClient构建器
     */
    @Test
    public void testReReadingAdvisor(@Autowired ChatClient.Builder chatClientBuilder) {
        // 1. 创建ChatClient并添加自定义重读顾问
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(), // 日志顾问
                        new ReReadingAdvisor()     // 自定义重读顾问
                )
                .build();

        // 2. 构建并执行请求
        String content = chatClient.prompt()
                .user("小小是谁") // 原始问题
                .call()
                .content();

        System.out.println(content);

        // 预期效果：AI会收到"小小是谁\nRead the question again: 小小是谁"
    }
}