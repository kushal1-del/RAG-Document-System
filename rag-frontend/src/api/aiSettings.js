import apiClient from "./client";

export async function getAiSettings() {
  const response = await apiClient.get("/api/ai/settings");
  return response.data;
}

export async function updateGenerationMode(generationMode) {
  const response = await apiClient.put("/api/ai/settings/mode", {
    generationMode,
  });

  return response.data;
}
