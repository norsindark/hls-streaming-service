package com.hls.streaming.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ErrorConfigConstants {

    //----------------------Informational----------------------
    public static final long DEFAULT_ERROR = 1000100000L;

    //----------------------successful----------------------
    public static final long SUCCESS = 1000200000L;

    //----------------------redirection----------------------
    public static final long REDIRECTION = 1000300000L;

    //----------------------client error----------------------
    public static final long CLIENT_ERROR = 1000400000L;
    public static final long ACCESS_FORBIDDEN = 1000400002L;
    public static final long AUTHORIZATION_MUST = 1000400003L;
    public static final long PARSING_TOKEN_FAILED = 1000400004L;
    public static final long TOKEN_INVALID = 1000400005L;
    public static final long TOKEN_IS_MISSING = 1000400006L;
    public static final long TOKEN_EXPIRED = 1000400007L;
    public static final long NOT_FOUND_KMS_CRYPTO_KEY_INFO = 1000400008L;
    public static final long NOT_FOUND_SECRET_VERSION_EMPTY = 1000400009L;
    public static final long NOT_FOUND_SECRET_ID_INFO = 1000400010L;
    public static final long NOT_FOUND_SECRET_VERSION_INFO = 1000400011L;
    public static final long TOKEN_FROM_VERSION_INVALID = 1000400012L;
    public static final long CACHE_ID_NOT_NULL = 1000400013L;
    public static final long PROCESS_IS_LOCKED = 1000400014L;
    public static final long TOO_MANY_REQUESTS = 1000400015L;
    public static final long MISSING_PERMISSION = 1000400016L;
    public static final long MISSING_PRIVATE_KEY = 1000400017L;

    // -------- USER DOMAIN --------
    public static final long USER_REQUIRED_FIELDS_EMPTY = 1000400018L;
    public static final long USERNAME_ALREADY_EXISTS = 1000400019L;
    public static final long EMAIL_ALREADY_EXISTS = 1000400020L;
    public static final long USER_NOT_FOUND = 1000400021L;
    public static final long USER_INACTIVE = 1000400022L;
    public static final long INVALID_PASSWORD = 1000400023L;
    public static final long IDENTIFIER_EMPTY = 1000400024L;

    //----------------------server error----------------------
    public static final long INTERNAL_SERVER_ERROR = 1000500000L;
}
