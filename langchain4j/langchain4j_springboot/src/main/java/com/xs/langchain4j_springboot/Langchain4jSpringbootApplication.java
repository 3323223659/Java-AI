package com.xs.langchain4j_springboot;

// 导入相关依赖...

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class Langchain4jSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(Langchain4jSpringbootApplication.class, args);
    }

    /**
     * 在Spring应用完全启动后执行,在所有Bean初始化完成后执行,在应用程序开始接收外部请求前执行
     * 文档向量化存储的初始化Bean
     * 功能：在应用启动时自动将文档分割、向量化并存储到向量数据库
     *
     * @param embeddingStore 向量存储接口（由Spring自动注入）
     * @param qwenEmbeddingModel 通义千问嵌入模型（用于文本向量化）
     * @return 命令行运行器
     */
    @Bean
    CommandLineRunner ingestTermOfServiceToVectorStore(
            EmbeddingStore embeddingStore,
            QwenEmbeddingModel qwenEmbeddingModel) {

        return args -> {
            // 1. 从classpath加载文档
            // 参数说明：
            // "rag/ragKnowledge.txt" - 文档相对路径(resources包下)
            // new TextDocumentParser() - 使用纯文本解析器
            Document document = ClassPathDocumentLoader.loadDocument(
                    "rag/ragKnowledge.txt",
                    new TextDocumentParser());

            // 2. 文档分割配置
            // 使用按行分割器，参数：
            // 150 - 每行最大字符数
            // 30 - 行与行之间的重叠字符数（保持上下文连贯）
            DocumentByLineSplitter splitter = new DocumentByLineSplitter(
                    150,  // maxSegmentSizeInChars
                    30    // maxOverlapSizeInChars
            );

            // 执行文档分割
            List<TextSegment> segments = splitter.split(document);

            // 3. 向量化处理
            // 使用通义千问模型批量生成嵌入向量
            List<Embedding> embeddings = qwenEmbeddingModel.embedAll(segments).content();

            // 4. 存储到向量数据库
            // 将向量和对应的文本片段存入向量存储
            embeddingStore.addAll(embeddings, segments);
        };
    }
}