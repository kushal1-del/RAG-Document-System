package com.kushal.rag.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kushal.rag.dto.LoginResponse;
import com.kushal.rag.entity.Role;
import com.kushal.rag.entity.User;
import com.kushal.rag.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(String email, String rawPassword) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "A user with this email already exists."
            );
        }

        String hashedPassword =
                passwordEncoder.encode(rawPassword);

        User user =
                new User(email, hashedPassword, Role.USER);

        return userRepository.save(user);
    }

    public LoginResponse login(String email, String rawPassword) {

        Optional<User> userOptional =
                userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOptional.get();

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("This account has been disabled.");
        }

        boolean passwordMatches =
                passwordEncoder.matches(rawPassword, user.getPassword());

        if (!passwordMatches) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole().name()
                );

        return new LoginResponse(token);
    }
}