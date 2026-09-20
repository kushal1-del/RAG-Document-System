import apiClient from "./client";

export async function addApiKey(provider, label, apiKey, expiresAt) {
  const response = await apiClient.post("/api/ai/keys", {
    provider,
    label,
    apiKey,
    expiresAt,
  });

  return response.data;
}

export async function listApiKeys() {
  const response = await apiClient.get("/api/ai/keys");
  return response.data;
}

export async function selectApiKey(apiKeyId) {
  const response = await apiClient.post(`/api/ai/keys/${apiKeyId}/select`);

  return response.data;
}

export async function disableApiKey(apiKeyId) {
  const response = await apiClient.delete(`/api/ai/keys/${apiKeyId}`);

  return response.data;
}
