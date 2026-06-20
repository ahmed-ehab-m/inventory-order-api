package com.global.order_api.feature.auth;

import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.user.dto.UserRequestDto;
import com.global.order_api.feature.user.dto.UserResponseDto;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// STANDARD AUTH /////////////////////////////////////

    @Nested
    @DisplayName("1. Standard Auth Tests (Register & Login)")
    class StandardAuthTests {

        /// // Register - Success
        @Test
        void register_WithValidData_ShouldReturnTokenAndUser() throws Exception {
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("test@gmail.com");
            requestDto.setPassword("password123");
            requestDto.setName("Test User");

            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(1L);
            userResponseDto.setEmail("test@gmail.com");

            /// will be returned from service
            AuthResponseDto authResponseDto = new AuthResponseDto("fake-jwt-token", userResponseDto);
            String fakeMessage = "User registered successfully";

            when(authService.register(ArgumentMatchers.any(UserRequestDto.class))).thenReturn(authResponseDto);
            when(appTranslator.getTranslatedAction(eq("success.created"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.token").value("fake-jwt-token"))
                    .andExpect(jsonPath("$.data.user.id").value(1L));
        }

        /// // Register - Invalid DTO (Bad Request)
        @Test
        void register_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            UserRequestDto invalidRequest = new UserRequestDto();

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
        void login_WithValidData_ShouldReturnTokenAndUser() throws Exception {
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("test@gmail.com");
            requestDto.setPassword("password123");
            requestDto.setName("ahmed");

            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(1L);

            AuthResponseDto authResponseDto = new AuthResponseDto("fake-jwt-token", userResponseDto);
            String fakeMessage = "Login successful";

            when(authService.login(ArgumentMatchers.any(UserRequestDto.class))).thenReturn(authResponseDto);
            when(appTranslator.translateMessage("success.login")).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.token").value("fake-jwt-token"));
        }

        /// // Login - Invalid DTO (Bad Request)
        @Test
        void login_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            UserRequestDto invalidRequest = new UserRequestDto();

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
        void loginWithGoogleMobile_ShouldReturnToken() throws Exception {
            SocialLoginRequestDto requestDto = new SocialLoginRequestDto();
            requestDto.setToken("google-mobile-token");

            String fakeJwtToken = "jwt-from-google-auth";
            String fakeMessage = "Login successful";

            when(socialAuthService.loginWithGoogle(requestDto.getToken())).thenReturn(fakeJwtToken);
            when(appTranslator.getTranslatedAction(eq("success.login"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/social/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data").value(fakeJwtToken));
        }

        /// // GitHub Login - Success
        @Test
        void loginWithGitHubMobile_ShouldReturnToken() throws Exception {
            SocialLoginRequestDto requestDto = new SocialLoginRequestDto();
            requestDto.setToken("github-mobile-token");

            String fakeJwtToken = "jwt-from-github-auth";
            String fakeMessage = "Login successful";

            when(socialAuthService.loginWithGitHub(requestDto.getToken())).thenReturn(fakeJwtToken);
            when(appTranslator.getTranslatedAction(eq("success.login"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/social/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data").value(fakeJwtToken));
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
    /// ///////////////////////////////// LOGOUT & OTHERS ///////////////////////////////////

    @Nested
    @DisplayName("3. Logout & OAuth2 Success Tests")
    class LogoutAndOthersTests {

        /// // Logout - Should Clear Cookie
        @Test
        void logout_ShouldClearCookieAndReturnSuccess() throws Exception {
            String fakeMessage = "Successfully logged out";

            when(appTranslator.getTranslatedAction(eq("success.logout"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(cookie().maxAge("jwt_token", 0))
                    .andExpect(cookie().httpOnly("jwt_token", true));
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