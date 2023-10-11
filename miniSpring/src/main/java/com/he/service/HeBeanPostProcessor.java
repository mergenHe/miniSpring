package com.he.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

@Component("aa")
public class HeBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        //在这里可以实现程序员自己的逻辑
        if (beanName.equals("orderService")) {
            ((OrderService)bean).setBeanName("veryGood");
            System.out.println("初始化前");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("初始化后");  
        return bean;
    }
}
