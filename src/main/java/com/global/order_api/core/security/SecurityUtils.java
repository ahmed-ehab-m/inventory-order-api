package com.global.order_api.core.security;

import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.feature.user.entity.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /// 1. Safe Method (For Auditing & Background Jobs)
    public static Long getCurrentUserIdSafe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userDetails) {
            return userDetails.getId();
        }

        return null;
    }
    /// 2. Strict Method (For Business Logic / Controllers)
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
        throw new BusinessLogicException("error.security.context");
    }
}
