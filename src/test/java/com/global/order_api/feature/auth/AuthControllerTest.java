package com.global.order_api.feature.auth;

import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.auth.dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class AuthControllerTest {

    private static final String ENTITY_KEY = "entity.user";
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private SocialAuthService socialAuthService;
    @MockitoBean
    private AppTranslator appTranslator;
    @Autowired
    private ObjectMapper objectMapper;


    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        /// mock this first request
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// STANDARD AUTH /////////////////////////////////////

    @Nested
    @DisplayName("1. Standard Auth Tests (Register & Login)")
    class StandardAuthTests {

        /// // Register - Success
        @Test
        void register_WithValidData_ShouldReturnTokensAndUser() throws Exception {
            RegisterRequestDto requestDto = new RegisterRequestDto();
            requestDto.setEmail("test@gmail.com");
            requestDto.setPassword("password123");
            requestDto.setName("Test User");

            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(1L);
            userResponseDto.setEmail("test@gmail.com");

            /// will be returned from service (Access + Refresh)
            AuthResponseDto authResponseDto = new AuthResponseDto("fake.access.token", "fake.refresh.token", userResponseDto);
            String fakeMessage = "User registered successfully";

            when(authService.register(ArgumentMatchers.any(RegisterRequestDto.class))).thenReturn(authResponseDto);
            when(appTranslator.getTranslatedAction(eq("success.created"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.accessToken").value("fake.access.token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("fake.refresh.token"))
                    .andExpect(jsonPath("$.data.user.id").value(1L));
        }

        /// // Register - Invalid DTO (Bad Request)
        @Test
        void register_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            RegisterRequestDto invalidRequest = new RegisterRequestDto();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").exists());
        }

        /// // Login - Success
        @Test
        void login_WithValidData_ShouldReturnTokensAndUser() throws Exception {
            LoginRequestDto requestDto = new LoginRequestDto();
            requestDto.setEmail("test@gmail.com");
            requestDto.setPassword("password123");

            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(1L);

            AuthResponseDto authResponseDto = new AuthResponseDto("fake.access.token", "fake.refresh.token", userResponseDto);
            String fakeMessage = "Login successful";

            when(authService.login(ArgumentMatchers.any(LoginRequestDto.class))).thenReturn(authResponseDto);
            when(appTranslator.getTranslatedAction(eq("success.login"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.accessToken").value("fake.access.token"));
        }

        /// // Login - Invalid DTO (Bad Request)
        @Test
        void login_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            RegisterRequestDto invalidRequest = new RegisterRequestDto();

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").exists());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// SOCIAL AUTH ///////////////////////////////////////

    @Nested
    @DisplayName("2. Social Auth Tests (Mobile)")
    class SocialAuthTests {

        /// // Google Login - Success
        @Test
        void loginWithGoogleMobile_ShouldReturnTokens() throws Exception {
            SocialLoginRequestDto requestDto = new SocialLoginRequestDto();
            requestDto.setToken("google-mobile-token");

            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(1L);
            AuthResponseDto authResponseDto = new AuthResponseDto("google.access", "google.refresh", userResponseDto);

            String fakeMessage = "Login successful";

            when(socialAuthService.loginWithGoogle(requestDto.getToken())).thenReturn(authResponseDto);
            when(appTranslator.getTranslatedAction(eq("success.login"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/social/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.accessToken").value("google.access"));
        }

        /// // GitHub Login - Success
        @Test
        void loginWithGitHubMobile_ShouldReturnTokens() throws Exception {
            SocialLoginRequestDto requestDto = new SocialLoginRequestDto();
            requestDto.setToken("github-mobile-token");

            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(1L);
            AuthResponseDto authResponseDto = new AuthResponseDto("github.access", "github.refresh", userResponseDto);

            String fakeMessage = "Login successful";

            when(socialAuthService.loginWithGitHub(requestDto.getToken())).thenReturn(authResponseDto);
            when(appTranslator.getTranslatedAction(eq("success.login"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/social/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.accessToken").value("github.access"));
        }

        /// // Social Login - Invalid DTO
        @Test
        void loginWithGoogleMobile_WithInvalidDto_ShouldReturnBadRequest() throws Exception {
            SocialLoginRequestDto invalidRequest = new SocialLoginRequestDto(); // توكن فاضي

            mockMvc.perform(post("/api/v1/auth/social/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// LOGOUT & REFRESH //////////////////////////////////

    @Nested
    @DisplayName("3. Logout, Refresh & OAuth2 Success Tests")
    class LogoutAndOthersTests {

        /// // Logout - Should Clear Cookies
        @Test
        void logout_ShouldClearCookiesAndReturnSuccess() throws Exception {
            String fakeMessage = "Successfully logged out";

            when(appTranslator.getTranslatedAction(eq("success.logout"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    // بنشيك على الكوكيز الاتنين إنهم متسحبين (Max age = 0)
                    .andExpect(cookie().maxAge("access_token", 0))
                    .andExpect(cookie().maxAge("refresh_token", 0));
        }

        /// // Refresh Token - Success
        @Test
        void refreshToken_WithValidToken_ShouldReturnNewAccessToken() throws Exception {
            // 1. Arrange
            // عشان منعملش كلاس جديد للـ DTO بتاع الريكويست، بنعمله كـ Map ونحوله لـ JSON
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("refreshToken", "valid.old.refresh.token");

            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(1L);

            AuthResponseDto authResponseDto = new AuthResponseDto("new.access.token", "valid.old.refresh.token", userResponseDto);
            String fakeMessage = "Token refreshed successfully";

            when(authService.refreshToken("valid.old.refresh.token")).thenReturn(authResponseDto);
            when(appTranslator.getTranslatedAction(eq("success.token_refreshed"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            // 2 & 3. Act & Assert
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.accessToken").value("new.access.token"));
        }

        /// // OAuth2 Success Redirect
        @Test
        void oauth2Success_ShouldReturnSuccessMessage() throws Exception {
            String fakeMessage = "Successfully logged in via Google/GitHub";

            when(appTranslator.getTranslatedAction(eq("success.login"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/auth/success")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data").value("Successfully logged in via Google/GitHub"));
        }
    }
}