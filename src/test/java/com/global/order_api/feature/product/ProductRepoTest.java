package com.global.order_api.feature.product;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.global.order_api.feature.category.CategoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

/// to make spring run dataBase parts only (entities and repos) => make test is very fast
/// built in => @Transactional to make DB is clean after each test
/// Auto Configuration to H2 not mySQL
@DataJpaTest(showSql = false)
class ProductRepoTest {

    @Autowired
    private ProductRepo productRepo;

    /// give me clear() => to clear hibernate L1 cache
    /// separate the repo => because we make this code actually to test repo
    /// so we will not use repo , we will use entityManager to add initial data to H2
    ///  Flush our operations in H2 DB
    @Autowired
    private TestEntityManager entityManager;

    ///////////////////////////////////////////////////////////////////
    ////////////////////////////////////READING METHODS////////////////////////////////////
    @Nested
    @DisplayName("1. Get Category Tests (GET)")
    class GetCategoryTests
    {
        //////////////// GET CATEGORY BY ID States/////////////
        ///// Find Product with category  by ID Exists - RETURN Product
        @Test
        void findProductWithCategoryById_WhenIdExists_ShouldReturnProductAndCategory()
        {
            /// 1=> create category and save it
            CategoryEntity category=new CategoryEntity();
            category.setName("New Electronics");
            /// call entity manager to save it into H2
            category=entityManager.persistAndFlush(category);

            /// 2=> CREATE Product and add it into created Category
            ProductEntity product=new ProductEntity();
            product.setName("Laptop");
            product.setPrice(BigDecimal.valueOf(15000.0));
            product.setCategory(category);
            /// call entity manager to save it into H2
            product=entityManager.persistAndFlush(product);

            /// 3=> clear cache to force test to go to read from H2 DataBase
            entityManager.clear();

            /// 4=> test our function
            Optional<ProductEntity> result=productRepo.findByIdWithCategory(product.getId());

            /// 5=> assert
            /// check if function return the product
            assertThat(result).isPresent();
            /// check the name
            assertThat(result.get().getName()).isEqualTo("Laptop");

            /// check category returned or not
            assertThat(result.get().getCategory()).isNotNull();
            assertThat(result.get().getCategory().getName()).isEqualTo("New Electronics");
        }
    }


}