package com.kushal.rag.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kushal.rag.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs once per incoming HTTP request, BEFORE it reaches any controller.
 *
 * Its job:
 *   1. Look for "Authorization: Bearer <token>" header.
 *   2. If found and valid -> tell Spring Security "this request is
 *      authenticated as this email, with this role."
 *   3. If missing/invalid -> do nothing here; let Spring Security's
 *      normal rules (from SecurityConfig) decide whether to allow
 *      or reject the request (e.g. /api/auth/** stays open regardless).
 *
 * "OncePerRequestFilter" guarantees this logic runs exactly once
 * per request, even in edge cases with internal request forwarding.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token at all, or not in "Bearer <token>" format -> skip.
        // We don't reject here; SecurityConfig's rules will handle
        // whether this specific endpoint requires authentication.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // Strip the "Bearer " prefix (7 characters) to get the raw token.
        String token = authHeader.substring(7);

        if (jwtService.isTokenValid(token)) {

            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            // Spring Security expects roles prefixed with "ROLE_"
            // when using hasRole(...) checks later (e.g. hasRole("ADMIN")).
            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + role);

            // This object tells Spring Security: "treat this request
            // as coming from this authenticated user, with this authority."
            // Credentials (password) are null here — we've already
            // proven identity via the valid JWT signature, we don't
            // need the password again.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(authority)
                    );

            // Register this authentication for the current request's context.
            // Everything downstream (controllers, @PreAuthorize checks, etc.)
            // can now see "who is making this request."
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Continue to the next filter / the actual controller,
        // whether or not authentication was set above.
        filterChain.doFilter(request, response);
    }
}