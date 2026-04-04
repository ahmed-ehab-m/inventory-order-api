package com.global.order_api.feature.category;

import org.hibernate.annotations.SQLDelete;

import com.global.order_api.core.base.SoftDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "categories")
@Getter // (Serialization)for jackson to convert object to json using get to get values to put it into json
// and mapstruct use it get , set for map , unmap
@NoArgsConstructor // jpa need it to create entity from db+ get ,set to get values or edit fields
// Desrialization jackson use it to create empty obj first 
@Setter // then use setter to put values into obj 
// jackson may use allargs constructor but need more settings
@AllArgsConstructor // for builder
@Builder
// to enable softdelete
// tell hibernate that => any delete method called don
@SQLDelete(sql="UPDATE categories SET is_deleted=true WHERE ID=?")
public class CategoryEntity extends SoftDeletableEntity<Long>{
	
	@Column(name = "name",nullable = false ,unique = true, length = 255)
	private String name;		
    @Column(name = "description", columnDefinition = "TEXT")
	private String description;
}
