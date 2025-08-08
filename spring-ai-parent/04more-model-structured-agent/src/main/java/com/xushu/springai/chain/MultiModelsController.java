package com.xushu.springai.chain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
public class MultiModelsController {

    @Autowired
    ChatClient planningChatClient;

    @Autowired
    ChatClient botChatClient;

    @GetMapping(value = "/stream", produces = "text/stream;charset=UTF8")
    Flux<String> stream(@RequestParam String message) {
        // 创建一个用于接收多条消息的
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 推送消息
        sink.tryEmitNext("正在计划任务...<br/>");


        new Thread(() -> {
        AiJob.Job job = planningChatClient.prompt().user(message)
                .call().entity(AiJob.Job.class);

        switch (job.jobType()){
            case CANCEL ->{
                System.out.println(job);
                if(job.keyInfos().size()==0){
                    sink.tryEmitNext("请输入姓名和订单号.");
                }
                else {
                    // todo.. 执行业务  ticketService.cancel
                    // --->springai --->json
                    sink.tryEmitNext("退票成功!");
                }
            }
            case QUERY -> {
                System.out.println(job);
                if(job.keyInfos().size()==0){
                    sink.tryEmitNext("请输入订单号.");
                }
                // todo.. 执行业务 ticketService.query()
                sink.tryEmitNext("查询预定信息：xxxx");
            }
            case OTHER -> {
                Flux<String> content = botChatClient.prompt().user(message).stream().content();
                content.doOnNext(sink::tryEmitNext) // 推送每条AI流内容
                        .doOnComplete(() -> sink.tryEmitComplete())
                        .subscribe();
            }
            default -> {
                System.out.println(job);
                sink.tryEmitNext("解析失败");
            }
        }
        }).start();

        return sink.asFlux();
    }
}