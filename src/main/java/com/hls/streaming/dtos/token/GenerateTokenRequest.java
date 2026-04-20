package com.hls.streaming.dtos.token;

import com.hls.streaming.security.models.TokenType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTokenRequest {
    private String userId;
    private TokenType tokenType;
}
