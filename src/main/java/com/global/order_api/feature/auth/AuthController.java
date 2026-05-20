package com.global.order_api.feature.auth;

import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Management",description = "APIs for managing Authentication")
public class AuthController
{
    private  final AppTranslator appTranslator;
    private static  final String ENTITY_KEY="entity.user";
    private final SocialAuthService socialAuthService;
    private final AuthService authService;

    /// Auth METHODS
    /// @validated => understand validation groups
    @PostMapping("/register")
    @Operation(summary = "Register New User", description = "Creates a new user account and returns a JWT token. Public access.")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Validated(ValidationGroups.OnRegister.class) @RequestBody UserRequestDto user)
    {
        AuthResponseDto authResponseDto=authService.register(user);
        String message=appTranslator.getTranslatedAction("success.created", ENTITY_KEY);
        ApiResponse<AuthResponseDto> apiResponse=ApiResponse.success(authResponseDto,message);
        return  ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user by email and password, and returns a JWT token. Public access.")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Validated(ValidationGroups.OnLogin.class) @RequestBody UserRequestDto user)
    {
        AuthResponseDto authResponseDto=authService.login(user);
        String message=appTranslator.translateMessage("success.login");
        ApiResponse<AuthResponseDto> apiResponse=ApiResponse.success(authResponseDto,message);
        return  ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/success")
    @Operation(summary = "OAuth2 Success Redirect", description = "Endpoint redirected to after successful OAuth2 login.")
    public ResponseEntity<ApiResponse<String>> oauth2Success() {
        String message = appTranslator.getTranslatedAction("success.login", ENTITY_KEY);
        ApiResponse<String> apiResponse = ApiResponse.success("Successfully logged in via Google/GitHub", message);
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Logout User", description = "Clears the authentication cookie to log the user out (For Web Clients).")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response)
    {
        /// 1=> create cookie with the same name
        Cookie cookie=new Cookie("jwt_token",null);
        /// 2=> delete cookie from browser
        cookie.setMaxAge(0);
        /// 3=> put same properties to the browser to know that => it is the old cookie
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");

        response.addCookie(cookie);

        /// 4=> for front-end
        String message = appTranslator.getTranslatedAction("success.logout", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message != null ? message : "Successfully logged out");

        return ResponseEntity.ok(apiResponse);
    }

    ///Mobile Google SignIN
    @Operation(summary = "Google Login for Mobile", description = "Receives a Google token from mobile client and returns a JWT.")
    @PostMapping("/social/google")
    public ResponseEntity<ApiResponse<String>> loginWithGoogleMobile(
            @Valid @RequestBody SocialLoginRequestDto request) {
        String jwtToken=socialAuthService.loginWithGoogle(request.getToken());
        String message = appTranslator.getTranslatedAction("success.login", ENTITY_KEY);
        ApiResponse<String> apiResponse = ApiResponse.success(jwtToken, message != null ? message : "Login successful");
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Github Login for Mobile", description = "Receives a github  Access token from mobile client and returns a JWT.")
    @PostMapping("/social/github")
    public ResponseEntity<ApiResponse<String>> loginWithGitHubMobile(@Valid @RequestBody SocialLoginRequestDto request) {
        String jwtToken = socialAuthService.loginWithGitHub(request.getToken());
        String message = appTranslator.getTranslatedAction("success.login", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(jwtToken, message));
    }
}
