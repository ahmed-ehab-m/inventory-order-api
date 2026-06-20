package com.global.order_api.feature.user;

import com.global.order_api.BaseRepoTest;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.enums.UserRole;
import com.global.order_api.feature.user.repo.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UserRepoTest extends BaseRepoTest {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private TestEntityManager entityManager;

    /// ////////////////////////////////////////////////////////////////
    /// //////////////////// HELPER METHODS ////////////////////////////
    /// ////////////////////////////////////////////////////////////////

    private UserEntity createAndSaveUser(String email, boolean isDeleted) {
        UserEntity user = new UserEntity();
        user.setName("Test User");
        user.setEmail(email != null ? email : "test_" + UUID.randomUUID().toString() + "@gmail.com");
        user.setPassword("hashedPassword123");
        user.setRole(UserRole.USER);
        user.setDeleted(isDeleted); // Assuming you have setDeleted() from BaseEntity
        return entityManager.persistAndFlush(user);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// READING METHODS ////////////////////////////////////
    /// test Derived Queries because=>
    /// soft delete
    /// Refactoring Safety if any field name changed
    /// check mapping
    /// Future changes
    @Nested
    @DisplayName("1. Get User Tests (GET)")
    class GetUserTests {

        /// // Find By Email - Exists (Return User)
        @Test
        void findByEmail_WhenEmailExists_ShouldReturnUser() {
            // 1=> Create fake User
            String targetEmail = "test@company.com";
            UserEntity user = createAndSaveUser(targetEmail, false);

            // 2=> Clear Cache
            entityManager.clear();

            // 3=> Test function
            Optional<UserEntity> result = userRepo.findByEmail(targetEmail);

            // 4=> Assert
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(targetEmail);
            assertThat(result.get().getName()).isEqualTo(user.getName());
        }

        /// // Find By Email - Not Exists (Return Empty)
        @Test
        void findByEmail_WhenEmailDoesNotExist_ShouldReturnEmpty() {
            entityManager.clear();
            Optional<UserEntity> result = userRepo.findByEmail("notfound@company.com");
            assertThat(result).isEmpty();
        }

        /// // Exists By Email - Email Exists (Return True)
        @Test
        void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
            String targetEmail = "exists@company.com";
            createAndSaveUser(targetEmail, false);

            entityManager.clear();

            boolean exists = userRepo.existsByEmail(targetEmail);

            assertThat(exists).isTrue();
        }

        /// // Exists By Email - Email Not Exists (Return False)
        @Test
        void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
            entityManager.clear();
            boolean exists = userRepo.existsByEmail("nobody@company.com");
            assertThat(exists).isFalse();
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// UPDATE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("2. Update & Restore User Tests (PUT)")
    class UpdateUserTests {

        /// // Restore User - Should set is_deleted to false
        @Test
        void restoreUser_ShouldSetIsDeletedFalse() {
            // Create soft-deleted user (isDeleted = true)
            UserEntity user = createAndSaveUser("deleted@company.com", true);

            entityManager.clear();

            // Act: Restore the user
            userRepo.restoreUser(user.getId());

            // Clear cache to force hibernate to fetch fresh data from DB after @Modifying
            entityManager.clear();

            // Verify using Native Query to bypass any Hibernate filters
            Object isDeleted = entityManager.getEntityManager()
                    .createNativeQuery("SELECT is_deleted FROM users WHERE id = :id")
                    .setParameter("id", user.getId())
                    .getSingleResult();

            assertThat(((Boolean) isDeleted)).isFalse();
        }

        /// // Restore User - When Id Does Not Exist (Should Not Throw)
        @Test
        void restoreUser_WhenIdDoesNotExist_ShouldNotThrowException() {
            assertDoesNotThrow(() -> userRepo.restoreUser(999L));
        }

        /// // Find By Id Including Deleted - When User Is Soft Deleted
        @Test
        void findByIdIncludingDeleted_WhenUserIsDeleted_ShouldReturnUser() {
            // 1. Arrange: Create a deleted user
            UserEntity user = createAndSaveUser("hidden@company.com", true);
            entityManager.clear();

            // 2. Act
            Optional<UserEntity> result = userRepo.findByIdIncludingDeleted(user.getId());

            // 3. Assert: Should bypass Hibernate filter and find it
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("hidden@company.com");
            assertThat(result.get().isDeleted()).isTrue();
        }

        /// // Find By Id Including Deleted - When Id Does Not Exist
        @Test
        void findByIdIncludingDeleted_WhenUserDoesNotExist_ShouldReturnEmpty() {
            Optional<UserEntity> result = userRepo.findByIdIncludingDeleted(999L);
            assertThat(result).isEmpty();
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// DELETE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("3. Delete User Tests (DELETE)")
    class DeleteUserTests {

        /// // Hard Delete User
        @Test
        void hardDeleteUser_ShouldDeleteUserCompletely() {
            UserEntity user = createAndSaveUser("todelete@company.com", false);

            entityManager.clear();

            // Act
            userRepo.hardDeleteUser(user.getId());
            entityManager.clear();

            // Verify user is completely removed from DB using findById
            Optional<UserEntity> deletedUser = userRepo.findById(user.getId());

            assertThat(deletedUser).isEmpty();
        }

        /// // Hard Delete User - When Id Does Not Exist (Should Not Throw)
        @Test
        void hardDeleteUser_WhenIdDoesNotExist_ShouldNotThrowException() {
            assertDoesNotThrow(() -> userRepo.hardDeleteUser(999L));
        }
    }
}