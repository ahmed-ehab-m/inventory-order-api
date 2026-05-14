package com.global.order_api.feature.user;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// READING METHODS ////////////////////////////////////

    @Nested
    @DisplayName("1. Get User Tests (GET)")
    class GetUserTests {

        @Test
        void getUserById_WhenUserExists_ShouldReturnDto() {
            // Arrange
            Long userId = 1L;
            UserEntity userEntity = new UserEntity();
            userEntity.setId(userId);
            UserResponseDto expectedDto = new UserResponseDto();
            expectedDto.setId(userId);

            // Assuming BaseService calls userRepo.findById
            when(userRepo.findById(userId)).thenReturn(Optional.of(userEntity));
            when(userMapper.mapToDto(userEntity)).thenReturn(expectedDto);

            // Act
            UserResponseDto result = userService.getUserById(userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            verify(userRepo).findById(userId);
        }

        @Test
        void findByEmail_WhenUserExists_ShouldReturnDto() {
            String email = "test@company.com";
            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(email);
            UserResponseDto expectedDto = new UserResponseDto();
            expectedDto.setEmail(email);

            when(userRepo.findByEmail(email)).thenReturn(Optional.of(userEntity));
            when(userMapper.mapToDto(userEntity)).thenReturn(expectedDto);

            UserResponseDto result = userService.findByEmail(email);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(email);
        }

        @Test
        void findByEmail_WhenUserDoesNotExist_ShouldThrowException() {
            String email = "notfound@company.com";

            when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByEmail(email))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining(email);
        }

        @Test
        void getUsersPage_ShouldReturnPagedResponse() {
            // Arrange
            UserFilterRequest filter = new UserFilterRequest();
            filter.setPage(0);
            filter.setSize(10);
            filter.setSortBy("id");
            filter.setSortDirection("ASC");

            UserEntity userEntity = new UserEntity();
            UserResponseDto responseDto = new UserResponseDto();

            Page<UserEntity> page = new PageImpl<>(List.of(userEntity));
            List<UserResponseDto> dtoList = List.of(responseDto);

            when(userRepo.findAll(ArgumentMatchers.<Specification<UserEntity>>any(), ArgumentMatchers.any(Pageable.class)))
                    .thenReturn(page);
            when(userMapper.mapToDtoList(page.getContent())).thenReturn(dtoList);

            // Act
            PageResponse<UserResponseDto> result = userService.getUsersPage(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getData().size()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// UPDATE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("2. Update User Tests (PUT)")
    class UpdateUserTests {

        @Test
        void updateUser_WithValidData_ShouldUpdateAndReturnDto() {
            // Arrange
            Long userId = 1L;
            UserRequestDto updateRequest = new UserRequestDto();
            updateRequest.setEmail("new@company.com");
            updateRequest.setName("New Name");
            updateRequest.setPassword("newPass");

            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);
            existingUser.setEmail("old@company.com"); // Different email to test the check

            UserEntity updatedUser = new UserEntity();
            UserResponseDto responseDto = new UserResponseDto();

            when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepo.existsByEmail(updateRequest.getEmail())).thenReturn(false); // Email is available
            when(passwordEncoder.encode(updateRequest.getPassword())).thenReturn("encodedPass");
            when(userRepo.save(existingUser)).thenReturn(updatedUser);
            when(userMapper.mapToDto(updatedUser)).thenReturn(responseDto);

            // Act
            UserResponseDto result = userService.updateUser(userId, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(passwordEncoder).encode("newPass");
            verify(userRepo).save(existingUser);
        }

        @Test
        void updateUser_WhenEmailIsTakenByAnotherUser_ShouldThrowException() {
            // Arrange
            Long userId = 1L;
            UserRequestDto updateRequest = new UserRequestDto();
            updateRequest.setEmail("taken@company.com");

            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);
            existingUser.setEmail("old@company.com");

            when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepo.existsByEmail(updateRequest.getEmail())).thenReturn(true); // Email is taken!

            // Act & Assert
            assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                    .isInstanceOf(DuplicateRecordException.class)
                    .hasMessageContaining(updateRequest.getEmail());

            // Verify save is never called
            verify(userRepo, never()).save(any(UserEntity.class));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// DELETE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("3. Delete User Tests (DELETE)")
    class DeleteUserTests {

        @Test
        void softDeleteUser_ShouldCallBaseServiceDelete() {
            Long userId = 1L;
            UserEntity existingUser = new UserEntity();
            existingUser.setId(userId);

            // BaseService delete() logic: findById -> setDeleted(true) -> save
            when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));

            userService.softDeleteUser(userId);

            verify(userRepo).findById(userId);
            verify(userRepo).save(existingUser);
            // If you have a getter for isDeleted, you can assert existingUser.isDeleted() is true here
        }

        @Test
        void hardDeleteUser_WhenUserExists_ShouldDelete() {
            Long userId = 1L;
            when(userRepo.existsById(userId)).thenReturn(true);

            assertDoesNotThrow(() -> userService.hardDeleteUser(userId));

            verify(userRepo).hardDeleteUser(userId);
        }

        @Test
        void hardDeleteUser_WhenUserDoesNotExist_ShouldThrowException() {
            Long userId = 999L;
            when(userRepo.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.hardDeleteUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(userId.toString());

            verify(userRepo, never()).hardDeleteUser(anyLong());
        }

        @Test
        void restoreUser_ShouldCallRepoRestore() {
            Long userId = 1L;

            assertDoesNotThrow(() -> userService.restoreUser(userId));

            verify(userRepo).restoreUser(userId);
        }
    }
}