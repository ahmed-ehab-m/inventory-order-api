package com.global.order_api.feature.user;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends BaseRepo<UserEntity,Long>, JpaSpecificationExecutor<UserEntity> {
    /// Static queries needed for basic Business
    /// for login
    Optional<UserEntity> findByEmail(String email);
    /// for better performance
    boolean existsByEmail(String email);
}
