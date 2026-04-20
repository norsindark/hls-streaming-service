package com.hls.streaming.security.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenClaim implements Serializable {

    @Serial
    private static final long serialVersionUID = -7810490836497183603L;

    private String userId;
    private Set<UserRole> privileges;
    private TokenType type;
}
