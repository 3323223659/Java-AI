package com.xushu.springai.rag.eval;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootTest
public class RagEvalTest {
    @Test
    public void testRag(
            @Autowired VectorStore vectorStore,
    @Autowired DashScopeChatModel dashScopeChatModel) {

        List<Document> documents = List.of(
                new Document("""
                        1. 预订航班
                        - 通过我们的网站或移动应用程序预订。
                        - 预订时需要全额付款。
                        - 确保个人信息（姓名、ID 等）的准确性，因为更正可能会产生 25 的费用。
                        """),
                new Document("""
                        2. 更改预订
                        - 允许在航班起飞前 24 小时更改。
                        - 通过在线更改或联系我们的支持人员。
                        - 改签费：经济舱 50，豪华经济舱 30，商务舱免费。
                        """),
                new Document("""
                        3. 取消预订
                        - 最晚在航班起飞前 48 小时取消。
                        - 取消费用：经济舱 75 美元，豪华经济舱50美元，商务舱25美元。
                        - 退款将在 7 个工作日内处理。
                        """));

        vectorStore.add(documents);


        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .build())
                .build();

        String query = "我叫什么名字";
        ChatResponse chatResponse = ChatClient.builder(dashScopeChatModel)
                .build().prompt(query).advisors(retrievalAugmentationAdvisor).call().chatResponse();

        /**
         * 1：用户的实际查询
         * 2：从向量数据库检索到的相关文档
         * 3：AI 模型生成的答案
         */
        EvaluationRequest evaluationRequest = new EvaluationRequest(
                // The original user question
                query,
                // The retrieved context from the RAG flow
                chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT),
                // The AI model's response
                chatResponse.getResult().getOutput().getText()
        );

        RelevancyEvaluator evaluator = new RelevancyEvaluator(ChatClient.builder(dashScopeChatModel));
        EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);
        System.out.println(evaluationResponse);
        System.out.println(chatResponse.getResult().getOutput().getText());
    }


    @TestConfiguration
    static class TestConfig {

        @Bean
        public VectorStore vectorStore(OllamaEmbeddingModel embeddingModel) {
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }
}
