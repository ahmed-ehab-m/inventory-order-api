package com.global.order_api.feature.user.entity;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    private UserEntity user;

    public UserPrincipal(UserEntity user) {
        this.user = user;
    }

    /// methods which spring security use it to authenticate
    @Override
    // ROLE
    public Collection<? extends GrantedAuthority> getAuthorities() {
        /// Collections.singleton => Because other systems the user may have more than one role
        /// singleton => create immutable list have only one element = better performance
        return Collections.singleton(
                /// Spring Security Role => Must Start with "ROLE_"
                /// SimpleGrantedAuthority => Object Which Spring Security Can read
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()) /// .name => return string
        );
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    // UserName => the unique field which user use it to login in our system
    // so we use email for login not userName
    public String getUsername() {
        return user.getEmail();
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
        return !user.isDeleted();
    }

    /// for me
    /// to get user data more easily in Controller
    public Long getId() {
        return user.getId();
    }

    /// for me
    /// to get user data more easily in Controller
    public UserEntity getUser() {
        return this.user;
    }

}
