import apiClient from "./client";

export async function askQuestion(question, documentId) {
  const response = await apiClient.post("/api/ask", {
    question,
    documentId: documentId || null,
  });
  return response.data;
}

// POST /api/search — returns raw chunks with scores, used to show
// "sources" behind an answer when the toggle is enabled.
export async function searchChunks(query, documentId) {
  const response = await apiClient.post("/api/search", {
    query,
    documentId: documentId || null,
  });
  return response.data;
}
