package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = "ROLE_USER";
        if (user.getUserRoleId() != null && user.getUserRoleId() == 1) {
            role = "SUPER";
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }

    public boolean userExists(String username) {
        return userRepository.findByUsername(username.toLowerCase()).isPresent();
    }

    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.findByEmail(email.toLowerCase()).isPresent();
    }

    public void createUser(String username, String rawPassword, String email, int userRoleId) {
        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setUserRoleId(userRoleId);
        userRepository.save(user);
    }

    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public void deleteUserById(String id) {
        userRepository.deleteById(id);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
