package com.global.order_api.feature.auth;

import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.security.JwtService;
import com.global.order_api.feature.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo, userMapper, passwordEncoder, authenticationManager, jwtService);
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// REGISTER METHODS //////////////////////////////////

    @Nested
    @DisplayName("1. Register Tests")
    class RegisterTests {

        ///// Register - Success (New Email)
        @Test
        void register_WithValidData_ShouldReturnAuthResponse() {
            // 1. Arrange new user data from register form
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("newuser@test.com");
            requestDto.setPassword("password123");

            //// from mapper
            UserEntity mappedEntity = new UserEntity();
            mappedEntity.setEmail("newuser@test.com");

            UserEntity savedEntity = new UserEntity();
            savedEntity.setId(1L);
            savedEntity.setEmail("newuser@test.com");
            savedEntity.setRole(UserRole.USER);

            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(1L);
            responseDto.setEmail("newuser@test.com");

            //// from jwt service
            String fakeToken = "eyJhbGciOiJIUzI1NiIsInR...fakeToken";

            when(userRepo.existsByEmail(requestDto.getEmail())).thenReturn(false);
            when(userMapper.mapToEntity(requestDto)).thenReturn(mappedEntity);
            when(passwordEncoder.encode(requestDto.getPassword())).thenReturn("encodedPassword123");
            when(userRepo.save(mappedEntity)).thenReturn(savedEntity);
            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn(fakeToken);
            when(userMapper.mapToDto(savedEntity)).thenReturn(responseDto);

            // 2. Act
            AuthResponseDto result = authService.register(requestDto);

            // 3. Assert
            assertNotNull(result);
            assertEquals(fakeToken, result.getToken());
            assertEquals(1L, result.getUser().getId());
            assertEquals(UserRole.USER, mappedEntity.getRole());

            verify(userRepo, times(1)).save(mappedEntity);
            verify(passwordEncoder, times(1)).encode("password123");
        }

        ///// Register - Failed (Email Already Exists)
        @Test
        void register_WhenEmailExists_ShouldThrowException() {
            // 1. Arrange
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("existing@test.com");

            when(userRepo.existsByEmail(requestDto.getEmail())).thenReturn(true);

            // 2 & 3. Act & Assert
            assertThrows(DuplicateRecordException.class, () -> authService.register(requestDto));

            verify(userRepo, never()).save(any());
            verify(jwtService, never()).generateToken(any());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// LOGIN METHODS /////////////////////////////////////

    @Nested
    @DisplayName("2. Login Tests")
    class LoginTests {

        ///// Login - Success (Correct Credentials)
        @Test
        void login_WithValidCredentials_ShouldReturnAuthResponse() {
            // 1. Arrange
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("user@test.com");
            requestDto.setPassword("correctPassword");

            UserEntity dbUser = new UserEntity();
            dbUser.setId(1L);
            dbUser.setEmail("user@test.com");

            UserResponseDto responseDto = new UserResponseDto();
            responseDto.setId(1L);
            responseDto.setEmail("user@test.com");

            String fakeToken = "eyJhbGciOiJIUzI1NiIsInR...fakeToken";

            //// return null from auth manager to check only data is right
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);

            when(userRepo.findByEmail(requestDto.getEmail())).thenReturn(Optional.of(dbUser));
            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn(fakeToken);
            when(userMapper.mapToDto(dbUser)).thenReturn(responseDto);

            // 2. Act
            AuthResponseDto result = authService.login(requestDto);

            // 3. Assert
            assertNotNull(result);
            assertEquals(fakeToken, result.getToken());
            assertEquals(1L, result.getUser().getId());

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService, times(1)).generateToken(any(UserPrincipal.class));
        }

        ///// Login - Failed (Bad Credentials)
        @Test
        void login_WithInvalidCredentials_ShouldThrowException() {
            // 1. Arrange
            UserRequestDto requestDto = new UserRequestDto();
            requestDto.setEmail("user@test.com");
            requestDto.setPassword("wrongPassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // 2 & 3. Act & Assert
            assertThrows(BadCredentialsException.class, () -> authService.login(requestDto));

            verify(userRepo, never()).findByEmail(anyString());
            verify(jwtService, never()).generateToken(any());
        }
    }
}