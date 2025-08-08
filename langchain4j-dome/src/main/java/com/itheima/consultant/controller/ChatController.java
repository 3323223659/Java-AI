package com.itheima.consultant.controller;

import com.itheima.consultant.aiservice.ConsultantService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI对话控制器
 * 实现三种不同版本的AI对话接口演进：
 * 1. 直接调用OpenAI模型（基础版）
 * 2. 通过Service层同步调用（业务隔离版）
 * 3. 响应式流式调用（生产推荐版）
 */
@RestController
public class ChatController {

    @Autowired
    private ConsultantService consultantService; // 封装AI业务逻辑的服务层

    /**
     * 流式对话接口（生产环境推荐）
     * @param memoryId 会话记忆ID（实现多轮对话上下文）
     * @param message 用户输入消息
     * @return Flux流式响应（SSE协议兼容）
     *
     * 技术要点：
     * 1. produces明确指定UTF-8编码，解决中文乱码
     * 2. 返回Flux实现逐词输出效果
     * 3. memoryId支持对话上下文保持
     *
     * 调用示例：
     * GET /chat?memoryId=123&message=你好
     */
    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String memoryId, String message) {
        return consultantService.chat(memoryId, message);
    }

    /* 演进版本1：直接调用模型（已废弃）
    @Autowired
    private OpenAiChatModel model;

    @RequestMapping("/chat")
    public String chat(String message) {
        // 问题：直接暴露模型调用，无业务逻辑隔离
        return model.chat(message);
    }*/

    /* 演进版本2：同步服务调用（过渡方案）
    @RequestMapping("/chat")
    public String chat(String message) {
        // 改进：通过Service层隔离业务逻辑
        return consultantService.chat(message);
    }*/
}