package com.global.order_api.feature.user;

import com.global.order_api.core.base.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET is_deleted=true WHERE id=?")
@SQLRestriction("is_deleted=false") // when use find all excluding deleted users
public class UserEntity extends SoftDeletableEntity<Long> {

    @Column(name = "name",nullable = false,length = 255)
    private String name;

    @Column(name = "email",nullable = false,length = 255,unique = true)
    private String email;

    @Column(name = "password",nullable = false,length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role",nullable = false,length = 50)
    private UserRole role;

    @Column(name = "location",nullable = true,columnDefinition = "TEXT")
    private String location;

    @Column(name = "phone",nullable = true,length = 50)
    private String phone;
}
