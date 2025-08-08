package com.xs.agent.evaluator_optimizer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.evaluation.EvaluationResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码评估与优化处理器
 * 通过生成-评估-改进的迭代循环不断优化代码质量
 */
public class SimpleEvaluatorOptimizer {

    // Spring AI的聊天客户端，用于与AI模型交互
    private final ChatClient chatClient;

    /**
     * 代码生成器提示词模板（中文）
     * 指导AI如何生成和改进Java代码
     * 使用文本块语法（Java 15+特性）保持格式
     */
    private static final String GENERATOR_PROMPT = """
        你是一个Java代码生成助手。请根据任务要求生成高质量的Java代码。
        重要提醒：
        - 第一次生成时，创建一个基础但完整的实现  
        - 如果收到反馈，请仔细分析每一条建议并逐一改进  
        - 每次迭代都要在前一版本基础上显著提升代码质量  
        - 不要一次性实现所有功能，而是逐步完善  
          
        必须以JSON格式回复：  
        {"thoughts":"详细说明本轮的改进思路","response":"改进后的Java代码"}  
            """;

    /**
     * 代码评估器提示词模板（中文）
     * 指导AI如何严格评估代码质量
     */
    private static final String EVALUATOR_PROMPT = """  
        你是一个非常严格的面试官。请从以下维度严格评估代码：
            1. 代码是否高效：从底层分析每一个类型以满足最佳性能！
            2. 满足不重复扩容影响的性能
            评估标准：
            - 只有当代码满足要求达到优秀水平时才返回PASS
            - 如果任何一个维度有改进空间，必须返回NEEDS_IMPROVEMENT 
            - 提供具体、详细的改进建议  
              
            必须以JSON格式回复：  
            {"evaluation":"PASS或NEEDS_IMPROVEMENT或FAIL","feedback":"详细的分维度反馈"}  
              
            记住：宁可严格也不要放松标准！ 
        """;

    /**
     * 构造函数
     * @param chatClient Spring AI的聊天客户端
     */
    public SimpleEvaluatorOptimizer(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // 迭代计数器
    int iteration = 0;
    // 上下文信息，用于在迭代间传递信息
    String context = "";

    /**
     * 主循环方法，执行生成-评估-改进的迭代过程
     * @param task 需要完成的任务描述
     * @return 最终优化后的代码解决方案
     */
    public RefinedResponse loop(String task) {
        System.out.println("=== 第" + (iteration + 1) + "轮迭代 ===");

        // 1. 生成代码阶段
        Generation generation = generate(task,context);

        // 2. 评估代码阶段
        EvaluationResponse evaluation = evaluate(generation.response(), task);
        System.out.println("生成结果: " + generation.response());
        System.out.println("评估结果: " + evaluation.evaluation());
        System.out.println("反馈: " + evaluation.feedback());

        // 3. 检查是否通过评估
        if (evaluation.evaluation() == EvaluationResponse.Evaluation.PASS) {
            System.out.println("代码通过评估！");
            return new RefinedResponse(generation.response());
        }
        else{
            // 准备下一轮的上下文（包含前一轮的代码和反馈）
            context = String.format("之前的尝试:\n%s\n\n评估反馈:\n%s\n\n请根据反馈改进代码。",
                    generation.response(), evaluation.feedback());
            iteration++;
            // 递归调用继续迭代
            return loop(task);
        }
    }

    /**
     * 生成代码方法
     * @param task 任务描述
     * @param context 上下文信息（前一轮的代码和反馈）
     * @return 生成的代码和思考过程
     */
    private Generation generate(String task, String context) {
        return chatClient.prompt()
                .user(u -> u.text("{prompt}\n{context}\n任务: {task}")
                        .param("prompt", GENERATOR_PROMPT)  // 生成器提示词
                        .param("context", context)  // 上下文信息
                        .param("task", task))  // 任务描述
                .call()
                .entity(Generation.class);  // 映射为Generation对象
    }

    /**
     * 评估代码方法
     * @param content 需要评估的代码内容
     * @param task 原始任务描述
     * @return 评估结果和反馈
     */
    private EvaluationResponse evaluate(String content, String task) {
        return chatClient.prompt()
                .user(u -> u.text("{prompt}\n\n任务: {task}\n\n代码:\n{content}")
                        .param("prompt", EVALUATOR_PROMPT)  // 评估器提示词
                        .param("task", task)  // 任务描述
                        .param("content", content))  // 需要评估的代码
                .call()
                .entity(EvaluationResponse.class);  // 映射为EvaluationResponse对象
    }

    // 以下是数据记录类（Java 14+ record特性）

    /**
     * 代码生成结果记录类
     * @param thoughts 生成过程中的思考过程
     * @param response 生成的代码
     */
    public static record Generation(String thoughts, String response) {}

    /**
     * 评估结果记录类
     * @param evaluation 评估结果枚举
     * @param feedback 详细的反馈信息
     */
    public static record EvaluationResponse(Evaluation evaluation, String feedback) {
        /**
         * 评估结果枚举
         * PASS: 通过评估
         * NEEDS_IMPROVEMENT: 需要改进
         * FAIL: 失败
         */
        public enum Evaluation { PASS, NEEDS_IMPROVEMENT, FAIL }
    }

    /**
     * 最终优化结果记录类
     * @param solution 优化后的解决方案
     */
    public static record RefinedResponse(String solution) {}
}