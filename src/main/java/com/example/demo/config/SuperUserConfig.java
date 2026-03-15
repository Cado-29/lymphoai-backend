package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SuperUserConfig {

    @Bean
    CommandLineRunner createSuperUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            boolean exists = userRepository.findByUsername("superuser").isPresent();
            if (!exists) {
                User superUser = new User();
                superUser.setUsername("superuser");
                superUser.setPassword(passwordEncoder.encode("superpassword"));
                superUser.setUserRoleId(1);
                userRepository.save(superUser);
            } else {
                System.out.println("Superuser already exists");
            }
        };
    }
}
