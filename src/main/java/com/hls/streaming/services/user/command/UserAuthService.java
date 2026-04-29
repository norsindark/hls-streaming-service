package com.hls.streaming.services.user.command;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.IdentifyUserRequest;
import com.hls.streaming.dtos.user.VerifyPasswordRequest;
import com.hls.streaming.enums.UserFlowStatusEnum;
import com.hls.streaming.exception.NotFoundException;
import com.hls.streaming.features.mapper.token.TokenMapper;
import com.hls.streaming.repositories.user.UserRepository;
import com.hls.streaming.security.models.TokenType;
import com.hls.streaming.services.token.TokenFacade;
import com.hls.streaming.services.user.internal.UserValidator;
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
