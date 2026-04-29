package com.hls.streaming.features.mapper.token;

import com.hls.streaming.documents.user.User;
import com.hls.streaming.dtos.token.UserAccessResponse;
import com.hls.streaming.dtos.user.UserLiteResponse;
import com.hls.streaming.features.mapper.CentralConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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
    @Mapping(target = "isVerified", expression = "java(user.getStatus() == com.hls.streaming.enums.UserStatusEnum.ACTIVE)")
    @Mapping(target = "isAdmin", expression = "java(user.getRoles().contains(com.hls.streaming.security.models.UserRole.ADMIN))")

    @Mapping(target = "isActive", ignore = true)
    UserLiteResponse toLite(User user);
}
