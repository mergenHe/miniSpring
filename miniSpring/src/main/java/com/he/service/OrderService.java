package com.he.service;

import com.spring.Autowired;
import com.spring.BeanNameAware;
import com.spring.Component;
import com.spring.InitializingBean;

@Component("orderService")
public class OrderService implements InitializingBean {
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Autowired
    private UserService userService;
    private String beanName;
    public void test(){
        System.out.println(userService);
        System.out.println(beanName);
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("初始化");
    }
}
