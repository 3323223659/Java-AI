package com.itheima.ai.repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;

/**
 * 本地PDF文件存储仓库实现类
 * 功能：
 * 1. PDF文件的本地存储管理
 * 2. 会话与文件的映射关系维护
 * 3. 向量存储的持久化与恢复
 *
 * 设计特点：
 * - 使用Properties文件维护会话ID与文件名的映射
 * - 实现VectorStore的自动加载/保存
 * - 支持文件资源的本地存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPdfFileRepository implements FileRepository {

    // 向量存储接口（实际使用SimpleVectorStore实现）
    private final VectorStore vectorStore;

    // 维护会话ID与PDF文件名的映射关系
    // Key: 会话ID, Value: PDF文件名
    private final Properties chatFiles = new Properties();

    /**
     * 保存文件到本地并记录映射关系
     * @param chatId 会话ID
     * @param resource PDF文件资源
     * @return 是否保存成功
     */
    @Override
    public boolean save(String chatId, Resource resource) {
        // 1. 保存文件到本地磁盘
        String filename = resource.getFilename();
        File target = new File(Objects.requireNonNull(filename));

        // 避免重复保存已存在的文件
        if (!target.exists()) {
            try {
                Files.copy(resource.getInputStream(), target.toPath());
            } catch (IOException e) {
                log.error("PDF文件保存失败", e);
                return false;
            }
        }

        // 2. 记录会话与文件的映射关系
        chatFiles.put(chatId, filename);
        return true;
    }

    /**
     * 根据会话ID获取文件资源
     * @param chatId 会话ID
     * @return 对应的PDF文件资源
     */
    @Override
    public Resource getFile(String chatId) {
        return new FileSystemResource(chatFiles.getProperty(chatId));
    }

    /**
     * 初始化方法 - 在Bean创建后自动执行
     * 功能：
     * 1. 加载历史会话文件映射
     * 2. 恢复向量存储数据
     */
    @PostConstruct
    private void init() {
        // 1. 加载会话-文件映射关系
        FileSystemResource pdfResource = new FileSystemResource("chat-pdf.properties");
        if (pdfResource.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pdfResource.getInputStream(), StandardCharsets.UTF_8))) {
                chatFiles.load(reader);
            } catch (IOException e) {
                throw new RuntimeException("会话映射关系加载失败", e);
            }
        }

        // 2. 加载向量存储数据
        FileSystemResource vectorResource = new FileSystemResource("chat-pdf.json");
        if (vectorResource.exists()) {
            SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;
            simpleVectorStore.load(vectorResource);
        }
    }

    /**
     * 销毁方法 - 在Bean销毁前自动执行
     * 功能：
     * 1. 持久化会话-文件映射关系
     * 2. 保存向量存储数据
     */
    @PreDestroy
    private void persistent() {
        try {
            // 1. 保存会话-文件映射关系
            chatFiles.store(new FileWriter("chat-pdf.properties"),
                    "Last updated: " + LocalDateTime.now());

            // 2. 保存向量存储
            SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;
            simpleVectorStore.save(new File("chat-pdf.json"));
        } catch (IOException e) {
            throw new RuntimeException("持久化数据失败", e);
        }
    }
}