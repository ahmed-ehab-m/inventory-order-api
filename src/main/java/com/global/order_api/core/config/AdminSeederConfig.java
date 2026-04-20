package com.global.order_api.core.config;

import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserRepo;
import com.global.order_api.feature.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Log4j2
@Order(1) /// because it work on any environment
public class AdminSeederConfig implements CommandLineRunner {

    private final UserRepo userRepo;
    private  final PasswordEncoder passwordEncoder;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private  String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        if(userRepo.findByEmail(adminEmail).isEmpty())
        {
            UserEntity admin=new UserEntity();
            admin.setName("Super Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            userRepo.save(admin);
            log.info("admin email created successfully");
        }
        else {
            log.info("admin email founded");
        }
    }
}
