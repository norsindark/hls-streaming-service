package com.hls.streaming.documents.user;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = -4571559251236126053L;

    @Field("birthday")
    private Instant birthday;

    @Field("bio")
    private String bio;

    @Field("channel_banner")
    private String channelBanner;

    @Field("enable_notify")
    private Boolean enableNotify;
}
