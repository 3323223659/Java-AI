package com.xushu.springai.rag.ELT;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
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
public class SplitterTest {

    /**
     * 测试基于Token的文本分割器（通用版）
     * @param resource 测试文本资源
     */
    @Test
    public void testTokenTextSplitter(@Value("classpath:rag/terms-of-service.txt") Resource resource) {
        // 1. 读取原始文档
        TextReader textReader = new TextReader(resource);
        List<Document> documents = textReader.read();

        // 2. 使用TokenTextSplitter进行分割
        TokenTextSplitter splitter = new TokenTextSplitter(); // 默认配置
        List<Document> splitDocuments = splitter.apply(documents);

        // 3. 输出分割结果
        splitDocuments.forEach(System.out::println);
    }

    /**
     * 测试中文优化的Token文本分割器
     * @param resource 测试文本资源
     */
    @Test
    public void testChineseTokenTextSplitter(@Value("classpath:rag/terms-of-service.txt") Resource resource) {
        // 1. 读取原始文档
        TextReader textReader = new TextReader(resource);
        List<Document> documents = textReader.read();

        // 2. 使用中文优化分割器
        ChineseTokenTextSplitter splitter = new ChineseTokenTextSplitter(); // 中文专用实现
        List<Document> splitDocuments = splitter.apply(documents);

        // 3. 输出分割结果
        splitDocuments.forEach(System.out::println);
    }

    /**
     * 测试关键词元数据增强器
     * @param vectorStore 向量存储
     * @param chatModel 聊天模型（用于生成关键词）
     * @param resource 测试文本资源
     */
    @Test
    public void testKeywordMetadataEnricher(
            @Autowired VectorStore vectorStore,
            @Autowired DashScopeChatModel chatModel,
            @Value("classpath:rag/terms-of-service.txt") Resource resource) {

        // 1. 读取文档并添加文件名元数据
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", resource.getFilename());
        List<Document> documents = textReader.read();

        // 2. 使用中文分割器预处理
        ChineseTokenTextSplitter splitter = new ChineseTokenTextSplitter();
        documents = splitter.apply(documents);

        // 3. 使用关键词增强器（生成5个关键词）
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(chatModel, 5);
        documents = enricher.apply(documents);

        // 4. 存储到向量数据库
        vectorStore.add(documents);

        // 5. 基于关键词进行检索
        /* 假如允许自定义提示词：KeywordMetadataEnricher.KEYWORDS_TEMPLATE= """
                给我按照我提供的内容{context_str},生成%s个关键字；
                允许的关键字有这些：
                ['退票','预定']
                只允许在这个关键字范围进行选择。
                """;*/
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression("filename in ('退票')") // 文件名过滤
                        .filterExpression("excerpt_keywords in ('退票')") // 关键词过滤
                        .build());

        // 6. 输出检索结果
        for (Document document : results) {
            System.out.println(document.getText());
            System.out.println(document.getText().length());
        }
    }

    /**
     * 测试配置类 - 提供向量存储Bean
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public VectorStore vectorStore(DashScopeEmbeddingModel embeddingModel) {
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }

    /**
     * 测试摘要元数据增强器
     * @param chatModel 聊天模型（用于生成摘要）
     * @param resource 测试文本资源
     */
    @Test
    public void testSummaryMetadataEnricher(
            @Autowired DashScopeChatModel chatModel,
            @Value("classpath:rag/terms-of-service.txt") Resource resource) {

        // 1. 读取文档并添加元数据
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", resource.getFilename());
        List<Document> documents = textReader.read();

        // 2. 使用中文分割器（带精细参数配置）
        ChineseTokenTextSplitter splitter = new ChineseTokenTextSplitter(
                130,   // 块大小
                10,    // 块重叠
                5,     // 最小块大小
                10000, // 最大输入长度
                true   // 是否计算token
        );
        List<Document> splitDocuments = splitter.apply(documents);

        // 3. 应用摘要增强器（生成前/中/后三段摘要）
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(
                chatModel,
                List.of(
                        SummaryMetadataEnricher.SummaryType.PREVIOUS,
                        SummaryMetadataEnricher.SummaryType.CURRENT,
                        SummaryMetadataEnricher.SummaryType.NEXT
                )
        );
        List<Document> enhancedDocuments = enricher.apply(splitDocuments);

        // 4. 输出增强后的文档
        System.out.println(enhancedDocuments);
    }
}