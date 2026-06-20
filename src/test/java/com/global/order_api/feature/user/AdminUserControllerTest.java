package com.global.order_api.feature.user;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.user.controller.AdminUserController;
import com.global.order_api.feature.user.specification.UserFilterRequest;
import com.global.order_api.feature.user.dto.UserResponseDto;
import com.global.order_api.feature.user.service.AdminUserService;
import com.global.order_api.feature.user.service.UserService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminUserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
class AdminUserControllerTest {

    private static final String ENTITY_KEY = "entity.user";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminUserService adminUserService;
    @MockitoBean private UserService userService; // Required because Admin Controller uses it for GetById & SoftDelete
    @MockitoBean private AppTranslator appTranslator;

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
    @DisplayName("Admin Users Operations")
    class AdminOperationsTests {

        @Test
        void getUsersPage_ShouldReturnPagedUsers() throws Exception {
            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(10L);
            PageResponse<UserResponseDto> fakePage = PageResponse.from(new PageImpl<>(List.of(responseDto)), List.of(responseDto));
            String fakeMessage = "Users retrieved successfully";

            when(adminUserService.getUsersPage(ArgumentMatchers.any(UserFilterRequest.class))).thenReturn(fakePage);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/admin/users")
                            .param("page", "0").param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.data[0].id").value(10L));
        }

        @Test
        void getUserById_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "User not found";

            when(userService.getUserById(invalidId)).thenThrow(new ResourceNotFoundException("User", "id", invalidId));
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class))).thenReturn(fakeErrorMessage);

            mockMvc.perform(get("/api/v1/admin/users/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }

        @Test
        void softDeleteUser_ShouldReturnOk() throws Exception {
            Long userId = 1L;
            String fakeMessage = "User deleted successfully";

            doNothing().when(userService).softDeleteUser(userId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/admin/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        @Test
        void hardDeleteUser_ShouldReturnOk() throws Exception {
            Long userId = 1L;
            String fakeMessage = "User deleted successfully";

            doNothing().when(adminUserService).hardDeleteUser(userId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/admin/users/{id}/force", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        @Test
        void restoreUser_ShouldReturnOk() throws Exception {
            Long userId = 1L;
            String fakeMessage = "User restored successfully";

            doNothing().when(adminUserService).restoreUser(userId);
            when(appTranslator.getTranslatedAction(eq("success.restored"), eq(ENTITY_KEY))).thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/admin/users/{id}/restore", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }
    }
}