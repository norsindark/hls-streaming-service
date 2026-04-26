package com.hls.streaming.dtos.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    private String id;

    private String username;
    private String email;

    private String displayName;

    private String avatar;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isVerified = false;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isAdmin;
}
