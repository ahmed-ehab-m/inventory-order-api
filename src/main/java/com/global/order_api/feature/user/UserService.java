package com.global.order_api.feature.user;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
/// no @RequiredArgsConstructor because we want to send user repo to base service
public class UserService extends BaseService<UserEntity, Long> {
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        super(userRepo);
        this.userRepo = userRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }
    /////////////////////////
    /// READ METHODS

    /// GET BY ID FOR FRONT-END DASHBOARD
    public UserResponseDto getUserById(Long id) {
        /// from base Service
        UserEntity userEntity = findById(id);
        return userMapper.mapToDto(userEntity);
    }

    /// GET BY EMAIL

    public UserResponseDto findByEmail(String email) {
        UserEntity userEntity = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.mapToDto(userEntity);
    }

    /// //GET ALL
    /// smart method for pagination
    /// take filter => smart object contains page number , size ,sort type
    /// return pageResponse we created in core folder
    public PageResponse<UserResponseDto> getUsersPage(UserFilterRequest filter) {
        ///// 1=> take user input => "ASC" OR "DESC" from headers
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());

        ////// 2=> Pageable => take all user input
        ///// will be translated to SQL
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        ///// holds data + meta data about it
        Specification<UserEntity> spec = UserSpecification.buildFilter(filter);
        //// 3=> call Repo
        Page<UserEntity> userEntityPage = userRepo.findAll(spec, pageable);
        ///// map Entities to Dtos
        List<UserResponseDto> dtos = userMapper.mapToDtoList(userEntityPage.getContent());
        return PageResponse.from(userEntityPage, dtos);
    }

    /// / UPDATE USER
    @Caching(
            evict = {
                    @CacheEvict(value = "security-users", key = "#updateRequest.email")
            }
    )
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto updateRequest) {
        /// 1=> get old user from db // Base Service
        UserEntity existingUser = findById(id);

        /// 2=> check if user change old email with new email of another user
        if (!existingUser.getEmail().equals(updateRequest.getEmail())
                && userRepo.existsByEmail(updateRequest.getEmail())) {
            throw new DuplicateRecordException("User", "email", updateRequest.getEmail());
        }
        /// 3=> update data
        existingUser.setName(updateRequest.getName());
        existingUser.setEmail(updateRequest.getEmail());
        if (updateRequest.getLocation() != null && !updateRequest.getLocation().isBlank()) {
            existingUser.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getPhone() != null && !updateRequest.getPhone().isBlank()) {
            existingUser.setPhone(updateRequest.getPhone());
        }
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        return userMapper.mapToDto(userRepo.save(existingUser));
    }

    /// /////////////////
    /// SOFT DELETE METHOD
    @Caching(
            evict = {

                    @CacheEvict(value = "security-users", key = "#updateRequest.email")
            }
    )
    @Transactional
    public void softDeleteUser(Long id) {
        UserEntity user = userRepo.findByIdOrThrow(id);
        if (user.isDeleted()) {
            return;
        }
        /// change email string
        /// time stamp ti ensure that email is always unique even if user remove his email again
        String changedEmail = user.getEmail() + "_deleted_" + System.currentTimeMillis();
        user.setEmail(changedEmail);
        user.setDeleted(true);
        userRepo.save(user);
    }

    /// HARD DELETE METHODS
    @Caching(
            evict = {
                    @CacheEvict(value = "security-users", allEntries = true)
            }
    )
    @Transactional
    public void hardDeleteUser(Long id) {
        if (!userRepo.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepo.hardDeleteUser(id);
    }

    /// / RESTORE USER
    @Caching(
            evict = {
                    @CacheEvict(value = "security-users", allEntries = true)
            }
    )
    @Transactional
    public void restoreUser(Long id) {
        /// get user data
        UserEntity user = userRepo.findByIdOrThrow(id);
        /// ensure that email is already deleted
        if (!user.isDeleted()) {
            throw new BusinessLogicException("error.account.active");
        }
        /// clean deleted email
        String currentEmail = user.getEmail();
        int deletedIndex = currentEmail.lastIndexOf("_deleted_");
        if (deletedIndex != -1) {
            String originalEmail = currentEmail.substring(0, deletedIndex);

            /// ensure that original email not of another active user
            if (userRepo.existsByEmail(originalEmail)) {
                throw new BusinessLogicException("error.account.registered");
            }
            user.setEmail(originalEmail);
        }
        userRepo.restoreUser(id);
        userRepo.save(user);
    }
}
