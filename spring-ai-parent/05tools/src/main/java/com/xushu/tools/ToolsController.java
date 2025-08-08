package com.xushu.tools;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工具集成控制器
 * 核心功能：
 * 1. 集成阿里云DashScope大模型
 * 2. 动态注册自定义工具服务
 * 3. 提供RESTful API接口
 */
@RestController
public class ToolsController {

    // ChatClient实例，用于与AI模型交互
    private final ChatClient chatClient;

    /**
     * 构造函数注入
     * @param ChatClientBuilder 自动配置的ChatClient构建器
     * @param toolService 自定义工具服务
     */
    public ToolsController(ChatClient.Builder ChatClientBuilder,
                           ToolService toolService) {
        this.chatClient = ChatClientBuilder
                // 设置默认系统消息（角色定义）
                .defaultSystem("""
                        # 角色
                        你是智能航空客服助手
                        ## 要求
                        严禁随意补全或猜测工具调用参数。参数如缺失或语义不准，
                        请不要补充或随意传递，请直接放弃本次工具调用。
                        """)
                // 注册工具服务（自动发现@Tool注解方法）
                .defaultTools(toolService)
//                .defaultToolCallbacks(toolService.getToolCallList(toolService)) //替代方案：动态工具注册（需手动构建ToolDefinition）
                .build();
    }

    /**
     * 工具调用接口
     * @param message 用户输入消息（默认值："讲个笑话"）
     * @return AI模型响应内容
     */
    @RequestMapping("/tool")
    public String tool(@RequestParam(value = "message", defaultValue = "讲个笑话")
                       String message) {
        return chatClient.prompt()
                .user(message)    // 设置用户输入
                .call()           // 执行模型调用
                .content();       // 获取文本响应
    }
}