package com.xs.langchain4j_springboot.config;

import com.xs.langchain4j_springboot.service.ToolsService;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.*;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j AI助手配置类
 * 功能：
 * 1. 配置不同类型的AI助手（普通助手、带记忆助手、持久化记忆助手）
 * 2. 管理对话记忆和内容检索机制
 * 3. 集成外部工具服务和嵌入模型
 */
@Configuration
public class AiConfig {

    /**
     * 基础AI助手接口定义
     * 提供聊天和流式聊天功能
     *
     * @SystemMessage 定义系统提示词模板，包含：
     *   - 角色定位（航空公司客服）
     *   - 行为准则（友好、乐于助人）
     *   - 必要信息收集要求（预订号、客户姓名）
     *   - 语言要求（中文）
     *   - 动态日期变量 {{current_date}}
     */
    public interface Assistant {
        // 普通聊天接口
        String chat(String message);

        // 流式聊天接口
        TokenStream stream(String message);

        // 带动态变量的流式聊天接口
        @SystemMessage("""
                您是“Tuling”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                您正在通过在线聊天系统与客户互动。
                在提供有关预订或取消预订的信息之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
                请讲中文。
                今天的日期是 {{current_date}}.
                """)
        TokenStream stream(@UserMessage String message,
                           @V("current_date") String currentDate);
    }

    /**
     * 内存嵌入存储 - 用于向量检索
     * @return 内存嵌入存储实例
     */
    @Bean
    public EmbeddingStore embeddingStore() {
        return new InMemoryEmbeddingStore(); // 基于内存的向量存储
    }

    /**
     * 基础助手配置
     * @param qwenChatModel 通义千问聊天模型
     * @param qwenStreamingChatModel 通义千问流式聊天模型
     * @param toolsService 工具服务（自定义工具）
     * @param embeddingStore 向量存储
     * @param qwenEmbeddingModel 通义千问嵌入模型
     * @return 配置完成的AI助手实例
     */
    @Bean
    public Assistant assistant(
            ChatLanguageModel qwenChatModel,
            StreamingChatLanguageModel qwenStreamingChatModel,
            ToolsService toolsService,
            EmbeddingStore embeddingStore,
            QwenEmbeddingModel qwenEmbeddingModel
    ) {
        // 对话记忆配置 - 最多保留10条对话历史
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 内容检索器配置 - 用于RAG（检索增强生成）
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)       // 向量存储
                .embeddingModel(qwenEmbeddingModel)   // 嵌入模型
                .maxResults(5)  // 返回最相似的5个结果
                .minScore(0.6)  // 相似度阈值0.6以上
                .build();

        // 构建AI助手服务
        Assistant assistant = AiServices.builder(Assistant.class)
                .tools(toolsService)                  // 注入自定义工具
                .contentRetriever(contentRetriever)   // 设置内容检索器
                .chatLanguageModel(qwenChatModel)      // 设置聊天模型
                .streamingChatLanguageModel(qwenStreamingChatModel) // 设置流式聊天模型
                .chatMemory(chatMemory)               // 设置对话记忆
                .build();

        return assistant;
    }

    /**
     * 带唯一记忆标识的AI助手接口
     * 支持基于memoryId的多会话管理
     */
    public interface AssistantUnique {
        // 普通聊天接口
        String chat(@MemoryId int memoryId, @UserMessage String userMessage);

        // 流式聊天接口
        TokenStream stream(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    /**
     * 基础版带记忆的AI助手（内存存储）
     * @param qwenChatModel 聊天模型
     * @param qwenStreamingChatModel 流式聊天模型
     * @return 配置完成的AI助手实例
     */
    @Bean
    public AssistantUnique assistantUnique(
            ChatLanguageModel qwenChatModel,
            StreamingChatLanguageModel qwenStreamingChatModel
    ) {
        // 构建带记忆的AI助手
        AssistantUnique assistant = AiServices.builder(AssistantUnique.class)
                .chatLanguageModel(qwenChatModel)      // 聊天模型
                .streamingChatLanguageModel(qwenStreamingChatModel) // 流式模型
                // 记忆提供者 - 基于memoryId创建独立记忆窗口
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .maxMessages(10)  // 最多10条记忆
                                .id(memoryId)     // 唯一标识
                                .build()
                )
                .build();

        return assistant;
    }

    /**
     * 持久化存储版带记忆的AI助手
     * @param qwenChatModel 聊天模型
     * @param qwenStreamingChatModel 流式聊天模型
     * @return 配置完成的AI助手实例
     */
    @Bean
    public AssistantUnique assistantUniqueStore(
            ChatLanguageModel qwenChatModel,
            StreamingChatLanguageModel qwenStreamingChatModel
    ) {
        // 自定义持久化存储实现
        PersistentChatMemoryStore store = new PersistentChatMemoryStore();

        // 记忆提供者 - 支持持久化存储
        ChatMemoryProvider chatMemoryProvider = memoryId ->
                MessageWindowChatMemory.builder()
                        .id(memoryId)             // 唯一标识
                        .maxMessages(10)          // 最多10条记忆
                        .chatMemoryStore(store)   // 持久化存储
                        .build();

        // 构建带持久化记忆的AI助手
        AssistantUnique assistant = AiServices.builder(AssistantUnique.class)
                .chatLanguageModel(qwenChatModel)      // 聊天模型
                .streamingChatLanguageModel(qwenStreamingChatModel) // 流式模型
                .chatMemoryProvider(chatMemoryProvider) // 持久化记忆提供者
                .build();

        return assistant;
    }
}