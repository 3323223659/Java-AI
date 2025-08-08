package com.xushu.springai.rag;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.apache.el.lang.ExpressionBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootTest
public class ChatClientRagTest {

    ChatClient chatClient;
    @BeforeEach
    public void init(
            @Autowired DashScopeChatModel chatModel,
            @Autowired VectorStore vectorStore) {
        Document doc = Document.builder()
                .text("""
                        预订航班:
                        - 通过我们的网站或移动应用程序预订。
                        - 预订时需要全额付款。
                        - 确保个人信息（姓名、ID 等）的准确性，因为更正可能会产生 25 的费用。
                        """)
                .build();
        Document doc2 = Document.builder()
                .text("""
                        取消预订:
                        - 最晚在航班起飞前 48 小时取消。
                        - 取消费用：经济舱 75 美元，豪华经济舱 50 美元，商务舱 25 美元。
                        - 退款将在 7 个工作日内处理。
                        """)
                .build();

        List<Document> documents = List.of(doc, doc2);


        // 存储向量（内部会自动向量化)
        vectorStore.add(documents);


    }


    @Test
    public void testRag(
            @Autowired DashScopeChatModel dashScopeChatModel,
            @Autowired VectorStore vectorStore) {


        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();

        String content = chatClient.prompt()
                .user("退票需要多少费用？")
                .advisors(
                        SimpleLoggerAdvisor.builder().build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                       SearchRequest.builder()
                                               .topK(5)
                                               .similarityThreshold(0.6)
                                               .build()
                                ).build()
                )
                .call()
                .content();



        System.out.println(content);
    }



    @Test
    public void testRag2(
            @Autowired DashScopeChatModel dashScopeChatModel,
            @Autowired VectorStore vectorStore) {


        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();
        //FilterExpression基于元数据过滤搜索结果的参数
        String content = chatClient.prompt()
                .user("退票需要多少费用？")
                .advisors(
                        SimpleLoggerAdvisor.builder().build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(5)
                                                .similarityThreshold(0.1)
                                                //.filterExpression()
                                                .build()
                                ).build()
                )
                .call()
                .content();



        System.out.println(content);
    }


    @Test
    public void testRag3(@Autowired VectorStore vectorStore,
                        @Autowired DashScopeChatModel dashScopeChatModel) {


        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .build();

        // 增强多
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                // 查 = QuestionAnswerAdvisor
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.0)
//                        .topK()
//                        .filterExpression()
                        .vectorStore(vectorStore)
                        .build())
                // 检索为空时，allowEmptyContext=false返回提示   allowEmptyContext=true 正常回答
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false)
                        .emptyContextPromptTemplate(PromptTemplate.builder().template("用户查询位于知识库之外。礼貌地告知用户您无法回答").build())
                        .build())
                //  检索查询转换器
                // 重写检索查询转换器(将用户的不必要的内容如情绪去除，增强向量得分)
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(ChatClient.builder(dashScopeChatModel))
                        .targetSearchSystem("航空票务助手")
                        .build())
                // 翻译转换器
                .queryTransformers(TranslationQueryTransformer.builder()
                                    .chatClientBuilder(ChatClient.builder(dashScopeChatModel))
                                    .targetLanguage("english")
                                    .build())
                // 检索后文档监控、操作
                .documentPostProcessors((query, documents) -> {

                    System.out.println("Original query: " + query.text());
                    System.out.println("Retrieved documents: " + documents.size());
                    return documents;
                })
                .build();

        String answer = chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user("我今天心情不好，不想去玩了，你能不能告诉我退票需要多少钱？")
                .call()
                .content();

        System.out.println(answer);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public VectorStore vectorStore(DashScopeEmbeddingModel embeddingModel) {
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }



}
