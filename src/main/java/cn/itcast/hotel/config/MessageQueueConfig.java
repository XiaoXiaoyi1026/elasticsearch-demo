package cn.itcast.hotel.config;

import cn.itcast.hotel.constants.MessageQueueConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 20609
 */
@Configuration
public class MessageQueueConfig {

    /**
     * 定义交换机
     *
     * @return 话题交换机
     */
    @Bean
    public TopicExchange topicExchange() {
        //                                   交换机名称                       持久化          自动删除
        return new TopicExchange(MessageQueueConstants.HOTEL_EXCHANGE, true, false);
    }

    /**
     * 插入/修改消息的队列
     */
    @Bean
    public Queue insertQueue() {
        return new Queue(MessageQueueConstants.HOTEL_INSERT_QUEUE, true);
    }

    /**
     * 删除消息的队列
     */
    @Bean
    public Queue deleteQueue() {
        return new Queue(MessageQueueConstants.HOTEL_DELETE_QUEUE, true);
    }

    /**
     * 将队列绑定到交换机上
     */
    @Bean
    public Binding insertQueueBinding() {
        return BindingBuilder.bind(insertQueue()).to(topicExchange()).with(MessageQueueConstants.HOTEL_INSERT_KEY);
    }

    /**
     * 将队列绑定到交换机上
     */
    @Bean
    public Binding deleteQueueBinding() {
        return BindingBuilder.bind(deleteQueue()).to(topicExchange()).with(MessageQueueConstants.HOTEL_DELETE_KEY);
    }

}
