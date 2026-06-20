package com.global.order_api.feature.user;

import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.feature.user.dto.ChangePasswordRequestDto;
import com.global.order_api.feature.user.dto.UserRequestDto;
import com.global.order_api.feature.user.dto.UserResponseDto;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.mapper.UserMapper;
import com.global.order_api.feature.user.repo.UserRepo;
import com.global.order_api.feature.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache; // Mocking the cache object itself

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // leniency to avoid UnnecessaryStubbingException if a test doesn't use cache
        lenient().when(cacheManager.getCache("security-users")).thenReturn(cache);
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Users Tests (GET)")
    class GetUsersTests {

        @Test
        void getUserById_WhenUserExists_ShouldReturnDto() {
            Long userId = 1L;
            UserEntity userEntity = new UserEntity();
            userEntity.setId(userId);

            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(userId);

            when(userRepo.findByIdOrThrow(userId)).thenReturn(userEntity);
            when(userMapper.mapToDto(userEntity)).thenReturn(responseDto);

            UserResponseDto result = userService.getUserById(userId);

            assertNotNull(result);
            assertEquals(userId, result.getId());
        }

        @Test
        void findByEmail_WhenUserExists_ShouldReturnDto() {
            String email = "test@company.com";
            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(email);

            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setEmail(email);

            when(userRepo.findByEmail(email)).thenReturn(Optional.of(userEntity));
            when(userMapper.mapToDto(userEntity)).thenReturn(responseDto);

            UserResponseDto result = userService.findByEmail(email);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Update User & Password Tests (PUT)")
    class UpdateUserTests {

        @Test
        void updateUser_WithValidData_ShouldUpdateUserAndEvictCache() {
            Long userId = 1L;
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("new@company.com");
            requestDto.setName("New Name");

            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);
            existingUser.setEmail("old@company.com");

            when(userRepo.findByIdOrThrow(userId)).thenReturn(existingUser);
            when(userRepo.existsByEmail(requestDto.getEmail())).thenReturn(false);
            when(userRepo.save(existingUser)).thenReturn(existingUser);
            when(userMapper.mapToDto(existingUser)).thenReturn(new UserResponseDto());

            userService.updateUser(userId, requestDto);

            // Verify cache eviction for BOTH old and new emails
            verify(cacheManager, atLeastOnce()).getCache("security-users");
            verify(cache, times(1)).evict("old@company.com");
            verify(cache, times(1)).evict("new@company.com");
        }

        @Test
        void updateUser_WhenEmailIsTaken_ShouldThrowException() {
            Long userId = 1L;
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("taken@company.com");

            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);
            existingUser.setEmail("old@company.com");

            when(userRepo.findByIdOrThrow(userId)).thenReturn(existingUser);
            when(userRepo.existsByEmail(requestDto.getEmail())).thenReturn(true);

            assertThrows(DuplicateRecordException.class, () -> userService.updateUser(userId, requestDto));
            verify(userRepo, never()).save(any());
        }

        @Test
        void changePassword_WithCorrectOldPassword_ShouldUpdatePassword() {
            Long userId = 1L;
            ChangePasswordRequestDto requestDto = new ChangePasswordRequestDto();
            requestDto.setOldPassword("old123");
            requestDto.setNewPassword("new123");
            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setEmail("user@test.com");
            user.setPassword("hashedOld");

            when(userRepo.findByIdOrThrow(userId)).thenReturn(user);
            when(passwordEncoder.matches(requestDto.getOldPassword(), user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(requestDto.getNewPassword())).thenReturn("hashedNew");

            userService.changePassword(userId, requestDto);

            assertEquals("hashedNew", user.getPassword());
            verify(userRepo, times(1)).save(user);
            verify(cache, times(1)).evict("user@test.com"); // verify cache eviction
        }

        @Test
        void changePassword_WithIncorrectOldPassword_ShouldThrowException() {
            Long userId = 1L;
            UserEntity user = new UserEntity();
            user.setPassword("hashedOld");

            ChangePasswordRequestDto requestDto = new ChangePasswordRequestDto();
            requestDto.setOldPassword("old123");
            requestDto.setNewPassword("new123");

            when(userRepo.findByIdOrThrow(userId)).thenReturn(user);
            when(passwordEncoder.matches(requestDto.getOldPassword(), user.getPassword())).thenReturn(false);

            assertThrows(BusinessLogicException.class, () -> userService.changePassword(userId, requestDto));
            verify(userRepo, never()).save(any());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("3. Soft Delete Tests (DELETE)")
    class SoftDeleteTests {

        @Test
        void softDeleteUser_ShouldChangeEmailAndEvictCache() {
            Long userId = 1L;
            String originalEmail = "test@test.com";

            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);
            existingUser.setEmail(originalEmail);
            existingUser.setDeleted(false);

            when(userRepo.findByIdOrThrow(userId)).thenReturn(existingUser);

            userService.softDeleteUser(userId);

            // Assert Email changed and marked as deleted
            assertTrue(existingUser.isDeleted());
            assertTrue(existingUser.getEmail().contains("_deleted_"));
            assertTrue(existingUser.getEmail().startsWith(originalEmail));

            verify(userRepo, times(1)).save(existingUser);
            verify(cache, times(1)).evict(originalEmail); // Cache of old email must be evicted
        }

        @Test
        void softDeleteUser_WhenAlreadyDeleted_ShouldDoNothing() {
            Long userId = 1L;
            UserEntity existingUser = new UserEntity();
            existingUser.setDeleted(true);

            when(userRepo.findByIdOrThrow(userId)).thenReturn(existingUser);

            userService.softDeleteUser(userId);

            verify(userRepo, never()).save(any());
            verify(cache, never()).evict(any());
        }
    }
}