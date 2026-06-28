package com.global.order_api.feature.auth;

import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.auth.dtos.AuthResponseDto;
import com.global.order_api.feature.auth.dtos.LoginRequestDto;
import com.global.order_api.feature.auth.dtos.SocialLoginRequestDto;
import com.global.order_api.feature.auth.dtos.RegisterRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Management", description = "APIs for managing Authentication")
public class AuthController {
    private static final String ENTITY_KEY = "entity.user";

    ///
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AppTranslator appTranslator;
    private final SocialAuthService socialAuthService;
    private final AuthService authService;

    /// in deploy = true , in localHost = false (default)
    @Value("${app.cookies.secure:false}")
    private boolean isCookieSecure;

    // ==================================================================================
    //                              1. LOGIN / REGISTER METHODS
    // ==================================================================================
    @PostMapping("/register")
    @Operation(summary = "Register New User", description = "Creates a new user account and returns Access & Refresh tokens.")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto user,
            HttpServletResponse response
    ) {
        AuthResponseDto authResponseDto = authService.register(user);
        setAuthCookies(response, authResponseDto.getAccessToken(), authResponseDto.getRefreshToken());

        String message = appTranslator.getTranslatedAction("success.created", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(authResponseDto, message));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user by email and password, and returns Access & Refresh tokens.")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto user,
            HttpServletResponse response) {
        AuthResponseDto authResponseDto = authService.login(user);
        setAuthCookies(response, authResponseDto.getAccessToken(), authResponseDto.getRefreshToken());
        String message = appTranslator.getTranslatedAction("success.login", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(authResponseDto, message));
    }

    // ==================================================================================
    //                              2. LOGIN METHODS OAuth2
    // ==================================================================================

    @GetMapping("/success")
    @Operation(summary = "OAuth2 Success Redirect", description = "Endpoint redirected to after successful OAuth2 login.")
    public ResponseEntity<ApiResponse<String>> oauth2Success() {
        String message = appTranslator.getTranslatedAction("success.login", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success("Successfully logged in via Google/GitHub", message));
    }

    /// Mobile Google SignIN
    @Operation(summary = "Google Login for Mobile", description = "Receives a Google token from mobile client and returns Access & Refresh tokens.")
    @PostMapping("/social/google")
    public ResponseEntity<ApiResponse<AuthResponseDto>> loginWithGoogleMobile(
            @Valid @RequestBody SocialLoginRequestDto request) {
        AuthResponseDto authResponseDto = socialAuthService.loginWithGoogle(request.getToken());
        String message = appTranslator.getTranslatedAction("success.login", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(authResponseDto, message));
    }

    /// Mobile GitHub SignIN
    @Operation(summary = "Github Login for Mobile", description = "Receives a github Access token from mobile client and returns Access & Refresh tokens.")
    @PostMapping("/social/github")
    public ResponseEntity<ApiResponse<AuthResponseDto>> loginWithGitHubMobile(@Valid @RequestBody SocialLoginRequestDto request) {
        AuthResponseDto authResponseDto = socialAuthService.loginWithGitHub(request.getToken());
        String message = appTranslator.getTranslatedAction("success.login", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(authResponseDto, message));
    }

    // ==================================================================================
    //                              3. LOGOUT METHOD
    // ==================================================================================
    @Operation(summary = "Logout User", description = "Clears the authentication cookies to log the user out (For Web Clients).")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE);
        clearCookie(response, REFRESH_TOKEN_COOKIE);

        String message = appTranslator.getTranslatedAction("success.logout", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // ==================================================================================
    //                              4. REFRESH TOKEN METHOD
    // ==================================================================================
        @Operation(summary = "Refresh Access Token", description = "Generates a new Access Token using a valid Refresh Token (from Body or Cookie).")
        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(
                @RequestBody(required = false) RefreshTokenRequestDto request,
                HttpServletRequest httpRequest,HttpServletResponse response) {

            String refreshToken = null;

            /// Mobile
            if (request != null && request.getRefreshToken() != null) {
                refreshToken = request.getRefreshToken();
            }
            /// WEB
            else if (httpRequest.getCookies() != null) {
                for (Cookie cookie : httpRequest.getCookies()) {
                    if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }

            if (refreshToken == null) {
                throw new BusinessLogicException("error.refresh.token.missing");
            }

            AuthResponseDto authResponseDto = authService.refreshToken(refreshToken);
            /// if web update cookie
            setAuthCookies(response, authResponseDto.getAccessToken(), authResponseDto.getRefreshToken());

            String message = appTranslator.getTranslatedAction("success.token_refreshed", ENTITY_KEY);
            return ResponseEntity.ok(ApiResponse.success(authResponseDto, message));
        }


        // ==================================================================================
    //                              HELPER METHODS
    // ==================================================================================

    private void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(isCookieSecure);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_COOKIE, accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(isCookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(isCookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }
}