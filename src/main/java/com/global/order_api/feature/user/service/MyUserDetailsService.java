package com.global.order_api.feature.user.service;

import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.entity.UserPrincipal;
import com.global.order_api.feature.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/// to impl loadUserByName to link with my DB
/// to Get User details using his name from my Custom table in DB
@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;


    @Override
    /// caching user for better performance
    //// no need to run extra query with each request it our system
    @Cacheable(value = "security-users", key = "#email")
    public UserDetails loadUserByUsername(String email) throws ResourceNotFoundException {
        /// talk to our repo to get user details using our unique field (email not name)
        UserEntity myUser = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.not.found", new Object[]{email}));

        /// convert UserEntity to UserDetails which Spring Security understands
        return new UserPrincipal(myUser);
    }
}
    
