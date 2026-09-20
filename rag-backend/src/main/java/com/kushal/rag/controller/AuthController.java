package com.kushal.rag.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kushal.rag.dto.LoginRequest;
import com.kushal.rag.dto.LoginResponse;
import com.kushal.rag.dto.RegisterRequest;
import com.kushal.rag.entity.User;
import com.kushal.rag.exception.RateLimitExceededException;
import com.kushal.rag.service.AuthService;
import com.kushal.rag.service.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class AuthController {

	private final AuthService authService;

	private final RateLimitService rateLimitService;

	public AuthController(AuthService authService, RateLimitService rateLimitService) {
	    this.authService = authService;
	    this.rateLimitService = rateLimitService;
	}
	@PostMapping("/api/auth/register")
	public String register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {

	    String clientIp = httpRequest.getRemoteAddr();

	    if (!rateLimitService.tryConsume(clientIp)) {
	        throw new RateLimitExceededException("Too many registration attempts. Please try again later.");
	    }

	    User user =
	            authService.register(
	                    request.getEmail(),
	                    request.getPassword()
	            );

	    return "User registered successfully with ID: " + user.getId();
	}

	@PostMapping("/api/auth/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {

		return authService.login(request.getEmail(), request.getPassword());
	}
}