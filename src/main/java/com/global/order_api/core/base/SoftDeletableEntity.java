package com.global.order_api.core.base;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter // (Serialization)for jackson to convert object to json using get to get values to put it into json
@Setter // 
@MappedSuperclass // tell hibernate that this entity not a table in db 
// this only a parent class and children extends his fields and in their table in db
@NoArgsConstructor
// to enable soft delete
// tell hibernate any sql statement created for this class add this statment to select statements
// for select only => to get where is deleted =false
@SQLRestriction("is_deleted =false")
public abstract class SoftDeletableEntity<ID> extends BaseEntity<ID>{
	
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted=false; // default value
}
