package com.xushu.springai.rag;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootTest
public class VectorStoreTest {

    @Test
    public void testVectorStore(@Autowired VectorStore vectorStore) {
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

        // 存储向量（内部会自动向量化)
        vectorStore.add(List.of(doc, doc2));

        SearchRequest searchRequest = SearchRequest.builder()
                .query("退票")
                .topK(2)
                .similarityThreshold(0.5)

                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        for (Document document : documents) {
            System.out.println(document.getText());
            System.out.println(document.getScore());
        }
    }

}
