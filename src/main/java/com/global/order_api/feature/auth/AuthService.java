package com.global.order_api.feature.auth;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.security.JwtService;
import com.global.order_api.feature.user.*;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService extends BaseService<UserEntity, Long> {

    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepo userRepo, UserMapper userMapper, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        super(userRepo);
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /// /////////////////////////////////
    /// WRITE METHODS
    /// / REGISTER USER
    @Transactional
    public AuthResponseDto register(UserRequestDto user) {
        //// 1=>  check email first
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new DuplicateRecordException("User", "email", user.getEmail());
        }
        //// 2=> map to Dto
        UserEntity userEntity = userMapper.mapToEntity(user);

        //// 3=> encoding password
        userEntity.setPassword(passwordEncoder.encode(user.getPassword()));

        //// 4=> Set Role
        userEntity.setRole(UserRole.USER);

        //// 5=> save in DB
        UserEntity savedUser = userRepo.save(userEntity);

        /// 6=> Auto Login => Generate token for new user
        /// to go home page direct not login page to take token
        String token = jwtService.generateToken(new UserPrincipal(savedUser));

        /// 7=> map to dto
        UserResponseDto userResponseDto = userMapper.mapToDto(savedUser);

        /// 8=> return token + user data
        return new AuthResponseDto(token, userResponseDto);

    }

    /// / LOGIN USER
    public AuthResponseDto login(UserRequestDto user) {
        //// 1=>  check email and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
        );
        //// 2=> get user from db
        UserEntity userEntity = userRepo.findByEmail(user.getEmail()).orElseThrow();

        //// 3=> generate jwt token
        String token = jwtService.generateToken(new UserPrincipal(userEntity));

        //// 4=> Map Entity to DTO
        UserResponseDto userResponseDto = userMapper.mapToDto(userEntity);

        /// return user data +token
        return new AuthResponseDto(token, userResponseDto);
    }
}
