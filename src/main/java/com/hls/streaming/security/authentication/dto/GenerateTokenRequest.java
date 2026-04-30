package com.hls.streaming.security.authentication.dto;

import com.hls.streaming.security.authentication.model.TokenType;
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
