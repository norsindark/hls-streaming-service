package com.hls.streaming.user.domain.document;

import com.hls.streaming.user.domain.enums.UserStatusEnum;
import com.hls.streaming.security.authentication.model.UserRole;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "hls_users")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = -416249910350586534L;

    @Id
    private String id;

    @Version
    private Long version;

    @Indexed(unique = true)
    @Field("username")
    private String username;

    @Field("display_name")
    private String displayName;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password")
    private String password;

    @Field("avatar")
    private String avatar;

    @Builder.Default
    @Field("status")
    private UserStatusEnum status = UserStatusEnum.ACTIVE;

    @Field("detail")
    private UserDetail detail;

    @Field("roles")
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
