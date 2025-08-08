package com.xs.langchain4j_springboot.controller;

import com.xs.langchain4j_springboot.config.AiConfig;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.TokenStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * AI聊天功能控制器
 * 提供多种聊天接口：
 * 1. 普通聊天
 * 2. 流式聊天
 * 3. 带记忆的聊天
 * 4. 多用户隔离的聊天
 */
@RestController
@RequestMapping("/ai") // 基础路径
public class ChatController {

    // 注入普通聊天模型（通义千问）
    @Autowired
    QwenChatModel chatModel;

    // 注入流式聊天模型（通义千问）
    @Autowired
    QwenStreamingChatModel streamingChatModel;

    /**
     * 普通聊天接口
     * @param message 聊天消息，默认"你是谁"
     * @return 聊天响应字符串
     */
    @RequestMapping("/chat")
    public String test(@RequestParam(defaultValue="你是谁") String message) {
        // 直接调用模型获取响应
        return chatModel.chat(message);
    }

    /**
     * 流式聊天接口
     * @param message 聊天消息，默认"你是谁"
     * @return 流式响应Flux
     *
     * 注意：produces指定返回text/event-stream格式
     */
    @RequestMapping(value = "/stream", produces = "text/stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam(defaultValue="你是谁") String message) {
        // 创建响应式流
        return Flux.create(fluxSink -> {
            streamingChatModel.chat(message, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    fluxSink.next(partialResponse); // 实时推送部分响应
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    fluxSink.complete(); // 结束流
                }

                @Override
                public void onError(Throwable error) {
                    fluxSink.error(error); // 错误处理
                }
            });
        });
    }

    // 注入带记忆的AI助手（来自AiConfig配置）
    @Autowired
    AiConfig.Assistant assistant;

    /**
     * 带记忆的聊天接口
     * @param message 聊天消息，默认"我叫徐庶"
     * @return 聊天响应
     */
    @RequestMapping(value = "/memory_chat")
    public String memoryChat(@RequestParam(defaultValue="我叫徐庶") String message) {
        return assistant.chat(message); // 自动维护对话历史
    }

    /**
     * 带记忆的流式聊天接口
     * @param message 聊天消息，默认"我是谁"
     * @param response Http响应对象
     * @return 流式响应Flux
     */
    @RequestMapping(value = "/memory_stream_chat", produces ="text/stream;charset=UTF-8")
    public Flux<String> memoryStreamChat(@RequestParam(defaultValue="我是谁") String message,
                                         HttpServletResponse response) {
        // 获取带日期上下文的流
        TokenStream stream = assistant.stream(message, LocalDate.now().toString());

        // 转换为Flux
        return Flux.create(sink -> {
            stream.onPartialResponse(s -> sink.next(s))
                    .onCompleteResponse(c -> sink.complete())
                    .onError(sink::error)
                    .start();
        });
    }

    // 注入支持多用户隔离的AI助手
    @Autowired
    AiConfig.AssistantUnique assistantUnique;

    /**
     * 多用户隔离的聊天接口
     * @param message 聊天消息，默认"我是谁"
     * @param userId 用户ID（用于隔离对话记忆）
     * @return 聊天响应
     */
    @RequestMapping(value = "/memoryId_chat")
    public String memoryChat(@RequestParam(defaultValue="我是谁") String message,
                             Integer userId) {
        return assistantUnique.chat(userId, message); // 基于userId隔离记忆
    }
}