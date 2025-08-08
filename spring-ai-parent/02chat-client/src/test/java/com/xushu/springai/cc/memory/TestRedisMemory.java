package com.xushu.springai.cc.memory;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import com.xushu.springai.cc.ReReadingAdvisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest
public class TestRedisMemory {

    // 共享的ChatClient实例
    ChatClient chatClient;

    /**
     * 初始化方法 - 配置带Redis记忆的ChatClient
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
                        PromptChatMemoryAdvisor.builder(chatMemory).build() // 使用Redis记忆
                )
                .build();
    }

    /**
     * 测试Redis记忆功能 - 多轮对话
     * 演示如何通过Redis持久化对话记忆
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

        // 注释说明后续处理流程
        // MQ 异步处理 ----> 存储向量数据库 ---> 相似性检索

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
     * Redis记忆配置类
     */
    @TestConfiguration
    static class Config {
        // Redis连接配置
        @Value("${spring.ai.memory.redis.host}")
        private String redisHost;
        @Value("${spring.ai.memory.redis.port}")
        private int redisPort;
        @Value("${spring.ai.memory.redis.password}")
        private String redisPassword;
        @Value("${spring.ai.memory.redis.timeout}")
        private int redisTimeout;

        /**
         * 配置Redis记忆存储库
         * @return RedisChatMemoryRepository实例
         */
        @Bean
        public RedisChatMemoryRepository redisChatMemoryRepository() {
            return RedisChatMemoryRepository.builder()
                    .host(redisHost) // Redis主机
                    .port(redisPort) // Redis端口
                    // 若没有设置密码则注释该项
                    // .password(redisPassword) // Redis密码
                    .timeout(redisTimeout) // 连接超时
                    .build();
        }

        /**
         * 配置基于Redis的聊天记忆
         * @param chatMemoryRepository Redis记忆存储库
         * @return 配置好的ChatMemory实例
         */
        @Bean
        ChatMemory chatMemory(RedisChatMemoryRepository chatMemoryRepository) {
            return MessageWindowChatMemory
                    .builder()
                    .maxMessages(10) // 保留最近10条消息
                    .chatMemoryRepository(chatMemoryRepository) // 使用Redis存储
                    .build();
        }
    }
}