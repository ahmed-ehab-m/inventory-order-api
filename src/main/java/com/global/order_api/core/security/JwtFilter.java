package com.global.order_api.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// every request this filter executes only once

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            final String authHeader=request.getHeader("Authorization");
             String jwtToken=null;
             String userEmail=null;

             /// reading from header (for Mobile , PostMan)
             if(authHeader !=null && authHeader.startsWith("Bearer"))
             {
                 /// get token
                 jwtToken=authHeader.substring(7);
             }

             /// reading from cookies (for web)
            if(jwtToken==null && request.getCookies() !=null)
            {
                for(Cookie cookie : request.getCookies())
                {
                    if("jwt_token".equals(cookie.getName()))
                    {
                        jwtToken=cookie.getValue();
                        break;
                    }
                }
            }
            /// user login first time
            if(jwtToken ==null)
            {
                //// Request go to next filter but not authenticated
                //// if request go to login or register endpoint
                //// will go direct to controller
                //// else 401 Unauthorized
                filterChain.doFilter(request, response);
                return;
            }

            userEmail= jwtService.extractUserEmail(jwtToken);
            /// check the token is valid
            /// SecurityContextHolder => temp memory of spring
            /// check this user not authenticated
            /// SecurityContextHolder.getContext().getAuthentication()==null =>
            /// we check that user not Authenticated (logged in ) now
            /// 1=> performance optimization => because we don't need to go to DB every time
            /// 2=> Redundancy check => we sure  make validation process and create
            /// authentication object only one in context holder
            if(userEmail !=null && SecurityContextHolder.getContext().getAuthentication()==null)
            {
                /// get user details from db
                UserDetails userDetails=userDetailsService.loadUserByUsername(userEmail);
                /// validate the token
                if(jwtService.validateToken(jwtToken,userDetails))
                {
                    UsernamePasswordAuthenticationToken authToken=
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    /// null => the password because we already validate the token
                                    /// and no need to store it in memory for more security
                                    null,
                                    userDetails.getAuthorities()
                            );
                    /// set additional details about this request
                    /// WebAuthenticationDetailsSource => take the request and extract
                    /// 1=> Ip address
                    /// 2=> session id
                    /// buildDetails() => take this two info and build the object
                    /// setDetails() => then take the object and attach it to auth token object
                    /// for auditing and logging and prevent any hacker steals user token
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        /// here filter done his work to go to another filters
        filterChain.doFilter(request, response);
    }
}
