package com.hls.streaming.dtos.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hls.streaming.dtos.user.UserLiteResponse;
import com.hls.streaming.enums.UserFlowStatusEnum;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAccessResponse {

    private UserFlowStatusEnum status;

    private String accessToken;
    private String refreshToken;
    private String createPasswordToken;
    private String otpVerificationToken;
    private String passwordVerificationToken;

    private UserLiteResponse userInfo;
    private Long availableTime;
}
