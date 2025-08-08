package com.xushu.springai.rag.ELT;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class ReaderTest {

    /**
     * 测试纯文本文件读取器
     * @param resource 通过@Value注入的文本文件资源
     */
    @Test
    public void testReaderText(@Value("classpath:rag/terms-of-service.txt") Resource resource) {
        // 创建文本阅读器实例
        TextReader textReader = new TextReader(resource);
        // 读取文档内容
        List<Document> documents = textReader.read();

        // 遍历并打印文档内容
        for (Document document : documents) {
            System.out.println(document.getText());
        }
    }

    /**
     * 测试Markdown文件读取器
     * @param resource 通过@Value注入的Markdown文件资源
     */
    @Test
    public void testReaderMD(@Value("classpath:rag/9_横店影视股份有限公司_0.md") Resource resource) {
        // 配置Markdown解析选项
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(false)     // 是否将分割线作为文档分隔符
                .withIncludeCodeBlock(false)                // 是否包含代码块作为独立文档
                .withIncludeBlockquote(false)               // 是否包含引用块作为独立文档
                .withAdditionalMetadata("filename", resource.getFilename()) // 添加文件名元数据
                .build();

        // 创建Markdown阅读器实例
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
        // 读取文档内容
        List<Document> documents = markdownDocumentReader.read();

        // 遍历并打印文档内容
        for (Document document : documents) {
            System.out.println(document.getText());
        }
    }

    /**
     * 测试PDF按页读取器
     * @param resource 通过@Value注入的PDF文件资源
     */
    @Test
    public void testReaderPdf(@Value("classpath:rag/平安银行2023年半年度报告摘要.pdf") Resource resource) {
        // 创建PDF按页阅读器实例（默认配置）
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder().build());

        // 读取文档内容（每页作为一个文档）
        List<Document> documents = pdfReader.read();

        // 遍历并打印文档内容
        for (Document document : documents) {
            System.out.println(document.getText());
        }
    }

    /**
     * 测试PDF按段落读取器（需要PDF包含目录结构）
     * @param resource 通过@Value注入的PDF文件资源
     */
    @Test
    public void testReaderParagraphPdf(@Value("classpath:rag/平安银行2023年半年度报告.pdf") Resource resource) {
        // 配置PDF段落解析选项
        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        // 修正某些PDF工具的坐标系问题
                        .withReversedParagraphPosition(true)
                        // 设置页面上边距
                        .withPageTopMargin(0)
                        // 配置文本提取格式
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                // 设置删除页面顶部N行（处理页眉等）
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .build());

        // 读取文档内容（按段落/章节作为文档）
        List<Document> documents = pdfReader.read();

        // 遍历并打印文档内容
        for (Document document : documents) {
            System.out.println(document.getText());
        }
    }
}