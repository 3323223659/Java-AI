package com.xs.agent.orchestrator_workers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 简单任务编排与执行处理器
 * 使用AI模型分解复杂任务并协调专业工作者完成任务
 */
public class SimpleOrchestratorWorkers {

    // 聊天客户端，用于与AI模型交互
    private final ChatClient chatClient;

    /**
     * 编排器提示词模板（中文）
     * 用于指导AI如何分解复杂任务
     * 使用文本块语法（Java 15+特性）保持格式
     */
    private static final String ORCHESTRATOR_PROMPT = """  
                你是一个项目管理专家，需要将复杂任务分解为可并行执行的专业子任务。
                    任务: {task}
                    请分析任务的复杂性和专业领域需求，将其分解为2-4个需要不同专业技能的子任务。
                    每个子任务应该：
                    1. 有明确的专业领域（如：前端开发、后端API、数据库设计、测试等）
                    2. 可以独立执行
                    3. 有具体的交付物
                    
                    请以JSON格式回复：
                    {
                        "analysis": "任务复杂度分析和分解策略",
                        "tasks": [
                            {
                                "type": "后端API开发",
                                "description": "设计并实现RESTful API接口，包括数据验证和错误处理"
                            },
                            {
                                "type": "前端界面开发",
                                "description": "创建响应式用户界面，实现与后端API的交互"
                            },
                            {
                                "type": "数据库设计",
                                "description": "设计数据表结构，编写SQL脚本和索引优化"
                            }
                        ]
                    }
            """;

    /**
     * 工作者提示词模板（中文）
     * 用于指导AI如何完成特定专业领域的子任务
     */
    private static final String WORKER_PROMPT = """  
            你是一个{task_type}领域的资深专家，请完成以下专业任务：
              项目背景: {original_task}
              专业领域: {task_type}
              具体任务: {task_description}
              
              请按照行业最佳实践完成任务，包括：
              1. 技术选型和架构考虑
              2. 具体实现方案
              3. 潜在风险和解决方案
              4. 质量保证措施
              
              请提供专业、详细的解决方案。
            """;

    /**
     * 构造函数
     * @param chatClient Spring AI的聊天客户端，用于与AI模型交互
     */
    public SimpleOrchestratorWorkers(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 处理任务的主方法
     * 1. 先使用编排器分解任务
     * 2. 然后使用工作者处理各个子任务
     * @param taskDescription 原始任务描述
     */
    public void process(String taskDescription) {
        System.out.println("=== 开始处理任务 ===");

        // 步骤1: 使用编排器分析并分解任务
        // 通过ChatClient发送提示词并获取响应
        OrchestratorResponse orchestratorResponse = chatClient.prompt()
                .system(p -> p.param("task", taskDescription))  // 设置系统参数
                .user(ORCHESTRATOR_PROMPT)  // 设置用户提示词
                .call()  // 执行调用
                .entity(OrchestratorResponse.class);  // 将响应映射为OrchestratorResponse对象

        // 打印编排器分析结果
        System.out.println("编排器分析: " + orchestratorResponse.analysis());
        System.out.println("子任务列表: " + orchestratorResponse.tasks());

        // 步骤2: 并行处理各个子任务
        orchestratorResponse.tasks().stream()
                .map(task -> {
                    // 打印当前处理的子任务信息
                    System.out.println("-----------------------------------处理子任务: " + task.type()+"--------------------------------");
                    // 调用工作者处理子任务
                    String content = chatClient.prompt()
                            .user(u -> u.text(WORKER_PROMPT)
                                    .param("original_task", taskDescription)  // 原始任务
                                    .param("task_type", task.type())  // 子任务类型
                                    .param("task_description", task.description()))  // 子任务描述
                            .call()
                            .content();  // 获取响应内容
                    System.out.println(content);
                    return task;
                }).toList();  // 收集结果（虽然这里没有使用结果）

        System.out.println("=== 所有工作者完成任务 ===");
    }

    // 以下是数据记录类（Java 14+ record特性）

    /**
     * 子任务记录类
     * @param type 任务类型（如"后端API开发"）
     * @param description 任务描述
     */
    public record Task(String type, String description) {}

    /**
     * 编排器响应记录类
     * @param analysis 任务分析结果
     * @param tasks 分解后的子任务列表
     */
    public record OrchestratorResponse(String analysis, List<Task> tasks) {}

    /**
     * 最终响应记录类（当前未使用）
     * @param analysis 分析结果
     * @param workerResponses 工作者响应列表
     */
    public record FinalResponse(String analysis, List<String> workerResponses) {}
}