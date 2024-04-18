package com.replay.uploadservice.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String REPLAY_EXCHANGE = "replay_exchange";
    public static final String REPLAY_QUEUE = "replay_queue";
    public static final String REPLAY_ROUTING_KEY_UPLOAD = "replay_routingKey_upload";

    @Bean
    public Queue queue(){
        return new Queue(REPLAY_QUEUE);
    }

    @Bean
    public TopicExchange exchange(){
        return new TopicExchange(REPLAY_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(REPLAY_ROUTING_KEY_UPLOAD);
    }

    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory){
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
