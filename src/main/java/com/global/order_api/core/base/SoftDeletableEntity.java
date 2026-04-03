package com.global.order_api.core.base;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass // tell hibernate that this entity not a table in db 
// this only a parent class 
public abstract class SoftDeletableEntity<ID> extends BaseEntity<ID>{
	
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted=false; // default value
}
