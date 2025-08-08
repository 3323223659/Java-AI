package com.xushu.springai.cc.memory;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest
public class TestMemory {

    /**
     * 测试基础记忆功能 - 手动拼接对话历史
     * 演示如何通过字符串拼接实现简单记忆
     *
     * @param chatModel 自动注入的DashScope聊天模型
     */
    @Test
    public void testMemory(@Autowired DashScopeChatModel chatModel) {
        // 1. 创建基础ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // 2. 第一次交互
        String chatHis = "我叫小小";
        String content = chatClient.prompt()
                .user(chatHis)
                .call()
                .content();
        System.out.println(content);
        System.out.println("----------------------------------");

        // 3. 第二次交互（手动拼接历史）
        chatHis += content;
        chatHis += "我叫什么？";
        content = chatClient.prompt()
                .user(chatHis) // 包含完整对话历史
                .call()
                .content();
        System.out.println(content);
    }

    /**
     * 测试ChatMemory实现 - 使用MessageWindowChatMemory
     * 演示Spring AI提供的标准记忆实现
     *
     * @param chatModel 自动注入的DashScope聊天模型
     */
    @Test
    public void testMemory2(@Autowired DashScopeChatModel chatModel) {
        // 1. 创建记忆组件（窗口大小默认为10）
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        String conversationId = "xx001"; // 对话唯一标识

        // 2. 第一次交互
        UserMessage userMessage1 = new UserMessage("我叫小小");
        chatMemory.add(conversationId, userMessage1); // 添加用户消息到记忆
        ChatResponse response1 = chatModel.call(new Prompt(chatMemory.get(conversationId)));
        chatMemory.add(conversationId, response1.getResult().getOutput()); // 添加AI响应到记忆

        // 3. 第二次交互
        UserMessage userMessage2 = new UserMessage("我叫什么?");
        chatMemory.add(conversationId, userMessage2);
        ChatResponse response2 = chatModel.call(new Prompt(chatMemory.get(conversationId)));
        chatMemory.add(conversationId, response2.getResult().getOutput());
        System.out.println(response2.getResult().getOutput().getText()); // 打印AI响应
    }

    // 共享的ChatClient实例
    ChatClient chatClient;

    /**
     * 初始化方法 - 配置带记忆顾问的ChatClient
     */
    @BeforeEach
    public void init(@Autowired ChatClient.Builder builder,
                     @Autowired ChatMemory chatMemory) {
        // 创建带记忆顾问的ChatClient
        this.chatClient = builder
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build() // 添加记忆顾问
                )
                .build();
    }

    /**
     * 测试记忆顾问 - PromptChatMemoryAdvisor
     * 演示如何通过顾问自动管理对话历史
     */
    @Test
    public void testMemoryAdvisor(@Autowired ChatMemory chatMemory) {
        // 1. 第一次交互
        String content = chatClient.prompt()
                .user("我叫小小")
                .call()
                .content();
        System.out.println(content);
        System.out.println("----------------------------------");

        // 2. 第二次交互（自动使用记忆）
        content = chatClient.prompt()
                .user("我叫什么？")
                .call()
                .content();
        System.out.println(content);
    }

    /**
     * 测试配置类 - 自定义记忆实现
     */
    @TestConfiguration
    static class Config {
        /**
         * 配置消息窗口记忆
         * @param chatMemoryRepository 记忆存储库
         * @return 配置好的ChatMemory实例
         */
        @Bean
        ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
            return MessageWindowChatMemory
                    .builder()
                    .maxMessages(1) // 只保留最近1条消息
                    .chatMemoryRepository(chatMemoryRepository)
                    .build();
        }
    }

    /**
     * 测试对话ID选项 - 多会话记忆隔离
     * 演示如何通过不同对话ID隔离记忆
     */
    @Test
    public void testChatOptions() {
        // 会话1 - 第一次交互
        String content = chatClient.prompt()
                .user("我叫小小")
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "1"))
                .call()
                .content();
        System.out.println(content);
        System.out.println("----------------------------------");

        // 会话1 - 第二次交互（有记忆）
        content = chatClient.prompt()
                .user("我叫什么？")
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "1"))
                .call()
                .content();
        System.out.println(content);

        System.out.println("----------------------------------");

        // 会话2 - 新会话（无记忆）
        content = chatClient.prompt()
                .user("我叫什么？")
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "2"))
                .call()
                .content();
        System.out.println(content);
    }
}