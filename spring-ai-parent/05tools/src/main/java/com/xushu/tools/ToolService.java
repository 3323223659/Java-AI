package com.xushu.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * AI工具服务实现类
 * 提供以下核心功能：
 * 1. 声明式工具方法定义
 * 2. 动态工具注册机制
 * 3. 基于Spring Security的权限控制
 */
@Service
public class ToolService {

    @Autowired
    private TicketService ticketService; // 票务服务依赖注入

    /**
     * 退票工具方法
     * @param ticketNumber 预定号（纯数字）
     * @param name 真实姓名（必填）
     * @return 操作结果
     *
     * 安全控制：
     * @PreAuthorize("hasRole('ADMIN')") - 仅管理员角色可访问
     *
     * AI集成：
     * @Tool - 声明为AI可调用工具
     * @ToolParam - 定义参数元数据
     */
    @Tool(description = "退票")
    @PreAuthorize("hasRole('ADMIN')")
    public String cancel(
            @ToolParam(description = "预定号，可以是纯数字") String ticketNumber,
            @ToolParam(description = "真实人名（必填，必须为人的真实姓名，严禁用其他信息代替；如缺失请传null）") String name) {

        // 获取当前认证用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 调用票务服务执行退票
        ticketService.cancel(ticketNumber, name);

        return username + "退票成功！";
    }

    /**
     * 天气查询工具
     * @param latitude 纬度
     * @param longitude 经度
     * @return 天气状况
     *
     * 示例说明：
     * - 无需权限控制（开放访问）
     * - 参数包含地理坐标信息
     */
    @Tool(description = "获取指定位置天气,根据位置自动推算经纬度")
    public String getAirQuality(
            @ToolParam(description = "纬度") double latitude,
            @ToolParam(description = "经度") double longitude) {
        return "天晴"; // 模拟实现
    }

    /**
     * 动态工具注册方法
     * @param toolService 工具服务实例（必须通过依赖注入获取）
     * @return 工具回调列表
     *
     * 典型应用场景：
     * 1. 从数据库读取工具配置
     * 2. 运行时动态注册工具
     * 3. 支持工具的热更新
     */
    public List<ToolCallback> getToolCallList(ToolService toolService) {
        // 示例：通过反射获取工具方法
        Method method = ReflectionUtils.findMethod(
                ToolService.class,
                "cancel",
                String.class,
                String.class);

        // 构建工具定义（JSON Schema格式）
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("cancel")
                .description("退票")
                .inputSchema("""
                    {
                      "type": "object",
                      "properties": {
                        "ticketNumber": {
                          "type": "string",
                          "description": "预定号，可以是纯数字"
                        },
                        "name": {
                          "type": "string",
                          "description": "真实人名"
                        }
                      },
                      "required": ["ticketNumber", "name"]
                    }
                    """)
                .build();

        // 构建工具回调
        ToolCallback toolCallback = MethodToolCallback.builder()
                .toolDefinition(toolDefinition)
                .toolMethod(method)
                .toolObject(toolService) // 必须使用注入的实例
                .build();

        return List.of(toolCallback);
    }
}