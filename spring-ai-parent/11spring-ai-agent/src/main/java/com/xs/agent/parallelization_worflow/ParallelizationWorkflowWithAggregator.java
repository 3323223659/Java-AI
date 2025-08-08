package com.xs.agent.parallelization_worflow;

import org.springframework.ai.chat.client.ChatClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 并行化工作流实现类，支持并行执行多个LLM任务并聚合结果
 * 实现了Spring AI中的并行化工作流模式
 */
public class ParallelizationWorkflowWithAggregator {

    private final ChatClient chatClient; // Spring AI的聊天客户端

    // 风险评估提示词模板
    private static final String RISK_ASSESSMENT_PROMPT = """  
            你是一个风险评估专家，请分析以下部门在数字化转型过程中面临的主要风险：  
              
            请从以下角度分析：  
            1. 技术风险  
            2. 人员风险    
            3. 业务连续性风险  
            4. 预算风险  
            5. 应对建议  
            """;

    /**
     * 构造函数
     * @param chatClient Spring AI的ChatClient实例
     */
    public ParallelizationWorkflowWithAggregator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 并行处理输入并聚合结果的主方法
     * @param inputs 输入列表（如不同部门/地区的名称）
     * @return 包含并行结果和聚合结果的AggregatedResult对象
     */
    public AggregatedResult parallelWithAggregation(List<String> inputs) {
        // 步骤1: 并行处理所有输入
        List<String> parallelResults = parallel(inputs);

        // 步骤2: 聚合所有并行结果
        String aggregatedOutput = aggregateResults(parallelResults);

        return new AggregatedResult(parallelResults, aggregatedOutput);
    }

    /**
     * 并行执行LLM调用的核心方法
     * @param inputs 输入列表
     * @return 并行处理的结果列表
     */
    private List<String> parallel(List<String> inputs) {
        // 创建与输入数量匹配的线程池
        ExecutorService executor = Executors.newFixedThreadPool(inputs.size());

        try {
            // 为每个输入创建CompletableFuture任务
            List<CompletableFuture<String>> futures = inputs.stream()
                    .map(input -> CompletableFuture.supplyAsync(() -> {
                        // 每个任务使用相同的提示词模板但不同的输入内容
                        return chatClient.prompt(RISK_ASSESSMENT_PROMPT + "\n输入内容: " + input)
                                .call()
                                .content();
                    }, executor))
                    .collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(CompletableFuture[]::new));
            allFutures.join();

            // 收集所有任务结果
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

        } finally {
            executor.shutdown(); // 确保线程池关闭
        }
    }

    /**
     * 聚合器：将多个并行结果合并为统一输出
     * @param results 并行处理的结果列表
     * @return 聚合后的综合报告
     */
    private String aggregateResults(List<String> results) {
        // 聚合提示词模板
        String aggregatorPrompt = """  
            你是一个数据聚合专家，请将以下多个分析结果合并为一份综合报告：  
              
            原始分析任务: {originalPrompt}  
              
            各部门/地区分析结果:  
            {results}  
              
            请提供：  
            1. 综合分析摘要  
            2. 共同趋势和模式  
            3. 关键差异对比  
            4. 整体结论和建议  
              
            请生成一份统一的综合报告。  
            """;

        // 合并所有结果，用分隔符分隔
        String combinedResults = String.join("\n\n---\n\n", results);

        // 调用LLM进行结果聚合
        return chatClient.prompt()
                .user(u -> u.text(aggregatorPrompt)
                        .param("originalPrompt", RISK_ASSESSMENT_PROMPT)
                        .param("results", combinedResults))
                .call()
                .content();
    }

    /**
     * 结果记录类，包含并行处理结果和聚合结果
     * @param individualResults 每个输入的独立处理结果
     * @param aggregatedOutput 聚合后的综合输出
     */
    public record AggregatedResult(List<String> individualResults, String aggregatedOutput) {}
}