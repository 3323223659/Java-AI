package com.xushu.springai.rag;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.xushu.springai.rag.ELT.ChineseTokenTextSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootTest
public class RerankTest {

    @BeforeEach
    public void init(
            @Autowired VectorStore vectorStore,
            @Value("classpath:rag/terms-of-service.txt") Resource resource) {
        // 读取
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", resource.getFilename());
        List<Document> documents = textReader.read();


        // 分隔
        ChineseTokenTextSplitter splitter = new ChineseTokenTextSplitter(80,10,5,10000,true);
        List<Document> apply = splitter.apply(documents);


        // 存储向量（内部会自动向量化)
        vectorStore.add(apply);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public VectorStore vectorStore(OllamaEmbeddingModel embeddingModel) {
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }

    @Test
    public void testRerank(
            @Autowired VectorStore vectorStore,
           @Autowired DashScopeRerankModel dashScopeRerankModel,
           @Autowired DashScopeChatModel dashScopeChatModel
    ) {

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();

        RetrievalRerankAdvisor retrievalRerankAdvisor = new RetrievalRerankAdvisor(
                vectorStore, dashScopeRerankModel,
                SearchRequest.builder().topK(200).build());

        String content = chatClient.prompt()
                .user("退费费用？")
                .advisors(retrievalRerankAdvisor)
                .call()
                .content();

        System.out.println(content);
    }

}
