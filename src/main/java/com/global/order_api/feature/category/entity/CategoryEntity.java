package com.global.order_api.feature.category.entity;

import com.global.order_api.core.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

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
// tell hibernate that => any delete method called
//@SQLDelete(sql="UPDATE categories SET is_deleted=true WHERE ID=?")
public class CategoryEntity extends BaseEntity<Long> {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
