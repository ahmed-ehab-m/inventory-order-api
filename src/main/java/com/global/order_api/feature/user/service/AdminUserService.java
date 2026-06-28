package com.global.order_api.feature.user.service;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.user.enums.UserRole;
import com.global.order_api.feature.user.specification.UserFilterRequest;
import com.global.order_api.feature.auth.dtos.UserResponseDto;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.mapper.UserMapper;
import com.global.order_api.feature.user.repo.UserRepo;
import com.global.order_api.feature.user.specification.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService extends BaseService<UserEntity, Long> {
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final UserService userService;

    public AdminUserService(UserRepo userRepo, UserMapper userMapper, UserService userService) {
        super(userRepo);
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    // ==================================================================================
    //                                1. READ METHODS (ADMIN)
    // ==================================================================================

    public PageResponse<UserResponseDto> getUsersPage(UserFilterRequest filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        Specification<UserEntity> spec = UserSpecification.buildFilter(filter);

        Page<UserEntity> userEntityPage = userRepo.findAll(spec, pageable);
        List<UserResponseDto> dtos = userMapper.mapToDtoList(userEntityPage.getContent());

        return PageResponse.from(userEntityPage, dtos);
    }

    // ==================================================================================
    //                                2. HARD DELETE & RESTORE &
    // ==================================================================================

    @Transactional
    public void hardDeleteUser(Long id) {
        UserEntity user = userRepo.findByIdOrThrow(id);
        String email = user.getEmail();

        userRepo.hardDeleteUser(id);

        userService.evictUserCache(email);
    }

    @Transactional
    public void restoreUser(Long id) {
        UserEntity user = userRepo.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.isDeleted()) {
            throw new BusinessLogicException("error.account.active");
        }

        String currentEmail = user.getEmail();
        int deletedIndex = currentEmail.lastIndexOf("_deleted_");

        if (deletedIndex != -1) {
            String originalEmail = currentEmail.substring(0, deletedIndex);

            if (userRepo.existsByEmail(originalEmail)) {
                throw new BusinessLogicException("error.account.registered");
            }
            user.setEmail(originalEmail);

            userService.evictUserCache(originalEmail);
        }

        user.setDeleted(false);
        userRepo.save(user);
    }

    // ==================================================================================
    //                                3. UPDATE ROLE (ADMIN)
    // ==================================================================================

    @Transactional
    public void promoteUserToAdmin(Long id) {
        UserEntity user = userRepo.findByIdOrThrow(id);


        if ("ADMIN".equalsIgnoreCase(user.getRole().toString())) {
            throw new BusinessLogicException("error.user.already.admin");
        }

        user.setRole(UserRole.ADMIN);

        userRepo.save(user);

        userService.evictUserCache(user.getEmail());
    }


}