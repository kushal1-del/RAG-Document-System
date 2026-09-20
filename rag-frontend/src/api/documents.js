import apiClient from "./client";

// GET /api/documents — list the current user's own documents
export async function listDocuments() {
  const response = await apiClient.get("/api/documents");
  return response.data;
}

// POST /api/documents/upload — upload a new PDF
export async function uploadDocument(file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await apiClient.post("/api/documents/upload", formData);
  return response.data;
}

// DELETE /api/documents/{id} — delete one of the current user's own documents
export async function deleteDocument(documentId) {
  const response = await apiClient.delete(`/api/documents/${documentId}`);
  return response.data;
}
