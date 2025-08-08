
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
package com.xs.agent.chain_workflow;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.xs.agent.config.RestClientConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// ------------------------------------------------------------
// 链式工作流主应用
// ------------------------------------------------------------

@SpringBootApplication
@Import(RestClientConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * 创建命令行运行器，用于执行实际业务逻辑
	 * @param dashScopeChatModel 注入的DashScope聊天模型
	 * @return CommandLineRunner实例
	 */
	@Bean
	public CommandLineRunner commandLineRunner(DashScopeChatModel dashScopeChatModel) {
		// 创建ChatClient实例
		var chatClient = ChatClient.create(dashScopeChatModel);

		return args -> {
			// 定义电商平台订单处理系统升级需求
			String requirements = """  
                    电商平台需要升级订单处理系统，要求：
                     1. 处理能力提升到每秒1000单
                     2. 支持多种支付方式和优惠券
                     3. 实时库存管理和预警
                     4. 订单状态实时跟踪
                     5. 数据分析和报表功能
                     现有系统：Spring Boot + MySQL，日订单量10万 
                           """;

			// 创建并执行工作流处理器
			new PracticalChainWorkflow(chatClient).process(requirements);
		};
	}


}
