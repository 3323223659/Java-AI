package com.xushu.springai.quickstart;

// 导入阿里云AI相关类

import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.*;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.chat.*;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.dashscope.image.*;
import com.alibaba.dashscope.aigc.videosynthesis.*;
import com.alibaba.dashscope.exception.*;
import com.alibaba.dashscope.utils.JsonUtils;

// 导入Spring AI相关类
import org.springframework.ai.audio.transcription.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.content.*;
import org.springframework.ai.image.*;

// 其他导入
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.*;
import org.springframework.util.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

@SpringBootTest
public class TestALI {

    // ========== 1. 基础聊天功能测试 ==========
    @Test
    public void testQwen(@Autowired DashScopeChatModel dashScopeChatModel) {
        // 调用通义千问基础聊天模型
        String content = dashScopeChatModel.call("你好你是谁");
        System.out.println(content);
    }

    // ========== 2. 文生图功能测试 ==========
    @Test
    public void text2Img(@Autowired DashScopeImageModel imageModel) {
        // 配置图像生成选项
        DashScopeImageOptions imageOptions = DashScopeImageOptions.builder()
                .withModel("wanx2.1-t2i-turbo") // 使用万相2.1极速版模型
                .build();

        // 生成图像
        ImageResponse imageResponse = imageModel.call(
                new ImagePrompt("程序员徐庶", imageOptions));

        // 获取结果
        String imageUrl = imageResponse.getResult().getOutput().getUrl();
        System.out.println(imageUrl); // 打印图片URL

        // 可选获取Base64编码
        // imageResponse.getResult().getOutput().getB64Json();
    }

    // ========== 3. 文本转语音测试 ==========
    @Test
    public void testText2Audio(@Autowired DashScopeSpeechSynthesisModel speechSynthesisModel)
            throws IOException {
        // 配置语音合成选项
        DashScopeSpeechSynthesisOptions options = DashScopeSpeechSynthesisOptions.builder()
                .voice("longyingtian")   // 使用"龙应天"音色
                .model("cosyvoice-v2")   // 使用cosyvoice-v2模型
                .build();

        // 合成语音
        SpeechSynthesisResponse response = speechSynthesisModel.call(
                new SpeechSynthesisPrompt("大家好，我是人帅活好的徐庶。", options)
        );

        // 保存为MP3文件
        File file = new File(System.getProperty("user.dir") + "/output.mp3");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer byteBuffer = response.getResult().getOutput().getAudio();
            fos.write(byteBuffer.array());
        }
    }

    // ========== 4. 语音转文本测试 ==========
    private static final String AUDIO_RESOURCES_URL =
            "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";

    @Test
    public void testAudio2Text(@Autowired DashScopeAudioTranscriptionModel transcriptionModel)
            throws MalformedURLException {
        // 配置语音识别选项
        DashScopeAudioTranscriptionOptions transcriptionOptions =
                DashScopeAudioTranscriptionOptions.builder().build();

        // 创建识别请求
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(
                new UrlResource(AUDIO_RESOURCES_URL),
                transcriptionOptions
        );

        // 执行语音识别
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);
        System.out.println(response.getResult().getOutput());
    }

    // ========== 5. 多模态图片理解测试 ==========
    @Test
    public void testMultimodal(@Autowired DashScopeChatModel dashScopeChatModel)
            throws MalformedURLException {
        // 加载图片资源
        var imageFile = new ClassPathResource("/files/xushu.png");
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, imageFile);

        // 配置多模态选项
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withMultiModel(true)
                .withModel("qwen-vl-max-latest") // 使用VL多模态模型
                .build();

        // 创建多模态请求
        Prompt prompt = Prompt.builder()
                .chatOptions(options)
                .messages(UserMessage.builder()
                        .media(media)
                        .text("识别图片")
                        .build())
                .build();

        // 执行多模态理解
        ChatResponse response = dashScopeChatModel.call(prompt);
        System.out.println(response.getResult().getOutput().getText());
    }

    // ========== 6. 多模态语音理解测试 ==========
    @Test
    public void testMultimodalSpeechToText(@Autowired DashScopeChatModel dashScopeChatModel)
            throws MalformedURLException {
        // 加载音频资源
        var audioFile = new ClassPathResource("/files/hello.MP3");
        Media media = new Media(MimeTypeUtils.parseMimeType("audio/mpeg"), audioFile);

        // 配置多模态选项
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withMultiModel(true)
                .withModel("qwen-omni-turbo") // 使用全能模型
                .build();

        // 创建多模态请求(指定消息格式为VIDEO)
        Prompt prompt = Prompt.builder()
                .chatOptions(options)
                .messages(UserMessage.builder()
                        .media(media)
                        .metadata(Map.of(
                                DashScopeApiConstants.MESSAGE_FORMAT,
                                MessageFormat.VIDEO))
                        .text("识别语音文件")
                        .build())
                .build();

        // 执行多模态理解
        ChatResponse response = dashScopeChatModel.call(prompt);
        System.out.println(response.getResult().getOutput().getText());
    }

    // ========== 7. 文本转视频测试(dashscope-sdk-java依赖) ==========
    @Test
    public void text2Video() throws ApiException, NoApiKeyException, InputRequiredException {
        // 创建视频合成实例
        VideoSynthesis vs = new VideoSynthesis();

        // 配置视频合成参数
        VideoSynthesisParam param = VideoSynthesisParam.builder()
                .model("wanx2.1-t2v-turbo") // 使用万相2.1视频模型
                .prompt("一只小猫在月光下奔跑")
                .size("1280*720")           // 视频分辨率
                .apiKey(System.getenv("ALI_AI_KEY"))
                .build();

        System.out.println("please wait...");
        // 执行视频合成
        VideoSynthesisResult result = vs.call(param);
        System.out.println(result.getOutput().getVideoUrl());
    }
}