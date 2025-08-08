import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestAgent {

    // 任务分类专家接口 - 用于判断用户意图
    interface GreetingExpert {
        /**
         * 分析文本任务类型
         * @param text 用户输入文本
         * @return 任务类型枚举
         *
         * @UserMessage 注解表示这是用户消息模板
         * {{it}} 是模板变量，会被实际参数替换
         */
        @UserMessage("以下文本是什么任务： {{it}}")
        TASKTYPE isTask(String text);
    }

    // 聊天机器人接口 - 用于通用对话
    interface ChatBot {
        /**
         * 客服回复方法
         * @param userMessage 用户消息
         * @return 客服回复
         *
         * @SystemMessage 定义系统角色设定
         */
        @SystemMessage("你是一名航空公司客服代理，请为客户服务：")
        String reply(String userMessage);
    }

    // 业务处理类 - 组合使用两个AI代理
    class MilesOfSmiles {
        private GreetingExpert greetingExpert; // 任务分类器
        private ChatBot chatBot;              // 通用聊天机器人

        public MilesOfSmiles(GreetingExpert greetingExpert, ChatBot chatBot) {
            this.greetingExpert = greetingExpert;
            this.chatBot = chatBot;
        }

        /**
         * 处理用户消息的核心方法
         * @param userMessage 用户输入
         * @return 处理结果
         */
        public String handle(String userMessage) {
            // 1. 先判断任务类型
            TASKTYPE task = greetingExpert.isTask(userMessage);

            // 2. 根据不同类型处理
            switch (task) {
                case MODIFY_TICKET:  // 改签
                case QUERY_TICKET:   // 查询
                case CANCEL_TICKET:  // 退票
                    return task.getName() + "调用service方法处理"; // 模拟业务处理
                case OTHER:          // 其他对话
                    return chatBot.reply(userMessage); // 交给聊天机器人
            }
            return null;
        }
    }

    // 测试用模型实例
    ChatLanguageModel qwen;    // 通义千问模型
    ChatLanguageModel deepseek; // DeepSeek模型

    /**
     * 测试初始化 - 在每个测试方法前执行
     */
    @BeforeEach
    public void init() {
        // 初始化通义千问模型
        qwen = QwenChatModel
                .builder()
                .apiKey(System.getenv("ALI_AI_KEY")) // 从环境变量获取API密钥
                .modelName("qwen-max")               // 使用qwen-max模型
                .build();

        // 初始化DeepSeek模型
        deepseek = OpenAiChatModel
                .builder()
                .baseUrl("https://api.deepseek.com") // 自定义API端点
                .apiKey(System.getenv("DEEP_SEEK_KEY"))
                .modelName("deepseek-reasoner")      // 使用deepseek-reasoner模型
                .build();
    }

    /**
     * 测试用例 - 模拟客服场景
     */
    @Test
    void test() {
        // 1. 创建任务分类专家（使用DeepSeek模型）
        GreetingExpert greetingExpert = AiServices.create(GreetingExpert.class, deepseek);

        // 2. 创建聊天机器人（使用通义千问模型）
        ChatBot chatBot = AiServices.create(ChatBot.class, qwen);

        // 3. 组合业务处理器
        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(greetingExpert, chatBot);

        // 4. 测试退票场景
        String greeting = milesOfSmiles.handle("我要退票！");
        System.out.println(greeting); // 预期输出："CANCEL_TICKET调用service方法处理"
    }
}