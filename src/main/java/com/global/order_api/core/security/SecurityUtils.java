package com.global.order_api.core.security;

import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.feature.user.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessLogicException("error.unauthorized");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userDetails) {
            return userDetails.getId();
        }
        throw new BusinessLogicException("Cannot extract user info from security context");
    }
}
