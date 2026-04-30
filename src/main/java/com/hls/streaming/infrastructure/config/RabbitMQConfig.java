package com.hls.streaming.infrastructure.config;

import com.hls.streaming.infrastructure.config.properties.RabbitMQConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitMQConfig {

    @Bean
    @ConditionalOnProperty(value = "io.hls.rabbitmq.enabled", havingValue = "true")
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        log.info("Initialized bean rabbitmq!");
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnProperty(value = "io.hls.rabbitmq.enabled", havingValue = "true")
    public Queue ffmpegQueue(RabbitMQConfigProperties props) {
        var q = props.getFfmpeg().getQueue();
        var dlx = props.getFfmpeg().getDlx();

        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlx.getName());
        args.put("x-dead-letter-routing-key", q.getDlq().getKey());

        return new Queue(
                q.getName(),
                q.isDurable(),
                q.isExclusive(),
                q.isAutoDelete(),
                args);
    }

    @Bean
    @ConditionalOnProperty(value = "io.hls.rabbitmq.enabled", havingValue = "true")
    public DirectExchange ffmpegExchange(RabbitMQConfigProperties props) {
        var ex = props.getFfmpeg().getExchange();
        return new DirectExchange(ex.getName(),
                ex.isDurable(),
                ex.isAutoDelete());
    }

    @Bean
    @ConditionalOnProperty(value = "io.hls.rabbitmq.enabled", havingValue = "true")
    public Binding ffmpegBinding(Queue ffmpegQueue, DirectExchange ffmpegExchange, RabbitMQConfigProperties props) {

        return BindingBuilder
                .bind(ffmpegQueue)
                .to(ffmpegExchange)
                .with(props.getFfmpeg().getQueue().getKey());
    }

    @Bean
    public DirectExchange deadLetterExchange(RabbitMQConfigProperties props) {
        return new DirectExchange(props.getFfmpeg().getDlx().getName(), true, false);
    }

    @Bean
    public Queue deadLetterQueue(RabbitMQConfigProperties props) {
        return new Queue(props.getFfmpeg().getQueue().getDlq().getName(), true);
    }

    @Bean
    public Binding deadLetterBinding(RabbitMQConfigProperties props,
            Queue deadLetterQueue,
            DirectExchange deadLetterExchange) {

        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(props.getFfmpeg().getQueue().getDlq().getKey());
    }
}
