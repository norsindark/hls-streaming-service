package com.hls.streaming.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    private boolean isActive;

    private boolean isVerified;

    private Boolean isAdmin;
}
