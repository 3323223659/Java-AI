package com.xushu.springai.cc;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
public class TestStructuredOut {
    // 声明ChatClient用于与AI模型交互
    ChatClient chatClient;

    /**
     * 初始化方法，在每个测试方法前执行
     * @param chatModel 自动注入的DashScopeChatModel实例
     */
    @BeforeEach
    public void init(@Autowired DashScopeChatModel chatModel) {
        // 使用建造者模式创建ChatClient实例
        chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 测试布尔值结构化输出
     * 演示如何让AI模型返回简单的布尔值结果
     */
    @Test
    public void testBoolOut() {
        // 使用ChatClient构建请求，指定系统提示词和用户输入
        Boolean isComplain = chatClient
                .prompt()
                .system("""
            请判断用户信息是否表达了投诉意图?
            只能用 true 或 false 回答，不要输出多余内容
            """) // 严格限制输出格式
                .user("你好！") // 用户输入文本
                .call() // 执行调用
                .entity(Boolean.class); // 将响应转换为Boolean类型

        // 根据返回的布尔值进行分支处理
        if (Boolean.TRUE.equals(isComplain)) {
            System.out.println("用户是投诉，转接人工客服！");
        } else {
            System.out.println("用户不是投诉，自动流转客服机器人。");
            // 可以继续调用客服ChatClient进行后续对话
        }
    }

    /**
     * 定义地址记录类
     * 用于接收结构化输出的地址信息
     */
    public record Address(
            String name,        // 收件人姓名
            String phone,       // 联系电话
            String province,    // 省
            String city,        // 市
            String district,    // 区/县
            String detail       // 详细地址
    ) {}

    /**
     * 测试实体类结构化输出
     * 演示如何从非结构化文本中提取结构化数据
     */
    @Test
    public void testEntityOut() {
        // 使用ChatClient构建请求，从文本中提取地址信息
        Address address = chatClient.prompt()
                .system("""
                        请从下面这条文本中提取收货信息,
                        """) // 系统指令
                .user("收货人：张三，电话13588888888，地址：浙江省杭州市西湖区文一西路100号8幢202室") // 包含地址信息的文本
                .call() // 执行调用
                .entity(Address.class); // 将响应转换为Address类型

        System.out.println(address); // 打印提取的结构化地址信息
    }

    /**
     * 定义演员电影记录类
     * 用于接收结构化输出的电影信息
     */
    public record ActorsFilms(
            String actor,
            String film1,
            String film2,
            String film3,
            String film4,
            String film5
    ) {}

    /**
     * 测试底层实体类结构化输出
     * 演示使用BeanOutputConverter进行更精细的控制
     * @param chatModel 自动注入的DashScopeChatModel实例
     */
    @Test
    public void testLowEntityOut(@Autowired DashScopeChatModel chatModel) {
        // 创建BeanOutputConverter实例，指定目标类型为ActorsFilms
        BeanOutputConverter<ActorsFilms> beanOutputConverter =
                new BeanOutputConverter<>(ActorsFilms.class); // 使用BeanOutputConverter进行结构化输出

        // 获取格式指令，这将指导AI模型如何格式化输出
        String format = beanOutputConverter.getFormat(); // 获取JSON Schema格式指令

        String actor = "周星驰";

        // 构建提示词模板，包含占位符
        String template = """
        提供5部{actor}导演的电影.
        {format}
        """; // 提示词模板包含格式占位符

        // 创建PromptTemplate实例并替换变量
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(template)
                .variables(Map.of("actor", actor, "format", format))
                .build(); // 构建提示词模板

        // 调用模型获取响应
        ChatResponse response = chatModel.call(promptTemplate.create());

        // 使用转换器将模型输出转换为目标类型
        ActorsFilms actorsFilms = beanOutputConverter.convert(
                response.getResult().getOutput().getText()); // 转换模型输出为Java对象

        System.out.println(actorsFilms); // 打印结构化电影信息
    }
}