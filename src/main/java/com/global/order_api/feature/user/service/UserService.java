package com.global.order_api.feature.user.service;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.auth.dtos.ChangePasswordRequestDto;
import com.global.order_api.feature.auth.dtos.RegisterRequestDto;
import com.global.order_api.feature.auth.dtos.UserResponseDto;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.mapper.UserMapper;
import com.global.order_api.feature.user.repo.UserRepo;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService extends BaseService<UserEntity, Long> {
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    public UserService(UserRepo userRepo, UserMapper userMapper, PasswordEncoder passwordEncoder, CacheManager cacheManager) {
        super(userRepo);
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.cacheManager = cacheManager;
    }

    // ==================================================================================
    //                                1. READ METHODS
    // ==================================================================================

    public UserResponseDto getUserById(Long id) {
        UserEntity userEntity = findById(id);
        return userMapper.mapToDto(userEntity);
    }

    public UserResponseDto findByEmail(String email) {
        UserEntity userEntity = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.mapToDto(userEntity);
    }

    // ==================================================================================
    //                                2. WRITE & UPDATE METHODS
    // ==================================================================================

    @Transactional
    public UserResponseDto updateUser(Long id, RegisterRequestDto updateRequest) {
        UserEntity existingUser = findById(id);
        String oldEmail = existingUser.getEmail();

        if (!oldEmail.equals(updateRequest.getEmail())
                && userRepo.existsByEmail(updateRequest.getEmail())) {
            throw new DuplicateRecordException("User", "email", updateRequest.getEmail());
        }

        existingUser.setName(updateRequest.getName());
        existingUser.setEmail(updateRequest.getEmail());

        if (updateRequest.getLocation() != null && !updateRequest.getLocation().isBlank()) {
            existingUser.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getPhone() != null && !updateRequest.getPhone().isBlank()) {
            existingUser.setPhone(updateRequest.getPhone());
        }

        evictUserCache(oldEmail);
        evictUserCache(updateRequest.getEmail());

        return userMapper.mapToDto(userRepo.save(existingUser));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDto changePasswordRequestDto) {
        UserEntity user = findById(userId);

        if (!passwordEncoder.matches(changePasswordRequestDto.getOldPassword(), user.getPassword())) {
            throw new BusinessLogicException("error.password.incorrect");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequestDto.getNewPassword()));
        userRepo.save(user);

        evictUserCache(user.getEmail());
    }

    // ==================================================================================
    //                                3. SOFT DELETE (DEACTIVATE)
    // ==================================================================================

    @Transactional
    public void softDeleteUser(Long id) {
        UserEntity user = userRepo.findByIdOrThrow(id);
        if (user.isDeleted()) {
            return;
        }

        String oldEmail = user.getEmail();
        String changedEmail = oldEmail + "_deleted_" + System.currentTimeMillis();

        user.setEmail(changedEmail);
        user.setDeleted(true);
        userRepo.save(user);
        /// must login again
        /// because loadUserByUserName() => throw ResourceNotFoundException
        /// so front-end get 401 Unauthorized.
        /// display Login Page
        evictUserCache(oldEmail);
    }

    // ==================================================================================
    //                                4. HELPER METHODS
    // ==================================================================================

    public void evictUserCache(String email) {
        Cache cache = cacheManager.getCache("security-users");
        if (cache != null && email != null) {
            cache.evict(email);
        }
    }
}