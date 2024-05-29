package com.replay.uploadservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String REPLAY_EXCHANGE = "replay_exchange";
    public static final String REPLAY_QUEUE = "replay_queue";

    public static final String REPLAY_ROUTING_KEY = "replay_routingKey";

    public static final String SUBSCRIPTION_EXCHANGE = "subscription_exchange";
    public static final String SUBSCRIPTION_QUEUE = "subscription_queue";

    public static final String SUBSCRIPTION_ROUTING_KEY = "subscription_routingKey";

    @Bean
    public Queue replayQueue(){
        return new Queue(REPLAY_QUEUE);
    }

    @Bean
    public TopicExchange replayExchange(){
        return new TopicExchange(REPLAY_EXCHANGE);
    }

    @Bean
    public Binding replayBinding(Queue replayQueue, TopicExchange replayExchange){
        return BindingBuilder.bind(replayQueue).to(replayExchange).with(REPLAY_ROUTING_KEY);
    }

    @Bean
    public Queue subscriptionQueue(){
        return new Queue(SUBSCRIPTION_QUEUE);
    }

    @Bean
    public TopicExchange subscriptionExchange(){
        return new TopicExchange(SUBSCRIPTION_EXCHANGE);
    }

    @Bean
    public Binding subscriptionBinding(Queue subscriptionQueue, TopicExchange subscriptionExchange){
        return BindingBuilder.bind(subscriptionQueue).to(subscriptionExchange).with(SUBSCRIPTION_ROUTING_KEY);
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
