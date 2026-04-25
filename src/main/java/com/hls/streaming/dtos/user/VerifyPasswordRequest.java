package com.hls.streaming.dtos.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPasswordRequest {

    @JsonIgnore
    private String userId;

    @NotBlank(message = "Password is required")
    private String password;
}
