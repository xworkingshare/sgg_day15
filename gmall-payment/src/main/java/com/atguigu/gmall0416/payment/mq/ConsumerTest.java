package com.atguigu.gmall0416.payment.mq;

import com.atguigu.gmall0416.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.*;
public class ConsumerTest {

    // 没有在ioc容器中！---
    @Autowired
    private static ActiveMQUtil activeMQUtil;

    public static void TestActiveMq() throws JMSException {
        Connection connection = activeMQUtil.getConnection();
        // 打开连接
        connection.start();
        // 需要消息队列，需要消息的提供者
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue queue = session.createQueue("TestActiveMq");
        MessageProducer producer = session.createProducer(queue);
        // 准备发送消息
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("Atguigu!");

        producer.send(activeMQTextMessage);

        // 关闭
        producer.close();
        session.close();
        connection.close();
    }
    // 消费者
    public static void main(String[] args) throws JMSException {

        TestActiveMq();
//        // 创建工厂
//        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, ActiveMQConnection.DEFAULT_PASSWORD,"tcp://192.168.67.204:61616");
//        // 创建连接
//        Connection connection = activeMQConnectionFactory.createConnection();
//        // 打开连接
//        connection.start();
//        // 需要消息队列，需要消息的消费者
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//
//        Queue queue = session.createQueue("Atguigu");
//        // 创建消费者
//        MessageConsumer consumer = session.createConsumer(queue);
//        // 监听消息
//        consumer.setMessageListener(new MessageListener() {
//            @Override
//            public void onMessage(Message message) {
//                if (message instanceof MapMessage){
//                    String text = null;
//                    try {
//                        text = ((MapMessage) message).getString("result");
//                        text+=((MapMessage) message).getString("orderId");
//                    } catch (JMSException e) {
//                        e.printStackTrace();
//                    }
//                    System.out.println(text+"接收的消息！");
//                }
//            }
//        });
//        consumer.close();
//        session.close();
//        connection.close();
    }
}
