package com.xushu.springai.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class TestDeepseek {

    /**
     * 测试基础对话功能
     * 使用DeepSeekChatModel的简单调用方式
     * @param deepSeekChatModel 自动注入的DeepSeek聊天模型
     */
    @Test
    public void testDeepseek(@Autowired DeepSeekChatModel deepSeekChatModel) {
        // 直接调用模型获取响应
        String content = deepSeekChatModel.call("你好你是谁");
        System.out.println(content); // 打印模型响应
    }

    /**
     * 测试流式对话功能
     * 使用Flux实现流式响应
     * @param deepSeekChatModel 自动注入的DeepSeek聊天模型
     */
    @Test
    public void testDeepseekStream(@Autowired DeepSeekChatModel deepSeekChatModel) {
        // 获取流式响应
        Flux<String> stream = deepSeekChatModel.stream("你好你是谁");
        // 将流转换为可迭代对象并逐个打印
        stream.toIterable().forEach(System.out::println);
    }

    /**
     * 测试推理模型(deepseek-reasoner)的思维链输出
     * 获取模型推理过程中的中间思考过程
     * @param deepSeekChatModel 自动注入的DeepSeek聊天模型
     */
    @Test
    public void testDeepseekReasoning(@Autowired DeepSeekChatModel deepSeekChatModel) {
        // 创建Prompt对象
        Prompt prompt = new Prompt("你好你是谁");
        // 获取完整响应
        ChatResponse response = deepSeekChatModel.call(prompt);

        // 转换为DeepSeek专用消息类型以获取推理内容
        DeepSeekAssistantMessage assistantMessage = (DeepSeekAssistantMessage)response.getResult().getOutput();

        // 打印思维链(Chain of Thought)内容
        System.out.println(assistantMessage.getReasoningContent());
        System.out.println("-----------------------------------------");
        // 打印最终响应文本
        System.out.println(assistantMessage.getText());
    }

    /**
     * 测试流式推理功能
     * 结合流式响应和思维链输出
     * @param deepSeekChatModel 自动注入的DeepSeek聊天模型
     */
    @Test
    public void testDeepseekStreamReasoning(@Autowired DeepSeekChatModel deepSeekChatModel) {
        // 获取流式响应
        Flux<ChatResponse> stream = deepSeekChatModel.stream(new Prompt("你好你是谁"));

        // 第一部分：打印思维链内容
        stream.toIterable().forEach(chatResponse -> {
            DeepSeekAssistantMessage assistantMessage = (DeepSeekAssistantMessage)chatResponse.getResult().getOutput();
            System.out.println(assistantMessage.getReasoningContent());
        });

        System.out.println("-----------------------------------------");

        // 第二部分：打印最终响应文本
        stream.toIterable().forEach(chatResponse -> {
            DeepSeekAssistantMessage assistantMessage = (DeepSeekAssistantMessage)chatResponse.getResult().getOutput();
            System.out.println(assistantMessage.getText());
        });
    }

    /**
     * 测试聊天选项配置
     * 演示如何通过DeepSeekChatOptions控制模型行为
     * @param chatModel 自动注入的DeepSeek聊天模型
     */
    @Test
    public void testChatOptions(@Autowired DeepSeekChatModel chatModel) {
        // 构建模型选项
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model("deepseek-chat") // 指定模型
                //.maxTokens(5)   // 可设置最大token数
                .stop(Arrays.asList("，")) // 设置停止序列
                .temperature(2.0) // 设置温度参数(0-2)
                .build();

        // 创建带选项的Prompt
        Prompt prompt = new Prompt("请写一句诗描述清晨。", options);

        // 获取模型响应
        ChatResponse res = chatModel.call(prompt);

        // 打印结果
        System.out.println(res.getResult().getOutput().getText());
    }
}