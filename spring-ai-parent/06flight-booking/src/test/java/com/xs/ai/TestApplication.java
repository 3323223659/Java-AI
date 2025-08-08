/*
package com.xs.ai;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

public class TestApplication {
    @Test
    public void test() throws NoApiKeyException, InputRequiredException {

        ApplicationParam param = ApplicationParam.builder()
                // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
                .apiKey(System.getenv("ALI_AI_KEY"))
                .appId("e90eb054075e4b5f8164ef4bff6b66a3")
                .prompt("规划长沙市政府到武汉市政府的骑行路线")
                .build();
        // 配置私网终端节点
        Application application = new Application();
        Flowable<ApplicationResult> flowable = application.streamCall(param);
        // 输出applicationResultFlowable
        flowable.blockingSubscribe(
                result ->{
                    System.out.println(result.getOutput().getText());
                },
                error -> System.out.printf("error: %s\n", error.getMessage()),
                () -> System.out.println("complete")
        );

    }
}
*/
