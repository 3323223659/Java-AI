package com.xushu.springai.rag.eval;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

@SpringBootTest
public class FactCheckingTest {

    @Test
    void testFactChecking(@Autowired DashScopeChatModel chatModel) {

        // 创建 FactCheckingEvaluator
        var factCheckingEvaluator = new FactCheckingEvaluator(ChatClient.builder(chatModel));

        // 发送给大模型上下文和声明
        Document doc = Document.builder()
                .text("""
                        取消预订:
                        - 最晚在航班起飞前 48 小时取消。
                        - 取消费用：经济舱 75 美元，豪华经济舱 50 美元，商务舱 25 美元。
                        - 退款将在 7 个工作日内处理。
                        """)
                .build();

        List<Document> documents = List.of(doc);

        // AI回答
        String response = "经济舱取消费用75 美元";

        // 创建 EvaluationRequest
        EvaluationRequest evaluationRequest = new EvaluationRequest(documents, response);

        // 执行评估
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);

        System.out.println(evaluationResponse);
    }


    /**
     * 问题：用户的实际查询
     * 上下文：从向量数据库检索到的相关文档
     * 响应：AI 模型生成的答案
     */
    @Test
    void testRelevancyEvaluator(@Autowired DashScopeChatModel chatModel) {

        // 创建 FactCheckingEvaluator
        var evaluator = new RelevancyEvaluator(ChatClient.builder(chatModel));

        // 示例上下文和声明
        String context = "地球是距离太阳的第三颗行星，也是已知唯一孕育生命的天文物体。";
        String claim = "地球是距离太阳的第四颗行星，也是已知唯一孕育生命的天文物体。";

        // 创建 EvaluationRequest
        EvaluationRequest evaluationRequest = new EvaluationRequest(context, Collections.emptyList(), claim);

        // 执行评估
        EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);


        System.out.println(evaluationResponse);
    }

}
