package com.atguigu.gmall0416.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {

    // 消息的提供者 psvm
    public static void main(String[] args) throws JMSException {
        // 创建工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.204:61616");
        // 创建连接
        Connection connection = activeMQConnectionFactory.createConnection();
        // 打开连接
        connection.start();
        // 需要消息队列，需要消息的提供者
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
//        Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
        Queue queue = session.createQueue("Hello");
        MessageProducer producer = session.createProducer(queue);
        // 准备发送消息
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("Atguigu!");
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
//        ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
//        mapMessage.setString("orderId","1001");
//        mapMessage.setString("result","success");

        producer.send(activeMQTextMessage);

        // 关闭
        producer.close();
        session.close();
        connection.close();

    }

}
