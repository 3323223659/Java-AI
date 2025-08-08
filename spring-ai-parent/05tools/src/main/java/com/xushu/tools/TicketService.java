package com.xushu.tools;

import org.springframework.stereotype.Service;

@Service
public class TicketService {

    // 退票实现
    public void cancel(String ticketNumber,String name) {
        System.out.println("退票成果");
    }
}
