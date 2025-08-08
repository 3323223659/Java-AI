package com.xs.ai;

import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootTest
class SpringAiDemoApplicationTests {

    @Test
    void contextLoads() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("java", "-version");

        Process process = processBuilder.start();

        process.errorReader().lines().forEach(System.out::println);
    }

    @Test
    void testMCPStdio() throws IOException, InterruptedException {
        String[] commands = {"java",
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dlogging.pattern.console=",
                "-jar",
                "D:\\ideaworkspace\\git_pull\\tuling-flight-booking_all\\mcp-stdio-server\\target\\mcp-stdio-server-xs-1.0.jar"};

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(commands);
        // processBuilder.environment().put("username","xushu");

        Process process = processBuilder.start();


        Thread thread = new Thread(() -> {
            try (BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = processReader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();


        Thread.sleep(1000);

        new Thread(() -> {

            try {
                //String jsonMessage="{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":\"3670122a-0\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"spring-ai-mcp-client\",\"version\":\"1.0.0\"}}}";
                String jsonMessage = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"3b3f3431-1\",\"params\":{}}";

                jsonMessage = jsonMessage.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");

                var os = process.getOutputStream();
                synchronized (os) {
                    os.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
                    os.write("\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                System.out.println("写入完成！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        thread.join();
        /*JSONRPCRequest[jsonrpc=2.0, method=initialize, id=5d83d0d1-0, params=InitializeRequest[protocolVersion=2024-11-05, capabilities=ClientCapabilities[experimental=null, roots=null, sampling=null],
        clientInfo=Implementation[name=spring-ai-mcp-client, version=1.0.0]]]*/
    }
}



