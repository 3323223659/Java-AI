package com.xushu.springai.cc;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import java.util.Map;

/**
 * 重读顾问实现 - 在用户提问前自动添加重读提示
 * 实现BaseAdvisor接口，提供请求前/后的处理能力
 */
public class ReReadingAdvisor implements BaseAdvisor {

    // 默认的重读提示模板
    // 使用{re2_input_query}作为占位符，将被实际用户问题替换
    private static final String DEFAULT_USER_TEXT_ADVISE = """
      {re2_input_query}
      Read the question again: {re2_input_query}
      """;

    /**
     * 请求前处理方法 - 修改用户提问
     * @param chatClientRequest 原始聊天请求
     * @param advisorChain 顾问链
     * @return 修改后的聊天请求
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 获取原始用户提示内容
        String contents = chatClientRequest.prompt().getContents();

        // 使用PromptTemplate渲染模板，将用户问题插入到重读提示中
        String re2InputQuery = PromptTemplate.builder()
                .template(DEFAULT_USER_TEXT_ADVISE)
                .build()
                .render(Map.of("re2_input_query", contents));

        // 构建新的请求，替换原始提示内容
        ChatClientRequest clientRequest = chatClientRequest.mutate()
                .prompt(Prompt.builder().content(re2InputQuery).build())
                .build();

        return clientRequest;
    }

    /**
     * 请求后处理方法 - 本实现不做处理直接返回响应
     * @param chatClientResponse 聊天响应
     * @param advisorChain 顾问链
     * @return 原始响应
     */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    /**
     * 获取顾问执行顺序
     * @return 执行顺序(数值越小优先级越高)
     */
    @Override
    public int getOrder() {
        return 0; // 最高优先级
    }
}