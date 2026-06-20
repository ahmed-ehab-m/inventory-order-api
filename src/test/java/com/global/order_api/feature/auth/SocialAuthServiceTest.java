package com.global.order_api.feature.auth;

import com.global.order_api.core.security.JwtService;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.entity.UserPrincipal;
import com.global.order_api.feature.user.repo.UserRepo;
import com.global.order_api.feature.user.enums.UserRole;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GoogleIdTokenVerifier googleVerifier;

    @InjectMocks
    private SocialAuthService socialAuthService;

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// GOOGLE TESTS //////////////////////////////////////

    @Nested
    @DisplayName("1. Google Login Tests")
    class GoogleLoginTests {

        /// // Google Login - New User
        @Test
        void loginWithGoogle_WhenUserIsNew_ShouldSaveUserAndReturnToken() throws Exception {
            // 1. Arrange
            /// fake google and jwt token
            String googleToken = "valid_google_token";
            String fakeJwtToken = "jwt_token_from_our_system";
            String email = "newuser@google.com";

            //// build fake object (payload) response from google servers
            GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
            mockPayload.setEmail(email);
            mockPayload.set("name", "New Google User");
            /// google return payload in GoogleIdToken object
            /// and this object requires => header , payload , signature bytes for encryption
            GoogleIdToken mockIdToken = new GoogleIdToken(
                    new GoogleIdToken.Header(), mockPayload, new byte[0], new byte[0]
            );

            when(googleVerifier.verify(googleToken)).thenReturn(mockIdToken);
            /// user not found in our DB (first time)
            when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

            //// create entity to save it in db
            UserEntity savedUser = new UserEntity();
            savedUser.setEmail(email);
            savedUser.setRole(UserRole.USER);
            when(userRepo.save(any(UserEntity.class))).thenReturn(savedUser);

            //// take our fake jwt token
            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn(fakeJwtToken);

            // 2. Act
            //// jwt token
            String result = socialAuthService.loginWithGoogle(googleToken);

            // 3. Assert
            assertNotNull(result);
            assertEquals(fakeJwtToken, result);
            verify(userRepo, times(1)).save(any(UserEntity.class));
        }

        /// // Google Login - Existing User - not save user again
        @Test
        void loginWithGoogle_WhenUserAlreadyExists_ShouldReturnTokenWithoutSaving() throws Exception {
            String googleToken = "valid_google_token";
            String fakeJwtToken = "jwt_token_from_our_system";
            String email = "existing@google.com";

            GoogleIdToken.Payload mockPayload = new GoogleIdToken.Payload();
            mockPayload.setEmail(email);
            mockPayload.set("name", "Existing User");
            GoogleIdToken mockIdToken = new GoogleIdToken(
                    new GoogleIdToken.Header(), mockPayload, new byte[0], new byte[0]
            );

            when(googleVerifier.verify(googleToken)).thenReturn(mockIdToken);

            UserEntity existingUser = new UserEntity();
            existingUser.setEmail(email);
            when(userRepo.findByEmail(email)).thenReturn(Optional.of(existingUser));

            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn(fakeJwtToken);

            String result = socialAuthService.loginWithGoogle(googleToken);

            assertNotNull(result);
            assertEquals(fakeJwtToken, result);
            /// not save user again
            verify(userRepo, never()).save(any(UserEntity.class));
        }

        /// // Google Login - Invalid Token
        @Test
        void loginWithGoogle_WhenTokenIsInvalid_ShouldThrowException() throws Exception {
            String invalidToken = "invalid_google_token";


            when(googleVerifier.verify(invalidToken)).thenThrow(new IllegalArgumentException("Invalid ID token"));

            assertThrows(BadCredentialsException.class, () -> socialAuthService.loginWithGoogle(invalidToken));
            verify(userRepo, never()).findByEmail(anyString()); // مكملش للكود اللي بعده
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// GITHUB TESTS //////////////////////////////////////

    @Nested
    @DisplayName("2. GitHub Login Tests")
    class GitHubLoginTests {

        /// // GitHub Login - New User
        @Test
        void loginWithGitHub_WhenUserIsNew_ShouldSaveUserAndReturnToken() {
            String githubToken = "valid_github_token";
            String fakeJwtToken = "jwt_token_from_our_system";
            String email = "newuser@github.com";

            /// fake json response map from git hub
            Map<String, Object> githubResponseMap = new HashMap<>();
            githubResponseMap.put("login", "newGithubUser");
            githubResponseMap.put("email", email);
            //// add data into http response
            //// because rest template return data in response entity
            ResponseEntity<Map> responseEntity = new ResponseEntity<>(githubResponseMap, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq("https://api.github.com/user"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(responseEntity);

            /// first time
            when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

            UserEntity savedUser = new UserEntity();
            savedUser.setEmail(email);
            savedUser.setRole(UserRole.USER);
            when(userRepo.save(any(UserEntity.class))).thenReturn(savedUser);

            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn(fakeJwtToken);

            String result = socialAuthService.loginWithGitHub(githubToken);

            assertNotNull(result);
            assertEquals(fakeJwtToken, result);
            verify(userRepo, times(1)).save(any(UserEntity.class));
        }

        /// // GitHub Login - Existing User
        @Test
        void loginWithGitHub_WhenUserAlreadyExists_ShouldReturnTokenWithoutSaving() {
            String githubToken = "valid_github_token";
            String fakeJwtToken = "jwt_token_from_our_system";
            String email = "existing@github.com";

            Map<String, Object> githubResponseMap = new HashMap<>();
            githubResponseMap.put("login", "existingGithubUser");
            githubResponseMap.put("email", email);
            ResponseEntity<Map> responseEntity = new ResponseEntity<>(githubResponseMap, HttpStatus.OK);

            when(restTemplate.exchange(
                    eq("https://api.github.com/user"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenReturn(responseEntity);

            UserEntity existingUser = new UserEntity();
            existingUser.setEmail(email);
            when(userRepo.findByEmail(email)).thenReturn(Optional.of(existingUser));

            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn(fakeJwtToken);

            String result = socialAuthService.loginWithGitHub(githubToken);

            assertEquals(fakeJwtToken, result);
            verify(userRepo, never()).save(any(UserEntity.class));
        }

        /// // GitHub Login - Invalid Token
        @Test
        void loginWithGitHub_WhenTokenIsInvalid_ShouldThrowException() {
            String invalidToken = "invalid_github_token";

            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(Map.class)
            )).thenThrow(new RuntimeException("401 Unauthorized"));

            assertThrows(BadCredentialsException.class, () -> socialAuthService.loginWithGitHub(invalidToken));
            verify(userRepo, never()).findByEmail(anyString());
        }
    }
}