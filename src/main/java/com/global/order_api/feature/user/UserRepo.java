package com.global.order_api.feature.user;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends BaseRepo<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    // ==================================================================================
    //                                1. READ METHODS
    // ==================================================================================

    /// Static queries needed for basic Business
    /// for login
    /// Indexing
    /// email unique => non cluster index => very fast
    Optional<UserEntity> findByEmail(String email);

    /// for better performance
    /// /// Indexing
    ///     /// email unique => non cluster index => very fast
    ///     Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);


    /// GET USER INCLUDING DELETED (For Restore Logic)
    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<UserEntity> findByIdIncludingDeleted(@Param("id") Long id);

    //// GET METHODS
    /// we comment findAll because => we extends JpaSpecificationExecutor
    //// which make findAll work as pagination function
//    @Override
//    Page<UserEntity> findAll(Specification<UserEntity> spec ,Pageable pageable);


    // ==================================================================================
    //                                2. DELETE & RESTORE METHODS
    // ==================================================================================

    /// / HARD DELETE
    /// clear hibernate cache L1
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM users where id =:id", nativeQuery = true)
    void hardDeleteUser(@Param("id") Long id);

    /// / RESTORE USER
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE users set is_deleted=false where id= :id", nativeQuery = true)
    void restoreUser(@Param("id") Long id);

}