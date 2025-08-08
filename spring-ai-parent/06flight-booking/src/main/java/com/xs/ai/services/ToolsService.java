package com.xs.ai.services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ToolsService {

    @Autowired
    FlightBookingService flightBookingService;

    /**
     * 退票
     * @param ticketNumber 预定号
     * @param name 真实人名
     * @return 退票成功
     */
    @Tool(description = "退票/取消预定,调用之前先查询航班")
    public String cancel(
            // @ToolParam告诉大模型参数的描述
      @ToolParam(description = "预定号，可以是纯数字") String ticketNumber,
      @ToolParam(description = "真实人名（必填，必须为人的真实姓名，严禁用其他信息代替；如缺失请传null）") String name
           ) {
        flightBookingService.cancelBooking(ticketNumber, name);
        return "退票成功";
    }

    @Tool(description = "获取航班信息")
    public FlightBookingService.BookingDetails getBookingDetails(
            @ToolParam(description = "预定号，可以是纯数字") String bookingNumber,
            @ToolParam(description = "真实人名（必填，必须为人的真实姓名，严禁用其他信息代替；如缺失请传null）") String name) {
        return flightBookingService.getBookingDetails(bookingNumber, name);
    }
}
