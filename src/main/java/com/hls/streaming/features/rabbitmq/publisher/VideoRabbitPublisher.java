package com.hls.streaming.features.rabbitmq.publisher;

import com.hls.streaming.config.properties.RabbitMQConfigProperties;
import com.hls.streaming.dtos.events.OnUploadVideoEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class VideoRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfigProperties rabbitMQProps;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishProcessVideoEvent(final OnUploadVideoEvent event) {
        if (Objects.isNull(event) || StringUtils.isBlank(event.getVideoId())) {
            return;
        }

        var exchange = rabbitMQProps.getFfmpeg().getExchange().getName();
        var routingKey = rabbitMQProps.getFfmpeg().getQueue().getKey();

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
