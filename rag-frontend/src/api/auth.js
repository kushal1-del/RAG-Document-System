import apiClient from "./client";

// Calls POST /api/auth/login on your Spring Boot backend.
// Returns the response data, which looks like: { token: "..." }
export async function login(email, password) {
  const response = await apiClient.post("/api/auth/login", {
    email,
    password,
  });

  return response.data;
}

// Calls POST /api/auth/register on your backend.
export async function register(email, password) {
  const response = await apiClient.post("/api/auth/register", {
    email,
    password,
  });

  return response.data;
}
