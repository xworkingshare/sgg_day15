package com.atguigu.gmall0416.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.Session;

@Configuration
public class ActiveMQConfig {

    // tcp://192.168.67.204:61616  === @Value
    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL ;
    // 监听器
    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;

    // 发送队列
    @Bean
    public ActiveMQUtil getActiveMQUtil(){
        if ("disabled".equals(brokerURL)){
            return null;
        }
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return  activeMQUtil;
    }


    /*<bean class="org.springframework.jms.config.DefaultJmsListenerContainerFactory">*/
    @Bean(name = "jmsQueueListener")
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {

        if("disabled".equals(listenerEnable)){
            return null;
        }
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);
        // 设置事务
        factory.setSessionTransacted(false);
        // 自动签收
        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        // 设置并发数
        factory.setConcurrency("5");
        // 重连间隔时间
        factory.setRecoveryInterval(5000L);

        return factory;
    }
    // 接收消息
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory ( ){
//        tcp://192.168.67.204:61616 ==
        ActiveMQConnectionFactory activeMQConnectionFactory =
                new ActiveMQConnectionFactory(brokerURL);
        return activeMQConnectionFactory;
    }
}