package com.hls.streaming.infrastructure.security.utils;

import com.hls.streaming.infrastructure.config.error.ErrorCodeConfig;
import com.hls.streaming.common.exception.NotFoundException;
import com.hls.streaming.infrastructure.security.context.AppSecurityContextHolder;
import com.hls.streaming.security.authentication.model.FullTokenClaim;
import com.hls.streaming.security.authentication.model.JwtUserDetails;
import com.hls.streaming.security.authentication.model.TokenClaim;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static com.hls.streaming.common.constant.ErrorConfigConstants.TOKEN_INVALID;

@UtilityClass
public class SecurityUtils {

    public static Optional<TokenClaim> getTokenClaimFromSecurityContext() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserDetails jwtUserDetails) {
            return Optional.of(jwtUserDetails.getTokenClaim());
        }
        return Optional.empty();
    }

    public static Optional<FullTokenClaim> getFullTokenClaimFromAppSecurityContext() {
        final var appSecurityContextHolderOpt = AppSecurityContextHolder.getContext();
        if (appSecurityContextHolderOpt.isPresent()) {
            final var fullTokenClaim = appSecurityContextHolderOpt.get().getFullTokenClaim();
            return null != fullTokenClaim ? Optional.of(fullTokenClaim) : Optional.empty();
        }
        return Optional.empty();
    }

    public static String getUserIdFromToken(final ErrorCodeConfig errorCodeConfig) {
        return SecurityUtils.getTokenClaimFromSecurityContext()
                .orElseThrow(() -> new NotFoundException(errorCodeConfig.getMessage(TOKEN_INVALID)))
                .getUserId();
    }
}
