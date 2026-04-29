package com.hls.streaming.features.mapper.token;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.enums.UserFlowStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenMapperFacade {

    private final TokenMapper mapper;

    public UserAccessResponse toAccessResponse(final User user, final String accessToken, final String refreshToken) {
        var response = mapper.toAccessResponse(user, accessToken, refreshToken);
        response.setStatus(UserFlowStatusEnum.COMPLETED);

        return response;
    }
}
