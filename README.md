# Spring AI & LangChain4j 学习项目集合

## 项目简介

本项目是一个综合性的 AI 大模型应用学习项目，包含了 Spring AI 和 LangChain4j 两个主流 Java AI 框架的完整学习示例。项目通过多个模块展示了从基础聊天到高级 AI Agent、RAG（检索增强生成）、工具调用等核心功能的实现，适合 Java 开发者学习和实践 AI 应用开发。

详细说明参考csdn文档：https://blog.csdn.net/2302_79380280?spm=1011.2415.3001.5343

## 技术架构

- **AI 框架**：Spring AI、LangChain4j
- **后端框架**：Spring Boot 3.x
- **大模型支持**：
  - 阿里云通义千问（DashScope）
  - DeepSeek
  - OpenAI
  - Ollama（本地模型）
- **向量数据库**：SimpleVectorStore、内存向量存储
- **构建工具**：Maven
- **Java 版本**：JDK 17+

## 目录结构

```text
ai-learning-projects/
├── spring-ai-parent/              # Spring AI 学习项目主目录
│   ├── 01quick-start/             # 快速入门示例
│   ├── 02chat-client/             # 聊天客户端基础用法
│   ├── 03more-platform-and-model/ # 多平台多模型集成
│   ├── 04more-model-structured-agent/ # 结构化 Agent 示例
│   ├── 05tools/                   # 工具调用功能
│   ├── 06flight-booking/          # 航班预订智能助手
│   ├── 07mcp-stdio-server/        # MCP STDIO 服务器
│   ├── 08mcp-sse-server/          # MCP SSE 服务器
│   ├── 09rag/                     # RAG 检索增强生成
│   ├── 10observability-actuator/  # 可观测性监控
│   └── 11spring-ai-agent/         # 高级 AI Agent 示例
├── spring-ai-dome/                # Spring AI 综合演示项目
│   ├── src/main/java/             # Java 源码
│   ├── src/main/resources/        # 配置文件和静态资源
|   ├── 前端代码                    # 该项目前端代码
│   └── pom.xml                    # Maven 配置
├── langchain4j/                   # LangChain4j 学习项目目录
│   └── langchain4j_springboot/    # LangChain4j + Spring Boot 集成
│       ├── src/main/java/         # Java 源码
│       ├── src/main/resources/    # 配置文件
│       └── pom.xml                # Maven 配置
└── README.md                      # 项目说明文档
```

## 核心功能模块

### Spring AI 模块

1. **基础聊天功能**
   - 多模型支持（通义千问、DeepSeek、Ollama）
   - 流式对话
   - 聊天记忆管理

2. **高级 AI Agent**
   - 链式工作流处理
   - 并行任务处理
   - 评估优化器
   - 任务编排器

3. **RAG 检索增强**
   - 文档向量化
   - 语义搜索
   - PDF 文档问答

4. **工具调用**
   - 航班预订助手
   - 天气查询工具
   - 自定义函数调用

5. **MCP 协议支持**
   - STDIO 通信
   - SSE 实时通信

### LangChain4j 模块

1. **聊天功能**
   - 基础对话
   - 流式聊天
   - 多用户记忆隔离

2. **RAG 系统**
   - 文档加载和分割
   - 向量存储和检索
   - 知识库问答

3. **AI 助手配置**
   - 系统提示词模板
   - 记忆管理
   - 工具集成

## 使用说明

### 1. 环境准备

- **JDK 17+**
- **Maven 3.6+**
- **IDE**：IntelliJ IDEA 或 Eclipse

### 2. 配置 API Key

在各模块的 `application.properties` 中配置相应的 API Key：

```properties
# 阿里云通义千问
spring.ai.dashscope.api-key=${ALI_AI_KEY}

# DeepSeek
spring.ai.deepseek.api-key=${DEEP_SEEK_KEY}

# OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

### 3. 运行项目

1. 克隆项目到本地
2. 配置环境变量或直接在配置文件中设置 API Key
3. 选择要运行的模块，执行：
   ```bash
   cd spring-ai-parent/01quick-start
   mvn spring-boot:run
   ```
4. spring-ai-dome里的前端代码需要下载后再启动
   ```bash
   npm install
   ```

### 4. 模块说明

- **spring-ai-parent**：Spring AI 框架的完整学习示例
- **spring-ai-dome**：Spring AI 综合演示项目，包含 Web 界面
- **langchain4j**：LangChain4j 框架学习示例
- 每个子模块都有独立的 README 和示例代码

### 5. 常见命令

```bash
# 编译整个项目
mvn clean compile

# 运行测试
mvn test

# 打包项目
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

## 学习路径建议

1. **入门阶段**：从 `01quick-start` 开始，了解基础聊天功能
2. **进阶阶段**：学习 `06flight-booking` 的工具调用和 `09rag` 的检索增强
3. **高级阶段**：研究 `11spring-ai-agent` 的复杂 Agent 实现
4. **对比学习**：比较 Spring AI 和 LangChain4j 的不同实现方式

## 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [阿里云百炼平台](https://bailian.console.aliyun.com/)
- [DeepSeek API 文档](https://platform.deepseek.com/api-docs/)

## 免责声明

本项目仅用于学习和技术交流，请遵守各 AI 服务提供商的使用条款。商业使用请确保符合相关法律法规和服务协议，如有侵权请联系删除。

## 贡献指南

欢迎提交 Issue 和 Pull Request 来完善项目。请确保代码风格一致，并添加必要的注释和文档。

## 许可证

本项目采用 MIT 许可证，详情请参阅 LICENSE 文件。
