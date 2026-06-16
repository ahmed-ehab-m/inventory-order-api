package com.global.order_api.feature.user;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class UserControllerTest {

    private static final String ENTITY_KEY = "entity.user";
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AppTranslator appTranslator;
    @Autowired
    private ObjectMapper objectMapper;

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// READING METHODS ///////////////////////////////////

    @Nested
    @DisplayName("1. Get Users Tests (GET)")
    class GetUsersTests {

        /// // Get All Users Page (Admin)
        @Test
        void getUsersPage_ShouldReturnPagedUsers() throws Exception {
            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(1L);

            List<UserResponseDto> dtoList = List.of(responseDto);
            org.springframework.data.domain.Page<UserResponseDto> dummyPage = new PageImpl<>(dtoList);
            PageResponse<UserResponseDto> fakePageResponse = PageResponse.from(dummyPage, dtoList);

            String fakeMessage = "Users retrieved successfully";

            when(userService.getUsersPage(ArgumentMatchers.any(UserFilterRequest.class)))
                    .thenReturn(fakePageResponse);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.data[0].id").value(1L))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        /// // Get User By ID - Success
        @Test
        void getUserById_ShouldReturnUser() throws Exception {
            Long userId = 1L;
            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(userId);

            String fakeMessage = "User retrieved successfully";

            when(userService.getUserById(userId)).thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.id").value(userId));
        }

        /// // Get User By ID - Not Found
        @Test
        void getUserById_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "User not found with id: " + invalidId;

            when(userService.getUserById(invalidId))
                    .thenThrow(new ResourceNotFoundException("User", "id", invalidId));
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(get("/api/v1/users/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// UPDATE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("2. Update User Tests (PUT)")
    class UpdateUserTests {

        /// // Update User - Success
        @Test
        void updateUser_WithValidData_ShouldReturnUpdatedUser() throws Exception {
            Long userId = 1L;
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setName("Updated Name");

            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(userId);
            responseDto.setName("Updated Name");

            String fakeMessage = "User updated successfully";

            when(userService.updateUser(eq(userId), ArgumentMatchers.any(UserRequestDto.class)))
                    .thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.id").value(userId));
        }

        /// // Update User - Not Found
        @Test
        void updateUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 999L;
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("ahmed@gmail.com");
            requestDto.setPassword("dsafsdffsdf");
            requestDto.setName("ahmed");
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "User not found with id: " + invalidId;

            when(userService.updateUser(eq(invalidId), ArgumentMatchers.any(UserRequestDto.class)))
                    .thenThrow(new ResourceNotFoundException("User", "id", invalidId));
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(put("/api/v1/users/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }

        /// / invalid DTO
        @Test
        void updateUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            Long userId = 1L;

            UserRequestDto invalidRequest = new UserRequestDto();

            mockMvc.perform(put("/api/v1/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest()) // يتوقع 400 Bad Request
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").exists()); // يتوقع إن الـ ExceptionHandler يرجع قائمة الأخطاء
        }


        /// // Soft Delete User - Success
        @Test
        void softDeleteUser_ShouldReturnOk() throws Exception {
            Long userId = 1L;
            String fakeMessage = "User deleted successfully";

            doNothing().when(userService).softDeleteUser(userId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/users/soft-delete/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Soft Delete User - Not Found
        @Test
        void softDeleteUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "User not found with id: " + invalidId;

            doThrow(new ResourceNotFoundException("User", "id", invalidId))
                    .when(userService).softDeleteUser(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(put("/api/v1/users/soft-delete/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }

        /// // Restore User - Success
        @Test
        void restoreUser_ShouldReturnOk() throws Exception {
            Long userId = 1L;
            String fakeMessage = "User restored successfully";

            doNothing().when(userService).restoreUser(userId);
            when(appTranslator.getTranslatedAction(eq("success.restored"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/users/restore-user/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Restore User - Not Found
        @Test
        void restoreUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "User not found with id: " + invalidId;

            doThrow(new ResourceNotFoundException("User", "id", invalidId))
                    .when(userService).restoreUser(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(put("/api/v1/users/restore-user/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// DELETE METHODS ////////////////////////////////////

    @Nested
    @DisplayName("3. Delete User Tests (DELETE)")
    class DeleteUserTests {

        /// // Hard Delete User - Success
        @Test
        void hardDeleteUser_ShouldReturnOk() throws Exception {
            Long userId = 1L;
            String fakeMessage = "User deleted successfully";

            doNothing().when(userService).hardDeleteUser(userId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/users/hard-delete/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Hard Delete User - Not Found
        @Test
        void hardDeleteUser_WhenUserNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "User not found with id: " + invalidId;

            doThrow(new ResourceNotFoundException("User", "id", invalidId))
                    .when(userService).hardDeleteUser(invalidId);
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(delete("/api/v1/users/hard-delete/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }
    }
}