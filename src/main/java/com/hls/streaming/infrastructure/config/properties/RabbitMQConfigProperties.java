package com.hls.streaming.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "io.hls.rabbitmq")
public class RabbitMQConfigProperties {

    private boolean enabled;

    private Ffmpeg ffmpeg;

    @Getter
    @Setter
    public static class Ffmpeg {
        private Queue queue;
        private Exchange exchange;
        private Dlx dlx;
    }

    @Getter
    @Setter
    public static class Queue {
        private String name;
        private String key;
        private boolean durable;
        private boolean exclusive;
        private boolean autoDelete;
        private Dlq dlq;
    }

    @Getter
    @Setter
    public static class Exchange {
        private String name;
        private boolean durable;
        private boolean autoDelete;
    }

    @Getter
    @Setter
    public static class Dlq {
        private String name;
        private String key;
    }

    @Getter
    @Setter
    public static class Dlx {
        private String name;
    }

}
