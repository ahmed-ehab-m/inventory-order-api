package com.global.order_api.feature.user;

import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.security.SecurityUtils;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.user.controller.UserController;
import com.global.order_api.feature.auth.dtos.ChangePasswordRequestDto;
import com.global.order_api.feature.auth.dtos.UserRequestDto;
import com.global.order_api.feature.auth.dtos.UserResponseDto;
import com.global.order_api.feature.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
class UserControllerTest {

    private static final String ENTITY_KEY = "entity.user";
    private static final Long CURRENT_USER_ID = 1L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;
    @MockitoBean private AppTranslator appTranslator;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    @Nested
    @DisplayName("User Profile Operations (Me)")
    class UserProfileTests {

        @Test
        void getMyProfile_ShouldReturnUser() throws Exception {
            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(CURRENT_USER_ID);
            String fakeMessage = "User retrieved successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(userService.getUserById(CURRENT_USER_ID)).thenReturn(responseDto);
                when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

                mockMvc.perform(get("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.id").value(CURRENT_USER_ID));
            }
        }

        @Test
        void updateMyProfile_WithValidData_ShouldReturnUpdatedUser() throws Exception {
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setName("Updated Name");
            requestDto.setEmail("test@test.com"); // Assuming this is required in DTO

            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(CURRENT_USER_ID);
            responseDto.setName("Updated Name");

            String fakeMessage = "User updated successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(userService.updateUser(eq(CURRENT_USER_ID), ArgumentMatchers.any(UserRequestDto.class))).thenReturn(responseDto);
                when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.name").value("Updated Name"));
            }
        }

        @Test
        void changePassword_WithValidData_ShouldReturnOk() throws Exception {
            ChangePasswordRequestDto requestDto = new ChangePasswordRequestDto();
            requestDto.setOldPassword("oldPassssss");
            requestDto.setNewPassword("newPassssss");

            String fakeMessage = "Password updated successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(userService).changePassword(CURRENT_USER_ID, requestDto);
                when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }

        @Test
        void deactivateMyAccount_ShouldReturnOk() throws Exception {
            String fakeMessage = "User deleted successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(userService).softDeleteUser(CURRENT_USER_ID);
                when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

                mockMvc.perform(delete("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }
    }
}