
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
package com.xs.agent.orchestrator_workers;

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
// 编排器-工作者模式主应用
// 功能：通过AI模型实现任务分解和协同处理
// ------------------------------------------------------------

@SpringBootApplication
@Import(RestClientConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * 命令行运行器Bean
	 * 系统启动后自动执行任务处理流程
	 *
	 * @param dashScopeChatModel 注入的AI聊天模型
	 * @return CommandLineRunner实例
	 */
	@Bean
	public CommandLineRunner commandLineRunner(DashScopeChatModel dashScopeChatModel) {
		// 创建AI聊天客户端
		var chatClient = ChatClient.create(dashScopeChatModel);

		return args -> {
			// 启动任务编排处理器，处理企业考勤系统设计需求
			new SimpleOrchestratorWorkers(chatClient)
					.process("设计一个企业级的员工考勤系统，支持多种打卡方式和报表生成");
		};
	}
}
