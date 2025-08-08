package com.itheima.consultant.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT, // 显式装配模式（避免Spring自动注入冲突）
        chatModel = "openAiChatModel",             // 同步聊天模型Bean名称（需在配置中定义）
        streamingChatModel = "openAiStreamingChatModel", // 流式模型Bean名称
        //chatMemory = "chatMemory",                // 直接指定记忆存储（已注释，改用provider）
        chatMemoryProvider = "chatMemoryProvider", // 记忆提供者Bean（动态获取记忆）
        contentRetriever = "contentRetriever",    // RAG检索器Bean（连接向量数据库）
        tools = "reservationTool"                 // 工具类Bean（如预订系统）
)
public interface ConsultantService {
    // 用于聊天的方法
    //public String chat(String message);
    //@SystemMessage("你是东哥的助手小月月,人美心善又多金!")

    // 从资源文件中读取系统消息
    @SystemMessage(fromResource = "system.txt")
    //@UserMessage("你是东哥的助手小月月,人美心善又多金!{{it}}")
    //@UserMessage("你是东哥的助手小月月,人美心善又多金!{{msg}}")
    public Flux<String> chat(/*@V("msg")*/@MemoryId String memoryId, @UserMessage String message);
}
