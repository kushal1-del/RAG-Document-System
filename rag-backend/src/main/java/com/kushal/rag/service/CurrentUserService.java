package com.kushal.rag.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.kushal.rag.entity.User;
import com.kushal.rag.repository.UserRepository;

/**
 * Small helper used anywhere in the app to answer:
 * "who is making this request right now?"
 *
 * Our JwtAuthFilter stores the user's EMAIL as the authentication
 * "principal" (see JwtAuthFilter -> UsernamePasswordAuthenticationToken).
 * This class takes that email and looks up the full User row,
 * so callers can get the user's actual ID (needed for ownership),
 * not just their email.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the full User entity for whoever is authenticated
     * on the current request. Throws if somehow called with no
     * authenticated user (shouldn't happen on a protected endpoint,
     * since JwtAuthFilter + SecurityConfig already enforce that).
     */
    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Authenticated user not found in database: " + email)
                );
    }
}