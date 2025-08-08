package com.xushu.springai.cc;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

@SpringBootTest
public class TestPrompt {


    // 系统提示词——预设角色
    @Test
    public void testSystemPrompt(@Autowired
                           ChatClient.Builder chatClientBuilder){
        // 为chatClient设置了提示词
        // 为ChatClient预设角色： 你是什么， 你能做什么， 你要注意什么， 具体应该怎么做
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("""
                        # 角色说明
                        你是一名专业法律顾问AI……
                                                
                        ## 回复格式
                        1. 问题分析
                        2. 相关依据
                        3. 梳理和建议
                                                
                        **特别注意：**
                        - 不承担律师责任。
                        - 不生成涉敏、虚假内容。
                        """)
                .build();

        String content = chatClient.prompt()
                // .system()   只为当前对话设置系统提示词
                .user("你好")
                .call().content();


        System.out.println(content);
    }


    // 提示词模板
    @Test
    public void testSystemPromptTemplate(@Autowired
                                 ChatClient.Builder chatClientBuilder){
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("""
                        # 角色说明
                        你是一名专业法律顾问AI……
                                                
                        ## 回复格式
                        1. 问题分析
                        2. 相关依据
                        3. 梳理和建议
                                                
                        **特别注意：**
                        - 不承担律师责任。
                        - 不生成涉敏、虚假内容。
                        
                        当前服务的用户：
                        姓名：{name}，年龄：{age}，性别：{sex}
                         
                        """)
                .build();

        String content = chatClient.prompt()
                // .system()   只为当前对话设置系统提示词
                .system(p -> p.param("name", "徐庶").param("age", "18").param("sex", "男"))
                .user("你好")
                .call().content();


        System.out.println(content);
    }


    // 提示词模板——伪系统提示词
    @Test
    public void testSystemPromptTemplate2(@Autowired
                                         ChatClient.Builder chatClientBuilder){
        ChatClient chatClient = chatClientBuilder
                .build();

        String content = chatClient.prompt()
                // .system()   只为当前对话设置系统提示词
                .system(p -> p.param("name", "徐庶").param("age", "18").param("sex", "男"))
                .user(u -> u.text(""" 
                        # 角色说明
                        你是一名专业法律顾问AI……
                                              \s
                        ## 回复格式
                        1. 问题分析
                        2. 相关依据
                        3. 梳理和建议
                                              \s
                        **特别注意：**
                        - 不承担律师责任。
                        - 不生成涉敏、虚假内容。

                        回答用户的法律咨询问题
                        {question}                    \s 
                        """).param("question", "被裁的补偿金"))
                .call().content();


        System.out.println(content);
    }



    // 提示词模板
    @Test
    public void testSystemPromptTemplate(@Autowired
                                         ChatClient.Builder chatClientBuilder,
            @Value("classpath:/files/prompt.st")
                                         Resource systemResource){
        ChatClient chatClient = chatClientBuilder
                .defaultSystem(systemResource)
                .build();

        String content = chatClient.prompt()
                // .system()   只为当前对话设置系统提示词
                .system(p -> p.param("name", "徐庶").param("age", "18").param("sex", "男"))
                .user("你好")
                .call().content();


        System.out.println(content);
    }
}
