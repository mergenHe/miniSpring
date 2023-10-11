package com.he;

import com.he.service.OrderService;
import com.spring.HeApplicationContext;

public class Test {
    public static void main(String[] args) {
        HeApplicationContext heApplicationContext = new HeApplicationContext(AppConfig.class);
        OrderService orderService = (OrderService) heApplicationContext.getBean("orderService");
        orderService.test();
    }
}
