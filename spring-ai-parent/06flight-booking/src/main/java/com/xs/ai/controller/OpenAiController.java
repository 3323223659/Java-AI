package com.xs.ai.controller;


import com.xs.ai.services.ToolsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;


/**
 * @author wx:程序员徐庶
 * @version 1.0
 */
@RestController
@CrossOrigin
public class OpenAiController {

    ChatClient chatClient;
    VectorStore vectorStore;

    public OpenAiController(ChatClient.Builder chatClientBuilder,
                            ChatMemory chatMemory,
                            ToolsService toolsService,
                            ToolCallbackProvider toolCallbackProvider,
                            VectorStore vectorStore
                           ) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
						##角色
                          您是“图灵”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                          您正在通过在线聊天系统与客户互动。
                        ##要求
                          1.在涉及增删改（除了查询）function-call前，必须等用户回复“确认”后再调用tool。
                          2.请讲中文。
                          
                          今天的日期是 {current_date}.
					""")
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .defaultTools(toolsService)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();


        this.vectorStore=vectorStore;
    }

    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(
            @RequestParam(value = "message", defaultValue = "讲个笑话") String message) {

        return chatClient.prompt().user(message)
                .system(p -> p.param("current_date", LocalDate.now()))
                .advisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(5)
                                                .similarityThreshold(0.4)
                                                .build())
                                .build()
                )
                .stream().content();
    }

}
