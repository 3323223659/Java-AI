package com.xs.langchain4j_springboot.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

@Service
public class ToolsService {

    // 告诉AI 什么对话才调用这个方法
    @Tool("某个地区有多少个名字的")
    public  Integer changshaNameCount(

            @P("地区")
            String location,
            // 告诉AI 需要提取的信息
            @P("姓名")
            String name){
        // todo...
        System.out.println(name);
        System.out.println(location);

        // 结果
        return 10;
    }


    @Tool("退票")
    public  String cancelBooking(

            @P("地区")
            String bookingNumber,
            // 告诉AI 需要提取的信息
            @P("姓名")
            String name){
        // todo... 业务方法，退票数据库操作
        System.out.println(name);
        System.out.println(bookingNumber);

        // 结果
        return "退票成功";
    }
}