package com.hls.streaming.user.service.command;

import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.user.dto.IdentifyUserRequest;
import com.hls.streaming.user.dto.VerifyPasswordRequest;
import com.hls.streaming.user.domain.enums.UserFlowStatusEnum;
import com.hls.streaming.common.exception.NotFoundException;
import com.hls.streaming.security.authentication.mapper.TokenMapper;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.security.authentication.model.TokenType;
import com.hls.streaming.security.authentication.service.TokenFacade;
import com.hls.streaming.user.service.internal.UserValidator;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserRepository userRepository;
    private final TokenFacade tokenFacade;
    private final UserValidator validator;
    private final TokenMapper tokenMapper;

    public UserAccessResponse identify(IdentifyUserRequest request) {

        validator.validateIdentifier(request.getIdentifier());

        var user = userRepository.findByEmail(request.getIdentifier())
                .orElseGet(() -> userRepository.findByUsername(request.getIdentifier())
                        .orElseThrow(() -> new NotFoundException("User not found")));

        validator.validateUserActive(user);

        var token = tokenFacade.generateToken(user, TokenType.PASSWORD_VERIFICATION_TOKEN);

        return UserAccessResponse.builder()
                .userInfo(tokenMapper.toLite(user))
                .status(UserFlowStatusEnum.WAITING_FOR_VERIFICATION)
                .passwordVerificationToken(token)
                .build();
    }

    public UserAccessResponse verifyPassword(VerifyPasswordRequest request) {

        var user = userRepository.findById(new ObjectId(request.getUserId()))
                .orElseThrow(() -> new NotFoundException("User not found"));

        validator.validateUserActive(user);
        validator.validatePassword(request.getPassword(), user.getPassword());

        return tokenFacade.generateAccessTokenPair(user);
    }
}
