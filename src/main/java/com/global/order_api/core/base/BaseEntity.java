package com.global.order_api.core.base;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

// auditing	
@Getter
@Setter
@MappedSuperclass // tell hibernate that this entity not a table in db 
// this only a parent class 
@EntityListeners(AuditingEntityListener.class) // tell SB if any data added into the table 
// add time , user automatic
// abstract to prevent any developer take an object by mistake
public abstract class BaseEntity<ID>{
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private ID id;
	
	@CreatedDate
	private LocalDateTime createdAt;
	
	@LastModifiedDate
	private LocalDateTime updatedAt;
	
	@CreatedBy
	private String createdBy;
	
	@LastModifiedBy
	private String updatedBy;
}

