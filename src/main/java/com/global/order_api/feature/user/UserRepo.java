package com.global.order_api.feature.user;

import com.global.order_api.core.base.BaseRepo;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends BaseRepo<UserEntity,Long>, JpaSpecificationExecutor<UserEntity> {
    /// Static queries needed for basic Business
    /// for login
    Optional<UserEntity> findByEmail(String email);
    /// for better performance
    boolean existsByEmail(String email);

    //// GET METHODS
    /// we comment findAll because => we extends JpaSpecificationExecutor
    //// which make findAll work as pagination function
//    @Override
//    Page<UserEntity> findAll(Specification<UserEntity> spec ,Pageable pageable);

    /// use jpql to work with spec
    @Query(value = "select u from UserEntity u where u.isDeleted=true")
    Page<UserEntity> findAllDeletedUsers(Specification<UserEntity> spec, Pageable pageable);

    //// HARD DELETE
    @Modifying
    @Query(value = "DELETE FROM users where id =:id",nativeQuery = true)
    void hardDeleteUser(@Param("id") Long id);

    //// RESTORE USER
    @Modifying
    @Query(value = "UPDATE users set is_deleted=false where id= :id",nativeQuery = true)
    void restoreUser(@Param("id") Long id);

}
