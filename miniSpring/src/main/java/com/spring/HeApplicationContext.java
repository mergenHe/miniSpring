package com.spring;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//spring容器类
public class HeApplicationContext {
    private Class configClass;//定义Spring配置文件属性
    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>();//单例池，存放单例对象
    //创建一个Map去保存BeanDefinition对象
    private ConcurrentHashMap<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    //创建一个集合保存实现了BeanPostProcessor这个接口的特殊的bean
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    public HeApplicationContext(Class configClass){
        this.configClass = configClass;

        /**
         *对于Spring而言它拿到了Config这个类，它就要去解析这个类，
         * 解析这个类，解析的是你这个类有没有我Spring提供的注解
         */
        //扫描
        Scan(configClass);
        //遍历BeanDefinitionMap集合
        for (Map.Entry<String, BeanDefinition> beanDefinitionEntry : beanDefinitionMap.entrySet()) {
            String beanName = beanDefinitionEntry.getKey();
            BeanDefinition beanDefinition = beanDefinitionEntry.getValue();
            if (beanDefinition.getScope().equals("singleton")){//判断是不是单例bean
                Object bean = createBean(beanName,beanDefinition);//创建一个bean
                singletonObjects.put(beanName,bean);//放到单例池里面去
            }

        }

    }

    /**
     *
     * @param beanDefinition,创建类的信息
     * @return
     */
    public Object createBean(String beanName, BeanDefinition beanDefinition){
        //获取类定义类的属性Clazz
        Class clazz = beanDefinition.getClazz();
        //通过调用无参的构造方法反射去得到实例对象
        Object  instance = null;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
            //解析BeanDefinition对象的class属性，把里面的全部属性解析出来
            for (Field field : clazz.getDeclaredFields()) {
                //判断哪个属性有Autowired注解，给他赋值
                if (field.isAnnotationPresent(Autowired.class)){
                    //根据属性的名字去找对象
                    Object bean = getBean(field.getName());
                    field.setAccessible(true);
                    field.set(instance,bean);
                }
            }
            //Aware回调
            // 判断当前实例是否实现了这个接口
            if (instance instanceof BeanNameAware){
                ((BeanNameAware)instance).setBeanName(beanName);
            }
            //在初始化前我们去遍历BeanPostProcessorList这个集合，调用初始化前的方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                beanPostProcessor.postProcessBeforeInitialization(instance,beanName);
            }
            //初始化
            if (instance instanceof InitializingBean){
                try {
                    ((InitializingBean)instance).afterPropertiesSet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            //在初始化后我们去遍历BeanPostProcessorList这个集合，调用初始化前的方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                beanPostProcessor.postProcessAfterInitialization(instance,beanName);
            }
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return instance;

    }

    private void Scan(Class configClass) {
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        //拿到ComponentScan注解之后，就要去获取里面的属性
        String path = componentScanAnnotation.value();
        //path就是扫描路径
        //扫描
        //处理一下扫描路径
        path =path.replace(".","/");
        //得到一个类加载器
        ClassLoader classLoader = HeApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource(path);//这个resource其实是一个目录
        //我们需要把这个目录转换成一个File对象
        File file = new File(resource.getFile());
        //判断一个这个file是不是一个目录
        if (file.isDirectory()) {
            //把这个目录所有的文件拿出来
            File[] files = file.listFiles();
            for (File f : files) {
                //现在可以获取f文件的完整路径
                String fileName = f.getAbsolutePath();
                //判断一下这个文件是不是class文件
                if(fileName.endsWith(".class")) {
                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    className = className.replace("\\", ".");
                    String c =className;
                    //再使用类加载器去加载这个类
                    try {
                        Class<?>  clazz = classLoader.loadClass(className);
                        //根据加载出来的类去判断是否有Spring提供的注解
                        //如果进入到这个条件判断则表示当前这个类是一个Bean
                        if (clazz.isAnnotationPresent(Component.class)) {
                            //在这里判断该类是不是实现了BeanPostProcessor这个接口
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)){
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                                //把这个特殊的bean存到集合
                                beanPostProcessorList.add(instance);
                            }

                            //拿到类之后就去解析这个类，然后把这个类的定义信息放到BeanDefinition类里面去
                            Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);//把当前的bean的Component注解拿出来
                            String beanName = componentAnnotation.value();
                            //定义一个beanDefinition对象
                            BeanDefinition beanDefinition = new BeanDefinition();
                            //还要设置类类型
                            beanDefinition.setClazz(clazz);
                            if (clazz.isAnnotationPresent(Scope.class)){
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                //如果有Scope注解，则需要把用户配置的值设置到BeanDefinition里面去
                                beanDefinition.setScope(scopeAnnotation.value());
                                //
                            }else {
                                //如果没有，则是单例bean
                                beanDefinition.setScope("singleton");
                            }
                            //把beanDefinition对象存放到集合当中
                            beanDefinitionMap.put(beanName,beanDefinition);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    //再给这个类提供一个getBean方法,返回值是一个对象，所以是Object
    public Object getBean(String beanName){
        //先判断这个beanDefinitionMap集合里面有没有传过来的beanName对应的对象
        if (beanDefinitionMap.containsKey(beanName)){
            //从beanDefinition中取出对象
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            //判断是单例bean还是原型bean
            if (beanDefinition.getScope().equals("singleton")) {
                //从单例池中取出bean对象
                Object o = singletonObjects.get(beanName);
                return o;
            }else {
                Object bean = createBean(beanName,beanDefinition);
                return bean;
            }
        }else {
            //不存在这个Bean
            throw new NullPointerException();
        }
    }
}
