    package com.global.order_api.feature.auth;

    import com.global.order_api.core.base.BaseService;
    import com.global.order_api.core.exception.BusinessLogicException;
    import com.global.order_api.core.exception.DuplicateRecordException;
    import com.global.order_api.core.security.JwtService;
    import com.global.order_api.feature.auth.dtos.AuthResponseDto;
    import com.global.order_api.feature.auth.dtos.UserRequestDto;
    import com.global.order_api.feature.auth.dtos.UserResponseDto;
    import com.global.order_api.feature.user.entity.UserEntity;
    import com.global.order_api.feature.user.entity.UserPrincipal;
    import com.global.order_api.feature.user.enums.UserRole;
    import com.global.order_api.feature.user.mapper.UserMapper;
    import com.global.order_api.feature.user.repo.UserRepo;
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

        // ==================================================================================
        //                                1. WRITING METHODS (USER)
        // ==================================================================================

        /// REGISTER USER
        @Transactional
        public AuthResponseDto register(UserRequestDto user) {
            //// 1=> check email first
            if (userRepo.existsByEmail(user.getEmail())) {
                throw new DuplicateRecordException("User", "email", user.getEmail());
            }

            //// 2=> map to Entity
            UserEntity userEntity = userMapper.mapToEntity(user);

            //// 3=> encoding password
            userEntity.setPassword(passwordEncoder.encode(user.getPassword()));

            //// 4=> Set Role
            userEntity.setRole(UserRole.USER);

            //// 5=> save in DB
            UserEntity savedUser = userRepo.save(userEntity);

            /// 6=> Auto Login => Generate Access & Refresh tokens
            UserPrincipal userPrincipal = new UserPrincipal(savedUser);
            String accessToken = jwtService.generateAccessToken(userPrincipal);
            String refreshToken = jwtService.generateRefreshToken(userPrincipal);

            /// 7=> map to dto
            UserResponseDto userResponseDto = userMapper.mapToDto(savedUser);

            /// 8=> return tokens + user data
            return new AuthResponseDto(accessToken, refreshToken, userResponseDto);
        }

        /// LOGIN USER
        public AuthResponseDto login(UserRequestDto user) {
            //// 1=> check email and password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );

            //// 2=> get user from db
            UserEntity userEntity = userRepo.findByEmail(user.getEmail()).orElseThrow();

            //// 3=> generate Access & Refresh tokens
            UserPrincipal userPrincipal = new UserPrincipal(userEntity);
            String accessToken = jwtService.generateAccessToken(userPrincipal);
            String refreshToken = jwtService.generateRefreshToken(userPrincipal);

            //// 4=> Map Entity to DTO
            UserResponseDto userResponseDto = userMapper.mapToDto(userEntity);

            /// return user data + tokens
            return new AuthResponseDto(accessToken, refreshToken, userResponseDto);
        }

        // ==================================================================================
        //                                2. REFRESH TOKEN METHOD
        // ==================================================================================

        /// REFRESH ACCESS TOKEN
        public AuthResponseDto refreshToken(String refreshToken) {
            String userEmail = jwtService.extractUserEmail(refreshToken);

            if (userEmail == null) {
                throw new BusinessLogicException("error.refresh.token.invalid");
            }

            /// get user from DB
            UserEntity userEntity = userRepo.findByEmail(userEmail)
                    .orElseThrow(() -> new BusinessLogicException("error.user.not.found", new Object[]{userEmail}));

             if (userEntity.isDeleted()) {
                 throw new BusinessLogicException("error.account.disabled");
             }

            UserPrincipal userPrincipal = new UserPrincipal(userEntity);

            /// validate out refresh token
            if (!jwtService.validateToken(refreshToken, userPrincipal)) {
                throw new BusinessLogicException("error.refresh.token.expired");
            }

            /// create new access token
            String newAccessToken = jwtService.generateAccessToken(userPrincipal);

            UserResponseDto userResponseDto = userMapper.mapToDto(userEntity);

            return new AuthResponseDto(newAccessToken, refreshToken, userResponseDto);
        }
    }