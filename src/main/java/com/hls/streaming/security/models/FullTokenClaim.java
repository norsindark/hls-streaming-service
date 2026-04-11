package com.hls.streaming.security.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullTokenClaim {
    private String iss;
    private TokenClaim tokenClaim;
    private Long exp;
    private Long iat;
    private String jti;
}
