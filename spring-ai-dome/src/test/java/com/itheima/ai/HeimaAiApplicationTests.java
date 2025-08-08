package com.itheima.ai;

import com.itheima.ai.utils.VectorDistanceUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class HeimaAiApplicationTests {

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;

    @Test
    public void testVectorStore() {
        // 1. 准备PDF文件资源
        Resource resource = new FileSystemResource("中二知识笔记.pdf");

        // 2. 配置PDF阅读器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource, // 指定PDF文件路径
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults()) // 使用默认文本格式化
                        .withPagesPerDocument(1) // 每页PDF作为一个独立文档
                        .build()
        );

        // 3. 读取并分割PDF文档
        // 将PDF按页拆分为多个Document对象，每个Document包含：
        // - 文本内容
        // - 元数据（如页码、文件名等）
        List<Document> documents = reader.read();

        // 4. 将文档存入向量数据库
        // 向量库会自动：
        // - 提取文本特征向量
        // - 建立向量索引
        vectorStore.add(documents);

        // 5. 构建搜索请求
        SearchRequest request = SearchRequest.builder()
                .query("论语中教育的目的是什么")  // 搜索查询语句
                .topK(1)                      // 返回最相似的1个结果
                .similarityThreshold(0.6)     // 相似度阈值（0-1之间）
                .filterExpression("file_name == '中二知识笔记.pdf'") // 按文件名过滤
                .build();

        // 6. 执行相似度搜索
        // 过程：
        // 1) 将查询文本向量化
        // 2) 在向量空间中找到最相似的文档
        List<Document> docs = vectorStore.similaritySearch(request);

        // 7. 处理搜索结果
        if (docs == null || docs.isEmpty()) {
            System.out.println("没有搜索到任何内容");
            return;
        }

        // 打印结果详情
        for (Document doc : docs) {
            System.out.println("--- 搜索结果 ---");
            System.out.println("文档ID: " + doc.getId());
            System.out.println("相似度得分: " + doc.getScore()); // 余弦相似度值
            System.out.println("文本内容: \n" + doc.getText());
        }
    }

    @Test
    void contextLoads() {
        // 1.测试数据
        // 1.1.用来查询的文本，国际冲突
        String query = "global conflicts";

        // 1.2.用来做比较的文本
        String[] texts = new String[]{
                "哈马斯称加沙下阶段停火谈判仍在进行 以方尚未做出承诺",
                "土耳其、芬兰、瑞典与北约代表将继续就瑞典“入约”问题进行谈判",
                "日本航空基地水井中检测出有机氟化物超标",
                "国家游泳中心（水立方）：恢复游泳、嬉水乐园等水上项目运营",
                "我国首次在空间站开展舱外辐射生物学暴露实验",
        };
        // 2.向量化
        // 2.1.先将查询文本向量化
        float[] queryVector = embeddingModel.embed(query);

        // 2.2.再将比较文本向量化，放到一个数组
        List<float[]> textVectors = embeddingModel.embed(Arrays.asList(texts));

        // 3.比较欧氏距离
        // 3.1.把查询文本自己与自己比较，肯定是相似度最高的
        System.out.println(VectorDistanceUtils.euclideanDistance(queryVector, queryVector));
        // 3.2.把查询文本与其它文本比较
        for (float[] textVector : textVectors) {
            System.out.println(VectorDistanceUtils.euclideanDistance(queryVector, textVector));
        }
        System.out.println("------------------");

        // 4.比较余弦距离
        // 4.1.把查询文本自己与自己比较，肯定是相似度最高的
        System.out.println(VectorDistanceUtils.cosineDistance(queryVector, queryVector));
        // 4.2.把查询文本与其它文本比较
        for (float[] textVector : textVectors) {
            System.out.println(VectorDistanceUtils.cosineDistance(queryVector, textVector));
        }
    }
}
