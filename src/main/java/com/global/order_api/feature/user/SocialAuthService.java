package com.global.order_api.feature.user;

import com.global.order_api.core.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

/// FOR MOBILE AUTH
@Service
@RequiredArgsConstructor
@Log4j2
public class SocialAuthService {
    private final UserRepo userRepo;
    private final JwtService jwtService;
    private  final RestTemplate restTemplate;

    @Value("{spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /// MOBILE GOOGLE SIGN IN
    public String loginWithGoogle(String googleToken)
    {
        try {
            /// 1=> setup checking tool from google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),new GsonFactory()
            ).setAudience(Collections.singletonList(googleClientId))
                    .build();
            /// 2=> check the token
        GoogleIdToken idToken=verifier.verify(googleToken);
        /// 3=> if token is right
        if(idToken !=null)
        {
           /// 4 => get the data
            GoogleIdToken.Payload payload=idToken.getPayload();
            String email= payload.getEmail();
            String name=(String) payload.get("name");

            /// 5=> get user from our db or create a new user
            UserEntity user = userRepo.findByEmail(email)
                    .orElseGet(() -> {
                        UserEntity newUser = new UserEntity();
                        newUser.setEmail(email);
                        newUser.setName(name != null ? name : "Google User");
                        newUser.setRole(UserRole.USER);
                        newUser.setPassword("");
                        return userRepo.save(newUser);
                    });
            /// 6=> generate jwt token
            UserPrincipal userPrincipal=new UserPrincipal(user);
            return  jwtService.generateToken(userPrincipal);
        }
        else {
            throw new BadCredentialsException("error.token.invalid");
        }
        }

    catch (Exception e) {
            log.error(e.getMessage());
            throw new BadCredentialsException("error.token.invalid");
    }
    }

    /// GITHUB LOGIN FOR MOBILE
    public String loginWithGitHub(String githubAccessToken) {
        try {
            /// 1=> put the token in header
            HttpHeaders headers=new HttpHeaders();
            headers.setBearerAuth(githubAccessToken);
            HttpEntity<String> entity=new HttpEntity<>(headers);
            /// 2=> talk to github to get user data
            ResponseEntity<Map> response=restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String,Object> body=response.getBody();
                String login = (String) body.get("login");
                String rawEmail = (String) body.get("email");
                /// if email is hidden or private
                if (rawEmail == null) rawEmail = login + "@github.com";
                String email=rawEmail;

                /// 3=> generate Jwt token
                UserEntity user = userRepo.findByEmail(email)
                        .orElseGet(() -> {
                            UserEntity newUser = new UserEntity();
                            newUser.setEmail(email);
                            newUser.setName(login);
                            newUser.setRole(UserRole.USER);
                            newUser.setPassword("");
                            return userRepo.save(newUser);
                        });

                UserPrincipal principal = new UserPrincipal(user);
                return jwtService.generateToken(principal);
            }
            throw new BadCredentialsException("error.token.invalid");
        } catch (Exception e) {
            throw new BadCredentialsException("error.token.invalid");

            }

    }
}
