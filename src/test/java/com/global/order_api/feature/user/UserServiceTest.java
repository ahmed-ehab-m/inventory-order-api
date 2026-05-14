package com.global.order_api.feature.user;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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

    @InjectMocks
    private UserService userService;


    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Users Tests (GET)")
    class GetUsersTests {

        ///// Get User By Id - RETURN DTO
        @Test
        void getUserById_WhenUserExists_ShouldReturnDto() {
            // 1. Arrange
            Long userId = 1L;
            UserEntity userEntity = new UserEntity();
            userEntity.setId(userId);

            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(userId);

            // BaseService calls findByIdOrThrow
            when(userRepo.findByIdOrThrow(userId)).thenReturn(userEntity);
            when(userMapper.mapToDto(userEntity)).thenReturn(responseDto);

            // 2. Act
            UserResponseDto result = userService.getUserById(userId);

            // 3. Assert
            assertNotNull(result);
            assertEquals(userId, result.getId());

            verify(userRepo, times(1)).findByIdOrThrow(userId);
            verify(userMapper, times(1)).mapToDto(userEntity);
        }

        ///// Get User By Id - Does Not Exist - THROW EXCEPTION
        @Test
        void getUserById_WhenUserDoesNotExist_ShouldThrowException() {
            Long userId = 999L;

            when(userRepo.findByIdOrThrow(userId))
                    .thenThrow(new ResourceNotFoundException("User", "id", userId));

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));

            verify(userMapper, never()).mapToDto(any());
        }

        ///// Find By Email - RETURN DTO
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
            verify(userRepo, times(1)).findByEmail(email);
        }

        ///// Find By Email - Does Not Exist - THROW EXCEPTION
        @Test
        void findByEmail_WhenUserDoesNotExist_ShouldThrowException() {
            String email = "notfound@company.com";

            when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> userService.findByEmail(email));
        }

        ///// Get Users Page - RETURN PAGE RESPONSE
        @Test
        void getUsersPage_ShouldReturnPagedUsers() {
            UserFilterRequest filter = new UserFilterRequest();

            UserEntity fakeEntity = new UserEntity();
            Page<UserEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            UserResponseDto fakeDto = new UserResponseDto();

            when(userRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockEntityPage);
            when(userMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            PageResponse<UserResponseDto> result = userService.getUsersPage(filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());

            verify(userRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(userMapper, times(1)).mapToDtoList(anyList());
        }

        ///// Get Users Page - No Users in DB - RETURN EMPTY PAGE
        @Test
        void getUsersPage_WhenNoUsersExist_ShouldReturnEmptyPage() {
            UserFilterRequest filter = new UserFilterRequest();


            Page<UserEntity> emptyMockPage = new PageImpl<>(List.of());

            when(userRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyMockPage);
            when(userMapper.mapToDtoList(emptyMockPage.getContent())).thenReturn(List.of());

            PageResponse<UserResponseDto> result = userService.getUsersPage(filter);

            assertNotNull(result);
            assertTrue(result.getData().isEmpty());
            assertEquals(0, result.getTotalElements());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Update User Tests (PUT)")
    class UpdateUserTests {

        ///// Update User - Valid Data - Should Update & Return DTO
        @Test
        void updateUser_WithValidData_ShouldUpdateUser() {
            // 1. Arrange
            Long userId = 1L;
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("new@company.com");
            requestDto.setName("New Name");
            requestDto.setPassword("newPass");

            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);
            existingUser.setEmail("old@company.com"); // Different email

            UserEntity savedUser = new UserEntity();
            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setEmail("new@company.com");

            when(userRepo.findByIdOrThrow(userId)).thenReturn(existingUser);
            when(userRepo.existsByEmail(requestDto.getEmail())).thenReturn(false); // Email not taken
            when(passwordEncoder.encode(requestDto.getPassword())).thenReturn("hashedPass");
            when(userRepo.save(existingUser)).thenReturn(savedUser);
            when(userMapper.mapToDto(savedUser)).thenReturn(responseDto);

            // 2. Act
            UserResponseDto result = userService.updateUser(userId, requestDto);

            // 3. Assert
            assertNotNull(result);
            assertEquals("new@company.com", result.getEmail());

            verify(passwordEncoder, times(1)).encode("newPass");
            verify(userRepo, times(1)).save(existingUser);
        }

        ///// Update User - Email Already Taken - Should Throw Exception
        @Test
        void updateUser_WhenEmailIsTaken_ShouldThrowException() {
            Long userId = 1L;
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("taken@company.com");

            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);
            existingUser.setEmail("old@company.com");

            when(userRepo.findByIdOrThrow(userId)).thenReturn(existingUser);
            when(userRepo.existsByEmail(requestDto.getEmail())).thenReturn(true); // Email is taken!

            assertThrows(DuplicateRecordException.class, () -> userService.updateUser(userId, requestDto));

            verify(userRepo, never()).save(any());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("3. Delete & Restore User Tests (DELETE / PUT)")
    class DeleteUserTests {

        ///// Soft Delete User
        @Test
        void softDeleteUser_ShouldCallBaseServiceDelete() {
            Long userId = 1L;
            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);

            // BaseService delete() logic -> findByIdOrThrow -> setDeleted(true) -> save
            when(userRepo.findByIdOrThrow(userId)).thenReturn(existingUser);

            userService.softDeleteUser(userId);

            verify(userRepo, times(1)).findByIdOrThrow(userId);
            verify(userRepo, times(1)).deleteById(userId); // Verifies that BaseService called save
        }

        ///// Hard Delete User - Exists - Should Hard Delete
        @Test
        void hardDeleteUser_WhenUserExists_ShouldHardDelete() {
            Long userId = 1L;

            when(userRepo.existsById(userId)).thenReturn(true);
            doNothing().when(userRepo).hardDeleteUser(userId);

            userService.hardDeleteUser(userId);

            verify(userRepo, times(1)).hardDeleteUser(userId);
        }

        ///// Hard Delete User - Not Exists - Should Throw Exception
        @Test
        void hardDeleteUser_WhenUserDoesNotExist_ShouldThrowException() {
            Long userId = 999L;

            when(userRepo.existsById(userId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> userService.hardDeleteUser(userId));

            verify(userRepo, never()).hardDeleteUser(any());
        }

        ///// Restore User
        @Test
        void restoreUser_ShouldCallRepoRestore() {
            Long userId = 1L;

            doNothing().when(userRepo).restoreUser(userId);

            userService.restoreUser(userId);

            verify(userRepo, times(1)).restoreUser(userId);
        }
        ///// Restore User - Not Exists - Should Throw Exception
        @Test
        void restoreUser_WhenUserDoesNotExist_ShouldThrowException() {
            Long userId = 999L;

            when(userRepo.existsById(userId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> userService.restoreUser(userId));

            verify(userRepo, never()).restoreUser(any());
        }
    }
}