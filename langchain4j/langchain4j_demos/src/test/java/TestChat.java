import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

public class TestChat {

    @Test
    void test01() {
        //
        ChatLanguageModel model = OpenAiChatModel
                .builder()
                .apiKey("demo")         // demo演示版本
                .modelName("gpt-4o-mini")
                .build();

        String answer = model.chat("你好，你是谁？");

        System.out.println(answer);
    }

    /**
     * 测试基本对话——接入deepseek
     */
    @Test
    void test02() {
        ChatLanguageModel model = OpenAiChatModel
                .builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(System.getenv("DEEP_SEEK_KEY"))
                .modelName("deepseek-reasoner")
                .build();

        String answer = model.chat("你好，你是谁？");

        System.out.println(answer);
    }

    /**
     * 测试基本对话——接入qwen-max
     */
    @Test
    void test03() {
        ChatLanguageModel model = QwenChatModel
                .builder()
                .baseUrl("https://dashscope.aliyuncs.com")
                .apiKey(System.getenv("ALI_AI_KEY"))
                .modelName("qwen-max")
                .build();

        String answer = model.chat("你好，你是谁？");

        System.out.println(answer);
    }

    /**
     * 测试基本对话——接入ollama
     */
    @Test
    void test04() {
        ChatLanguageModel model = OllamaChatModel
                .builder()
                .baseUrl("http://localhost:11434")
                //.apiKey(System.getenv("ALI_AI_KEY"))
                .modelName("deepseek-r1:1.5b")
                .build();

        String answer = model.chat("你好，你是谁？");

        System.out.println(answer);
    }


    @Test
    public void test() {
        WanxImageModel wanxImageModel = WanxImageModel.builder()
                .modelName("wanx2.1-t2i-plus")
                .apiKey(System.getenv("ALI_AI_KEY"))
                .build();

        Response<Image> response = wanxImageModel.generate("美女");
        System.out.println(response.content().url());
    }

    /**
     * 测试多轮对话——错误用法
     */
    @Test
    void test03_bad() {
        ChatLanguageModel model = OpenAiChatModel
                .builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();

        System.out.println(model.chat("你好，我是小小"));
        System.out.println("----");
        System.out.println(model.chat("我叫什么"));


    }

    /**
     * 测试多轮对话——正确用法
     */
    @Test
    void test03_good() {
        ChatLanguageModel model = OpenAiChatModel
                .builder()
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();


        UserMessage userMessage1 = UserMessage.userMessage("你好，我是小小");
        ChatResponse response1 = model.chat(userMessage1);
        AiMessage aiMessage1 = response1.aiMessage(); // 大模型的第一次响应
        System.out.println(aiMessage1.text());
        System.out.println("----");

        // 下面一行代码是重点
        ChatResponse response2 = model.chat(userMessage1, aiMessage1, UserMessage.userMessage("我叫什么"));
        AiMessage aiMessage2 = response2.aiMessage(); // 大模型的第二次响应
        System.out.println(aiMessage2.text());

    }
}
