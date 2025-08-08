
/* 
* Copyright 2024 - 2024 the original author or authors.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* https://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.xs.agent.evaluator_optimizer;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.xs.agent.config.RestClientConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// ------------------------------------------------------------
// 评估优化器主应用
// ------------------------------------------------------------

@SpringBootApplication
@Import(RestClientConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * 创建命令行运行器Bean
	 * 配置并启动评估优化流程
	 *
	 * @param dashScopeChatModel 注入的DashScope聊天模型
	 * @return CommandLineRunner实例
	 */
	@Bean
	public CommandLineRunner commandLineRunner(DashScopeChatModel dashScopeChatModel) {
		// 创建ChatClient实例，包装DashScope聊天模型
		var chatClient = ChatClient.create(dashScopeChatModel);

		return args -> {
			// 定义评估优化任务：List<User>转Map<Id,User>的高效实现
			new SimpleEvaluatorOptimizer(chatClient).loop("""
                <user input>
                  面试被问：怎么高效的将10000行list<User>数据，转化成map<id，user>，不是用stream.
                </user input>
                """);
		};
	}
}
