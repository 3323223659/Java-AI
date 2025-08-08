package com.xushu.springai.more;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;

@RestController
public class MorPlatformAndModelController {

    HashMap<String, ChatModel> platforms=new HashMap<>();

    public MorPlatformAndModelController(
            DashScopeChatModel dashScopeChatModel,
            DeepSeekChatModel deepSeekChatModel,
            OllamaChatModel ollamaChatModel
    ) {
        platforms.put("dashscope", dashScopeChatModel);
        platforms.put("ollama", ollamaChatModel);
        platforms.put("deepseek", deepSeekChatModel);
    }

    @RequestMapping(value="/chat",produces = "text/stream;charset=UTF-8")
    public Flux<String> chat(
            String message,
            MorePlatformAndModelOptions  options){

        String platform = options.getPlatform();
        ChatModel chatModel = platforms.get(platform);

        ChatClient.Builder builder = ChatClient.builder(chatModel);

        ChatClient chatClient = builder.defaultOptions(
                ChatOptions.builder()
                        .temperature(options.getTemperature())
                        .model(options.getModel())
                        .build()
        ).build();

        Flux<String> content = chatClient.prompt().user(message).stream().content();

        return content;

    }
}
