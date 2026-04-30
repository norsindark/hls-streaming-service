package com.hls.streaming.security.authentication.mapper;

import com.hls.streaming.user.domain.document.User;
import com.hls.streaming.security.authentication.dto.UserAccessResponse;
import com.hls.streaming.user.dto.UserLiteResponse;
import com.hls.streaming.infrastructure.config.mapper.CentralConfig;
import com.hls.streaming.user.domain.enums.UserFlowStatusEnum;
import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.security.authentication.model.UserRole;
import org.springframework.util.CollectionUtils;
import org.mapstruct.*;
import java.util.Objects;

@Mapper(config = CentralConfig.class)
public interface TokenMapper {

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createPasswordToken", ignore = true)
    @Mapping(target = "otpVerificationToken", ignore = true)
    @Mapping(target = "passwordVerificationToken", ignore = true)
    @Mapping(target = "availableTime", ignore = true)
    @Mapping(target = "userInfo", source = "user", qualifiedByName = "toLite")
    UserAccessResponse toAccessResponse(
            User user,
            String accessToken,
            String refreshToken);

    @Named("toLite")
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "isAdmin", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    UserLiteResponse toLite(User user);

    @AfterMapping
    default void afterToLite(User user, @MappingTarget UserLiteResponse response) {
        if (Objects.nonNull(user) && Objects.nonNull(response)) {

            response.setVerified(Objects.equals(user.getStatus(), UserStatusEnum.ACTIVE));
            if (!CollectionUtils.isEmpty(user.getRoles())) {
                response.setIsAdmin(user.getRoles().contains(UserRole.ADMIN));
            } else {
                response.setIsAdmin(false);
            }
        }
    }

    @AfterMapping
    default void afterToAccessResponse(@MappingTarget UserAccessResponse response) {
        if (Objects.nonNull(response)) {
            response.setStatus(UserFlowStatusEnum.COMPLETED);
        }
    }
}
