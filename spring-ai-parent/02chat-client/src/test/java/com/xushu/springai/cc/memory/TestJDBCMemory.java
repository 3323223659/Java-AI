package com.xushu.springai.cc.memory;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.xushu.springai.cc.ReReadingAdvisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest
public class TestJDBCMemory {

    // 共享的ChatClient实例
    ChatClient chatClient;

    /**
     * 初始化方法 - 配置带JDBC记忆的ChatClient
     * @param chatModel 自动注入的DashScope聊天模型
     * @param chatMemory 自动注入的ChatMemory实例
     */
    @BeforeEach
    public void init(@Autowired DashScopeChatModel chatModel,
                     @Autowired ChatMemory chatMemory) {
        // 创建带记忆顾问的ChatClient
        this.chatClient = ChatClient
                .builder(chatModel)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build() // 使用JDBC记忆
                )
                .build();
    }

    /**
     * 测试JDBC记忆功能 - 多轮对话
     * 演示如何通过JDBC持久化对话记忆
     */
    @Test
    public void testChatOptions() {
        // 第一轮对话 - 设置用户信息
        String content = chatClient.prompt()
                .user("你好，我叫小小！") // 用户输入
                .advisors(new ReReadingAdvisor()) // 添加重读顾问
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "1")) // 设置对话ID
                .call()
                .content();
        System.out.println(content);
        System.out.println("----------------------------------");

        // 第二轮对话 - 查询用户信息
        content = chatClient.prompt()
                .user("我叫什么？") // 基于记忆的查询
                .advisors(new ReReadingAdvisor()) // 再次添加重读顾问
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "1")) // 相同对话ID
                .call()
                .content();
        System.out.println(content);
    }

    /**
     * JDBC记忆配置类
     */
    @TestConfiguration
    static class Config {
        /**
         * 配置基于JDBC的聊天记忆
         * @param chatMemoryRepository JDBC记忆存储库
         * @return 配置好的ChatMemory实例
         */
        @Bean
        ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
            return MessageWindowChatMemory
                    .builder()
                    .maxMessages(1) // 保留最近1条消息
                    .chatMemoryRepository(chatMemoryRepository) // 使用JDBC存储
                    .build();
        }
    }
}