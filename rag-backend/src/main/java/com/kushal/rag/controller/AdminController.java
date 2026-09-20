package com.kushal.rag.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kushal.rag.dto.AdminCreateUserRequest;
import com.kushal.rag.dto.DocumentSummaryResponse;
import com.kushal.rag.dto.PagedResponse;
import com.kushal.rag.dto.UserSummaryResponse;
import com.kushal.rag.service.AdminService;

@RestController
public class AdminController {

	private final AdminService adminService;
	
	@Value("${pagination.default-page-size}")
	private int defaultPageSize;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/api/admin/users")
	public PagedResponse<UserSummaryResponse> listAllUsers(
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(required = false) Integer size) {

	    int effectiveSize = (size != null) ? size : defaultPageSize;

	    return adminService.listAllUsers(page, effectiveSize);
	}

	@GetMapping("/api/admin/documents")
	public PagedResponse<DocumentSummaryResponse> listAllDocuments(
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(required = false) Integer size) {

	    int effectiveSize = (size != null) ? size : defaultPageSize;

	    return adminService.listAllDocuments(page, effectiveSize);
	}
	@PostMapping("/api/admin/createUser")
	public UserSummaryResponse createUser(@RequestBody AdminCreateUserRequest request) {

		return adminService.createUser(request.getEmail(), request.getPassword(), request.getRole());
	}

	@PatchMapping("/api/admin/users/{id}/disable")
	public UserSummaryResponse disableUser(@PathVariable String id) {
		return adminService.disableUser(id);
	}

	@DeleteMapping("/api/admin/users/{id}")
	public String hardDeleteUser(@PathVariable String id) {
		adminService.hardDeleteUser(id);
		return "User and all associated data deleted successfully.";
	}
	@PatchMapping("/api/admin/users/{id}/enable")
	public UserSummaryResponse enableUser(@PathVariable String id) {
	    return adminService.enableUser(id);
	}
	
}