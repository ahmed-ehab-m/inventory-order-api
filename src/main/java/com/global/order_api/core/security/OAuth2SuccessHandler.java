package com.global.order_api.core.security;

import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserPrincipal;
import com.global.order_api.feature.user.UserRepo;
import com.global.order_api.feature.user.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private  final UserRepo userRepo;
    private final JwtService jwtService;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws
            IOException, ServletException
    {
        /// extract user details from google/githup
        OAuth2User oAuth2User=(OAuth2User) authentication.getPrincipal();
        String email=oAuth2User.getAttribute("email");
        String rawName=oAuth2User.getAttribute("name");
        /// because GitHub may dont't send "name" send "login"
        final String name=(rawName !=null) ? rawName : oAuth2User.getAttribute("login");
        /// search the user in db or create a new user
        UserEntity user=userRepo.findByEmail(email)
                .orElseGet(()->
                {
                    UserEntity newUser=new UserEntity();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setRole(UserRole.USER); /// default
                    newUser.setPassword(""); /// no password
                    return userRepo.save(newUser);
                });
        /// GENERATE JWT TOKEN
        UserPrincipal principal=new UserPrincipal(user);
        String token=jwtService.generateToken(principal);
        /// CREATE HTTP ONLY COOKIE TO SEND JWT TOKEN TO FRONT-END
        Cookie cookie= new Cookie("jwt_token",token);
        cookie.setHttpOnly(true); /// prevent JS from reading it
        cookie.setSecure(false);
        cookie.setPath("/"); /// cookie will be sent with any request
        cookie.setMaxAge(10*60*60); /// 10 hours
        response.addCookie(cookie); /// add the cookie to our response
        /// redirect user to simple end point
        getRedirectStrategy().sendRedirect(request, response, "/api/v1/auth/success");
    }

}
