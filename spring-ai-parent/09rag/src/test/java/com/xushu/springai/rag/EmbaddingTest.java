package com.xushu.springai.rag;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootTest
public class EmbaddingTest {

    @Test
    public void testEmbadding(@Autowired OllamaEmbeddingModel
                                          ollamaEmbeddingModel) {

        float[] embedded = ollamaEmbeddingModel.embed("我叫小小");
        System.out.println(embedded.length);
        System.out.println(Arrays.toString(embedded));

    }


    @Test
    public void testAliEmbadding(@Autowired DashScopeEmbeddingModel
                                      embeddingModel) {
        float[] embedded = embeddingModel.embed("我叫小小");
        System.out.println(embedded.length);
        System.out.println(Arrays.toString(embedded));

    }

}