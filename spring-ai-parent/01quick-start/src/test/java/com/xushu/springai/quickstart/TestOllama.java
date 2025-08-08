package com.xushu.springai.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatProperties;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

@SpringBootTest
public class TestOllama {

    @Test
    public void testOllama(
            @Autowired OllamaChatModel ollamaChatModel
            ) {
        ///no_think (关闭思考模式)

        System.out.println(ollamaChatModel.call("你好你是谁?/no_think"));
    }

    @Test
    public void testDeepseekStream(@Autowired OllamaChatModel ollamaChatModel) {
        Flux<String> stream = ollamaChatModel.stream("你好你是谁");
        stream.toIterable().forEach(System.out::println);
    }

    /**
     * 多模态  图像识别，  采用的gemma3
     * @param ollamaChatModel
     */
    @Test
    public void testMultimodality(@Autowired OllamaChatModel ollamaChatModel) {
        var imageResource = new ClassPathResource("files/xushu.png");

        OllamaOptions ollamaOptions = OllamaOptions.builder()
                .model("gemma3")
                .build();

        Media media = new Media(MimeTypeUtils.IMAGE_PNG, imageResource);


        ChatResponse response = ollamaChatModel.call(
                new Prompt(
                        UserMessage.builder().media(media)
                                .text("识别图片").build(),
                        ollamaOptions
                )
        );

        System.out.println(response.getResult().getOutput().getText());
    }
}


