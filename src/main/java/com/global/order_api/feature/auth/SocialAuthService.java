package com.global.order_api.feature.auth;

import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.security.JwtService;
import com.global.order_api.feature.user.dto.UserResponseDto;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.entity.UserPrincipal;
import com.global.order_api.feature.user.repo.UserRepo;
import com.global.order_api.feature.user.enums.UserRole;
import com.global.order_api.feature.user.mapper.UserMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/// FOR MOBILE AUTH
@Service
@RequiredArgsConstructor
@Log4j2
public class SocialAuthService {
    private final UserRepo userRepo;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final GoogleIdTokenVerifier googleVerifier;
    private final UserMapper userMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    //////////////////////////////////// MOBILE ONLY /////////////////////
    // ==================================================================================
    //                              1. LOGIN METHODS OAuth2
    // ==================================================================================

    /// MOBILE GOOGLE SIGN IN
    public AuthResponseDto loginWithGoogle(String googleToken) {
        try {
            /// 1=> check the token
            GoogleIdToken idToken = googleVerifier.verify(googleToken);

            /// 2=> if token is right
            if (idToken != null) {
                /// 3 => get the data
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                /// 4=> get user from our db or create a new user
                UserEntity user = findOrCreateUser(email, name != null ? name : "Google User");

                /// 5=> generate tokens and return response
                return buildAuthResponse(user);
            } else {
                throw new BusinessLogicException("error.token.invalid");
            }
        } catch (Exception e) {
            log.error("Google Auth Error: {}", e.getMessage());
            throw new BusinessLogicException("error.token.invalid");
        }
    }

    /// GITHUB LOGIN FOR MOBILE
    public AuthResponseDto loginWithGitHub(String githubAccessToken) {
        try {
            /// 1=> put the token in header
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(githubAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            /// 2=> talk to github to get user data
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String login = (String) body.get("login");
                String email = (String) body.get("email");

                /// if email is hidden or private
                if (email == null) {
                    email = login + "@github.com";
                }

                /// 3=> get user from our db or create a new user
                UserEntity user = findOrCreateUser(email, login);

                /// 4=> generate tokens and return response
                return buildAuthResponse(user);
            }
            throw new BusinessLogicException("error.token.invalid");
        } catch (Exception e) {
            log.error("GitHub Auth Error: {}", e.getMessage());
            throw new BusinessLogicException("error.token.invalid");
        }
    }

    // ==================================================================================
    //                              HELPER METHODS (تنظيف للكود)
    // ==================================================================================

    private UserEntity findOrCreateUser(String email, String name) {
        return userRepo.findByEmail(email)
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setRole(UserRole.USER);
                    newUser.setPassword("");
                    return userRepo.save(newUser);
                });
    }

    /// Helper Method for building DTO
    private AuthResponseDto buildAuthResponse(UserEntity user) {
        UserPrincipal userPrincipal = new UserPrincipal(user);

        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        UserResponseDto userResponseDto = userMapper.mapToDto(user);

        return new AuthResponseDto(accessToken, refreshToken, userResponseDto);
    }
}