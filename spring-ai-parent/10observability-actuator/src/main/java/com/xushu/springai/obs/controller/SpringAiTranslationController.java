package com.xushu.springai.obs.controller;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.*;

/**
 * 多语言翻译API控制器
 * 基于Spring AI大模型能力实现智能翻译功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SpringAiTranslationController {

    private final ChatClient chatClient;

    /**
     * 智能翻译接口
     * @param request 翻译请求体（包含原文和语言对）
     * @return 标准化翻译响应
     */
    @PostMapping("/translate")
    public TranslationResponse translate(@RequestBody TranslationRequest request) {
        // 日志记录输入参数（生产环境建议脱敏）
        log.info("翻译请求: {} -> {} | 文本长度: {}",
                request.getSourceLanguage(),
                request.getTargetLanguage(),
                request.getText().length());

        // 构造大模型提示词（加入风格保留指令）
        String prompt = String.format("""
                作为专业翻译助手，请严格遵循以下要求：
                1. 将%s文本翻译为%s
                2. 保持原文的专业术语准确性
                3. 保留原文的语气风格（正式/口语化等）
                4. 处理文化差异表达
                
                待翻译内容：
                %s
                """,
                request.getSourceLanguage(),
                request.getTargetLanguage(),
                request.getText());

        // 调用大模型（带日志记录）
        String translatedText = chatClient.prompt()
                .user(prompt)
                .advisors(SimpleLoggerAdvisor.builder().build()) // 请求日志记录
                .call()
                .content();

        // 构建标准化响应
        return TranslationResponse.builder()
                .originalText(request.getText())
                .translatedText(translatedText.trim()) // 去除模型可能添加的额外换行
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 建议添加的异常处理（示例）
    @ExceptionHandler(Exception.class)
    public TranslationResponse handleError(Exception ex) {
        log.error("翻译服务异常", ex);
        return TranslationResponse.builder()
                .translatedText("翻译服务暂时不可用: " + ex.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

/**
 * 翻译请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TranslationRequest {
    @NonNull
    private String text;
    @NonNull
    private String sourceLanguage; // 建议使用ISO 639-1标准代码（如zh/en）
    @NonNull
    private String targetLanguage;
}

/**
 * 翻译响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TranslationResponse {
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private Long timestamp; // 响应时间戳（可用于审计）
}