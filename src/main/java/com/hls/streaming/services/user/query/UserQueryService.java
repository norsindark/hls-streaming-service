package com.hls.streaming.services.user.query;

import com.hls.streaming.config.error.ErrorCodeConfig;
import com.hls.streaming.constant.ErrorConfigConstants;
import com.hls.streaming.dtos.user.UserLiteResponse;
import com.hls.streaming.enums.UserStatusEnum;
import com.hls.streaming.exception.NotFoundException;
import com.hls.streaming.repositories.user.UserRepository;
import com.hls.streaming.security.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final ErrorCodeConfig errorCodeConfig;

    public UserLiteResponse getProfile(String userId) {

        var user = userRepository.findById(new ObjectId(userId))
                .orElseThrow(() -> new NotFoundException(
                        errorCodeConfig.getMessage(ErrorConfigConstants.USER_NOT_FOUND)));

        return UserLiteResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatar(user.getAvatar())
                .isActive(UserStatusEnum.ACTIVE.equals(user.getStatus()))
                .isAdmin(user.getRoles().contains(UserRole.ADMIN))
                .build();
    }
}
