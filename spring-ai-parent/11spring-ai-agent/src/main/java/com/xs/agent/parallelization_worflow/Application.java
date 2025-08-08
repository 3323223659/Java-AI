
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
package com.xs.agent.parallelization_worflow;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.xs.agent.config.RestClientConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

// ------------------------------------------------------------
// 并行化工作流主应用
// 功能：通过AI模型实现多部门需求的并行分析与结果聚合
// ------------------------------------------------------------

@SpringBootApplication
@Import(RestClientConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * 命令行运行器Bean
	 * 系统启动后自动执行并行分析流程
	 *
	 * @param dashScopeChatModel 注入的AI聊天模型
	 * @return CommandLineRunner实例
	 */
	@Bean
	public CommandLineRunner commandLineRunner(DashScopeChatModel dashScopeChatModel) {
		// 创建AI聊天客户端
		var chatClient = ChatClient.create(dashScopeChatModel);

		return args -> {
			// 定义需要分析的部门列表及特征
			List<String> departments = List.of(
					"IT部门：负责系统架构升级，团队技术水平参差不齐，预算紧张",
					"销售部门：需要学习新的CRM系统，担心影响客户关系，抗拒变化",
					"财务部门：要求数据安全性极高，对云端存储有顾虑，流程复杂",
					"人力资源部门：需要数字化招聘流程，缺乏相关技术人员，时间紧迫"
			);

			System.out.println("=== 并行分析 + 聚合处理 ===");
			// 执行并行分析流程并获取聚合结果
			ParallelizationWorkflowWithAggregator.AggregatedResult result = new ParallelizationWorkflowWithAggregator(chatClient)
					.parallelWithAggregation(departments);

			// 输出各部门独立分析结果
			System.out.println("\n=== 各部门独立分析结果 ===");
			for (int i = 0; i < result.individualResults().size(); i++) {
				System.out.println("部门" + (i + 1) + ":");
				System.out.println(result.individualResults().get(i));
				System.out.println("\n" + "-".repeat(50) + "\n");
			}

			// 输出聚合器综合报告
			System.out.println("\n=== 聚合器综合报告 ===");
			System.out.println(result.aggregatedOutput());
		};
	}
}
