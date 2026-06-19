package com.global.order_api.feature.user;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserService userService; // Mocking the dependency to verify Cache Eviction

    @InjectMocks
    private AdminUserService adminUserService;

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Users Tests (Admin)")
    class GetUsersTests {

        @Test
        void getUsersPage_ShouldReturnPagedUsers() {
            UserFilterRequest filter = new UserFilterRequest();

            UserEntity fakeEntity = new UserEntity();
            Page<UserEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            UserResponseDto fakeDto = new UserResponseDto();

            when(userRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockEntityPage);
            when(userMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            PageResponse<UserResponseDto> result = adminUserService.getUsersPage(filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());

            verify(userRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////DELETE & RESTORE METHODS////////////////////////////

    @Nested
    @DisplayName("2. Hard Delete & Restore Tests (Admin)")
    class DeleteAndRestoreTests {

        @Test
        void hardDeleteUser_WhenUserExists_ShouldHardDeleteAndEvictCache() {
            Long userId = 1L;
            UserEntity user = new UserEntity();
            user.setEmail("admin@test.com");

            when(userRepo.findByIdOrThrow(userId)).thenReturn(user);
            doNothing().when(userRepo).hardDeleteUser(userId);

            adminUserService.hardDeleteUser(userId);

            verify(userRepo, times(1)).hardDeleteUser(userId);
            verify(userService, times(1)).evictUserCache("admin@test.com"); // check cache delegation
        }

        @Test
        void restoreUser_WithValidDeletedUser_ShouldRestoreAndEvictCache() {
            Long userId = 1L;
            String originalEmail = "test@company.com";
            String mangledEmail = originalEmail + "_deleted_123456";

            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setDeleted(true);
            user.setEmail(mangledEmail);

            when(userRepo.findByIdIncludingDeleted(userId)).thenReturn(Optional.of(user));
            when(userRepo.existsByEmail(originalEmail)).thenReturn(false); // Original email is free

            adminUserService.restoreUser(userId);

            assertFalse(user.isDeleted());
            assertEquals(originalEmail, user.getEmail());
            verify(userRepo, times(1)).save(user);
            verify(userService, times(1)).evictUserCache(originalEmail);
        }

        @Test
        void restoreUser_WhenUserIsAlreadyActive_ShouldThrowException() {
            Long userId = 1L;
            UserEntity user = new UserEntity();
            user.setDeleted(false); // Active User

            when(userRepo.findByIdIncludingDeleted(userId)).thenReturn(Optional.of(user));

            assertThrows(BusinessLogicException.class, () -> adminUserService.restoreUser(userId));
            verify(userRepo, never()).save(any());
        }

        @Test
        void restoreUser_WhenOriginalEmailIsTakenByAnotherUser_ShouldThrowException() {
            Long userId = 1L;
            String originalEmail = "taken@company.com";

            UserEntity user = new UserEntity();
            user.setDeleted(true);
            user.setEmail(originalEmail + "_deleted_9999");

            when(userRepo.findByIdIncludingDeleted(userId)).thenReturn(Optional.of(user));
            when(userRepo.existsByEmail(originalEmail)).thenReturn(true); // Email taken!

            assertThrows(BusinessLogicException.class, () -> adminUserService.restoreUser(userId));
            verify(userRepo, never()).save(any());
        }

        @Test
        void restoreUser_WhenUserDoesNotExist_ShouldThrowException() {
            Long userId = 999L;

            when(userRepo.findByIdIncludingDeleted(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> adminUserService.restoreUser(userId));
            verify(userRepo, never()).save(any());
        }
    }
}