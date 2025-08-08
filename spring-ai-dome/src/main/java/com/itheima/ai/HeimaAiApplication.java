package com.itheima.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@MapperScan("com.itheima.ai.mapper")
@SpringBootApplication
public class HeimaAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeimaAiApplication.class, args);
    }

}
