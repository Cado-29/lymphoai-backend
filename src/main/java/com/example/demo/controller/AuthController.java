package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.demo.config.JwtUtil;
import com.example.demo.entity.AuthRequest;
import com.example.demo.entity.User;
import com.example.demo.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // Authenticate username/password
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // Load user details
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // Generate JWT token
            final String token = jwtUtil.generateToken(userDetails.getUsername());

            // Fetch additional info (assuming your UserDetailsService can give it)
            User userEntity = userDetailsService.getUserEntityByUsername(request.getUsername());
            String userId = userEntity.getId();           // the user's ID
            Integer roleId = userEntity.getUserRoleId();       // the role ID (adapt if your role is an object)

            return ResponseEntity.ok(Map.of(
                "username", request.getUsername(),
                "token", token,
                "userId", userId,
                "roleId", roleId,
                "email", userEntity.getEmail() == null ? "" : userEntity.getEmail()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(HttpServletRequest httpRequest) {
        User currentUser = authorizeSuperUser(httpRequest);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have Permission");
        }

        List<Map<String, Object>> users = userDetailsService.getAllUsers().stream()
                .map(this::toUserPayload)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable String id,
            @RequestBody AuthRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = authorizeSuperUser(httpRequest);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have Permission");
        }

        User existing = userDetailsService.getUserById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        String newUsername = request.getUsername() == null ? existing.getUsername() : request.getUsername().trim().toLowerCase();
        String newEmail = request.getEmail() == null ? existing.getEmail() : request.getEmail().trim().toLowerCase();
        Integer newRoleId = request.getRoleId() == null ? existing.getUserRoleId() : request.getRoleId();

        if (newUsername.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username cannot be empty");
        }

        if (newEmail == null || newEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email cannot be empty");
        }

        for (User user : userDetailsService.getAllUsers()) {
            if (!user.getId().equals(existing.getId())) {
                if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(newUsername)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
                }
                if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(newEmail)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
                }
            }
        }

        existing.setUsername(newUsername);
        existing.setEmail(newEmail);
        existing.setUserRoleId(newRoleId);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(userDetailsService.getPasswordEncoder().encode(request.getPassword()));
        }

        User saved = userDetailsService.saveUser(existing);
        return ResponseEntity.ok(toUserPayload(saved));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, HttpServletRequest httpRequest) {
        User currentUser = authorizeSuperUser(httpRequest);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have Permission");
        }

        if (currentUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You cannot delete your own account");
        }

        User existing = userDetailsService.getUserById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        userDetailsService.deleteUserById(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        User currentUser = authorizeSuperUser(httpRequest);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have Permission");
        }

        if (request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username and password are required");
        }

        // Check if user already exists
        if (userDetailsService.userExists(request.getUsername())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Username already exists");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && userDetailsService.emailExists(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Email already exists");
        }

        int roleId = request.getRoleId() == null ? 2 : request.getRoleId();
        userDetailsService.createUser(request.getUsername(), request.getPassword(), request.getEmail(), roleId);

        return ResponseEntity.ok("User registered successfully");
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        User currentUser = authorizeUser(httpRequest);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String newUsername = request.getUsername() == null ? "" : request.getUsername().trim().toLowerCase();
        String newEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();

        if (newUsername.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username cannot be empty");
        }

        if (newEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email cannot be empty");
        }

        for (User user : userDetailsService.getAllUsers()) {
            if (!user.getId().equals(currentUser.getId())) {
                if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(newUsername)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
                }
                if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(newEmail)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
                }
            }
        }

        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        User saved = userDetailsService.saveUser(currentUser);
        return ResponseEntity.ok(toUserPayload(saved));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> payload, HttpServletRequest httpRequest) {
        User currentUser = authorizeUser(httpRequest);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String currentPassword = payload.getOrDefault("currentPassword", "").trim();
        String newPassword = payload.getOrDefault("newPassword", "").trim();

        if (currentPassword.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current password and new password are required");
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("New password must be at least 8 characters");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(currentUser.getUsername(), currentPassword));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current password is incorrect");
        }

        currentUser.setPassword(userDetailsService.getPasswordEncoder().encode(newPassword));
        userDetailsService.saveUser(currentUser);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    private User authorizeUser(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);

        String currentUsername;
        try {
            currentUsername = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            return null;
        }

        return userDetailsService.getUserEntityByUsername(currentUsername);
    }

    private User authorizeSuperUser(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);

        String currentUsername;
        try {
            currentUsername = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            return null;
        }

        UserDetails currentUser = userDetailsService.loadUserByUsername(currentUsername);
        boolean isSuperUser = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("SUPER"));

        if (!isSuperUser) {
            return null;
        }

        return userDetailsService.getUserEntityByUsername(currentUsername);
    }

    private Map<String, Object> toUserPayload(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername() == null ? "" : user.getUsername(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "roleId", user.getUserRoleId() == null ? 2 : user.getUserRoleId()
        );
    }
}
