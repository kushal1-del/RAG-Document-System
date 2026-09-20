import apiClient from "./client";

export async function listUsers(page = 0, size = 20) {
  const response = await apiClient.get("/api/admin/users", {
    params: { page, size },
  });
  return response.data;
}

export async function listAllDocuments(page = 0, size = 20) {
  const response = await apiClient.get("/api/admin/documents", {
    params: { page, size },
  });
  return response.data;
}

export async function createUser(email, password, role) {
  const response = await apiClient.post("/api/admin/createUser", {
    email,
    password,
    role,
  });
  return response.data;
}

export async function disableUser(userId) {
  const response = await apiClient.patch(`/api/admin/users/${userId}/disable`);
  return response.data;
}

export async function enableUser(userId) {
  const response = await apiClient.patch(`/api/admin/users/${userId}/enable`);
  return response.data;
}

export async function hardDeleteUser(userId) {
  const response = await apiClient.delete(`/api/admin/users/${userId}`);
  return response.data;
}
