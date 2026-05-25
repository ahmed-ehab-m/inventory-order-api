package com.global.order_api.feature.product;

import java.math.BigDecimal;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.Id;

import com.global.order_api.core.base.SoftDeletableEntity;
import com.global.order_api.feature.category.CategoryEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter // (Serialization)for jackson to convert object to json using get to get values to put it into json
// and mapstruct use it get , set for map , unmap
@NoArgsConstructor // jpa need it to create entity from db+ get ,set to get values or edit fields
// Desrialization jackson use it to create empty obj first 
@Setter // then use setter to put values into obj 
// jackson may use allargs constructor but need more settings
@AllArgsConstructor // for builder
@Builder
// to enable softdelete
// tell hibernate that => any delete method called 
@SQLDelete(sql="UPDATE products SET is_deleted=true WHERE ID=?")
public class ProductEntity extends SoftDeletableEntity<Long> {

    /// every update hibernate increase version field +1
    /// add WHERE VERSION = OLD VALUE  to any sql statement
    /// if db return update failure because version has been changed
    /// then return OptimisticLockingFailureException  to refresh page in front-end
    @Version
    private Long version;

	@Column(name = "name",nullable = false , length = 255)
	private String name;
	
    @Column(name = "description", columnDefinition = "TEXT")
	private String description;
    
	@Column(name="price",nullable = false, precision = 10, scale = 2)
	private BigDecimal price;
	
    @Column(name = "image", columnDefinition = "TEXT")
	private String image;
    
    @Column(name = "stock_count", nullable = false)
	private int stockCount;
    
    // for better performance even if using DTOS 
    // first => because service talks to repo to get products
    // hibernate if fetch type eager => will get full category entity too
    // because this process occurs before map to DTO
    // second => even if i use LAZY  => N+1 Problem
    // hibernate will get all products using one sql statment ok
    // then map struct want to mapping to DTO
    // in DTO we using category name
    // Sooo hibernate will create like 100 (n of products) sql statement to get category name
    // solution is => entity must be lazy 
    // and in repo layer => create SQL to get products and categories in one SQL Statement
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "category_id",nullable = false)
    private CategoryEntity category;
    
}
