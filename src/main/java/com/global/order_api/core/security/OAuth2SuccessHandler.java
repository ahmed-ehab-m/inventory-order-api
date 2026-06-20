package com.global.order_api.core.security;

import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.entity.UserPrincipal;
import com.global.order_api.feature.user.repo.UserRepo;
import com.global.order_api.feature.user.enums.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@RequiredArgsConstructor

/// after successful login (google , github) WEB
/// save user in DB
/// create access token , refresh token and pass it into cookies
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepo userRepo;
    private final JwtService jwtService;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.cookies.secure:false}")
    private boolean isCookieSecure;

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws
            IOException, ServletException {
        /// extract user details from google/githup
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String rawName = oAuth2User.getAttribute("name");
        /// because GitHub may dont't send "name" send "login"
        final String name = (rawName != null) ? rawName : oAuth2User.getAttribute("login");
        /// search the user in db or create a new user
        UserEntity user = userRepo.findByEmail(email)
                .orElseGet(() ->
                {
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setRole(UserRole.USER); /// default
                    newUser.setPassword(""); /// no password
                    return userRepo.save(newUser);
                });
        /// 3. GENERATE JWT TOKENS (Access & Refresh)
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);;
        /// 4. CREATE HTTP ONLY COOKIES
        setAuthCookies(response, accessToken, refreshToken);
        /// redirect user to simple end point
        getRedirectStrategy().sendRedirect(request, response, "/api/v1/auth/success");
    }


    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_COOKIE, accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(isCookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 minutes

        // 2. Refresh Token Cookie (7 أيام)
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(isCookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }
}

