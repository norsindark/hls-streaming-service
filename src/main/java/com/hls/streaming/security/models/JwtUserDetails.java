package com.hls.streaming.security.models;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JwtUserDetails implements UserDetails {

    private final TokenClaim claim;

    private final List<GrantedAuthority> authorities;

    public JwtUserDetails(final TokenClaim claim) {
        this.authorities = AuthorityUtils.createAuthorityList(
                claim.getPrivileges().stream()
                        .map(UserRole::toString)
                        .toList().toArray(String[]::new));
        this.claim = claim;
    }

    public TokenClaim getTokenClaim() {
        return claim;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.unmodifiableCollection(authorities);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return claim.getUserId().toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
