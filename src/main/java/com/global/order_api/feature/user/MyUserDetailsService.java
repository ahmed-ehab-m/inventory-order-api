package com.global.order_api.feature.user;

import com.global.order_api.core.utils.AppTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/// to impl loadUserByName to link with my DB
/// to Get User details using his name from my Custom table in DB
@Service
@RequiredArgsConstructor
public class MyUserDetailsService  implements UserDetailsService {
    private final UserRepo userRepo;
    private final AppTranslator appTranslator;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        /// talk to our repo to get user details using our unique field (email not name)
        String message=appTranslator.translateMessage("error.user.not.found",email);
        UserEntity myUser=userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException(message));

        /// convert UserEntity to UserDetails which Spring Security understands
        return new UserPrincipal(myUser);
    }
}
