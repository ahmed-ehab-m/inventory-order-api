package com.global.order_api.feature.product;

import com.global.order_api.BaseRepoTest;
import com.global.order_api.feature.category.CategoryEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class ProductRepoTest extends BaseRepoTest {

    @Autowired
    private ProductRepo productRepo;

    /// give me clear() => to clear hibernate L1 cache
    /// separate the repo => because we make this code actually to test repo
    /// so we will not use repo , we will use entityManager to add initial data to H2
    ///  Flush our operations in H2 DB
    @Autowired
    private TestEntityManager entityManager;

    /// ////////////////////////////////////////////////////////////////
    /// //////////////////// HELPER METHODS ////////////////////////////
    /// ////////////////////////////////////////////////////////////////

    private CategoryEntity createAndSaveCategory(String name) {
        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        return entityManager.persistAndFlush(category);
    }

    private ProductEntity createAndSaveProduct(String name, double price, CategoryEntity category, boolean isDeleted) {
        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(price));
        product.setCategory(category);
        product.setDeleted(isDeleted);
        return entityManager.persistAndFlush(product);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////
    @Nested
    @DisplayName("1. Get Product Tests (GET)")
    class GetProductTests {

        /// ///////////// GET CATEGORY BY ID States/////////////
        /// // Find Product with category  by ID Exists - RETURN Product
        @Test
        void findProductByIdWithCategory_WhenIdExists_ShouldReturnProductAndCategory() {
            /// 1=> create category and save it
            CategoryEntity category = createAndSaveCategory("New Electronics");

            /// 2=> CREATE Product and add it into created Category
            ProductEntity product = createAndSaveProduct("Laptop", 15000.0, category, false);

            /// 3=> clear cache to force test to go to read from H2 DataBase
            entityManager.clear();

            /// 4=> test our function
            Optional<ProductEntity> result = productRepo.findByIdWithCategory(product.getId());

            /// 5=> assert
            /// check if function return the product
            assertThat(result).isPresent();
            /// check the name
            assertThat(result.get().getName()).isEqualTo("Laptop");

            /// check category returned or not
            assertThat(result.get().getCategory()).isNotNull();
            assertThat(result.get().getCategory().getName()).isEqualTo("New Electronics");
        }

        /// // Find Product with category  by ID doesn't Exist - Return not found
        @Test
        void findProductByIdWithCategory_WhenIdDoesNotExist_ShouldReturnNotFound() {

            /// 1=> clear cache to force test to go to read from H2 DataBase
            entityManager.clear();

            /// 2=> test our function
            Optional<ProductEntity> result = productRepo.findByIdWithCategory(10L);

            /// 3=> assert
            /// check if function return Empty
            assertThat(result).isEmpty();
        }

        /// // Find All Products with category  - RETURN ALL PRODUCTS
        /// if table name changed
        /// and to see n+1 problem was solved or not
        /// and check sql statement that join is right or not and sql statement overall
        @Test
        void findAll_ShouldReturnPageOfProductsWithTheirCategories() {
            /// 1=> create category and save it
            CategoryEntity category = createAndSaveCategory("Smartphones_Page_Test");

            /// 2=>create some products
            ProductEntity product1 = createAndSaveProduct("Apple iPhone", 40000.0, category, false);
            ProductEntity product2 = createAndSaveProduct("Samsung Galaxy", 35000.0, category, false);

            /// 3=> clear cache
            entityManager.clear();

            /// 4=> create pageable to pass it into out test function
            Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());

            /// 5=> test our function
            Page<ProductEntity> resultPage = productRepo.findAll(pageable);

            /// 6=> assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().isEmpty()).isFalse();
            assertThat(resultPage.getTotalElements()).isGreaterThanOrEqualTo(2);
            /// check the right products are returned successfully
            ProductEntity firstProduct = resultPage.getContent().get(0);
            assertThat(firstProduct.getName()).isEqualTo("Apple iPhone");
            /// check category
            assertThat(firstProduct.getCategory()).isNotNull();
            assertThat(firstProduct.getCategory().getName()).isEqualTo("Smartphones_Page_Test");
        }

        /// // Find All Products with category but DB is Empty - RETURN Empty Page
        @Test
        void findAll_ShouldReturnEmptyPage() {
            /// 1=> clear cache
            entityManager.clear();

            /// 2=> create pageable to pass it into out test function
            Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());

            /// 3=> test our function
            Page<ProductEntity> resultPage = productRepo.findAll(pageable);

            /// 4=> assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(0);
            assertThat(resultPage.getContent().isEmpty()).isTrue();

        }

        /// ///////////// FULL TEXT SEARCH TESTS (ACTIVE PRODUCTS) /////////////

        @Test
        @Disabled("Skipped due to MySQL InnoDB Full-Text commit constraints in test environment")
        void searchActiveByNameFullText_WhenMatchExists_ShouldReturnActiveProductsOnly() {
            CategoryEntity category = createAndSaveCategory("Smartphones");

            ProductEntity activeMatch = createAndSaveProduct("Apple iPhone 15", 40000.0, category, false);
            ProductEntity activeNoMatch = createAndSaveProduct("Samsung Galaxy", 35000.0, category, false);
            ProductEntity deletedMatch = createAndSaveProduct("Apple iPad", 30000.0, category, true);

            entityManager.clear();
            Pageable pageable = PageRequest.of(0, 10);

            Page<ProductEntity> resultPage = productRepo.searchActiveByNameFullText("app*", pageable);
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().size()).isEqualTo(1);
            assertThat(resultPage.getContent().get(0).getName()).isEqualTo("Apple iPhone 15");
        }

        @Test
        void searchActiveByNameFullText_WhenNoMatch_ShouldReturnEmptyPage() {
            CategoryEntity category = createAndSaveCategory("Smartphones");
            createAndSaveProduct("Samsung Galaxy", 35000.0, category, false);

            entityManager.clear();
            Pageable pageable = PageRequest.of(0, 10);

            Page<ProductEntity> resultPage = productRepo.searchActiveByNameFullText("nokia", pageable);

            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().isEmpty()).isTrue();
        }

        /// ///////////// FULL TEXT SEARCH TESTS (DELETED PRODUCTS) /////////////


        @Test
        @Disabled("Skipped due to MySQL InnoDB Full-Text commit constraints in test environment")
        void searchDeletedByNameFullText_WhenMatchExists_ShouldReturnDeletedProductsOnly() {
            CategoryEntity category = createAndSaveCategory("Smartphones");

            ProductEntity activeMatch = createAndSaveProduct("Apple iPhone 15", 40000.0, category, false);
            ProductEntity deletedMatch = createAndSaveProduct("Apple iPad", 30000.0, category, true);

            entityManager.clear();
            Pageable pageable = PageRequest.of(0, 10);

            Page<ProductEntity> resultPage = productRepo.searchActiveByNameFullText("app*", pageable);
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().size()).isEqualTo(1);
            assertThat(resultPage.getContent().get(0).getName()).isEqualTo("Apple iPad");
        }

        @Test
        void searchDeletedByNameFullText_WhenNoMatch_ShouldReturnEmptyPage() {
            entityManager.clear();
            Pageable pageable = PageRequest.of(0, 10);

            Page<ProductEntity> resultPage = productRepo.searchDeletedByNameFullText("app", pageable);

            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().isEmpty()).isTrue();
        }

        /// // Find Product by Category Id with category  by ID Exists - RETURN Products Page
        @Test
        void findProductByCategoryIdWithCategory_WhenIdExists_ShouldReturnProductsPage() {
            /// 1=> create category and save it
            CategoryEntity category = createAndSaveCategory("New Electronics");

            /// 2=> CREATE Product and add it into created Category
            ProductEntity product = createAndSaveProduct("Apple iPhone", 15000.0, category, false);
            ProductEntity product2 = createAndSaveProduct("galaxy", 15000.0, category, false);

            /// 3=> clear cache to force test to go to read from H2 DataBase
            entityManager.clear();
            /// 4=> create pageable to pass it into out test function
            Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());
            /// 5=> test our function
            Page<ProductEntity> resultPage = productRepo.findByCategoryId(category.getId(), pageable);

            /// 6=> assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().isEmpty()).isFalse();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            /// check the right products are returned successfully
            ProductEntity firstProduct = resultPage.getContent().get(0);
            assertThat(firstProduct.getName()).isEqualTo("Apple iPhone");
            /// check category
            assertThat(firstProduct.getCategory()).isNotNull();
            assertThat(firstProduct.getCategory().getName()).isEqualTo("New Electronics");
        }

        /// // Find Product by Category Id with category  by ID doesn't Exist - RETURN Empty Page
        @Test
        void findProductByCategoryIdWithCategory_WhenIdDoesNotExist_ShouldReturnEmptyPage() {

            /// 1=> tegory and save it
            CategoryEntity category = createAndSaveCategory("New Electronics");

            /// 2=> CREATE Product and add it into created Category
            ProductEntity product = createAndSaveProduct("Apple iPhone", 15000.0, category, false);

            /// 3=> clear cache to force test to go to read from H2 DataBase
            entityManager.clear();

            Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());
            /// 5=> test our function
            Page<ProductEntity> resultPage = productRepo.findByCategoryId(5L, pageable);

            /// 5=> assert
            /// check if function return Empty
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().isEmpty()).isTrue();
        }

        /// // Find All Soft Deleted Products - RETURN ALL PRODUCTSl
        @Test
        void findAllSoftDeletedProducts_ShouldReturnPageOfProducts() {
            /// 1=> create category and save it
            CategoryEntity category = createAndSaveCategory("Smartphones_Page_Test");

            /// 2=>create some products
            ProductEntity product1 = createAndSaveProduct("Apple iPhone", 40000.0, category, false);
            ProductEntity product2 = createAndSaveProduct("Samsung Galaxy", 35000.0, category, false);

            /// 3=> clear cache
            entityManager.clear();

            /// 4=> create pageable to pass it into out test function
            Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());

            /// 5=> test our function
            Page<ProductEntity> resultPage = productRepo.findAll(pageable);

            /// 6=> assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent().isEmpty()).isFalse();
            assertThat(resultPage.getTotalElements()).isGreaterThanOrEqualTo(2);
            /// check the right products are returned successfully
            ProductEntity firstProduct = resultPage.getContent().get(0);
            assertThat(firstProduct.getName()).isEqualTo("Apple iPhone");
            /// check category
            assertThat(firstProduct.getCategory()).isNotNull();
            assertThat(firstProduct.getCategory().getName()).isEqualTo("Smartphones_Page_Test");
        }

        /// // Find All Products with category but DB is Empty - RETURN Empty Page
        @Test
        void findAllSoftDeletedProducts_ShouldReturnEmptyPage() {
            /// 1=> clear cache
            entityManager.clear();

            /// 2=> create pageable to pass it into out test function
            Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());

            /// 3=> test our function
            Page<ProductEntity> resultPage = productRepo.findAll(pageable);

            /// 4=> assert
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(0);
            assertThat(resultPage.getContent().isEmpty()).isTrue();

        }

        /// // Find Product Image Url Even if deleted - RETURN URL from Cloudinary
        @Test
        void findProductImageUrlEvenIfDeleted_ShouldReturnUrl() {
            /// 1=> create category and save it
            CategoryEntity category = createAndSaveCategory("Smartphones_Page_Test");

            /// create some product
            ProductEntity product1 = createAndSaveProduct("Apple iPhone", 40000.0, category, false);
            product1.setImage("fake image url for testing");
            entityManager.persistAndFlush(product1);

            /// 2=> clear cache
            entityManager.clear();

            /// 3=> soft delete product
            productRepo.delete(product1);

            entityManager.clear();

            /// 4=> test our function
            Optional<String> imageUrl = productRepo.getImageUrlByIdEvenIfDeleted(product1.getId());
            /// 6=> assert
            /// contains => 1. check the value is present
            /// 2. check the value match the another value
            assertThat(imageUrl).contains("fake image url for testing");
        }

        /// // Find Product Image Url Even if deleted id not found - RETURN Empty object
        @Test
        void findProductImageUrlEvenIfDeleted_IdNotFound_ShouldReturnEmptyObject() {
            /// 1=> clear cache
            entityManager.clear();

            /// 2=> test our function
            Optional<String> imageUrl = productRepo.getImageUrlByIdEvenIfDeleted(10L);
            /// 3=> assert
            assertThat(imageUrl).isNotPresent();
        }

        /// ///////////// GET PRODUCT FOR UPDATE State /////////////
        /// // Find Product by ID with Pessimistic Lock - RETURN Product
        @Test
        void findByIdForUpdate_WhenIdExists_ShouldReturnProduct() {
            /// 1=> create category and save it
            CategoryEntity category = createAndSaveCategory("New Electronics");

            /// 2=> CREATE Product and add it into created Category
            ProductEntity product = createAndSaveProduct("Laptop", 15000.0, category, false);

            /// 3=> clear cache to force test to go to read from H2 DataBase
            entityManager.clear();

            /// 4=> test our function
            Optional<ProductEntity> result = productRepo.findByIdForUpdate(product.getId());

            /// 5=> assert
            /// check if function return the product
            assertThat(result).isPresent();
            /// check the name
            assertThat(result.get().getName()).isEqualTo("Laptop");

            assertThat(result.get().getStockCount()).isNotNull();
        }

        /// // Find Product by ID with Pessimistic Lock - RETURN Empty Optional when ID does not exist
        @Test
        void findByIdForUpdate_WhenIdDoesNotExist_ShouldReturnEmptyOptional() {
            /// 1=> define a non-existing ID
            Long nonExistingId = 999L;

            /// 2=> clear cache just to be safe (optional here since we didn't save anything)
            entityManager.clear();

            /// 3=> test our function
            Optional<ProductEntity> result = productRepo.findByIdForUpdate(nonExistingId);

            /// 4=> assert
            /// check if function returns empty optional instead of throwing unexpected exceptions
            assertThat(result).isEmpty();
            assertThat(result).isNotPresent();
        }

        /// ///////////// GET STOCK COUNT BY ID States /////////////
        /// // Find Stock Count - SUCCESS (ID Exists)
        @Test
        void findStockCountById_WhenIdExists_ShouldReturnStockCount() {
            /// 1=> create category and save it
            CategoryEntity category = createAndSaveCategory("Smartphones");

            /// 2=> CREATE Product with specific stock count and save it
            ProductEntity product = createAndSaveProduct("iPhone 15", 35000.0, category, false);
            product.setStockCount(50);
            /// hibernate doesn't send update direct to database
            /// he save it into his memory and take another queries to send all to db
            /// here we force it to update the database direct
            productRepo.saveAndFlush(product);

            /// 3=> clear cache to force test to go to read from H2 DataBase
            entityManager.clear();

            /// 4=> test our function
            Optional<Integer> result = productRepo.findStockCountById(product.getId());

            /// 5=> assert
            /// check if function returned a value
            assertThat(result).isPresent();
            /// check if the returned value matches the exactly saved stock count
            assertThat(result.get()).isEqualTo(50);
        }

        /// // Find Stock Count - FAIL (ID Does Not Exist)
        @Test
        void findStockCountById_WhenIdDoesNotExist_ShouldReturnEmptyOptional() {
            /// 1=> define a non-existing ID
            Long nonExistingId = 999L;

            /// 2=> clear cache just to be safe
            entityManager.clear();

            /// 3=> test our function
            Optional<Integer> result = productRepo.findStockCountById(nonExistingId);

            /// 4=> assert
            /// check if function returns empty optional safely without throwing SQL exceptions
            assertThat(result).isEmpty();
            assertThat(result).isNotPresent();
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Update Product fields Tests (PUT)")
    class UpdateProductTest {
        /// // move Products ToDefaultCategory - Products are moved
        @Test
        void moveProductsToDefaultCategory_ShouldUpdateCategoryIdto999() {
            /// 1=> create category
            CategoryEntity oldCategory = createAndSaveCategory("Category_To_Be_Deleted");

            ProductEntity product1 = createAndSaveProduct("Product A", 100.0, oldCategory, false);
            ProductEntity product2 = createAndSaveProduct("Product B", 200.0, oldCategory, false);

            entityManager.clear();

            productRepo.moveProductsToDefaultCategory(oldCategory.getId());

            /// because moveProductsToDefaultCategory() edit to Db directly
            /// so if we don't do this, hibernate will still remember products in deleted category
            entityManager.clear();
            Optional<ProductEntity> updatedProduct1 = productRepo.findById(product1.getId());
            Optional<ProductEntity> updatedProduct2 = productRepo.findById(product2.getId());

            assertThat(updatedProduct1).isPresent();
            assertThat(updatedProduct2).isPresent();

            assertThat(updatedProduct1.get().getCategory().getId()).isEqualTo(999L);
            assertThat(updatedProduct2.get().getCategory().getId()).isEqualTo(999L);

        }

        /// // move Products ToDefaultCategory but category doesn't exist - Products not moved
        @Test
        void moveProductsToDefaultCategory_WhenCategoryIdDoesNotExist_ShouldNotAffectOtherProducts() {
            // === Arrange ===
            CategoryEntity realCategory = createAndSaveCategory("Keep_Me_Category");

            ProductEntity product = createAndSaveProduct("Safe Product", 100.0, realCategory, false);

            entityManager.clear();

            /// invalid ID
            productRepo.moveProductsToDefaultCategory(8888L);
            entityManager.clear();

            /// check our products not been moved to 999
            Optional<ProductEntity> checkedProduct = productRepo.findById(product.getId());
            assertThat(checkedProduct.get().getCategory().getId()).isEqualTo(realCategory.getId());
        }

        /// // restore Product - Product restored again
        @Test
        void restoreProductsToDefaultCategory_ShouldSetIsDeletedTrue() {
            /// 1=> create category
            CategoryEntity oldCategory = createAndSaveCategory("Category_To_Be_Deleted");

            ProductEntity product1 = createAndSaveProduct("Product A", 100.0, oldCategory, true);

            entityManager.clear();
            productRepo.restoreProduct(product1.getId());
            entityManager.clear();
            Optional<ProductEntity> updatedProduct1 = productRepo.findById(product1.getId());


            assertThat(updatedProduct1).isPresent();
            assertThat(updatedProduct1.get().isDeleted()).isFalse();
        }

        /// / restore product - id not found - should not throw exception
        @Test
        void restoreProduct_WhenIdDoesNotExist_ShouldNotThrowException() {
            Long fakeId = 9999L;
            assertDoesNotThrow(() -> productRepo.restoreProduct(fakeId));
        }
    }

    /// ////////////////////////////////////////

    @Nested
    @DisplayName("3. Delete Product Tests (Delete)")
    class DeleteProductTest {
        /// hard delete product using id - deleted successfully
        @Test
        void hardDeleteProductById_ShouldDeleteProduct() {
            /// 1=> create category
            CategoryEntity oldCategory = createAndSaveCategory("Category_To_Be_Deleted");

            ProductEntity product1 = createAndSaveProduct("Product A", 100.0, oldCategory, false);

            entityManager.clear();
            productRepo.hardDeleteProduct(product1.getId());
            entityManager.clear();

            Optional<ProductEntity> deletedProduct = productRepo.findById(product1.getId());
            assertThat(deletedProduct).isEmpty();
        }

        /// hard delete product using id - Should Not Throw Exception
        @Test
        void hardDeleteProductById_WhenIdDoesNotExist_ShouldNotThrowException() {
            assertDoesNotThrow(() -> productRepo.hardDeleteProduct(9999L));
        }

        /// Soft delete product using id - Should update is_deleted
        @Test
        void softDeleteMechanism_ShouldHideDeletedProductsFromNormalQueries() {
            // === 1. Arrange ===
            CategoryEntity category = createAndSaveCategory("Category For Soft Delete");

            /// soft delete product
            ProductEntity product = createAndSaveProduct("Product to be hidden", 100.0, category, true);
            Long productId = product.getId();

            entityManager.clear();

            Optional<ProductEntity> result = productRepo.findById(productId);
            assertThat(result).isEmpty();

            /// check the product still in DB using Native Query
            /// object because java don't know the return type
            Object count = entityManager.getEntityManager()
                    .createNativeQuery("SELECT COUNT(*) FROM products WHERE id = :id")
                    .setParameter("id", productId)
                    .getSingleResult();

            /// Casting the returned object
            /// int value because returned value may be Long and 1 is Integer
            assertThat(((Number) count).intValue()).isEqualTo(1);
        }
    }
}