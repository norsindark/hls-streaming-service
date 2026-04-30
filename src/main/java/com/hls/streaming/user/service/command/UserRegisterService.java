package com.hls.streaming.user.service.command;

import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.user.domain.document.UserDetail;
import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.user.dto.RegisterUserRequest;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.user.domain.repository.UserRepository;
import com.hls.streaming.security.authentication.model.UserRole;
import com.hls.streaming.security.authentication.service.TokenFacade;
import com.hls.streaming.user.service.internal.UserValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRegisterService {

    private final UserRepository userRepository;
    private final TokenFacade tokenFacade;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator validator;

    @Transactional
    public UserAccessResponse register(RegisterUserRequest request) {

        validator.validateRegisterInput(
                request.getUsername(),
                request.getEmail(),
                request.getPassword());

        validator.validateUsernameNotExists(request.getUsername());
        validator.validateEmailNotExists(request.getEmail());

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .displayName(StringUtils.defaultIfBlank(request.getDisplayName(), request.getUsername()))
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatusEnum.ACTIVE)
                .roles(Set.of(UserRole.USER))
                .detail(UserDetail.builder().enableNotify(true).build())
                .build();

        var saved = userRepository.save(user);

        return tokenFacade.generateAccessTokenPair(saved);
    }
}
