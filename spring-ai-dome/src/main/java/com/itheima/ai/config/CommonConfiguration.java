package com.itheima.ai.config;

import com.itheima.ai.constants.SystemConstants;
import com.itheima.ai.model.AlibabaOpenAiChatModel;
import com.itheima.ai.tools.CourseTools;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.openai.OpenAiChatProperties;
import org.springframework.ai.autoconfigure.openai.OpenAiConnectionProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;

/**
 * AI核心配置类
 * 功能：配置多种AI聊天客户端及基础设施组件
 *
 * 核心组件：
 * 1. 聊天记忆管理（ChatMemory）
 * 2. 向量存储（VectorStore）
 * 3. 多种场景的ChatClient实例
 * 4. 定制化的AlibabaOpenAiChatModel
 *
 * 设计特点：
 * - 支持多场景对话配置（游戏/服务/PDF问答等）
 * - 集成Spring AI生态组件
 * - 支持可观测性（Observation）
 */
@Configuration
public class CommonConfiguration {

    /**
     * 内存式聊天记忆存储
     * @return InMemoryChatMemory 实例
     *
     * 作用：保存对话上下文，实现多轮对话能力
     * 实现原理：基于ConcurrentHashMap的线程安全实现
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    /**
     * 向量存储配置
     * @param embeddingModel 嵌入模型（用于文本向量化）
     * @return SimpleVectorStore 实例
     *
     * 应用场景：
     * - 文档语义搜索
     * - PDF内容检索
     */
    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 通用聊天客户端
     * @param model 阿里云OpenAI模型
     * @param chatMemory 聊天记忆
     * @return 配置好的ChatClient
     *
     * 默认配置：
     * - 使用qwen-omni-turbo模型
     * - 设定AI人格为"小团团"
     * - 启用日志记录和记忆功能
     */
    @Bean
    public ChatClient chatClient(AlibabaOpenAiChatModel model, ChatMemory chatMemory) {
//        String systemMsg = String.format("你是一个热心、可爱的智能助手，你的名字叫小团团，请以小团团的身份和语气回答问题,今天的日期是%s。", LocalDate.now());
        return ChatClient
                .builder(model)
                .defaultOptions(ChatOptions.builder().model("qwen-omni-turbo").build())
                .defaultSystem("你是一个热心、可爱的智能助手，你的名字叫小团团，请以小团团的身份和语气回答问题,今天的日期是：{current_date}。")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),  // 日志记录
                        new MessageChatMemoryAdvisor(chatMemory) // 记忆功能
                )
                .build();
    }

    /**
     * 游戏场景聊天客户端
     * @param model OpenAI模型
     * @param chatMemory 聊天记忆
     * @return 游戏专用ChatClient
     *
     * 特点：
     * - 使用预定义的游戏系统提示词
     */
    @Bean
    public ChatClient gameChatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    /**
     * 客服场景聊天客户端
     * @param model 阿里云OpenAI模型
     * @param chatMemory 聊天记忆
     * @param courseTools 课程查询工具
     * @return 客服专用ChatClient
     *
     * 扩展能力：
     * - 集成课程查询工具（ToolCalling）
     */
    @Bean
    public ChatClient serviceChatClient(AlibabaOpenAiChatModel model, ChatMemory chatMemory, CourseTools courseTools) {
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.SERVICE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .defaultTools(courseTools)  // 工具调用能力
                .build();
    }

    /**
     * PDF文档问答客户端
     * @param model OpenAI模型
     * @param chatMemory 聊天记忆
     * @param vectorStore 向量存储
     * @return PDF专用ChatClient
     *
     * 核心机制：
     * - 基于向量相似度检索（相似度阈值0.6，返回Top2结果）
     */
    @Bean
    public ChatClient pdfChatClient(OpenAiChatModel model, ChatMemory chatMemory, VectorStore vectorStore) {
        return ChatClient
                .builder(model)
                .defaultSystem("请根据上下文回答问题，遇到上下文没有的问题，不要随意编造。")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(  // 向量检索增强
                                vectorStore,
                                SearchRequest.builder()
                                        .similarityThreshold(0.6)  // 相似度阈值(大于才符合)
                                        .topK(2)  // 返回结果数
                                        .build()
                        )
                )
                .build();
    }

    /**
     * 定制化阿里云OpenAI模型
     * @return AlibabaOpenAiChatModel 实例
     *
     * 配置要点：
     * 1. 支持多级参数继承（chatProperties > commonProperties）
     * 2. 自动配置HTTP客户端（RestClient/WebClient）
     * 3. 集成可观测性体系
     */
    @Bean
    public AlibabaOpenAiChatModel alibabaOpenAiChatModel(
            OpenAiConnectionProperties commonProperties,
            OpenAiChatProperties chatProperties,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider,
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate,
            ResponseErrorHandler responseErrorHandler,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatModelObservationConvention> observationConvention) {

        // 参数优先级处理
        String baseUrl = StringUtils.hasText(chatProperties.getBaseUrl())
                ? chatProperties.getBaseUrl()
                : commonProperties.getBaseUrl();

        String apiKey = StringUtils.hasText(chatProperties.getApiKey())
                ? chatProperties.getApiKey()
                : commonProperties.getApiKey();

        // 组织头信息配置
        Map<String, List<String>> connectionHeaders = new HashMap<>();
        Optional.ofNullable(chatProperties.getProjectId())
                .filter(StringUtils::hasText)
                .ifPresent(projectId ->
                        connectionHeaders.put("OpenAI-Project", List.of(projectId)));

        Optional.ofNullable(chatProperties.getOrganizationId())
                .filter(StringUtils::hasText)
                .ifPresent(orgId ->
                        connectionHeaders.put("OpenAI-Organization", List.of(orgId)));

        // 构建OpenAI API客户端
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))
                .headers(CollectionUtils.toMultiValueMap(connectionHeaders))
                .completionsPath(chatProperties.getCompletionsPath())
                .embeddingsPath("/v1/embeddings")
                .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
                .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
                .responseErrorHandler(responseErrorHandler)
                .build();

        // 构建定制化聊天模型
        AlibabaOpenAiChatModel chatModel = AlibabaOpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatProperties.getOptions())
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();

        // 配置可观测性约定
        observationConvention.ifAvailable(chatModel::setObservationConvention);

        return chatModel;
    }
}