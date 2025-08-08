package com.itheima.consultant.config;

import com.itheima.consultant.aiservice.ConsultantService;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * LangChain4j 核心组件配置类
 * 功能：
 * 1. 配置对话记忆管理（Redis持久化）
 * 2. 初始化向量数据库（RAG支持）
 * 3. 构建AI服务基础设施
 */
@Configuration
public class CommonConfig {
    @Autowired
    private OpenAiChatModel model; // OpenAI 聊天模型（同步）
    @Autowired
    private ChatMemoryStore redisChatMemoryStore; // Redis实现的对话记忆存储
    @Autowired
    private EmbeddingModel embeddingModel; // 文本嵌入模型（如text-embedding-v3）
    @Autowired
    private RedisEmbeddingStore redisEmbeddingStore; // Redis向量数据库
    /*@Bean
    public ConsultantService consultantService(){
        ConsultantService consultantService = AiServices.builder(ConsultantService.class)
                .chatModel(model)
                .build();
        return consultantService;
    }*/

    /**
     * 对话记忆配置（单会话）
     * @return 基于内存的对话记忆，保留最近20条消息
     *
     * 生产建议：改用RedisChatMemoryStore实现持久化
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20) // 记忆窗口大小
                .build();
    }

    /**
     * 对话记忆提供者（多会话支持）
     * @return 根据memoryId动态创建记忆实例
     *
     * 工作流程：
     * 1. 根据memoryId从Redis加载历史消息
     * 2. 新会话自动初始化空记忆
     * 3. 对话更新后自动持久化到Redis
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId) // 会话唯一标识（通常为用户ID）
                .maxMessages(20) // 每个会话的记忆容量
                .chatMemoryStore(redisChatMemoryStore) // Redis存储后端
                .build();
    }

    // 构建向量数据库操作对象
    // TODO: (第一次启动用这个@bean，后面存到redis中有数据就可以去掉了，但是如果是上传知识库的业务可以参考我的其他文章src/main/resources/static/index.html)
    //@Bean
    public EmbeddingStore store(){//embeddingStore的对象, 这个对象的名字不能重复,所以这里使用store
        //1.加载文档进内存
        //List<Document> documents = ClassPathDocumentLoader.loadDocuments("content");
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content",new ApachePdfBoxDocumentParser());
        //List<Document> documents = FileSystemDocumentLoader.loadDocuments("C:\\Users\\Administrator\\ideaProjects\\consultant\\src\\main\\resources\\content");
        //2.构建向量数据库操作对象  操作的是内存版本的向量数据库
        //InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();

        //构建文档分割器对象
        DocumentSplitter ds = DocumentSplitters.recursive(500,100);
        //3.构建一个EmbeddingStoreIngestor对象,完成文本数据切割,向量化, 存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                //.embeddingStore(store)
                .embeddingStore(redisEmbeddingStore)
                .documentSplitter(ds)
                .embeddingModel(embeddingModel)
                .build();
        ingestor.ingest(documents);
        return redisEmbeddingStore;
    }

    // 构建向量数据库检索对象
    @Bean
    public ContentRetriever contentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore) // 指定向量库
                .minScore(0.5)    // 最小相似度阈值
                .maxResults(3)    // 返回结果数
                .embeddingModel(embeddingModel) // 查询向量化模型
                .build();
    }
}
