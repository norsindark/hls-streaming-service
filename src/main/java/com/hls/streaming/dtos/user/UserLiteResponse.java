package com.hls.streaming.dtos.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserLiteResponse {

    private String id;

    private String username;

    private String displayName;

    private String avatar;

    @Builder.Default
    @JsonProperty(value = "isActive")
    private boolean isActive = true;

    @Builder.Default
    @JsonProperty(value = "isVerified")
    private boolean isVerified = false;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "isAdmin")
    private Boolean isAdmin;
}
