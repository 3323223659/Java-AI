package com.xushu.springai.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Scope("prototype") // 声明为原型作用域的Bean，每次依赖注入时都会创建新实例
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
        // 创建RestClient.Builder基础实例
        RestClient.Builder builder = RestClient.builder()
                // 配置请求工厂，设置超时参数
                .requestFactory(ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withReadTimeout(Duration.ofSeconds(60)) // 读取超时60秒
                                .withConnectTimeout(Duration.ofSeconds(60)) // 连接超时60秒
                ));

        // 应用RestClientBuilderConfigurer的额外配置
        return restClientBuilderConfigurer.configure(builder);
    }
}
